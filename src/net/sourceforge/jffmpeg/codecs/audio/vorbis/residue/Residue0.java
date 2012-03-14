/*
 * Java port of ogg demultiplexer.
 * Copyright (c) 2004 Jonathan Hueber.
 *
 * License conditions are the same as OggVorbis.  See README.
 * 1a39e335700bec46ae31a38e2156a898
 */
/********************************************************************
 *                                                                  *
 * THIS FILE IS PART OF THE OggVorbis SOFTWARE CODEC SOURCE CODE.   *
 * USE, DISTRIBUTION AND REPRODUCTION OF THIS LIBRARY SOURCE IS     *
 * GOVERNED BY A BSD-STYLE SOURCE LICENSE INCLUDED WITH THIS SOURCE *
 * IN 'COPYING'. PLEASE READ THESE TERMS BEFORE DISTRIBUTING.       *
 *                                                                  *
 * THE OggVorbis SOURCE CODE IS (C) COPYRIGHT 1994-2002             *
 * by the XIPHOPHORUS Company http://www.xiph.org/                  *
 *                                                                  *
 ********************************************************************/

package net.sourceforge.jffmpeg.codecs.audio.vorbis.residue;

import net.sourceforge.jffmpeg.codecs.audio.vorbis.CodeBook;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.VorbisDecoder;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;

public class Residue0 extends Residue {
    protected int begin;
    protected int end;
    protected int grouping;
    protected int partitions;
    protected int groupbook;
    protected int[] secondstages;
    protected int[] booklist = new int[ 32 * 8 ];


    private static int icount( int v ) {
        int ret = 0;
        while( v > 0 ) {
            ret += v&1;
            v >>= 1;
        }
        return ret;
    }

    public void unpack( OggReader oggRead ) {
        begin      = (int)oggRead.getBits( 24 );
        end        = (int)oggRead.getBits( 24 );
        grouping   = (int)oggRead.getBits( 24 ) + 1;
        partitions = (int)oggRead.getBits( 6 ) + 1;
        groupbook  = (int)oggRead.getBits( 8 );

        secondstages = new int[ partitions ];
        int acc = 0;
        for ( int i = 0; i < partitions; i++ ) {
            int cascade = (int)oggRead.getBits( 3 );
            if ( oggRead.getBits(1) == 1 ) {
                cascade |= ((int)oggRead.getBits( 5 )) << 3;
            }
            secondstages[i] = cascade;
            acc += icount( cascade );
        }
        for ( int i = 0; i < acc; i++ ) {
            booklist[i] = (int)oggRead.getBits(8);
        }
    }

    protected int partvals;
    protected int stages;
    protected long[][] decodemap;
    protected CodeBook[][] partbooks;
    protected CodeBook phrasebook;

    public void look( VorbisDecoder vorbis ) {
        int parts = partitions;
        partbooks = new CodeBook[ parts ][];
        phrasebook = vorbis.getCodeBook( groupbook );
        int dim = phrasebook.getDim();
//	System.out.println( "residue_look" );
        int maxstage=0;
        int acc = 0;
        for ( int j = 0; j < parts; j++ ) {
            int stages = ilog(secondstages[j]);
            if( stages != 0 ) {
                if( stages > maxstage ) maxstage = stages;
                partbooks[ j ] = new CodeBook[ stages ];
                for(int k = 0; k < stages; k++ ) {
                    if ( (secondstages[j] & (1<<k)) != 0 ) {
                        partbooks[j][k]= vorbis.getCodeBook( booklist[acc++] );
                    }
                }
            }
        }

        partvals = (int)Math.rint( Math.pow( (double)parts, (double)dim ) );
        decodemap = new long[ partvals ][ dim ];
        stages = maxstage;
        for ( int j = 0; j < partvals; j++ ) {
            long val=j;
            long mult= partvals / parts;
            for( int k = 0; k < dim; k++ ) {
                long deco = val / mult;
                val -= deco * mult;
                mult /= parts;
                decodemap[j][k] = deco;
//System.out.println( "decodemap[" + j + "][" + k + "]=" + deco );
            }
        }
    }

    private static final int ilog( int v ) {
        int ret=0;
        while( v > 0 ) {
            ret++;
            v >>= 1;
        }
        return ret;
    }



//    public abstract void forward();


    public void inverse( OggReader oggRead, float[][] in, int[] nonZero, int channels ) {
//        System.out.println( "Residue0 inverse" );

        int used = 0;
        for ( int i = 0; i < channels; i++ ) {
            if ( nonZero[ i ] != 0 ) {
                in[ used++ ] = in[ i ];
            }
        }
        if ( used > 0 ) {
            _01inverse( oggRead, in, used );
        }
    }

    protected void _01inverse( OggReader oggRead, float[][] in, int ch ) {
        /* move all this setup out later */
        int samples_per_partition = grouping;
        int partitions_per_word = phrasebook.getDim();
        int n = end - begin;
  
        int partvals = n/samples_per_partition;
        int partwords = (partvals+partitions_per_word-1)/partitions_per_word;

        long[][][] partword = new long[ ch ][ partwords ][];

        for( int s = 0; s < stages; s++ ) {
            /* each loop decodes on partition codeword containing 
               partitions_pre_word partitions */
            int i = 0;
            int l = 0;
            for ( ; i < partvals; l++ ) {
                if( s == 0 ) {
                    /* fetch the partition word for each channel */
                    for ( int j = 0; j < ch; j++ ) {
                        int temp = phrasebook.decode( oggRead );
                        partword[j][l] = decodemap[temp];
                    }
                }
      
                /* now we decode residual values for the partitions */
                for( int k = 0; k < partitions_per_word && i < partvals;
                                k++, i++ ) {
                    for( int j = 0; j < ch; j++ ) {
                        long offset = begin + i * samples_per_partition;
                        int pw = (int)partword[j][l][k];
                        if ( (secondstages[ pw ] & (1<<s)) != 0 ) {
                            CodeBook stagebook=partbooks[pw][s];
                            if ( stagebook != null ) {
                                decodepart( oggRead, stagebook, 
                                            in[j], (int)offset,
                                            samples_per_partition);
                            }
                        }
                    }
                } 
            }
        }
    }

    /**
     * Overrriden in Residue 1
     */
    protected void decodepart( OggReader oggRead, CodeBook b, 
                               float[] in, int offset, int spp ) {
        b.decodevs_add( in, offset, oggRead, spp );
    }
}
