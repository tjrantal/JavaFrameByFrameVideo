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

/*
 * non intra picture macro block coded block pattern + mb type 
 */
public class DiscreteCosineChrominanceVlc1 extends VLCTable {

    public DiscreteCosineChrominanceVlc1() {
        vlcCodes = new long[][] {
            { 0x0, 2 },{ 0x1, 2 },{ 0x4, 3 },{ 0x7, 3 },
            { 0xb, 4 },{ 0xd, 4 },{ 0x15, 5 },{ 0x28, 6 },
            { 0x30, 6 },{ 0x32, 6 },{ 0x52, 7 },{ 0x62, 7 },
            { 0x66, 7 },{ 0xa6, 8 },{ 0xc6, 8 },{ 0xcf, 8 },
            { 0x14f, 9 },{ 0x18e, 9 },{ 0x19c, 9 },{ 0x29d, 10 },
            { 0x33a, 10 },{ 0x538, 11 },{ 0x63c, 11 },{ 0x63e, 11 },
            { 0x63f, 11 },{ 0x676, 11 },{ 0xa73, 12 },{ 0xc7a, 12 },
            { 0xcef, 12 },{ 0x14e5, 13 },{ 0x19dd, 13 },{ 0x29c8, 14 },
            { 0x29c9, 14 },{ 0x63dd, 15 },{ 0x33b8, 14 },{ 0x33b9, 14 },
            { 0xc7b6, 16 },{ 0x63d8, 15 },{ 0x63df, 15 },{ 0xc7b3, 16 },
            { 0xc7b4, 16 },{ 0xc7b5, 16 },{ 0x63de, 15 },{ 0xc7b7, 16 },
            { 0xc7b8, 16 },{ 0xc7b9, 16 },{ 0x18f65, 17 },{ 0x31ec8, 18 },
            { 0xc7b248, 24 },{ 0xc7b249, 24 },{ 0xc7b24a, 24 },{ 0xc7b24b, 24 },
            { 0xc7b24c, 24 },{ 0xc7b24d, 24 },{ 0xc7b24e, 24 },{ 0xc7b24f, 24 },
            { 0xc7b250, 24 },{ 0xc7b251, 24 },{ 0xc7b252, 24 },{ 0xc7b253, 24 },
            { 0xc7b254, 24 },{ 0xc7b255, 24 },{ 0xc7b256, 24 },{ 0xc7b257, 24 },
            { 0xc7b258, 24 },{ 0xc7b259, 24 },{ 0xc7b25a, 24 },{ 0xc7b25b, 24 },
            { 0xc7b25c, 24 },{ 0xc7b25d, 24 },{ 0xc7b25e, 24 },{ 0xc7b25f, 24 },
            { 0xc7b260, 24 },{ 0xc7b261, 24 },{ 0xc7b262, 24 },{ 0xc7b263, 24 },
            { 0xc7b264, 24 },{ 0xc7b265, 24 },{ 0xc7b266, 24 },{ 0xc7b267, 24 },
            { 0xc7b268, 24 },{ 0xc7b269, 24 },{ 0xc7b26a, 24 },{ 0xc7b26b, 24 },
            { 0xc7b26c, 24 },{ 0xc7b26d, 24 },{ 0xc7b26e, 24 },{ 0xc7b26f, 24 },
            { 0xc7b270, 24 },{ 0xc7b271, 24 },{ 0xc7b272, 24 },{ 0xc7b273, 24 },
            { 0xc7b274, 24 },{ 0xc7b275, 24 },{ 0xc7b276, 24 },{ 0xc7b277, 24 },
            { 0xc7b278, 24 },{ 0xc7b279, 24 },{ 0xc7b27a, 24 },{ 0xc7b27b, 24 },
            { 0xc7b27c, 24 },{ 0xc7b27d, 24 },{ 0xc7b27e, 24 },{ 0xc7b27f, 24 },
            { 0x18f6480, 25 },{ 0x18f6481, 25 },{ 0x18f6482, 25 },{ 0x18f6483, 25 },
            { 0x18f6484, 25 },{ 0x18f6485, 25 },{ 0x18f6486, 25 },{ 0x18f6487, 25 },
            { 0x18f6488, 25 },{ 0x18f6489, 25 },{ 0x18f648a, 25 },{ 0x18f648b, 25 },
            { 0x18f648c, 25 },{ 0x18f648d, 25 },{ 0x18f648e, 25 },{ 0x18f648f, 25 },
        };
        createHighSpeedTable();
    }
}
