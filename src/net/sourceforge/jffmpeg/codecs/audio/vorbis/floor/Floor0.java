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

package net.sourceforge.jffmpeg.codecs.audio.vorbis.floor;

import net.sourceforge.jffmpeg.codecs.audio.vorbis.VorbisDecoder;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.CodeBook;

public class Floor0 extends Floor {

    private int order;
    private int rate;
    private int barkmap;
    private int ampbits;
    private int ampdB;
    private int numbooks;
    private int[] books;

    /* Look structure */
    private int ln;
    private int  m;
    private int[][] linearmap;
    private int[] n;
    private float[] lsp_look;
    private long bits;
    private long frames;

    public void unpack( OggReader oggRead ) {
        order    = (int)oggRead.getBits(8);
        rate     = (int)oggRead.getBits(16);
        barkmap  = (int)oggRead.getBits(16);
        ampbits  = (int)oggRead.getBits(6);
        ampdB    = (int)oggRead.getBits(8);
        numbooks = (int)oggRead.getBits(4) + 1;
        books = new int[ numbooks ];
        for ( int i = 0; i < numbooks; i++ ) {
            books[i] = (int)oggRead.getBits( 8 );
        }
    }


    private static final int _ilog( long v ) {
       int ret=0;
       while( v > 0 ){
           ret++;
           v >>= 1;
       }
       return(ret);
    }

    public void look() {
//        System.out.println( "Floor0 look" );
    }

    public Object inverse1( OggReader oggRead, VorbisDecoder vorbis ) {
        System.out.println( "Floor0 inverse1" );

        int ampraw = (int)oggRead.getBits( ampbits );

        if( ampraw > 0 ) {
            long maxval= (1<<ampbits) - 1;
            float amp = ((float)ampraw) / maxval * ampdB;
            int booknum = (int)oggRead.getBits(_ilog(numbooks));
            if( booknum < numbooks) {
                CodeBook b = vorbis.getCodeBook(booknum);
                float last=0.f;
                int dim = b.getDim();
		float[] lsp = new float[ m + dim + 1 ];
            
                for( int j = 0;j < m; j += dim) {
                    b.decodev_set( lsp, j, oggRead, dim);
                }
                for( int j=0; j<m; ) {
                    for( int k = 0; k < dim; k++,j++) {
                        lsp[j] += last;
                        last=lsp[j-1];
                    }
                }                      
                lsp[ m ]= amp;
                return lsp;
            }
        }
        throw new Error( "Bad value" );
    }

    public void inverse2( Object floor, float[] pcm, VorbisDecoder vorbis ) {
        System.out.println( "Floor0 inverse2" );
        float[] lsp = (float[])floor;
        float   amp = lsp[ m ];

        /* take the coefficients back to a spectral envelope curve *
        vorbis_lsp_to_curve( out,
                             linearmap[W],
                             n[W],
                             ln,
                             lsp,
                             m, amp, (float)ampdB);
	*/
    }

//    public abstract void inverse2();
}
