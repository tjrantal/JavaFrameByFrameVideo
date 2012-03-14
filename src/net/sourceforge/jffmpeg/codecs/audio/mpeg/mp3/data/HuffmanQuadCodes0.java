/*
 * Java port of ffmpeg mp3 decoder.
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2000, 2001 Fabrice Bellard.
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
package net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3.data;

/**
 *
 */
public class HuffmanQuadCodes0 extends HuffmanCodes {
    
    /** Creates a new instance of HuffmanCodes1 */
    public HuffmanQuadCodes0() {
        codes = new long[] {
            1,  5,  4,  5,  6,  5,  4,  4, 7,  3,  6,  0,  7,  2,  3,  1
        };
        codesSize = new long[] {
            1, 4, 4, 5, 4, 6, 5, 6, 4, 5, 5, 6, 5, 6, 6, 6
        };
        xsize = 2;
        generateVLCCodes();
    }
}
