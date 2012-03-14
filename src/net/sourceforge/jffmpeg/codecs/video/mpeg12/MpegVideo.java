/*
 * Java port of ffmpeg mpeg1/2 decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2000,2001 Fabrice Bellard.
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
package net.sourceforge.jffmpeg.codecs.video.mpeg12;

import java.io.*;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.Buffer;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import java.awt.Dimension;

import net.sourceforge.jffmpeg.JMFCodec;

import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.Tables;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.MbPTypeVLC;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.MbBTypeVLC;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.AddressIncrementVlc;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.MotionVectorVlc;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.PatVLC;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.DiscreteCosineLuminanceVlc;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.data.DiscreteCosineChrominanceVlc;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.rltables.RLTable;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.rltables.Mpeg1RLTable;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.rltables.Mpeg2RLTable;
import net.sourceforge.jffmpeg.codecs.video.mpeg12.scantable.*;

import net.sourceforge.jffmpeg.codecs.utils.BitStream;
import net.sourceforge.jffmpeg.codecs.utils.VLCTable;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;

import net.sourceforge.jffmpeg.codecs.video.mpeg.DisplayOutput;


/**
 * This codec can decode MPEG 1 and MPEG 2 streams.
 */
public class MpegVideo implements Codec, JMFCodec {
    /**
     * Input and output variables
     */
    protected BitStream in = new BitStream();
    private   DisplayOutput displayOutput;
    
    /**
     * Debugging tools
     *  - showInterlace shows interlaced streams side-by-side
     *  - skipBFrames   does not display B frames (much faster)
     *  - debug         dumps debugging data
     */
    public static final boolean showInterlace = false;
    public static final boolean skipBFrames = false;
    public static final boolean debug = false;
    
    /**
     * Speed management
     */
    private int numberOfFramesDelivered = 0;
    public int targetFrameBuffer  = 50;
    public boolean hurryUp = false;

    /**
     * Synchronisation codes
     */
    public static final int SYNC_BYTES = 0x000001;

    public static final int SEQ_END_CODE         = 0x00000b7;
    public static final int SEQ_START_CODE       = 0x00000b3;
    public static final int GOP_START_CODE       = 0x00000b8;
    public static final int PICTURE_START_CODE   = 0x0000000;
    public static final int SLICE_MIN_START_CODE = 0x0000001;
    public static final int SLICE_MAX_START_CODE = 0x00000af;
    public static final int EXT_START_CODE       = 0x00000b5;
    public static final int USER_START_CODE      = 0x00000b2;

    /**
     * Extension codes
     */
    private static final int SEQUENCE_EXTENSION         = 1;
    private static final int SEQUENCE_DISPLAY_EXTENSION = 2;
    private static final int QUANT_MATRIX_EXTENSION     = 3;
    private static final int PICTURE_DISPLAY_EXTENSION  = 7;
    private static final int PICTURE_CODING_EXTENSION   = 8;

    /**
     * Picture types
     */
    public static final int I_TYPE = 1;
    public static final int P_TYPE = 2;
    public static final int B_TYPE = 3;
    public static final int SKIP_FRAME_TYPE = -1;
    
    public static final int PICT_FRAME = 3;

    public static final int MT_FIELD = 1;
    public static final int MT_FRAME = 2;
    public static final int MT_16X8  = 2;
    public static final int MT_DMV   = 3;
    
    /**
     * Macroblock motion types
     */
    public static final int MV_TYPE_16X16 = 0;   // 1 vector for the whole mb 
    public static final int MV_TYPE_8X8   = 1;   // 4 vectors (h263,  4MV) 
    public static final int MV_TYPE_16X8  = 2;   // 2 vectors, one per 16x8 block  
    public static final int MV_TYPE_FIELD = 3;   // 2 vectors, one per field  
    public static final int MV_TYPE_DMV   = 4;   // 2 vectors, special mpeg2 Dual Prime Vectors 
    
    /**
     * Macroblock motion direction (from last or to next I/P frame)
     */
    public static final int MV_DIR_FORWARD  = 2;
    public static final int MV_DIR_BACKWARD = 1;
    
    
    private int currentHeader;
    
    /**
     * Width/Height and format (mpeg2 or mpeg1)
     */
    private int mbWidth;
    private int mbHeight;
    protected boolean mpeg2;
    
    /**
     * Sequence header
     */
    protected int width;
    protected int height;
    protected int aspectRatio;
    protected int frame_rate_index;
    protected int bit_rate;
    protected float frameRate = 299/10;
    
    /**
     * Internal data tables
     */
    private ScanTable alternateVerticalScanTable   = new AlternateVerticalScan();
    private ScanTable alternateHorizontalScanTable = new AlternateHorizontalScan();
    private ScanTable zigZagDirect                 = new ZigZagDirect();

    private ScanTable intraScanTable   = new ZigZagDirect();
    private ScanTable interScanTable   = new ZigZagDirect();
    private ScanTable intraHScanTable  = new AlternateHorizontalScan();
    private ScanTable intraVScanTable  = new AlternateVerticalScan();

    private final int[] dsp_idct_permutation          = Tables.getDspIdctPermutation();
    private final int[] ff_mpeg1_default_intra_matrix = Tables.getMpeg1DefaultIntraMatrix();
    private final int[] ff_mpeg1_default_non_intra_matrix = Tables.getMpeg1DefaultNonIntraMatrix();
    
    private final int[] non_linear_qscale = Tables.getNonLinearQscale();
    
    public final VLCTable mbincr_vlc = new AddressIncrementVlc();
    public final VLCTable mv_vlc     = new MotionVectorVlc();
    
    public final RLTable rl_mpeg1 = new Mpeg1RLTable();
    public final RLTable rl_mpeg2 = new Mpeg2RLTable();
    
    private final VLCTable dc_lum_vlc = new DiscreteCosineLuminanceVlc();
    private final VLCTable dc_chroma_vlc =  new DiscreteCosineChrominanceVlc();

    private int[] ptype2mb_type = Tables.getPType2mb_type();
    private int[] btype2mb_type = Tables.getBType2mb_type();

    private VLCTable mb_ptype_vlc = new MbPTypeVLC();
    private VLCTable mb_btype_vlc = new MbBTypeVLC();
    
    private VLCTable mb_pat_vlc = new PatVLC();

    /**
     * Internal State - Quantization matricies
     */
    private int[] intra_matrix        = new int[ 64 ];
    private int[] inter_matrix        = new int[ 64 ];
    private int[] chroma_intra_matrix = new int[ 64 ];
    private int[] chroma_inter_matrix = new int[ 64 ];


    /**
     * Internal State - Motion code
     */
    private int[][] motion_val;
    private int mv_dir;
    private int mv_type;
    private int motion_type;
    private int[] mv = new int[ 8 ];
    private int[] last_mv = new int[ 8 ];
    private boolean[] full_pel     = new boolean[ 2 ];
    private boolean[] field_select = new boolean[ 4 ];
    
    /**
     * Internal State - I type macroblock
     */
    private boolean mb_intra = true;
    
    /**
     * Pels forming a Macroblock
     */
    protected int mb_type;
    public static final int NUMBER_OF_BLOCKS = 6;
    private int[] blockWrap  = new int[ NUMBER_OF_BLOCKS ];
    private int[] blockIndex = new int[ NUMBER_OF_BLOCKS ];
    private int[][] block    = new int[ NUMBER_OF_BLOCKS ][ 64 ];
    
    /**
     * Cache for pel DC compoent decoding
     */
    private int[] last_dc = new int[] { 0x080, 0x080, 0x080 };
    
    /**
     * Sequence extension information
     */
    protected int profile;
    protected int level;
    protected boolean progressive_sequence;
    protected int vdv_buf_ext;

    /**
     * Pan and scan information
     */
    private int panScanWidth;
    private int panScanHeight;
    
    protected int intra_dc_precision;
    protected int picture_structure = PICT_FRAME;
    protected boolean top_field_first;
    protected boolean frame_pred_frame_dct;
    protected boolean concealment_motion_vectors;
    protected boolean q_scale_type;
    protected boolean intra_vlc_format;
    protected boolean alternate_scan;
    protected boolean repeat_first_field;
    protected boolean chroma_420_type;
    protected boolean progressive_frame;

    protected boolean first_field;

    /**
     * Picture definistion codes
     */
    protected int pict_type;
    protected int picture_number;
    protected int[] mpeg_f_code = new int[ 4 ];
    protected int y_dc_scale;
    protected int c_dc_scale;
    protected boolean first_slice;
    
    /**
     * Current macroblock positions
     */
    protected boolean field_pic;
    protected int resync_mb_x;
    protected int resync_mb_y;
    protected int mb_x;
    protected int mb_y;
    
    /**
     * scaling
     */
    protected boolean interlaced_dct;
    protected int repeat_pict;
    
    protected int qscale;
    protected int mb_skip_run;
    
    protected final float[] frameRateTable = Tables.getFrameRateTable();

    /**
     * Creates a new instance of MpegVideo 
     * The only useful initialisation for this codec is
     * the video widtha and height.
     */
    public MpegVideo() {
    }

    /**
     * Initialise the codec for this width and height
     */
    private void initialise( int width, int height ) {
        /**
         * Calculate number of macroblocks
         */
        mbWidth = (width + 15) / 16;
        mbHeight = (height + 15) / 16;
        if ( displayOutput == null ) displayOutput = new DisplayOutput(mbWidth, mbHeight);
        
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
         * Initialise arrays
         */
        motion_val = new int[ 1 + (mbWidth * 2 + 2) * mbHeight * 2  * 4 ][2];
    }

    /**
     * This packet describes the highest level video attributes:
     *   - Width, Height
     *   - Aspect ratio
     *   - Frame/Bit rate
     *   - Luminance and Chrominance matricies
     */
    private void mpeg1_decode_sequence() {
        width  = in.getBits( 12 );
        height = in.getBits( 12 );
        aspectRatio = in.getBits( 4 );
        frame_rate_index = in.getBits( 4 );
        frameRate = frameRateTable[ frame_rate_index ];
//        System.out.println( "fri " + frameRate );
        bit_rate = in.getBits( 18 ) * 400;
        in.getTrueFalse();
        in.getBits(10);
        in.getTrueFalse();
        
        /* Get Intra Matrix */
        if ( in.getTrueFalse() ) {
            for ( int i = 0; i < 64; i++ ) {
                int v = in.getBits( 8 );
                int j = intraScanTable.getPermutated()[ i ];
                intra_matrix[ j ] = v;
                chroma_intra_matrix[ j ] = v;
            }
        } else {
            for ( int i = 0; i < 64; i++ ) {
                int j = dsp_idct_permutation[ i ];
                int v = ff_mpeg1_default_intra_matrix[ i ];
                intra_matrix[ j ] = v;
                chroma_intra_matrix[ j ] = v;
            }
        }
        /* Get Non-intra Matrix */
        if ( in.getTrueFalse() ) {
            for ( int i = 0; i < 64; i++ ) {
                int v = in.getBits( 8 );
                int j = intraScanTable.getPermutated()[ i ];
                inter_matrix[ j ] = v;
                chroma_inter_matrix[ j ] = v;
            }
        } else {
            for ( int i = 0; i < 64; i++ ) {
                int j = dsp_idct_permutation[ i ];
                int v = ff_mpeg1_default_non_intra_matrix[ i ];
                inter_matrix[ j ] = v;
                chroma_inter_matrix[ j ] = v;
            }
        }

        /* Initialise variables for Mpeg1 */
        progressive_sequence = true;
        progressive_frame = true;
        picture_structure = PICT_FRAME;
        frame_pred_frame_dct = true;
    }
    
    /**
     * This packet is only found in MPEG2 streams.  It defines:
     *   - Extended Width and height
     */
    private void mpeg_decode_sequence_extension() {
        in.getTrueFalse();    // Profile and level escape
        profile  = in.getBits(3);
        level    = in.getBits(4);
        progressive_sequence = in.getTrueFalse();
        in.getBits( 2 );     //Chroma format
        width   |= (in.getBits( 2 ) << 12);
        height  |= (in.getBits( 2 ) << 12);
        bit_rate = ((bit_rate/400)|(in.getBits(12) << 12)) * 400;
        in.getTrueFalse();
        vdv_buf_ext = in.getBits( 8 );
        
        in.getTrueFalse();
        int frame_rate_ext_n = in.getBits(2);
        int frame_rate_ext_d = in.getBits(5);
     
//        System.out.println( "Frame rate " + frame_rate_ext_n + " " + frame_rate_ext_d );
//        System.out.println( "progressive " + progressive_sequence );
        mpeg2 = true;
    }

    /**
     * This packet is used for pan and scan.  It describes:
     *  - Width and Height of pan area
     */
    private void mpeg_decode_sequence_display_extension() {
        in.getBits(3);
        if (in.getTrueFalse()) {
            in.getBits(24);
        }
        
        int width = in.getBits( 14 );
        in.getTrueFalse();
        int height = in.getBits( 14 );
        in.getTrueFalse();
        
        panScanWidth  = 16 * width;
        panScanHeight = 16 * height;
    }

    /**
     * This packet describes a variety of display variables:
     *  - Field selection
     *  
     */    
    private void mpeg_decode_picture_coding_extension() {
        full_pel[0] = false;
        full_pel[1] = false;
        mpeg_f_code[0]          = in.getBits(4);
        mpeg_f_code[1]          = in.getBits(4);
        mpeg_f_code[2]          = in.getBits(4);
        mpeg_f_code[3]          = in.getBits(4);
        intra_dc_precision         = in.getBits(2);
        picture_structure          = in.getBits(2);
        top_field_first            = in.getTrueFalse();
        frame_pred_frame_dct       = in.getTrueFalse();
        concealment_motion_vectors = in.getTrueFalse();
        q_scale_type               = in.getTrueFalse();
        intra_vlc_format           = in.getTrueFalse();
        alternate_scan             = in.getTrueFalse();
        repeat_first_field         = in.getTrueFalse();
        chroma_420_type            = in.getTrueFalse();
        progressive_frame          = in.getTrueFalse();

        if( picture_structure == PICT_FRAME ) {
            first_field = false;
        } else {
            first_field = !first_field;
            /** Removed memset0 */
        }

        if( alternate_scan ) {
            intraScanTable   = alternateVerticalScanTable;
            interScanTable   = alternateVerticalScanTable;
            intraHScanTable  = alternateVerticalScanTable;
            intraVScanTable  = alternateVerticalScanTable;
        } else {
            intraScanTable   = zigZagDirect;
            interScanTable   = zigZagDirect;
            intraHScanTable  = alternateHorizontalScanTable;
            intraVScanTable  = alternateVerticalScanTable;
        }

        /* composite display not parsed */
        if ( debug ) 
        {
            System.out.println("intra_dc_precision=" + intra_dc_precision);
            System.out.println("picture_structure=" + picture_structure);
            System.out.println("top field first=" + (top_field_first?1:0));
            System.out.println("repeat first field=" + (repeat_first_field?1:0));
            System.out.println("conceal=" + (concealment_motion_vectors?1:0));
            System.out.println("intra_vlc_format=" + (intra_vlc_format?1:0));
            System.out.println("alternate_scan=" + (alternate_scan?1:0));
            System.out.println("frame_pred_frame_dct=" + (frame_pred_frame_dct?1:0));
            System.out.println("progressive_frame=" +( progressive_frame?1:0));
        }
    }

    /**
     * Extract quantisation matrix extension
     */
    private void mpeg_decode_quant_matrix_extension() {
        int i, v, j;
        if (in.getTrueFalse()) {
            for(i=0;i<64;i++) {
                v = in.getBits(8);
                j= zigZagDirect.getPermutated()[i];
                intra_matrix[j] = v;
                chroma_intra_matrix[j] = v;
            }
        }
        if (in.getTrueFalse()) {
            for(i=0;i<64;i++) {
                v = in.getBits(8);
                j= zigZagDirect.getPermutated()[i];
                inter_matrix[j] = v;
                chroma_inter_matrix[j] = v;
            }
        }
        if (in.getTrueFalse()) {
            for(i=0;i<64;i++) {
                v = in.getBits(8);
                j= zigZagDirect.getPermutated()[i];
                chroma_intra_matrix[j] = v;
            }
        }
        if (in.getTrueFalse()) {
            for(i=0;i<64;i++) {
                v = in.getBits(8);
                j= zigZagDirect.getPermutated()[i];
                chroma_inter_matrix[j] = v;
            }
        }
    }
    
    /**
     * Decode extension codes
     */
    private void mpeg_decode_extension() {
        int extensionType = in.getBits( 4 );
        switch (extensionType) {
            case SEQUENCE_EXTENSION: {
                mpeg_decode_sequence_extension();
                break;
            }
            case SEQUENCE_DISPLAY_EXTENSION: {
                mpeg_decode_sequence_display_extension();
                break;
            }
            case QUANT_MATRIX_EXTENSION: {
                mpeg_decode_quant_matrix_extension();
                break;
            }
            case PICTURE_DISPLAY_EXTENSION: {
                break;
            }
            case PICTURE_CODING_EXTENSION: {
                mpeg_decode_picture_coding_extension();
                break;
            }
            default: {
                break;
            }
        }
    }
        
    /**
     * Start to decode a frame.  This includes:
     *  - Frame reference number
     *  - Frame type (I, P, or B)
     *  - Field management
     */
    private void mpeg1_decode_picture() {
        int frameReference = in.getBits( 10 );
        int f_code;
        
        pict_type = in.getBits( 3 );
        
//        System.out.println( "pict_type=" + pict_type + " number=" + reference + " " + frame_rate_index + " " + inverseTelecine );
        
        in.getBits(16);
        if ( pict_type == P_TYPE || pict_type == B_TYPE ) {
            full_pel[0] = in.getTrueFalse();
            f_code = in.getBits(3);
            mpeg_f_code[0] = f_code;
            mpeg_f_code[1] = f_code;
        }
        if ( pict_type == B_TYPE ) {
            full_pel[1] = in.getTrueFalse();
            f_code = in.getBits(3);
            mpeg_f_code[2] = f_code;
            mpeg_f_code[3] = f_code;
        }
        
        y_dc_scale = 8;
        c_dc_scale = 8;
        first_slice = true;
    }

    /**
     * Decode a motion vector
     */
    private int mpeg_decode_motion( int fcode, int predicted ) throws FFMpegException {
        int code = in.getVLC( mv_vlc );
        if ( code == 0 ) return predicted;
        
        boolean sign = in.getTrueFalse();
        int shift = fcode - 1;
        if ( shift != 0 ) {
            code = (code - 1) << shift;
            code |= in.getBits( shift );
            code++;
        }
        if ( sign ) code = -code;
        code += predicted;

        int l = 1 << (shift + 4);
        code = ((code + l)&(l*2 - 1)) - l;
        return code;
    }
    
    /**
     * Decode a DC value
     */
    private int decode_dc( int component ) throws FFMpegException {
        int code = in.getVLC( (component == 0) ? dc_lum_vlc : dc_chroma_vlc );
        if ( code == 0 ) {
            return 0;
        } else {
            int diff = in.getBits( code );
            if ((diff & (1 << (code - 1))) == 0)  {
                diff = (-1 << code) | (diff + 1);
            }
            return diff;
        }
    }
    
    /* MPEG 1 **************************************************************************/
    /**
     * Decode a pel for a predicted MPEG1 macroblock
     */
    private void mpeg1_decode_block_inter( int[] block, int blockNumber ) throws FFMpegException {
        RLTable rltable = rl_mpeg1;
        ScanTable scantable = intraScanTable;
        int[] quant_matrix= inter_matrix;

        {
            int i = -1;
            int j;
            /* special case for the first coef. no need to add a second vlc table */
            int v = in.showBits( 2 );
            if ((v & 2) != 0) {
                in.getBits(2);
                level = ( 3 * qscale * quant_matrix[0] ) >> 4;
                level = (level-1)|1;
                if( ( v & 1 ) != 0) {
                    level= - level;
                }
                block[0] = level;
                i++;
            }

            /* now quantify & encode AC coefs */
            for(;;) {
                int index = in.getVLC( rltable );
                int level = rltable.getLevel( index );
                int run   = rltable.getRun( index );

                if ( level == 127 ) {
                    break;
                } else if( level != 0 ) {
                    i += run;
                    j = scantable.getPermutated()[ i ];

                    level= (( level * 2 + 1) * qscale * quant_matrix[j] ) >> 4;
                    level= ( level - 1 )|1;
                    if ( in.getTrueFalse() ) {
                        level = -level;
                    }
                } else {
                    /* escape */
                    run = in.getBits(6)+1;
                    level = in.getBits(8);
                    if ( (level & (1 << 7)) != 0) level |= ~255;
                    if (level == -128) {
                        level = in.getBits(8) - 256;
                    } else if (level == 0) {
                        level = in.getBits(8);
                    }
                    i += run;
                    j = scantable.getPermutated()[i];

                    if ( level<0 ) {
                        level= -level;
                        level= ((level*2+1)*qscale*quant_matrix[j])>>4;
                        level= (level-1)|1;
                        level= -level;
                    } else {
                        level= ((level*2+1)*qscale*quant_matrix[j])>>4;
                        level= (level-1)|1;
                    }
                }
                if (i > 63 ) {
                    throw new MpegException( "Illegal MB code" );
                }
                block[j] = level;
            }
        }
    }
    
    /**
     * Decode a pel for an I-type MPEG1 macroblock
     */
    private void mpeg1_decode_block_intra( int[] block, int blockNumber ) throws FFMpegException {
        RLTable rltable = rl_mpeg1;
        ScanTable scantable = intraScanTable;
        int[] quant_matrix= intra_matrix;

        /* DC coef */
        int component = (blockNumber <= 3 ? 0 : blockNumber - 4 + 1);
        int diff = decode_dc( component);
        int dc = last_dc[component];
        dc += diff;
        last_dc[component] = dc;
        
        block[0] = dc << 3;

        int i = 0;
        int j;
        {
            /* now quantify & encode AC coefs */
            for(;;) {
                int index = in.getVLC( rltable );
                int level = rltable.getLevel( index );
                int run   = rltable.getRun( index );

                if( level == 127 ) {
                    break;
                } else if( level != 0 ) {
                    i += run;
                    j = scantable.getPermutated()[ i ];

                    level= ( level * qscale * quant_matrix[j]) >> 3;
                    level= ( level - 1 )|1;
                    if ( in.getTrueFalse() ) {
                        level = -level;
                    }
                } else {
                    /* escape */
                    run = in.getBits(6) + 1;
                    level = in.getBits(8);
                    if ( (level & (1<<7)) != 0 ) level |= ~255;
                    if ( level == -128 ) {
                        level = in.getBits(8) - 256;
                    } else if ( level == 0 ) {
                        level = in.getBits(8);
                    }
                    i += run;
                    j = scantable.getPermutated()[i];

                    if( level < 0 ) {
                        level= -level;
                        level= ( level * qscale * quant_matrix[j] ) >> 3;
                        level= ( level - 1 ) | 1;
                        level= -level;
                    }else{
                        level= ( level * qscale * quant_matrix[j] ) >> 3;
                        level= ( level - 1 ) | 1;
                    }
                }
                if (i > 63){
                    throw new MpegException( "Error decoding mb" );
                }

                block[j] = level;
            }
        }
    }
    
    /* MPEG 2 **************************************************************************/
    /**
     * Decode a pel for a predicted MPEG2 macroblock
     */
    private void mpeg2_decode_block_intra(int[] block, int blockNumber) throws FFMpegException {
        int[] intra_scantable = intraScanTable.getPermutated();
        int[] quant_matrix = (blockNumber < 4) ? intra_matrix : chroma_intra_matrix;
        int component = (blockNumber < 4) ? 0 : blockNumber - 3;
        last_dc[ component ] += decode_dc( component );
        block[0] = last_dc[ component ] << (3 - intra_dc_precision);

        int mismatch = block[0]^1;
        
        RLTable rltable = intra_vlc_format ? rl_mpeg2 : rl_mpeg1;
        int i = 0;
        int j = 0;

        for (;;) {
            int index = in.getVLC( rltable );
            int level = rltable.getLevel( index );
            int run   = rltable.getRun( index );

            if ( level == 127 ) break;
            if ( level != 0 ) {
                i += run;
                j = intra_scantable[ i ];
                level = (level * qscale * quant_matrix[j]) >> 4;
                if ( in.getTrueFalse() ) {
                    level = (level ^ ~0) + 1;
                }
            } else {
                /* escape code */
                run = in.getBits(6) + 1;
                
                if ( in.getTrueFalse() ) {
                    level = in.getBits(11) | (~0x7ff);
                } else {
                    level = in.getBits(11);
                }
                i += run;
                j = intra_scantable[ i ];
                if ( level < 0 ) {
                    level = ((-level) * qscale * quant_matrix[j]) >> 4;
                    level = -level;
                } else {
                    level = (level * qscale * quant_matrix[j]) >> 4;
                }
            }
            if ( i > 63 ) throw new MpegException( "Error" );

            mismatch ^= level;
            block[j] = level;
        }
        block[63] ^= mismatch & 1;
    }

    /**
     * Decode a pel for an I-TYPE MPEG2 macroblock
     */
    private void mpeg2_decode_block_non_intra(int[] block, int blockNumber) throws FFMpegException {
        int[] intra_scantable = intraScanTable.getPermutated();
        int[] quant_matrix = (blockNumber < 4) ? inter_matrix : chroma_inter_matrix;

        int mismatch = 1;
        int i = -1;

        int v = in.showBits(2);
        if ( (v & 2) == 2 ) {
            v = in.getBits(2);
            block[ 0 ] = (3 * qscale * quant_matrix[ 0 ]) >> 5;
            if ( (v & 1) == 1 ) block[0] = -block[0];
            mismatch ^= block[0];
            i++;
        }

        RLTable rltable = rl_mpeg1;
        int j = 0;

        for (;;) {
            int index = in.getVLC( rltable );
            int level = rltable.getLevel( index );
            int run   = rltable.getRun( index );

            if ( level == 127 ) break;
            if ( level != 0 ) {
                i += run;
                j = intra_scantable[ i ];
                level = ((level*2+1) * qscale * quant_matrix[j]) >> 5;
                if ( in.getTrueFalse() ) {
                    level = (level ^ ~0) + 1;
                }
            } else {
                /* escape code */
                run = in.getBits(6) + 1;
                
                if ( in.getTrueFalse() ) {
                    level = in.getBits(11) | (~0x7ff);
                } else {
                    level = in.getBits(11);
                }
                i += run;
                j = intra_scantable[ i ];
                if ( level < 0 ) {
                    level = ((-level*2+1) * qscale * quant_matrix[j]) >> 5;
                    level = -level;
                } else {
                    level = ((level*2+1) * qscale * quant_matrix[j]) >> 5;
                }
            }
            if ( i > 63 ) throw new MpegException( "Error" );

            mismatch ^= level;
            block[j] = level;
        }
        block[63] ^= mismatch & 1;
    }
    
    /**
     * Decode a macroblock and the motion vector
     */
    private void mpeg_decode_mb() throws FFMpegException {
//System.out.println( "decode_mb: x=" + mb_x + " y=" + mb_y);
        if ( mb_skip_run-- != 0 ) {
            if ( pict_type == I_TYPE ) throw new Error( "skip in IFrame" );
            /* skip mb */
            mv_type = MV_TYPE_16X16;
            if (pict_type == P_TYPE) {
                /* if P type, zero motion vector is implied */
                mv_dir = MV_DIR_FORWARD;
                mv[ 0 ] = 0;
                mv[ 1 ] = 0;
                last_mv[ 0 ] = 0;
                last_mv[ 1 ] = 0;
                last_mv[ 2 ] = 0;
                last_mv[ 3 ] = 0;
            } else {
                /* if B type, reuse previous vectors and directions */
                mv[ 0 ] = last_mv[ 0 ];
                mv[ 1 ] = last_mv[ 1 ];
                mv[ 4 ] = last_mv[ 4 ];
                mv[ 5 ] = last_mv[ 5 ];
            }

            mb_intra = false;
            //TODO motion code
            return;
        }

        switch( pict_type ) {
            case I_TYPE: {
                if ( in.getTrueFalse() ) {
                    mb_type = Tables.MB_TYPE_INTRA;
                } else if ( in.getTrueFalse() ) {
                    mb_type = Tables.MB_TYPE_INTRA | Tables.MB_TYPE_QUANT;
                } else {
                    throw new MpegException( "Invalid mb type" );
                }
                break;
            }
            case P_TYPE: {
                mb_type = ptype2mb_type[ in.getVLC( mb_ptype_vlc ) ];
                break;
            }
            case B_TYPE: {
                mb_type = btype2mb_type[ in.getVLC( mb_btype_vlc ) ];
                break;
            }
        }

        if ( (Tables.MB_IS_INTRA_MASK & mb_type) != 0 ) {
            /**
             * This is an I-type macro block
             */
            if ( picture_structure == PICT_FRAME && !frame_pred_frame_dct ) {
                interlaced_dct = in.getTrueFalse();
            }
            if ( (Tables.MB_TYPE_QUANT & mb_type) != 0 ) {
                qscale = get_qscale();
            }

            if ( concealment_motion_vectors ) {
                if ( picture_structure != PICT_FRAME ) in.getTrueFalse();

                mv[ 0 ] = mpeg_decode_motion(mpeg_f_code[1], last_mv[0]);
                mv[ 1 ] = mpeg_decode_motion(mpeg_f_code[1], last_mv[1]);
                last_mv[0] = mv[0];
                last_mv[2] = mv[0];
                last_mv[1] = mv[1];
                last_mv[3] = mv[1];

                in.getTrueFalse();
            } else {
                last_mv[0] = 0;
                last_mv[2] = 0;
                last_mv[1] = 0;
                last_mv[3] = 0;
                last_mv[4] = 0;
                last_mv[6] = 0;
                last_mv[5] = 0;
                last_mv[7] = 0;
            }
            mb_intra = true;
            if (mpeg2) {
                for (int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                    mpeg2_decode_block_intra(block[i], i);
                }
            } else {
                for( int i = 0; i < 6; i++ ) {
                    mpeg1_decode_block_intra( block[i], i );
                }
            }
        } else {
            /**
             * This is a P type macro block
             */
            if ( (mb_type & Tables.MB_TYPE_ZERO_MV) != 0 ) {
                if ( picture_structure == PICT_FRAME && !frame_pred_frame_dct ) {
                    interlaced_dct = in.getTrueFalse();
                }
                
                if ( (Tables.MB_TYPE_QUANT & mb_type) != 0 ) {
                    qscale = get_qscale();
                }
                mv_dir = MV_DIR_FORWARD;
                mv_type = MV_TYPE_16X16;
                last_mv[0] = 0;
                last_mv[1] = 0;
                last_mv[2] = 0;
                last_mv[3] = 0;
                mv[0] = 0;
                mv[1] = 0;
            } else {
                if ( frame_pred_frame_dct ) {
                    motion_type = MT_FRAME;
                } else {
                    motion_type = in.getBits(2);
                }

                if ( picture_structure == PICT_FRAME && !frame_pred_frame_dct 
                     && ((mb_type & Tables.MB_TYPE_PAT) != 0)) {
                    interlaced_dct = in.getTrueFalse();
                }

                if ( (Tables.MB_TYPE_QUANT & mb_type) != 0 ) {
                    qscale = get_qscale();
                }

                /**
                 * Decode motion vectors for macro block
                 */
                mv_dir = 0;
                for ( int i = 0; i < 2; i++ ) {
                    if ( 0 == (mb_type & ((Tables.MB_TYPE_P0L0|Tables.MB_TYPE_P1L0) << (2 * i))) ) {
                        continue;
                    }

                    mv_dir |= (MV_DIR_FORWARD >> i);
                    switch ( motion_type ) {
                        case MT_FRAME: {
                            if ( picture_structure == PICT_FRAME ) {
                                /* MT_FRAME */
//                            System.out.println( "MT_FRAME" );
                                mb_type |= Tables.MB_TYPE_16x16;
                                mv_type = MV_TYPE_16X16;
                                mv[i * 4] = mpeg_decode_motion( mpeg_f_code[i* 2], 
                                                                  last_mv[i * 4] );
                                last_mv[i * 4]     = mv[i * 4];
                                last_mv[i * 4 + 2] = mv[i * 4];
                                mv[i * 4 + 1] = mpeg_decode_motion( mpeg_f_code[i * 2 + 1], 
                                                                    last_mv[i * 4 + 1] );
                                last_mv[i * 4 + 1] = mv[i * 4 + 1];
                                last_mv[i * 4 + 3] = mv[i * 4 + 1];
                                if ( full_pel[i] ) {
                                    mv[i * 4] <<= 1;                                    
                                    mv[i * 4 + 1] <<= 1;
                                }
                            } else {
                                /* MT_16X8 */
//                            System.out.println( "MT_16X8" );
                                mb_type |= Tables.MB_TYPE_16x8;
                                mv_type = MV_TYPE_16X8;
                                for ( int j = 0; j < 2; j++ ) {
                                    field_select[i * 2 + j] = in.getTrueFalse();
                                    for ( int k = 0; k < 2; k++ ) {
                                        mv[i * 4 + j * 2 + k] = mpeg_decode_motion( mpeg_f_code[i * 2 + k],
                                                                                    last_mv[i * 4 + j * 2 + k] );
                                        last_mv[i * 4 + j * 2 + k] = mv[i * 4 + j * 2 + k];
                                    }
                                }
                            }
                            break;
                        }
                        case MT_FIELD: {
//                            System.out.println( "MT_FIELD" );
                            mv_type = MV_TYPE_FIELD;
                            if ( picture_structure == PICT_FRAME ) {
                                mb_type |= Tables.MB_TYPE_16x8 | Tables.MB_TYPE_INTERLACED;
                                for ( int j = 0; j < 2; j++ ) {
                                    field_select[i * 2 + j] = in.getTrueFalse();
                                    last_mv[i * 4 + j * 2] = mpeg_decode_motion( mpeg_f_code[i * 2],
                                                                           last_mv[i * 4 + j * 2] );
                                    mv[i * 4 + j * 2] = last_mv[i * 4 + j * 2];
                                    last_mv[i * 4 + j * 2 + 1] = mpeg_decode_motion( mpeg_f_code[i * 2 + 1],
                                                                           last_mv[i * 4 + j * 2 + 1] >> 1 );
                                    mv[i * 4 + j * 2 + 1] = last_mv[i * 4 + j * 2 + 1];
                                    last_mv[i * 4 + j * 2 + 1] <<= 1;
                                }
                            } else {
                                mb_type |= Tables.MB_TYPE_16x16;
                                field_select[i * 2] = in.getTrueFalse();
                                for ( int k = 0; k < 2; k++ ) {
                                    last_mv[i * 4 + k] = mpeg_decode_motion( mpeg_f_code[i * 2 + k],
                                                                             last_mv[i * 4 + k] );
                                    last_mv[i * 4 + 2 + k] = last_mv[i * 4 + k];
                                    mv[i * 4 + k]          =  last_mv[i * 4 + k];
                                }
                            }
                            break;
                        }
                        case MT_DMV: {
                            System.out.println( "MT_DMV" );
                            break;
                        }   
                        default: {
                            System.out.println( "UNKNOWN" );
                            break;
                        }                            
                    }
                }
            }
            
            /**
             * Decode intra macro block
             */
            mb_intra = false;
            if ( (mb_type & Tables.MB_TYPE_PAT) != 0 ) {
                int cbp = in.getVLC( mb_pat_vlc );
                cbp++;
                if (mpeg2) {
                    for (int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                        if ( (cbp & 32) == 32 ) {
                            mpeg2_decode_block_non_intra(block[i], i);
                        } else {
                        }
                        cbp *= 2;
                    }
                } else {
                    for( int i = 0; i < 6; i++ ) {
                        if ( (cbp & 32) == 32 ) {
                            mpeg1_decode_block_inter( block[i], i);
                        } else {
                        }
                        cbp *= 2;
                    }
                }
            }        
        }
    }

    /**
     * Read the quantization scale
     */
    private int get_qscale() {
        int qscale;
        if (mpeg2) {
            if (q_scale_type) {
                qscale = non_linear_qscale[in.getBits(5)];
            } else {
                qscale = in.getBits(5) << 1;
            }
        } else {
            /* for mpeg1, we use the generic unquant code */
            qscale = in.getBits(5);
        }
        return qscale;
    }

    private void MPV_decode_mb() {
        /**
         * If this is not an I type macroblock the dc cache 
         * needs to be reset
         */
        if (!mb_intra) {
             last_dc[0] = 128 << intra_dc_precision;
             last_dc[1] = 128 << intra_dc_precision;
             last_dc[2] = 128 << intra_dc_precision;
        }
        
        /**
         * If we are skipping B frames go no further than this
         */
        if ( (skipBFrames || hurryUp) && pict_type == B_TYPE ) return;
        
        if ( !mb_intra ) {
            /**
             * P or B Type macroblock
             */
//            if ( pict_type == I_TYPE ) throw new Error( "non intra I frame" );

            int x = mb_x;
            int y = mb_y;
            
            /** 
             * Forward motion
             */
            boolean average = false;
            if ( (mv_dir & MV_DIR_FORWARD) != 0 ) {
                if ( pict_type == P_TYPE ) {
                    displayOutput.moveFromNext( x, y, mv, 0, false, average, mv_type, field_select, 0 );
                } else {
                    displayOutput.moveFromLast( x, y, mv, 0, true, average, mv_type, field_select, 0 );
                }
                average = true;
            }
            
            /**
             * Backward motion
             */
            if ( (mv_dir & MV_DIR_BACKWARD) != 0 ) {
                if ( pict_type == P_TYPE ) {
//                    throw new Error( "Backwards motion in P Frame" );
                } else {
                    displayOutput.moveFromNext( x, y, mv, 4, true, average, mv_type, field_select, 2 );
                }
            }

            /**
             * Add delta corrections
             */
            displayOutput.addLuminanceIdct( x * 2,     y * 2,     block[ 0 ], interlaced_dct );
            displayOutput.addLuminanceIdct( x * 2 + 1, y * 2,     block[ 1 ], interlaced_dct );
            displayOutput.addLuminanceIdct( x * 2,     y * 2 + 1, block[ 2 ], interlaced_dct );
            displayOutput.addLuminanceIdct( x * 2 + 1, y * 2 + 1, block[ 3 ], interlaced_dct );
            displayOutput.addRedIdct( x, y, block[ 5 ] );
            displayOutput.addBlueIdct( x, y, block[ 4 ] );
        } else {
            int x = mb_x;
            int y = mb_y;

            /**
             * I Type macroblock - just display it
             */
            displayOutput.putLuminanceIdct( x * 2,     y * 2,     block[ 0 ], interlaced_dct );
            displayOutput.putLuminanceIdct( x * 2 + 1, y * 2,     block[ 1 ], interlaced_dct );
            displayOutput.putLuminanceIdct( x * 2,     y * 2 + 1, block[ 2 ], interlaced_dct );
            displayOutput.putLuminanceIdct( x * 2 + 1, y * 2 + 1, block[ 3 ], interlaced_dct );
            displayOutput.putRedIdct( x, y, block[ 5 ] );
            displayOutput.putBlueIdct( x, y, block[ 4 ] );
        }
    }
    

    /**
     * Decode a slice (row of macroblocks)
     */
    private void mpeg_decode_slice(int sliceNumber ) throws FFMpegException {
        last_dc[0]=1 << (7 + intra_dc_precision);
        last_dc[1]=last_dc[0];
        last_dc[2]=last_dc[0];
        last_mv[0] = 0;
        last_mv[1] = 0;
        last_mv[2] = 0;
        last_mv[3] = 0;
        last_mv[4] = 0;
        last_mv[5] = 0;
        last_mv[6] = 0;
        last_mv[7] = 0;
        
        field_pic = (picture_structure != PICT_FRAME);
        //ff_mpeg1_clean_buffers;
        interlaced_dct = false;
        
        if ( first_slice ) {
            if ( first_field || !field_pic ) {
                repeat_pict = 0;
                if ( repeat_first_field ) {
                    if ( progressive_sequence ) {
                        repeat_pict = top_field_first ?4:2;
                    } else if ( progressive_frame ) {
                        repeat_pict = 1;
                    }
                }
            } else {
                for ( int i = 0; i < 4; i++ ) {
                    /* TODO picture_data */
                }
            }
        }
        first_slice = false;
        
/*
    System.out.println( "Slice: " + sliceNumber );
    System.out.println( "qp:" + qscale +
                                    " fc:" + mpeg_f_code[0][0] + " " + mpeg_f_code[0][1] + " " + mpeg_f_code[1][0] + " " + mpeg_f_code[1][1] + 
                                    " " + (pict_type == I_TYPE ? "I" : (pict_type == P_TYPE ? "P" : (pict_type == B_TYPE ? "B" : "S"))) +
                                    " " + (progressive_sequence ? "pro" :"") + 
                                    " " + (alternate_scan ? "alt" :"") +
                                    " " + (top_field_first ? "top" :"") +
                                    " dc:" + intra_dc_precision +
                                    " pstruct:" + picture_structure +
                                    " fdct:" + frame_pred_frame_dct +
                                    " cmv:" + concealment_motion_vectors +
                                    " qtype:" + q_scale_type +
                                    " ivlc:" + intra_vlc_format +
                                    " rff:" + repeat_first_field + " " + (chroma_420_type ? "420" :""));
*/

        qscale = get_qscale();

        /* Extra slice information */
        while ( in.getTrueFalse() ) {
            in.getBits( 8 );
        }
        
        mb_x = 0;
        int code;
        do {
            code = in.getVLC( mbincr_vlc );
            if ( code <= 33 ) mb_x += code;
        } while ( code >= 33 );

        resync_mb_x = mb_x;
        resync_mb_y = sliceNumber;
        mb_y =sliceNumber;

        /**
         * Initialise the quick lookup arrays
         * ff_init_block_index( mb_x, mb_y );
         */
        blockIndex[ 0 ] =   blockWrap[ 0 ] * ( mb_y * 2 + 1 ) - 1 + mb_x * 2;
        blockIndex[ 1 ] =   blockIndex[ 0 ] + 1;
        blockIndex[ 2 ] =   blockIndex[ 0 ] + blockWrap[ 0 ];
        blockIndex[ 3 ] =   blockIndex[ 2 ] + 1;
        blockIndex[ 4 ] =   blockWrap[ 0 ] * ( mbHeight * 2 + 2 )
                          + blockWrap[ 4 ] * ( mb_y + 1 ) + mb_x;
        blockIndex[ 5 ] =   blockIndex[ 4 ] + blockWrap[ 4 ] * (mbHeight + 2);
        
        mb_skip_run = 0;
        boolean endOfSlice = false;
        while (!endOfSlice && ((mb_y << (field_pic?1:0)) < mbHeight)) {
            if ( mb_x > mbWidth ) System.out.println( "X too large" );
            
            //clear_blocks
            for ( int i = 0; i < NUMBER_OF_BLOCKS; i++ ) {
                for ( int j = 0; j < 64; j++ ) block[i][j] = 0;
            }
            
            mpeg_decode_mb();
            
            if ( pict_type != B_TYPE ) {  //Note this is probably unnecessary
                int wrap = blockWrap[ 0 ];
                int xy = mb_x * 2 + 1 + ( mb_y * 2 + 1 ) * wrap;
                int motion_x;
                int motion_y;
                if ( mb_intra ) {
                    motion_x = 0;
                    motion_y = 0;
                } else if ( mv_type == MV_TYPE_16X16 ) {
                    motion_x = mv[0];
                    motion_y = mv[1];
                } else {
                    /* Field */
                    motion_x = mv[0] + mv[2];
                    motion_y = mv[1] + mv[3];
                    motion_x = (motion_x >> 1) | (motion_x & 1);
                }
                motion_val[ xy            ][ 0 ] = motion_x;
                motion_val[ xy            ][ 1 ] = motion_y;
                motion_val[ xy        + 1 ][ 0 ] = motion_x;
                motion_val[ xy        + 1 ][ 1 ] = motion_y;
                motion_val[ xy + wrap     ][ 0 ] = motion_x;
                motion_val[ xy + wrap     ][ 1 ] = motion_y;
                motion_val[ xy + wrap + 1 ][ 0 ] = motion_x;
                motion_val[ xy + wrap + 1 ][ 1 ] = motion_y;
            }
            
            MPV_decode_mb();
            if ( ++mb_x >= mbWidth ) {
                mb_x = 0;
                mb_y++;
                if((mb_y << (field_pic?1:0)) >= mbHeight){
                    endOfSlice = true;
                    break;
                }
            }
            
            /* Skip mb handling */
            if ( mb_skip_run == -1 ) {
                mb_skip_run = 0;
                do {
                    code = in.getVLC( mbincr_vlc );
                    if ( code <= 33 ) mb_skip_run += code;
                    if ( code == 35 ) {
                        endOfSlice = true;
                        break;
                    }
                } while ( code >= 33 );
            }
        }
        in.seek( ((in.getPos()/8)-2)*8 );
    }

    /**
     * Decode a picture frame
     */
    private int lastFullFrame;
    private boolean notConsumed = false;
    private boolean findSequenceHeader = true;

    public void decodeFrame( byte[] data, int size ) throws FFMpegException {
        /**
         * Manage data buffer
         */
        if ( !notConsumed ) {
            in.addData( data, 0, size );
            int end = in.getPos() + in.availableBits() - size * 8;
            
            /* Find full frame */
            lastFullFrame = 0;
            boolean sequenceFrame = false;
            boolean dropThisFrame = false;
            for ( int i = 0; i < size - 6; i++ ) {
                /**
                 * Extract pointers to frames 
                 */
                if (    data[ i     ] == 0
                     && data[ i + 1 ] == 0
                     && data[ i + 2 ] == 1 ) {
                    byte header = data[ i + 3 ];
                    if ( header == (byte)PICTURE_START_CODE ) {

                        /* Picture start code *
                        int reference = ((currentData[ i + 4 ] & 0xff) << 2) | ( (currentData[ i + 5 ] >> 6 ) & 0x03 );
                        int pict_code = (currentData[ i + 5 ] >>3) & 0x7;
                        System.out.println( pictName[pict_code] + " " + reference );
                        */
                        if ( !sequenceFrame ) {
                            lastFullFrame = i * 8 + end;
                        }
                        sequenceFrame = false;
                    }
                    if ( !sequenceFrame && (header == (byte)SEQ_START_CODE || header == (byte)GOP_START_CODE) ) {
                        sequenceFrame = true;
                        lastFullFrame = i * 8 + end;
                        findSequenceHeader = false;
                    }
                }
            }
        }
        if ( lastFullFrame == 0 || findSequenceHeader ) {
//            System.out.println( "Insufficient data" + lastFullFrame);
            pict_type = SKIP_FRAME_TYPE;
            lastFullFrame = 0;
            return;
        }
        notConsumed = false;
        
        /**
         * Until the end of frame (or we run out of data)
         */
        boolean endOfFrame = false;
        while ( !endOfFrame && in.availableBits() > 24 ) {
            /* Find current header */
            do {
                if ( in.showBits( 24 ) == SYNC_BYTES ) {
                    in.getBits( 24 );
                    currentHeader = in.getBits( 8 );
                } else {
                    in.getBits(8 -(in.getPos() %8));
                    currentHeader = -1;
                }
            } while ( currentHeader == -1 );

            switch ( currentHeader ) {
                case SEQ_START_CODE: {
//                    System.out.println( "SEQ_START_CODE" );
                    mpeg1_decode_sequence();
                    break;
                }
                case PICTURE_START_CODE: {
//                    System.out.println( "PICTURE_START_CODE" );
                    mpeg1_decode_picture();
                    break;
                }
                case EXT_START_CODE: {
//                    System.out.println( "EXT_START_CODE" );
                     mpeg_decode_extension();
                     break;
                }
                case USER_START_CODE: {
                    break;
                }
                case GOP_START_CODE: {
                    first_field = false;
                    break;
                }
                default: {
                    if (currentHeader >= SLICE_MIN_START_CODE &&
                        currentHeader <= SLICE_MAX_START_CODE) {
                        mpeg_decode_slice( currentHeader - SLICE_MIN_START_CODE );

                        if ( mb_y >= mbHeight ) { 
                            endOfFrame = true;
//                            System.out.println( "EOF" );
                        }
                    }
                    break;
                }
            }
        }
            
        /* Find next header Sequence header*/
        while ( in.showBits( 24 ) != SYNC_BYTES ) {
                in.getBits(8 -(in.getPos() %8));
        }
    }
    
    /**
     * Retrieve the supported input formats.  Currently "mpeg" 
     *
     * @return Format[] the supported input formats
     */
    public Format[] getSupportedInputFormats() {
        return new Format[] { new VideoFormat( "mpeg" ) };
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
    private VideoFormat inputFormat;
    public Format setInputFormat( Format format ) {
        inputFormat = (VideoFormat)format;
        initialise( (int)inputFormat.getSize().getWidth(), 
                    (int)inputFormat.getSize().getHeight() );
        if ( inputFormat.getFrameRate() > 0 ) {
            frameRate = inputFormat.getFrameRate();
        }
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
        return new RGBFormat( new Dimension( showInterlace ? mbWidth * 32 : mbWidth * 16, 
                                             showInterlace ? mbHeight * 8 : mbHeight * 16 ), 
                             -1, (new int[0]).getClass(),         // array
                             inputFormat.getFrameRate(),          // Frames/sec
                             32, 0xff0000, 0x00ff00, 0x0000ff) ;  //Colours 
    }
    
    /**
     * Converts a byte array Buffer of Mpeg1/2 video data
     * into an integer array Buffer of video data.
     * 
     * Always output one frame.
     *
     * @return BUFFER_PROCESSED_OK The output buffer contains a valid frame
     * @return BUFFER_PROCESSED_FAILED A decoding problem was encountered
     */
    private long lastTime;
    private int frames;
    private int skipToIFrame = 0;
    public int process( Buffer in, Buffer out ) {
        /* Flush buffer */
        if ( (in.getFlags() & Buffer.FLAG_FLUSH) != 0 ) reset();
        
        /* Do we need to set the time in the output buffer */
        out.setFlags( in.getFlags() );
        if ( (in.getFlags() & Buffer.FLAG_NO_WAIT) == 0 ) {
            /* Has the input buffer specified a new timestamp */
            if ( lastTime != in.getTimeStamp() ) {
                lastTime = in.getTimeStamp();
                frames = 0;
            } else {
                /* Calculate time depending on frame rate */
                out.setFlags( in.getFlags() | Buffer.FLAG_RELATIVE_TIME | Buffer.FLAG_NO_DROP );
            }
            out.setTimeStamp( in.getTimeStamp() + (long)(((long)1000000000)/frameRate) * frames);
        }
        
        try {
            byte[] data = (byte[])in.getData();
            decodeFrame( data, in.getLength() );

            if ( pict_type == I_TYPE || pict_type == P_TYPE) {
                /*
                 * P/I type - these become the "next" frame
                 *
                 * Display the last I or P frame we decoded
                 */
                displayOutput.showNextScreen(out);
                displayOutput.endIPFrame();
            } else if ( pict_type == B_TYPE ) {
                /*
                 * B frame - simply show the current frame (it will be discarded)
                 */
                if ( !skipBFrames && !hurryUp ) {
                    displayOutput.showScreen(out);
                    displayOutput.endBFrame();
                } else {
                    displayOutput.endBFrame();
                    out.setLength(0);
                }
            } else {
                /**
                 * We do not have enough data for a full frame - freeze
                 */
                out.setLength(0);
                numberOfFramesDelivered = 0;
                return BUFFER_PROCESSED_OK;
            }                
            
            /* Manage video discontinuity */
            if ( pict_type == I_TYPE && skipToIFrame != 0 ) skipToIFrame--;
            if ( pict_type == P_TYPE && skipToIFrame == 1 ) skipToIFrame--;
            if ( skipToIFrame != 0 ) {
                out.setLength(0);
                out.setFlags( Buffer.FLAG_NO_WAIT );
                frames = 0;
            }

        } catch( Error e ) {
            e.printStackTrace();
            this.in = new BitStream();
            out.setLength(0);
            return BUFFER_PROCESSED_OK;
        } catch ( Exception e ) {
            e.printStackTrace();
            this.in = new BitStream();
//            this.in.seek( this.in.getPos() + this.in.availableBits() );
            out.setLength(0);
            return BUFFER_PROCESSED_OK;
        }

        numberOfFramesDelivered++;
        frames++;
        if ( this.in.getPos() < lastFullFrame ) {
            
/*            System.out.println( "End of frame at " + this.in.getPos()
                              + ".  Last frame is " + lastFullFrame + " " 
                              +   (this.in.getPos() + this.in.availableBits())
                              + " remaining " + this.in.availableBits());  */
            notConsumed = true;
            return INPUT_BUFFER_NOT_CONSUMED;
        } else {
//            System.out.println( "End of buffer at " + this.in.getPos() + ".  Last frame is " + lastFullFrame + " " + Integer.toHexString( this.in.showBits(32) ) + " remaining " + this.in.availableBits());
            hurryUp = false;
            numberOfFramesDelivered = 0;
            return BUFFER_PROCESSED_OK;
        }
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
        frames = 0;
        in.seek( in.getPos() + in.availableBits() ) ;
        skipToIFrame = 2;
    }
    
    /**
     * Retrives the name of this video codec: "MPEG video decoder"
     * @return Codec name
     */
    public String getName() {
        return "MPEG video decoder";
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
	setInputFormat( new VideoFormat( "MPEG", size, -1, (new byte[ 0 ]).getClass(), 0 ) );
    }

    public void setEncoding( String encoding ) {
    }

    public void setIsRtp( boolean isRtp ) {
    }

    public void setIsTruncated( boolean isTruncated ) {
    }
}
