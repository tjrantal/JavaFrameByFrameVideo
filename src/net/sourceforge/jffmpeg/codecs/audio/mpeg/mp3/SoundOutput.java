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

import net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3.data.Table;

/**
 *
 */
public class SoundOutput {
    private static final int[] mdct_win;
    
    /* cos(pi*i/18) */
    public static final int C1_18 = FIXR(0.98480775301220805936);
    public static final int C2_18 = FIXR(0.93969262078590838405);
    public static final int C3_18 = FIXR(0.86602540378443864676);
    public static final int C4_18 = FIXR(0.76604444311897803520);
    public static final int C5_18 = FIXR(0.64278760968653932632);
    public static final int C6_18 = FIXR(0.5);
    public static final int C7_18 = FIXR(0.34202014332566873304);
    public static final int C8_18 = FIXR(0.17364817766693034885);

    /* 0.5 / cos(pi*(2*i+1)/36) */
    public static final int[] icos36 = new int[] {
        FIXR(0.50190991877167369479),
        FIXR(0.51763809020504152469),
        FIXR(0.55168895948124587824),
        FIXR(0.61038729438072803416),
        FIXR(0.70710678118654752439),
        FIXR(0.87172339781054900991),
        FIXR(1.18310079157624925896),
        FIXR(1.93185165257813657349),
        FIXR(5.73685662283492756461),
    };

    public static final int icos72[] = new int[] {
        /* 0.5 / cos(pi*(2*i+19)/72) */
        FIXR(0.74009361646113053152),
        FIXR(0.82133981585229078570),
        FIXR(0.93057949835178895673),
        FIXR(1.08284028510010010928),
        FIXR(1.30656296487637652785),
        FIXR(1.66275476171152078719),
        FIXR(2.31011315767264929558),
        FIXR(3.83064878777019433457),
        FIXR(11.46279281302667383546),

        /* 0.5 / cos(pi*(2*(i + 18) +19)/72) */
        FIXR(-0.67817085245462840086),
        FIXR(-0.63023620700513223342),
        FIXR(-0.59284452371708034528),
        FIXR(-0.56369097343317117734),
        FIXR(-0.54119610014619698439),
        FIXR(-0.52426456257040533932),
        FIXR(-0.51213975715725461845),
        FIXR(-0.50431448029007636036),
        FIXR(-0.50047634258165998492)
    };

    int[] imdct36TmpBuffer = new int[ 18 ];
    private void imdct36( int[] out, int[] in, int offset ) {
        int i, j, t0, t1, t2, t3, s0, s1, s2, s3;
        long in3_3, in6_6;

        for(i=17 + offset;i>=1 + offset;i--)
            in[i] += in[i-1];
        for(i=17 + offset;i>=3 + offset;i-=2)
            in[i] += in[i-2];

        for(j=0;j<2;j++) {
            int jin = j + offset;
            in3_3 = MUL64(in[jin + 2*3], C3_18);
            in6_6 = MUL64(in[jin + 2*6], C6_18);

            imdct36TmpBuffer[j + 0] = FRAC_RND(MUL64(in[jin + 2*1], C1_18) + in3_3 + 
                                      MUL64(in[jin + 2*5], C5_18) + MUL64(in[jin + 2*7], C7_18));
            imdct36TmpBuffer[j + 2] = in[jin + 2*0] + FRAC_RND(MUL64(in[jin + 2*2], C2_18) + 
                                          MUL64(in[jin + 2*4], C4_18) + in6_6 + 
                                          MUL64(in[jin + 2*8], C8_18));
            imdct36TmpBuffer[j + 4] = FRAC_RND(MUL64(in[jin + 2*1] - in[jin + 2*5] - in[jin + 2*7], C3_18));
            imdct36TmpBuffer[j + 6] = FRAC_RND(MUL64(in[jin + 2*2] - in[jin + 2*4] - in[jin + 2*8], C6_18)) - 
                in[jin + 2*6] + in[jin + 2*0];
            imdct36TmpBuffer[j + 8] = FRAC_RND(MUL64(in[jin + 2*1], C5_18) - in3_3 - 
                               MUL64(in[jin + 2*5], C7_18) + MUL64(in[jin + 2*7], C1_18));
            imdct36TmpBuffer[j + 10] = in[jin + 2*0] + FRAC_RND(MUL64(-in[jin + 2*2], C8_18) - 
                                           MUL64(in[jin + 2*4], C2_18) + in6_6 + 
                                           MUL64(in[jin + 2*8], C4_18));
            imdct36TmpBuffer[j + 12] = FRAC_RND(MUL64(in[jin + 2*1], C7_18) - in3_3 + 
                                MUL64(in[jin + 2*5], C1_18) - 
                                MUL64(in[jin + 2*7], C5_18));
            imdct36TmpBuffer[j + 14] = in[jin + 2*0] + FRAC_RND(MUL64(-in[jin + 2*2], C4_18) + 
                                           MUL64(in[jin + 2*4], C8_18) + in6_6 - 
                                           MUL64(in[jin + 2*8], C2_18));
            imdct36TmpBuffer[j + 16] = in[jin + 2*0] - in[jin + 2*2] + in[jin + 2*4] - in[jin + 2*6] + in[jin + 2*8];
        }

        i = 0;
        for(j=0;j<4;j++) {
            t0 = imdct36TmpBuffer[i];
            t1 = imdct36TmpBuffer[i + 2];
            s0 = t1 + t0;
            s2 = t1 - t0;

            t2 = imdct36TmpBuffer[i + 1];
            t3 = imdct36TmpBuffer[i + 3];
            s1 = MULL(t3 + t2, icos36[j]);
            s3 = MULL(t3 - t2, icos36[8 - j]);

            t0 = MULL(s0 + s1, icos72[9 + 8 - j]);
            t1 = MULL(s0 - s1, icos72[8 - j]);
            out[18 + 9 + j] = t0;
            out[18 + 8 - j] = t0;
            out[9 + j] = -t1;
            out[8 - j] = t1;

            t0 = MULL(s2 + s3, icos72[9+j]);
            t1 = MULL(s2 - s3, icos72[j]);
            out[18 + 9 + (8 - j)] = t0;
            out[18 + j] = t0;
            out[9 + (8 - j)] = -t1;
            out[j] = t1;
            i += 4;
        }

        s0 = imdct36TmpBuffer[16];
        s1 = MULL(imdct36TmpBuffer[17], icos36[4]);
        t0 = MULL(s0 + s1, icos72[9 + 4]);
        t1 = MULL(s0 - s1, icos72[4]);
        out[18 + 9 + 4] = t0;
        out[18 + 8 - 4] = t0;
        out[9 + 4] = -t1;
        out[8 - 4] = t1;        
    }
        
/* cos(pi*i/24) */
    public static final int C1 = FIXR(0.99144486137381041114);
    public static final int C3 = FIXR(0.92387953251128675612);
    public static final int C5 = FIXR(0.79335334029123516458);
    public static final int C7 = FIXR(0.60876142900872063941);
    public static final int C9 = FIXR(0.38268343236508977173);
    public static final int C11 = FIXR(0.13052619222005159154);

    private void imdct12( int[] out, int[] in ) {
        int tmp;
        long in1_3, in1_9, in4_3, in4_9;

        in1_3 = MUL64(in[1], C3);
        in1_9 = MUL64(in[1], C9);
        in4_3 = MUL64(in[4], C3);
        in4_9 = MUL64(in[4], C9);

        tmp = FRAC_RND(MUL64(in[0], C7) - in1_3 - MUL64(in[2], C11) + 
                       MUL64(in[3], C1) - in4_9 - MUL64(in[5], C5));
        out[0] = tmp;
        out[5] = -tmp;
        tmp = FRAC_RND(MUL64(in[0] - in[3], C9) - in1_3 + 
                       MUL64(in[2] + in[5], C3) - in4_9);
        out[1] = tmp;
        out[4] = -tmp;
        tmp = FRAC_RND(MUL64(in[0], C11) - in1_9 + MUL64(in[2], C7) -
                       MUL64(in[3], C5) + in4_3 - MUL64(in[5], C1));
        out[2] = tmp;
        out[3] = -tmp;
        tmp = FRAC_RND(MUL64(-in[0], C5) + in1_9 + MUL64(in[2], C1) + 
                       MUL64(in[3], C11) - in4_3 - MUL64(in[5], C7));
        out[6] = tmp;
        out[11] = tmp;
        tmp = FRAC_RND(MUL64(-in[0] + in[3], C3) - in1_9 + 
                       MUL64(in[2] + in[5], C9) + in4_3);
        out[7] = tmp;
        out[10] = tmp;
        tmp = FRAC_RND(-MUL64(in[0], C1) - in1_3 - MUL64(in[2], C5) -
                       MUL64(in[3], C7) - in4_9 - MUL64(in[5], C11));
        out[8] = tmp;
        out[9] = tmp;
    }
    
    int[] computerImdctTmpBuffer = new int[ 36 ];
    int[] computerImdctTmpBuffer2 = new int[ 12 ];
    int[] computerImdctTmpIn = new int[ 6 ];
    public void computeImdct( Granule granule,
                              int[]   sb_samples,
                              int[]   mdct_buf ) {
        int[] sb_hybrid = granule.getSbHybrid();

        /* Caluculate sblimit (the last non-zero block) */
        int pointer  = 576;
        int pointer1 = 2 * 18;
        while ( pointer >= pointer1 ) {
            pointer -= 6;
            if ( (sb_hybrid[ pointer     ] | sb_hybrid[ pointer + 1 ]
                | sb_hybrid[ pointer + 2 ] | sb_hybrid[ pointer + 3 ]
                | sb_hybrid[ pointer + 4 ] | sb_hybrid[ pointer + 5 ]) != 0 ) {
                break;
            }
        }
        int sblimit = pointer / 18 + 1;

        int bufferPointer = 0;
        
        /* Calculate the end of the long section */
        int mdct_long_end = 0;
        if ( granule.getBlockType() == 2 ) {
            if ( granule.getSwitchPoint() ) {
                mdct_long_end = 2;
            }
        } else {
            mdct_long_end = sblimit;
        }
        
        /* Long wavelengths */
        pointer = 0;
        for ( int j = 0; j < mdct_long_end; j++ ) {
            imdct36( computerImdctTmpBuffer, sb_hybrid, pointer );
            
/*            System.out.println( "imdct36" );
            for ( int p = 0; p < out.length; p++ ) {
                System.out.print( out[ p ] + " " );
            }
            System.out.println();
*/            
            int outPtr = j;
            int win1;
            if ( granule.getSwitchPoint() && j < 2 ) {
                win1 = 0;
            } else {
                win1 = 36 * granule.getBlockType();
            }
            
            win1 += (4 * 36) & (-(j & 1) );
            for ( int i = 0; i < 18; i++ ) {
                sb_samples[ outPtr ] = MULL( computerImdctTmpBuffer[ i ], mdct_win[ win1 + i ] ) + mdct_buf[ bufferPointer + i];
                mdct_buf[ bufferPointer + i ] = MULL( computerImdctTmpBuffer[ i + 18 ], mdct_win[ win1 + i + 18 ] );
                outPtr += Granule.SBLIMIT;
/*                
                System.out.println(  out[ i ] + " " + mdct_win[ win1 + i ] + " "
                                   + out[ i+18 ] + " " + mdct_win[ win1 + i + 18 ] );
 */
            }
            pointer += 18;
            bufferPointer += 18;
        }
        
//        System.out.println( "Point A" );
        for ( int j = mdct_long_end; j < sblimit; j++ ) {
            for ( int i = 0; i < 6; i++ ) {
                computerImdctTmpBuffer[ i ]      = 0;
                computerImdctTmpBuffer[ i + 6  ] = 0;
                computerImdctTmpBuffer[ i + 30 ] = 0;
            }
            int win = (4 * 36) & (-(j & 1) );
            int buf2 = 6;
            for ( int k = 0; k < 3; k++ ) {
                int ptr1 = pointer + k;
                for ( int i = 0; i < 6; i++ ) {
                    computerImdctTmpIn[ i ] = sb_hybrid[ ptr1 ];
                    ptr1 += 3;
//                    System.out.println( "--" + in[ i ] );
                }
                imdct12( computerImdctTmpBuffer2, computerImdctTmpIn );
                for ( int i = 0; i < 6; i++ ) {
                    computerImdctTmpBuffer[ buf2 + i     ] = MULL( computerImdctTmpBuffer2[ i     ], mdct_win[ 2 * 36 + win + i ] ) + computerImdctTmpBuffer[ buf2 + i ];
                    computerImdctTmpBuffer[ buf2 + i + 6 ] = MULL( computerImdctTmpBuffer2[ i + 6 ], mdct_win[ 2 * 36 + win + i + 6 ] );
/*
                    System.out.println(  out2[ i ] + " " + mdct_win[ 2 * 36 + win + i ] + " "
                                       + out2[ i+6 ] + " " + mdct_win[ 2 * 36 + win + i + 6 ] );
 */
                }
                buf2 += 6;
            }
            /* Overlap */
            int out_ptr = 0;
            for ( int i = 0; i < 18; i++ ) {
                sb_samples[ j + out_ptr ] = computerImdctTmpBuffer[ i ] + mdct_buf[ bufferPointer + i ];
                mdct_buf[ bufferPointer + i ] = computerImdctTmpBuffer[ i + 18 ];
                out_ptr += Granule.SBLIMIT;
            }
            bufferPointer += 18;
            pointer += 18;
        }
/*        System.out.println( "Sample dump A" );
        for ( int i = 0; i < 576; i++ ) {
            System.out.print( " " + sb_samples[i] );
            if ( (i % 18) == 17 ) System.out.println();
        }
 */
        /* Zero bands */
        for ( int j = sblimit; j < Granule.SBLIMIT; j++ ) {
            int out_ptr = j;
            for ( int i = 0; i < 18; i++ ) {
                sb_samples[ out_ptr ] = mdct_buf[ bufferPointer + i ];
                mdct_buf[ bufferPointer + i ] = 0;
                out_ptr += Granule.SBLIMIT;
            }
            bufferPointer += 18;
        }
        /*
        System.out.println( "Sample dump" );
        for ( int i = 0; i < 576; i++ ) {
            System.out.print( " " + sb_samples[i] );
            if ( (i % 18) == 17 ) System.out.println();
        }
         **/
    }
    
    /* butterfly operator */
    private final void BF(int[] tab, int tabPointer, int a, int b, int c) {
        a += tabPointer;
        b+= tabPointer;
        int tmp0 = tab[a] + tab[b];
        int tmp1 = tab[a] - tab[b];
        tab[a] = tmp0;
        tab[b] = MULL(tmp1, c);
    }
    
    public final void BF1(int[] tab, int tabPointer, int a, int b, int c, int d) {
        BF(tab, tabPointer, a, b, COS4_0);
        BF(tab, tabPointer, c, d, -COS4_0);
        tab[tabPointer + c] += tab[tabPointer + d];
    }
    
    public final void BF2(int[] tab, int tabPointer, int a, int b, int c, int d) {
        BF(tab, tabPointer, a, b, COS4_0);
        BF(tab, tabPointer, c, d, -COS4_0);
        tab[tabPointer + c] += tab[tabPointer + d];
        tab[tabPointer + a] += tab[tabPointer + c];
        tab[tabPointer + c] += tab[tabPointer + b];
        tab[tabPointer + b] += tab[tabPointer + d];
    }

    /* cos(i*pi/64) */
    public static final int COS0_0  = FIXR(0.50060299823519630134);
    public static final int COS0_1  = FIXR(0.50547095989754365998);
    public static final int COS0_2  = FIXR(0.51544730992262454697);
    public static final int COS0_3  = FIXR(0.53104259108978417447);
    public static final int COS0_4  = FIXR(0.55310389603444452782);
    public static final int COS0_5  = FIXR(0.58293496820613387367);
    public static final int COS0_6  = FIXR(0.62250412303566481615);
    public static final int COS0_7  = FIXR(0.67480834145500574602);
    public static final int COS0_8  = FIXR(0.74453627100229844977);
    public static final int COS0_9  = FIXR(0.83934964541552703873);
    public static final int COS0_10 = FIXR(0.97256823786196069369);
    public static final int COS0_11 = FIXR(1.16943993343288495515);
    public static final int COS0_12 = FIXR(1.48416461631416627724);
    public static final int COS0_13 = FIXR(2.05778100995341155085);
    public static final int COS0_14 = FIXR(3.40760841846871878570);
    public static final int COS0_15 = FIXR(10.19000812354805681150);

    public static final int COS1_0 = FIXR(0.50241928618815570551);
    public static final int COS1_1 = FIXR(0.52249861493968888062);
    public static final int COS1_2 = FIXR(0.56694403481635770368);
    public static final int COS1_3 = FIXR(0.64682178335999012954);
    public static final int COS1_4 = FIXR(0.78815462345125022473);
    public static final int COS1_5 = FIXR(1.06067768599034747134);
    public static final int COS1_6 = FIXR(1.72244709823833392782);
    public static final int COS1_7 = FIXR(5.10114861868916385802);

    public static final int COS2_0 = FIXR(0.50979557910415916894);
    public static final int COS2_1 = FIXR(0.60134488693504528054);
    public static final int COS2_2 = FIXR(0.89997622313641570463);
    public static final int COS2_3 = FIXR(2.56291544774150617881);

    public static final int COS3_0 = FIXR(0.54119610014619698439);
    public static final int COS3_1 = FIXR(1.30656296487637652785);

    public static final int COS4_0 = FIXR(0.70710678118654752439);

    /* DCT32 without 1/sqrt(2) coef zero scaling. */
    private void dct32(int[] out, int in[], int inPointer) {
        /* pass 1 */
        BF(in, inPointer, 0, 31, COS0_0);
        BF(in, inPointer, 1, 30, COS0_1);
        BF(in, inPointer, 2, 29, COS0_2);
        BF(in, inPointer, 3, 28, COS0_3);
        BF(in, inPointer, 4, 27, COS0_4);
        BF(in, inPointer, 5, 26, COS0_5);
        BF(in, inPointer, 6, 25, COS0_6);
        BF(in, inPointer, 7, 24, COS0_7);
        BF(in, inPointer, 8, 23, COS0_8);
        BF(in, inPointer, 9, 22, COS0_9);
        BF(in, inPointer, 10, 21, COS0_10);
        BF(in, inPointer, 11, 20, COS0_11);
        BF(in, inPointer, 12, 19, COS0_12);
        BF(in, inPointer, 13, 18, COS0_13);
        BF(in, inPointer, 14, 17, COS0_14);
        BF(in, inPointer, 15, 16, COS0_15);

        /* pass 2 */
        BF(in, inPointer, 0, 15, COS1_0);
        BF(in, inPointer, 1, 14, COS1_1);
        BF(in, inPointer, 2, 13, COS1_2);
        BF(in, inPointer, 3, 12, COS1_3);
        BF(in, inPointer, 4, 11, COS1_4);
        BF(in, inPointer, 5, 10, COS1_5);
        BF(in, inPointer, 6,  9, COS1_6);
        BF(in, inPointer, 7,  8, COS1_7);

        BF(in, inPointer, 16, 31, -COS1_0);
        BF(in, inPointer, 17, 30, -COS1_1);
        BF(in, inPointer, 18, 29, -COS1_2);
        BF(in, inPointer, 19, 28, -COS1_3);
        BF(in, inPointer, 20, 27, -COS1_4);
        BF(in, inPointer, 21, 26, -COS1_5);
        BF(in, inPointer, 22, 25, -COS1_6);
        BF(in, inPointer, 23, 24, -COS1_7);

        /* pass 3 */
        BF(in, inPointer, 0, 7, COS2_0);
        BF(in, inPointer, 1, 6, COS2_1);
        BF(in, inPointer, 2, 5, COS2_2);
        BF(in, inPointer, 3, 4, COS2_3);

        BF(in, inPointer, 8, 15, -COS2_0);
        BF(in, inPointer, 9, 14, -COS2_1);
        BF(in, inPointer, 10, 13, -COS2_2);
        BF(in, inPointer, 11, 12, -COS2_3);

        BF(in, inPointer, 16, 23, COS2_0);
        BF(in, inPointer, 17, 22, COS2_1);
        BF(in, inPointer, 18, 21, COS2_2);
        BF(in, inPointer, 19, 20, COS2_3);

        BF(in, inPointer, 24, 31, -COS2_0);
        BF(in, inPointer, 25, 30, -COS2_1);
        BF(in, inPointer, 26, 29, -COS2_2);
        BF(in, inPointer, 27, 28, -COS2_3);

        /* pass 4 */
        BF(in, inPointer, 0, 3, COS3_0);
        BF(in, inPointer, 1, 2, COS3_1);

        BF(in, inPointer, 4, 7, -COS3_0);
        BF(in, inPointer, 5, 6, -COS3_1);

        BF(in, inPointer, 8, 11, COS3_0);
        BF(in, inPointer, 9, 10, COS3_1);

        BF(in, inPointer, 12, 15, -COS3_0);
        BF(in, inPointer, 13, 14, -COS3_1);

        BF(in, inPointer, 16, 19, COS3_0);
        BF(in, inPointer, 17, 18, COS3_1);

        BF(in, inPointer, 20, 23, -COS3_0);
        BF(in, inPointer, 21, 22, -COS3_1);

        BF(in, inPointer, 24, 27, COS3_0);
        BF(in, inPointer, 25, 26, COS3_1);

        BF(in, inPointer, 28, 31, -COS3_0);
        BF(in, inPointer, 29, 30, -COS3_1);
    
        /* pass 5 */
        BF1(in, inPointer, 0, 1, 2, 3);
        BF2(in, inPointer, 4, 5, 6, 7);
        BF1(in, inPointer, 8, 9, 10, 11);
        BF2(in, inPointer, 12, 13, 14, 15);
        BF1(in, inPointer, 16, 17, 18, 19);
        BF2(in, inPointer, 20, 21, 22, 23);
        BF1(in, inPointer, 24, 25, 26, 27);
        BF2(in, inPointer, 28, 29, 30, 31);
    
        /* pass 6 */
        in[ inPointer + 8  ] += in[ inPointer + 12 ];
        in[ inPointer + 12 ] += in[ inPointer + 10 ];
        in[ inPointer + 10 ] += in[ inPointer + 14 ];
        in[ inPointer + 14 ] += in[ inPointer + 9  ];
        in[ inPointer + 9  ] += in[ inPointer + 13 ];
        in[ inPointer + 13 ] += in[ inPointer + 11 ];
        in[ inPointer + 11 ] += in[ inPointer + 15 ];

        out[ 0] = in[ inPointer + 0];
        out[16] = in[ inPointer + 1];
        out[ 8] = in[ inPointer + 2];
        out[24] = in[ inPointer + 3];
        out[ 4] = in[ inPointer + 4];
        out[20] = in[ inPointer + 5];
        out[12] = in[ inPointer + 6];
        out[28] = in[ inPointer + 7];
        out[ 2] = in[ inPointer + 8];
        out[18] = in[ inPointer + 9];
        out[10] = in[ inPointer + 10];
        out[26] = in[ inPointer + 11];
        out[ 6] = in[ inPointer + 12];
        out[22] = in[ inPointer + 13];
        out[14] = in[ inPointer + 14];
        out[30] = in[ inPointer + 15];
    
        in[ inPointer + 24 ] += in[ inPointer + 28 ];
        in[ inPointer + 28 ] += in[ inPointer + 26 ];
        in[ inPointer + 26 ] += in[ inPointer + 30 ];
        in[ inPointer + 30 ] += in[ inPointer + 25 ];
        in[ inPointer + 25 ] += in[ inPointer + 29 ];
        in[ inPointer + 29 ] += in[ inPointer + 27 ];
        in[ inPointer + 27 ] += in[ inPointer + 31 ];

        out[ 1] = in[ inPointer + 16] + in[ inPointer + 24];
        out[17] = in[ inPointer + 17] + in[ inPointer + 25];
        out[ 9] = in[ inPointer + 18] + in[ inPointer + 26];
        out[25] = in[ inPointer + 19] + in[ inPointer + 27];
        out[ 5] = in[ inPointer + 20] + in[ inPointer + 28];
        out[21] = in[ inPointer + 21] + in[ inPointer + 29];
        out[13] = in[ inPointer + 22] + in[ inPointer + 30];
        out[29] = in[ inPointer + 23] + in[ inPointer + 31];
        out[ 3] = in[ inPointer + 24] + in[ inPointer + 20];
        out[19] = in[ inPointer + 25] + in[ inPointer + 21];
        out[11] = in[ inPointer + 26] + in[ inPointer + 22];
        out[27] = in[ inPointer + 27] + in[ inPointer + 23];
        out[ 7] = in[ inPointer + 28] + in[ inPointer + 18];
        out[23] = in[ inPointer + 29] + in[ inPointer + 19];
        out[15] = in[ inPointer + 30] + in[ inPointer + 17];
        out[31] = in[ inPointer + 31];
}

    private long SUM8( long sum, int windowOffset, int[] synth_buffer, int bufferPointer ) {
        sum += MUL64( window[ 0 * 64 + windowOffset ], synth_buffer[ 0 * 64 + bufferPointer ] );
        sum += MUL64( window[ 1 * 64 + windowOffset ], synth_buffer[ 1 * 64 + bufferPointer ] );
        sum += MUL64( window[ 2 * 64 + windowOffset ], synth_buffer[ 2 * 64 + bufferPointer ] );
        sum += MUL64( window[ 3 * 64 + windowOffset ], synth_buffer[ 3 * 64 + bufferPointer ] );
        sum += MUL64( window[ 4 * 64 + windowOffset ], synth_buffer[ 4 * 64 + bufferPointer ] );
        sum += MUL64( window[ 5 * 64 + windowOffset ], synth_buffer[ 5 * 64 + bufferPointer ] );
        sum += MUL64( window[ 6 * 64 + windowOffset ], synth_buffer[ 6 * 64 + bufferPointer ] );
        sum += MUL64( window[ 7 * 64 + windowOffset ], synth_buffer[ 7 * 64 + bufferPointer ] );
        return sum;
    }

    private long SUB8( long sum, int windowOffset, int[] synth_buffer, int bufferPointer ) {
        sum -= MUL64( window[ 0 * 64 + windowOffset ], synth_buffer[ 0 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 1 * 64 + windowOffset ], synth_buffer[ 1 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 2 * 64 + windowOffset ], synth_buffer[ 2 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 3 * 64 + windowOffset ], synth_buffer[ 3 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 4 * 64 + windowOffset ], synth_buffer[ 4 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 5 * 64 + windowOffset ], synth_buffer[ 5 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 6 * 64 + windowOffset ], synth_buffer[ 6 * 64 + bufferPointer ] );
        sum -= MUL64( window[ 7 * 64 + windowOffset ], synth_buffer[ 7 * 64 + bufferPointer ] );
        return sum;
    }
    
    private void OUT_SAMPLE(long sum, byte[] out, int outPointer) {
        int sum1 = (int)((sum + (((long)1) << (OUT_SHIFT - 1))) >> OUT_SHIFT);
             if ( sum1 < -32768 ) { sum1 = -32768;}
        else if ( sum1 >  32767 ) { sum1 = 32767;}
//        System.out.println( sum  );
        out[ outPointer ]     = (byte)(sum1 & 0xff);
        out[ outPointer + 1 ] = (byte)((sum1 >> 8) & 0xff);
    }

    
    private int[]   synth_buf_offset = new int[ 2 ];
    private int[][] synth_buf        = new int[ 2 ][ 512 * 2 ];
    
    private int[] synth_filterTmpBuffer = new int[32];
    public int synth_filter( int channel, int nb_channels,
                             byte[] out, int outPointer, 
                             int[] in, int inPointer ) {
/*        MPA_INT *synth_buf, *p;
        MPA_INT *w;
        int j, offset, v;
        long sum;
*/
        dct32(synth_filterTmpBuffer, in, inPointer);
        
if ( Granule.debug ) {
    System.out.println( "dct32" );
        for ( int i = 0; i < 32; i++ ) {
            System.out.print( synth_filterTmpBuffer[ i ] + " " );
        }
        System.out.println();
}
        int offset = synth_buf_offset[channel];
        int[] synth_buffer = synth_buf[channel];

        for(int j=0;j<32;j++) {
            synth_buffer[ j + offset ] = synth_filterTmpBuffer[ j ];
        }
        
        /* copy to avoid wrap */
        System.arraycopy( synth_buffer, offset, synth_buffer, offset + 512, 32 );
        
/* I am here */
//        System.out.println( "A" );
        int windowOffset = 0;
        long sum = 0;
        int p;
        for(int j=0;j<16;j++) {
            sum = 0;
            p = offset + 16 + j;    /* 0-15  */
            sum = SUM8(sum, windowOffset, synth_buffer, p);
            p = offset + 48 - j;    /* 32-47 */
            sum = SUB8(sum, windowOffset + 32, synth_buffer, p);

            OUT_SAMPLE(sum, out, outPointer);
            outPointer += 2 * nb_channels;  // CHECKME
            windowOffset++;
        }

//        System.out.println( "B" );
        p = offset + 32; /* 48 */
        sum = 0;
        sum = SUB8(sum, windowOffset + 32, synth_buffer, p);
        OUT_SAMPLE(sum, out, outPointer);
        outPointer += 2 * nb_channels;  // CHECKME
        windowOffset++;

//        System.out.println( "C" );
        for(int j=17;j<32;j++) {
            sum = 0;
            p = offset + 48 - j;    /* 0-15  */
            sum = SUB8(sum, windowOffset, synth_buffer, p);
            p = offset + 16 + j;    /* 32-47 */
            sum = SUB8(sum, windowOffset + 32, synth_buffer, p);

            OUT_SAMPLE(sum, out, outPointer);
            outPointer += 2 * nb_channels;  // CHECKME
            windowOffset++;
        }
        offset = (offset - 32) & 511;
        synth_buf_offset[channel] = offset;
        
        return outPointer;
    }

    
    private static final int[]   window           = new int[ 512 ];
    private static final int FRAC_BITS = 23;
    private static final int WFRAC_BITS = 16;
    public static final int OUT_SHIFT = WFRAC_BITS + FRAC_BITS - 15;
    
    private static final int MULL( int a, int b ) {
        return (int)(((long)a * (long)b) >> FRAC_BITS);
    }
    
    static {
        mdct_win = new int[ 8 * 36 ];

        int i,j;
        /* compute mdct windows */
        for(i=0;i<36;i++) {
            int v;
            v = FIXR(Granule.sin(Granule.M_PI * (i + 0.5) / 36.0));
            mdct_win[0 * 36 + i] = v;
            mdct_win[1 * 36 + i] = v;
            mdct_win[3 * 36 + i] = v;
        }
        for(i=0;i<6;i++) {
            mdct_win[1 * 36 + 18 + i] = FIXR(1.0);
            mdct_win[1 * 36 + 24 + i] = FIXR(Granule.sin(Granule.M_PI * ((i + 6) + 0.5) / 12.0));
            mdct_win[1 * 36 + 30 + i] = FIXR(0.0);

            mdct_win[3 * 36 + i] = FIXR(0.0);
            mdct_win[3 * 36 + 6 + i] = FIXR(Granule.sin(Granule.M_PI * (i + 0.5) / 12.0));
            mdct_win[3 * 36 + 12 + i] = FIXR(1.0);
        }

        for(i=0;i<12;i++)
            mdct_win[2 * 36 + i] = FIXR(Granule.sin(Granule.M_PI * (i + 0.5) / 12.0));
        
        /* NOTE: we do frequency inversion adter the MDCT by changing
           the sign of the right window coefs */
        for(j=0;j<4;j++) {
            for(i=0;i<36;i+=2) {
                mdct_win[ (j + 4) * 36 + i ]     = mdct_win[j * 36 + i];
                mdct_win[ (j + 4) * 36 + i + 1 ] = -mdct_win[j * 36 + i + 1];
            }
        }
/*
        for(j=0;j<8;j++) {
//            System.out.println( "win" + j + "=" );
            for(i=0;i<36;i++)
                System.out.print( "" + mdct_win[j * 36 + i] + ", " );
            System.out.println();
        }
*/
        /* window */
        /* max = 18760, max sum over all 16 coefs : 44736 */
        int[] mpa_enwindow = Table.getMpaEnTable();
        for(i=0;i<257;i++) {
            int v;
            v = mpa_enwindow[i];
            window[i] = v;
            if ((i & 63) != 0)
                v = -v;
            if (i != 0)
                window[512 - i] = v;
        }
    }
    
    public static final int FIXR( double a ) {
        return (int)(a * (1 << FRAC_BITS) + 0.5);
    }
    
    public static final long MUL64( long a, long b ) {
        return a * b;
    }
    
    public static int FRAC_RND( long a ) {
        return (int)((a + ((1 << FRAC_BITS)/2)) >> FRAC_BITS);
    }
}
