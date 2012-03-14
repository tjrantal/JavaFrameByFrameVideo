/*
 * Java port of ffmpeg mp3 decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2000, 2001 Fabrice Bellard.
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
package net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3;

import net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3.data.*;

import net.sourceforge.jffmpeg.codecs.utils.BitStream;
import net.sourceforge.jffmpeg.codecs.utils.VLCTable;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;

/**
 *
 */
public class Granule {
    public static final boolean debug = false;
    
    public final static int SBLIMIT = 32;
    
    private int granuleStartPosition;
    
    private int scfsi;
    private int part23Length;
    private int bigValues;
    private int globalGain;
    private int scaleFactorCompress;
    private boolean blockSplitFlag;
    private int blockType;
    private boolean switchPoint;
    private int[] tableSelect = new int[ 3 ];
    private int[] subBlockGain = new int[ 3 ];
    private int     preflag;
    private int     scaleFacScale;
    private int     count1TableSelect;
    private int     region_address1;
    private int     region_address2;
    
    private int[] scaleFactors = new int[ 40 ];
    private int[] exponents = new int[ 576 ];
    private int[] sb_hybrid = new int[ SBLIMIT * 18 ];
    
    /**
     * Internal data
     */
    private int[] slenTable1 = Table.getSlenTable1();
    private int[] slenTable2 = Table.getSlenTable2();
    private int[][] band_size_long  = Table.getBandSizeLong();
    private int[][] band_size_short = Table.getBandSizeShort();
    private int[][] mpa_pretab = Table.getPreTab();
    private int[][] band_index_long = Table.getBandIndexLong();

    private int[][] mpa_huff_data = Table.getHuffData();
    HuffmanCodes[]  huff_vlc = HuffmanCodes.getHuffmanCodes();
    HuffmanCodes[]  huff_quad_vlc = new HuffmanCodes[] {
                                        new HuffmanQuadCodes0(),
                                        new HuffmanQuadCodes1()
                                    };
                                    
    private int[][][] lsf_nsf_table = Table.getLsfNsfTable();
    
    public int getScfsi() {
        return scfsi;
    }
    
    public void setScfsi( int scfsi ) {
        this.scfsi = scfsi;
    }
    
    public void read( BitStream in, boolean lsf, int mode_ext ) {
        part23Length        = in.getBits( 12 );
        bigValues           = in.getBits( 9 );
        globalGain          = in.getBits( 8 );
        if ( (mode_ext & (MP3.MODE_EXT_I_STEREO|MP3.MODE_EXT_MS_STEREO)) == MP3.MODE_EXT_MS_STEREO ) {
            globalGain-=2;
        }
        scaleFactorCompress = in.getBits( lsf ? 9:4 );
if ( debug )            System.out.println("part23Length " + part23Length
                              + " bigValues " + bigValues
                              + " globalGain " + globalGain
                              + " scaleFactorCompress " + scaleFactorCompress );

        blockSplitFlag      = in.getTrueFalse();
        if ( blockSplitFlag ) {
            blockType       = in.getBits( 2 );
            switchPoint     = in.getTrueFalse();
            tableSelect[0]  = in.getBits(5);
            tableSelect[1]  = in.getBits(5);
            subBlockGain[0] = in.getBits(3);
            subBlockGain[1] = in.getBits(3);
            subBlockGain[2] = in.getBits(3);
            /*
            System.out.println( "Blocktype " + blockType
                              + " switchPoint " + (switchPoint ? 1:0)
                              + " tableSelect " + tableSelect[ 0 ]
                              + " tableSelect " + tableSelect[ 1 ]
                              + " subBlockGain " + subBlockGain[ 0 ]
                              + " subBlockGain " + subBlockGain[ 1 ]
                              + " subBlockGain " + subBlockGain[ 2 ] );
              */
        } else {
            blockType = 0;
            tableSelect[0] = in.getBits(5);
            tableSelect[1] = in.getBits(5);
            tableSelect[2] = in.getBits(5);
            region_address1 = in.getBits( 4 );
            region_address2 = in.getBits( 3 );
            /*
            System.out.println( "tableSelect " + tableSelect[ 0 ]
                              + " tableSelect " + tableSelect[ 1 ]
                              + " tableSelect " + tableSelect[ 2 ] );
            
            System.out.println( "region1=" + region_address1
                              + " region2=" + region_address2 );
             **/
        }

        preflag = lsf ? 0 : in.getBits(1);
        scaleFacScale = in.getBits(1);
        count1TableSelect = in.getBits(1);
    }

    
    private final void lsf_sf_expand( int[] slen, int sf, int n1, int n2, int n3 ) {
        if ( n3 != 0 ) {
            slen[ 3 ] = sf % n3;
            sf /= n3;
        } else {
            slen[ 3 ] = 0;
        }
        if ( n2 != 0 ) {
            slen[ 2 ] = sf % n2;
            sf /= n2;
        } else {
            slen[ 2 ] = 0;
        }
        slen[ 1 ] = sf % n1;
        sf /= n1;
        slen[ 0 ] = sf;
    }
    
    public void readScaleFactors( BitStream in, boolean lsf, Granule copyFrom, int channel, int mode_ext ) {
        granuleStartPosition = in.getPos();
/* System.out.println( "GranuleStartPosition " + granuleStartPosition );
        System.out.println( "scfsi=" + Integer.toHexString( scfsi )
                          + " scale_factors:" );
        System.out.println( "lsf " + (lsf ? 1:0) ); */
        if ( !lsf ) {
            int slen1 = slenTable1[ scaleFactorCompress ];
            int slen2 = slenTable2[ scaleFactorCompress ];
            if ( blockType == 2 ) {
                int j = 0;
                for ( int i = 0; i < (switchPoint ? 17 : 18); i++ ) {
                    scaleFactors[ j++ ] = in.getBits( slen1 );
//                    System.out.println( "A " + scaleFactors[ j - 1 ] );
                }
                for ( int i = 0; i < 18; i++ ) {
                    scaleFactors[ j++ ] = in.getBits( slen2 );
//                    System.out.println( "B " + scaleFactors[ j - 1 ] );
                }
                scaleFactors[ j++ ] = 0;
                scaleFactors[ j++ ] = 0;
                scaleFactors[ j++ ] = 0;                
            } else {
                int j = 0;
                for ( int i = 0; i < 4; i++ ) {
                    int n = (i == 0) ? 6 : 5;
                    if ( (scfsi & ( 0x08 >> i)) == 0 ) {
                        for ( int k = 0; k < n; k++ ) {
                            scaleFactors[ j++ ] = in.getBits( (i < 2) ? slen1 : slen2 );
//                            System.out.println( "C " + scaleFactors[ j - 1 ] );
                        }
                    } else {
                        for ( int k = 0; k < n; k++ ) {
                            scaleFactors[ j ] = copyFrom.scaleFactors[ j ];
                            j++;
//                            System.out.println( "D " + scaleFactors[ j - 1 ] );
                        }
                    }
                }
                scaleFactors[ j++ ] = 0;                
            }
        } else {
            int tindex = 0;
            int tindex2 = 0;
            if ( blockType == 2 ) {
                tindex = switchPoint ? 2 : 1;
            }
            int sf = scaleFactorCompress;
            int[] slen = new int[ 4 ];
            if ( (mode_ext & MP3.MODE_EXT_I_STEREO) != 0 && channel == 1 ) {
                sf >>=1;
                if ( sf < 180 ) {
                    lsf_sf_expand( slen, sf, 6, 6, 0 );
                    tindex2 = 3;
                } else if ( sf < 244 ) {
                    lsf_sf_expand( slen, sf - 180, 4, 4, 0 );
                    tindex2 = 4;
                } else {
                    lsf_sf_expand( slen, sf - 244, 3, 0, 0 );
                    tindex2 = 5;
                }
            } else {
                if ( sf < 400 ) {
                    lsf_sf_expand( slen, sf, 5, 4, 4 );
                    tindex2 = 0;
                } else if ( sf < 500 ) {
                    lsf_sf_expand( slen, sf - 400, 5, 4, 0);
                    tindex2 = 1;
                } else {
                    lsf_sf_expand( slen, sf - 500, 3, 0, 0);
                    tindex2 = 2;
                    preflag = 1;
                }
            }
            int j = 0;
            for ( int k = 0; k < 4; k++ ) {
                int n = lsf_nsf_table[ tindex2 ][ tindex ][ k ];
                int sl = slen[ k ];
                for ( int i = 0; i < n; i++ ) {
                    scaleFactors[ j++ ] = in.getBits( sl );
                }
            }
            for ( ; j < 40; j++ ) {
                scaleFactors[ j ] = 0;
            }
//            throw new Error( "Code me!!!" );
        }
    }

    private int longEnd;
    private int shortStart;

    private int[] gains = new int[3];
    public void exponents_from_scale_factors( int sample_rate_index ) {
        /* Calculate starts and ends */        
        longEnd = 0;
        shortStart = 0;
        if ( blockType == 2 ) {
            if ( switchPoint ) {
                if ( sample_rate_index <= 2 ) {
                    longEnd = 8;
                    shortStart = 2;
                } else if ( sample_rate_index != 8 ) {
                    longEnd = 6;
                    shortStart = 3;
                } else {
                    longEnd = 4;
                    shortStart = 2;
                }
            }
        } else {
            longEnd = 22;
            shortStart = 13;
        }
        
        /**
         * Exponents from scale factors
         */
        int exponentPointer = 0;
        int gain = globalGain - 210;
        int shift = scaleFacScale + 1;
        
        int[] bstab = band_size_long[ sample_rate_index ];
        int[] pretab = mpa_pretab[ preflag ];
        
        for ( int i = 0; i < longEnd; i++ ) {
            int v0 = gain - ((scaleFactors[i]+ pretab[ i ]) << shift);
            for ( int j = bstab[i]; j > 0; j-- ) {
                exponents[ exponentPointer++ ] = v0;
            }
        }
        
        if ( blockType == 2 ) {
            bstab = band_size_short[ sample_rate_index ];
            gains[ 0 ] = gain - (subBlockGain[0] << 3);
            gains[ 1 ] = gain - (subBlockGain[1] << 3);
            gains[ 2 ] = gain - (subBlockGain[2] << 3);
            int k = longEnd;
            for ( int i = shortStart; i < 13; i++ ) {
                int len = bstab[i];
                for ( int l = 0; l < 3; l++ ) {
                    int v0 = gains[ l ] - (scaleFactors[ k++ ] << shift);
                    for ( int j = bstab[i]; j > 0; j-- ) {
                        exponents[ exponentPointer++ ] = v0;
                    }
                }
            }
        }
    }

    private final int MULL( int a, int b ) {
        return (int)((((long)a) * ((long)b)) >> FRAC_BITS);
    }
    
    final int ISQRT2 = (int)( 0.70710678118654752440 * FRAC_ONE + 0.5 );
    
    private boolean[] non_zero_found_short = new boolean[ 3 ];
    public void computeStereo( MP3 mp3, Granule granule0 ) {
        if ( mp3.mode_ext == MP3.MODE_EXT_I_STEREO ) {
            int[][] is_tab;
            int sf_max;
            if ( !mp3.lsf ) {
                is_tab = is_table;
                sf_max = 7;
            } else {
                is_tab = is_table_lsf[ scaleFactorCompress & 1 ];
                sf_max = 16;
            }
            
            int tableIndex0 = 576;
            int tableIndex1 = 576;
            
            non_zero_found_short[ 0 ] = false;
            non_zero_found_short[ 1 ] = false;
            non_zero_found_short[ 2 ] = false;
            
            int k = (13 - shortStart) * 3 + longEnd - 3;
            for ( int i = 12; i >= shortStart; i-- ) {
                if ( i != 11 ) k -= 3;
                int len = band_size_short[ mp3.sample_rate_index ][ i ];
                for ( int l = 2; l >= 0; l-- ) {
                    tableIndex0 -= len;
                    tableIndex1 -= len;
                    if ( !non_zero_found_short[ l ] ) {
                        for ( int j = 0; j < len; j++ ) {
                            if ( sb_hybrid[ tableIndex1 + j ] != 0 ) {
                                non_zero_found_short[ l ] = true;
                                break;
                            }
                        }
                    }
                    if ( !non_zero_found_short[ l ] ) {
                        int sf = scaleFactors[ k + 1 ];
                        if ( sf < sf_max ) {
                            int v1 = is_tab[ 0 ][ sf ];
                            int v2 = is_tab[ 1 ][ sf ];
                            for ( int j = 0; j < len; j++ ) {
                                int tmp = granule0.sb_hybrid[ tableIndex0 + j ];
                                granule0.sb_hybrid[ tableIndex0 + j ] = MULL( tmp, v1 );
                                         sb_hybrid[ tableIndex1 + j ] = MULL( tmp, v2 );
                            }
                            continue;
                        }
                    }
                    if ( mp3.mode_ext == MP3.MODE_EXT_MS_STEREO ) {
                        for ( int j = 0; j < len; j++ ) {
                            int tmp0 = granule0.sb_hybrid[ tableIndex0 + j ];
                            int tmp1 =          sb_hybrid[ tableIndex1 + j ];
                            granule0.sb_hybrid[ tableIndex0 + j ] = MULL( tmp0 + tmp1, ISQRT2 );
                                     sb_hybrid[ tableIndex1 + j ] = MULL( tmp0 - tmp1, ISQRT2 );
                        }
                    }
                }
            }
            
            boolean non_zero_found =  non_zero_found_short[ 0 ]
                                    | non_zero_found_short[ 1 ]
                                    | non_zero_found_short[ 2 ];
            
            for ( int i = longEnd - 1; i >=0; i-- ) {
                int len = band_size_long[ mp3.sample_rate_index ][ i ];
                tableIndex0 -= len;
                tableIndex1 -= len;
                if ( !non_zero_found ) {
                    for ( int j = 0; j < len; j++ ) {
                        if ( sb_hybrid[ tableIndex1 + j ] != 0 ) {
                            non_zero_found = true;
                            break;
                        }
                    }
                }
                if ( !non_zero_found ) {
                    k = (i == 21 ) ? 20 : i;
                    int sf = scaleFactors[ k ];
                    if ( sf < sf_max ) {
                        int v1 = is_tab[ 0 ][ sf ];
                        int v2 = is_tab[ 1 ][ sf ];
                        for ( int j = 0; j < len; j++ ) {
                            int tmp = granule0.sb_hybrid[ tableIndex0 + j ];
                            granule0.sb_hybrid[ tableIndex0 + j ] = MULL( tmp, v1 );
                                     sb_hybrid[ tableIndex1 + j ] = MULL( tmp, v2 );
                        }
                        continue;
                    }
                }
                if ( mp3.mode_ext == MP3.MODE_EXT_MS_STEREO ) {
                    for ( int j = 0; j < len; j++ ) {
                        int tmp0 = granule0.sb_hybrid[ tableIndex0 + j ];
                        int tmp1 =          sb_hybrid[ tableIndex1 + j ];
                        granule0.sb_hybrid[ tableIndex0 + j ] = MULL( tmp0 + tmp1, ISQRT2 );
                                 sb_hybrid[ tableIndex1 + j ] = MULL( tmp0 - tmp1, ISQRT2 );
                    }
                }
            }   
        } else if ( mp3.mode_ext == MP3.MODE_EXT_MS_STEREO ) {
            for ( int i = 0; i < 576; i++ ) {
                int tmp0 = granule0.sb_hybrid[ i ];
                int tmp1 =          sb_hybrid[ i ];
                granule0.sb_hybrid[ i ] = tmp0 + tmp1;
                         sb_hybrid[ i ] = tmp0 - tmp1;
            }
        }
        if ( debug ) {
        for ( int i = 0; i < 576; i++ ) {
            System.out.print( " " + sb_hybrid[i] );
            if ( (i % 18) == 17 ) System.out.println();
        }
        }
         
    }

    public final int[] getSbHybrid() {
        return sb_hybrid;
    }
    
    public final int getBlockType() {
        return blockType;
    }
    
    public boolean getSwitchPoint() {
        return switchPoint;
    }
    
    public static final int FRAC_BITS  = 23;
    public static final int WFRAC_BITS = 16;
    
    public static final int FRAC_ONE   = 1 << FRAC_BITS;
    
    /* 2^(n/4) */
    public int[] scale_factor_mult3 = new int[] {
        (int)( 1.0 * FRAC_ONE + 0.5 ),
        (int)( 1.18920711500272106671 * FRAC_ONE + 0.5 ),
        (int)( 1.41421356237309504880 * FRAC_ONE + 0.5 ),
        (int)( 1.68179283050742908605 * FRAC_ONE + 0.5 )
    };
      
    /* value^(4/3) * 2^(exponent/4) */
    private int l3_unscale( int value, int exponent ) {
        int  e = FRAC_BITS - (table_4_3_exp[value] + (exponent >> 2));
        long m = ((long)table_4_3_value[value]) * ((long)scale_factor_mult3[ exponent & 3 ]);
        long y = (long)((m + (((long)1)<<(e - 1))) >> e);   //TODO check 64 bit shift
   /*     
        System.out.println( "" + value + "^" + exponent + " = "
                          + table_4_3_value[value] + " "
                          + scale_factor_mult3[ exponent & 3 ]
                          + " " + (m>>32) + " " + y + " " + e + " " + table_4_3_exp[value]);
     */    
        return (int)y;
    }
    
    
    private int[] regionSize = new int[ 3 ];
    public void huffman_decode( BitStream in, int sample_rate_index ) throws FFMpegException {
        /* Calculate Region Size */
        if ( blockSplitFlag ) {
            if ( blockType == 2 ) {
                regionSize[ 0 ] = 36/2;
            } else {
                if ( sample_rate_index <= 2 ) {
                    regionSize[ 0 ] = 36/2;
                } else if ( sample_rate_index != 8 ) {
                    regionSize[ 0 ] = 54/2;
                } else {
                    regionSize[ 0 ] = 108/2;
                }
            }
            regionSize[ 1 ] = 576/2;
        } else {
            regionSize[ 0 ] = band_index_long[sample_rate_index][region_address1 + 1] >> 1;
            int l = region_address1 + region_address2 + 2;
            if ( l > 22 ) l = 22;
            regionSize[ 1 ] = band_index_long[sample_rate_index][l] >> 1;
        }
        regionSize[2] = 576/2;

        int j = 0;
        for ( int i = 0; i < 3; i++ ) {
            int k = regionSize[ i ];
            if ( k > bigValues ) k = bigValues;
            regionSize[ i ] = k - j;
            j = k;
        }

        if ( debug ) {
            System.out.println( "Region 0: " + regionSize[ 0 ] );
            System.out.println( "Region 1: " + regionSize[ 1 ] );
            System.out.println( "Region 2: " + regionSize[ 2 ] );
        }
        /* Low frequencies [big values] */
        int s_index = 0;
        for ( int i = 0; i < 3; i++ ) {
            int count = regionSize[ i ];
            if ( count == 0 ) continue;
            
            int k = tableSelect[ i ];
            int l        = mpa_huff_data[ k ][ 0 ];
            int linbits  = mpa_huff_data[ k ][ 1 ];
            HuffmanCodes vlc = huff_vlc[ l ];
            int[] code_table = vlc.getHuffCodeTable();
            
//            System.out.println( "Table number " + l + " " + Integer.toHexString( in.showBits(24) ) );
            for ( ; count > 0; count-- ) {
                int x = 0;
                int y = 0;
                if ( code_table != null ) {
                    int code = in.getVLC( vlc );
                    y = code_table[ code ];
                    x = y >> 4;
                    y &= 0xf;
                }
                
if ( debug ) {                System.out.println( "region=" + i + " n=" + (regionSize[i]-count)
                          + " x=" + x + " y=" + y + " exp=" + exponents[ s_index ] 
                          + " " + Integer.toHexString( in.showBits(24) ) + " " + s_index);
}    
                int v = 0;
                if ( x != 0 ) {
                    if ( x == 15 ) {
                        x += in.getBits( linbits );
                    }
                    v = l3_unscale( x, exponents[ s_index ] );
                    v = in.getTrueFalse() ? -v : v;
                }
                sb_hybrid[ s_index++ ] = v;
                
                v = 0;
                if ( y != 0 ) {
                    if ( y == 15 ) {
                        y += in.getBits( linbits );
                    }
                    v = l3_unscale( y, exponents[ s_index ] );
                    v = in.getTrueFalse() ? -v : v;
                }
                sb_hybrid[ s_index++ ] = v;
            }
        }
        
        /* High frequencies */
        HuffmanCodes vlc = huff_quad_vlc[ count1TableSelect ];
        while ( s_index < 572 ) {
            if ( in.getPos() >= granuleStartPosition + part23Length ) break;
            
            int code = in.getVLC( vlc );
//            System.out.println( "t=" + count1TableSelect + " code=" + code );
            
            for ( int i = 0; i < 4; i++ ) {
                int v = 0;
                if ( (code & (8 >> i)) != 0 ) {
                    v = l3_unscale( 1, exponents[ s_index ] );
                    v = in.getTrueFalse() ? -v : v;
                }
                sb_hybrid[ s_index++ ] = v;
            }
        }
        
        while ( s_index < 572 ) sb_hybrid[ s_index++ ] = 0;

        /** skip to end of stream */
        in.seek( part23Length + granuleStartPosition );
        
        /** Dump samples */
//        System.out.println( "pos=0" );
if ( debug ) {        for ( int i = 0; i < 576; i++ ) {
            System.out.print( " " + sb_hybrid[i] );
            if ( (i % 18) == 17 ) System.out.println();
        }
}
    }
    
    /* 1111112222222333333 --> 123123123123123 */
    private int[] tmp = new int[ 576 ];
    public void reorderBlock( MP3 mp3 ) {
        if ( blockType == 2 ) {
            int sb_hybridPointer = 0;
            if ( switchPoint ) {
                if ( mp3.sample_rate_index != 8  ) {
                    sb_hybridPointer = 36;
                } else {
                    sb_hybridPointer = 48;
                }
            }
            
            int dst = 0;
            for ( int i = shortStart; i < 13; i++ ) {
                int len = band_size_short[ mp3.sample_rate_index ][ i ];
                int ptr1 = sb_hybridPointer;
                for ( int k = 0; k < 3; k++ ) {
                    dst = k;
                    for ( int j = len; j > 0; j-- ) {
                        tmp[ dst ] = sb_hybrid[ sb_hybridPointer++ ];
                        dst += 3;
                    }
                }
                System.arraycopy( tmp, 0, sb_hybrid, ptr1, len * 3 );
            }
        }
        if ( debug ) {
        for ( int i = 0; i < 576; i++ ) {
            System.out.print( " " + sb_hybrid[i] );
            if ( (i % 18) == 17 ) System.out.println();
        }
        }

    }
    
    public void antialias( MP3 mp3 ) {
        int n;
        if ( blockType == 2 ) {
            if ( !switchPoint ) return;
            n = 1;
        } else {
            n = SBLIMIT - 1;
        }
        
        int sb_hybridPointer = 18;
        for ( int i = n; i > 0; i-- ) {
            int p0 = sb_hybridPointer - 1;
            int p1 = sb_hybridPointer;
            for ( int j = 0; j < 8; j++ ) {
                int tmp0 = sb_hybrid[ p0 ];
                int tmp1 = sb_hybrid[ p1 ];
                sb_hybrid[ p0-- ] = (int)((FRAC_ONE/2 + ( ((long)tmp0) * ((long)csa_table[ j * 2     ]) - ((long)tmp1) * ((long)csa_table[ j * 2 + 1 ] ) )) >> FRAC_BITS);
                sb_hybrid[ p1++ ] = (int)((FRAC_ONE/2 + ( ((long)tmp0) * ((long)csa_table[ j * 2 + 1 ]) + ((long)tmp1) * ((long)csa_table[ j * 2     ] ) )) >> FRAC_BITS);
            }                
            sb_hybridPointer += 18;
        }
    }
    
    public void dumpScaleFactors() {
        System.out.println( "scfsi=" + Integer.toHexString(scfsi) + " scale_factors:" );
        for ( int i = 0; i < scaleFactors.length; i++ ) {
            System.out.print( " " + scaleFactors[i] );
        }
        System.out.println();
    }
    
    public void dumpHybrid() {
                for ( int i = 0; i < 576; i++ ) {
            System.out.print( " " + sb_hybrid[i] );
            if ( (i % 18) == 17 ) System.out.println();
        }
    }
    
    /** Creates a new instance of Granule */
    public Granule() {
    }    
    
    /** Create string */
    public String toString() {
        return  "scfsi " + scfsi + "\n"
              + "tableSelect[0] " + tableSelect[ 0 ];
    }
    
    /* Create table */
    public static final int TABLE_4_3_SIZE = 8191 + 16;
    public static final int[] table_4_3_value = new int[ TABLE_4_3_SIZE ];
    public static final int[] table_4_3_exp   = new int[ TABLE_4_3_SIZE ];
    
    static {
        /* Internal accuracy 24 bit fraction */
        final int POW_FRAC_BITS = 24;
        final int POW_FRAC_ONE  = 1 << POW_FRAC_BITS;
        final int DEV_ORDER     = 13;

        /* 2^(i/3) */
        int[] pow_mult3 = new int[] {
            (int)(1.0 * POW_FRAC_ONE ),
            (int)(1.25992104989487316476 * POW_FRAC_ONE),
            (int)(1.58740105196819947474 * POW_FRAC_ONE)
        };
        
        /* Power series ( 1 + i ) ^ (4/3)*/
        int[] dev_4_3_coefs = new int[ DEV_ORDER ];
        int t = POW_FRAC_ONE;
        for ( int i = 0; i < DEV_ORDER; i++ ) {
            int t2 = ((4 * POW_FRAC_ONE) / 3 - i * POW_FRAC_ONE) / ( i + 1 );
            t = (int)(((long)t * (long)t2) >> POW_FRAC_BITS);
            dev_4_3_coefs[ i ] = t;
        }
        
        /* Calculate i ^ 4/3 */
        for ( int i = 1; i < TABLE_4_3_SIZE; i++ ) {
            
            /* Convert integer to mantissa and expoenent [i * POW_FRAC_ONE = a * 2 ^ e] */
            int a = i;
            int e = POW_FRAC_BITS;
            while ( a < (1 << (POW_FRAC_BITS - 1))) {
                a = a << 1;
                e--;
            }
            /* Subtract 1 [Taylor expansion is (1 + i)] */
            a -= 1 << POW_FRAC_BITS;
            
            /* Power series is Taylor expansion */
            int a1 = 0;
            for ( int j = DEV_ORDER - 1; j >= 0; j-- ) {
                a1 = (int)(( (long)a * ((long)dev_4_3_coefs[j] + a1) ) >> POW_FRAC_BITS );
            }
            a = (1 << POW_FRAC_BITS) + a1;
            
            /* Exponent 4 / 3 */
            e = e * 4;
            int er = e % 3;
            int eq = e / 3;
            
            /* Fix up mantissa */
            a = (int)(( (long)a * (long)pow_mult3[er] ) >> POW_FRAC_BITS );
            
            /* Renormalise */
            while ( a >= 2 * POW_FRAC_ONE ) {
                a = a >> 1;
                eq++;
            }
            while ( a < POW_FRAC_ONE ) {
                a = a << 1;
                eq--;
            }

            /* Renormalise to global fraction - round up 1/2 */
            a = a + ( 1 << (POW_FRAC_BITS - FRAC_BITS - 1) );
            a >>= POW_FRAC_BITS - FRAC_BITS;
            while ( a >= 2 * ( 1 << FRAC_BITS ) ) {
                a = a >> 1;
                eq++;
            }
            
//            System.out.println( "" + i + " " + a + " " + eq );
            table_4_3_value[ i ] = a;
            table_4_3_exp[ i ] = eq;
        }        
    }

    private static int[][] is_table;
    private static int[][][] is_table_lsf;
    private static int[] csa_table;
    public static final double M_PI = 3.14159265358979323846;

    /* table for alias reduction (XXX: store it as integer !) */
    private static double[] ci_table = new double[] {
        -0.6, -0.535, -0.33, -0.185, -0.095, -0.041, -0.0142, -0.0037
    };
    static {

        is_table = new int[ 2 ][ 16 ];
        for( int i = 0; i < 7; i++ ) {
            double f;
            int v;
            if (i != 6) {
                f = tan((double)i * M_PI / 12.0);
                v = (int)(FRAC_ONE * (f / (1.0 + f)) + 0.5);
            } else {
                v = FRAC_ONE;
            }
            is_table[0][i] = v;
            is_table[1][6 - i] = v;
        }
        /* invalid values */
        for( int i = 7; i < 16; i++ ) {
            is_table[0][i] = 0;
            is_table[1][i] = 0;
        }
        
        is_table_lsf = new int[ 2 ][ 2 ][ 16 ];
        for( int i = 0; i < 16; i++ ) {

            for( int j=0; j < 2; j++ ) {
                int e = -(j + 1) * ((i + 1) >> 1);
                double f = pow(2.0, e / 4.0);
                int k = i & 1;
                is_table_lsf[ j ][ k ^ 1 ][i] = (int)(FRAC_ONE * f + 0.5);
                is_table_lsf[j][k][i] = FRAC_ONE;
            }
        }
        
        csa_table = new int[ 16 ];
        for ( int i = 0; i < 8; i++ ) {
            double ci = ci_table[ i ];
            double cs = 1/ sqrt( 1.0 + ci * ci);
            double ca = cs * ci;
            csa_table[ i * 2     ] = (int)(cs * FRAC_ONE);
            csa_table[ i * 2 + 1 ] = (int)(ca * FRAC_ONE);
        }

    }

    public static final double sin( double x ) {
        return Math.sin( x );
    }
    public static final double tan( double x ) {
        return Math.tan(x);
    }
    public static final double pow( double a, double b ) {
        return Math.pow(a,b);
    }

    public static final double sqrt( double a ) {
        return Math.sqrt(a);
    }
    
/*    
    public static final double sin( double x ) {
        return x - x * x * x / ( 3 * 2 ) + x*x*x*x*x/( 5*4*3*2 );
    }
    public static final double tan( double x ) {
        return sin( x );
    }
    public static final double pow( double a, double b ) {
        int i = 0;
        double ret = a;
        for ( i = 1; i < b; i++ ) {
            ret *= a;
        }
        return ret;
    }

    public static final double sqrt( double a ) {
        return a / 4;
    }
 */
}
