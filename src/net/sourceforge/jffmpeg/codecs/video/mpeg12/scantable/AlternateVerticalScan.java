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
public class AlternateVerticalScan extends ScanTable {
    
    /** Creates a new instance of AlternateVerticalScan */
    public AlternateVerticalScan() {
        scanTable = new int [] {
            0,  8,  16, 24,  1,  9,  2, 10, 
            17, 25, 32, 40, 48, 56, 57, 49,
            41, 33, 26, 18,  3, 11,  4, 12, 
            19, 27, 34, 42, 50, 58, 35, 43,
            51, 59, 20, 28,  5, 13,  6, 14, 
            21, 29, 36, 44, 52, 60, 37, 45,
            53, 61, 22, 30,  7, 15, 23, 31, 
            38, 46, 54, 62, 39, 47, 55, 63,
        };
        createScanTable();
    }
}
