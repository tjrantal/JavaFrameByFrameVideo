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
public class AddressIncrementVlc extends VLCTable {
    public AddressIncrementVlc() {
        vlcCodes = new long[][] {
            {0x1, 1},
            {0x3, 3},
            {0x2, 3},
            {0x3, 4},
            {0x2, 4},
            {0x3, 5},
            {0x2, 5},
            {0x7, 7},
            {0x6, 7},
            {0xb, 8},
            {0xa, 8},
            {0x9, 8},
            {0x8, 8},
            {0x7, 8},
            {0x6, 8},
            {0x17, 10},
            {0x16, 10},
            {0x15, 10},
            {0x14, 10},
            {0x13, 10},
            {0x12, 10},
            {0x23, 11},
            {0x22, 11},
            {0x21, 11},
            {0x20, 11},
            {0x1f, 11},
            {0x1e, 11},
            {0x1d, 11},
            {0x1c, 11},
            {0x1b, 11},
            {0x1a, 11},
            {0x19, 11},
            {0x18, 11},
            {0x8, 11}, /* escape */
            {0xf, 11}, /* stuffing */
            {0x0, 8}, /* end (and 15 more 0 bits should follow) */
        };
        createHighSpeedTable();
    }
}
