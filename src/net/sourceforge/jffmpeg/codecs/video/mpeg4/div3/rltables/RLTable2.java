/*
 * Java port of parts of the ffmpeg Mpeg4 base decoder.
 * Copyright (c) 2003 Jonathan Hueber.
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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.rltables;

public class RLTable2 extends RLTable {
    
    public RLTable2() {
        super();
        n = 185;
        last = 119;
        vlcCodes = new long[][] {
            { 0x1, 2 },{ 0x5, 3 },{ 0xd, 4 },{ 0x12, 5 },
            { 0xe, 6 },{ 0x15, 7 },{ 0x13, 8 },{ 0x3f, 8 },
            { 0x4b, 9 },{ 0x11f, 9 },{ 0xb8, 10 },{ 0x3e3, 10 },
            { 0x172, 11 },{ 0x24d, 12 },{ 0x3da, 12 },{ 0x2dd, 13 },
            { 0x1f55, 13 },{ 0x5b9, 14 },{ 0x3eae, 14 },{ 0x0, 4 },
            { 0x10, 5 },{ 0x8, 7 },{ 0x20, 8 },{ 0x29, 9 },
            { 0x1f4, 9 },{ 0x233, 10 },{ 0x1e0, 11 },{ 0x12a, 12 },
            { 0x3dd, 12 },{ 0x50a, 13 },{ 0x1f29, 13 },{ 0xa42, 14 },
            { 0x1272, 15 },{ 0x1737, 15 },{ 0x3, 5 },{ 0x11, 7 },
            { 0xc4, 8 },{ 0x4b, 10 },{ 0xb4, 11 },{ 0x7d4, 11 },
            { 0x345, 12 },{ 0x2d7, 13 },{ 0x7bf, 13 },{ 0x938, 14 },
            { 0xbbb, 14 },{ 0x95e, 15 },{ 0x13, 5 },{ 0x78, 7 },
            { 0x69, 9 },{ 0x232, 10 },{ 0x461, 11 },{ 0x3ec, 12 },
            { 0x520, 13 },{ 0x1f2a, 13 },{ 0x3e50, 14 },{ 0x3e51, 14 },
            { 0x1486, 15 },{ 0xc, 6 },{ 0x24, 9 },{ 0x94, 11 },
            { 0x8c0, 12 },{ 0xf09, 14 },{ 0x1ef0, 15 },{ 0x3d, 6 },
            { 0x53, 9 },{ 0x1a0, 11 },{ 0x2d6, 13 },{ 0xf08, 14 },
            { 0x13, 7 },{ 0x7c, 9 },{ 0x7c1, 11 },{ 0x4ac, 14 },
            { 0x1b, 7 },{ 0xa0, 10 },{ 0x344, 12 },{ 0xf79, 14 },
            { 0x79, 7 },{ 0x3e1, 10 },{ 0x2d4, 13 },{ 0x2306, 14 },
            { 0x21, 8 },{ 0x23c, 10 },{ 0xfae, 12 },{ 0x23de, 14 },
            { 0x35, 8 },{ 0x175, 11 },{ 0x7b3, 13 },{ 0xc5, 8 },
            { 0x174, 11 },{ 0x785, 13 },{ 0x48, 9 },{ 0x1a3, 11 },
            { 0x49e, 13 },{ 0x2c, 9 },{ 0xfa, 10 },{ 0x7d6, 11 },
            { 0x92, 10 },{ 0x5cc, 13 },{ 0x1ef1, 15 },{ 0xa3, 10 },
            { 0x3ed, 12 },{ 0x93e, 14 },{ 0x1e2, 11 },{ 0x1273, 15 },
            { 0x7c4, 11 },{ 0x1487, 15 },{ 0x291, 12 },{ 0x293, 12 },
            { 0xf8a, 12 },{ 0x509, 13 },{ 0x508, 13 },{ 0x78d, 13 },
            { 0x7be, 13 },{ 0x78c, 13 },{ 0x4ae, 14 },{ 0xbba, 14 },
            { 0x2307, 14 },{ 0xb9a, 14 },{ 0x1736, 15 },{ 0xe, 4 },
            { 0x45, 7 },{ 0x1f3, 9 },{ 0x47a, 11 },{ 0x5dc, 13 },
            { 0x23df, 14 },{ 0x19, 5 },{ 0x28, 9 },{ 0x176, 11 },
            { 0x49d, 13 },{ 0x23dd, 14 },{ 0x30, 6 },{ 0xa2, 10 },
            { 0x2ef, 12 },{ 0x5b8, 14 },{ 0x3f, 6 },{ 0xa5, 10 },
            { 0x3db, 12 },{ 0x93f, 14 },{ 0x44, 7 },{ 0x7cb, 11 },
            { 0x95f, 15 },{ 0x63, 7 },{ 0x3c3, 12 },{ 0x15, 8 },
            { 0x8f6, 12 },{ 0x17, 8 },{ 0x498, 13 },{ 0x2c, 8 },
            { 0x7b2, 13 },{ 0x2f, 8 },{ 0x1f54, 13 },{ 0x8d, 8 },
            { 0x7bd, 13 },{ 0x8e, 8 },{ 0x1182, 13 },{ 0xfb, 8 },
            { 0x50b, 13 },{ 0x2d, 8 },{ 0x7c0, 11 },{ 0x79, 9 },
            { 0x1f5f, 13 },{ 0x7a, 9 },{ 0x1f56, 13 },{ 0x231, 10 },
            { 0x3e4, 10 },{ 0x1a1, 11 },{ 0x143, 11 },{ 0x1f7, 11 },
            { 0x16f, 12 },{ 0x292, 12 },{ 0x2e7, 12 },{ 0x16c, 12 },
            { 0x16d, 12 },{ 0x3dc, 12 },{ 0xf8b, 12 },{ 0x499, 13 },
            { 0x3d8, 12 },{ 0x78e, 13 },{ 0x2d5, 13 },{ 0x1f5e, 13 },
            { 0x1f2b, 13 },{ 0x78f, 13 },{ 0x4ad, 14 },{ 0x3eaf, 14 },
            { 0x23dc, 14 },{ 0x4a, 9 },
        };
        table_run = new int[] {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  2,  2,  2,  2,  2,  2,
            2,  2,  2,  2,  2,  2,  3,  3,
            3,  3,  3,  3,  3,  3,  3,  3,
            3,  4,  4,  4,  4,  4,  4,  5,
            5,  5,  5,  5,  6,  6,  6,  6,
            7,  7,  7,  7,  8,  8,  8,  8,
            9,  9,  9,  9, 10, 10, 10, 11,
            11, 11, 12, 12, 12, 13, 13, 13,
            14, 14, 14, 15, 15, 15, 16, 16,
            17, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 26, 27, 28, 29, 30,  0,
            0,  0,  0,  0,  0,  1,  1,  1,
            1,  1,  2,  2,  2,  2,  3,  3,
            3,  3,  4,  4,  4,  5,  5,  6,
            6,  7,  7,  8,  8,  9,  9, 10,
            10, 11, 11, 12, 12, 13, 13, 14,
            14, 15, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28,
            29, 30, 31, 32, 33, 34, 35, 36,
            37,
        };
        table_level = new int[] {
            1,  2,  3,  4,  5,  6,  7,  8,
            9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19,  1,  2,  3,  4,  5,
            6,  7,  8,  9, 10, 11, 12, 13,
            14, 15,  1,  2,  3,  4,  5,  6,
            7,  8,  9, 10, 11, 12,  1,  2,
            3,  4,  5,  6,  7,  8,  9, 10,
            11,  1,  2,  3,  4,  5,  6,  1,
            2,  3,  4,  5,  1,  2,  3,  4,
            1,  2,  3,  4,  1,  2,  3,  4,
            1,  2,  3,  4,  1,  2,  3,  1,
            2,  3,  1,  2,  3,  1,  2,  3,
            1,  2,  3,  1,  2,  3,  1,  2,
            1,  2,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            2,  3,  4,  5,  6,  1,  2,  3,
            4,  5,  1,  2,  3,  4,  1,  2,
            3,  4,  1,  2,  3,  1,  2,  1,
            2,  1,  2,  1,  2,  1,  2,  1,
            2,  1,  2,  1,  2,  1,  2,  1,
            2,  1,  2,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,
        };
        calculateStats();
    }
    
}

