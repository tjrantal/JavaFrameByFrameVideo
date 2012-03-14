/*
 * Java port of ffmpeg VOB demultiplexer.
 * Contains some liba52 and Xine code (GPL)
 * Copyright (c) 2003 Jonathan Hueber.
 *
 * Copyright (c) 2000, 2001, 2002 Fabrice Bellard.
 *
 * vobdemux is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * vobdemux is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sourceforge.jffmpeg.demux.vob;

import java.io.IOException;
import java.io.InputStream;
import javax.media.protocol.PullSourceStream;

import net.sourceforge.jffmpeg.GPLLicense;

/**
 *
 */
public class PullSourceInputStream extends InputStream implements GPLLicense {
    private PullSourceStream wrapped;
    
    /** Creates a new instance of PullSourceStream */
    public PullSourceInputStream( PullSourceStream wrapped ) {
        this.wrapped = wrapped;
    }
    
    public int read( byte[] data, int offset, int length ) throws IOException {
        return wrapped.read( data, offset, length );
    }

    private byte[] singleByte = new byte[ 1 ];
    public int read() throws IOException {
        if ( read( singleByte, 0, 1 ) == -1 ) return -1;
        return singleByte[ 0 ] & 0xff;
    }
    
    private byte[] dump = new byte[ 2000 ];
    public final void skip( int i ) throws IOException {
        do {
            i -= wrapped.read( dump, 0, i % 2000 );
        } while ( i > 0 );
    }
}
