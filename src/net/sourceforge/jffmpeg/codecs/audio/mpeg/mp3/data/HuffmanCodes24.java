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
public class HuffmanCodes24 extends HuffmanCodes {
    
    /** Creates a new instance of HuffmanCodes1 */
    public HuffmanCodes24() {
        codes = new long[] {
            0x000f, 0x000d, 0x002e, 0x0050, 0x0092, 0x0106, 0x00f8, 0x01b2,
             0x01aa, 0x029d, 0x028d, 0x0289, 0x026d, 0x0205, 0x0408, 0x0058,
             0x000e, 0x000c, 0x0015, 0x0026, 0x0047, 0x0082, 0x007a, 0x00d8,
             0x00d1, 0x00c6, 0x0147, 0x0159, 0x013f, 0x0129, 0x0117, 0x002a,
             0x002f, 0x0016, 0x0029, 0x004a, 0x0044, 0x0080, 0x0078, 0x00dd,
             0x00cf, 0x00c2, 0x00b6, 0x0154, 0x013b, 0x0127, 0x021d, 0x0012,
             0x0051, 0x0027, 0x004b, 0x0046, 0x0086, 0x007d, 0x0074, 0x00dc,
             0x00cc, 0x00be, 0x00b2, 0x0145, 0x0137, 0x0125, 0x010f, 0x0010,
             0x0093, 0x0048, 0x0045, 0x0087, 0x007f, 0x0076, 0x0070, 0x00d2,
             0x00c8, 0x00bc, 0x0160, 0x0143, 0x0132, 0x011d, 0x021c, 0x000e,
             0x0107, 0x0042, 0x0081, 0x007e, 0x0077, 0x0072, 0x00d6, 0x00ca,
             0x00c0, 0x00b4, 0x0155, 0x013d, 0x012d, 0x0119, 0x0106, 0x000c,
             0x00f9, 0x007b, 0x0079, 0x0075, 0x0071, 0x00d7, 0x00ce, 0x00c3,
             0x00b9, 0x015b, 0x014a, 0x0134, 0x0123, 0x0110, 0x0208, 0x000a,
             0x01b3, 0x0073, 0x006f, 0x006d, 0x00d3, 0x00cb, 0x00c4, 0x00bb,
             0x0161, 0x014c, 0x0139, 0x012a, 0x011b, 0x0213, 0x017d, 0x0011,
             0x01ab, 0x00d4, 0x00d0, 0x00cd, 0x00c9, 0x00c1, 0x00ba, 0x00b1,
             0x00a9, 0x0140, 0x012f, 0x011e, 0x010c, 0x0202, 0x0179, 0x0010,
             0x014f, 0x00c7, 0x00c5, 0x00bf, 0x00bd, 0x00b5, 0x00ae, 0x014d,
             0x0141, 0x0131, 0x0121, 0x0113, 0x0209, 0x017b, 0x0173, 0x000b,
             0x029c, 0x00b8, 0x00b7, 0x00b3, 0x00af, 0x0158, 0x014b, 0x013a,
             0x0130, 0x0122, 0x0115, 0x0212, 0x017f, 0x0175, 0x016e, 0x000a,
             0x028c, 0x015a, 0x00ab, 0x00a8, 0x00a4, 0x013e, 0x0135, 0x012b,
             0x011f, 0x0114, 0x0107, 0x0201, 0x0177, 0x0170, 0x016a, 0x0006,
             0x0288, 0x0142, 0x013c, 0x0138, 0x0133, 0x012e, 0x0124, 0x011c,
             0x010d, 0x0105, 0x0200, 0x0178, 0x0172, 0x016c, 0x0167, 0x0004,
             0x026c, 0x012c, 0x0128, 0x0126, 0x0120, 0x011a, 0x0111, 0x010a,
             0x0203, 0x017c, 0x0176, 0x0171, 0x016d, 0x0169, 0x0165, 0x0002,
             0x0409, 0x0118, 0x0116, 0x0112, 0x010b, 0x0108, 0x0103, 0x017e,
             0x017a, 0x0174, 0x016f, 0x016b, 0x0168, 0x0166, 0x0164, 0x0000,
             0x002b, 0x0014, 0x0013, 0x0011, 0x000f, 0x000d, 0x000b, 0x0009,
             0x0007, 0x0006, 0x0004, 0x0007, 0x0005, 0x0003, 0x0001, 0x0003,
        };
        codesSize = new long[] {
            4,  4,  6,  7,  8,  9,  9, 10,
             10, 11, 11, 11, 11, 11, 12,  9,
              4,  4,  5,  6,  7,  8,  8,  9,
              9,  9, 10, 10, 10, 10, 10,  8,
              6,  5,  6,  7,  7,  8,  8,  9,
              9,  9,  9, 10, 10, 10, 11,  7,
              7,  6,  7,  7,  8,  8,  8,  9,
              9,  9,  9, 10, 10, 10, 10,  7,
              8,  7,  7,  8,  8,  8,  8,  9,
              9,  9, 10, 10, 10, 10, 11,  7,
              9,  7,  8,  8,  8,  8,  9,  9,
              9,  9, 10, 10, 10, 10, 10,  7,
              9,  8,  8,  8,  8,  9,  9,  9,
              9, 10, 10, 10, 10, 10, 11,  7,
             10,  8,  8,  8,  9,  9,  9,  9,
             10, 10, 10, 10, 10, 11, 11,  8,
             10,  9,  9,  9,  9,  9,  9,  9,
              9, 10, 10, 10, 10, 11, 11,  8,
             10,  9,  9,  9,  9,  9,  9, 10,
             10, 10, 10, 10, 11, 11, 11,  8,
             11,  9,  9,  9,  9, 10, 10, 10,
             10, 10, 10, 11, 11, 11, 11,  8,
             11, 10,  9,  9,  9, 10, 10, 10,
             10, 10, 10, 11, 11, 11, 11,  8,
             11, 10, 10, 10, 10, 10, 10, 10,
             10, 10, 11, 11, 11, 11, 11,  8,
             11, 10, 10, 10, 10, 10, 10, 10,
             11, 11, 11, 11, 11, 11, 11,  8,
             12, 10, 10, 10, 10, 10, 10, 11,
             11, 11, 11, 11, 11, 11, 11,  8,
              8,  7,  7,  7,  7,  7,  7,  7,
              7,  7,  7,  8,  8,  8,  8,  4,
        };
        xsize = 16;
        generateVLCCodes();
    }
}
