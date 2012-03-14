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
 * 1a39e335700bec46ae31a38e2156a898
 */
package net.sourceforge.jffmpeg.demux.vob;

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

import java.util.Iterator;

import net.sourceforge.jffmpeg.GPLLicense;

/**
 * VOB file demultiplexer.  Effectively this simply maintains a HashMap
 * of data buffers representing the audio and video streams in the VOB file.
 *
 * Some timing information is tracked here
 */
public class VobDemux implements Demultiplexer, Positionable, GPLLicense {
    /**
     * Last time stamp read from Vob file    
     */
    private long timeStamp = 0;
    
    /**
     * Constants for maintaining Lip-sync
     */
    public static final int SYNC_CHANNEL = 0x80;
    public static final long MAX_AUDIO_BUFFER = 4200;
    public static final long MIN_AUDIO_BUFFER = 100;
    public static final boolean debugLipSync = false;
    public static final boolean showAllFrames = false;
    
    /**
     * File size for fast-forward and rewind
     */
    private long totalFileSize = 0;
    private long rateEstimateBase = 0;
    private long rateEstimateTime = 0;
    
    /**
     * Input and output streams
     */
    private InputStream in;
    private DataBuffer[] streams = new DataBuffer[ 512 ];
    
    /** Vob file constants */
    public static final int PACK_START_CODE          = 0x000001ba;
    public static final int SYSTEM_HEADER_START_CODE = 0x000001bb;
    public static final int SEQUENCE_END_CODE        = 0x000001b7;
    public static final int PACKET_START_CODE_MASK   = 0xffffff00;
    public static final int PACKET_START_CODE_PREFIX = 0x00000100;
    public static final int ISO_11172_END_CODE       = 0x000001b9;
  
    /* mpeg2  constants */
    public static final int PROGRAM_STREAM_MAP       = 0x000001bc;
    public static final int PRIVATE_STREAM_1         = 0x000001bd;
    public static final int PADDING_STREAM           = 0x000001be;
    public static final int PRIVATE_STREAM_2         = 0x000001bf;

    public static final int CODEC_TYPE_VIDEO         = 0;
    public static final int CODEC_TYPE_AUDIO         = 1;

    public static final int CODEC_ID_MPEG1VIDEO      = 0;
    public static final int CODEC_ID_MP2             = 1;
    public static final int CODEC_ID_AC3             = 2;
    public static final int CODEC_ID_PCM_S16BE       = 3;

    /** Empty constructor */
    public VobDemux() {
    }
  
    /** Creates a new instance of VobDemux based on an InputStream */
    public VobDemux( InputStream in ) {
        this.in = in;
    }
    
    /**
     * Read until we find a start code 0x000001xx
     */
    private int findStartCode() throws IOException {
        int state = 0xff;
        do {
            state = ((state << 8) | in.read()) & 0xffffffff;
        } while ( (state & 0xffffff00) != 0x100 );
        return state;
    }
    
    /**
     * Read a Presentation Time Stamp
     */
    private long getPts( int c ) throws IOException {
        if (c < 0) c = in.read();
        long pts = (((long)c >> 1) & 0x07) << 30;
        
        long val = (in.read() << 8) | in.read();
        pts |= (val >> 1) << 15;
        val = (in.read() << 8) | in.read();
        pts |= (val >> 1);
        return pts;
    }

    /**
     * This Find and parse packet ID "readPacket"
     */
    protected synchronized void parse( int readPacket ) throws IOException {
        int startCode;
        do {
            /** Find packet ID */
            startCode = findStartCode();
            
            switch ( startCode ) {
                case PACK_START_CODE: {
                    /* Read timestamp information */
                    int byte4 = in.read();
                    int byte5 = in.read();
                    int byte6 = in.read();
                    int byte7 = in.read();
                    int byte8 = in.read();
                    timeStamp = (( byte4 & 0x08 ) << 27)
                               |(( byte4 & 0x03 ) << 28)
                               |(( byte5        ) << 20)
                               |(( byte6 & 0xf8 ) << 12 )
                               |(( byte6 & 0x03 ) << 13 )
                               |(( byte7        ) << 5 )
                               |(( byte8 & 0xfe ) >> 3 );
                    timeStamp *= 300;
                    timeStamp /= 26900;
//                    timeStamp /= 28000;
                    break;
                }
                case SYSTEM_HEADER_START_CODE: {
                    /** Ignore information packet */
//                    System.out.println( "SystemHeaderStartCode" );
                    break;
                }
                case PADDING_STREAM:
                case PRIVATE_STREAM_2: {
                    /* Skip unsupported packets */
                    int length = (in.read() << 8) | in.read();
                    in.skip( length );
                    break;
                }
                default: {
                    /* Ignore unrecognised packets */
                    if (!((startCode >= 0x1c0 && startCode <= 0x1df) ||
                          (startCode >= 0x1e0 && startCode <= 0x1ef) ||
                          (startCode == 0x1bd))) {
//                        System.out.println( "Unrecognised " + startCode );
                        break;
                    }

                    /* packet length */
                    long pts = 0;
                    long dts = 0;
                    int length = (in.read() << 8) | in.read();
                    
                    /* Read control flags */
                    int c;
                    do {
                        c = in.read();
                        length--;
                    } while ( c == 0xff );

                    /* Escape code */
                    if ( (c & 0xc0) == 0x40 ) {
                        in.read();
                        c = in.read();
                        length -=2;
                    }
                    
                    /* Read Pts/Dts */
                    switch ( c & 0xf0 ) {
                        case 0x20: {
                            pts = getPts(c);
                            length -= 4;
                            break;
                        }
                        case 0x30: {
                            pts = getPts(c);
                            dts = getPts(-1);
                            length -= 9;
                            break;
                        }
                        default: {
                            if ( (c & 0xc0) != 0x80 ) break;
//                            if ( (c & 0x30) != 0) throw new IOException( "Encrypted streams not supported" );

                            /* Escaped header */
                            int flags = in.read();
                            int headerLength = in.read();
                            length -= 2;
                            if ( headerLength > length ) continue;

                            switch ( flags & 0xc0 ) {
                                case 0x80: {
                                    pts = getPts(-1);
                                    headerLength -= 5;
                                    length -= 5;
                                    break;
                                } 
                                case 0xc0: {
                                    pts = getPts(-1);
                                    dts = getPts(-1);
                                    headerLength -= 10;
                                    length -= 10;
                                    break;
                                }
                            }
                            
                            /* Skip remainder of header */
                            length -= headerLength;
                            in.skip( headerLength );
                            break;
                        }
                    }   

//                    System.out.println( "pts " + pts + " dts " + dts );
                    
                    /** Read Audio stream header 0x1bd */
                    if ( startCode == PRIVATE_STREAM_1 ) {
                        startCode = in.read();
                        length--;
                        if (startCode >= 0x80 && startCode <= 0xbf) {
                            /* audio: skip header */
                            in.skip( 3 );
                            length -= 3;
                        }
                    }

                    /* Stream ID is in startCode */
                    DataBuffer out = streams[ startCode ];
                    if ( out == null ) {
//                        System.out.println( "Allocate " + startCode );
                        out = allocateStream( startCode );
                        if ( out == null ) {
                            /* Unknown data */
                            in.skip( length );
                            break;
                        }
                    }
                    out.readData( timeStamp, in, length );
                    break;
                }
            }
        } while ( readPacket != startCode );
    }
    
    private DataBuffer allocateStream( int startCode ) {
        DataBuffer out;
//        int type;
//        int codec_id;
        if (startCode >= 0x1e0 && startCode <= 0x1ef) {
//            type = CODEC_TYPE_VIDEO;
//            codec_id = CODEC_ID_MPEG1VIDEO;
            out = new VideoTrack(this, startCode);
        } else if (startCode >= 0x1c0 && startCode <= 0x1df) {
//            type = CODEC_TYPE_AUDIO;
//            codec_id = CODEC_ID_MP2;
            out = new VideoTrack(this, startCode);
        } else if (startCode >= 0x80 && startCode <= 0x9f) {
//            type = CODEC_TYPE_AUDIO;
//            codec_id = CODEC_ID_AC3;
            out = new AudioTrack(this, startCode);
        } else if (startCode >= 0xa0 && startCode <= 0xbf) {
//            type = CODEC_TYPE_AUDIO;
//            codec_id = CODEC_ID_PCM_S16BE;
            out = new AudioTrack(this, startCode);
        } else {
            return null;
        }
        /* New stream */
        streams[ startCode] = out;
        return out;
    }
 
    /**
     * Read video data into buffer
     */
    protected synchronized void readVideo( int stream, Buffer buffer ) throws IOException {
        DataBuffer bsos;
        do {
            bsos = streams[ stream ];
            if ( bsos == null || bsos.getCurrentSize() == 0 ) {
                parse( SYNC_CHANNEL );
                bsos = null;
            }
        } while ( bsos == null );
        byte[] data = bsos.getBuffer();
        byte[] oldData = (byte[])buffer.getData();


        buffer.setData( data );
        buffer.setLength( bsos.getCurrentSize() );

        if ( oldData != null ) {
            bsos.resetBuffer( oldData );
        } else {
            bsos.resetBuffer( new byte[ data.length ] );
        }
    }
    
    /**
     * Read audio data into buffer
     */
    protected synchronized void readAudio( int stream, Buffer buffer ) throws IOException {
//        System.out.println( "byte rate " + estimateByteRate() );
        DataBuffer bsos;
        do {
            bsos = streams[ stream ];
            if ( bsos == null || (bsos.getCurrentSize() == 0)) {
                parse( SYNC_CHANNEL );
            }   
        } while (bsos == null);
            
        byte[] data = bsos.getBuffer();
        byte[] oldData = (byte[])buffer.getData();

        buffer.setData( data );
        buffer.setLength( bsos.getCurrentSize() );

        if ( oldData != null ) {
            bsos.resetBuffer( oldData );
        } else {
            bsos.resetBuffer( new byte[ data.length ] );
        }
    }
    
    /* Get Start time *
    private long startTime = 0;
    public long getStartTime() {
        return startTime;
    }
    */
    /** Required methods to be a Demultiplexer */    
    public void close() {
    }    
    
    public Object getControl(String str) {
        return null;
    }
    
    public Object[] getControls() {
        return new Object[0];
    }

    Time estimatedDuration = new Time( 1000 );
    public Time getDuration() {
        return estimatedDuration;
    }
    
    public Time getMediaTime() {
        System.out.println( "getMediaTime" );
        return new Time( 5000000 );
    }
    
    public String getName() {
        return "VOB demux";
    }
    
    public ContentDescriptor[] getSupportedInputContentDescriptors() {
//        System.out.println( "Requesting content descriptor" );
        return new ContentDescriptor[] {
            new FileTypeDescriptor( "video.vob" )
        };
    }
    
    public Track[] getTracks() throws IOException, BadHeaderException {
        int length = 0;
        for ( int i = 0; i < streams.length; i++ ) {
            if ( streams[ i ] != null ) length++;
        }
        Track[] tracks = new Track[ length ];
        for ( int i = 0; i < streams.length; i++ ) {
            if ( streams[ i ] != null ) {
                --length;
                tracks[ length ] = (Track)streams[i];
            }
        }
        return tracks;
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

        /**
         * Clear pending buffers
         **/
        for ( int i = 0; i < streams.length; i++ ) {
            if ( streams[ i ] != null ) {
                DataBuffer dataBuffer = (DataBuffer)streams[ i ];
                dataBuffer.drop();
            }
        }
        
        try { 
             seekSource.seek( (time.getNanoseconds() * ESTIMATED_BYTE_RATE)/1000000000 );
             timeStamp = 0;
             start();
             System.out.println( "Aim for position " + (time.getNanoseconds()/1000000) + " actual " + timeStamp );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        startTime = 0;  //System.currentTimeMillis() - timeStamp - MAX_AUDIO_BUFFER;
        
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

//                System.out.println( "Data source " + this.dataSource + (this.dataSource instanceof Seekable) );
                
                /* Find file size */
                estimatedDuration = new Time( 1000 );
                try {
                    MediaLocator locator = inputDataSource.getLocator();
                    if ( locator != null ) {
                        URL url = locator.getURL();
                        if ( url != null ) {
                            File file = new File( url.getFile() );
                            totalFileSize = file.length();
//                System.out.println( "File size " + (totalFileSize * 1000000/ ESTIMATED_BYTE_RATE) );
                            estimatedDuration = new Time( (totalFileSize * 1000000000 / ESTIMATED_BYTE_RATE) );
                        }
                    }
                } catch ( IOException e ) {
                }
            }
            in = new PullSourceInputStream( pullSource );
            return;
        }
        throw new javax.media.IncompatibleSourceException();
    }
    
    public static final long ESTIMATED_BYTE_RATE = 540000;
    /*
    protected float estimateByteRate() {
        long time  = System.currentTimeMillis() - rateEstimateTime;
        long bytes = (seekSource.tell() - rateEstimateBase) * 1000;
        
        if ( time == 0 || rateEstimateTime == 0 || time > 50000 ) {
            rateEstimateTime = System.currentTimeMillis();
            rateEstimateBase = seekSource.tell();
            if ( time <= 50000 ) return ESTIMATED_BYTE_RATE;
        }
        return bytes / time;
    }
    */
    
    public synchronized void start() throws java.io.IOException {
        parse( SYNC_CHANNEL );
        startTime = System.currentTimeMillis() - timeStamp - MAX_AUDIO_BUFFER;
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
        parse( SYNC_CHANNEL );
    }
    
    public void stop() {
    }
    
    /**
     * The estmated startTime of the stream timer
     */
    private long startTime = 0;
    
    /**
     * Note the Audio is buffered so this is not exact.
     * We fiddle the startTime so that the video is never more than
     * MAX_AUDIO_BUFFER behind
     */
    public final void setAudioTimeStamp( long timeStamp ) {
        long time = System.currentTimeMillis() - startTime;
        
        /* Video is too far behind audio */
        if ( time + MAX_AUDIO_BUFFER < timeStamp ) {
if (debugLipSync)            System.out.println( "Audio resync " + time + " " + timeStamp );
            startTime -= timeStamp - time - MAX_AUDIO_BUFFER;
        }
        
        if ( time >= timeStamp && timeStamp > MAX_AUDIO_BUFFER ) {
            startTime -= timeStamp - time - MAX_AUDIO_BUFFER;
            return;
        }
        /* Video is too far behind audio */
        if ( time + MIN_AUDIO_BUFFER > timeStamp && timeStamp > MAX_AUDIO_BUFFER  ) {
if (debugLipSync)           System.out.println( "Audio behind " + time + " " + timeStamp );
            startTime -= timeStamp - time - MAX_AUDIO_BUFFER;
        }
    }
    
    /**
     * Returns true if the video is behind target
     */
    public final boolean isVideoSlow( long target ) {
        if ( showAllFrames ) return false;
        
        if ( startTime == 0 ) startTime = System.currentTimeMillis();  //demux.getStartTime();

        long time = System.currentTimeMillis() - startTime;
        
        if ( target < 1000 && time - target > 5000 ) {
            /* change video section */
            startTime = System.currentTimeMillis();
            return false;
        }
        
if (debugLipSync)       System.out.print( "time " + time + " target " + target  );
        if (target <= time) { 
if (debugLipSync)  System.out.println( " slow " + ( time - target ) );
            return true;
        }
if (debugLipSync)  System.out.println();
        if (target > time + 400) {
            synchronized( this ) {
                try {
                    if ( target - time - 15 < 200 ) {
if (debugLipSync)                    System.out.println( "Wait " + (target - time - 15 ) );
                    this.wait( target - time - 15 );
                    } else {
                        this.wait( 100 );
                    }
                } catch ( Exception e ) {}
            }
        }
        return false;
    }
}
