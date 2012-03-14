/*
 * Java port of parts of the ffmpeg Mpeg4 base decoder.
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
 */
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.yuvtables;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * non intra picture macro block coded block pattern + mb type 
 */
public class DiscreteCosineLuminanceVlc0 extends VLCTable {
    public static final int DC_MAX = 119;

    public DiscreteCosineLuminanceVlc0() {
        vlcCodes = new long[][] {
            { 0x1, 1 },{ 0x1, 2 },{ 0x1, 4 },{ 0x1, 5 },
            { 0x5, 5 },{ 0x7, 5 },{ 0x8, 6 },{ 0xc, 6 },
            { 0x0, 7 },{ 0x2, 7 },{ 0x12, 7 },{ 0x1a, 7 },
            { 0x3, 8 },{ 0x7, 8 },{ 0x27, 8 },{ 0x37, 8 },
            { 0x5, 9 },{ 0x4c, 9 },{ 0x6c, 9 },{ 0x6d, 9 },
            { 0x8, 10 },{ 0x19, 10 },{ 0x9b, 10 },{ 0x1b, 10 },
            { 0x9a, 10 },{ 0x13, 11 },{ 0x34, 11 },{ 0x35, 11 },
            { 0x61, 12 },{ 0x48, 13 },{ 0xc4, 13 },{ 0x4a, 13 },
            { 0xc6, 13 },{ 0xc7, 13 },{ 0x92, 14 },{ 0x18b, 14 },
            { 0x93, 14 },{ 0x183, 14 },{ 0x182, 14 },{ 0x96, 14 },
            { 0x97, 14 },{ 0x180, 14 },{ 0x314, 15 },{ 0x315, 15 },
            { 0x605, 16 },{ 0x604, 16 },{ 0x606, 16 },{ 0xc0e, 17 },
            { 0x303cd, 23 },{ 0x303c9, 23 },{ 0x303c8, 23 },{ 0x303ca, 23 },
            { 0x303cb, 23 },{ 0x303cc, 23 },{ 0x303ce, 23 },{ 0x303cf, 23 },
            { 0x303d0, 23 },{ 0x303d1, 23 },{ 0x303d2, 23 },{ 0x303d3, 23 },
            { 0x303d4, 23 },{ 0x303d5, 23 },{ 0x303d6, 23 },{ 0x303d7, 23 },
            { 0x303d8, 23 },{ 0x303d9, 23 },{ 0x303da, 23 },{ 0x303db, 23 },
            { 0x303dc, 23 },{ 0x303dd, 23 },{ 0x303de, 23 },{ 0x303df, 23 },
            { 0x303e0, 23 },{ 0x303e1, 23 },{ 0x303e2, 23 },{ 0x303e3, 23 },
            { 0x303e4, 23 },{ 0x303e5, 23 },{ 0x303e6, 23 },{ 0x303e7, 23 },
            { 0x303e8, 23 },{ 0x303e9, 23 },{ 0x303ea, 23 },{ 0x303eb, 23 },
            { 0x303ec, 23 },{ 0x303ed, 23 },{ 0x303ee, 23 },{ 0x303ef, 23 },
            { 0x303f0, 23 },{ 0x303f1, 23 },{ 0x303f2, 23 },{ 0x303f3, 23 },
            { 0x303f4, 23 },{ 0x303f5, 23 },{ 0x303f6, 23 },{ 0x303f7, 23 },
            { 0x303f8, 23 },{ 0x303f9, 23 },{ 0x303fa, 23 },{ 0x303fb, 23 },
            { 0x303fc, 23 },{ 0x303fd, 23 },{ 0x303fe, 23 },{ 0x303ff, 23 },
            { 0x60780, 24 },{ 0x60781, 24 },{ 0x60782, 24 },{ 0x60783, 24 },
            { 0x60784, 24 },{ 0x60785, 24 },{ 0x60786, 24 },{ 0x60787, 24 },
            { 0x60788, 24 },{ 0x60789, 24 },{ 0x6078a, 24 },{ 0x6078b, 24 },
            { 0x6078c, 24 },{ 0x6078d, 24 },{ 0x6078e, 24 },{ 0x6078f, 24 },
        };
        createHighSpeedTable();
    }
}

