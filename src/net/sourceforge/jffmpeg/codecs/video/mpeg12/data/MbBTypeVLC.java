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
public class MbBTypeVLC extends VLCTable {
    public MbBTypeVLC() {
        vlcCodes = new long[][] {
            { 3, 5 }, // 0x01 MB_INTRA
            { 2, 3 }, // 0x04 MB_BACK
            { 3, 3 }, // 0x06 MB_BACK|MB_PAT
            { 2, 4 }, // 0x08 MB_FOR
            { 3, 4 }, // 0x0A MB_FOR|MB_PAT
            { 2, 2 }, // 0x0C MB_FOR|MB_BACK
            { 3, 2 }, // 0x0E MB_FOR|MB_BACK|MB_PAT
            { 1, 6 }, // 0x11 MB_QUANT|MB_INTRA
            { 2, 6 }, // 0x16 MB_QUANT|MB_BACK|MB_PAT
            { 3, 6 }, // 0x1A MB_QUANT|MB_FOR|MB_PAT
            { 2, 5 }, // 0x1E MB_QUANT|MB_FOR|MB_BACK|MB_PAT
        };
        createHighSpeedTable();
    }
}
