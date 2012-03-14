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
public class V2DiscreteCosineLuminance extends VLCTable {
    public V2DiscreteCosineLuminance() {
        vlcCodes = new long[ 512 ][ 2 ];

        long[][] h263LumCodes = new long[][] {
            {3,3}, {3,2}, {2,2}, {2,3}, {1,3}, {1,4}, {1,5}, {1,6}, {1,7},
            {1,8}, {1,9}, {1,10}, {1,11},
        };
        for ( int level = -256; level < 256; level++ ) {
            /* Get size in bits */
            int size = 0;
            int v = (level < 0) ? -level : level;
            while ( v != 0 ) {
                v >>= 1;
                size++;
            }

            int l = (level < 0) ? (-level)^((1 << size) - 1) : level;

            long uni_code = h263LumCodes[ size ][0];
            long uni_len  = h263LumCodes[ size ][1];

            /* Munge */
            uni_code ^= (1 << uni_len) - 1;
            if ( size > 0 ) {
                uni_code <<= size;
                uni_code |= l;
                uni_len += size;
                if ( size > 8 ) {
                    uni_code <<= 1;
                    uni_code |= 1;
                    uni_len++;
                }
            }
            vlcCodes[ level + 256 ][ 0 ] = uni_code;
            vlcCodes[ level + 256 ][ 1 ] = uni_len;
        }
        createHighSpeedTable();
    }
}
