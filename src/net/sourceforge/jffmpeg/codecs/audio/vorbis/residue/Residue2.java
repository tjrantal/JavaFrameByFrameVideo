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
import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;

public class Residue2 extends Residue0 {
//    public abstract void look();
//    public abstract void forward();


    public void inverse(OggReader oggRead, float[][] in, int[] nonZero, int ch) {
        /* move all this setup out later */
        int samples_per_partition = grouping;
        int partitions_per_word = phrasebook.getDim();
        int n = end - begin;
  
        int partvals = n/samples_per_partition;
        int partwords = (partvals+partitions_per_word-1)/partitions_per_word;

        long[][] partword = new long[ partwords ][];

//        System.out.println( "Residue2 inverse" );

        int i;
        for( i = 0; i < ch; i++ ) if ( nonZero[i] != 0 ) break;
        if( i == ch ) return; /* no nonzero vectors */

        for( int s = 0; s < stages; s++ ) {
            int l = 0;
            for( i = 0; i < partvals; l++ ) {
                if ( s == 0 ) {
                    /* fetch the partition word */
                    int temp = phrasebook.decode(oggRead);
//System.out.println( "temp " + temp );
                    partword[l] = decodemap[temp];
                }

                /* now we decode residual values for the partitions */
                for(int k=0; k < partitions_per_word && i<partvals; k++,i++ ) {
                    if ( (secondstages[(int)partword[l][k]]&(1<<s)) != 0 ) {
                        CodeBook stagebook = partbooks[(int)partword[l][k]][s];
	  
                        if ( stagebook != null ) {
                            stagebook.decodevv_add( in, 
                              i * samples_per_partition + begin, ch, 
                              oggRead, samples_per_partition );
                        }
                    }
                } 
            }
        }
    }
}
