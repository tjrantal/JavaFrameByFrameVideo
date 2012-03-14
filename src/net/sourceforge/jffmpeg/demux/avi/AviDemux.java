/*
 * Java port of Xine AVI demultiplexer.
 * Contains Xine code (GPL)
 *
 * Copyright (c) 2003-2005 Jonathan Hueber.
 *
 * This AVI demultiplexer is only used for debugging.
 *
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.demux.avi;

import java.io.*;
import java.net.URL;
import java.util.HashMap;

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
import javax.media.Track;
import javax.media.TrackListener;
import javax.media.format.VideoFormat;
import javax.media.format.AudioFormat;
import javax.media.Format;
import java.awt.Dimension;

import java.util.Iterator;

import net.sourceforge.jffmpeg.GPLLicense;

/**
 * AVI file demultiplexer.  Effectively this simply maintains a HashMap
 * of data buffers representing the audio and video streams in the VOB file.
 *
 * Some timing information is tracked here
 */
public class AviDemux implements Demultiplexer, Positionable, GPLLicense {
    private final int MAX_AUDIO_STREAMS = 1;

    private int numberOfAudioChannels = 0;
    private AviTrack[] track = new AviTrack[ 1 + MAX_AUDIO_STREAMS ];
    private byte[] idx = null;

    /** Empty constructor */
    public AviDemux() {
    }
  
    /** Required methods to be a Demultiplexer */    
    public void close() {
    }    
    
    public Object getControl(String str) {
        return null;
    }
    
    public Object[] getControls() {
        return new Object[0];
    }

    public Time getDuration() {
        return new Time( 5000000 );
    }
    
    public Time getMediaTime() {
        return new Time( 5000000 );
    }
    
    public String getName() {
        return "AVI demux";
    }
    
    public ContentDescriptor[] getSupportedInputContentDescriptors() {
        return new ContentDescriptor[] {
            new FileTypeDescriptor( "video.avit" )
        };
    }
    
    public Track[] getTracks() throws IOException, BadHeaderException {
        return track;
    }
    
    public boolean isPositionable() {
        return true;
    }
    
    public boolean isRandomAccess() {
        return true; 
    }
    
    public void open() throws javax.media.ResourceUnavailableException {
    }
    
    public void reset() {
    }
    
    public Time setPosition(javax.media.Time time, int param) {
        return time;
    }
    
    private PullSourceStream dataSource;
    private Seekable seekSource;
    
    public void setSource( DataSource inputDataSource ) throws java.io.IOException, javax.media.IncompatibleSourceException {
        if ( inputDataSource instanceof PullDataSource ) {
            PullSourceStream pullSource = ((PullDataSource)inputDataSource).getStreams()[0];
            this.dataSource = pullSource;
            if ( pullSource instanceof Seekable ) {
                this.seekSource = (Seekable)pullSource;
            }
            return;
        }
        throw new javax.media.IncompatibleSourceException();
    }
    
    /** Parse headers */
    public void start() throws java.io.IOException {
        /**
         * Check this is an AVI file
         */
        String id = new String( readBuffer( 4 ), "ASCII" );
        readBuffer( 4 );
        String type = new String( readBuffer( 4 ), "ASCII" );

        if (   !"RIFF".equalsIgnoreCase( id ) 
            || !"AVI ".equalsIgnoreCase( type ) ) throw new IOException( "Not AVI file" );

        /**
         * Extract header data 
         */
        byte[] hdrl = null;

        while ( true ) {
            String command = new String( readBuffer( 4 ), "ASCII" );
            int length = (readBytes(4) + 1) &~1;

            if ( "LIST".equalsIgnoreCase( command ) ) {
                command = new String( readBuffer( 4 ), "ASCII" ); length -= 4;
                if ( "movi".equalsIgnoreCase( command ) ) {
                    break;
		}
                if ( "hdrl".equalsIgnoreCase( command ) ) {
                    hdrl = readBuffer( length );
		}
                if ( "idx1".equalsIgnoreCase( command ) ) {
                    idx = readBuffer( length );
		}
                if ( "iddx".equalsIgnoreCase( command ) ) {
                    idx = readBuffer( length );
		}

	    } else {
                readBuffer( length );
            }
        }

        /**
         * Parse hdrl
         */
        int streamNumber = 0;
        int lastTagID = 0;
        for ( int i = 0; i < hdrl.length; ) {
            String command = new String( hdrl, i, 4 );
            int size = str2ulong( hdrl, i + 4 );

            if ( "LIST".equalsIgnoreCase( command ) ) {
                i += 12;
                continue;
            }

            String command2 = new String( hdrl, i+8, 4 );
            if ( "strh".equalsIgnoreCase( command ) ) {
                lastTagID = 0;
                if ( "vids".equalsIgnoreCase( command2 ) ) {
                    String compressor = new String( hdrl, i+12, 4);
                    int scale = str2ulong( hdrl, i+28 );
                    int rate  = str2ulong( hdrl, i+32 );
                    track[0] = new Video( this, streamNumber++, compressor, scale, rate );
                    streamVideoTag = ((Video)track[0]).getVideoTag();
                    lastTagID = 1;
                }
                if ( "auds".equalsIgnoreCase( command2 ) ) {
                    int scale = str2ulong( hdrl, i+28);
                    int rate = str2ulong( hdrl, i+32);
                    int sampleSize = str2ulong( hdrl, i+52);
                    track[1+numberOfAudioChannels++] = new Audio( this, streamNumber++, scale, rate, sampleSize );
                    lastTagID = 2;
                }
            }

            if ( "strf".equalsIgnoreCase( command ) ) {
		if ( lastTagID == 1 ) {
                    /**
                     * Video information
                     */
                    byte[] information = new byte[ size - 4 ];
                    System.arraycopy( hdrl, i + 4, 
                                      information, 0, information.length );
                    track[0].setBih( information );
                }
		if ( lastTagID == 2 ) {
                    /**
                     * Audio information
                     */
                    byte[] information = new byte[ size - 4 ];
                    System.arraycopy( hdrl, i + 4, 
                                      information, 0, information.length );
                    track[ 1 + numberOfAudioChannels - 1].setBih( information );
                }
            }

            i += size + 8;
	}

        endOfHeader = seekSource.tell();
    }

    long endOfHeader;
    public synchronized long readFrame( Buffer buffer, boolean video, long position ) throws IOException {
        boolean isVideo;

        if ( position < endOfHeader ) position = endOfHeader;
        seekSource.seek( position );
        do {
            isVideo = getChunk( buffer );
            /**
             * Skip padding
             */
            if ( (buffer.getLength() & 1) == 1 ) readBuffer( 1 );
        } while ( isVideo != video );
        return seekSource.tell();
    }
        

    private String streamVideoTag;
    private boolean getChunk( Buffer output ) throws IOException {
        String command = new String( readBuffer( 4 ), "ASCII" ).toUpperCase();
        int size = readBytes(4);

	/**
         * Skip LIST and RIFF
         */ 
        while (   "LIST".equals( command )
               || "RIFF".equals( command ) ) { 
            readBuffer( 4 );
            command = new String( readBuffer( 4 ), "ASCII" ).toUpperCase();
            size = readBytes(4);
        }

        /**
         * Look for ##db ##dc ##wb [video]
         */
        String videoTag = streamVideoTag.substring(0, 3);
        if ( command.substring(0, 3).equalsIgnoreCase( videoTag ) &&
             (command.charAt(3) == 'B' || command.charAt(3) == 'C') ) {
	    /**
             * Video
             */
            output.setData( readBuffer(size) );
            output.setLength( size );
            return true; 
        }
        /**
         * Match Audio strings
         */
        for ( int i = 0; i < numberOfAudioChannels; i++ ) {
//            if ( command.equalsIgnoreCase( audio[i].getAudioTag() ) ) {
                /**
                 * Audio
                 */
                output.setData( readBuffer(size) );
                output.setLength( size );
                return false; 
//          }
        }
        throw new IOException( "Not header " + command );
    }
    
    /**
     * str2ulong
     */
    public static final int str2ulong( byte[] data, int i ) {
        return    (data[ i ] & 0xff) 
               | ((data[ i + 1 ] & 0xff) << 8 )
	       | ((data[ i + 2 ] & 0xff) << 16 )
	       | ((data[ i + 3 ] & 0xff) << 24 );
    }

    /**
     * Read a byte array 
     */
    private final byte[] readBuffer( int size ) throws IOException {
        byte[] buffer = new byte[ size ];

        int read = 0;
        while ( read < size ) {
            int next = dataSource.read( buffer, read, size - read );
            if ( next < 0 ) throw new IOException( "End of Stream" );
            read += next;
	}
        return buffer;
    }


    /**
     * Read unsigned byte
     */
    private final int readByte() throws IOException {
        byte[] data = new byte[ 1 ];
        dataSource.read( data, 0, 1 );
        return data[0];
    }


    /**
     * Read up to 4 bytes
     */
    private final int readBytes( int number ) throws IOException {
        byte[] buffer = new byte[ number ];
        int read = dataSource.read( buffer, 0, number );

        if ( read != buffer.length ) {
            if ( read < 0 ) throw new IOException( "End of Stream" );
            for ( int i = read; i < buffer.length; i++ ) buffer[ i ] = (byte)readByte();
        }
        
	/**
         * Create integer
         */
        switch ( number ) {
            case 1: return (buffer[ 0 ] & 0xff);
	    case 2: return (buffer[ 0 ] & 0xff) | ((buffer[ 1 ] & 0xff) << 8);
	    case 3: return (buffer[ 0 ] & 0xff) | ((buffer[ 1 ] & 0xff) << 8) | ((buffer[ 2 ] & 0xff) << 16);
	    case 4: return (buffer[ 0 ] & 0xff) | ((buffer[ 1 ] & 0xff) << 8) | ((buffer[ 2 ] & 0xff) << 16) | ((buffer[ 3 ] & 0xff) << 24);
	    default: throw new IOException( "Illegal Read quantity" );
	}
    }


    public void stop() {
    }  
}

abstract class AviTrack implements Track, GPLLicense {
    protected AviDemux demux;

    public AviTrack( AviDemux demux ) {
        this.demux = demux;
    }

    public Time getDuration() {
        return new Time( 1000 );
    }

    public Time getStartTime() {
        return new Time( 2000 );
    }
    
    public boolean isEnabled() {
        return true;
    }
    
    public Time mapFrameToTime(int param) {
        return new Time( param );
    }
    
    public int mapTimeToFrame(javax.media.Time time) {
        return 0;
    }

    private long pos;
    /**
     * Supply a frame of data to codec
     */
    public void readFrame(Buffer outputBuffer) {
//        System.out.println( "Read Frame" );
        try {
        pos = demux.readFrame( outputBuffer, isVideo(), pos );
        } catch (IOException e) {}
    }

    public void setEnabled(boolean enabled) {
    }
    
    public void setTrackListener(TrackListener trackListener) {
    }
    
    protected byte[] bih;

    /**
     * See buffer.h/xine_bmiheader
     */
    public void setBih( byte[] bih ) {
        this.bih = bih;
    }

    public abstract boolean isVideo();
}


class Video extends AviTrack implements GPLLicense {
    private int streamNumber;
    private String compressor;
    private int scale;
    private int rate;

    public Video( AviDemux demux, int streamNumber, String compressor, int scale, int rate ) {
        super( demux );
        this.streamNumber = streamNumber;
        this.compressor = compressor;
        this.scale = scale;
        this.rate = rate;
    }

    public boolean isVideo() {
	return true;
    }

    public String toString() {
        return "Stream: " + streamNumber + " Compressor " + compressor
             + "  Scale " + scale + " Rate " + rate;
    }

    public int getWidth() {
        return AviDemux.str2ulong( bih, 8 );
    }

    public int getHeight() {
        return AviDemux.str2ulong( bih, 12 );
    }

    public String getVideoTag() {
        return new String( new char[] { (char)((streamNumber / 10) + '0'),
				        (char)((streamNumber % 10) + '0'),
				        'd',
                                        'b' } );
    }        

    public Format getFormat() {
//	System.out.println( toString() );
        return new VideoFormat( compressor, 
                                new Dimension(getWidth(), getHeight()), 
                                10000, 
                                (new byte[0]).getClass(), 
                                ((float)rate)/100 );
    }

    /**
     * Also known as biClrUsed
     */
    public int getPaletteCount() {
        return AviDemux.str2ulong( bih, 32 );
    }
}


class Audio extends AviTrack implements GPLLicense {
    private int streamNumber;
    private int scale;
    private int rate;
    private int sampleSize;
    
    public Audio( AviDemux demux, int streamNumber, int scale, int rate, int sampleSize ) {
        super( demux );
        this.streamNumber = streamNumber;
        this.scale = scale;
        this.rate = rate;
        this.sampleSize = sampleSize;
    }

    public boolean isVideo() {
	return false;
    }

    public String getAudioTag() {
        return new String( new char[] { (char)((streamNumber / 10) + '0'),
				        (char)((streamNumber % 10) + '0'),
				        'w',
                                        'b' } );
    }

    public String toString() {
        return "Stream: " + streamNumber + " Audio Rate " + rate + " Scale " + scale;
    }

    public Format getFormat() {
//        System.out.println( toString() );
        return new AudioFormat( "mpeglayer3", 24000,
                                       16,
                                       2,
                                       0, 1); // endian, int signed
    }
}
