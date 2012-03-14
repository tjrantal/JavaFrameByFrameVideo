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
public class DiscreteCosineChrominanceVlc extends VLCTable {
    public DiscreteCosineChrominanceVlc() {
        vlcCodes = new long[][] {
            {0x000, 2},
            {0x001, 2},
            {0x002, 2},
            {0x006, 3},
            {0x00e, 4},
            {0x01e, 5},
            {0x03e, 6},
            {0x07e, 7},            
            {0x0fe, 8},
            {0x1fe, 9},
            {0x3fe, 10},
            {0x3ff, 10},
        };
        createHighSpeedTable();
    }
}
