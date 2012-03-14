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

import net.sourceforge.jffmpeg.GPLLicense;

class DataBuffer implements GPLLicense {
    protected byte[] buffer = new byte[ 10000 ];
    protected int size = 0;
    
    public final byte[] getBuffer() {
        return buffer;
    }
    
    public final int getCurrentSize() {
        return size;
    }
    
    public void readData( long timeStamp, InputStream in, int length ) throws IOException {
        if ( buffer.length < length + size ) {
            byte[] temp = new byte[ (length + size) * 2 ];
            System.arraycopy( buffer, 0, temp, 0, size );
            buffer = temp;
        }
        
        int p = size;
        int read = 0;
        while ( length > 0 ) {
            read = in.read( buffer, size, length );
            if ( read < 0 ) throw new IOException( "End of Stream" );
            length -= read;
            size += read;
        }        
    }
   
    public void resetBuffer( byte[] buffer ) {
        this.buffer = buffer;
        size = 0;
    }
/*    
    public int getFrameEnd() {
        return frameEnd;
    }
    
    public void drop() {
        size = 0;
        frameEnd = 0;
    }
*/
    public void setBuffer( byte[] buffer, int existingSize ) {
        this.buffer = buffer;
        size = existingSize;
    }
    
    public void bufferData() throws IOException {
    }
    
    /**
     * Discard all data (pending seek)
     */
    public void drop() {
        size = 0;
    }
}
