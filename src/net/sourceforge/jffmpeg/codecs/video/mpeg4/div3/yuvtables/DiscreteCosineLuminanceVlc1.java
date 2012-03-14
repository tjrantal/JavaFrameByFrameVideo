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

/*
 * non intra picture macro block coded block pattern + mb type 
 */
public class DiscreteCosineLuminanceVlc1 extends VLCTable {

    public DiscreteCosineLuminanceVlc1() {
        vlcCodes = new long[][] {
            { 0x2, 2 },{ 0x3, 2 },{ 0x3, 3 },{ 0x2, 4 },
            { 0x5, 4 },{ 0x1, 5 },{ 0x3, 5 },{ 0x8, 5 },
            { 0x0, 6 },{ 0x5, 6 },{ 0xd, 6 },{ 0xf, 6 },
            { 0x13, 6 },{ 0x8, 7 },{ 0x18, 7 },{ 0x1c, 7 },
            { 0x24, 7 },{ 0x4, 8 },{ 0x6, 8 },{ 0x12, 8 },
            { 0x32, 8 },{ 0x3b, 8 },{ 0x4a, 8 },{ 0x4b, 8 },
            { 0xb, 9 },{ 0x26, 9 },{ 0x27, 9 },{ 0x66, 9 },
            { 0x74, 9 },{ 0x75, 9 },{ 0x14, 10 },{ 0x1c, 10 },
            { 0x1f, 10 },{ 0x1d, 10 },{ 0x2b, 11 },{ 0x3d, 11 },
            { 0x19d, 11 },{ 0x19f, 11 },{ 0x54, 12 },{ 0x339, 12 },
            { 0x338, 12 },{ 0x33d, 12 },{ 0xab, 13 },{ 0xf1, 13 },
            { 0x678, 13 },{ 0xf2, 13 },{ 0x1e0, 14 },{ 0x1e1, 14 },
            { 0x154, 14 },{ 0xcf2, 14 },{ 0x3cc, 15 },{ 0x2ab, 15 },
            { 0x19e7, 15 },{ 0x3ce, 15 },{ 0x19e6, 15 },{ 0x554, 16 },
            { 0x79f, 16 },{ 0x555, 16 },{ 0xf3d, 17 },{ 0xf37, 17 },
            { 0xf3c, 17 },{ 0xf35, 17 },{ 0x1e6d, 18 },{ 0x1e68, 18 },
            { 0x3cd8, 19 },{ 0x3cd3, 19 },{ 0x3cd9, 19 },{ 0x79a4, 20 },
            { 0xf34ba, 25 },{ 0xf34b4, 25 },{ 0xf34b5, 25 },{ 0xf34b6, 25 },
            { 0xf34b7, 25 },{ 0xf34b8, 25 },{ 0xf34b9, 25 },{ 0xf34bb, 25 },
            { 0xf34bc, 25 },{ 0xf34bd, 25 },{ 0xf34be, 25 },{ 0xf34bf, 25 },
            { 0x1e6940, 26 },{ 0x1e6941, 26 },{ 0x1e6942, 26 },{ 0x1e6943, 26 },
            { 0x1e6944, 26 },{ 0x1e6945, 26 },{ 0x1e6946, 26 },{ 0x1e6947, 26 },
            { 0x1e6948, 26 },{ 0x1e6949, 26 },{ 0x1e694a, 26 },{ 0x1e694b, 26 },
            { 0x1e694c, 26 },{ 0x1e694d, 26 },{ 0x1e694e, 26 },{ 0x1e694f, 26 },
            { 0x1e6950, 26 },{ 0x1e6951, 26 },{ 0x1e6952, 26 },{ 0x1e6953, 26 },
            { 0x1e6954, 26 },{ 0x1e6955, 26 },{ 0x1e6956, 26 },{ 0x1e6957, 26 },
            { 0x1e6958, 26 },{ 0x1e6959, 26 },{ 0x1e695a, 26 },{ 0x1e695b, 26 },
            { 0x1e695c, 26 },{ 0x1e695d, 26 },{ 0x1e695e, 26 },{ 0x1e695f, 26 },
            { 0x1e6960, 26 },{ 0x1e6961, 26 },{ 0x1e6962, 26 },{ 0x1e6963, 26 },
            { 0x1e6964, 26 },{ 0x1e6965, 26 },{ 0x1e6966, 26 },{ 0x1e6967, 26 },
        };
        createHighSpeedTable();
    }
}
