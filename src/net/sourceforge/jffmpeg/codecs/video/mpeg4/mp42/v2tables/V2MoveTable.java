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
public class V2MoveTable extends VLCTable {
    public V2MoveTable() {
        vlcCodes = new long[][] {
            {1,1}, {1,2}, {1,3}, {1,4}, {3,6}, {5,7}, {4,7}, {3,7},
            {11,9}, {10,9}, {9,9}, {17,10}, {16,10}, {15,10}, {14,10}, {13,10},
            {12,10}, {11,10}, {10,10}, {9,10}, {8,10}, {7,10}, {6,10}, {5,10},
            {4,10}, {7,11}, {6,11}, {5,11}, {4,11}, {3,11}, {2,11}, {3,12},
            {2,12}
        };
        createHighSpeedTable();
    }
}

