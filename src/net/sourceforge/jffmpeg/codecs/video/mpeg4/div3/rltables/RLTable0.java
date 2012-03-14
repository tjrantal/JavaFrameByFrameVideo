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

public class RLTable0 extends RLTable {
    
    public RLTable0() {
        super();
        n = 132;
        last = 85;
        vlcCodes = new long[][]  {
            { 0x1, 2 },{ 0x6, 3 },{ 0xf, 4 },{ 0x16, 5 },
            { 0x20, 6 },{ 0x18, 7 },{ 0x8, 8 },{ 0x9a, 8 },
            { 0x56, 9 },{ 0x13e, 9 },{ 0xf0, 10 },{ 0x3a5, 10 },
            { 0x77, 11 },{ 0x1ef, 11 },{ 0x9a, 12 },{ 0x5d, 13 },
            { 0x1, 4 },{ 0x11, 5 },{ 0x2, 7 },{ 0xb, 8 },
            { 0x12, 9 },{ 0x1d6, 9 },{ 0x27e, 10 },{ 0x191, 11 },
            { 0xea, 12 },{ 0x3dc, 12 },{ 0x13b, 13 },{ 0x4, 5 },
            { 0x14, 7 },{ 0x9e, 8 },{ 0x9, 10 },{ 0x1ac, 11 },
            { 0x1e2, 11 },{ 0x3ca, 12 },{ 0x5f, 13 },{ 0x17, 5 },
            { 0x4e, 7 },{ 0x5e, 9 },{ 0xf3, 10 },{ 0x1ad, 11 },
            { 0xec, 12 },{ 0x5f0, 13 },{ 0xe, 6 },{ 0xe1, 8 },
            { 0x3a4, 10 },{ 0x9c, 12 },{ 0x13d, 13 },{ 0x3b, 6 },
            { 0x1c, 9 },{ 0x14, 11 },{ 0x9be, 12 },{ 0x6, 7 },
            { 0x7a, 9 },{ 0x190, 11 },{ 0x137, 13 },{ 0x1b, 7 },
            { 0x8, 10 },{ 0x75c, 11 },{ 0x71, 7 },{ 0xd7, 10 },
            { 0x9bf, 12 },{ 0x7, 8 },{ 0xaf, 10 },{ 0x4cc, 11 },
            { 0x34, 8 },{ 0x265, 10 },{ 0x9f, 12 },{ 0xe0, 8 },
            { 0x16, 11 },{ 0x327, 12 },{ 0x15, 9 },{ 0x17d, 11 },
            { 0xebb, 12 },{ 0x14, 9 },{ 0xf6, 10 },{ 0x1e4, 11 },
            { 0xcb, 10 },{ 0x99d, 12 },{ 0xca, 10 },{ 0x2fc, 12 },
            { 0x17f, 11 },{ 0x4cd, 11 },{ 0x2fd, 12 },{ 0x4fe, 11 },
            { 0x13a, 13 },{ 0xa, 4 },{ 0x42, 7 },{ 0x1d3, 9 },
            { 0x4dd, 11 },{ 0x12, 5 },{ 0xe8, 8 },{ 0x4c, 11 },
            { 0x136, 13 },{ 0x39, 6 },{ 0x264, 10 },{ 0xeba, 12 },
            { 0x0, 7 },{ 0xae, 10 },{ 0x99c, 12 },{ 0x1f, 7 },
            { 0x4de, 11 },{ 0x43, 7 },{ 0x4dc, 11 },{ 0x3, 8 },
            { 0x3cb, 12 },{ 0x6, 8 },{ 0x99e, 12 },{ 0x2a, 8 },
            { 0x5f1, 13 },{ 0xf, 8 },{ 0x9fe, 12 },{ 0x33, 8 },
            { 0x9ff, 12 },{ 0x98, 8 },{ 0x99f, 12 },{ 0xea, 8 },
            { 0x13c, 13 },{ 0x2e, 8 },{ 0x192, 11 },{ 0x136, 9 },
            { 0x6a, 9 },{ 0x15, 11 },{ 0x3af, 10 },{ 0x1e3, 11 },
            { 0x74, 11 },{ 0xeb, 12 },{ 0x2f9, 12 },{ 0x5c, 13 },
            { 0xed, 12 },{ 0x3dd, 12 },{ 0x326, 12 },{ 0x5e, 13 },
            { 0x16, 7 },
        };
        table_run = new int[] {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  2,  2,  2,  2,  2,
            2,  2,  2,  3,  3,  3,  3,  3,
            3,  3,  4,  4,  4,  4,  4,  5,
            5,  5,  5,  6,  6,  6,  6,  7,
            7,  7,  8,  8,  8,  9,  9,  9,
            10, 10, 10, 11, 11, 11, 12, 12,
            12, 13, 13, 13, 14, 14, 15, 15,
            16, 17, 18, 19, 20,  0,  0,  0,
            0,  1,  1,  1,  1,  2,  2,  2,
            3,  3,  3,  4,  4,  5,  5,  6,
            6,  7,  7,  8,  8,  9,  9, 10,
            10, 11, 11, 12, 12, 13, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22,
            23, 24, 25, 26,
        };
        table_level = new int[] {
            1,  2,  3,  4,  5,  6,  7,  8,
            9, 10, 11, 12, 13, 14, 15, 16,
            1,  2,  3,  4,  5,  6,  7,  8,
            9, 10, 11,  1,  2,  3,  4,  5,
            6,  7,  8,  1,  2,  3,  4,  5,
            6,  7,  1,  2,  3,  4,  5,  1,
            2,  3,  4,  1,  2,  3,  4,  1,
            2,  3,  1,  2,  3,  1,  2,  3,
            1,  2,  3,  1,  2,  3,  1,  2,
            3,  1,  2,  3,  1,  2,  1,  2,
            1,  1,  1,  1,  1,  1,  2,  3,
            4,  1,  2,  3,  4,  1,  2,  3,
            1,  2,  3,  1,  2,  1,  2,  1,
            2,  1,  2,  1,  2,  1,  2,  1,
            2,  1,  2,  1,  2,  1,  2,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,
        };
        calculateStats();
    }
    
}

