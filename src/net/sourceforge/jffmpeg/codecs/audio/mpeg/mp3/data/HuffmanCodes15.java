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
public class HuffmanCodes15 extends HuffmanCodes {
    
    /** Creates a new instance of HuffmanCodes1 */
    public HuffmanCodes15() {
        codes = new long[] {
             0x0007, 0x000c, 0x0012, 0x0035, 0x002f, 0x004c, 0x007c, 0x006c,
             0x0059, 0x007b, 0x006c, 0x0077, 0x006b, 0x0051, 0x007a, 0x003f,
             0x000d, 0x0005, 0x0010, 0x001b, 0x002e, 0x0024, 0x003d, 0x0033,
             0x002a, 0x0046, 0x0034, 0x0053, 0x0041, 0x0029, 0x003b, 0x0024,
             0x0013, 0x0011, 0x000f, 0x0018, 0x0029, 0x0022, 0x003b, 0x0030,
             0x0028, 0x0040, 0x0032, 0x004e, 0x003e, 0x0050, 0x0038, 0x0021,
             0x001d, 0x001c, 0x0019, 0x002b, 0x0027, 0x003f, 0x0037, 0x005d,
             0x004c, 0x003b, 0x005d, 0x0048, 0x0036, 0x004b, 0x0032, 0x001d,
             0x0034, 0x0016, 0x002a, 0x0028, 0x0043, 0x0039, 0x005f, 0x004f,
             0x0048, 0x0039, 0x0059, 0x0045, 0x0031, 0x0042, 0x002e, 0x001b,
             0x004d, 0x0025, 0x0023, 0x0042, 0x003a, 0x0034, 0x005b, 0x004a,
             0x003e, 0x0030, 0x004f, 0x003f, 0x005a, 0x003e, 0x0028, 0x0026,
             0x007d, 0x0020, 0x003c, 0x0038, 0x0032, 0x005c, 0x004e, 0x0041,
             0x0037, 0x0057, 0x0047, 0x0033, 0x0049, 0x0033, 0x0046, 0x001e,
             0x006d, 0x0035, 0x0031, 0x005e, 0x0058, 0x004b, 0x0042, 0x007a,
             0x005b, 0x0049, 0x0038, 0x002a, 0x0040, 0x002c, 0x0015, 0x0019,
             0x005a, 0x002b, 0x0029, 0x004d, 0x0049, 0x003f, 0x0038, 0x005c,
             0x004d, 0x0042, 0x002f, 0x0043, 0x0030, 0x0035, 0x0024, 0x0014,
             0x0047, 0x0022, 0x0043, 0x003c, 0x003a, 0x0031, 0x0058, 0x004c,
             0x0043, 0x006a, 0x0047, 0x0036, 0x0026, 0x0027, 0x0017, 0x000f,
             0x006d, 0x0035, 0x0033, 0x002f, 0x005a, 0x0052, 0x003a, 0x0039,
             0x0030, 0x0048, 0x0039, 0x0029, 0x0017, 0x001b, 0x003e, 0x0009,
             0x0056, 0x002a, 0x0028, 0x0025, 0x0046, 0x0040, 0x0034, 0x002b,
             0x0046, 0x0037, 0x002a, 0x0019, 0x001d, 0x0012, 0x000b, 0x000b,
             0x0076, 0x0044, 0x001e, 0x0037, 0x0032, 0x002e, 0x004a, 0x0041,
             0x0031, 0x0027, 0x0018, 0x0010, 0x0016, 0x000d, 0x000e, 0x0007,
             0x005b, 0x002c, 0x0027, 0x0026, 0x0022, 0x003f, 0x0034, 0x002d,
             0x001f, 0x0034, 0x001c, 0x0013, 0x000e, 0x0008, 0x0009, 0x0003,
             0x007b, 0x003c, 0x003a, 0x0035, 0x002f, 0x002b, 0x0020, 0x0016,
             0x0025, 0x0018, 0x0011, 0x000c, 0x000f, 0x000a, 0x0002, 0x0001,
             0x0047, 0x0025, 0x0022, 0x001e, 0x001c, 0x0014, 0x0011, 0x001a,
             0x0015, 0x0010, 0x000a, 0x0006, 0x0008, 0x0006, 0x0002, 0x0000,
        };
        codesSize = new long[] {
              3,  4,  5,  7,  7,  8,  9,  9,
              9, 10, 10, 11, 11, 11, 12, 13,
              4,  3,  5,  6,  7,  7,  8,  8,
              8,  9,  9, 10, 10, 10, 11, 11,
              5,  5,  5,  6,  7,  7,  8,  8,
              8,  9,  9, 10, 10, 11, 11, 11,
              6,  6,  6,  7,  7,  8,  8,  9,
              9,  9, 10, 10, 10, 11, 11, 11,
              7,  6,  7,  7,  8,  8,  9,  9,
              9,  9, 10, 10, 10, 11, 11, 11,
              8,  7,  7,  8,  8,  8,  9,  9,
              9,  9, 10, 10, 11, 11, 11, 12,
              9,  7,  8,  8,  8,  9,  9,  9,
              9, 10, 10, 10, 11, 11, 12, 12,
              9,  8,  8,  9,  9,  9,  9, 10,
             10, 10, 10, 10, 11, 11, 11, 12,
              9,  8,  8,  9,  9,  9,  9, 10,
             10, 10, 10, 11, 11, 12, 12, 12,
              9,  8,  9,  9,  9,  9, 10, 10,
             10, 11, 11, 11, 11, 12, 12, 12,
             10,  9,  9,  9, 10, 10, 10, 10,
             10, 11, 11, 11, 11, 12, 13, 12,
             10,  9,  9,  9, 10, 10, 10, 10,
             11, 11, 11, 11, 12, 12, 12, 13,
             11, 10,  9, 10, 10, 10, 11, 11,
             11, 11, 11, 11, 12, 12, 13, 13,
             11, 10, 10, 10, 10, 11, 11, 11,
             11, 12, 12, 12, 12, 12, 13, 13,
             12, 11, 11, 11, 11, 11, 11, 11,
             12, 12, 12, 12, 13, 13, 12, 13,
             12, 11, 11, 11, 11, 11, 11, 12,
             12, 12, 12, 12, 13, 13, 13, 13,
        };
        xsize = 16;
        generateVLCCodes();
    }
}
