/*
 * This is a Java port of the a52dec audio codec,a free ATSC A-52 stream decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (C) 2000-2003 Michel Lespinasse <walken@zoy.org>
 * Copyright (C) 1999-2000 Aaron Holtzman <aholtzma@ess.engr.uvic.ca>
 *
 * a52dec is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * a52dec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.codecs.audio.ac3;

import java.awt.Dimension;

import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.Buffer;

import net.sourceforge.jffmpeg.JMFCodec;
import net.sourceforge.jffmpeg.GPLLicense;
import net.sourceforge.jffmpeg.codecs.audio.ac3.data.Tables;

import net.sourceforge.jffmpeg.codecs.utils.BitStream;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;

/**
 * AC3 Codec
 */
public class AC3Decoder implements Codec, GPLLicense, JMFCodec {
    public static final boolean debug = false;
    int bias = 0;

    
    public static final int SYNC_BYTES = 0x0b77;
    public static final int HEADER_LENGTH = 7;
    
    public static final int A52_CHANNEL      = 0;
    public static final int A52_MONO         = 1;
    public static final int A52_STEREO       = 2;
    public static final int A52_3F           = 3;
    public static final int A52_2F1R         = 4;
    public static final int A52_3F1R         = 5;
    public static final int A52_2F2R         = 6;
    public static final int A52_3F2R         = 7;
    public static final int A52_CHANNEL1     = 8;
    public static final int A52_CHANNEL2     = 9;
    public static final int A52_DOLBY        = 10;
    public static final int A52_CHANNEL_MASK = 15;

    public static final int A52_LFE          = 16;
    public static final int A52_ADJUST_LEVEL = 32;
    
    private static double LEVEL_PLUS6DB = 2.0;
    private static double LEVEL_PLUS3DB = 1.4142135623730951;
    private static double LEVEL_3DB     = 0.7071067811865476;
    private static double LEVEL_45DB    = 0.5946035575013605;
    private static double LEVEL_6DB     = 0.5;

    
    private static final int EXP_REUSE = 0;
    private static final int EXP_D15   = 1;
    private static final int EXP_D25   = 2;
    private static final int EXP_D45   = 3;
    
    private static final int DELTA_BIT_REUSE    = 0;
    private static final int DELTA_BIT_NEW      = 1;
    private static final int DELTA_BIT_NONE     = 2;
    private static final int DELTA_BIT_RESERVED = 3;
    /*
     * Header information 
     */
    private int flags;
    private int sample_rate;
    private int bit_rate;
    private int frame_length;   // Including header
    
    /*
     * Frame information
     */
    private int fscod;
    private int halfrate;
    private int acmod;
    private double clev, slev;
    private boolean lfeon;
    
    private int language;
    
    /* Internal state */
    public static final int MAX_CHANNELS = 5;
    public static final int MAX_BANDS    = 18;
    public static final int EXPONENT_SIZE = 256;
    public static final int MAX_DELT_BA_SIZE = 50;
    
    private double level = 200;
    public boolean dynrnge = false;
    private double dynrng;
    private int chincpl;
    private boolean phsflginu;
    private int ncplbnd;
    private int cplstrtbnd;
    private int cplstrtmant;
    private int cplendmant;
    private int cplbndstrc;
    private double[][] cplco = new double[ MAX_CHANNELS ][ MAX_BANDS ];
    private int rematflg;
    private int[] endmant = new int[ MAX_CHANNELS ];
//    private int cplextstr;
    private byte[]   cpl_expbapExp = new byte[ EXPONENT_SIZE ];
    private byte[][] fbw_expbapExp = new byte[ MAX_CHANNELS ][ EXPONENT_SIZE ];
    private byte[]   lfe_expbapExp = new byte[ EXPONENT_SIZE ];
    private byte[]   cpl_expbapBap = new byte[ EXPONENT_SIZE ];
    private byte[][] fbw_expbapBap = new byte[ MAX_CHANNELS ][ EXPONENT_SIZE ];
    private byte[]   lfe_expbapBap = new byte[ EXPONENT_SIZE ];
    private int bai;
    private int   cplbaBai;
    private int   cplbaDeltbae;
    private int[] cplbaDeltba = new int[ MAX_DELT_BA_SIZE ];
    private int[] baBai     = new int[ MAX_CHANNELS ];
    private int[] baDeltbae = new int[ MAX_CHANNELS ];
    private int[][] baDeltba = new int[ MAX_CHANNELS ][ MAX_DELT_BA_SIZE ];
    private int   lfebaBai;
    private int   lfebaDeltbae;  //Note DeltBae is always DELTA_BIT_NONE for lfe
    private int[] lfebaDeltba = new int[ MAX_DELT_BA_SIZE ];
    private int csnroffst;
    private int cplfleak;
    private int cplsleak;
    private int lfsr_state = 1;
    
    private Quantizer quant = new Quantizer();
    
    private double[] samplesOut = new double[ (MAX_CHANNELS + 2) * 256 * 2];
    private boolean downmixed;
    
    BitStream in = new BitStream();
    
    
    public static final int[] halfRate = {0,0,0,0,0,0,0,0,0,1,2,3};
    public static final int[] rate     = { 32,  40,  48,  56,  64,  80,  96, 112,
			                   128, 160, 192, 224, 256, 320, 384, 448,
			                   512, 576, 640};
    public static final int[] lfeonValues    = {0x10, 0x10, 0x04, 0x04, 0x04, 0x01, 0x04, 0x01};
    public static final double[] clevValues = { LEVEL_3DB, LEVEL_45DB, LEVEL_6DB, LEVEL_45DB };
    public static final double[] slevValues = { LEVEL_3DB, LEVEL_6DB, 0, LEVEL_6DB };

    public final byte[] exp_1 = Tables.getExponentTable1();
    public final byte[] exp_2 = Tables.getExponentTable2();
    public final byte[] exp_3 = Tables.getExponentTable3();
    
    public final int[] dither_lutp = Tables.getDitherLoopupTable();
    
    public final double[] scale_factor = Tables.getScaleFactors();
    public final double[] q_1_0 = Tables.getQ10Table();
    public final double[] q_1_1 = Tables.getQ11Table();
    public final double[] q_1_2 = Tables.getQ12Table();
    public final double[] q_2_0 = Tables.getQ20Table();
    public final double[] q_2_1 = Tables.getQ21Table();
    public final double[] q_2_2 = Tables.getQ22Table();
    public final double[] q_3   = Tables.getQ3Table();
    public final double[] q_4_0 = Tables.getQ40Table();
    public final double[] q_4_1 = Tables.getQ41Table();
    public final double[] q_5   = Tables.getQ5Table();
    
    SoundOutput soundOutput = new SoundOutput();
    /**
     * Read synchronisation bytes and header
     */
    private boolean a52_syncinfo() throws FFMpegException {
        /* Check for synchronisation block */
        while ( in.showBits( 16 ) != SYNC_BYTES ) {
            in.getBits(8);
//            System.out.println( "Jump" );
            //throw new Mpeg4Exception( "Not sync block" );
            if ( in.availableBits() <= HEADER_LENGTH * 8 ) return false;
        }
        in.getBits(16);
        
        in.getBits( 16 );                   //Skip
        int byte4 = in.getBits( 8 );
        int byte5 = in.getBits( 8 );
        int byte6 = in.getBits( 8 );
        
        int half = halfRate[ byte5 >> 3 ];
        int acmod = byte6 >> 5;
        
        flags =   (((byte6 & 0xf8) == 0x50) ? A52_DOLBY : acmod )
                | (((byte6 & lfeonValues[ acmod ]) != 0) ? A52_LFE   : 0 );
                
        int frmsizecod = byte4 & 63;
        if ( frmsizecod >= 38 ) throw new AC3Exception( "Unknown rate" );
        bit_rate = (rate[ frmsizecod >> 1 ] * 1000) >> half;
        
        switch ( byte4 & 0xc0 ) {
            case 0x00: {
                sample_rate = 48000 >> half;
                frame_length = 4 * rate[ frmsizecod >> 1 ];
                break;
            }
            case 0x40: {
                sample_rate = 44100 >> half;
                frame_length = 2 * ( 320 * rate[ frmsizecod >> 1 ] / 147 + (frmsizecod & 1) );
                break;
            }
            case 0x80: {
                sample_rate = 32000 >> half;
                frame_length = 6 * rate[ frmsizecod >> 1 ];
                break;
            }
            default: {
                throw new AC3Exception( "Unrecognised sample rate multiplier" );
            }
        }
        

if ( debug) System.out.println( "Sync - flags:" + Integer.toHexString( flags )
                            + " sample_rate:" + sample_rate + " bit_rate:" + bit_rate );
        
        in.seek( in.getPos() - 56 );  //Return to start of frame
        return true;
    }
    

    /* Read a frame of data */
    private void a52_frame() throws FFMpegException {
        if ( debug ) System.out.println( "a52_frame" + in.getPos() );
        in.getBits( 16 + 16 );
        fscod = in.getBits( 3 );
        in.getBits( 5 );
        int halfRateIndex = in.getBits( 5 );
        if ( halfRateIndex >= halfRate.length ) throw new FFMpegException( "Illegal half rate" );
        halfrate = halfRate[ halfRateIndex ];
        in.getBits( 3 );
        acmod = in.getBits( 3 );
        if ( (acmod == 2) && (in.getBits(2) == 2) ) { 
            //acmod = A52_DOLBY;  /* !!!! Note this is a local variable in the C version !!!! */
        }
        
        clev = 0;
        if ( ((acmod & 1) != 0) && (acmod != 1) ) {
            clev = clevValues[ in.getBits(2) ];
        }

        slev = 0;
        if ( (acmod & 4) != 0) {
            slev = slevValues[ in.getBits(2) ];
        }
        if ( debug ) System.out.println( "clev " + show_sample(clev) + " slev " + show_sample(slev) );
        
        lfeon = in.getTrueFalse();

        level = 2;
        if ( debug ) System.out.println( "level " + show_sample(level) );
        downmix_init(acmod);
        if ( debug ) System.out.println( "bias " + show_sample(bias) );
        
        //set level and bias
        level *= 2;
        dynrng = level;
        dynrnge = false;    //No callback
        //SET DELTA_BIT_NONE
        
        boolean repeat = (acmod == 0);
        do {
            in.getBits( 5 );
            if ( in.getTrueFalse() ) in.getBits( 8 );   //Compression
            if ( in.getTrueFalse() ) language = in.getBits( 8 );   //Language code
            if ( in.getTrueFalse() ) in.getBits( 7 );   //Mix level and room type
            repeat = !repeat;
        } while ( !repeat );
        in.getBits( 2 );  //Copyright and original bits
        
        if ( in.getTrueFalse() ) in.getBits( 14 );   //Time code 1
        if ( in.getTrueFalse() ) in.getBits( 14 );   //Time code 2
        
        if ( in.getTrueFalse() ) {                   //Skip additionl data
            int addbsil = in.getBits( 6 );
            in.seek( in.getPos() + addbsil * 8 );
        }
    }

    public static final int[] nfchansValues    = new int[] {2, 1, 2, 3, 3, 4, 4, 5, 1, 1, 2};
    public static final int[] cplstrtbndValues = new int[] { 31, 35, 37, 39, 41, 42, 43, 44,
                                                             45, 45, 46, 46, 47, 47, 48, 48 };
    public static final int[] rematrix_band    = new int[] {25, 37, 61, 253};

    private void a52_block() throws FFMpegException {
        /* Number of channels */
        int nfchans = nfchansValues[ acmod ];
if ( debug && nfchans != 2 ) System.out.println( "nfchans " + nfchans + " acmod " + acmod );
        /* Read block switch */
        boolean[] blksw = new boolean[ 5 ];
        for ( int i = 0; i < nfchans; i++ ) {
            blksw[i] = in.getTrueFalse();
if ( debug ) System.out.println( "blksw[" + i + "]=" + (blksw[i]? 1:0) );
        }
        
        /* Read dither flags */
        boolean[] dithflag = new boolean[ 5 ];
        for ( int i = 0; i < nfchans; i++ ) {
            dithflag[i] = in.getTrueFalse();
if ( debug ) System.out.println( "dithflag[" + i + "]=" + (dithflag[i]? 1:0) );
        }
        
        /* Read dynrng */
        boolean repeat = (acmod == 0);
        do {
            /* Read dynrng */
            if ( in.getTrueFalse() ) {
                int dynrngLocal = bitstream_get_2(8);
                if ( dynrnge ) {
                    if ( debug ) System.out.println( "dynrnge" );
                    dynrng = ((( dynrngLocal & 0x1f ) | 0x20) << 13) * scale_factor[ 2 - (dynrngLocal >> 5) ] * level;
                }
                // CALL dynrngcall
                if ( debug ) System.out.println( "dynrng=" + dynrng );
            }
            if ( debug ) System.out.println( "dynrngLoop" );
            repeat = !repeat;
        } while ( !repeat );
        if ( debug ) System.out.println( "clev " + show_sample(clev) + " slev " + show_sample(slev) );
        
        /* Read cplstre */
        if ( in.getTrueFalse() ) {
            chincpl = 0;
            if ( in.getTrueFalse() ) {
                for ( int i = 0; i < nfchans; i++ ) {
                    chincpl |= in.getBits(1) << i;
                }
if (debug) System.out.println( "chincpl=" + chincpl );
                switch( acmod ) {
                    case 0:
                    case 1: {
                        throw new AC3Exception( "Invalid mode" );
                    }
                    case 2: {
                        phsflginu = in.getTrueFalse();
                        break;
                    }
                }
                
                int cplbegf = in.getBits( 4 );
                int cplendf = in.getBits( 4 );
if ( debug ) System.out.println( "cplbegf=" + cplbegf );
                if ( cplendf + 3 - cplbegf < 0 ) throw new AC3Exception( "Invalid values" );
                
                ncplbnd     = cplendf + 3 - cplbegf;
                cplstrtbnd  = cplstrtbndValues[ cplbegf ];
                cplstrtmant = cplbegf * 12 + 37;
                cplendmant  = cplendf * 12 + 73;
                
                /* Read cplbndstrc */
                cplbndstrc  = 0;
                int ncplsubnd = ncplbnd;
                for ( int i = 0; i < ncplsubnd - 1; i++ ) {
                    if ( in.getTrueFalse() ) {
                        cplbndstrc |= 1 << i;
                        ncplbnd--;
                    }
                }
            }
        }
        
        /* Read cplinu */
        if ( chincpl != 0 ) {
            boolean cplcoe = false;
            for ( int i = 0; i <nfchans; i++ ) {
                if ( ((chincpl >> i ) & 1) != 0 ) {
                    if ( in.getTrueFalse() ) {
                        cplcoe = true;
                        int mstrcplco = 3 * in.getBits(2);
                        for ( int j = 0; j < ncplbnd; j++ ) {
                            int cplcoexp  = in.getBits(4);
                            int cplcomant = in.getBits(4);
                            if ( cplcoexp == 15) {
                                cplcomant <<= 14;
                            } else  {
                                cplcomant = (cplcomant | 0x10) << 13;
                            }
                            cplco[ i ][ j ] = cplcomant * scale_factor[cplcoexp + mstrcplco];
if ( debug ) System.out.println( "i="+i+" j="+j+" cplco=" + cplco[i][j] );
                        }
                    }
                }
            }
            if ( acmod == 2 && phsflginu && cplcoe ) {
                for ( int j = 0; j < ncplbnd; j++ ) {
                    if ( in.getTrueFalse() ) {
                        cplco[ 1 ][ j ] = - cplco[ 1 ][ j ];
                    }
                }
            }
        }
        
        /* Read rmatstr */
       if ( (acmod == 2) && in.getTrueFalse() ) {
           rematflg = 0;
           int end = (chincpl != 0) ? cplstrtmant : 253;
           int i = 0;
           do {
               rematflg |= in.getBits(1) << i;
      if ( debug ) System.out.println( "rematflg["+ i + "]=" + ((rematflg>>i)&1) );
           } while ( rematrix_band[ i++ ] < end );
        }
        
        /* cplexpstr */
        int cplexpstr = EXP_REUSE;
        int lfeexpstr = EXP_REUSE;
        if ( chincpl != 0 ) {
            cplexpstr = in.getBits(2);
if ( debug ) System.out.print( "cplextstr=" + cplexpstr + " " );
        }
        int[] chexpstr = new int[ 5 ];
        for ( int i = 0; i < nfchans; i++ ) {
            chexpstr[i] = in.getBits(2);
if ( debug ) System.out.print( "chextstr=" + chexpstr[i] + " " );
        }
        if ( lfeon ) {
            lfeexpstr = in.getBits( 1 );
if ( debug ) System.out.print( "lfeexpstr=" + lfeexpstr + " " );
        }
        
if ( debug ) System.out.println();
        for ( int i = 0; i < nfchans; i++ ) {
            if ( chexpstr[i] != EXP_REUSE ) {
                if (((chincpl >> i) & 1) != 0) {
                    endmant[i] = cplstrtmant;
                } else {
                    int chbwcod = in.getBits(6);
                    if ( chbwcod > 60 ) throw new AC3Exception( "chbwcod too large" );
                    endmant[i] = chbwcod * 3 + 73;
                }
if ( debug ) System.out.println( "endmant[" + i + "]=" + endmant[i] );
            }
        }
        
        int do_bit_alloc = 0;
if ( debug ) System.out.println( "cplendmant=" + cplendmant );
if ( debug ) System.out.println( "cplstrtmant=" + cplstrtmant );
        if ( cplexpstr != EXP_REUSE ) {
            do_bit_alloc = 0x40;
            int ncplgrps  = (cplendmant - cplstrtmant)/(3 << (cplexpstr - 1));
            byte cplabsexp = (byte)(in.getBits( 4 ) << 1);
            parse_exponents( cplexpstr, ncplgrps, cplabsexp, cpl_expbapExp, cplstrtmant );
        }
        for ( int i = 0; i < nfchans; i++ ) {
            if ( chexpstr[i] != EXP_REUSE ) {
                do_bit_alloc |= 1 << i;
                int grp_size = 3 << (chexpstr[i] - 1);
                int nchgrps = (endmant[i] + grp_size - 4)/grp_size;
                fbw_expbapExp[i][0] = (byte)in.getBits(4);
                parse_exponents( chexpstr[i], nchgrps, fbw_expbapExp[i][0], 
                                 fbw_expbapExp[i], 1 );
                in.getBits( 2 );  /* gainrng */
            }
        }
        if ( lfeexpstr != EXP_REUSE ) {
            do_bit_alloc |= 0x20;
            lfe_expbapExp[0] = (byte)in.getBits( 4 );
            parse_exponents( lfeexpstr, 2, lfe_expbapExp[0], lfe_expbapExp, 1 );
        }
        
        /* Read baie */
        if ( in.getTrueFalse() ) {
            do_bit_alloc = 0x7f;
            bai = in.getBits( 11 );
if ( debug ) System.out.println( "bai=" + (bai & 7));
        }
        
        /* Read snroffst */
        if ( in.getTrueFalse() ) {
            do_bit_alloc = 0x7f;
            csnroffst = in.getBits( 6 );
            if ( chincpl != 0 ) {
                cplbaBai = in.getBits( 7 );
if ( debug ) System.out.println( "cplbaBai=" + (cplbaBai & 7));
            }
            for ( int i = 0; i < nfchans; i++ ) {
                baBai[i] = in.getBits( 7 );
if ( debug ) System.out.println( "baBai=" + (baBai[i] &7));
            }
            if ( lfeon ) {
                lfebaBai = in.getBits( 7 );
if ( debug ) System.out.println( "lfebaBai=" + (lfebaBai &7));
            }
        }
        
        /* Read cplleak */
        if ( (chincpl != 0) && in.getTrueFalse() ) {
            do_bit_alloc |= 0x40;
            cplfleak = 9 - in.getBits( 3 );
            cplsleak = 9 - in.getBits( 3 );
        }
        
        /* Read deltbaie */
        if ( in.getTrueFalse() ) {
            do_bit_alloc = 0x7f;
            if ( chincpl != 0 ) {
                cplbaDeltbae = in.getBits( 2 );
            }
            for ( int i = 0; i < nfchans; i++ ) {
                baDeltbae[ i ] = in.getBits( 2 );
            }
            if ( chincpl != 0 && cplbaDeltbae == DELTA_BIT_NEW ) {
                parse_deltba( cplbaDeltba );
            }
            for ( int i = 0; i < nfchans; i++ ) {
                if ( baDeltbae[i] == DELTA_BIT_NEW ) {
                    parse_deltba( baDeltba[i] );
                }
            }
        }
        
        if (debug ) System.out.println( "clev " + show_sample(clev) + " slev " + show_sample(slev) );
        /* Manage memory allocation or clearing */
        if ( do_bit_alloc != 0 ) {
            if (zero_snr_offsets (nfchans)) {
                for ( int i = 0; i < cpl_expbapBap.length; i++ ) {
                    cpl_expbapBap[i] = 0;
                }
                for ( int j = 0; j < nfchans; j++ ) {
                    for ( int i = 0; i < fbw_expbapBap[j].length; i++ ) {
                        fbw_expbapBap[j][i] = 0;
                    }
                }
                for ( int i = 0; i < lfe_expbapBap.length; i++ ) {
                    lfe_expbapBap[i] = 0;
                }
            } else {
                if ( (chincpl != 0) && ((do_bit_alloc & 64) != 0) ) {
                    a52_bit_allocate ( cplbaBai, cplbaDeltbae, cplbaDeltba,
                                   cplstrtbnd, cplstrtmant, cplendmant,
                                   cplfleak << 8, cplsleak << 8,
                                   cpl_expbapExp, cpl_expbapBap );
                }
                for (int i = 0; i < nfchans; i++)
                    if ((do_bit_alloc & (1 << i)) != 0)
                        a52_bit_allocate (baBai[i], baDeltbae[i], baDeltba[i],
                                     0, 0, endmant[i], 
                                     0, 0, 
                                     fbw_expbapExp[i], fbw_expbapBap[i]);
                if ( lfeon && ((do_bit_alloc & 32) != 0)) {
                    lfebaDeltbae = DELTA_BIT_NONE;
                    a52_bit_allocate (lfebaBai, lfebaDeltbae, lfebaDeltba,
                                  0, 0, 7, 
                                  0, 0, 
                                  lfe_expbapExp, lfe_expbapBap);
                }
            }
        }
        
        if ( in.getTrueFalse() ) {
            int i = in.getBits( 9 );
            in.seek( in.getPos() + i * 8 );
            if ( debug ) System.out.println( "Skip " + i );
        }
        
        int samplesPointer = 0;
//        if ( output & A52_LFE )
            samplesPointer += 256;
            
        double[] coeff = new double[ 5 ];
        if ( debug ) System.out.println( "dynrng " + show_sample(dynrng) + " clev " + show_sample(clev) + " slev " + show_sample(slev) );
        a52_downmix_coeff( coeff, acmod, dynrng, clev, slev );
        
        
        boolean done_cpl = false;
        int j;
        quant.setQ1Pointer( -1 );
        quant.setQ2Pointer( -1 );
        quant.setQ4Pointer( -1 );
        for ( int i = 0; i < nfchans; i++ ) {
            coeff_get( samplesOut, samplesPointer + 256 * i,    // Output
                       fbw_expbapExp[i], fbw_expbapBap[i],    // Work area
                       quant,
                       coeff[ i ], dithflag[ i ], endmant[ i ] );
            if ( ((chincpl >> i) & 1) != 0 ) {
                if ( !done_cpl ) {
                    done_cpl = true;
                    coeff_get_coupling( nfchans, coeff, 
                                        samplesOut, samplesPointer, quant, dithflag ); 
                }
                j = cplendmant;
            } else {
                j = endmant[i];
            }
            
            do {
                samplesOut[ samplesPointer + 256*i +j ] = 0;
            } while ( ++j < 256 );
        }
        
if ( debug ) {
    System.out.println( "point1" );
    for ( int ch = 0; ch < nfchans; ch++ ) {
        System.out.println( "\nChannel " + ch );
        for ( int i = 0; i < 256; i++ ) {
            System.out.print( show_sample( samplesOut[ samplesPointer + i + 256 * ch] ) + " " );
        }
    }
    System.out.println();
}
        
        if ( acmod == 2 ) {
            int i = 0;
            j = 13;
            int end = ( endmant[ 0 ] < endmant[ 1 ] ) ? endmant[ 0 ] : endmant[ 1 ];
            int rematflgLocal = rematflg;
            do {
                if ( (rematflgLocal & 1) == 0 ) {
                    rematflgLocal >>= 1;
                    j = rematrix_band[ i++ ];
                    continue;
                }
                rematflgLocal >>= 1;
                int band = rematrix_band[ i++ ];
                if ( band > end ) band = end;
                do {
//System.out.println( "Reorder " + j );
                    double tmp0 = samplesOut[ samplesPointer + j ];
                    double tmp1 = samplesOut[ samplesPointer + j + 256 ];
                    samplesOut[ samplesPointer + j       ] = tmp0 + tmp1;
                    samplesOut[ samplesPointer + j + 256 ] = tmp0 - tmp1;
                } while ( ++j < band );
            } while ( j < end );
        }
        
if ( debug ) {
    System.out.println( "point2" );
    for ( int ch = 0; ch < nfchans; ch++ ) {
        System.out.println( "\nChannel " + ch );
        for ( int i = 0; i < 256; i++ ) {
            System.out.print( show_sample( samplesOut[ samplesPointer + i + 256 * ch] ) + " " );
        }
    }
    System.out.println();
}
        if ( lfeon ) {
//            if ( output & A52_LFE )
            {
                coeff_get( samplesOut, samplesPointer - 256, 
                           lfe_expbapExp, lfe_expbapBap,
                           quant,
                           0, false, 7 );
                for ( int i = 7; i < 256; i++ ) {
                    samplesOut[ samplesPointer - 256 + i ] = 0;
                }
                soundOutput.a52_imdct_512( samplesOut, samplesPointer - 256, samplesPointer -256 + 1536, bias );
            }
        }
        
        int i = 0;
        /* Stereo or 5.2 */
//        if ( nfchans_tbl[ output & A52_CHANNEL_MASK ] < nfchans )
        if ( 2 < nfchans )
        {
            for ( i = 1; i < nfchans; i++ ) {
                if (blksw[i] != blksw[0] ) break;
            }
        }
        
        if ( i < nfchans ) {
            if ( debug ) System.out.println( "i < nfchans" );
            if ( downmixed ) {
                downmixed = false;
//                a52_upmix( samplesOut, 1536, acmod, output );
            }
            
            for ( i = 0; i < nfchans; i++ ) {
//                if ( (chanbias & (1<<i)) == 0 ) {
//                    bias = this.bias;
//                }
                
                if ( coeff[i] != 0 ) {
                    if ( blksw[i] ) {
                        soundOutput.a52_imdct_256( samplesOut, samplesPointer + 256 * i, 
                                                   samplesPointer + 1536 + 256 * i, bias );
                    } else {
                        soundOutput.a52_imdct_512( samplesOut, samplesPointer + 256 * i, 
                                       samplesPointer + 1536 + 256 * i, bias );
                    }
                } else {
                    for ( j = 0; j < 256; j++ ) {
                        samplesOut[ samplesPointer + 256 * i + j ] = 0;
                    }
                }
            }
            //a52_downmix( );
        } else {
            if ( debug ) System.out.println( "i >= nfchans" );
            
            int bias = 0;
            //nfchans
            //a52_downmix( );
            if ( !downmixed ) {
                downmixed = true;
                //a52_downmix
            }

            if ( blksw[ 0 ] ) {
                for ( i = 0; i < nfchans; i++ ) {
                    soundOutput.a52_imdct_256( samplesOut, samplesPointer + 256 * i, 
                                               samplesPointer + 1536 + 256 * i, bias );
                }
            } else {
                for ( i = 0; i < nfchans; i++ ) {
                    soundOutput.a52_imdct_512( samplesOut, samplesPointer + 256 * i, 
                                               samplesPointer + 1536 + 256 * i, bias );
                }
            }
        }          
    }

    private static final int[] slowgainValues = new int[] { 0x540, 0x4d8, 0x478, 0x410 };
    private static final int[] dbpbValues = new int[] { 0xc00, 0x500, 0x300, 0x100 };
    private static final int[] floorValues = new int[] { 0x910, 0x950, 0x990, 0x9d0,
                                                         0xa10, 0xa90, 0xb10, 0x1400 };
    private static final int[][] hthValues = Tables.getBitAllocHthTable();
    private static final int[] zeroBaArray = new int[ MAX_DELT_BA_SIZE ];
    private static final byte[] bapTable = Tables.getBitAllocBapTable();
    private static final int[] bndTable = Tables.getBitAllocBndTable();
    private static final int[] laTable  = Tables.getBitAllocLaTable();   
        
    private void a52_bit_allocate ( int baBai, int deltbae, int[] deltba,
                                    int bndstart, int start, int end,
                                    int fastleak, int slowleak, 
                                    byte[] expbapExp, byte[] expbapBap ) {
       byte[] exp = expbapExp;
       byte[] bap = expbapBap;
       
       if ( debug ) System.out.println( "bit_alloc" );
       
       int fdecay = ( 63 + 20 * ((this.bai >> 7) & 3)) >> halfrate;
       int fgain  = 128 + 128 * (baBai & 7);
       int sdecay = (15 + 2 * (this.bai >> 9)) >> halfrate;
       int sgain  = slowgainValues[ (this.bai >> 5 ) & 3 ];
       int dbknee = dbpbValues[ ( this.bai >> 3 ) & 3 ];
       int[] hth    = hthValues[ fscod ];
       if ( deltbae == DELTA_BIT_NONE ) deltba = zeroBaArray;
       int floor = floorValues[ this.bai & 7 ];
       int snroffset = 960 - 64 * csnroffst - 4 * ( baBai >> 3) + floor;
       floor >>= 5;
       
       int psd;
       int mask;
       
       int i = bndstart;
       int j = start;
       if ( start == 0 ) {
           /* Not the coupling channel */
           int lowcomp = 0;
           j = end - 1;
           do {
               if ( i < j ) {
                   if ( exp[ i + 1 ] == exp[ i ] - 2 ) {
                       lowcomp = 384;
                   } else if ( (lowcomp != 0) && exp[ i + 1 ] > exp[ i ] ) {
                       lowcomp -= 64;
                   }
               }
               psd = 128 * exp[i];
               mask = psd + fgain + lowcomp;
               /* COMPUTE_MASK */
               if ( psd > dbknee )               mask -= (psd - dbknee)>>2;
               if ( mask > hth[ i >> halfrate] ) mask = hth[ i >> halfrate ];
               mask -= snroffset + 128 * deltba[ i ];
               mask = (mask > 0) ? 0: ((-mask) >> 5);
               mask -= floor;
               /* End COMPUTE_MASK */
               bap[ i ] = bapTable[ 156 + mask + 4 * exp[i] ];  
if ( debug ) System.out.println( "Abap[" + i + "]=" + bap[i] + " exp=" + exp[i] + " mask=" + mask + " deltba=" + deltba[i] + " dbknee=" + dbknee + " psd=" +psd + " hth=" + hth[ i >> halfrate]);
               i++;
           } while ( (i < 3) || ( i < 7 && exp[i] > exp[i - 1]) );
           fastleak = psd + fgain;
           slowleak = psd + sgain;
           
           while ( i < 7 ) {
               if ( i < j ) {
                   if ( exp[ i + 1 ] == exp[ i ] - 2 ) {
                       lowcomp = 384;
                   } else if ( (lowcomp != 0) && exp[ i + 1 ] > exp[ i ] ) {
                       lowcomp -= 64;
                   }
               }
               psd = 128 * exp[i];
               /* UPDATE_LEAK */
               fastleak += fdecay;
               if ( fastleak > psd + fgain ) fastleak = psd + fgain;
               slowleak += sdecay;
               if ( slowleak > psd + sgain ) slowleak = psd + sgain;
               /* End UPDATE_LEAK */
               mask = ((fastleak + lowcomp) < slowleak) ? (fastleak + lowcomp) : slowleak;
               /* COMPUTE_MASK */
               if ( psd > dbknee )               mask -= (psd - dbknee)>>2;
               if ( mask > hth[ i >> halfrate] ) mask = hth[ i >> halfrate ];
               mask -= snroffset + 128 * deltba[ i ];
               mask = (mask > 0) ? 0: ((-mask) >> 5);
               mask -= floor;
               /* End COMPUTE_MASK */
               bap[i] = bapTable[ 156 + mask + 4 * exp[i] ];
if ( debug ) System.out.println( "Bbap[" + i + "]=" + bap[i] );
              i++;
           }
           if ( end == 7 ) return; /* LFE channel */
           
           do {
               if ( exp[ i + 1 ] == exp[ i ] - 2 ) {
                   lowcomp = 320;
               } else if ( (lowcomp != 0) && exp[ i + 1 ] > exp[ i ] ) {
                   lowcomp -= 64;
               }
               psd = 128 * exp[i];
               /* UPDATE_LEAK */
               fastleak += fdecay;
               if ( fastleak > psd + fgain ) fastleak = psd + fgain;
               slowleak += sdecay;
               if ( slowleak > psd + sgain ) slowleak = psd + sgain;
               /* End UPDATE_LEAK */
               mask = ((fastleak + lowcomp) < slowleak) ? (fastleak + lowcomp) : slowleak;
               /* COMPUTE_MASK */
               if ( psd > dbknee )               mask -= (psd - dbknee)>>2;
               if ( mask > hth[ i >> halfrate] ) mask = hth[ i >> halfrate ];
               mask -= snroffset + 128 * deltba[ i ];
               mask = (mask > 0) ? 0: ((-mask) >> 5);
               mask -= floor;
               /* End COMPUTE_MASK */
               bap[i] = bapTable[ 156 + mask + 4 * exp[ i ] ];
if ( debug ) System.out.println( "Cbap[" + i + "]=" + bap[i] );
               i++;
           } while ( i < 20 );
           
           while ( lowcomp > 128 ) {
               lowcomp -= 128;
               psd = 128 * exp[i];
               /* UPDATE_LEAK */
               fastleak += fdecay;
               if ( fastleak > psd + fgain ) fastleak = psd + fgain;
               slowleak += sdecay;
               if ( slowleak > psd + sgain ) slowleak = psd + sgain;
               /* End UPDATE_LEAK */
               mask = ((fastleak + lowcomp) < slowleak) ? (fastleak + lowcomp) : slowleak;
               /* COMPUTE_MASK */
               if ( psd > dbknee )               mask -= (psd - dbknee)>>2;
               if ( mask > hth[ i >> halfrate] ) mask = hth[ i >> halfrate ];
               mask -= snroffset + 128 * deltba[ i ];
               mask = (mask > 0) ? 0: ((-mask) >> 5);
               mask -= floor;
               /* End COMPUTE_MASK */
               bap[i] = bapTable[ 156 + mask + 4 * exp[ i ] ];
if ( debug ) System.out.println( "Dbap[" + i + "]=" + bap[i] );
               i++;
           }
           j = i;
       }
       
       do {
           int startband = j;
           int endband = ( bndTable[ i - 20] < end ) ? bndTable[ i - 20 ] : end;
           psd = 128 * exp[ j++ ];
           while ( j < endband ) {
               int next = 128 * exp[j++];
               int delta = next - psd;
               switch ( delta >> 9 ) {
                   case -6:
                   case -5:
                   case -4:
                   case -3:
                   case -2: {
                       psd = next;
                       break;
                   }
                   case -1: {
                       psd = next + laTable[ (-delta) >> 1 ];
                       break;
                   }
                   case 0: {
                       psd += laTable[ delta >> 1 ];
                       break;
                   }
               }
           }
if ( debug ) System.out.println( "leak " + fdecay + " " + sdecay + " " + fastleak + " " + slowleak + " " + psd);
           /* UPDATE_LEAK */
           fastleak += fdecay;
           if ( fastleak > psd + fgain ) fastleak = psd + fgain;
           slowleak += sdecay;
           if ( slowleak > psd + sgain ) slowleak = psd + sgain;
           /* End UPDATE_LEAK */
           mask = (fastleak < slowleak) ? fastleak : slowleak;
if ( debug ) System.out.println( "mask1 " + mask );
           /* COMPUTE_MASK */
           if ( psd > dbknee )               mask -= (psd - dbknee)>>2;
           if ( mask > hth[ i >> halfrate] ) mask = hth[ i >> halfrate ];
           mask -= snroffset + 128 * deltba[ i ];
           mask = (mask > 0) ? 0: ((-mask) >> 5);
           mask -= floor;
           /* End COMPUTE_MASK */
           i++;
           j = startband;
           do {
               bap[j] = bapTable[ 156 + mask + 4 * exp[ j ] ];
if ( debug ) System.out.println( "Ebap[" + j + "] exp="+exp[j]+" mask=" +mask);
           } while ( ++j < endband );
       } while ( j < end );                       
    }

    private boolean zero_snr_offsets( int nfchans ) {
        int i;

        /* Check the value of fsnroffst ( bits 3-7 of bai ) */
        if ((csnroffst != 0) || 
            (chincpl != 0 && ((cplbaBai >>3)!= 0)) ||
            (lfeon && ((lfebaBai >> 3) != 0))) {
            return false;
        }
        for (i = 0; i < nfchans; i++) {
            if ((baBai[i] >> 3) != 0) return false;
        }
        return true;
    }
    
    private int dither_gen() {
        int nstate = dither_lutp[ lfsr_state >> 8 ] ^ (lfsr_state << 8);
        if ( (nstate & 0x8000) != 0 ) {
            nstate |= (-1 << 16);
        } else {
            nstate &= 0xffff;
        }
        lfsr_state = nstate & 0xffff;
if ( debug ) System.out.println( "dither_gen=" + lfsr_state );
        return nstate;  /* NOTE VERSION */
    }
        
    
    private void coeff_get( double[] samples, int samplesPointer, byte[] exp, byte[] bap,
                            Quantizer quant,
                            double level, boolean dither, int end) throws FFMpegException {

                                if ( debug ) System.out.println( "coeff_get " + show_sample(level) );
        double[] factor = new double[ 25 ];
        for ( int i = 0; i <= 24; i++ ) {
            factor[ i ] = scale_factor[ i ] * level;
        }
        
        for ( int i = 0; i < end; i++ ) {
if ( debug ) if ( i != 0 ) System.out.println( "coeff_get: sample=" + show_sample(samples[ samplesPointer + i - 1]) );
            int bapi = bap[i];
if ( debug ) System.out.println( "bapi=" + bapi );
            switch ( bapi ) {
                case 0: {
                    if ( dither ) {
                        int d = dither_gen();
                        samples[ samplesPointer + i ] = d * factor[exp[i]] * LEVEL_3DB;    /* NOTE VERSION */
if ( debug ) System.out.println( "dither " + exp[i] + " " + d + " " + show_sample(samples[ samplesPointer + i ]) );
                    } else {
                        samples[ samplesPointer + i ] = 0;
                    }
                    break;
                }
                case -1: {
                    if ( quant.getQ1Pointer() >= 0 ) {
                        samples[ samplesPointer + i ] = quant.getQ1()[ quant.getQ1Pointer() ] * factor[ exp[i] ];
//System.out.println( "case -1 Q1=" + show_sample(quant.getQ1()[ quant.getQ1Pointer() ]) );
                        quant.setQ1Pointer( quant.getQ1Pointer() - 1 );
                    } else {
                        int code = in.getBits( 5 );
//System.out.println( "case -1 code=" + code );
                        quant.setQ1Pointer( 1 );
                        quant.getQ1()[0] = q_1_2[code];
                        quant.getQ1()[1] = q_1_1[code];
                        samples[ samplesPointer + i ] =q_1_0[code] * factor[ exp[i] ];
                    }
                    break;
                }
                case -2: {
                    if ( quant.getQ2Pointer() >= 0 ) {
                        samples[ samplesPointer + i ] =quant.getQ2()[ quant.getQ2Pointer() ] * factor[ exp[i] ];
                        quant.setQ2Pointer( quant.getQ2Pointer() - 1 );
                    } else {
                        int code = in.getBits( 7 );
                        quant.setQ2Pointer( 1 );
                        quant.getQ2()[0] = q_2_2[code];
                        quant.getQ2()[1] = q_2_1[code];
                        samples[ samplesPointer + i ] = q_2_0[code] * factor[ exp[i] ];
                    }
                    break;
                }
                case 3: {
                    samples[ samplesPointer + i ] = q_3[ in.getBits(3) ] * factor[ exp[i] ];
                    break;
                }
                case -3: {
                    if ( quant.getQ4Pointer() == 0 ) {
                        samples[ samplesPointer + i ] = quant.getQ4()[0] * factor[ exp[i] ];
//System.out.println( "-3A: q_4=" + quant.getQ4()[0] + " exp[" + i+"]=" + exp[i] );
                        quant.setQ4Pointer( -1 );
                    } else {
                        int code = in.getBits( 7 );
//System.out.println( "-3B: code=" + code + " q_4_0=" + q_4_0[code] + " level=" + level );
                        quant.setQ4Pointer( 0 );
                        quant.getQ4()[0] = q_4_1[code];
                        samples[ samplesPointer + i ] = q_4_0[code] * factor[ exp[i] ];
                    }
                    break;
                }
                case 4: {
                    samples[ samplesPointer + i ] = q_5[in.getBits(4)] * factor[ exp[i] ];
                    break;
                }
                default: {
                    int tmp = bitstream_get_2(bapi);
                    if ( debug ) System.out.println( "default " + tmp + " " + i + " " + exp[i] );
                    samples[ samplesPointer + i ] = ((double)(tmp << (16 - bapi))) * factor[exp[i]];
                    break;
                }
            }                
        }   
    }
    
    /**
     * Signed get
     */
    private int bitstream_get_2( int numberOfBits ) {
        int tmp = in.getBits( numberOfBits );
        if ( (tmp & ( 1 << (numberOfBits - 1) )) != 0 ) {
            tmp |= (-1 << numberOfBits );
        }
        return tmp;
    }
        
    private void coeff_get_coupling( int nfchans, double[] coeff, 
                                     double[] samples, int samplesPointer, 
                                     Quantizer quant,
                                     boolean[] dithflag ) throws FFMpegException {
        double[] cplcoLocal = new double[ 5 ];
        byte[] exp = cpl_expbapExp;
        byte[] bap = cpl_expbapBap;
        int bnd = 0;
        int cplbndstrcLocal = cplbndstrc;
        int i = cplstrtmant;
        while ( i < cplendmant ) {
            int i_end = i + 12;
            while ( (cplbndstrcLocal & 1) != 0 ) {
                cplbndstrcLocal >>= 1;
                i_end += 12;
            }
            cplbndstrcLocal >>= 1;
            for ( int ch = 0; ch < nfchans; ch++ ) {
                cplcoLocal[ch] = cplco[ch][bnd] * coeff[ch];
if ( debug ) System.out.println( "cplcoLocal[" + ch + "]=" + show_sample(cplcoLocal[ch]) + " coeff=" + show_sample(coeff[ch]));
            }
            bnd++;
            
            if ( debug ) System.out.println( "i_end " + (i_end - cplstrtmant) );
            while ( i < i_end ) {
                double cplcoeff;
if ( debug && i != 0 ) { 
    System.out.print( "coeff_get_coupling: sample=" );
    for ( int ch = 0; ch < nfchans; ch++ ) {
        System.out.print( show_sample(samples[ samplesPointer + i - 1 + ch * 256 ])+ ", ");
    }
    System.out.println();
}
                int bapi = bap[i];
if ( debug ) System.out.println( "bapi=" + bapi );
                switch ( bapi ) {
                    case 0: {
                        cplcoeff = LEVEL_3DB * scale_factor[exp[i]];
                        for ( int ch = 0; ch < nfchans; ch++ ) {
                            if ( ((chincpl >> ch ) & 1) != 0 ) {
                                if ( dithflag[ ch ] ) {
                                    samples[ samplesPointer + i + ch * 256 ] = cplcoeff * cplcoLocal[ch] * dither_gen();
if ( debug ) System.out.println( "cplcoeff " + show_sample(cplcoLocal[ch]) );
                                } else {
if ( debug ) System.out.println( "!dithflag" );
                                    samples[ samplesPointer + i + ch * 256 ] = 0;
                                }
                            }
                        }
                        i++;
                        break;
                    }
                    case -1: {
                        if ( quant.getQ1Pointer() >= 0 ) {
                            cplcoeff = quant.getQ1()[ quant.getQ1Pointer() ];
                            quant.setQ1Pointer( quant.getQ1Pointer() - 1 );
                        } else {
                            int code = in.getBits( 5 );
                            quant.setQ1Pointer( 1 );
                            quant.getQ1()[0] = q_1_2[code];
                            quant.getQ1()[1] = q_1_1[code];
                            cplcoeff = q_1_0[code];
                        }
                        break;
                    }                
                    case -2: {
                        if ( quant.getQ2Pointer() >= 0 ) {
                            cplcoeff = quant.getQ2()[ quant.getQ2Pointer() ];
                            quant.setQ2Pointer( quant.getQ2Pointer() - 1 );
                        } else {
                            int code = in.getBits( 7 );
                            quant.setQ2Pointer( 1 );
                            quant.getQ2()[0] = q_2_2[code];
                            quant.getQ2()[1] = q_2_1[code];
                            cplcoeff = q_2_0[code];
                        }
                        break;
                    }
                    case 3: {
                        cplcoeff = q_3[ in.getBits(3) ];
                        break;
                    }
                    case -3: {
                        if ( quant.getQ4Pointer() == 0 ) {
                            cplcoeff = quant.getQ4()[ 0 ];
                            quant.setQ4Pointer( -1 );
                        } else {
                            int code = in.getBits( 7 );
                            quant.setQ4Pointer( 0 );
                            quant.getQ4()[0] = q_4_1[code];
                            cplcoeff = q_4_0[code];
                        }
                        break;
                    }
                    case 4: {
                        cplcoeff = q_5[ in.getBits(4) ];
                        break;
                    }
                    default: {
                        cplcoeff = bitstream_get_2( bapi ) << (16 - bapi);
                        break;
                    }
                }
                if (bapi == 0 ) continue;
                cplcoeff *= scale_factor[ exp[i] ];
                for ( int ch = 0; ch < nfchans; ch++ ) {
                    if ( ((chincpl >> ch ) & 1) != 0 ) {
                        samples[ samplesPointer + i + ch * 256 ] = cplcoeff * cplcoLocal[ch];
                    }                    
                }
                i++;
            }
        }           
    }
                            
    protected static String show_sample( double s ) {
       if ( s == 0 ) return "0";
       if ( s != 0 ) {
           while ( (s < 1) && (s > -1) ) {
               s *= 10;
           }
           while ( s > 10 || s < -10 ) {
               s /= 10;
           }
       }
       String sampleDisplay = Double.toString( s );
       if ( sampleDisplay.length() < 4 ) sampleDisplay += "0000";
       if ( sampleDisplay.length() > 4 ) sampleDisplay = sampleDisplay.substring( 0, 4 );
       return sampleDisplay;
    }

    private void downmix_init( int acmod ) {
        /* Stereo output 
        switch ( acmod ) {
            case A52_3F: {
                level /= 1 + clev;
                break;
            }
            case A52_3F1R: {
                level /= 1 + clev + slev * LEVEL_3DB;
                break;
            }
            case A52_2F2R: {
                level /= 1 + slev;
                break;
            }
            case A52_3F2R: {
                level /= 1 + clev + slev;
                break;
            }
        }        
         */
    }
    
    private void a52_downmix_coeff( double[] coeff, int acmod, double level, double clev, double slev ) {
        /* TODO - depends on output mode */
        /* Stereo output */
        switch ( acmod ) {
            case A52_STEREO: {
                coeff[ 0 ] = level;
                coeff[ 1 ] = level;
                coeff[ 2 ] = level;
                coeff[ 3 ] = level;
                coeff[ 4 ] = level;
                 break;
            }
            case A52_3F: {
                coeff[ 0 ] = level;
                coeff[ 1 ] = level * clev;
                coeff[ 2 ] = level;
                coeff[ 3 ] = level;
                coeff[ 4 ] = level;
                break;
            }
            case A52_2F1R: {
                coeff[ 0 ] = level;
                coeff[ 1 ] = level;
                coeff[ 2 ] = level * slev * LEVEL_3DB;
                coeff[ 3 ] = level;
                coeff[ 4 ] = level;
                break;
            }
            case A52_3F1R: {
                coeff[ 0 ] = level;
                coeff[ 1 ] = level * clev;
                coeff[ 2 ] = level;
                coeff[ 3 ] = level * slev * LEVEL_3DB;
                coeff[ 4 ] = level;
                break;
            }
            case A52_2F2R: {
                coeff[ 0 ] = level;
                coeff[ 1 ] = level;
                coeff[ 2 ] = level * slev;
                coeff[ 3 ] = level * slev;
                coeff[ 4 ] = level;
                break;
            }
            case A52_3F2R: {
                coeff[ 0 ] = level;
                coeff[ 1 ] = level * clev;
                coeff[ 2 ] = level;
                coeff[ 3 ] = level * slev;
                coeff[ 4 ] = level * slev;
                break;
            }
            default: {
                break;
            }
        }
    }
    
    /* Read Deltba */
    private void parse_deltba( int[] deltba ) throws FFMpegException {
        for ( int i = 0; i < deltba.length; i++ ) {
            deltba[i] = 0;
        }
        int deltnseg = in.getBits( 3 );
        int j = 0;
        do {
            j += in.getBits( 5 );
            int deltlen = in.getBits( 4 );
            int delta = in.getBits( 3 );
            delta -= (delta >= 4) ? 3:4;
if ( debug ) System.out.println( "j=" + j + " delta=" + delta + " len=" + deltlen );
            while ( deltlen-- != 0 ) { deltba[ j++ ] = delta; }
        } while ( deltnseg-- != 0 );
    }
    /*
     * Extract exponents
     */
    private void parse_exponents( int expstr, int ngrps, byte exponent,
                                  byte[] exponents, int exponentPointer ) throws FFMpegException {
if ( debug ) System.out.println( "parse_exponents " + ngrps );
        while ( ngrps-- != 0 ) {
            int exps = in.getBits( 7 );
//System.out.println( "exps=" + exps );

            /* Exponent 1 */
            exponent += exp_1[ exps ];
            if ( (0xff & exponent) > 24 ) throw new AC3Exception( "Exponent too large" );            
            switch ( expstr ) {
                case EXP_D45:
                    exponents[ exponentPointer++ ] = exponent;
                    exponents[ exponentPointer++ ] = exponent;
                case EXP_D25:
                    exponents[ exponentPointer++ ] = exponent;
                case EXP_D15:
                    exponents[ exponentPointer++ ] = exponent;
            }

            /* Exponent 2 */
            exponent += exp_2[ exps ];
            if ( (0xff & exponent) > 24 ) throw new AC3Exception( "Exponent too large" );
            switch ( expstr ) {
                case EXP_D45:
                    exponents[ exponentPointer++ ] = exponent;
                    exponents[ exponentPointer++ ] = exponent;
                case EXP_D25:
                    exponents[ exponentPointer++ ] = exponent;
                case EXP_D15:
                    exponents[ exponentPointer++ ] = exponent;
            }

            /* Exponent 3 */
            exponent += exp_3[ exps ];
            if ( (0xff & exponent) > 24 ) throw new AC3Exception( "Exponent too large" );
            switch ( expstr ) {
                case EXP_D45:
                    exponents[ exponentPointer++ ] = exponent;
                    exponents[ exponentPointer++ ] = exponent;
                case EXP_D25:
                    exponents[ exponentPointer++ ] = exponent;
                case EXP_D15:
                    exponents[ exponentPointer++ ] = exponent;
            }
        }
    }
    
    /**
     * Codec management
     */
    public Format[] getSupportedInputFormats() {
        return new Format[] { new AudioFormat( "ac3" ) };
    }
    
    public Format[] getSupportedOutputFormats(Format format) {
        return new Format[] { new AudioFormat( "LINEAR" ) };
    }
    
    private AudioFormat inputFormat;
    public Format setInputFormat( Format format ) {
        inputFormat = (AudioFormat)format;
        return format;
    }
    
    public Format setOutputFormat( Format format ) {
        return new AudioFormat("LINEAR", inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits() > 0 ? inputFormat.getSampleSizeInBits() : 16,
                                         inputFormat.getChannels(),
                                         0, 1); // endian, int signed
    }
    
    private boolean readSyncBlock = true;
    public int process( Buffer input, Buffer output ) {
        output.setFlags( input.getFlags() );
        output.setTimeStamp( input.getTimeStamp() );
        output.setDuration( input.getDuration() );

        try {
            byte[] data = (byte[])input.getData();  //in.getLength
            int    length = input.getLength();

            output.setLength(0);
//            System.out.println( "Process" );
            /*
            System.out.println( "Parsing packet" );            
            for ( int i = 0; i < length; i++ ) {
                System.out.print( Integer.toHexString( data[i] & 0xff ) + " " );
            }
            */
            
            /**
             * Parse data
             */
//            in = new Mpeg4Stream();
            in.addData( data, 0, length );
            
            /**
             * Until we have no more data 
             */
            while ( in.availableBits() > HEADER_LENGTH * 8 ) {
                /**
                 * Find sync block
                 */
                if ( readSyncBlock ) {
//                    System.out.println( "SYNC" );
                    in.seek(in.getPos() & ~0x7);
                    if ( !a52_syncinfo() ) continue;
                    readSyncBlock = false;
                }

                /**
                 * Do we have an entire frame?
                 */
                if ( in.availableBits() >= frame_length * 8 ) {
                    int syncPos = in.getPos();
                    a52_frame();
                    //a52_dynring()
                    while ( in.getPos() - syncPos < (frame_length - HEADER_LENGTH) * 8 ) {
                        a52_block();
                        soundOutput.getAudioBuffer( samplesOut, 2, output );
                    }
                    readSyncBlock = true;
                } else break;
            } 
//            System.out.println( "EXITING!!!" );
        } catch (Exception e) {
//            e.printStackTrace();
            readSyncBlock = true;
            in.seek( in.getPos() + in.availableBits() );
            return BUFFER_PROCESSED_FAILED;
        }
        return BUFFER_PROCESSED_OK;
    }
            
    public void open() {
    }
    
    public void close() {
    }
    
    public void reset() {
    }
    
    public String getName() {
        return "ac3";
    }
    
    public Object[] getControls() {
        return new Object[ 0 ];
    }
    
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
    }

    public void setEncoding( String encoding ) {
    }

    public void setIsRtp( boolean isRtp ) {
    }

    public void setIsTruncated( boolean isTruncated ) {
    }
}
