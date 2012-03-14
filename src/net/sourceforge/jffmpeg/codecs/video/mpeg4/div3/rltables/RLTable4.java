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

public class RLTable4 extends RLTable {
    
    public RLTable4() {
        n = 168;
        last = 99;
        vlcCodes = new long[][] {
            { 0x0, 3 },{ 0x3, 4 },{ 0xb, 5 },{ 0x14, 6 },
            { 0x3f, 6 },{ 0x5d, 7 },{ 0xa2, 8 },{ 0xac, 9 },
            { 0x16e, 9 },{ 0x20a, 10 },{ 0x2e2, 10 },{ 0x432, 11 },
            { 0x5c9, 11 },{ 0x827, 12 },{ 0xb54, 12 },{ 0x4e6, 13 },
            
            { 0x105f, 13 },{ 0x172a, 13 },{ 0x20b2, 14 },{ 0x2d4e, 14 },
            { 0x39f0, 14 },{ 0x4175, 15 },{ 0x5a9e, 15 },{ 0x4, 4 },
            { 0x1e, 5 },{ 0x42, 7 },{ 0xb6, 8 },{ 0x173, 9 },
            { 0x395, 10 },{ 0x72e, 11 },{ 0xb94, 12 },{ 0x16a4, 13 },
            
            { 0x20b3, 14 },{ 0x2e45, 14 },{ 0x5, 5 },{ 0x40, 7 },
            { 0x49, 9 },{ 0x28f, 10 },{ 0x5cb, 11 },{ 0x48a, 13 },
            { 0x9dd, 14 },{ 0x73e2, 15 },{ 0x18, 5 },{ 0x25, 8 },
            { 0x8a, 10 },{ 0x51b, 11 },{ 0xe5f, 12 },{ 0x9c9, 14 },
            
            { 0x139c, 15 },{ 0x29, 6 },{ 0x4f, 9 },{ 0x412, 11 },
            { 0x48d, 13 },{ 0x2e41, 14 },{ 0x38, 6 },{ 0x10e, 9 },
            { 0x5a8, 11 },{ 0x105c, 13 },{ 0x39f2, 14 },{ 0x58, 7 },
            { 0x21f, 10 },{ 0xe7e, 12 },{ 0x39ff, 14 },{ 0x23, 8 },
            
            
            { 0x2e3, 10 },{ 0x4e5, 13 },{ 0x2e40, 14 },{ 0xa1, 8 },
            { 0x5be, 11 },{ 0x9c8, 14 },{ 0x83, 8 },{ 0x13a, 11 },
            { 0x1721, 13 },{ 0x44, 9 },{ 0x276, 12 },{ 0x39f6, 14 },
            { 0x8b, 10 },{ 0x4ef, 13 },{ 0x5a9b, 15 },{ 0x208, 10 },
            
            { 0x1cfe, 13 },{ 0x399, 10 },{ 0x1cb4, 13 },{ 0x39e, 10 },
            { 0x39f3, 14 },{ 0x5ab, 11 },{ 0x73e3, 15 },{ 0x737, 11 },
            { 0x5a9f, 15 },{ 0x82d, 12 },{ 0xe69, 12 },{ 0xe68, 12 },
            { 0x433, 11 },{ 0xb7b, 12 },{ 0x2df8, 14 },{ 0x2e56, 14 },
            
            { 0x2e57, 14 },{ 0x39f7, 14 },{ 0x51a5, 15 },{ 0x3, 3 },
            { 0x2a, 6 },{ 0xe4, 8 },{ 0x28e, 10 },{ 0x735, 11 },
            { 0x1058, 13 },{ 0x1cfa, 13 },{ 0x2df9, 14 },{ 0x4174, 15 },
            { 0x9, 4 },{ 0x54, 8 },{ 0x398, 10 },{ 0x48b, 13 },
            
            { 0x139d, 15 },{ 0xd, 4 },{ 0xad, 9 },{ 0x826, 12 },
            { 0x2d4c, 14 },{ 0x11, 5 },{ 0x16b, 9 },{ 0xb7f, 12 },
            { 0x51a4, 15 },{ 0x19, 5 },{ 0x21b, 10 },{ 0x16fd, 13 },
            { 0x1d, 5 },{ 0x394, 10 },{ 0x28d3, 14 },{ 0x2b, 6 },
            
            
            { 0x5bc, 11 },{ 0x5a9a, 15 },{ 0x2f, 6 },{ 0x247, 12 },
            { 0x10, 7 },{ 0xa35, 12 },{ 0x3e, 6 },{ 0xb7a, 12 },
            { 0x59, 7 },{ 0x105e, 13 },{ 0x26, 8 },{ 0x9cf, 14 },
            { 0x55, 8 },{ 0x1cb5, 13 },{ 0x57, 8 },{ 0xe5b, 12 },
            
            { 0xa0, 8 },{ 0x1468, 13 },{ 0x170, 9 },{ 0x90, 10 },
            { 0x1ce, 9 },{ 0x21a, 10 },{ 0x218, 10 },{ 0x168, 9 },
            { 0x21e, 10 },{ 0x244, 12 },{ 0x736, 11 },{ 0x138, 11 },
            { 0x519, 11 },{ 0xe5e, 12 },{ 0x72c, 11 },{ 0xb55, 12 },
            
            { 0x9dc, 14 },{ 0x20bb, 14 },{ 0x48c, 13 },{ 0x1723, 13 },
            { 0x2e44, 14 },{ 0x16a5, 13 },{ 0x518, 11 },{ 0x39fe, 14 },
            { 0x169, 9 },
        };
        table_run = new int[] {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            
            1,  1,  2,  2,  2,  2,  2,  2,
            2,  2,  3,  3,  3,  3,  3,  3,
            3,  4,  4,  4,  4,  4,  5,  5,
            5,  5,  5,  6,  6,  6,  6,  7,
            
            7,  7,  7,  8,  8,  8,  9,  9,
            9, 10, 10, 10, 11, 11, 11, 12,
            12, 13, 13, 14, 14, 15, 15, 16,
            16, 17, 18, 19, 20, 21, 22, 23,
            
            24, 25, 26,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  1,  1,  1,  1,
            1,  2,  2,  2,  2,  3,  3,  3,
            3,  4,  4,  4,  5,  5,  5,  6,
            
            6,  6,  7,  7,  8,  8,  9,  9,
            10, 10, 11, 11, 12, 12, 13, 13,
            14, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28,
            
            29, 30, 31, 32, 33, 34, 35, 36,
        };
        table_level = new int[] {
            1,  2,  3,  4,  5,  6,  7,  8,
            9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23,  1,
            2,  3,  4,  5,  6,  7,  8,  9,
            
            10, 11,  1,  2,  3,  4,  5,  6,
            7,  8,  1,  2,  3,  4,  5,  6,
            7,  1,  2,  3,  4,  5,  1,  2,
            3,  4,  5,  1,  2,  3,  4,  1,
            
            2,  3,  4,  1,  2,  3,  1,  2,
            3,  1,  2,  3,  1,  2,  3,  1,
            2,  1,  2,  1,  2,  1,  2,  1,
            2,  1,  1,  1,  1,  1,  1,  1,
            
            1,  1,  1,  1,  2,  3,  4,  5,
            6,  7,  8,  9,  1,  2,  3,  4,
            5,  1,  2,  3,  4,  1,  2,  3,
            4,  1,  2,  3,  1,  2,  3,  1,
            
            2,  3,  1,  2,  1,  2,  1,  2,
            1,  2,  1,  2,  1,  2,  1,  2,
            1,  2,  1,  1,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,
            
            1,  1,  1,  1,  1,  1,  1,  1,
        };
        calculateStats();
    }
    
}

