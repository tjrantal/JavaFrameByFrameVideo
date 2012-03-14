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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.mbtables;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/* non intra picture macro block coded block pattern + mb type */
public class NonIntraMacroBlock extends VLCTable {

    public NonIntraMacroBlock() {
        vlcCodes = new long[][] {
            { 0x40, 7 },{ 0x13c9, 13 },{ 0x9fd, 12 },{ 0x1fc, 15 },
            { 0x9fc, 12 },{ 0xa83, 18 },{ 0x12d34, 17 },{ 0x83bc, 16 },
            { 0x83a, 12 },{ 0x7f8, 17 },{ 0x3fd, 16 },{ 0x3ff, 16 },
            { 0x79, 13 },{ 0xa82, 18 },{ 0x969d, 16 },{ 0x2a4, 16 },
            { 0x978, 12 },{ 0x543, 17 },{ 0x41df, 15 },{ 0x7f9, 17 },
            { 0x12f3, 13 },{ 0x25a6b, 18 },{ 0x25ef9, 18 },{ 0x3fa, 16 },
            { 0x20ee, 14 },{ 0x969ab, 20 },{ 0x969c, 16 },{ 0x25ef8, 18 },
            { 0x12d2, 13 },{ 0xa85, 18 },{ 0x969e, 16 },{ 0x4bc8, 15 },
            { 0x3d, 12 },{ 0x12f7f, 17 },{ 0x2a2, 16 },{ 0x969f, 16 },
            { 0x25ee, 14 },{ 0x12d355, 21 },{ 0x12f7d, 17 },{ 0x12f7e, 17 },
            { 0x9e5, 12 },{ 0xa81, 18 },{ 0x4b4d4, 19 },{ 0x83bd, 16 },
            { 0x78, 13 },{ 0x969b, 16 },{ 0x3fe, 16 },{ 0x2a5, 16 },
            { 0x7e, 13 },{ 0xa80, 18 },{ 0x2a3, 16 },{ 0x3fb, 16 },
            { 0x1076, 13 },{ 0xa84, 18 },{ 0x153, 15 },{ 0x4bc9, 15 },
            { 0x55, 13 },{ 0x12d354, 21 },{ 0x4bde, 15 },{ 0x25e5, 14 },
            { 0x25b, 10 },{ 0x4b4c, 15 },{ 0x96b, 12 },{ 0x96a, 12 },
            { 0x1, 2 },{ 0x0, 7 },{ 0x26, 6 },{ 0x12b, 9 },
            { 0x7, 3 },{ 0x20f, 10 },{ 0x4, 9 },{ 0x28, 12 },
            { 0x6, 3 },{ 0x20a, 10 },{ 0x128, 9 },{ 0x2b, 12 },
            { 0x11, 5 },{ 0x1b, 11 },{ 0x13a, 9 },{ 0x4ff, 11 },
            { 0x3, 4 },{ 0x277, 10 },{ 0x106, 9 },{ 0x839, 12 },
            { 0xb, 4 },{ 0x27b, 10 },{ 0x12c, 9 },{ 0x4bf, 11 },
            { 0x9, 6 },{ 0x35, 12 },{ 0x27e, 10 },{ 0x13c8, 13 },
            { 0x1, 6 },{ 0x4aa, 11 },{ 0x208, 10 },{ 0x29, 12 },
            { 0x1, 4 },{ 0x254, 10 },{ 0x12e, 9 },{ 0x838, 12 },
            { 0x24, 6 },{ 0x4f3, 11 },{ 0x276, 10 },{ 0x12f6, 13 },
            { 0x1, 5 },{ 0x27a, 10 },{ 0x13e, 9 },{ 0x3e, 12 },
            { 0x8, 6 },{ 0x413, 11 },{ 0xc, 10 },{ 0x4be, 11 },
            { 0x14, 5 },{ 0x412, 11 },{ 0x253, 10 },{ 0x97a, 12 },
            { 0x21, 6 },{ 0x4ab, 11 },{ 0x20b, 10 },{ 0x34, 12 },
            { 0x15, 5 },{ 0x278, 10 },{ 0x252, 10 },{ 0x968, 12 },
            { 0x5, 5 },{ 0xb, 10 },{ 0x9c, 8 },{ 0xe, 10 },
        };
        createHighSpeedTable();
    }
}
