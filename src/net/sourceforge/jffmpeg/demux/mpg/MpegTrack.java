/*
 * Java port of ffmpeg MPEG demultiplexer.
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
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.demux.mpg;

import javax.media.Demultiplexer;
import javax.media.protocol.Positionable;
import javax.media.protocol.Seekable;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.Track;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PullSourceStream;
import javax.media.MediaLocator;
import javax.media.BadHeaderException;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.TrackListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Enumeration;

import java.awt.Dimension;

/**
 * Mpeg Track implementation.  Reads an MPEG stream
 */
public class MpegTrack implements Track {
    /**
     * Demultiplexer
     */
    protected MpegDemux demux;

    /**
     * Track identification
     */
    protected int trackId;
    protected int streamId;
    protected Format format;

    /**
     * Position in demultiplexer
     */
    protected long pos;

    /**
     * Constructor - pass Track Identifier
     */
    public MpegTrack( MpegDemux demux, int trackId ) {
        this.demux = demux;
        this.trackId = trackId;
        pos = 0;

    }
    
    public Time getDuration() {
        return new Time(0);
    }

    /**
     * Return Audio format
     */
    public Format getFormat() {
        return format;
    }

    public void setEnabled( boolean enabled ) {
    }
    
    public boolean isEnabled() {
        return true;
    }
    
    public Time mapFrameToTime( int frame ) {
        return new Time( 0 );
    }
    
    public int mapTimeToFrame(javax.media.Time time) {
        return 0;
    }

    public Time getStartTime() {
        return new Time( 0 );
    }

    public void setTrackListener(TrackListener trackListener) {
    }    

    /**
     * Return a buffer containing audio data
     */
    public void readFrame(Buffer buffer) {
        try {
            /**
             * Initialise buffer
             */
            buffer.setLength(0);
            buffer.setOffset( 0 );
            buffer.setFlags( Buffer.FLAG_NO_WAIT );


            /**
             * Loop until we have the correct packet 
             */
            boolean gotPacket = false;
            while (!gotPacket) {
                /**
                 * Look at the next packet 
                 */
                int startcode = demux.peekPacket( pos );

                /* Is this a DTS packet? */
                if ( ((startcode >= 0x1c0 && startcode <= 0x1df) ||
                      (startcode >= 0x1e0 && startcode <= 0x1ef) ||
                      (startcode == 0x1bd))) {
                    /**
                     * Is this the right kind of DTS/PTS packet? 
                     */
                    if ( startcode != trackId ) {
                        pos = demux.skipDTSPacket( pos );
                        continue;
                    }
 
                    /** Read the DTS/PTS packet */
                    pos = demux.readDTSPacket( buffer, streamId, pos );
                    gotPacket = true;
                    break;
                }

                /**
                 * Skip packet
                 */
                pos = demux.skipPacket( pos );
            }
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
}
