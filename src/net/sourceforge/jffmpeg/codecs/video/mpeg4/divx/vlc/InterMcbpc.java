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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.divx.vlc;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * Intra picture macro block coded block pattern 
 */
public class InterMcbpc extends VLCTable {

    public InterMcbpc() {
        vlcCodes = new long[][] {
            { 0x1, 1 },{ 0x3, 4 },{ 0x2, 4 },{ 0x5, 6 },
            { 0x3, 5 },{ 0x4, 8 },{ 0x3, 8 },{ 0x3, 7 },
            { 0x3, 3 },{ 0x7, 7 },{ 0x6, 7 },{ 0x5, 9 },
            { 0x4, 6 },{ 0x4, 9 },{ 0x3, 9 },{ 0x2, 9 },
            { 0x2, 3 },{ 0x5, 7 },{ 0x4, 7 },{ 0x5, 8 },
            { 0x1, 9 },{ 0x0, 0 },{ 0x0, 0 },{ 0x0, 0 },
            { 0x2, 11},{ 0xc, 13},{ 0xe, 13},{ 0xf, 13}
        };
        createHighSpeedTable();
    }
}
