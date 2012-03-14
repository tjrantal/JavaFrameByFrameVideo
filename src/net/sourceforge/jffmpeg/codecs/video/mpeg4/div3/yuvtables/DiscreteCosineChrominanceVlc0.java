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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.yuvtables;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * non intra picture macro block coded block pattern + mb type 
 */
public class DiscreteCosineChrominanceVlc0 extends VLCTable {

    public DiscreteCosineChrominanceVlc0() {
        vlcCodes = new long[][] {
            { 0x0, 2 },{ 0x1, 2 },{ 0x5, 3 },{ 0x9, 4 },
            { 0xd, 4 },{ 0x11, 5 },{ 0x1d, 5 },{ 0x1f, 5 },
            { 0x21, 6 },{ 0x31, 6 },{ 0x38, 6 },{ 0x33, 6 },
            { 0x39, 6 },{ 0x3d, 6 },{ 0x61, 7 },{ 0x79, 7 },
            { 0x80, 8 },{ 0xc8, 8 },{ 0xca, 8 },{ 0xf0, 8 },
            { 0x81, 8 },{ 0xc0, 8 },{ 0xc9, 8 },{ 0x107, 9 },
            { 0x106, 9 },{ 0x196, 9 },{ 0x183, 9 },{ 0x1e3, 9 },
            { 0x1e2, 9 },{ 0x20a, 10 },{ 0x20b, 10 },{ 0x609, 11 },
            { 0x412, 11 },{ 0x413, 11 },{ 0x60b, 11 },{ 0x411, 11 },
            { 0x60a, 11 },{ 0x65f, 11 },{ 0x410, 11 },{ 0x65d, 11 },
            { 0x65e, 11 },{ 0xcb8, 12 },{ 0xc10, 12 },{ 0xcb9, 12 },
            { 0x1823, 13 },{ 0x3045, 14 },{ 0x6089, 15 },{ 0xc110, 16 },
            { 0x304448, 22 },{ 0x304449, 22 },{ 0x30444a, 22 },{ 0x30444b, 22 },
            { 0x30444c, 22 },{ 0x30444d, 22 },{ 0x30444e, 22 },{ 0x30444f, 22 },
            { 0x304450, 22 },{ 0x304451, 22 },{ 0x304452, 22 },{ 0x304453, 22 },
            { 0x304454, 22 },{ 0x304455, 22 },{ 0x304456, 22 },{ 0x304457, 22 },
            { 0x304458, 22 },{ 0x304459, 22 },{ 0x30445a, 22 },{ 0x30445b, 22 },
            { 0x30445c, 22 },{ 0x30445d, 22 },{ 0x30445e, 22 },{ 0x30445f, 22 },
            { 0x304460, 22 },{ 0x304461, 22 },{ 0x304462, 22 },{ 0x304463, 22 },
            { 0x304464, 22 },{ 0x304465, 22 },{ 0x304466, 22 },{ 0x304467, 22 },
            { 0x304468, 22 },{ 0x304469, 22 },{ 0x30446a, 22 },{ 0x30446b, 22 },
            { 0x30446c, 22 },{ 0x30446d, 22 },{ 0x30446e, 22 },{ 0x30446f, 22 },
            { 0x304470, 22 },{ 0x304471, 22 },{ 0x304472, 22 },{ 0x304473, 22 },
            { 0x304474, 22 },{ 0x304475, 22 },{ 0x304476, 22 },{ 0x304477, 22 },
            { 0x304478, 22 },{ 0x304479, 22 },{ 0x30447a, 22 },{ 0x30447b, 22 },
            { 0x30447c, 22 },{ 0x30447d, 22 },{ 0x30447e, 22 },{ 0x30447f, 22 },
            { 0x608880, 23 },{ 0x608881, 23 },{ 0x608882, 23 },{ 0x608883, 23 },
            { 0x608884, 23 },{ 0x608885, 23 },{ 0x608886, 23 },{ 0x608887, 23 },
            { 0x608888, 23 },{ 0x608889, 23 },{ 0x60888a, 23 },{ 0x60888b, 23 },
            { 0x60888c, 23 },{ 0x60888d, 23 },{ 0x60888e, 23 },{ 0x60888f, 23 },
        };
        createHighSpeedTable();
    }
}
