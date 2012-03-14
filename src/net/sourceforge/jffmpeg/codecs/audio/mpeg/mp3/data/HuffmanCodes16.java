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
public class HuffmanCodes16 extends HuffmanCodes {
    
    /** Creates a new instance of HuffmanCodes1 */
    public HuffmanCodes16() {
        codes = new long[] {
             0x0001, 0x0005, 0x000e, 0x002c, 0x004a, 0x003f, 0x006e, 0x005d,
             0x00ac, 0x0095, 0x008a, 0x00f2, 0x00e1, 0x00c3, 0x0178, 0x0011,
             0x0003, 0x0004, 0x000c, 0x0014, 0x0023, 0x003e, 0x0035, 0x002f,
             0x0053, 0x004b, 0x0044, 0x0077, 0x00c9, 0x006b, 0x00cf, 0x0009,
             0x000f, 0x000d, 0x0017, 0x0026, 0x0043, 0x003a, 0x0067, 0x005a,
             0x00a1, 0x0048, 0x007f, 0x0075, 0x006e, 0x00d1, 0x00ce, 0x0010,
             0x002d, 0x0015, 0x0027, 0x0045, 0x0040, 0x0072, 0x0063, 0x0057,
             0x009e, 0x008c, 0x00fc, 0x00d4, 0x00c7, 0x0183, 0x016d, 0x001a,
             0x004b, 0x0024, 0x0044, 0x0041, 0x0073, 0x0065, 0x00b3, 0x00a4,
             0x009b, 0x0108, 0x00f6, 0x00e2, 0x018b, 0x017e, 0x016a, 0x0009,
             0x0042, 0x001e, 0x003b, 0x0038, 0x0066, 0x00b9, 0x00ad, 0x0109,
             0x008e, 0x00fd, 0x00e8, 0x0190, 0x0184, 0x017a, 0x01bd, 0x0010,
             0x006f, 0x0036, 0x0034, 0x0064, 0x00b8, 0x00b2, 0x00a0, 0x0085,
             0x0101, 0x00f4, 0x00e4, 0x00d9, 0x0181, 0x016e, 0x02cb, 0x000a,
             0x0062, 0x0030, 0x005b, 0x0058, 0x00a5, 0x009d, 0x0094, 0x0105,
             0x00f8, 0x0197, 0x018d, 0x0174, 0x017c, 0x0379, 0x0374, 0x0008,
             0x0055, 0x0054, 0x0051, 0x009f, 0x009c, 0x008f, 0x0104, 0x00f9,
             0x01ab, 0x0191, 0x0188, 0x017f, 0x02d7, 0x02c9, 0x02c4, 0x0007,
             0x009a, 0x004c, 0x0049, 0x008d, 0x0083, 0x0100, 0x00f5, 0x01aa,
             0x0196, 0x018a, 0x0180, 0x02df, 0x0167, 0x02c6, 0x0160, 0x000b,
             0x008b, 0x0081, 0x0043, 0x007d, 0x00f7, 0x00e9, 0x00e5, 0x00db,
             0x0189, 0x02e7, 0x02e1, 0x02d0, 0x0375, 0x0372, 0x01b7, 0x0004,
             0x00f3, 0x0078, 0x0076, 0x0073, 0x00e3, 0x00df, 0x018c, 0x02ea,
             0x02e6, 0x02e0, 0x02d1, 0x02c8, 0x02c2, 0x00df, 0x01b4, 0x0006,
             0x00ca, 0x00e0, 0x00de, 0x00da, 0x00d8, 0x0185, 0x0182, 0x017d,
             0x016c, 0x0378, 0x01bb, 0x02c3, 0x01b8, 0x01b5, 0x06c0, 0x0004,
             0x02eb, 0x00d3, 0x00d2, 0x00d0, 0x0172, 0x017b, 0x02de, 0x02d3,
             0x02ca, 0x06c7, 0x0373, 0x036d, 0x036c, 0x0d83, 0x0361, 0x0002,
             0x0179, 0x0171, 0x0066, 0x00bb, 0x02d6, 0x02d2, 0x0166, 0x02c7,
             0x02c5, 0x0362, 0x06c6, 0x0367, 0x0d82, 0x0366, 0x01b2, 0x0000,
             0x000c, 0x000a, 0x0007, 0x000b, 0x000a, 0x0011, 0x000b, 0x0009,
             0x000d, 0x000c, 0x000a, 0x0007, 0x0005, 0x0003, 0x0001, 0x0003,
        };
        codesSize = new long[] {
              1,  4,  6,  8,  9,  9, 10, 10,
             11, 11, 11, 12, 12, 12, 13,  9,
              3,  4,  6,  7,  8,  9,  9,  9,
             10, 10, 10, 11, 12, 11, 12,  8,
              6,  6,  7,  8,  9,  9, 10, 10,
             11, 10, 11, 11, 11, 12, 12,  9,
              8,  7,  8,  9,  9, 10, 10, 10,
             11, 11, 12, 12, 12, 13, 13, 10,
              9,  8,  9,  9, 10, 10, 11, 11,
             11, 12, 12, 12, 13, 13, 13,  9,
              9,  8,  9,  9, 10, 11, 11, 12,
             11, 12, 12, 13, 13, 13, 14, 10,
             10,  9,  9, 10, 11, 11, 11, 11,
             12, 12, 12, 12, 13, 13, 14, 10,
             10,  9, 10, 10, 11, 11, 11, 12,
             12, 13, 13, 13, 13, 15, 15, 10,
             10, 10, 10, 11, 11, 11, 12, 12,
             13, 13, 13, 13, 14, 14, 14, 10,
             11, 10, 10, 11, 11, 12, 12, 13,
             13, 13, 13, 14, 13, 14, 13, 11,
             11, 11, 10, 11, 12, 12, 12, 12,
             13, 14, 14, 14, 15, 15, 14, 10,
             12, 11, 11, 11, 12, 12, 13, 14,
             14, 14, 14, 14, 14, 13, 14, 11,
             12, 12, 12, 12, 12, 13, 13, 13,
             13, 15, 14, 14, 14, 14, 16, 11,
             14, 12, 12, 12, 13, 13, 14, 14,
             14, 16, 15, 15, 15, 17, 15, 11,
             13, 13, 11, 12, 14, 14, 13, 14,
             14, 15, 16, 15, 17, 15, 14, 11,
              9,  8,  8,  9,  9, 10, 10, 10,
             11, 11, 11, 11, 11, 11, 11,  8,
        };
        xsize = 16;
        generateVLCCodes();
    }
}
