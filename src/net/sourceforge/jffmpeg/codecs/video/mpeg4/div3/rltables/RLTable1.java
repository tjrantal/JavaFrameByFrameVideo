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

public class RLTable1 extends RLTable {
    
    public RLTable1() {
        n = 148;
        last = 81;
        vlcCodes = new long[][] {
            { 0x4, 3 },{ 0x14, 5 },{ 0x17, 7 },{ 0x7f, 8 },
            { 0x154, 9 },{ 0x1f2, 10 },{ 0xbf, 11 },{ 0x65, 12 },
            { 0xaaa, 12 },{ 0x630, 13 },{ 0x1597, 13 },{ 0x3b7, 14 },
            { 0x2b22, 14 },{ 0xbe6, 15 },{ 0xb, 4 },{ 0x37, 7 },
            { 0x62, 9 },{ 0x7, 11 },{ 0x166, 12 },{ 0xce, 13 },
            { 0x1590, 13 },{ 0x5f6, 14 },{ 0xbe7, 15 },{ 0x7, 5 },
            { 0x6d, 8 },{ 0x3, 11 },{ 0x31f, 12 },{ 0x5f2, 14 },
            { 0x2, 6 },{ 0x61, 9 },{ 0x55, 12 },{ 0x1df, 14 },
            { 0x1a, 6 },{ 0x1e, 10 },{ 0xac9, 12 },{ 0x2b23, 14 },
            { 0x1e, 6 },{ 0x1f, 10 },{ 0xac3, 12 },{ 0x2b2b, 14 },
            { 0x6, 7 },{ 0x4, 11 },{ 0x2f8, 13 },{ 0x19, 7 },
            { 0x6, 11 },{ 0x63d, 13 },{ 0x57, 7 },{ 0x182, 11 },
            { 0x2aa2, 14 },{ 0x4, 8 },{ 0x180, 11 },{ 0x59c, 14 },
            { 0x7d, 8 },{ 0x164, 12 },{ 0x76d, 15 },{ 0x2, 9 },
            { 0x18d, 11 },{ 0x1581, 13 },{ 0xad, 8 },{ 0x60, 12 },
            { 0xc67, 14 },{ 0x1c, 9 },{ 0xee, 13 },{ 0x3, 9 },
            { 0x2cf, 13 },{ 0xd9, 9 },{ 0x1580, 13 },{ 0x2, 11 },
            { 0x183, 11 },{ 0x57, 12 },{ 0x61, 12 },{ 0x31, 11 },
            { 0x66, 12 },{ 0x631, 13 },{ 0x632, 13 },{ 0xac, 13 },
            { 0x31d, 12 },{ 0x76, 12 },{ 0x3a, 11 },{ 0x165, 12 },
            { 0xc66, 14 },{ 0x3, 2 },{ 0x54, 7 },{ 0x2ab, 10 },
            { 0x16, 13 },{ 0x5f7, 14 },{ 0x5, 4 },{ 0xf8, 9 },
            { 0xaa9, 12 },{ 0x5f, 15 },{ 0x4, 4 },{ 0x1c, 10 },
            { 0x1550, 13 },{ 0x4, 5 },{ 0x77, 11 },{ 0x76c, 15 },
            { 0xe, 5 },{ 0xa, 12 },{ 0xc, 5 },{ 0x562, 11 },
            { 0x4, 6 },{ 0x31c, 12 },{ 0x6, 6 },{ 0xc8, 13 },
            { 0xd, 6 },{ 0x1da, 13 },{ 0x7, 6 },{ 0xc9, 13 },
            { 0x1, 7 },{ 0x2e, 14 },{ 0x14, 7 },{ 0x1596, 13 },
            { 0xa, 7 },{ 0xac2, 12 },{ 0x16, 7 },{ 0x15b, 14 },
            { 0x15, 7 },{ 0x15a, 14 },{ 0xf, 8 },{ 0x5e, 15 },
            { 0x7e, 8 },{ 0xab, 8 },{ 0x2d, 9 },{ 0xd8, 9 },
            { 0xb, 9 },{ 0x14, 10 },{ 0x2b3, 10 },{ 0x1f3, 10 },
            { 0x3a, 10 },{ 0x0, 10 },{ 0x58, 10 },{ 0x2e, 9 },
            { 0x5e, 10 },{ 0x563, 11 },{ 0xec, 12 },{ 0x54, 12 },
            { 0xac1, 12 },{ 0x1556, 13 },{ 0x2fa, 13 },{ 0x181, 11 },
            { 0x1557, 13 },{ 0x59d, 14 },{ 0x2aa3, 14 },{ 0x2b2a, 14 },
            { 0x1de, 14 },{ 0x63c, 13 },{ 0xcf, 13 },{ 0x1594, 13 },
            { 0xd, 9 },
        };
        table_run = new int[] {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  2,
            2,  2,  2,  2,  3,  3,  3,  3,
            4,  4,  4,  4,  5,  5,  5,  5,
            6,  6,  6,  7,  7,  7,  8,  8,
            8,  9,  9,  9, 10, 10, 10, 11,
            11, 11, 12, 12, 12, 13, 13, 14,
            14, 15, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28,
            29,  0,  0,  0,  0,  0,  1,  1,
            1,  1,  2,  2,  2,  3,  3,  3,
            4,  4,  5,  5,  6,  6,  7,  7,
            8,  8,  9,  9, 10, 10, 11, 11,
            12, 12, 13, 13, 14, 14, 15, 15,
            16, 17, 18, 19, 20, 21, 22, 23,
            24, 25, 26, 27, 28, 29, 30, 31,
            32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43,
        };
        table_level = new int[] {
            1,  2,  3,  4,  5,  6,  7,  8,
            9, 10, 11, 12, 13, 14,  1,  2,
            3,  4,  5,  6,  7,  8,  9,  1,
            2,  3,  4,  5,  1,  2,  3,  4,
            1,  2,  3,  4,  1,  2,  3,  4,
            1,  2,  3,  1,  2,  3,  1,  2,
            3,  1,  2,  3,  1,  2,  3,  1,
            2,  3,  1,  2,  3,  1,  2,  1,
            2,  1,  2,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  2,  3,  4,  5,  1,  2,
            3,  4,  1,  2,  3,  1,  2,  3,
            1,  2,  1,  2,  1,  2,  1,  2,
            1,  2,  1,  2,  1,  2,  1,  2,
            1,  2,  1,  2,  1,  2,  1,  2,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,
        };
        calculateStats();
    }
    
}

