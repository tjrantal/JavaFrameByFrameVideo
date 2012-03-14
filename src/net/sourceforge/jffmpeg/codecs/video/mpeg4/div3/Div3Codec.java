/*
 * Java port of ffmpeg DIV3 decoder.
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
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3;

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
import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.yuvtables.*;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.rltables.*;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.tables.*;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.motiontables.*;
import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.mbtables.*;

/**
 * This is a JMF Video Codec.
 * This is a port from ffmpeg - This version targets Div3
 */
public class Div3Codec extends Mpeg4 implements Codec, JMFCodec {
    /**
     * Per Macroblock RL tables only appear in 
     * streams of bitrate MBAC_BITRATE
     */
    private static final int MBAC_BITRATE = 50 * 1024;

    /**
     * Variable length codes describing macroblock types
     */
    private static final VLCTable   intraMacroBlock    = new IntraMacroBlock();
    private static final VLCTable   nonIntraMacroBlock = new NonIntraMacroBlock();
    private static final VLCTable   interIntraVlc      = new InterIntraVlc();

    /**
     * Variable length codes describing top level luminance and chrominance
     */
    private static final VLCTable[] dc_lum_vlc         = new VLCTable[] { new DiscreteCosineLuminanceVlc0(),
                                                                          new DiscreteCosineLuminanceVlc1() };

    private static final VLCTable[] dc_chroma_vlc      = new VLCTable[] { new DiscreteCosineChrominanceVlc0(),
                                                                          new DiscreteCosineChrominanceVlc1() };

    private static final MVTable[]  moveTable          = new MVTable[] { new MVTable0(),
                                                                         new MVTable1() };

    /**
     * Variable length codes describing used for luminance and chrominance encoding
     */
    private static final RLTable[]  rlTables           = new RLTable[] { new RLTable0(),
                                                                         new RLTable2(),
                                                                         new IntraRLTable(),
                                                                         new RLTable1(),
                                                                         new RLTable4(),
                                                                         new InterRLTable() };


    /**
     * The negotiated InputFormat
     */
    private VideoFormat inputFormat;

    /**
     * Quckly fills part of an array
     */
    private void memset( int[] array, int offset, int size, int value ) {
        for ( int i = 0; i < size; i++ ) {
            array[ offset++ ] = value;
        }
    }

    /**
     * Decode a frame
     */
    protected void decodeFrame( byte[] frameData, int dataLength ) throws FFMpegException {
        if ( dataLength < 16 ) return;
        in.setData( frameData, dataLength );

        /**
         * Decode Picture Header
         */
        pictType = in.getBits( 2 ) + 1;
        qscale   = in.getBits( 5 );

/*  Check ME */ 
        y_dc_scale = yDcScaleTable[ qscale ];
        c_dc_scale = cDcScaleTable[ qscale ];

        /* Sanity Checking 
        if ( pictType != I_FRAME_TYPE && pictType != P_FRAME_TYPE ) {
            throw new Mpeg4Exception( "PictType " + pictType + " unsupported" );
        }

        if ( qscale == 0 ) throw new Mpeg4Exception( "invalid qscale" );
        */
        /* Parse type specific data */
        if ( pictType == I_FRAME_TYPE ) {
            /* Calculate sliceHeight */
            int code = in.getBits( 5 );
            //if ( code < 0x17 ) throw new Mpeg4Exception( "Invalid code " + code );
            sliceHeight = mbHeight / ( code - 0x16 );

/* Version 4    
            in.getBits( 5 );   //FPS
            bitRate = in.getBits( 11 ) * 1024 ;
            flipFlopRounding = in.getTrueFalse();  

            perMbRlTable = (( bitRate > MBAC_BITRATE ) && in.getTrueFalse());
*/
//       	    if (perMbRlTable) {
                rlChromaTableIndex = in.decode012();
                rlTableIndex = in.decode012(); 
//            }
            dcTableIndex = in.getBits(1);
            interIntraPred = false;
            no_rounding = true;
/*
            Debug.println( "qscale:" + qscale + 
                                " rlc:" + rlChromaTableIndex +
                                " rl:" + rlTableIndex +
                                " dc:" + dcTableIndex + 
                                " mbrl:" + (perMbRlTable?1:0) + 
                                " slice:" + sliceHeight +
                                " mbheight:" + mbHeight + "  ");
 */
        } else {
            /* P frame */
            use_skip_mb_code   = in.getTrueFalse();
            rlChromaTableIndex = in.decode012();
            rlTableIndex       = rlChromaTableIndex;
            dcTableIndex       = in.getBits(1);
            mv_table_index     = in.getBits(1);

/*
            Debug.println( "skip:" + (use_skip_mb_code?1:0) +
                                " rl:" + rlTableIndex +
                                " rlc:" + rlChromaTableIndex +
                                " dc:" + dcTableIndex +
                                " mv:" + mv_table_index +
                                " mbrl:" + (perMbRlTable?1:0) +
                                " qp:" + qscale + "   ");
 **/
/*                   System.out.println( "h263_pred_motion");
        for ( int u = 2; u < 1300; u++ ) {
            System.out.print( motion_val[u][0] + " " );
            if ( (u % 100) == 99 ) System.out.println();
        }
        System.out.println();
*/
            no_rounding = flipFlopRounding ? !no_rounding : false;
        }

        /**
         * Loop over entire screen
         */
        for ( int y = 0; y < mbHeight; y++ ) {

            /**
             * Initialise the quick lookup arrays
             */
            blockIndex[ 0 ] =   blockWrap[ 0 ] * ( y * 2 + 1 ) - 1;
            blockIndex[ 1 ] =   blockIndex[ 0 ] + 1;
            blockIndex[ 2 ] =   blockIndex[ 0 ] + blockWrap[ 0 ];
            blockIndex[ 3 ] =   blockIndex[ 2 ] + 1;
            blockIndex[ 4 ] =   blockWrap[ 0 ] * ( mbHeight * 2 + 2 )
                              + blockWrap[ 4 ] * ( y + 1 );
            blockIndex[ 5 ] =   blockIndex[ 4 ] + blockWrap[ 4 ] * (mbHeight + 2);

            for ( int x = 0; x < mbWidth; x++ ) {
    //	    if ( Debug.on ) System.out.println( "next pel: " + Integer.toHexString( in.showBits(24 ) ) );
                /**
                 * Increment block index
                 */
                blockIndex[ 0 ] += 2;
                blockIndex[ 1 ] += 2;
                blockIndex[ 2 ] += 2;
                blockIndex[ 3 ] += 2;
                blockIndex[ 4 ]++;
                blockIndex[ 5 ]++;


                /**
                 * Decode Macroblock
                 */
                int cbp = 0;
                if ( pictType == I_FRAME_TYPE ) {
                    mb_intra = true;

                    /* I Frame */
                    int code = in.getVLC( intraMacroBlock );
                    for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                        // TODO calculate coded block pred flags
                        int val = (code >> (5 - i)) & 1;
                        if ( i < 4 ) {
                            int xy = blockIndex[ i ];
                            int wrap = blockWrap[0];
                            int a = coded_block[xy - 1];
                            int b = coded_block[xy - 1 - wrap];
                            int c = coded_block[xy     - wrap];
                            val ^= ( b == c ) ? a : c;
                            coded_block[xy] = val;
                        }
                        cbp |= val << (5 - i);
                    }
//                    Debug.println( "I frame cbp: " + cbp );
                } else {
//                    if ( Debug.on ) System.out.println( "P frame cbp: " + Integer.toHexString( in.showBits(24) ) );
                    /* P Frame */
                    if ( use_skip_mb_code && in.getTrueFalse() ) {
                        /* Skip this macro block */
                        mb_intra = false;
//                        if ( Debug.on ) System.out.println( "P skip at " + x + " " + y );
                        pel_motionX = 0;
                        pel_motionY = 0;
                        mb_skipped = true;

//                        Debug.println( "before" );
                        MPV_decode_mb( x, y );
                        continue;
                    }
                    mb_skipped = false;
                    int code = in.getVLC( nonIntraMacroBlock );
                    mb_intra = ((code & 0x40) == 0);
                    cbp = code & 0x3f;
//                    if ( Debug.on ) System.out.println( "P frame cbp: " + cbp + " " + code + " " + Integer.toHexString( in.showBits(24) ) );
                }
                /**
                 * Is intra frame
                 */
                if ( mb_intra ) {
//                    if ( Debug.on ) System.out.println( "Intra at " + x + " " + y + " " + ((((cbp & 3) != 0)?1:0)+(((cbp&0x3c) != 0)?2:0)) + " " + Integer.toHexString( in.showBits(24) ) );
                    ac_pred = in.getTrueFalse();
                    if (inter_intra_pred) {
                        h263_aic_dir = in.getVLC( interIntraVlc );
                    }
                    if ( perMbRlTable && (cbp != 0) ) {
                        rlChromaTableIndex = in.decode012();
                        rlTableIndex = rlChromaTableIndex; 
                    }

                } else {
                    ac_pred = false;
//                    if ( Debug.on ) System.out.println( "Motion at " + x + " " + y + " " + cbp + " " + Integer.toHexString( in.showBits(24) ) );
                    if ( perMbRlTable && (cbp != 0) ) {
                        rlChromaTableIndex = in.decode012();
                        rlTableIndex = rlChromaTableIndex; 
                    }

                    /**
                     * Read  TODO Prediction 
                     */
                    int[] dx = h263_pred_motion(x, y, 0);
    //                System.out.println( "h263_pred_motion " + dx[0] + " " + dx[1] + " " + blockIndex[ 0 ] );
                    int mx, my;
                    int mvCode = in.getVLC( moveTable[mv_table_index] );
                    if ( mvCode == moveTable[ mv_table_index ].getEscapeCode() ) {
                        mx = in.getBits( 6 );
                        my = in.getBits( 6 );
                    } else {
                        mx = moveTable[ mv_table_index ].getXCode( mvCode );
                        my = moveTable[ mv_table_index ].getYCode( mvCode );
                    }
                    mx -= 32 - dx[0];
                    my -= 32 - dx[1];

                         if ( mx <= -64 ) { mx += 64; }
                    else if ( mx >=  64 ) { mx -= 64; }
                         if ( my <= -64 ) { my += 64; }
                    else if ( my >=  64 ) { my -= 64; }

//                    Debug.println( "table: " + mv_table_index + " mvCode: " + mvCode + " mx: " + mx + " my: " + my );
//                    if ( Debug.on ) System.out.println( "Motion at " + x + " " + y + " " + cbp + " " + Integer.toHexString( in.showBits(24) ) );
                    pel_motionX = mx;
                    pel_motionY = my;
                }

                /**
                 * Display Macroblocks
                 */
                for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                    decodeBlock( i, ((cbp >> (5 - i) ) & 1) != 0 );
                }
                MPV_decode_mb( x, y );
            }
        
/*
            System.out.println( "B h263_pred_motion" );
        for ( int u = 2; u < 1300; u++ ) {
            System.out.print( motion_val[u][0] + " " );
            if ( (u % 100) == 99 ) System.out.println();
        } 
        System.out.println();
 */
        }
        
        /**
         * Frame end code 
         */
        if ( pictType == I_FRAME_TYPE ) {
            flipFlopRounding = false;  
            if ( in.availableBits() >= 17 ) {
                /**
                 * msmpeg4_decode_ext_header
                 */
                int fps = in.getBits( 5 );   //FPS
                bitRate = in.getBits( 11 ) * 1024 ;
                flipFlopRounding = in.getTrueFalse();  

                perMbRlTable = (( bitRate > MBAC_BITRATE ) && in.getTrueFalse());

//                if ( Debug.on ) System.out.print( "fps:" + fps + " bps:"+ (bitRate/1024) + " roundingType:" );
// \              if ( Debug.on ) System.out.println( (flipFlopRounding ? "1":"0") );
	    }
	}
        displayOutput.endFrame();
    }

    /**
     * Decode a pel (blockNumber) in a Macroblock
     */
    protected void decodeBlock( int blockNumber,boolean isCoded ) throws FFMpegException {

        int thisRlTableIndex;
        int i = 0;

        int qmul = 1;
        int qadd = 0;
        int run_diff = 0;

        int[] scan_table = null;

//	if ( Debug.on ) System.out.println( "isCoded: " + (isCoded?1:0) );
        /**
         * Is intra frame
         */
        if ( mb_intra ) {
            int level;
//	    in.reveal();
//	    if ( Debug.on ) System.out.println( "dcTableIndex: " + dcTableIndex );
            
            if ( blockNumber < 4 ) { 
                 level = in.getVLC( dc_lum_vlc[ dcTableIndex ] ); 
            } else {
                 level = in.getVLC( dc_chroma_vlc[ dcTableIndex ] ); 
            }
            if ( level == DiscreteCosineLuminanceVlc0.DC_MAX ) {
                level = in.getBits( 8 );
            }
            if ( level != 0 ) {
                level = in.getTrueFalse() ? -level : level;
	    }

            /**
             * msmpeg_pred_dc
             */
            int pred;
            int scale = ( blockNumber < 4 ) ? y_dc_scale : c_dc_scale;

            int wrap = blockWrap[ blockNumber ];
            int dcValPointer = blockIndex[ blockNumber ];
//	    if (blockIndex[blockNumber] == 1497)  System.out.println( "blockIndex " + blockIndex[ blockNumber ] );
            int a = (dc_val[ dcValPointer - 1        ] + (scale >> 1))/scale;
            int b = (dc_val[ dcValPointer - 1 - wrap ] + (scale >> 1))/scale;
            int c = (dc_val[ dcValPointer     - wrap ] + (scale >> 1))/scale;
/*            if (blockNumber == 2) System.out.println( "a " + Integer.toHexString(a)
                             + " b " + Integer.toHexString(b)
                             + " c " + Integer.toHexString(c)
                             + " scale " + Integer.toHexString( scale ));
*/
            if ( abs(a - b) <= abs(b - c) ) {
                pred = c;
                dc_pred_dir = true;
            } else {
                pred = a;
                dc_pred_dir = false;
            }
            level += pred;

            dc_val[ dcValPointer ] = level * scale;
//            if ( Debug.on ) System.out.println( "Pred msmpeg3 " + pred );
//            if (Debug.on) System.out.println( "Level " + level );
//            for ( int w = 0; w < 256; w++ ) System.out.print( dc_val[0][w] + " " );
//            System.out.println();
            
            /*
             * Got DC block
             */
//            if ( level < 0 ) throw new Mpeg4Exception( "Level < 0" );

            if ( blockNumber < 4 ) {
                thisRlTableIndex = rlTableIndex;
            } else {
                thisRlTableIndex = 3 + rlChromaTableIndex;
            }

            block[ blockNumber ][0] = level;
//            if ( blockIndex[blockNumber] == 1497 )  System.out.println( "Put:  " + block[4][0] );

            skipBlock[blockNumber] = false;
//            thisRlTableIndex = 0;
//	    System.out.println( "Table number " + thisRlTableIndex  );
            if ( isCoded ) {
//DO Scantable STUFF
                if ( ac_pred ) {
                    if ( !dc_pred_dir ) {
			scan_table = ScanTable.getIntraVScanTable();
 //                       if ( Debug.on ) System.out.println( "intra_v_scantable" );
		    } else {
			scan_table = ScanTable.getIntraHScanTable();
//                        if ( Debug.on ) System.out.println( "intra_h_scantable" );
                    }
		} else {
                    scan_table = ScanTable.getIntraScanTable();
//                    if ( Debug.on ) System.out.println( "intra_scantable" );
                }
	    }
        } else {
            qmul = qscale <<1;
            qadd = (qscale - 1)|1;
            i = -1;

            thisRlTableIndex = 3 + rlTableIndex;
//DO STUFF
//	    System.out.println( "BTable number " + thisRlTableIndex  );
            if ( !isCoded ) {
//                System.out.println( "Premature return" );
                return;
            }
            scan_table = ScanTable.getInterScanTable();
//            if ( Debug.on ) System.out.println( "inter_scantable" );
            run_diff = 1;
        }       
        if ( isCoded ) {
            boolean last = false;
//		System.out.println( "Before VLC " + Integer.toHexString( in.showBits(24) ) );
            while ( !last ) {
//                in.reveal();

                int index = in.getVLC( rlTables[ thisRlTableIndex ] );
                int level = rlTables[ thisRlTableIndex ].getLevel( index );
                int run   = rlTables[ thisRlTableIndex ].getRun( index );
                if ( level == 0 ) {
                    if ( !in.getTrueFalse()  ) {
                        if ( !in.getTrueFalse() ) {
                            last  = in.getTrueFalse();
                            run   = in.getBits( 6 );
                            level = in.getBits( 8 );
                            if ( level < 128 ) {
                                level = level * qmul + qadd;
                            } else {
                                level = (-256 + level) * qmul - qadd;
                            }
                            i += run + 1 + (last ? 192 : 0);
//			    System.out.println( "Point1 " + run + " " + level + " " + i );
                        } else {
                            index = in.getVLC( rlTables[ thisRlTableIndex ] );
                            level = rlTables[ thisRlTableIndex ].getLevel( index ) * qmul + qadd;
                            run   = rlTables[ thisRlTableIndex ].getRun( index );
                            i += run + rlTables[ thisRlTableIndex ].getMaxRun()[ run>>7 ][ level/qmul ] + run_diff;
                            if ( in.getTrueFalse() ) {
                                 level = ( ~level ) + 1;
	         	    }
//	        	    System.out.println( "Point2 " + run + " " + level + " " + i );
                        }
                    } else {
                        index = in.getVLC( rlTables[ thisRlTableIndex ] );
                        level = rlTables[ thisRlTableIndex ].getLevel( index ) * qmul + qadd;
                        run   = rlTables[ thisRlTableIndex ].getRun( index );
                        i += run;
                        level += rlTables[ thisRlTableIndex ].getMaxLevel()[ run>>7 ][ (run-1)&63 ] * qmul;
                        if ( in.getTrueFalse() ) {
                            level = ( ~level ) + 1;
                        }
//                        System.out.println( "Point3 " + run + " " + level + " " + i);
                    }
                } else {
                    i += run;
                    level = level  * qmul + qadd;
                    if ( in.getTrueFalse() ) {
                        level = ( ~level ) + 1;
                    }
//		    System.out.println( "Point4 " + run + " " + level + " " + i );
                }
//   if (Debug.on) System.out.println( "Point " + run + " " + level + " " + i );

//	        if ( Debug.on ) System.out.println( "i " + i );
                if ( i > 62 ) {
                    i -= 192;
//                    if ( i < 0 || i > 63 ) throw new Mpeg4Exception( "Illegal i " + i );
                    last = true;
                    block[blockNumber][scan_table[i]] = level;
                } else {
                    block[blockNumber][scan_table[i]] = level;
		}
//		if ( Debug.on ) System.out.println( "Scan table " + i + " " + scan_table[i] );
            }
//            if ( Debug.on ) System.out.println( Integer.toHexString( in.showBits(24) ) );
        }
        
        if (mb_intra) {
/*                System.out.println( "Before" );
                for ( int o = 0; o < 64; o++ ) {
                    System.out.print( Integer.toHexString( block[blockNumber][o] ) + " " );
                }
                System.out.println();
*/ 
            /* TODO mpeg_pred_ac h263.c */
            int acPredOffset = blockIndex[blockNumber] * 16;
            if ( ac_pred ) {
                if ( !dc_pred_dir ) {
//		    System.out.println( "Left prediction " + Integer.toString(acPredOffset - 16) );
                    for ( int p = 1; p < 8; p++ ) {
                        block[blockNumber][p * 8] += ac_val[acPredOffset + p - 16];
//			System.out.print( Integer.toHexString( ac_val[acPredOffset + p - 16] ) + " ");
                    }
                } else {
//		    System.out.println( "Top prediction " + Integer.toString(acPredOffset - 16 * blockWrap[blockNumber] + 8) );
                    for ( int p = 1; p < 8; p++ ) {
                        block[blockNumber][p] += ac_val[acPredOffset + p - 16 * blockWrap[blockNumber] + 8 ];
                        
//			System.out.print( Integer.toHexString( ac_val[acPredOffset + p - 16 * blockWrap[blockNumber] + 8] ) + " ");
                    }
		}
//                System.out.println();
            }

            for( int ii = 1; ii < 8; ii++ ) {
                /* left copy */
                ac_val[acPredOffset     + ii] = block[blockNumber][ii * 8];
                ac_val[acPredOffset + 8 + ii] = block[blockNumber][ii];
            }

/*            if ( Debug.on ) 
            {
                System.out.println("ac_val " + acPredOffset);
                for ( int ii = acPredOffset; ii < acPredOffset + 16; ii++ ) System.out.print( Integer.toHexString( ac_val[0][ii] ) + " " );
                System.out.println();
 */
 /*           if ( blockIndex[ blockNumber ] == 987 ) {
                System.out.println( "After" );
                for ( int o = 0; o < 64; o++ ) {
                    System.out.print( Integer.toHexString( block[blockNumber][o] ) + " " );
                }
                System.out.println();            
            }
  **/
 
            
            skipBlock[ blockNumber ] = ( i < 0 ) ;
        }
    }

    /**
     * Return absolute value of a
     */
    private static final int abs( int a ) {
        return (a < 0) ? -a : a;
    }

    private static final int[] motionOffset = new int[] { 2, 1, 1, -1 };
    /**
     * Read the motion vectors for this block
     */
    protected final int[] h263_pred_motion( int x, int y, int blockNumber ) {
        /**
         * Only the luminance blocks have motion vectors.  Chrominance
         * components follow them
         */
        int wrap = blockWrap[0];
        int xy   = blockIndex[ blockNumber ];

        int[] a, b, c;
        int px = 0;
        int py = 0;

        /**
         * A represents motion vectors to the left of this block
         */
        a = motion_val[ xy - 1 ];
        if ( y == 0 && blockNumber < 3 ) {
            /**
             * Top row of blocks
             */
            if ( blockNumber == 0 ) {
                if ( x + 1 == resync_mb_x ) {
                    /**
                     * C represents motion vectors above/right of this block
                     */
                    c = motion_val[ xy + motionOffset[blockNumber] - wrap ];
                    if ( x == 0 ) {
                        /**
                         * Predict value as row above
                         */
                        px = c[0];
                        py = c[1];
                    } else {
                        /**
                         * Predict value as "middle value" of above/right and left
                         */
                        px = mid_pred( a[0], 0, c[0] );
                        py = mid_pred( a[1], 0, c[1] );
                    }
                } else if ( x != resync_mb_x ) {
                    /**
                     * Predict value as left
                     */
                    px = a[0];
                    py = a[1];
                }
            } else if ( blockNumber == 1 ) {
                if ( x + 1 == resync_mb_x ) {
                    /**
                     * Predict value as "middle value" of above/right and left (and 0)
                     */
                    c = motion_val[ xy + motionOffset[blockNumber] - wrap ];
                    px = mid_pred( a[0], 0, c[0] );
                    py = mid_pred( a[1], 0, c[1] );
                } else {
                    /**
                     * Predict value as the same as block to the left
                     */
                    px = a[0];
                    py = a[1];
                }
            } else {  //block == 2
                /**
                 * Predict as "middle value" of above, above/right, and left
                 */
                b = motion_val[ xy                             - wrap ];
                c = motion_val[ xy + motionOffset[blockNumber] - wrap ];
                if ( x == resync_mb_x ) {
                    a[0] = 0;
                    a[1] = 0;
                }
                px = mid_pred( a[0], b[0], c[0] );
                py = mid_pred( a[1], b[1], c[1] );
            }
        } else {
            /**
             * Predict as "middle value" of above, above/right, and left
             */
           b = motion_val[ xy                             - wrap ];
           c = motion_val[ xy + motionOffset[blockNumber] - wrap ];
           px = mid_pred( a[0], b[0], c[0] );
           py = mid_pred( a[1], b[1], c[1] );
        }
        
        /**
         * Cache these motion vectors and return a reference to them
         */
        motion_val[ xy ][ 0 ] = px;
        motion_val[ xy ][ 1 ] = py;
        return motion_val[ xy ];
    }

    /**
     * Return the middle value  So c>b>a returns b
     */
    private final int mid_pred( int a, int b, int c ) {
        int vmax = a;
        int vmin = a;
        if ( b < vmin ) { vmin = b; } else { vmax = b; }
        if ( c < vmin ) { vmin = c; } else if ( c > vmax ) { vmax = c; }
        return a + b + c - vmin - vmax;
    }

    /**
     * Set the width and height in pixels for this video stream
     */
    protected void initialise( int width, int height ) {
        super.initialise( width, height );

        /**
         * Reset dc/ac values
         */
        for ( int y = 0; y < mbHeight; y++ ) {
            int wrap = 2 * mbWidth + 2;
            memset( dc_val, 1 + y * wrap, mbWidth * 2, 0x400 );
        }
        
        yDcScaleTable = ScaleTable.getMpeg4Luminance();
        cDcScaleTable = ScaleTable.getMpeg4Chrominance();
    }

    /**
     * Construct the codec.  Initialization of the codec
     * should be performced via setInputFormat. 
     */
    public Div3Codec() {
        super();
    }

    /**
     * Construct the codec with the width and height of the 
     * video in pixels.  This is the least data required to
     * use the codec.
     */
    public Div3Codec(int width, int height) {
        super();
        initialise( width, height );
    }
    
    /**
     * Retrieve the supported input formats.  Currently "DIV3" 
     *
     * @return Format[] the supported input formats
     */
    public Format[] getSupportedInputFormats() {
        return new Format[] { new VideoFormat( "DIV3" ) };
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
    
    /**
     * Converts a byte array Buffer of DIV3 video data
     * into an integer array Buffer of video data.
     * 
     * Note: The DIV3 codec requires that the input buffer represents
     * exactly one frame
     *
     * @return BUFFER_PROCESSED_OK The output buffer contains a valid frame
     * @return BUFFER_PROCESSED_FAILED A decoding problem was encountered
     */
    public int process( Buffer in, Buffer out ) {
        try {
            /* Extract the input data */
            byte[] data = (byte[])in.getData();
            byte[] buffer = new byte[ in.getLength() + 20 ];
            System.arraycopy( data, 0, buffer, 0, in.getLength() );
            
            /* Decode and create an output image */
            decodeFrame( buffer, in.getLength() );
            displayOutput.showScreen(out);
            out.setTimeStamp( in.getTimeStamp() );
            out.setFlags( in.getFlags() );
        } catch ( Exception e ) {
            return BUFFER_PROCESSED_FAILED;
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
     * Retrives the name of this video codec: "DIV3 video decoder"
     * @return Codec name
     */
    public String getName() {
        return "DIV3 video decoder";
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
	setInputFormat( new VideoFormat( "DIV3", size, -1, (new byte[ 0 ]).getClass(), 0 ) );
    }

    public void setEncoding( String encoding ) {
    }

    public void setIsRtp( boolean isRtp ) {
    }

    public void setIsTruncated( boolean isTruncated ) {
    }
}
