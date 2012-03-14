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
public class MpegVideoTrack extends MpegTrack {
    /**
     * Constructor - pass Track Identifier
     */
    public MpegVideoTrack( MpegDemux demux, int trackId ) {
        super( demux, trackId );
    }

    /**
     * Return Audio format
     */
    public Format getFormat() {
        if ( !parsedHeader ) {
            try {
                parseHeader();
            } catch ( IOException e ) {
            }
        }
        return format;
    }

    /**
     * Header information
     */
    private boolean parsedHeader = false;
    private Buffer header = new Buffer();

    /**
     * Mpeg Frame rate conversion
     */
    private float[] frameRateTable = new float[] {
        0, (float)24000/1001, 24, 25,
        (float)30000/1001, 30, 50, (float)60000/1001,
        60, 15, 5, 10, 12, 15, 0
    };

    /**
     * Read Header information
     */
    protected synchronized void parseHeader() throws IOException {
        long firstMpeg2Packet = 0;
        int width = 0;
        int height = 0;
        float frameRate = 0.0f;
 
        /**
         * Parse Header packets
         */
        while ( !parsedHeader ) {
            /**
             * Read Packet Identifier
             */
            int packetID = demux.peekPacket( pos );

            /**
             * Check this is a header packet 
             */
            if ( packetID < 0x1b0 ) {
                parsedHeader = true;
                break;
            }

            /**
             * Embedded Stream -- MPEG2
             */
            if ( ((packetID >= 0x1c0 && packetID <= 0x1df) ||
                  (packetID >= 0x1e0 && packetID <= 0x1ef) ||
                  (packetID == 0x1bd))) {
                /**
                 * Enter video stream
                 */
                firstMpeg2Packet = pos;
                pos = demux.readDTSHeader( header, pos );
                continue;
            }

            /**
             * Read Header Packet 
             */
            int pointer = header.getLength() + 4;

            /**
             * Sequence Start code
             */
            if ( packetID == MpegDemux.SEQUENCE_START_CODE ) {
                /**
                 * Get Width/Hight/Frame rate 
                 */
                pos = demux.readPacket( header, trackId, pos );

                byte[] packet = (byte[])header.getData();
                width = ((packet[ pointer ] & 0xff) << 4)
                      | ((packet[ pointer + 1 ] >>4) &0xf);
                height = ((packet[ pointer + 1 ] & 0xf) << 8)
                      | ((packet[ pointer + 2 ]) & 0xff);
                int frame_rate_index = packet[ pointer + 3 ] & 0xf;
                frameRate = frameRateTable[ frame_rate_index ];

                format = new VideoFormat( "mpeg", new Dimension(width,height), 
                                          10000, (new byte[1]).getClass(), 
                                          frameRate );
            } else if ( packetID == MpegDemux.EXT_START_CODE ) {
                /**
                 * MPEG 2 Extension 
                 */
                pos = demux.readPacket( header, trackId, pos );

                byte[] packet = (byte[])header.getData();
                int extensionCode = (packet[pointer]>>4) & 0xf;

                /* Sequence Extension */
                if ( extensionCode == 1 ) {
                    width  |= ((packet[pointer+1]&0x1)<<12)
                             |((packet[pointer+2]&0x80)<<3);
                    height |= ((packet[pointer+2]&0xa0)<<5);

                format = new VideoFormat( "mpeg", new Dimension(width,height), 
                                          10000, (new byte[1]).getClass(), 
                                          frameRate );
                }
            } else {
                /* Not a recognised header packet */
                int p = header.getLength();
                pos = demux.readPacket( header, trackId, pos );
                header.setLength(p);
            }
        }

        /**
         * Exit Mpeg 2 packet
         */
        if ( firstMpeg2Packet != 0 ) {
            pos = firstMpeg2Packet;
        }
    }

    /**
     * Return a buffer containing audio data
     */
    public void readFrame(Buffer buffer) {
//        if ( trackId == MpegDemux.AUDIO ) return;
        try {
            /** First things first -- Read headers */
            if (!parsedHeader) parseHeader();

            /** Allocate buffer */
            byte[] buf = (byte[])buffer.getData();
            if ( buf == null || buf.length < 100000 ) {
                buf = new byte[ 1000000 ];
                buffer.setData( buf );
            }

            buffer.setLength(0);
            buffer.setOffset( 0 );


            /** Loop until we have a "trackId" packet */
            boolean gotPacket = false;
            while (!gotPacket) {
                /* Look at the next packet */
                int startcode = demux.peekPacket( pos );
//		System.out.println( trackId + " " + Long.toHexString(pos) + ": " +Integer.toHexString( startcode ) );

                /* Is this a DTS/PTS type packet (Mpeg2)? */
                if ( ((startcode >= 0x1c0 && startcode <= 0x1df) ||
                      (startcode >= 0x1e0 && startcode <= 0x1ef) ||
                      (startcode == 0x1bd))) {

                    /** Is this the right kind of DTS/PTS packet? */
                    if ( startcode != trackId )
                    {
                        pos = demux.skipDTSPacket( pos );
                        continue;
                    }
 
                    pos = demux.readDTSHeader( buffer, pos );
                    continue;
                }
                    

                /* This is an MPEG1 type packet */
                if ( startcode == MpegDemux.SEQUENCE_END_CODE ) {
                    /* End of file */
                    buffer.setEOM( true );
                }

                if (   startcode == MpegDemux.SEQUENCE_END_CODE
                    || startcode == MpegDemux.GOP_START_CODE ) {
                    /* End of Group Of Pictures */
                    gotPacket = true;
                    pos = demux.skipPacket( pos );
                    break;
                }

                if ( startcode == MpegDemux.PACK_START_CODE ) {
                    /* Audio packet */
                    if ( trackId == startcode ) {
                        pos = demux.readPacket( buffer, trackId, pos + 4 );
                        gotPacket = true;
                    } else {
                        pos = demux.skipPacket( pos );
                    }
                } else {
                    /* Video Packet */
                    /* MPEGI video tracks all start with a header */
                    if ( buffer.getLength() == 0 ) {
                        byte[] buff = (byte[])buffer.getData();
                        buffer.setLength( header.getLength() );
                        buffer.setOffset( 0 );
                        System.arraycopy( header.getData(), 0, buff, 0, header.getLength() );
                    }
                    pos = demux.readPacket( buffer, trackId, pos );
                }
            }
//	    System.out.println( "Got packet" );
            /* Set buffer information */
            buffer.setFlags( Buffer.FLAG_NO_WAIT );
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
}
