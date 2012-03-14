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
package net.sourceforge.jffmpeg.codecs.video.mpeg12.scantable;

/**
 *
 */
public class AlternateHorizontalScan extends ScanTable {
    
    /** Creates a new instance of AlternateVerticalScan */
    public AlternateHorizontalScan() {
        scanTable = new int [] {
            0,  1,   2,  3,  8,  9, 16, 17, 
            10, 11,  4,  5,  6,  7, 15, 14,
            13, 12, 19, 18, 24, 25, 32, 33, 
            26, 27, 20, 21, 22, 23, 28, 29,
            30, 31, 34, 35, 40, 41, 48, 49, 
            42, 43, 36, 37, 38, 39, 44, 45,
            46, 47, 50, 51, 56, 57, 58, 59, 
            52, 53, 54, 55, 60, 61, 62, 63,
        };
        createScanTable();
    }
}
