/*
 * Java port of parts of the ffmpeg Mpeg4 base decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2001 Fabrice Bellard.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * See Credits file and Readme for details
 */
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3;

import net.sourceforge.jffmpeg.codecs.video.mpeg.DisplayOutput;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.Mpeg4Exception;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;
import net.sourceforge.jffmpeg.codecs.utils.BitStream;

/**
 * MPEG4 base decoder.  This contains many functions common to various
 * Mpeg4-like video codecs.
 */
public abstract class Mpeg4 {
    /**
     * PictType values
     */
    public static final int I_FRAME_TYPE  = 1;
    public static final int P_FRAME_TYPE  = 2;
    public static final int B_FRAME_TYPE  = 3;


    /**
     * Input handler
     */
    protected BitStream in = new BitStream();

    /**
     * Decoder state
     */
    protected boolean truncatedFlag;
    protected int pictType;  
    protected int qscale; 
    protected int sliceHeight;

    protected int bitRate;
    protected boolean flipFlopRounding;

    protected boolean perMbRlTable;
    protected int rlChromaTableIndex;
    protected int rlTableIndex;

    protected int dcTableIndex;
    protected boolean interIntraPred;

    protected boolean ac_Pred;
    protected int h263_aic_dir;
    protected boolean inter_intra_pred;

    /**
     * Predefined values
     */
    protected int mbWidth;
    protected int mbHeight;

    protected int[] blankBlock = new int[ 64 ];
    public static final int NUMBER_OF_BLOCKS = 6;
    protected int[] blockIndex = new int[ NUMBER_OF_BLOCKS ];
    protected int[] blockWrap  = new int[ NUMBER_OF_BLOCKS ];
    protected int[][] block    = new int[ NUMBER_OF_BLOCKS ][ 64 ];
    protected boolean []skipBlock = new boolean[ NUMBER_OF_BLOCKS ];

    protected int[] yDcScaleTable;
    protected int[] cDcScaleTable;
    protected int y_dc_scale = 8; // from q_scale
    protected int c_dc_scale = 8;
    protected int[] dc_val;      //DC values
    protected int[] ac_val;      //AC values
    protected int[][] motion_val;  //Motion values
    protected int pel_motionX;
    protected int pel_motionY;
    protected int resync_mb_x;

    protected boolean use_skip_mb_code;
    
    /**
     * If we skip a macroblock twice in a row, we do nothing
     */
    protected boolean[][] skipTable;
    protected boolean mb_skipped;
    protected int mv_table_index;

    protected boolean mb_intra;
    protected int[] coded_block;

    protected boolean h263_pred = true;
    protected boolean h263_aic;
    protected int[] mbintra_table;

    protected boolean dc_pred_dir;
    protected boolean ac_pred;

    protected boolean no_rounding = true;
    /**
     * Output
     */
    protected DisplayOutput displayOutput;

    /**
     * Decode a frame
     */
    protected abstract void decodeFrame( byte[] frameData, int dataLength ) throws FFMpegException;

    /**
     * Unquantize DCT values for B frames
     */
    protected void dct_unquantize_h263( int blockNumber ) {
        int i = 0;
        
        if ( mb_intra ) {
            block[ blockNumber ][ 0 ] *= (blockNumber < 4 ) ? y_dc_scale : c_dc_scale;
            i = 1;
        } 

        int qmul = qscale << 1;
        int qadd = (qscale - 1) | 1;
        for ( ; i < 64; i++ ) {
            if ( block[ blockNumber ][ i ] > 0 ) {
                block[ blockNumber ][ i ] = block[ blockNumber ][ i ] * qmul + qadd;
            } else if ( block[ blockNumber ][ i ] < 0 ) {
                block[ blockNumber ][ i ] = block[ blockNumber ][ i ] * qmul - qadd;
            }
        }
    }

    /*
     * Display a Macroblock
     *    - DCT for I macroblocks
     *    - motion followed by addition of DCT for P macroblocks
     */
    protected void MPV_decode_mb( int x, int y ) {
        if ( !mb_intra ) {
            /*
             * Predicted macroblock
             */
            if ( h263_pred || h263_aic ) {
                if ( mbintra_table[ y * mbWidth + x ] != 0 ) {
		    mbintra_table[ y * mbWidth + x ] = 0;
                    /** ff_clean_intra_table_entries */
                    int xy = blockIndex[0];
                    int wrap = blockWrap[0];

                    dc_val[ xy            ] = 0x400;
                    dc_val[ xy + 1        ] = 0x400;
                    dc_val[ xy     + wrap ] = 0x400;
                    dc_val[ xy + 1 + wrap ] = 0x400;

                    /** ac_val */
                    System.arraycopy( blankBlock, 0, ac_val, xy * 16, 32 );
                    System.arraycopy( blankBlock, 0, ac_val, (xy + wrap)*16, 32 );

                    /** Chroma */
                    wrap = blockWrap[4];
                    xy = blockWrap[ 0 ] * ( mbHeight * 2 + 2 )
                         + x + 1 + (y + 1) * wrap;

                    dc_val[xy] = 0x400;
                    dc_val[xy + wrap * (mbHeight+2) ] = 0x400;

                    /* ac pred */
                    System.arraycopy( blankBlock, 0, ac_val, xy*16, 16 );
                    System.arraycopy( blankBlock, 0, ac_val, (xy + wrap * (mbHeight + 2))*16, 16 );
                }
            }
            
            /* Can we skip this mb? - note frame buffer */
            if ( !(mb_skipped && skipTable[ x ][ y ]) ) {
                skipTable[ x ][ y ] = mb_skipped;

                /* Motion code */
                displayOutput.move( x, y, pel_motionX, pel_motionY, !no_rounding );

                displayOutput.addLuminanceIdct( x * 2,     y * 2,     block[ 0 ] );
                displayOutput.addLuminanceIdct( x * 2 + 1, y * 2,     block[ 1 ] );
                displayOutput.addLuminanceIdct( x * 2,     y * 2 + 1, block[ 2 ] );
                displayOutput.addLuminanceIdct( x * 2 + 1, y * 2 + 1, block[ 3 ] );
                displayOutput.addRedIdct( x, y, block[ 5 ] );
                displayOutput.addBlueIdct( x, y, block[ 4 ] );
            }
        } else {
            /**
             * I type macroblock
             */
            skipTable[ x ][ y ] = false;
            if ( h263_pred || h263_aic ) {
                mbintra_table[ y * mbWidth + x ] = 1;
            }
            
            /**
             * Not motion
             */
            pel_motionX = 0;
            pel_motionY = 0;
        
	    /* Display Macro block */
            int mbX = x;
            int mbY = y;
            dct_unquantize_h263( 0 );
	    dct_unquantize_h263( 1 );
	    dct_unquantize_h263( 2 );
	    dct_unquantize_h263( 3 );
	    dct_unquantize_h263( 4 );
	    dct_unquantize_h263( 5 ); 
            displayOutput.putLuminanceIdct( x * 2,     y * 2,     block[ 0 ] );
            displayOutput.putLuminanceIdct( x * 2 + 1, y * 2,     block[ 1 ] );
            displayOutput.putLuminanceIdct( x * 2,     y * 2 + 1, block[ 2 ] );
            displayOutput.putLuminanceIdct( x * 2 + 1, y * 2 + 1, block[ 3 ] );
            displayOutput.putRedIdct( x, y, block[ 5 ] );
            displayOutput.putBlueIdct( x, y, block[ 4 ] );
        }
        
        /**
         * Motion cache
         */
        for ( int i = 0; i < 4; i++ ) {
            motion_val[blockIndex[ i ]][0] = pel_motionX;
            motion_val[blockIndex[ i ]][1] = pel_motionY;
        }

        /**
         * Clear blocks for next round
         */
        System.arraycopy( blankBlock, 0, block[0], 0, 64 );
        System.arraycopy( blankBlock, 0, block[1], 0, 64 );
        System.arraycopy( blankBlock, 0, block[2], 0, 64 );
        System.arraycopy( blankBlock, 0, block[3], 0, 64 );
        System.arraycopy( blankBlock, 0, block[4], 0, 64 );
        System.arraycopy( blankBlock, 0, block[5], 0, 64 );
    }

    /**
     * Constructor does nothing
     */
    protected Mpeg4() {}
    
    /**
     * Initialise the width and height of this codec
     */
    protected void initialise( int width, int height ) {
        /**
         * Calculate number of macroblocks
         */
        mbWidth = (width + 15) /16;
        mbHeight = (height + 15) / 16;

        /**
         * Block wrapping 
         */
        blockWrap[ 0 ] = mbWidth * 2 + 2;
        blockWrap[ 1 ] = mbWidth * 2 + 2;
        blockWrap[ 2 ] = mbWidth * 2 + 2;
        blockWrap[ 3 ] = mbWidth * 2 + 2;
        blockWrap[ 4 ] = mbWidth     + 2;
        blockWrap[ 5 ] = mbWidth     + 2;

        /**
         * Initialise DC / AC / motion prediction tables
         */
        dc_val     = new int[ 1 + (mbWidth * 2 + 2) * mbHeight * 2  * 4 ];
        ac_val     = new int[ 1 + (mbWidth * 2 + 2) * mbHeight * 16 * 4 * 16];
        motion_val = new int[ 1 + (mbWidth * 2 + 2) * mbHeight * 2  * 4 ][2];
        skipTable   = new boolean[ mbWidth ][ mbHeight ];
        
        for ( int i = 0; i < dc_val.length; i++ ) {
            dc_val[i] = 0x400;
            ac_val[i] = 0;
        }

        coded_block = new int[ 1 + (mbWidth * 2 + 2) * mbHeight * 16 * 4 ];

        mbintra_table = new int[  1 + (mbWidth * 2 + 2) * mbHeight * 2 * 4 ];
        for ( int y = 0; y < mbintra_table.length; y++ ) mbintra_table[ y ] = 1;

        /*
         * Initialise output
         */
        displayOutput = new DisplayOutput( mbWidth, mbHeight );
    }
}
