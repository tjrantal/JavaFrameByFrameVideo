/*
 * Java port of ffmpeg mpeg1/2 decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2000,2001 Fabrice Bellard.
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
package net.sourceforge.jffmpeg.codecs.video.mpeg12.data;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * non intra picture macro block coded block pattern + mb type 
 */
public class PatVLC extends VLCTable {
    public PatVLC() {
        vlcCodes = new long[][] {
            {0xb, 5},
            {0x9, 5},
            {0xd, 6},
            {0xd, 4},
            {0x17, 7},
            {0x13, 7},
            {0x1f, 8},
            {0xc, 4},
            {0x16, 7},
            {0x12, 7},
            {0x1e, 8},
            {0x13, 5},
            {0x1b, 8},
            {0x17, 8},
            {0x13, 8},
            {0xb, 4},
            {0x15, 7},
            {0x11, 7},
            {0x1d, 8},
            {0x11, 5},
            {0x19, 8},
            {0x15, 8},
            {0x11, 8},
            {0xf, 6},
            {0xf, 8},
            {0xd, 8},
            {0x3, 9},
            {0xf, 5},
            {0xb, 8},
            {0x7, 8},
            {0x7, 9},
            {0xa, 4},
            {0x14, 7},
            {0x10, 7},
            {0x1c, 8},
            {0xe, 6},
            {0xe, 8},
            {0xc, 8},
            {0x2, 9},
            {0x10, 5},
            {0x18, 8},
            {0x14, 8},
            {0x10, 8},
            {0xe, 5},
            {0xa, 8},
            {0x6, 8},
            {0x6, 9},
            {0x12, 5},
            {0x1a, 8},
            {0x16, 8},
            {0x12, 8},
            {0xd, 5},
            {0x9, 8},
            {0x5, 8},
            {0x5, 9},
            {0xc, 5},
            {0x8, 8},
            {0x4, 8},
            {0x4, 9},
            {0x7, 3},
            {0xa, 5},
            {0x8, 5},
            {0xc, 6}
        };
        createHighSpeedTable();
    }
}
