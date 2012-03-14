/*
 * Java port of ffmpeg DIVX decoder.
 * Copyright (c) 2004 Jonathan Hueber.
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
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.codecs.video.mpeg4.divx;

import javax.media.Codec;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.Buffer;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import java.awt.Dimension;

import net.sourceforge.jffmpeg.JMFCodec;

import net.sourceforge.jffmpeg.codecs.video.mpeg4.Mpeg4Exception;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;
import net.sourceforge.jffmpeg.codecs.utils.BitStream;
import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

import net.sourceforge.jffmpeg.codecs.video.mpeg.DisplayOutput;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.divx.vlc.*;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.divx.rltables.*;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.divx.tables.ScanTable;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.rltables.RLTable;

/**
 * This is a JMF Video Codec.
 * This is a port from ffmpeg - This version targets H263
 */
public class DIVXCodec implements Codec, JMFCodec {
    public static final boolean debug = false;
    public static final boolean debug2 = false;

    /**
     * Width and height
     */
    int width, height;
    int mbWidth, mbHeight;

    /**
     * Current picture cache
     */
    private int[] mb_type;
    private int[] cbp_table;
    private int[] qscale_table;
    private int[][] motion_val;
    private int[] ac_val;
    private int[] dc_val;
    private int[] pred_dir_table;
    private int[] mbintra_table;

    /**
     * mb_type (used in BFrame)
     */
    private int[] mb_type_b_frame;
    private int[] mb_type_ip_frame;

    private int[][] next_motion_val;

    /**
     * The negotiated InputFormat
     */
    private VideoFormat inputFormat;
    private DisplayOutput displayOutput;

    /**
     * Input handler
     */
    protected BitStream in = new BitStream();

    private boolean[][] skipTable;
    private boolean[][] mb_skiptable;

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
        int pel_motionX = mv[0][0][0];
        int pel_motionY = mv[0][0][1];
        boolean h263_pred = true;
        boolean h263_aic  = false;

        qscale_table[ x + y * mbWidth ] = qscale;
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
                    System.arraycopy( blank, 0, ac_val, xy * 16 + mbWidth * 16, 32 );
                    System.arraycopy( blank, 0, ac_val, (xy + wrap)*16 + mbWidth * 16, 32 );

                    /** Chroma */
                    dc_val[blockIndex[4]] = 0x400;
                    dc_val[blockIndex[5]] = 0x400;

                    /* ac pred */
		    if ( debug2 ) DisplayOutput.debug.println( "ac_pred_clean" );
                    System.arraycopy( blank, 0, ac_val, blockIndex[4]*16 + mbWidth * 16, 16 );
                    System.arraycopy( blank, 0, ac_val, blockIndex[5]*16 + mbWidth * 16, 16 );
                }
            }

            /* Store skipped MBs (for use in BFrame) */
            if ( pict_type == I_TYPE || pict_type == P_TYPE ) 
            {
                mb_skiptable[ x ][ y ] = mb_skipped;
            }
            
            /* Can we skip this mb? - note frame buffer */
	    mb_skipped = false;
            if ( !(mb_skipped && skipTable[ x ][ y ]) ) {
                skipTable[ x ][ y ] = mb_skipped;

                /* Motion code */
                if ( pict_type == B_TYPE ) {
                    if ( (mv_dir & MV_DIR_FORWARD) != 0 ) {   //testing
                        if ( mv_type == MV_TYPE_16X16 ) {
                            displayOutput.move( x, y, pel_motionX, pel_motionY, !no_rounding );
                        } else if ( mv_type == MV_TYPE_FIELD ) {
                            displayOutput.moveField( x, y, mv[0][0][0], mv[0][0][1], !no_rounding, false);
                            displayOutput.moveField( x, y, mv[0][1][0], mv[0][1][1], !no_rounding, true );
                        } else if ( mv_type == MV_TYPE_8X8 ) {
                            displayOutput.move8x8( x, y, mv[0][0][0], mv[0][0][1], !no_rounding, 0, 0 );
                            displayOutput.move8x8( x, y, mv[0][1][0], mv[0][1][1], !no_rounding, 8, 0 );
                            displayOutput.move8x8( x, y, mv[0][2][0], mv[0][2][1], !no_rounding, 0, 8 );
                            displayOutput.move8x8( x, y, mv[0][3][0], mv[0][3][1], !no_rounding, 8, 8 );
                        }
                    }

                    /* Backward motion (B Frames) */
                    if ( (mv_dir & MV_DIR_BACKWARD) != 0 ) {
                        boolean merge = (mv_dir & MV_DIR_FORWARD) != 0;
                        if ( mv_type == MV_TYPE_16X16 ) {
                            displayOutput.moveFromNext( x, y, mv[1][0][0], mv[1][0][1], !no_rounding, merge );
                        } else if ( mv_type == MV_TYPE_FIELD ) {
                            displayOutput.moveFieldFromNext( x, y, mv[1][0][0], mv[1][0][1], !no_rounding, false, merge);
                            displayOutput.moveFieldFromNext( x, y, mv[1][1][0], mv[1][1][1], !no_rounding, true, merge );
                        } else if ( mv_type == MV_TYPE_8X8 ) {
                            displayOutput.move8x8FromNext( x, y, mv[1][0][0], mv[1][0][1], !no_rounding, 0, 0, merge );
                            displayOutput.move8x8FromNext( x, y, mv[1][1][0], mv[1][1][1], !no_rounding, 8, 0, merge );
                            displayOutput.move8x8FromNext( x, y, mv[1][2][0], mv[1][2][1], !no_rounding, 0, 8, merge );
                            displayOutput.move8x8FromNext( x, y, mv[1][3][0], mv[1][3][1], !no_rounding, 8, 8, merge );
                       }
		    }
                } else {
                    /* PFrame */
                    if ( mv_type == MV_TYPE_16X16 ) {
                        displayOutput.moveFromIPFrame( x, y, pel_motionX, pel_motionY, !no_rounding );
                    } else if ( mv_type == MV_TYPE_FIELD ) {
                        displayOutput.moveFieldFromIPFrame( x, y, mv[0][0][0], mv[0][0][1], !no_rounding, false);
                        displayOutput.moveFieldFromIPFrame( x, y, mv[0][1][0], mv[0][1][1], !no_rounding, true );
                    } else if ( mv_type == MV_TYPE_8X8 ) {
                        displayOutput.move8x8FromIPFrame( x, y, mv[0][0][0], mv[0][0][1], !no_rounding, 0, 0 );
                        displayOutput.move8x8FromIPFrame( x, y, mv[0][1][0], mv[0][1][1], !no_rounding, 8, 0 );
                        displayOutput.move8x8FromIPFrame( x, y, mv[0][2][0], mv[0][2][1], !no_rounding, 0, 8 );
                        displayOutput.move8x8FromIPFrame( x, y, mv[0][3][0], mv[0][3][1], !no_rounding, 8, 8 );
                    }
                }

                displayOutput.addLuminanceIdct( x * 2,     y * 2,     block[ 0 ] );
                displayOutput.addLuminanceIdct( x * 2 + 1, y * 2,     block[ 1 ] );
                displayOutput.addLuminanceIdct( x * 2,     y * 2 + 1, block[ 2 ] );
                displayOutput.addLuminanceIdct( x * 2 + 1, y * 2 + 1, block[ 3 ] );
                displayOutput.addBlueIdct( x, y, block[ 4 ] );
                displayOutput.addRedIdct( x, y, block[ 5 ] );
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
            mv_type = MV_TYPE_16X16;
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
            displayOutput.putBlueIdct( x, y, block[ 4 ] );
            displayOutput.putRedIdct( x, y, block[ 5 ] );
        }
        
        /**
         * Motion cache
         */
        if ( pict_type != B_TYPE && debug ) System.out.println( "mv_type " + mv_type );
        if ( pict_type != B_TYPE && mv_type != MV_TYPE_8X8) {
if ( debug ) System.out.println( "motion_x " + pel_motionX + " motion_y " + pel_motionY + " xy " + (blockIndex[0]-82) );
            for ( int i = 0; i < 4; i++ ) {
                motion_val[blockIndex[ i ]][0] = pel_motionX;
                motion_val[blockIndex[ i ]][1] = pel_motionY;
            } 
        }

        /**
         * Clear blocks for next round
         */
        System.arraycopy( blank, 0, block[0], 0, 64 );
        System.arraycopy( blank, 0, block[1], 0, 64 );
        System.arraycopy( blank, 0, block[2], 0, 64 );
        System.arraycopy( blank, 0, block[3], 0, 64 );
        System.arraycopy( blank, 0, block[4], 0, 64 );
        System.arraycopy( blank, 0, block[5], 0, 64 );
    }

    /**
     * Set the width and height in pixels for this video stream
     */
    protected void initialise( int width, int height ) {
        this.width = width;
        this.height = height;
        mbWidth= (width+15) / 16;
        mbHeight = (height+15) / 16;
        mb_stride = mbWidth;
        blockWrap[ 0 ] = mbWidth * 2 + 1;
        blockWrap[ 1 ] = mbWidth * 2 + 1;
        blockWrap[ 2 ] = mbWidth * 2 + 1;
        blockWrap[ 3 ] = mbWidth * 2 + 1;
        blockWrap[ 4 ] = mbWidth     + 1;
        blockWrap[ 5 ] = mbWidth     + 1;

        mb_type_b_frame  = new int[ mbWidth * mbHeight * 2 ];
        mb_type_ip_frame = new int[ mbWidth * mbHeight * 2 ];
        cbp_table    = new int[ mbWidth * mbHeight * 2 ];
        qscale_table = new int[ mbWidth * mbHeight * 2 ];
        for ( int i = 0; i < qscale_table.length; i++ ) {
            qscale_table[i] = 4;  /* 3 or 4 TODO figure this out!!! */
        }
        motion_val = new int[ mbWidth * mbHeight * 4 * 2][ 2 ];
        ac_val       = new int[ mbWidth * (mbHeight+2) * 4 * 16 * 2 ];
        dc_val       = new int[ mbWidth * mbHeight * 4 * 2 ];
        pred_dir_table  = new int[ mbWidth * mbHeight * 2];
        skipTable = new boolean[ mbWidth * 2 ][mbHeight * 2];
        mb_skiptable = new boolean[ mbWidth * 2 ][mbHeight * 2];
        mbintra_table = new int[ mbWidth * mbHeight * 2 ];
        /* TODO move this to the correct place */
        for ( int i = 0; i < dc_val.length; i++ ) dc_val[i] = 1024;
        displayOutput = new DisplayOutput( mbWidth, mbHeight );
    }

    /**
     * Construct the codec.  Initialization of the codec
     * should be performced via setInputFormat. 
     */
    public DIVXCodec() {
        super();
    }

    /**
     * Construct the codec with the width and height of the 
     * video in pixels.  This is the least data required to
     * use the codec.
     */
    public DIVXCodec(int width, int height) {
        super();
        initialise( width, height );
    }
    
    /**
     * Retrieve the supported input formats.  Currently "DIVX" 
     *
     * @return Format[] the supported input formats
     */
    public Format[] getSupportedInputFormats() {
        return new Format[] { new VideoFormat( "DIVX" ) };
    }
    
    /**
     * Retrieve the supported output formats.  Currently RGBVideo
     *
     * @return Format[] the supported output formats
     */
    public Format[] getSupportedOutputFormats(Format format) {
        return new Format[] { new RGBFormat() };
    }
    
    /**
     * Negotiate the format for the input data.  
     *
     * Only the width and height entries are used
     *
     * @return Format the negotiated input format
     */
    public Format setInputFormat( Format format ) {
        inputFormat = (VideoFormat)format;
        initialise( (int)inputFormat.getSize().getWidth(), 
                    (int)inputFormat.getSize().getHeight() );
        return format;
    }
    
    /**
     * Negotiate the format for screen display renderer.  
     *
     * Only the frame rate entry is used.  All the other 
     * values are populated using the negotiated input formnat.
     *
     * @return Format RGBFormat to supply to display renderer.
     */
    public Format setOutputFormat( Format format ) {
        return new RGBFormat( new Dimension( mbWidth * 16, mbHeight * 16 ), 
                             -1, (new int[0]).getClass(),         // array
                             inputFormat.getFrameRate(),          // Frames/sec
                             32, 0xff0000, 0x00ff00, 0x0000ff) ;  //Colours 
    }


    private static final int mid_pred( int a, int b, int c ) {
        if ( a > b ) {
            if ( c > b ) {
                if ( c > a ) b=a;
                else         b=c;
            }
        } else {
            if ( b > c ) {
                if ( c > a ) b=c;
                else         b=a;
            }
        }
        return b;
    }

    private int h263_predMotionX;
    private int h263_predMotionY;
    private void h263_pred_motion( int blockNumber ) {
        boolean h263_pred = true;
        int[] off = new int[] {2, 1, 1, -1};

        int mot_val_offset = blockIndex[ blockNumber ];
        int[] a;
        int[] b;
        int[] c;
        if ( debug ) {
            b = new int[2];
            c = new int[2];
        }
        int wrap = blockWrap[blockNumber];

        a = motion_val[ mot_val_offset - 1];
        if ( !first_slice_line || blockNumber >= 3 ) {
            /* Normal operation */
	    if ( debug ) System.out.println( "Normal " + (blockIndex[ blockNumber ]-82) );
            b = motion_val[ mot_val_offset- wrap];
            c = motion_val[ mot_val_offset + off[ blockNumber ] - wrap];
            h263_predMotionX = mid_pred(a[0], b[0], c[0]);
            h263_predMotionY = mid_pred(a[1], b[1], c[1]);
        } else {
            /* Edge cases */
            if ( blockNumber == 0 ) {
                if( mb_x  == resync_mb_x ) {
                    h263_predMotionX = 0;
                    h263_predMotionY = 0;
                } else if ( mb_x + 1 == resync_mb_x && h263_pred ) {
                    c = motion_val[ mot_val_offset + off[ blockNumber ] - wrap];
                    if ( mb_x == 0 ) {
                        h263_predMotionX = c[0];
                        h263_predMotionY = c[1];
                    } else {
                        h263_predMotionX = mid_pred(a[0], 0, c[0]);
                        h263_predMotionY = mid_pred(a[1], 0, c[1]);
                    }
                } else {
                    h263_predMotionX = a[0];
                    h263_predMotionY = a[1];
                }
            } else if ( blockNumber == 1 ) {
                if ( mb_x + 1 == resync_mb_x && h263_pred) {
                    c = motion_val[ mot_val_offset + off[ blockNumber ] - wrap];
                    h263_predMotionX = mid_pred(a[0], 0, c[0]);
                    h263_predMotionY = mid_pred(a[1], 0, c[1]);
                }else{
                    h263_predMotionX = a[0];
                    h263_predMotionY = a[1];
                }
            } else {
                /* blockNumber == 2 */
                b = motion_val[ mot_val_offset - wrap];
                c = motion_val[ mot_val_offset + off[ blockNumber ] - wrap];
                if( mb_x == resync_mb_x) {
                    a[0] = 0;
                    a[1] = 0;
                }
                h263_predMotionX = mid_pred(a[0], b[0], c[0]);
                h263_predMotionY = mid_pred(a[1], b[1], c[1]);
            }
        }
        if ( debug ) System.out.println( "A:" + a[0] + " B:" + b[0] + " C:" + c[0] + " " + h263_predMotionX + " " + h263_predMotionY );

    }

    private VLCTable mv_vlc = new MVTable();
    private int h263_decode_motion( int pred, int f_code ) throws FFMpegException {
        int code = in.getVLC( mv_vlc );
        if (code == 0)
            return pred;

        boolean sign = in.getTrueFalse();
        int shift = f_code - 1;
        int val = code;
        if ( debug ) System.out.println( "Sign " + (sign?1:0) + " " + shift + " " + val );
        if ( shift != 0 ) {
            val = (val - 1) << shift;
            val |= in.getBits(shift);
            val++;
        }
        if (sign)
            val = -val;
        val += pred;

        /* modulo decoding */
        boolean h263_long_vectors = false;
        if (!h263_long_vectors) {
            int l = 32 - 5 - f_code;
            val = ((val<<l)&0xffffffff)>>l;
        } else {
            /* horrible h263 long vector mode */
            if (pred < -31 && val < -63)
                val += 64;
            if (pred > 32 && val > 63)
                val -= 64;
        }
        return val;
    }

    private static final int ROUNDED_DIV( int a, int b ) {
        return (((a)>0 ? (a) + ((b)>>1) : (a) - ((b)>>1))/(b));
    }

    private void mpeg4_pred_ac( int[] block, int n ) {
        if ( debug ) System.out.println( "mpeg4_pred_ac" );
	if(debug2)        DisplayOutput.debug.println( "mpeg4_pred_ac" );
        int i;
        /* find prediction */
        int ac_val_offset  = blockIndex[n] * 16 + mbWidth * 16;
        int ac_val_offset1 = ac_val_offset;
        if (ac_pred) {
            if (!dc_pred_dir) {
                int xy= mb_x - 1 + mb_y * mbWidth -1;  /*note extra -1 here */
                /* left prediction */
                ac_val_offset -= 16;
            
                if(mb_x==0 || qscale == qscale_table[ xy+1] || n==1 || n==3 ) {
                     /* same qscale */
if (debug2) DisplayOutput.debug.println( "A" );
                    for(i=1;i<8;i++) {
                        block[ i<<3 ] += ac_val[ i + ac_val_offset ];
			if ( debug2) DisplayOutput.debug.print( Integer.toHexString( block[ i<<3 ] )+ " " + Integer.toHexString(ac_val[ i + ac_val_offset ]) + " ");
                    }
                } else {
                    /* different qscale, we must rescale */
if (debug2) DisplayOutput.debug.println( "B" );
                    for( i = 1; i < 8; i++ ) {
                        block[ i<<3 ] += ROUNDED_DIV(ac_val[ i + ac_val_offset ] * qscale_table[xy + 1], qscale);
			if ( debug2 ) DisplayOutput.debug.print( Integer.toHexString( block[ i<<3 ] )+ " " + Integer.toHexString(ac_val[ i + ac_val_offset ]) + " " );
                    }
                }
            } else {
                int xy= mb_x + (mb_y - 1)* mbWidth-1;

                /* top prediction */
                ac_val_offset -= 16 * blockWrap[n];

                if ( mb_y == 0 || qscale == qscale_table[ xy+1] || n==2 || n==3) {
                    /* same qscale */
                    for( i = 1; i < 8; i++ ) {
                        block[i] += ac_val[ i + 8 + ac_val_offset ];
			if (debug2) DisplayOutput.debug.print( Integer.toHexString( block[ i ] )+ " " );
                    }
                } else {
                    /* different qscale, we must rescale */
                    for ( i = 1; i < 8; i++ ) {
                        block[i] += ROUNDED_DIV(ac_val[i + 8 + ac_val_offset ]*qscale_table[xy+1], qscale);
			if (debug2 ) DisplayOutput.debug.print( Integer.toHexString( block[ i ] )+ " " );
                    }
                }
            }
        }
        /* left copy */
        for( i = 1; i < 8; i++ ) {
            ac_val[i + ac_val_offset1 ] = block[i<<3];
	    //DisplayOutput.debug.print( block[i<<3] + " " );
	}
        /* top copy */
        for(i=1;i<8;i++) {
            ac_val[8 + i + ac_val_offset1] = block[i];
	    //DisplayOutput.debug.print( block[i] + " " );
        }
	if (debug2 ) DisplayOutput.debug.println();
    }

    private VLCTable dc_lumTable = new dcLuminanceVlc();
    private VLCTable dc_chromTable = new dcChrominanceVlc();

    private int mpeg4_decode_dc( int n ) throws FFMpegException {
        int level = 0;

        int code;
        if ( n < 4 ) {
            code = in.getVLC( dc_lumTable );
        } else {
            code = in.getVLC( dc_chromTable );
        }
        if ( code != 0 ) {
            /* get_xbits() */
            level = in.getBits( code );
            if ( (level & (1<< (code-1))) == 0 ) {
                /* Negative (assume top bit) */
                level = (-1<<code) | (level) + 1;  
            }
            if ( code > 8 ) in.getTrueFalse();
        }
	if ( debug ) System.out.println( "l1 " + level );

        /* ff_mpeg4_pred_dc */
        int scale = (n<4)?y_dc_scale:c_dc_scale;
        int dc_offset = blockIndex[n];
        int wrap = blockWrap[n];

        int a = dc_val[ dc_offset - 1        ];
        int b = dc_val[ dc_offset - 1 - wrap ];
        int c = dc_val[ dc_offset     - wrap ];

        if ( first_slice_line && n != 3 ) {
            if ( n != 2 ) {
                b = 1024;
                c = 1024;
            }
            if ( n != 1 && mb_x == resync_mb_x ) {
                a = 1024;
                b = 1024;
            }
        }
        if ( mb_x == resync_mb_x && mb_y == (resync_mb_y + 1) ) {
            if ( n == 0 || n == 4 || n == 5 ) {
                b = 1024;
            }
        }
        if ( debug ) System.out.println( "offset " + (dc_offset-blockWrap[0]-1) + " a:" + a +" b:" + b + " c:" + c + " fsl " + (first_slice_line?1:0));

        int pred;
        if ( (a-b)*(a-b) < (b-c)*(b-c) ) {
            pred = c;
            dc_pred_dir = true; 
        } else {
            pred = a;
            dc_pred_dir = false; 
        }
        pred = (pred + (scale>>1))/scale;
	if ( debug ) System.out.println( "pred " + pred );
        /* end ff_mpeg4_pred_dc */

        level += pred;
        if ( level < 0 ) level = 0;

        dc_val[ dc_offset ] = level * ((n<4)?y_dc_scale:c_dc_scale);
        return level;
    }

    private RLTable rvlc_rl_intra = new RLRVlcRLIntra();
    private RLTable rl_intra      = new RLIntra();
    private RLTable rvlc_rl_inter = new RLRVlcRLInter();
    private RLTable rl_inter      = new RLInter();

    private boolean dc_pred_dir;
    private void mpeg4_decode_block( int[] block, int n, boolean coded, boolean intra ) throws FFMpegException {
        boolean rvlc = false;
        if ( debug ) System.out.println( "mpeg4_decode_block");
        int[] scan_table = intra_scantable;
        RLTable rl = null;
        int level;
        int i = 0;
        int qmul = 1;
        int qadd = 0;
        if ( intra ) {
            if(qscale < intra_dc_threshold){
                if ( partitioned_frame ) {
                    level = dc_val[ blockIndex[ n ] ];
                    if ( n < 4 ) {
		        level = (level + (y_dc_scale>>1))/y_dc_scale;
                    } else {
		        level = (level + (c_dc_scale>>1))/c_dc_scale;
                    }
                    dc_pred_dir = (0 != ((pred_dir_table[ mb_x + mb_y * mb_stride ] << n) & 32));
                } else {
                   level = mpeg4_decode_dc( n );
                }
                block[0] = level;
                i = 0;
                if ( debug ) System.out.println( "Level " + level );
            } else {
                i = -1;
            }

            if ( coded ) {
                rl = rvlc ? rvlc_rl_intra : rl_intra;

                if ( ac_pred ) {
                    if (!dc_pred_dir) 
                        scan_table = intra_v_scantable;
                    else
                        scan_table = intra_h_scantable;
                } else {
                    scan_table = intra_scantable;
                }
                qmul = 1;
                qadd = 0;
            }
        } else {
            if ( debug ) System.out.println( "Inter" );
            i = -1;
            if (!coded) {
                block_last_index[ n ] = i;
                return;
            }
            rl = rvlc ? rvlc_rl_inter : rl_inter;
   
            scan_table = intra_scantable;

            if(mpeg_quant) {
                qmul=1;
                qadd=0;
            }else{
                qmul = qscale << 1;
                qadd = (qscale - 1) | 1;
            }
        }

        /* Decode DCTELEM */
        if ( debug ) if ( coded ) System.out.println( "Code " + Integer.toHexString( in.showBits(24) ) );
        while ( coded ) {
            int code = in.getVLC( rl );
            level = rl.getLevel( code );
            int run   = rl.getRun( code );
            boolean last = false;
            if ( level == 0 ) {
                if ( rvlc ) {
                } else {
		    if ( in.getTrueFalse() ) {
                        if ( in.getTrueFalse() ) {
                            last = in.getTrueFalse();
                            run  = in.getBits(6);
                            in.getTrueFalse();
                            level = in.getBits(12); //TODO signed
                            if ( (level & (1<<11)) != 0 ) {
                                level |= (-1)<<11;
                            }
                            in.getTrueFalse();
                            level = level * qmul + ((level > 0)?qadd:-qadd);
                            i += run + 1;
                            if ( last ) i += 192;
                        } else {
                            code = in.getVLC( rl );
                            level = rl.getLevel( code );
                            run = rl.getRun( code );
                            i += run + rl.getMaxRun()[run>>7][level] + 1;
                            level = level * qmul + qadd;
                            if ( in.getTrueFalse() ) level = -level;
                        }
                    } else {
                        code = in.getVLC( rl );
                        run  = rl.getRun( code );
                        level = rl.getLevel( code ) * qmul + qadd;
                        i += run;
                        level = level + rl.getMaxLevel()[run>>7][(run-1)&63]*qmul;
                        if ( in.getTrueFalse() ) level = -level;
                    }
                }
            } else {
                i += run;
                level = level * qmul + qadd;
                if ( in.getTrueFalse() ) level = -level;
            }
//	    System.out.print( Integer.toHexString(level) + " " );
            if ( debug ) System.out.println( "Level " + level + " run " + run + " i " + i);
            if ( i > 62 ) {
                i -= 192;
                block[scan_table[i]] = level;
                break;
            }
            block[scan_table[i]] = level;
        }
        /* Add prediction */
        if ( mb_intra ) {
            if( qscale >= intra_dc_threshold ) {
                System.out.println( "TODO preddc" );
//                block[0] = ff_mpeg4_pred_dc(s, n, block[0], &dc_pred_dir, 0);
            
                if(i == -1) i=0;
            }

//            System.out.println( "predac" );
            mpeg4_pred_ac( block, n );
            if ( ac_pred ) i = 63;
        }
        block_last_index[ n ] = i;
//	System.out.println();
    }


    private int get_amv( int i ) throws FFMpegException {
        System.out.println( "get_amv " + i );
        return i;
    }

    private boolean mb_intra;
    private int mv_dir;
    private int mv_type;
    private boolean mcsel;
    private boolean mb_skipped;
    private int motion_pred_x;
    private int motion_pred_y;
    private boolean interlaced_dct;

    private void ff_mpeg4_decode_mb() throws FFMpegException {
        boolean mpeg4_decode_block_intra = false;
        int cbp = 0;
        int cbpc = 0;
        int dquant = 0;
        if ( debug ) System.out.println( "ff_mpeg4_decode_mb" );
        int xy = mb_x + mb_y * mbWidth;
        if ( pict_type == P_TYPE || pict_type == S_TYPE ) {
            if ( debug ) System.out.println( "P/S_TYPE" );
            do {
                if ( in.getTrueFalse() ) {
                    mb_intra = false;
                    for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                        block_last_index[i] = -1;
                    }
                    mv_dir = MV_DIR_FORWARD;
                    mv_type = MV_TYPE_16X16;
                    if ( pict_type == S_TYPE && vol_sprite_usage == GMC_SPRITE ) {
                        mb_type[xy] =MB_TYPE_SKIP | MB_TYPE_GMC | MB_TYPE_16x16 | MB_TYPE_L0;
                        mcsel = true;
                        mv[0][0][0] = get_amv(0);
                        mv[0][0][1] = get_amv(1);
                        mb_skipped = false;
                    } else {
                        mb_type[xy] =MB_TYPE_SKIP | MB_TYPE_16x16 | MB_TYPE_L0;
                        mcsel = false;
                        mv[0][0][0] = 0;
                        mv[0][0][1] = 0;
                        mb_skipped = true;
                    }
//System.out.println( "Go to end" );  //TODO mpeg4_resync
                    return;
    //                throw new Error( "Code goto end" );
                }
                cbpc = in.getVLC( inter_MCBPC );

//System.out.println( "CBPC: " + cbpc);
            } while ( cbpc == 20 );
            dquant = cbpc & 8;
            mb_intra = (cbpc & 4) != 0;

            if ( !mb_intra ) {   //REM goto intra:
                int mx = 0, my = 0;
//System.out.println( "No goto intra" );
                mcsel = false;
                if(    pict_type == S_TYPE 
                    && vol_sprite_usage == GMC_SPRITE 
                    && (cbpc & 16) == 0 ) {
                    mcsel = in.getTrueFalse();
                }
                int cbpy = in.getVLC( cbpyVlc )^0xf;

                if ( debug ) System.out.println( "cbpy " + cbpy );
                cbp = (cbpc & 3) | (cbpy << 2);
                if ( dquant != 0 ) {
                    int[] quant_tab = new int[] { -1, -2, 1, 2 };
                    ff_set_qscale( qscale + quant_tab[ in.getBits(2) ] );
                }
                if(!progressive_sequence && cbp != 0) {
                    boolean interlaced_dct= in.getTrueFalse();
                } 

                mv_dir = MV_DIR_FORWARD;
                if ((cbpc & 16) == 0) {
                    if(mcsel){
                        if ( debug ) System.out.println( "Motion 1" );
                        mb_type[xy]= MB_TYPE_GMC | MB_TYPE_16x16 | MB_TYPE_L0;

                        /* 16x16 global motion prediction */
                        mv_type = MV_TYPE_16X16;
                        mx= get_amv(0);
                        my= get_amv(1);
                        mv[0][0][0] = mx;
                        mv[0][0][1] = my;
                        if ( debug ) System.out.println( "mx: " + mx + " my:" + my );
                    } else if((!progressive_sequence) && in.getTrueFalse() ){
                        if ( debug ) System.out.println( "Motion 2" );
    
                        mb_type[xy]= MB_TYPE_16x8|MB_TYPE_L0|MB_TYPE_INTERLACED;

                        /* 16x8 field motion prediction */
                        mv_type= MV_TYPE_FIELD;

                        boolean[][] field_select = new boolean[ 2 ][ 2 ];
                        field_select[0][0] = in.getTrueFalse();
                        field_select[0][1] = in.getTrueFalse();

                        h263_pred_motion(0);
                
                        for(int i=0; i<2; i++){
                            mx = h263_decode_motion(h263_predMotionX, f_code);
                            my = h263_decode_motion(h263_predMotionY/2, f_code);
                            mv[0][i][0] = mx;
                            mv[0][i][1] = my;
                            if ( debug ) System.out.println( "mx: " + mx + " my:" + my );
                        }
                    } else {
                        if ( debug ) System.out.println( "Motion 3" );
                        mb_type[xy]= MB_TYPE_16x16 | MB_TYPE_L0; 
                        /* 16x16 motion prediction */
                        mv_type = MV_TYPE_16X16;
                        h263_pred_motion( 0 );
                        mx = h263_decode_motion(h263_predMotionX, f_code);
                        my = h263_decode_motion(h263_predMotionY, f_code);
            
                        mv[0][0][0] = mx;
                        mv[0][0][1] = my;
                        if ( debug ) System.out.println( "mx: " + mx + " my:" + my );
                    }
                } else {
                     if ( debug ) System.out.println( "Motion 4" );
                     mb_type[xy]= MB_TYPE_8x8 | MB_TYPE_L0; 
                     mv_type = MV_TYPE_8X8;
                     for( int i = 0; i < 4; i++ ) {
                         h263_pred_motion( i );
                         mx = h263_decode_motion(h263_predMotionX, f_code);
                         my = h263_decode_motion(h263_predMotionY, f_code);

                         mv[0][i][0] = mx;
                         mv[0][i][1] = my;

                         motion_val[ blockIndex[ i ] ][0] = mx;
                         motion_val[ blockIndex[ i ] ][1] = my;
 if ( debug ) System.out.println( "8X8 " + (blockIndex[i]-82) + ", mx: " + mx + " my:" + my );
                     }
                }
//System.out.println( "mx " + mx + " my " + my );
            }
        } else if ( pict_type == B_TYPE ) {
            /* BFrames in XVID only */
            if ( debug ) System.out.println( "B_TYPE" );
            mb_intra = false;

            int mx = 0;
            int my = 0;
            if( mb_x == 0 ) {
                for( int i=0; i < 2; i++ ) {
                    last_mv[i][0][0]= 0;
                    last_mv[i][0][1]= 0;
                    last_mv[i][1][0]= 0;
                    last_mv[i][1][1]= 0;
                }
            }

            /* if we skipped it in the future P Frame than skip it now too */
            if( mb_skiptable[ mb_x ][ mb_y ] ) {
                /* skip mb */
                 mv_dir = MV_DIR_FORWARD;
                 mv_type = MV_TYPE_16X16;
                 mv[0][0][0] = 0;
                 mv[0][0][1] = 0;
                 mv[1][0][0] = 0;
                 mv[1][0][1] = 0;
                 mb_type[xy]= MB_TYPE_SKIP | MB_TYPE_16x16 | MB_TYPE_L0; 
	         if ( debug ) System.out.println( "BSKIP" );
                 return;
            }


            boolean modb1 = in.getTrueFalse();
            if( modb1 ) {
                mb_type[xy] =  MB_TYPE_DIRECT2 | MB_TYPE_SKIP | MB_TYPE_L0L1;
                cbp=0;
            } else {
                boolean modb2 = in.getTrueFalse();
                mb_type[xy] = in.getVLC( mb_type_b );
                mb_type[xy] = mb_type_b_map[ mb_type[xy] ];
                if ( modb2 ) {
                    cbp= 0;
                } else {
                    cbp= in.getBits( 6 );
		}

                if ( ((IS_DIRECT_MASK & mb_type[xy]) == 0) && cbp != 0 ) {
                    if ( in.getTrueFalse() ) {
                        ff_set_qscale( qscale + in.getBits( 1 ) * 4 - 2);
                    }
                }

                if ( !progressive_sequence ) {
                    if ( cbp != 0 ) {
                        interlaced_dct= in.getTrueFalse();
                    }

                    if( ((IS_DIRECT_MASK & mb_type[xy]) == 0) && in.getTrueFalse() ) {
                        mb_type[xy] |= MB_TYPE_16x8 | MB_TYPE_INTERLACED;
                        mb_type[xy] &= ~MB_TYPE_16x16;

                        if(USES_LIST(mb_type[xy], 0)){
                            field_select[0][0]= in.getBits(1);
                            field_select[0][1]= in.getBits(1);
                        }
                        if(USES_LIST(mb_type[xy], 1)){
                            field_select[1][0]= in.getBits(1);
                            field_select[1][1]= in.getBits(1);
                        }
                    }
                }

                mv_dir = 0;
                if ( (mb_type[xy] & (MB_TYPE_DIRECT2|MB_TYPE_INTERLACED)) == 0 ) {
                    mv_type = MV_TYPE_16X16;

                    if ( USES_LIST(mb_type[xy], 0) ) {
                        mv_dir = MV_DIR_FORWARD;
if (debug) System.out.println( "BMotion 1" );
                        mx = h263_decode_motion( last_mv[0][0][0], f_code );
                        my = h263_decode_motion( last_mv[0][0][1], f_code );
                        last_mv[0][1][0] = mx;
                        last_mv[0][0][0] = mx;
                        mv[0][0][0]      = mx;

                        last_mv[0][1][1] = my;
       	                last_mv[0][0][1] = my;
                        mv[0][0][1]      = my;
                    }
    
                    if ( USES_LIST(mb_type[xy], 1)){
                        mv_dir |= MV_DIR_BACKWARD;

if (debug) System.out.println( "BMotion 2" );
                        mx = h263_decode_motion( last_mv[1][0][0], b_code );
                        my = h263_decode_motion( last_mv[1][0][1], b_code );
                        last_mv[1][1][0] = mx;
                        last_mv[1][0][0] = mx;
                        mv[1][0][0]      = mx;

                        last_mv[1][1][1] = my;
                        last_mv[1][0][1] = my;
                        mv[1][0][1]      = my;
                    }
                } else if( ((IS_DIRECT_MASK & mb_type[xy]) == 0) ) {
                    mv_type= MV_TYPE_FIELD;

                    if ( USES_LIST(mb_type[xy], 0) ) {
                        mv_dir = MV_DIR_FORWARD;
                
if (debug) System.out.println( "BMotion 3" );
                        for( int i = 0; i < 2; i++ ) {
                            mx = h263_decode_motion(last_mv[0][i][0],  f_code);
                            my = h263_decode_motion(last_mv[0][i][1]/2, f_code);
                            last_mv[0][i][0] = mx;
                            mv[0][i][0] = mx;
                            last_mv[0][i][1] = my * 2;
                            mv[0][i][1] = my;
                        }
                    }
    
                    if ( USES_LIST(mb_type[xy], 1 ) ) {
                        mv_dir |= MV_DIR_BACKWARD;

if (debug) System.out.println( "BMotion 4" );
                        for( int i = 0; i < 2; i++ ) {
                            mx = h263_decode_motion(last_mv[1][i][0], b_code);
                            my = h263_decode_motion(last_mv[1][i][1]/2, b_code);
                            last_mv[1][i][0] = mx;
                            mv[1][i][0] = mx;
                            last_mv[1][i][1] = my * 2;
                            mv[1][i][1] = my;
                        }
                    }
                }
            }
          
            if ( debug ) System.out.println( "mb_type " + mb_type[xy] );
            if( ((IS_DIRECT_MASK & mb_type[xy]) != 0) ){
                if( (IS_SKIP_MASK & mb_type[xy]) != 0 ) {
                    mx = 0;
                    my = 0;
                } else {
if (debug) System.out.println( "BMotion 5" );
                    mx = h263_decode_motion(0, 1);
                    my = h263_decode_motion(0, 1);
                }
 
                mv_dir = MV_DIR_FORWARD | MV_DIR_BACKWARD | MV_DIRECT;
                mb_type[xy] |= ff_mpeg4_set_direct_mv(mx, my);
            }
//System.out.println( "mx " + mx + " my " + my );
        } else {
            if ( debug ) System.out.println( "I_TYPE" );
            do {
                cbpc = in.getVLC( intra_MCBPC );
            } while ( cbpc == 8 );
            dquant = cbpc & 4;
            mb_intra = true;
        }

/* intra: */
        if ( mb_intra ) {
            ac_pred = in.getTrueFalse();

            mb_type[ xy ] = MB_TYPE_INTRA;
            if ( ac_pred ) {
                mb_type[ xy ] = MB_TYPE_INTRA | MB_TYPE_ACPRED;
            }
            if ( debug ) System.out.println( "cbpc " + cbpc );

            int cbpy = in.getVLC( cbpyVlc );

            if ( debug ) System.out.println( "cbpy " + cbpy );

            cbp = (cbpc & 3) | (cbpy << 2);
            if ( dquant != 0 ) {
                ff_set_qscale( qscale + quant_tab[ in.getBits(2) ] );
            }
            if(!progressive_sequence) {
                interlaced_dct= in.getTrueFalse();
            } 
            mpeg4_decode_block_intra = true;
        }

        for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
            mpeg4_decode_block(block[i], i, (cbp & 32) != 0, mpeg4_decode_block_intra);
            cbp <<= 1;
        }
        /* End of slice */
    }
                int[] quant_tab = new int[] { -1, -2, 1, 2 };

    private int ff_mpeg4_set_direct_mv( int mx, int my ) {
        int time_pb = pb_time;
        int time_pp = pp_time;
        int mb_index = mb_x + mb_y * mbWidth;
        int colocated_mb_type = mb_type_ip_frame[mb_index];
        int xy = blockIndex[0];

	if ( debug) System.out.println( "colocated_mb_type " + colocated_mb_type );
        if ( debug ) System.out.println( "set_direct" );
    
        next_motion_val = motion_val;  /* This works as this function is only called in BFrames */
        if( (IS_8X8_MASK & colocated_mb_type) != 0 ) {
            mv_type = MV_TYPE_8X8;
            for( int i = 0; i < 4; i++ ) {
                xy = blockIndex[i];
                mv[0][i][0] = (next_motion_val[xy][0] * time_pb)/time_pp + mx;
                mv[0][i][1] = (next_motion_val[xy][1] * time_pb)/time_pp + my;
                mv[1][i][0] = (mx == 0) ? (mv[0][i][0] - next_motion_val[xy][0])
                                        : next_motion_val[xy][0]* (time_pb - time_pp) / time_pp;
                mv[1][i][1] = (my == 0) ? (mv[0][i][1] - next_motion_val[xy][1])
                                        : next_motion_val[xy][1]* (time_pb - time_pp) / time_pp;
            }
            return MB_TYPE_DIRECT2 | MB_TYPE_8x8 | MB_TYPE_L0L1;
        } else if ( (IS_INTERLACED_MASK & colocated_mb_type) != 0 ) {
/* TODO Code me *
            mv_type = MV_TYPE_FIELD;
            for ( int i = 0; i < 2; i++ ) {
                int field_select= s->next_picture.ref_index[0][s->block_index[2*i]];
            if(s->top_field_first){
                time_pp= s->pp_field_time - field_select + i;
                time_pb= s->pb_field_time - field_select + i;
            }else{
                time_pp= s->pp_field_time + field_select - i;
                time_pb= s->pb_field_time + field_select - i;
            }
            s->mv[0][i][0] = s->p_field_mv_table[i][0][mb_index][0]*time_pb/time_pp + mx;
            s->mv[0][i][1] = s->p_field_mv_table[i][0][mb_index][1]*time_pb/time_pp + my;
            s->mv[1][i][0] = mx ? s->mv[0][i][0] - s->p_field_mv_table[i][0][mb_index][0]
                                : s->p_field_mv_table[i][0][mb_index][0]*(time_pb - time_pp)/time_pp;
            s->mv[1][i][1] = my ? s->mv[0][i][1] - s->p_field_mv_table[i][0][mb_index][1] 
                                : s->p_field_mv_table[i][0][mb_index][1]*(time_pb - time_pp)/time_pp;
            }
*/
            System.out.println( "TODO:  IS_INTERLACED" );
            return MB_TYPE_DIRECT2 | MB_TYPE_16x8 | MB_TYPE_L0L1 | MB_TYPE_INTERLACED;
        } else {
            int t = (next_motion_val[xy][0] * time_pb)/time_pp + mx;
            mv[0][0][0] = t;
            mv[0][1][0] = t;
            mv[0][2][0] = t;
            mv[0][3][0] = t;

            t = (next_motion_val[xy][1] * time_pb)/time_pp + my;
            mv[0][0][1] = t;
            mv[0][1][1] = t;
            mv[0][2][1] = t;
            mv[0][3][1] = t;

            t = (mx != 0) ? mv[0][0][0] - next_motion_val[xy][0]
                          : next_motion_val[xy][0]*(time_pb - time_pp)/time_pp;
            mv[1][0][0] = t;
            mv[1][1][0] = t;
            mv[1][2][0] = t;
            mv[1][3][0] = t;

            t = (my != 0) ? mv[0][0][1] - next_motion_val[xy][1] 
                          : next_motion_val[xy][1] * (time_pb - time_pp)/time_pp;
            mv[1][0][1] = t;
            mv[1][1][1] = t;
            mv[1][2][1] = t;
            mv[1][3][1] = t;
/*System.out.println( "direct " + mv[0][0][0] + " " + mv[0][0][1] + " "
                              + mv[1][0][0] + " " + mv[1][0][1] + " " + next_motion_val[xy][0] + " "
                              + mx + " " + my + " " + time_pb + " " + time_pp  );
*/
            if( !quarter_sample ) {
                mv_type= MV_TYPE_16X16;
            } else {
                mv_type= MV_TYPE_8X8;
            }
            return MB_TYPE_DIRECT2 | MB_TYPE_16x16 | MB_TYPE_L0L1; //Note see prev line
        }
    }

    /**
     * set qscale and update qscale dependant variables.
     */
    private void ff_set_qscale(int qscale) {
        if (qscale < 1)
            qscale = 1;
        else if (qscale > 31)
            qscale = 31;
        
        this.qscale = qscale;
        int chroma_qscale = chroma_qscale_table[qscale];

        y_dc_scale= ff_mpeg4_y_dc_scale_table[ qscale ];
        c_dc_scale= ff_mpeg4_c_dc_scale_table[ chroma_qscale ];
    }

    private int[] chroma_qscale_table = ff_default_chroma_qscale_table;

    private static int[] ff_default_chroma_qscale_table = new int[] {
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
    };


    private VLCTable inter_MCBPC = new InterMcbpc();
    private VLCTable intra_MCBPC = new IntraMcbpc();
    private VLCTable cbpyVlc = new CbpyVlc();

    private static final int IS_INTRA_MASK  = 7;
    private static final int IS_DIRECT_MASK = 0x100;
    private static final int IS_SKIP_MASK = 0x800;
    private static final int IS_8X8_MASK  = 0x40;
    private static final int IS_INTERLACED_MASK = 0x80;

    private static final int MB_TYPE_DIRECT2 = 0x100; 
    private static final int MB_TYPE_ACPRED = 0x200; 
    private static final int MB_TYPE_GMC    = 0x400;
    private static final int MB_TYPE_SKIP   = 0x800;
    private static final int MB_TYPE_INTRA  = 0x1;
    private static final int MB_TYPE_INTERLACED   = 0x80;
    private static final int MB_TYPE_8x8    = 0x40;
    private static final int MB_TYPE_16x8    = 0x10;
    private static final int MB_TYPE_16x16  = 0x8;
    private static final int MB_TYPE_P0L0   = 0x1000;
    private static final int MB_TYPE_P1L0   = 0x2000;
    private static final int MB_TYPE_P0L1   = 0x4000;
    private static final int MB_TYPE_P1L1   = 0x8000;
    private static final int MB_TYPE_L0     = MB_TYPE_P0L0|MB_TYPE_P1L0;
    private static final int MB_TYPE_L1     = MB_TYPE_P0L1 | MB_TYPE_P1L1;
    private static final int MB_TYPE_L0L1   = MB_TYPE_L0   | MB_TYPE_L1;

    private static final int MV_TYPE_FIELD  = 3;
    private static final int MV_DIR_FORWARD = 2;
    private static final int MV_DIR_BACKWARD = 1;
    private static final int MV_DIRECT       = 4;
    private static final int MV_TYPE_8X8    = 1;
    private static final int MV_TYPE_16X16  = 0;

    private static final boolean USES_LIST( int a, int list) {
        return ((a) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0;
        ///< does this mb use listX, note doesnt work if subMBs
    }


    /* XVID only */
    private VLCTable mb_type_b = new MbTypeBTable();
    private static final int[] mb_type_b_map = new int[] {
        MB_TYPE_DIRECT2 | MB_TYPE_L0L1,
        MB_TYPE_L0L1 | MB_TYPE_16x16,
        MB_TYPE_L1 | MB_TYPE_16x16,
        MB_TYPE_L0 | MB_TYPE_16x16,
    };



    private int[][][] mv = new int[ 2 ][ 4 ][ 2 ];
    private int[][][] last_mv = new int[ 2 ][ 4 ][ 2 ];

    private boolean ac_pred;

    private void mpeg4_decode_partitioned_mb() throws FFMpegException {
        if ( debug ) System.out.println( "mpeg4_decode_partitioned_mb" );
        int mv_dir, mv_type;

        int xy = mb_x + mb_y * mbWidth;
        int mb_type = this.mb_type[ xy ];
        int cbp = cbp_table[ xy ];
        if ( qscale != qscale_table[ xy ] ) {
            ff_set_qscale( qscale_table[ xy ] );
        }
        if ( pict_type == P_TYPE || pict_type == S_TYPE ) {
            for ( int i = 0; i < 4; i++ ) {
                mv[0][i][0] = motion_val[ blockIndex[i] ][0];
                mv[0][i][1] = motion_val[ blockIndex[i] ][1];
            }
            mb_intra = (IS_INTRA_MASK & mb_type) != 0;
            boolean isSkip = (MB_TYPE_SKIP & mb_type) != 0;
            if ( isSkip ) {
                for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                    block_last_index[i] = -1;
                }
                mv_dir  = MV_DIR_FORWARD;
                mv_type = MV_TYPE_16X16;

                boolean mcsel = false;
                mb_skipped = true; 
                if ( pict_type == S_TYPE && vol_sprite_usage == GMC_SPRITE ) {
                    mcsel = true;
                    mb_skipped = false;
                }
	    } else if ( mb_intra ) {
                ac_pred = (MB_TYPE_ACPRED & mb_type)!= 0;
                /* Not relevant */
                mv_type = MV_TYPE_16X16;
            } else if ( !mb_intra ) {
                mv_dir = MV_DIR_FORWARD;
                mv_type = MV_TYPE_16X16;
                if ( (MB_TYPE_8x8 & mb_type) != 0 ) {
                    mv_type = MV_TYPE_8X8;
		    if ( debug ) System.out.println( "MV_TYPE_8X8" );
                }
            }
        } else {
            /* IFRAME */
            mb_intra = true;
            ac_pred = (MB_TYPE_ACPRED & mb_type) != 0;
        }

        boolean isSkip = (MB_TYPE_SKIP & mb_type) != 0;
        if ( !isSkip ) {
            for ( int i = 0; i < 6; i++ ) {
//              mpeg4_decode_block( block[i], i, cbp & 32, mb_intra, rvlc );
                cbp <<= 1;
            }
        }

        /* TODO Slice end prediction */
    }
 
    private int[] ff_mpeg4_y_dc_scale_table = new int[] {
        0, 8, 8, 8, 8,10,12,14,16,17,18,19,20,
        21,22,23,24,25,26,27,28,29,30,31,32,34,36,38,40,42,44,46
    };

    private int[] ff_mpeg4_c_dc_scale_table = new int[] {
        0, 8, 8, 8, 8, 9, 9,10,10,11,11,12,12,
        13,13,14,14,15,15,16,16,17,17,18,18,19,20,21,22,23,24,25
    };


    private static final int NUMBER_OF_BLOCKS = 6;
    private int[] blockWrap  = new int[ NUMBER_OF_BLOCKS ];
    private int[] blockIndex = new int[ NUMBER_OF_BLOCKS ];
    private int[][] block    = new int[ NUMBER_OF_BLOCKS ][ 64 ];
    private int[] block_last_index = new int[ NUMBER_OF_BLOCKS ];
    private int[] blank = new int[ 64 ];
    private int y_dc_scale;
    private int c_dc_scale;
    private boolean first_slice_line;
    private int resync_mb_x;
    private int resync_mb_y;

    private void decode_slice() throws FFMpegException {
        int last_resync_gb = in.getPos();
        first_slice_line = true;
        resync_mb_x = mb_x;
        resync_mb_y = mb_y;

        /* TODO chose tables dynamically? */
        ff_set_qscale( qscale );
        if ( debug ) System.out.println( "y_dc_scale " + y_dc_scale );
        if ( debug ) System.out.println( "c_dc_scale " + c_dc_scale );

        for ( ; mb_y < mbHeight; mb_y++ ) {
            /**
             * Initialise the quick lookup arrays
             */
            blockIndex[ 0 ] =   blockWrap[ 0 ] * ( mb_y * 2 + 1 ) - 1;
            blockIndex[ 1 ] =   blockIndex[ 0 ] + 1;
            blockIndex[ 2 ] =   blockIndex[ 0 ] + blockWrap[ 0 ];
            blockIndex[ 3 ] =   blockIndex[ 2 ] + 1;
            blockIndex[ 4 ] =   blockWrap[ 0 ] * ( mbHeight * 2 + 1)
                              + blockWrap[ 4 ] * ( mb_y + 1 );
            blockIndex[ 5 ] =   blockIndex[ 4 ] + blockWrap[ 4 ] * (mbHeight + 1);
            for ( ; mb_x < mbWidth; mb_x++ ) {
                if ( debug ) System.out.println( "XY" + mb_x + "," + mb_y );
                /**
                 * Increment block index
                 */
                blockIndex[ 0 ] += 2;
                blockIndex[ 1 ] += 2;
                blockIndex[ 2 ] += 2;
                blockIndex[ 3 ] += 2;
                blockIndex[ 4 ]++;
                blockIndex[ 5 ]++;

                if (    resync_mb_x == mb_x
       	             && resync_mb_y == mb_y - 1 ) first_slice_line = false;

                /**
                 * Clear blocks
                 */
                for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                    System.arraycopy( blank, 0, block[i], 0, blank.length );
                }
                int mv_dir = 0;  //MV_DIR_FORWARD
                int mv_type = 0; //MV_TYPE_16X16
                if ( decode_mb == 0 ) {
                    //ff_h263_decode_mb
                    ff_mpeg4_decode_mb();
//                    throw new FFMpegException( "codeme" );
                } else {
                    mpeg4_decode_partitioned_mb();
                }
                MPV_decode_mb( mb_x, mb_y );
            }
            mb_x = 0;
        }
        if ( debug ) System.out.println( "End of Screen" );
    }

    private int vo_type;
    private int vo_ver_id;
    private int shape;
    private static int FF_ASPECT_EXTENDED = 15;

    private static final int RECT_SHAPE = 0;
    private static final int BIN_ONLY_SHAPE = 2;
    private static final int GRAY_SHAPE = 3;

    private static final int STATIC_SPRITE = 1;
    private static final int GMC_SPRITE = 2;

    private int[] intra_matrix = new int[ 64 ];
    private int[] chroma_intra_matrix = new int[ 64 ];
    private int[] inter_matrix = new int[ 64 ];
    private int[] chroma_inter_matrix = new int[ 64 ];

    private boolean data_partitioning;
    private int time_increment_resolution;
    private int time_increment_bits;
    private boolean vol_control_parameters;
    private int quant_precision;
    private int qscale;
    private boolean mpeg_quant;

    private void decode_vol_header() throws FFMpegException {
        if ( debug ) System.out.println( "decode_vol_header" );
        in.getTrueFalse();
        vo_type = in.getBits( 8 );
        vo_ver_id = 1;
        if ( in.getTrueFalse() ) {
            vo_ver_id = in.getBits( 4 );
            in.getBits( 3 );
        }
        int aspect_ratio_info = in.getBits( 4 );
        if ( aspect_ratio_info == FF_ASPECT_EXTENDED ) {
            int aspected_width  = in.getBits(8);
            int aspected_height = in.getBits(8);
        } else {
            /* TODO lookup table */
        }
        vol_control_parameters = in.getTrueFalse();
        if ( vol_control_parameters ) {
            int chroma_format = in.getBits( 2 );
            if ( chroma_format != 1 ) throw new FFMpegException( "Illegal Chroma" );
            boolean low_delay = in.getTrueFalse();
            if ( in.getTrueFalse() ) {
                in.getBits( 15 );
                in.getTrueFalse();
                in.getBits( 15 );
                in.getTrueFalse();
                in.getBits( 15 );
                in.getTrueFalse();
                in.getBits( 3 );
                in.getBits( 11 );
                in.getTrueFalse();
                in.getBits( 15 );
                in.getTrueFalse();
            }
        } else {
           int low_delay = 0;
        }

        shape = in.getBits( 2 );
        if ( shape != RECT_SHAPE ) if ( debug ) System.out.println( "Not RECT_SHAPE" );
        if ( shape == GRAY_SHAPE ) {
            in.getBits(4);
        }
if ( debug ) System.out.println( "shape: " + shape );
        in.getTrueFalse();
        time_increment_resolution = in.getBits(16);
if ( debug ) System.out.println( "time_increment_resolution: " + time_increment_resolution );
        time_increment_bits = av_log2( time_increment_resolution - 1 );
if ( debug ) System.out.println( "time_increment_bits: " + time_increment_bits );
        if ( time_increment_bits < 1 ) time_increment_bits = 1;
        in.getTrueFalse();
        if ( in.getTrueFalse() ) in.getBits( time_increment_bits );
        if ( shape != BIN_ONLY_SHAPE ) {
            if ( shape == RECT_SHAPE ) {
                in.getTrueFalse();
                int w = in.getBits(13);
                in.getTrueFalse();
                int h = in.getBits(13);
                in.getTrueFalse();
                if ( w != 0 && h != 0 ) {
                    width = w;
                    height = h;
                }
            }
            progressive_sequence = !in.getTrueFalse();
            in.getTrueFalse();
            vol_sprite_usage = in.getBits( vo_ver_id == 1 ? 1:2 );
            if (    vol_sprite_usage == STATIC_SPRITE
                 || vol_sprite_usage == GMC_SPRITE ) {
                if ( vol_sprite_usage == STATIC_SPRITE ) {
                    int sprite_width = in.getBits(13);
                    in.getTrueFalse();
                    int sprite_height = in.getBits(13);
                    in.getTrueFalse();
                    int sprite_left = in.getBits(13);
                    in.getTrueFalse();
                    int sprite_top = in.getBits(13);
                    in.getTrueFalse();
                }
                num_sprite_warping_points = in.getBits(6);
                sprite_warping_accuracy = in.getBits(2);
                boolean brightness_change = in.getTrueFalse();
                if ( vol_sprite_usage == STATIC_SPRITE ) {
                    boolean low_latency_sprite = in.getTrueFalse();
                }
            }
            quant_precision = 5;
            if ( in.getTrueFalse() ) {
                quant_precision = in.getBits(4);
                in.getBits(4);
            }
if ( debug ) System.out.println( "quant_precision: " + quant_precision );

            mpeg_quant = in.getTrueFalse();
            if ( mpeg_quant ) {
                /* Default matricies */
                for ( int i = 0; i < 64; i++ ) {
                    intra_matrix[i] = Tables.ff_mpeg4_default_intra_matrix[i];
                    chroma_intra_matrix[i] = Tables.ff_mpeg4_default_intra_matrix[i];
                    inter_matrix[i] = Tables.ff_mpeg4_default_non_intra_matrix[i];
                    chroma_inter_matrix[i] = Tables.ff_mpeg4_default_non_intra_matrix[i];
                }
                if ( in.getTrueFalse() ) {
                    int last = 0;
                    int i = 0;
                    for ( ; i < 64; i++ ) {
                        int v = in.getBits( 8 );
                        if ( v == 0 ) break;
                        last = v;
                        intra_matrix[ i ] = v;
                        chroma_intra_matrix[i ] = v;
                    }
                    for ( ; i < 64; i++ ) {
                        intra_matrix[ i ] = last;
                        chroma_intra_matrix[i ] = last;
                    }
                }
                if ( in.getTrueFalse() ) {
                    int last = 0;
                    int i = 0;
                    for ( ; i < 64; i++ ) {
                        int v = in.getBits( 8 );
                        if ( v == 0 ) break;
                        last = v;
                        inter_matrix[ i ] = v;
                        chroma_inter_matrix[i ] = v;
                    }
                    for ( ; i < 64; i++ ) {
                        inter_matrix[ i ] = last;
                        chroma_inter_matrix[i ] = last;
                    }
                }
            }

            quarter_sample = false;
            if ( vo_ver_id != 1) quarter_sample = in.getTrueFalse();
            in.getTrueFalse();
            resync_marker = !in.getTrueFalse();
            data_partitioning = in.getTrueFalse();
            if ( data_partitioning ) { 
                boolean rvlc = in.getTrueFalse();
            }
            boolean new_pred = false;
            boolean reduced_res_vop = false;
            if ( vo_ver_id != 1 ) {
                new_pred = in.getTrueFalse();
                if (new_pred) {
                   in.getBits(3);  //Not supported
                }
                reduced_res_vop = in.getTrueFalse();
            }
            scalability = in.getTrueFalse();
            if ( scalability ) {
                boolean hierarchy_type = in.getTrueFalse();
                int ref_layer_id = in.getBits(4);
                boolean ref_layer_sampling_dir = in.getTrueFalse();
                int h_sampling_factor_n = in.getBits(5);
                int h_sampling_factor_m = in.getBits(5);
                int v_sampling_factor_n = in.getBits(5);
                int v_sampling_factor_m = in.getBits(5);
                enhancement_type = in.getTrueFalse();
            }
        }
        if ( debug ) System.out.println( "decode_vol_header " + Integer.toHexString( in.showBits( 24 ) ) );
    }

    private static final int av_log2( int a ) {
	int n = 0;
        while ( a != 0 ) {
            a = (a >> 1) &0xffff;
            n++;
        }
        return n;
    }

    private void decode_user_data() throws FFMpegException {
        char[] buffer = new char[ 256 ];
        int i;
        buffer[0] = (char)in.getBits(8);
        for ( i = 1; i < 256; i++ ) {
            buffer[i] = (char)in.showBits(8);
            if ( buffer[i] == 0 ) break;
            in.getBits(8);
        }
        String version = new String( buffer, 0, i );
        if ( debug ) System.out.println( "User data: " + version );
    }

    private void mpeg4_decode_gop_header() throws FFMpegException {
        int hours = in.getBits(5);
        int minutes = in.getBits(6);
        in.getTrueFalse();
        int seconds = in.getBits(6);
        in.getTrueFalse();
        in.getTrueFalse();
        if ( debug ) System.out.println( "" + hours + ":" + minutes + ":" + seconds );
    }

    private static int I_TYPE = 1;
    private static int P_TYPE = 2;
    private static int B_TYPE = 3;
    private static int S_TYPE = 4;
    private boolean progressive_sequence;
    private boolean alternate_scan;

    private int vol_sprite_usage;
    private boolean scalability;
    private boolean enhancement_type;
    private boolean top_field_first;
    private boolean quarter_sample;
    private boolean resync_marker;
    private int num_sprite_warping_points;
    private int sprite_warping_accuracy;
    private int intra_dc_threshold = 0;
    private int decode_mb;
    private int pict_type;
    private boolean partitioned_frame;
    private boolean no_rounding;
    private int f_code;
    private int b_code;
    private int[][] field_select = new int[2][2];

    private int time_base;
    private int last_time_base;
    private int last_non_b_time;
    private int pp_time;
    private int pb_time;

    private int[] inter_scantable;
    private int[] intra_scantable;
    private int[] intra_h_scantable;
    private int[] intra_v_scantable;

    private static final int[] ff_zigzag_direct = ScanTable.getZigZagDirectTable();
    private static final int[] ff_alternate_horizontal_scan = ScanTable.getAlternativeHScanTable();
    private static final int[] ff_alternate_vertical_scan = ScanTable.getAlternativeVScanTable();

    private static final int[] mpeg4_dc_threshold = new int[] {
        99, 13, 15, 17, 19, 21, 23, 0
    };

    
    /* Returns true for full header */
    private boolean decode_vop_header() throws FFMpegException {
        if ( debug ) System.out.println( "decode_vop_header " + Integer.toHexString( in.showBits( 24 ) ) );
        pict_type = in.getBits(2) + I_TYPE;
        if ( debug ) System.out.println( "pict_type " + pict_type );
        partitioned_frame = data_partitioning & (pict_type != B_TYPE);
        decode_mb = partitioned_frame ? 1:0;
        if (time_increment_resolution == 0) {
            time_increment_resolution = 1;
        }
        int time_incr = 0;
        while (in.getTrueFalse()) time_incr++;
        if ( !in.getTrueFalse() ) throw new FFMpegException( "before_time_incr" );
	if ( debug ) System.out.println( "time_incr " + time_incr );
        int time_increment = in.getBits( time_increment_bits );
        if ( pict_type != B_TYPE ) {
            last_time_base = time_base;
            time_base += time_incr;
            int time = time_base * time_increment_resolution + time_increment;
            pp_time = time - last_non_b_time;
//System.out.println( "pp_time " + pp_time + " " + time_base + " " + time_increment_resolution + " " + time_increment );
            last_non_b_time = time;
        } else {
            /* TODO Time stuff */
            int time= (last_time_base + time_incr)*time_increment_resolution + time_increment;
            pb_time = pp_time - (last_non_b_time - time);        
        }
        if ( debug ) System.out.println( "time_increment_bits " + time_increment_bits );
        if ( debug ) System.out.println( "time_increment " + time_increment );

        if ( !in.getTrueFalse() ) throw new FFMpegException( "before_vop_coded" );
        if ( !in.getTrueFalse() ) return false;
        no_rounding = false;
        if (shape != BIN_ONLY_SHAPE
            && ( pict_type == P_TYPE || (pict_type == S_TYPE && vol_sprite_usage==GMC_SPRITE ) ) ) {
	    no_rounding = in.getTrueFalse();
        } 
        if ( shape != RECT_SHAPE ) {
            if ( vol_sprite_usage != 1 || pict_type != I_TYPE ) {
                int width = in.getBits(13);
                in.getTrueFalse();
                int height = in.getBits(13);
                in.getTrueFalse();
                int hor_spat_ref = in.getBits(13);
                in.getTrueFalse();
                int ver_spat_ref = in.getBits(13);
            }
            in.getTrueFalse();
            if( in.getTrueFalse() ) {
                in.getBits(8);
            }
        }

        if ( shape != BIN_ONLY_SHAPE ) {
            int t = in.getBits(3);
            intra_dc_threshold = mpeg4_dc_threshold[ t ];
            alternate_scan = false;
            if ( !progressive_sequence ) {
                top_field_first = in.getTrueFalse();
                alternate_scan = in.getTrueFalse();
            }
        }

        if ( alternate_scan ) {
            /* inter_scantable */
            inter_scantable   = ff_alternate_vertical_scan;
            intra_scantable   = ff_alternate_vertical_scan;
            intra_h_scantable = ff_alternate_vertical_scan;
            intra_v_scantable = ff_alternate_vertical_scan;
        } else {
            inter_scantable   = ff_zigzag_direct;
            intra_scantable   = ff_zigzag_direct;
            intra_h_scantable = ff_alternate_horizontal_scan;
            intra_v_scantable = ff_alternate_vertical_scan;
        }

        if (    pict_type == S_TYPE 
            && (vol_sprite_usage == STATIC_SPRITE || vol_sprite_usage==GMC_SPRITE) ) {
//            mpeg4_decode_sprite_trajectory();
            throw new FFMpegException( "mpeg4_decode_sprite_trajectory" );
        }

        if ( debug ) System.out.println( "Before qscale " + Integer.toHexString( in.showBits( 24 ) ) );
        if ( shape != BIN_ONLY_SHAPE ) {
            if ( debug ) System.out.println( "quant_precision " + quant_precision );
            qscale = in.getBits( quant_precision );
            int choma_qscale = qscale;

            f_code = 1;
	    if ( pict_type != I_TYPE ) {
                f_code = in.getBits(3);
            }

            b_code = 1;
            if ( pict_type == B_TYPE ) {
                b_code = in.getBits(3);
            }


            if ( debug ) System.out.println( "qp:" + qscale + 
                            " fc:" + f_code + "," + b_code +
                            " " + (pict_type == I_TYPE ? "I" : (pict_type == P_TYPE ? "P" : (pict_type == B_TYPE ? "B" : "S"))) +
/*			    " size:" + in.availableBits()  + */
			    " pro:" + (progressive_sequence?1:0) +
			    " alt:" + (alternate_scan?1:0) +
			    " top:" + (top_field_first?1:0) +
                            " " + (quarter_sample?"q":"h") + "pel" +
                            " part:" + (data_partitioning?1:0) + 
                            " resync:" + (resync_marker?1:0) +
                            " w:" + num_sprite_warping_points +
                            " a:" + sprite_warping_accuracy +
                            " rnd:" + (no_rounding?0:1) +
                            " vot:" + vo_type + (vol_control_parameters?" VOLC" :" ") +
		            " dc:" + intra_dc_threshold );

            if ( !scalability ) {
                if ( shape != RECT_SHAPE && pict_type != I_TYPE ) {
                    in.getTrueFalse();
                }
            } else {
                if ( enhancement_type ) {
                    boolean load_backward_shape = in.getTrueFalse();
                    if ( load_backward_shape ) throw new FFMpegException( "backward" );
                }
                in.getBits(2);
            }
        }

        /* y_dc_scale_table */
        return true;
    }


    public static final int VOL_HEADER = 0x120;
    public static final int USER_DATA_STARTCODE = 0x1b2;
    public static final int GOP_STARTCODE = 0x1b3;
    public static final int VOP_STARTCODE = 0x1b6;

    private void ff_mpeg4_decode_picture_header() throws FFMpegException {
        /** Align */
        if ( debug ) System.out.println( "ff_mpeg4_decode_picture_header" );
        in.seek( in.getPos() - ( in.getPos() & 0x7 ) );

        long startCode = 0xff;
        while ( in.availableBits() >= 8 ) {
            startCode = ((startCode << 8) | in.getBits(8)) & 0xffffffff;
            if ( (int)(startCode & 0xffffff00) != 0x100 ) continue;

            if ( debug ) System.out.println( "Start code " + Integer.toHexString((int)startCode & 0x1ff) );
            switch( (int)(startCode & 0x1ff) ) {
                case VOL_HEADER: {
                    decode_vol_header();
                    break;
                }
                case USER_DATA_STARTCODE: {
                    decode_user_data();
                    break;
                }
                case GOP_STARTCODE: {
                    mpeg4_decode_gop_header();
                    break;
                }
                case VOP_STARTCODE: {
                    if ( !decode_vop_header() ) {
                        break;
                    }
                    if ( debug ) System.out.println( "leaving decode_header" );
                    return;
                }
                default: {
                    break;
                }
	    }
            startCode = 0xff;
            in.seek( in.getPos() - ( in.getPos() & 0x7 ) );
        }
    }

    private int mb_x;
    private int mb_y;
    private int mb_stride;

    /**
     * Decode a frame
     */
    protected void decodeFrame( Buffer out ) throws FFMpegException {
        {
            ff_mpeg4_decode_picture_header();
	    if ( pict_type == B_TYPE ) {
                mb_type = mb_type_b_frame;
		if ( debug2 ) DisplayOutput.debug.println( "Picture (B)" );
            } else {
                mb_type = mb_type_ip_frame;
if ( debug2 ) DisplayOutput.debug.println( "Picture (" + (pict_type==I_TYPE?"I":"P") + ")" );
            }

            mb_x = 0;
            mb_y = 0;
            do {
                decode_slice();
            } while ( mb_y < mbHeight );
/*            if ( pict_type != B_TYPE ) {
                displayOutput.endFrame();
                displayOutput.showScreen(out);
            } else {
                displayOutput.endBFrame();
                displayOutput.showScreen(out);
            }
*/
            displayOutput.expandIntoBoarder();
            if ( pict_type != B_TYPE ) {
                displayOutput.showNextScreen(out);
                displayOutput.endIPFrame();
            } else {
                displayOutput.endBFrame();
                displayOutput.showScreen(out);
            }
        }
    }

    /**
     * Converts a byte array Buffer of DIVX video data
     * into an integer array Buffer of video data.
     * 
     * Note: The DIVX codec requires that the input buffer represents
     * exactly one frame
     *
     * @return BUFFER_PROCESSED_OK The output buffer contains a valid frame
     * @return BUFFER_PROCESSED_FAILED A decoding problem was encountered
     */
    public int process( Buffer in, Buffer out ) {
        try {
            /* Extract the input data */
            byte[] data = (byte[])in.getData();
            this.in.addData( data, in.getOffset(), in.getLength() );

            /* Decode and create an output image */
            decodeFrame( out );

            out.setTimeStamp( in.getTimeStamp() );
            out.setFlags( in.getFlags() );
        } catch ( Throwable e ) {
            System.out.println( e );
            e.printStackTrace();
            return BUFFER_PROCESSED_OK;
        }
        return BUFFER_PROCESSED_OK;
    }
    
    /**
     * Initialise the video codec for use.
     */
    public void open() {
    }
    
    /**
     * Deallocate resources, and shutdown.
     */
    public void close() {
    }
    
    /**
     * Reset the internal state of the video codec.
     */
    public void reset() {
    }
    
    /**
     * Retrives the name of this video codec: "DIVX video decoder"
     * @return Codec name
     */
    public String getName() {
        return "DIVX video decoder";
    }
    
    /**
     * This method returns the interfaces that can be used
     * to control this codec.  Currently no interfaces are defined.
     */
    public Object[] getControls() {
        return new Object[ 0 ];
    }

    /**
     * This method returns an interface that can be used
     * to control this codec.  Currently no interfaces are defined.
     */
    public Object getControl( String type ) {
        return null;
    }

    /**
     * Implement the Jffmpeg codec interface
     */
    public boolean isCodecAvailable() {
        return true;
    }

    /**
     * Outofbands video size
     */
    public void setVideoSize( Dimension size ) {
	setInputFormat( new VideoFormat( "DIVX", size, -1, (new byte[ 0 ]).getClass(), 0 ) );
    }

    public void setEncoding( String encoding ) {
    }

    public void setIsRtp( boolean isRtp ) {
    }

    public void setIsTruncated( boolean isTruncated ) {
    }
}
