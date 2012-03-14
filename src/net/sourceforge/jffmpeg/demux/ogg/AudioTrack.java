/*
 * Java port of ogg demultiplexer.
 * Copyright (c) 2004 Jonathan Hueber.
 *
 * License conditions are the same as OggVorbis.  See README.
 * 1a39e335700bec46ae31a38e2156a898
 */
/********************************************************************
 *                                                                  *
 * THIS FILE IS PART OF THE OggVorbis SOFTWARE CODEC SOURCE CODE.   *
 * USE, DISTRIBUTION AND REPRODUCTION OF THIS LIBRARY SOURCE IS     *
 * GOVERNED BY A BSD-STYLE SOURCE LICENSE INCLUDED WITH THIS SOURCE *
 * IN 'COPYING'. PLEASE READ THESE TERMS BEFORE DISTRIBUTING.       *
 *                                                                  *
 * THE OggVorbis SOURCE CODE IS (C) COPYRIGHT 1994-2002             *
 * by the XIPHOPHORUS Company http://www.xiph.org/                  *
 *                                                                  *
 ********************************************************************/

package net.sourceforge.jffmpeg.demux.ogg;

import javax.media.Track;
import javax.media.Time;
import javax.media.Format;
import javax.media.Buffer;
import javax.media.TrackListener;
import javax.media.format.AudioFormat;

import java.io.IOException;
import java.io.InputStream;
import javax.media.Time;

/**
 * This class handles audio data read from a VOB file
 */
public class AudioTrack implements Track {
    private long sampleRate     = 44100;
    private long sampleDuration = Time.ONE_SECOND / 44100;

    /** Data source */
    private OggDemux demux;
    private int serial;
    
    /** Creates a new instance of AudioTrack */
    public AudioTrack( OggDemux demux, int serial ) {
        this.demux = demux;
        this.serial = serial;
    }
    
    public Time getDuration() {
        return demux.getDuration();
    }

    /**
     * Return Audio format
     */
    public Format getFormat() {
        /* We need to parse the three OGG headers for the Rate */
        if (headersRequired > 0 ) {
            while (headersRequired > 0 ) {
                readFrame( new Buffer() );
            }
            demux.seekPacket( 0, serial );
        }
        return new AudioFormat("vorbis", sampleRate,16,2,0,1);
    }

    public void setEnabled( boolean enabled ) {
    }
    
    public boolean isEnabled() {
        return true;
    }
    
    public Time mapFrameToTime( int frame ) {
        return new Time( frame * sampleDuration );
    }
    
    public int mapTimeToFrame(javax.media.Time time) {
        return (int)(time.getNanoseconds() / sampleDuration);
    }

    /**
     * Return a buffer containing audio data
     */
    public void readFrame(Buffer buffer) {
        try {
            demux.readFrame( buffer, serial, sampleDuration );
            if ( headersRequired > 0 ) process( buffer );
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

    public Time getStartTime() {
        return new Time( 0 );
    }

    public void setTrackListener(TrackListener trackListener) {
    }    


    /* Header parsing (copy from VorbisDecoder) 
     *  I have decided against passing the headers to the
     *  codec as an object to simplify compatability with
     *  other vorbis implementations.
     *
     * Unfortuantly this means a bit of logic duplication.
     */

    private int headersRequired = 3;

    private static final int HEADER_INFO    = 1;
    private static final int HEADER_COMMENT = 3;
    private static final int HEADER_BOOKS   = 5;

    private byte[] packetBuffer = new byte[ 0 ];
    private int packetBufferLength = 0;
    private void process( Buffer input ) {
        try {
            byte[] data = (byte[])input.getData();  //in.getLength
            int dataLength = input.getLength();

            /* Parse segments */
            int numberOfSegments = data[ 26 ] & 0xff;
            int segmentNumber = 0;
            int dataPointer = 27 + numberOfSegments;

            while ( segmentNumber < numberOfSegments ) {
                int length = 0;
                do {
                    length += data[ 27 + segmentNumber ] & 0xff;
                    segmentNumber++;
                } while(   data[ 27 + (segmentNumber - 1) ] == -1
                        && segmentNumber < numberOfSegments );

                /* Copy data to spare buffer */
                if ( packetBuffer.length < length + packetBufferLength ) {
                    byte[] t = packetBuffer;
                    packetBuffer = new byte[ length + packetBufferLength ];
                    System.arraycopy(t, 0, packetBuffer,0, packetBufferLength);
                }
                System.arraycopy( data, dataPointer, 
                                  packetBuffer, packetBufferLength, length );
                packetBufferLength += length;

                /* Is this packet going to wrap to next packet ? */
                if ( data[ 27 + (segmentNumber - 1) ] == -1 ) {
                    continue;
                }
                decodeHeader( packetBuffer, 0, packetBufferLength );
                dataPointer += length;
                packetBufferLength = 0;
            }
        } catch ( Exception e ) {
        } catch( Error e ) {
        }
    }


    private void decodeHeader( byte[] data, int offset, int length ) {
        int packType = data[ offset ];
        if (    data[ offset + 1 ] != 'v'
             || data[ offset + 2 ] != 'o'
             || data[ offset + 3 ] != 'r'
             || data[ offset + 4 ] != 'b'
             || data[ offset + 5 ] != 'i'
             || data[ offset + 6 ] != 's' ) {
            return;
        }
        offset += 7;
        length -= 7;
        switch (packType) {
            case HEADER_INFO: {
                vorbis_unpack_info( data, offset, length );
                break;
            }
            case HEADER_COMMENT: {
//                vorbis_unpack_comment( data, offset, length );
                break;
            }
            case HEADER_BOOKS: {
//                vorbis_unpack_books( data, offset, length);
                break;
            }
            default: {
                throw new Error( "Invalid header ID: " + packType );
            }
        }
        headersRequired--;
    }

    private void vorbis_unpack_info( byte[] data, int offset, int length ) {
        int version         = readInt( data, offset );
        int channels            = data[ offset + 4 ] & 0xff;
        int rate            = readInt( data, offset + 5 );
        int bitrate_upper   = readInt( data, offset + 9 );
        int bitrate_nominal = readInt( data, offset + 13 );
        int bitrate_lower   = readInt( data, offset + 17 );

        sampleRate = rate;
        sampleDuration = Time.ONE_SECOND / rate;
//	System.out.println( "Extracted Rate: " + rate );
    }

    /* Read an OGG int */
    private int readInt( byte[] buffer, int offset ) {
        int ret = 0;
        ret = (ret << 8 ) | (buffer[ offset + 3 ] & 0xff);
        ret = (ret << 8 ) | (buffer[ offset + 2 ] & 0xff);
        ret = (ret << 8 ) | (buffer[ offset + 1 ] & 0xff);
        ret = (ret << 8 ) | (buffer[ offset + 0 ] & 0xff);
        return ret;
    }
}
