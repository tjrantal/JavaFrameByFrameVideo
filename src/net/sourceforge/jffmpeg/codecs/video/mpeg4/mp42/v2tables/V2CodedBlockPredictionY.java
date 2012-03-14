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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.mp42.v2tables;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * non intra picture macro block coded block pattern + mb type 
 */
public class V2CodedBlockPredictionY extends VLCTable {
    public V2CodedBlockPredictionY() {
        vlcCodes = new long[][] {
            {3,4}, {5,5}, {4,5}, {9,4}, {3,5}, {7,4}, {2,6}, {11,4},
            {2,5}, {3,6}, {5,4}, {10,4}, {4,4}, {8,4}, {6,4}, {3,2}
        };
        createHighSpeedTable();
    }
}

