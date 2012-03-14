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

package net.sourceforge.jffmpeg.codecs.audio.vorbis.mapping;

import net.sourceforge.jffmpeg.codecs.audio.vorbis.VorbisDecoder;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;

import javax.media.Buffer;

public class Mapping0 extends Mapping {
    private int channels;
    private int submaps;
    private int coupling_steps;
    private int[] coupling_mag  = new int[ 257 ];
    private int[] coupling_ang  = new int[ 257 ];
    private int[] chmuxlist     = new int[ 6 ];
    private int[] floorsubmap   = new int[ 17 ];
    private int[] residuesubmap = new int[ 17 ];
    private float[][] pcm;
    private int pcmend;

    private static final int ilog( long v ) {
       int ret=0;
       if ( v > 0 ) v--;
       while( v > 0 ){
           ret++;
           v >>= 1;
       }
       return(ret);
    }

    public void unpack( OggReader oggRead, int channels ) {
        this.channels = channels;

        if ( oggRead.getBits( 1 ) == 1 ) {
            submaps = (int)oggRead.getBits(4) + 1;
        } else {
            submaps = 1;
        }

        if ( oggRead.getBits( 1 ) == 1 ) {
            coupling_steps = (int)oggRead.getBits(8) + 1;
            for ( int i = 0; i < coupling_steps; i++ ) {
                coupling_mag[i] = (int)oggRead.getBits( ilog(channels) );
                coupling_ang[i] = (int)oggRead.getBits( ilog(channels) );
            }
        }
        if ( oggRead.getBits(2) > 0 ) throw new Error( "Reserved" );

        if ( submaps> 1 ) {
            for ( int i = 0; i < channels; i++ ) {
                chmuxlist[i] = (int)oggRead.getBits( 4 );
            }
        }

        for ( int i = 0; i < submaps; i++ ) {
            oggRead.getBits(8);
            floorsubmap[i] = (int)oggRead.getBits( 8 );
            residuesubmap[i] = (int)oggRead.getBits( 8 );
        }

        pcm = new float[ channels ][ 4096 ];  //TODO moveme
        pcmb = new float[ channels ][ 4096 ];  //TODO moveme
    }

    public void inverse( OggReader oggRead, VorbisDecoder vorbis ) {
        pcmend = vorbis.getBlockSize( vorbis.getW() ? 1:0 );
        long  n = pcmend;
//        System.out.println( "mapping0_inverse " + n );

        float[][] pcmbundle  = new float[ channels ][ (int)n ];
        int[]     zerobundle = new int[ channels ];
        int[]     nonzero    = new int[ channels ];
        Object[]  floormemo  = new Object[ channels ];

        /* recover the spectral envelope; store it in the PCM vector for now */
        for(int i = 0; i < channels; i++ ) {
            int submap = chmuxlist[ i ];
            floormemo[i]=vorbis.getFloor( floorsubmap[submap] ).inverse1(oggRead, vorbis);
            if( floormemo[i] != null) {
                nonzero[i]=1;
            } else {
                nonzero[i]=0;
            }
            // TODO clear channel pcm
            for ( int j = 0; j < n; j++ ) {
                pcm[i][j] = 0;
            }
        }
    
        /* channel coupling can 'dirty' the nonzero listing */
        for( int i = 0; i < coupling_steps; i++ ){
            if( nonzero[coupling_mag[i]] != 0  ||
                nonzero[coupling_ang[i]] != 0 ){
                   nonzero[coupling_mag[i]]=1; 
                   nonzero[coupling_ang[i]]=1; 
	    }
        }

        /* recover the residue into our working vectors */
        for(int i = 0; i < submaps; i++) {
            int ch_in_bundle = 0;
            for(int j = 0; j < channels; j++) {
                if(chmuxlist[j]==i) {
                    if( nonzero[j] != 0 ) {
                        zerobundle[ch_in_bundle]=1;
                    } else {
                        zerobundle[ch_in_bundle]=0;
                    }
                    pcmbundle[ ch_in_bundle++ ] = pcm[j];
                }
            }
            vorbis.getResidue(residuesubmap[i]).inverse( oggRead,
                 pcmbundle, zerobundle, ch_in_bundle);
        }

        /* channel coupling */
        for(int i = coupling_steps-1; i >= 0; i-- ){
            float[] pcmM=pcm[ coupling_mag[i] ];
            float[] pcmA=pcm[ coupling_ang[i] ];

            for( int j = 0; j < n/2; j++ ) {
                float mag=pcmM[j];
                float ang=pcmA[j];

                if(mag>0) {
                    if(ang>0){
                        pcmM[j]=mag;
                        pcmA[j]=mag-ang;
                    } else {
                        pcmA[j]=mag;
                        pcmM[j]=mag+ang;
                    }
                } else {
                    if(ang>0){
                        pcmM[j]=mag;
                        pcmA[j]=mag+ang;
                    } else {
                        pcmA[j]=mag;
                        pcmM[j]=mag-ang;
                    }
                }
            }
        }

        /* compute and apply spectral envelope */
        for( int i = 0; i < channels; i++ ) {
            float[] pcmt = pcm[i];
            int submap = chmuxlist[i];
            vorbis.getFloor( floorsubmap[submap] ).inverse2( floormemo[i], pcmt, vorbis );
        }

        /* transform the PCM data; takes PCM vector, vb; modifies PCM vector */
        /* only MDCT right now.... */
        for ( int i = 0; i < channels; i++ ) {
            float[] pcmt = pcm[i];
            vorbis.getMdct().mdct_backward(pcmt,pcmt);

        }

        /* window the data */
        for( int i = 0; i < channels; i++ ) {
            float[] pcmt = pcm[i];
            if ( nonzero[i] != 0 ) {
                _vorbis_apply_window(pcmt, vorbis);
            } else {
                for( int j = 0; j < n; j++ ) {
                     pcmt[j] = 0.f;
                }
            }
    { /*
        int q;
        System.out.println( "after window" );
        for ( q = 0; q < n; q++ ) {
            System.out.print( ((int)(pcmt[q] * 10000000)) + " " );
        }
        System.out.println();
      */     } 
        }
//System.out.println( "mapping 0 done" );
    }

//    public abstract void forward();
    private float[][] window = new float[ 2 ][];

    public Mapping0( VorbisDecoder vorbis ) {
        for ( int w = 0; w < 2; w++ ) {
            int left = vorbis.getBlockSize(w) / 2;
            window[w] = new float[ left ];

            /* The 'vorbis window' (window 0) is sin(sin(x)*sin(x)*2pi) */
            for( int i = 0; i < left; i++ ) {
                double x = ((double)i + 0.5f) / ((double)left) * Math.PI / 2.;
                x  = Math.sin(x);
                x *= x;
                x *= Math.PI / 2.f;
                x  = Math.sin(x);
                window[w][i] = (float)x;
            }
        }
    }

    private void _vorbis_apply_window( float[] d, VorbisDecoder vorbis ) {
        int lW = vorbis.getW() ? vorbis.getlW() : 0;
        int nW = vorbis.getW() ? vorbis.getnW() : 0;

        int n  = vorbis.getBlockSize( vorbis.getW() ? 1 : 0 );
        int ln = vorbis.getBlockSize( lW );
        int rn = vorbis.getBlockSize( nW );
    
        int leftbegin=n/4-ln/4;
        int leftend=leftbegin+ln/2;
    
        int rightbegin=n/2+n/4-rn/4;
        int rightend=rightbegin+rn/2;
        int i,p;
    
        for(i=0;i<leftbegin;i++) {
           d[i] = 0.f;
        }
    
        for(p=0;i<leftend;i++,p++) {
           d[i] *= window[ lW ][p];
        }
    
        for(i=rightbegin,p=rn/2-1;i<rightend;i++,p--) {
           d[i] *= window[ nW ][p];
        }
    
        for( ; i < n; i++ ) {
           d[i]  =0.f;
        }
    }

    private static boolean lastWindow;
    private static boolean thisWindow = false;
    private static int centerW      = -1;
    private static int pcm_returned = -1;
    private static int pcm_current  = -1;
    private static float[][] pcmb;
    public void vorbis_synthesis_blockin( VorbisDecoder vorbis ) {
//        System.out.println( "VORBIS_SYNTHESIS_BLOCKIN" );
        lastWindow = thisWindow;
        thisWindow = vorbis.getW();

        int n  = vorbis.getBlockSize( thisWindow ? 1 : 0 )/ 2;
        int n0 = vorbis.getBlockSize( 0 ) / 2;
        int n1 = vorbis.getBlockSize( 1 ) / 2;
    
        int thisCenter;
        int prevCenter;
    
        if ( centerW != 0 ) {
            thisCenter = n1;
            prevCenter = 0;
        } else {
            thisCenter = 0;
            prevCenter = n1;
        }
//System.out.println( "Wd " + (lastWindow?1:0) + " " + (thisWindow?1:0) );
//System.out.println( "B " + n + " " + n0 + " " + n1 + " " + thisCenter + " " + prevCenter );
        /* v->pcm is now used like a two-stage double buffer.  We don't want
            to have to constantly shift *or* adjust memory usage.  Don't
            accept a new block until the old is shifted out */
    
        /* overlap/add PCM */
    
        for( int j = 0; j < channels; j++ ) {
    {/*
        int q;
        System.out.println( "before overlap" );
        for ( q = 0; q < n; q++ ) {
            System.out.print( ((int)(pcm[j][q] * 10000000)) + " " );
        }
        System.out.println(); */
     } 
//    System.out.println( "do overlap" );
           /* the overlap/add section */
           if( lastWindow ) {
               if ( thisWindow ) {
                   /* large/large */
                   int pcmPointer  = prevCenter;
                   int pcmPointer2 = 0;
                   for ( int i = 0; i < n1; i++ ) {
//System.out.print( " " + ((int)(pcm[j][i + pcmPointer2 ] *10000000 ) ) );
                       pcmb[j][i + pcmPointer] += pcm[j][i + pcmPointer2 ];
                   }
               } else {
                   /* large/small */
                   int pcmPointer  = prevCenter + n1 / 2 - n0 / 2;
                   int pcmPointer2 = 0;
                   for( int i = 0; i < n0; i++ ) {
//System.out.print( " " + ((int)(pcm[j][i + pcmPointer2 ] *10000000 ) ) );
                       pcmb[j][i + pcmPointer] += pcm[j][i + pcmPointer2 ];
                   }
               }
           } else {
               if ( thisWindow ) {
                   /* small/large */
                   int pcmPointer  = prevCenter;
                   int pcmPointer2 = n1 / 2 - n0 / 2;
                   int i;
                   for ( i = 0; i < n0; i++ ) {
//System.out.print( " " + ((int)(pcm[j][i + pcmPointer2 ] *10000000 ) ) );
                       pcmb[j][i + pcmPointer ] += pcm[j][i + pcmPointer2];
                   }
                   for ( ; i < n1 / 2 + n0 / 2; i++ ) {
//System.out.print( " " + ((int)(pcm[j][i + pcmPointer2 ] *10000000 ) ) );
                       pcmb[j][i + pcmPointer ] = pcm[j][i + pcmPointer2];
                   }
               } else {
                   /* small/small */
                   int pcmPointer = prevCenter;
                   int pcmPointer2 = 0;
                   for( int i = 0; i < n0; i++ ) {
//System.out.print( " " + ((int)(pcm[j][i + pcmPointer2 ] *10000000 ) ) );
                        pcmb[j][i + pcmPointer ] += pcm[j][i + pcmPointer2];
                   }
               }
           }
//  System.out.println();
           /* the copy section */
           int pcmPointer  = thisCenter;
           int pcmPointer2 = n;
           for( int i = 0; i < n; i++ ) {
               pcmb[j][i + pcmPointer]=pcm[j][i + pcmPointer2];
           }
       }

       if( centerW != 0 ) {
           centerW = 0;
       } else {
           centerW = n1;
       }
    
       /* deal with initial packet state; we do this using the explicit
          pcm_returned==-1 flag otherwise we're sensitive to first block
          being short or long */
    
       if ( pcm_returned ==- 1 ) {
           pcm_returned = thisCenter;
           pcm_current  = thisCenter;
       } else {
           pcm_returned=prevCenter;
           pcm_current= prevCenter 
                          + vorbis.getBlockSize(lastWindow ? 1 : 0)/4
                          + vorbis.getBlockSize(thisWindow ? 1 : 0)/4;
       }
   }

    public void soundOutput( Buffer output ) {
        int size = (pcm_current - pcm_returned);
        byte[] out = (byte[])output.getData();
        if ( out == null || out.length < output.getLength() + size * 4 ) {
            byte[] t = new byte[ output.getLength() + size * 4 ];
            if ( out != null ) System.arraycopy( t, 0, out, 0, output.getLength() );
            output.setData( t );
            out = t;
        }
//	System.out.println( " " + pcm_current + " " + size );
        int offset = output.getLength();
//        System.out.println( "Channel" );
        for ( int i = 0; i < size; i++ ) {
            int s = scale( pcmb[ 0 ][ i + pcm_returned ] );
            out[ offset + i * 4 + 1  ] = (byte)((s & 0xff00) >> 8);
            out[ offset + i * 4 + 0 ] = (byte)(s & 0xff);
//            System.out.print( " " + s );
            s = scale( pcmb[ 1 ][ i + pcm_returned ] );
            out[ offset + i * 4 + 3 ] = (byte)((s & 0xff00) >> 8);
            out[ offset + i * 4 + 2 ] = (byte)(s & 0xff);
        }
//        System.out.println();
        output.setLength( offset + (size) * 4 );
    }

    private static final int scale( double in ) {
        int val = (int)(in * 32767.f);
        if(val>32767){
            val=32767;
        }
        if(val<-32768){
            val=-32768;
        }
        return val;
    }
}
