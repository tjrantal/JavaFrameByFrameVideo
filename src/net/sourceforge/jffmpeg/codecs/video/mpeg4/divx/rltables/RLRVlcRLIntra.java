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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.divx.rltables;

import net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.rltables.RLTable;

public class RLRVlcRLIntra extends RLTable {
    
    public RLRVlcRLIntra() {
        super();
        n = 169;
        last = 103;
        vlcCodes = new long[][]  {
            {0x0006,  3},{0x0007,  3},{0x000A,  4},{0x0009,  5},
            {0x0014,  6},{0x0015,  6},{0x0034,  7},{0x0074,  8},
            {0x0075,  8},{0x00DD,  9},{0x00EC,  9},{0x01EC, 10},
            {0x01ED, 10},{0x01F4, 10},{0x03EC, 11},{0x03ED, 11},
            {0x03F4, 11},{0x077D, 12},{0x07BC, 12},{0x0FBD, 13},
            {0x0FDC, 13},{0x07BD, 12},{0x0FDD, 13},{0x1FBD, 14},
            {0x1FDC, 14},{0x1FDD, 14},{0x1FFC, 15},{0x0001,  4},
            {0x0008,  5},{0x002D,  7},{0x006C,  8},{0x006D,  8},
            {0x00DC,  9},{0x01DD, 10},{0x03DC, 11},{0x03DD, 11},
            {0x077C, 12},{0x0FBC, 13},{0x1F7D, 14},{0x1FBC, 14},
            {0x0004,  5},{0x002C,  7},{0x00BC,  9},{0x01DC, 10},
            {0x03BC, 11},{0x03BD, 11},{0x0EFD, 13},{0x0F7C, 13},
            {0x0F7D, 13},{0x1EFD, 14},{0x1F7C, 14},{0x0005,  5},
            {0x005C,  8},{0x00BD,  9},{0x037D, 11},{0x06FC, 12},
            {0x0EFC, 13},{0x1DFD, 14},{0x1EFC, 14},{0x1FFD, 15},
            {0x000C,  6},{0x005D,  8},{0x01BD, 10},{0x03FD, 12},
            {0x06FD, 12},{0x1BFD, 14},{0x000D,  6},{0x007D,  9},
            {0x02FC, 11},{0x05FC, 12},{0x1BFC, 14},{0x1DFC, 14},
            {0x001C,  7},{0x017C, 10},{0x02FD, 11},{0x05FD, 12},
            {0x2FFC, 15},{0x001D,  7},{0x017D, 10},{0x037C, 11},
            {0x0DFD, 13},{0x2FFD, 15},{0x003C,  8},{0x01BC, 10},
            {0x0BFD, 13},{0x17FD, 14},{0x003D,  8},{0x01FD, 11},
            {0x0DFC, 13},{0x37FC, 15},{0x007C,  9},{0x03FC, 12},
            {0x00FC, 10},{0x0BFC, 13},{0x00FD, 10},{0x37FD, 15},
            {0x01FC, 11},{0x07FC, 13},{0x07FD, 13},{0x0FFC, 14},
            {0x0FFD, 14},{0x17FC, 14},{0x3BFC, 15},
            {0x000B,  4},{0x0078,  8},{0x03F5, 11},{0x0FEC, 13},
            {0x1FEC, 14},{0x0012,  5},{0x00ED,  9},{0x07DC, 12},
            {0x1FED, 14},{0x3BFD, 15},{0x0013,  5},{0x03F8, 11},
            {0x3DFC, 15},{0x0018,  6},{0x07DD, 12},{0x0019,  6},
            {0x07EC, 12},{0x0022,  6},{0x0FED, 13},{0x0023,  6},
            {0x0FF4, 13},{0x0035,  7},{0x0FF5, 13},{0x0038,  7},
            {0x0FF8, 13},{0x0039,  7},{0x0FF9, 13},{0x0042,  7},
            {0x1FF4, 14},{0x0043,  7},{0x1FF5, 14},{0x0079,  8},
            {0x1FF8, 14},{0x0082,  8},{0x3DFD, 15},{0x0083,  8},
            {0x00F4,  9},{0x00F5,  9},{0x00F8,  9},{0x00F9,  9},
            {0x0102,  9},{0x0103,  9},{0x01F5, 10},{0x01F8, 10},
            {0x01F9, 10},{0x0202, 10},{0x0203, 10},{0x03F9, 11},
            {0x0402, 11},{0x0403, 11},{0x07ED, 12},{0x07F4, 12},
            {0x07F5, 12},{0x07F8, 12},{0x07F9, 12},{0x0802, 12},
            {0x0803, 12},{0x1002, 13},{0x1003, 13},{0x1FF9, 14},
            {0x2002, 14},{0x2003, 14},{0x3EFC, 15},{0x3EFD, 15},
            {0x3F7C, 15},{0x3F7D, 15},{0x0000,  4}
        };
        table_run = new int[] {
             0,  0,  0,  0,  0,  0,  0,  0, 
             0,  0,  0,  0,  0,  0,  0,  0, 
             0,  0,  0,  0,  0,  0,  0,  0, 
             0,  0,  0,  1,  1,  1,  1,  1, 
             1,  1,  1,  1,  1,  1,  1,  1, 
             2,  2,  2,  2,  2,  2,  2,  2, 
             2,  2,  2,  3,  3,  3,  3,  3, 
             3,  3,  3,  3,  4,  4,  4,  4, 
             4,  4,  5,  5,  5,  5,  5,  5, 
             6,  6,  6,  6,  6,  7,  7,  7, 
             7,  7,  8,  8,  8,  8,  9,  9, 
             9,  9, 10, 10, 11, 11, 12, 12, 
            13, 14, 15, 16, 17, 18, 19, 
             0,  0,  0,  0,  0,  1,  1,  1, 
             1,  1,  2,  2,  2,  3,  3,  4, 
             4,  5,  5,  6,  6,  7,  7,  8, 
             8,  9,  9, 10, 10, 11, 11, 12, 
            12, 13, 13, 14, 15, 16, 17, 18, 
            19, 20, 21, 22, 23, 24, 25, 26, 
            27, 28, 29, 30, 31, 32, 33, 34, 
            35, 36, 37, 38, 39, 40, 41, 42, 
            43, 44, 
        };
        table_level = new int[] {
             1,  2,  3,  4,  5,  6,  7,  8, 
             9, 10, 11, 12, 13, 14, 15, 16, 
            17, 18, 19, 20, 21, 22, 23, 24, 
            25, 26, 27,  1,  2,  3,  4,  5, 
             6,  7,  8,  9, 10, 11, 12, 13, 
             1,  2,  3,  4,  5,  6,  7,  8, 
             9, 10, 11,  1,  2,  3,  4,  5, 
             6,  7,  8,  9,  1,  2,  3,  4, 
             5,  6,  1,  2,  3,  4,  5,  6, 
             1,  2,  3,  4,  5,  1,  2,  3, 
             4,  5,  1,  2,  3,  4,  1,  2, 
             3,  4,  1,  2,  1,  2,  1,  2, 
             1,  1,  1,  1,  1,  1,  1,  
             1,  2,  3,  4,  5,  1,  2,  3, 
             4,  5,  1,  2,  3,  1,  2,  1, 
             2,  1,  2,  1,  2,  1,  2,  1, 
             2,  1,  2,  1,  2,  1,  2,  1, 
             2,  1,  2,  1,  1,  1,  1,  1, 
             1,  1,  1,  1,  1,  1,  1,  1, 
             1,  1,  1,  1,  1,  1,  1,  1, 
             1,  1,  1,  1,  1,  1,  1,  1, 
             1,  1, 
        };
        calculateStats();
    }                
}
