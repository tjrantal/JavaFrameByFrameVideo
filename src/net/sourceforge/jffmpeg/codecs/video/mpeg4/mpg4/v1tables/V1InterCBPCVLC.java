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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.mpg4.v1tables;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * non intra picture macro block coded block pattern + mb type 
 */
public class V1InterCBPCVLC extends VLCTable {
    public V1InterCBPCVLC() {
        vlcCodes = new long[][] {
	    {1,1},{3,4},{2,4},{5,6},
	    {3,5},{4,8},{3,8},{3,7},
	    {3,3},{7,7},{6,7},{5,9},
	    {4,6},{4,9},{3,9},{2,9},
	    {2,3},{5,7},{4,7},{5,8},
	    {1,9},{0,0},{0,0},{0,0},
	    {2,11},{12,13},{14,13},{15,13}
        };
        createHighSpeedTable();
    }
}

