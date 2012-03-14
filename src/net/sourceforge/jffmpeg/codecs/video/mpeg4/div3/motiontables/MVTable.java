/*
 * Java port of parts of the ffmpeg Mpeg4 base decoder.
 * Copyright (c) 2003 Jonathan Hueber.
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
package net.sourceforge.jffmpeg.codecs.video.mpeg4.div3.motiontables;

import net.sourceforge.jffmpeg.codecs.video.mpeg4.Mpeg4Exception;

import net.sourceforge.jffmpeg.codecs.utils.VLCTable;

/**
 * This table 
 */
public abstract class MVTable extends VLCTable {
    protected int   escapeCode;
    protected int[] codes;
    protected int[] codesSize;

    protected int[] codesX;
    protected int[] codesY;

    public final int getEscapeCode() {
        return escapeCode;
    }

    protected void generateVLCCodes() {
        vlcCodes = new long[codes.length][2];
        for ( int i = 0; i < vlcCodes.length; i++ ) {
            vlcCodes[i][0] = codes[ i ];
            vlcCodes[i][1] = codesSize[ i ];
        }
        createHighSpeedTable();
    }
    
    public final int getCode( int i ) {
        return codes[ i ];
    }

    public final int getXCode( int i ) {
        return codesX[ i ];
    }

    public final int getYCode( int i ) {
        return codesY[ i ];
    }
}
