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

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 *
 */
public class HuffmanCodes extends VLCTable {
    protected long[] codes;
    protected long[] codesSize;
    protected int xsize;
    protected int[] huff_code_table;
    
    protected void generateVLCCodes() {
        vlcCodes = new long[codes.length][2];
        for ( int i = 0; i < vlcCodes.length; i++ ) {
            vlcCodes[i][0] = codes[ i ];
            vlcCodes[i][1] = codesSize[ i ];
        }
        
        huff_code_table = new int[ xsize * xsize ];
        int j = 0;
        for ( int x = 0; x < xsize; x++ ) {
            for ( int y = 0; y < xsize; y++ ) {
                huff_code_table[ j++ ] = (x << 4)|y;
            }
        }
        createHighSpeedTable();
    }

    /** Creates a new instance of HuffmanCodes */
    public HuffmanCodes() {
    }
    
    public int[] getHuffCodeTable() {
        return huff_code_table;
    }
    
    public static final HuffmanCodes[] getHuffmanCodes() {
        return new HuffmanCodes[] { 
            new HuffmanCodesNull(),
            new HuffmanCodes1(),
            new HuffmanCodes2(),
            new HuffmanCodes3(),
            new HuffmanCodes5(),
            new HuffmanCodes6(),
            new HuffmanCodes7(),
            new HuffmanCodes8(),
            new HuffmanCodes9(),
            new HuffmanCodes10(),
            new HuffmanCodes11(),
            new HuffmanCodes12(),
            new HuffmanCodes13(),
            new HuffmanCodes15(),
            new HuffmanCodes16(),
            new HuffmanCodes24()
        };
    }
}
