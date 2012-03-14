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
public class MotionVectorVlc extends VLCTable {
    public MotionVectorVlc() {
        vlcCodes = new long[][] {
            { 0x1, 1 },
            { 0x1, 2 },
            { 0x1, 3 },
            { 0x1, 4 },
            { 0x3, 6 },
            { 0x5, 7 },
            { 0x4, 7 },
            { 0x3, 7 },
            { 0xb, 9 },
            { 0xa, 9 },
            { 0x9, 9 },
            { 0x11, 10 },
            { 0x10, 10 },
            { 0xf, 10 },
            { 0xe, 10 },
            { 0xd, 10 },
            { 0xc, 10 }
        };
        createHighSpeedTable();
    }
}
