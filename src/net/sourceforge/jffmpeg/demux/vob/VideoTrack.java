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

import javax.media.Track;
import javax.media.Time;
import javax.media.Format;
import javax.media.Buffer;
import javax.media.TrackListener;
import javax.media.format.VideoFormat;
import java.awt.Dimension;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.jffmpeg.GPLLicense;

/**
 * This class handles video data read from a VOB file
 */
public class VideoTrack extends DataBuffer implements Track, GPLLicense {
    /**
     * Internal statistics
     */
    private int totalNumberOfFrames = 0;
    private int framesDelivered     = 0;
    public int videoFramesBufferTargetLow  = 0;
    public int videoFramesBufferTargetHigh = 0;

    /* We only really get 1 frame every 20 reads */
    private int packetsPerFrame = 0;
    private static final int PACKETS_PER_FRAME = 10;
    
    /**
     * Time in milliseconds this frame was meant to start
     */
    private int width, height;
    private float frameRate = 30;
    private long halfSeconds = 0;

    VobDemux demux;
    int streamNumber;
    boolean enabled;

    /**
     * Creates a new instance of Video Track
     */
    public VideoTrack( VobDemux demux, int streamNumber ) {
        this.demux = demux;
        this.streamNumber = streamNumber;
        enabled = (streamNumber == 0x1e0);

        /** 
         * Initialise frame buffers
         */
        for ( int i = 0; i < frameBuffer.length; i++ ) {
            frameBuffer[ i ] = new Buffer();
            frameBuffer[ i ].setData( new byte[ 1000 ] );
        }
        partialFrame.setData( new byte[ 1000 ] );
        partialFrame.setLength( 0 );
    }

    Time time = new Time( (long)(1000 / frameRate) );
    public Time getDuration() {
//        System.out.println( "Get Duration" );
        return time;
    }

    /**
     * Video format
     */
    public Format getFormat() {
        return new VideoFormat( "mpeg", 
                                new Dimension(width, height), 
                                10000, 
                                (new byte[0]).getClass(), 
                                frameRate );
    }

    public Time getStartTime() {
        System.out.println( "Get Start time" );
        return new Time( 2000 );
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Time mapFrameToTime(int param) {
        System.out.println( "Map Frame to time" );
        return new Time( param );
    }
    
    public int mapTimeToFrame(javax.media.Time time) {
        System.out.println( "Map Time to Frame" );
        return 0;
    }
    
    /**
     * Append data to a buffer
     */
    private static final void appendBuffer( Buffer buffer, byte[] data, int length ) {
        byte[] bdata = (byte[])buffer.getData();
        int    blen  = buffer.getLength();
        
        /**
         * Is the buffer large enough ?
         */
        if ( bdata.length < blen + length ) {
            byte[] temp = new byte[ (blen + length) * 2];
            System.arraycopy( bdata, 0, temp, 0, blen );
            buffer.setData( temp );
            bdata = temp;
        }
        System.arraycopy( data, 0, bdata, blen, length );
        buffer.setLength( blen + length );
    }
    
    /**
     * Copy data to a buffer
     */
    private static final void setBuffer( Buffer buffer, byte[] data, int offset, int length ) {
        byte[] bdata = (byte[])buffer.getData();
        
        /**
         * Is the buffer large enough ?
         */
        if ( bdata.length < length ) {
            bdata = new byte[ length * 2];
            buffer.setData( bdata );
        }
        System.arraycopy( data, offset, bdata, 0, length );
        buffer.setLength( length );
    }        

    public static final byte PICTURE_START_CODE = 0;
    public static final byte SEQUENCE_START_CODE = (byte)0xb3;
    public static final int I_TYPE = 1;
    public static final int P_TYPE = 2;
    public static final int B_TYPE = 3;
    public static final String[] pictName = new String[] { "0", "I", "P", "B", "4", "5", "6", "7" };

    /**
     * Split the input data into frames of data
     */
    private int currentFrameNumber = 0;
    
    public static final int FRAME_BUFFER_MASK = 255;
    private int[]    frameType      = new int[ FRAME_BUFFER_MASK + 1 ];
    private long[]   frameReference = new long[ FRAME_BUFFER_MASK + 1 ];
    private Buffer[] frameBuffer    = new Buffer[ FRAME_BUFFER_MASK + 1 ];
    
    private Buffer   partialFrame   = new Buffer();

    /**
     * Temporary work areas
     */
    private Buffer temp = new Buffer();
    private int[]    framePointer = new int[ FRAME_BUFFER_MASK + 1 ];
    
    /**
     * Supply a frame of data to codec
     */
    public void readFrame(Buffer outputBuffer) {
        outputBuffer.setFlags( Buffer.FLAG_NO_WAIT );
        try {
            /* Ignore data if this channel is disabled */
            if ( !enabled ) {
                demux.readVideo( streamNumber, temp );
                outputBuffer.setLength( 0 );
                return;
            }
            
            do {
                /**
                 * Read data from VOB file
                 */
                while ( currentFrameNumber == getNumberOfFrames() ) {
                    demux.parse( streamNumber );
                }
                
//    System.out.println( "Frame number " + currentFrameNumber + " " + getNumberOfFrames() );
                /* Supply next frame */
                outputBuffer.setData( frameBuffer[ currentFrameNumber ].getData() );
                outputBuffer.setLength( frameBuffer[ currentFrameNumber ].getLength() );
                    
                currentFrameNumber = (currentFrameNumber + 1) & FRAME_BUFFER_MASK;
                
                /* Remove B-Frame if we are running slow */
            } while (   frameType[ (currentFrameNumber - 1) & FRAME_BUFFER_MASK ] == B_TYPE
                     && demux.isVideoSlow( frameReference[ currentFrameNumber ] ) );
            
            framesDelivered++;
        } catch (IOException e) {
        }
    }
        
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setTrackListener(TrackListener trackListener) {
    }

    public static final float[] frameRateTable = new float[] {
        0,     24000, 24024, 25025, 30000, 30030, 50050,
        60000, 60060, 15015,  5005, 10010, 12012, 15015 
    };
    
    public static final double[] aspectRatioTable = new double[] {
        0,   1.0, -3.0/4.0, -9.0/16.0, -1.0/2.21,
        0,   1.0, -3.0/4.0, -9.0/16.0, -1.0/2.21,
        0,   1.0, -3.0/4.0, -9.0/16.0, -1.0/2.21,
        0,   1.0, -3.0/4.0, -9.0/16.0, -1.0/2.21,
    };

    private int numberOfFramesAvailable = 0;
    
    /**
     * We need to synchronize access to the number of frames available
     */
    private synchronized int getNumberOfFrames() {
        return numberOfFramesAvailable;
    }
    
    private synchronized void setNumberOfFrames( int nof) {
        numberOfFramesAvailable = nof;
    }

    /**
     * Extract frames into frame buffer
     */
    public synchronized void readData( long timeStamp, InputStream in, int length ) throws IOException {
        /* Read array of data */
        if ( buffer.length < length ) {
            buffer = new byte[ length * 2 ];
        }
        
        size = 0;
        int read = 0;
        while ( length > 0 ) {
            read = in.read( buffer, size, length );
            if ( read < 0 ) throw new IOException( "End of Stream" );
            length -= read;
            size += read;
        }
        
        /* Append to existing data */
        appendBuffer( partialFrame, buffer, size );
        
        /* We only really get 1 frame every 20 reads */
        if ( packetsPerFrame-- > 0 ) return;
        packetsPerFrame = PACKETS_PER_FRAME;
        
        byte[] currentData    = (byte[])partialFrame.getData();
        int currentDataLength =         partialFrame.getLength();

        /**
         * Extract frame data
         */
        int numberOfFrames = getNumberOfFrames();
        int oldNumberOfFrames = numberOfFrames;
//        currentFrameNumber = 0;

        boolean sequenceFrame = false;
//        boolean dropThisFrame = false;
        for ( int i = 0; i < currentDataLength - 6; i++ ) {
            /**
             * Extract pointers to frames 
             */
            if (    currentData[ i     ] == 0
                 && currentData[ i + 1 ] == 0
                 && currentData[ i + 2 ] == 1 ) {
                byte header = currentData[ i + 3 ];
                if ( header == PICTURE_START_CODE ) {

                    /* Picture start code */
                    int reference = ((currentData[ i + 4 ] & 0xff) << 2) | ( (currentData[ i + 5 ] >> 6 ) & 0x03 );
                    int pict_code = (currentData[ i + 5 ] >>3) & 0x7;

//                    System.out.println( pictName[pict_code] + " " + reference );                            

                    if ( !sequenceFrame ) {
                        framePointer[ numberOfFrames ] = i;
                        numberOfFrames = (numberOfFrames + 1) & FRAME_BUFFER_MASK;
                    }
                    frameType[ (numberOfFrames - 1) & FRAME_BUFFER_MASK ] = pict_code;
                    frameReference[ (numberOfFrames - 1) & FRAME_BUFFER_MASK ] = timeStamp;
                    sequenceFrame = false;
                }
                if ( header == SEQUENCE_START_CODE ) {
                    sequenceFrame = true;
                    framePointer[ numberOfFrames ] = i;
                    numberOfFrames = (numberOfFrames + 1) & FRAME_BUFFER_MASK;

                    width  = ((currentData[ i + 4 ] & 0xff)<<4)|((currentData[ i + 5 ] & 0xff)>>4);
                    height = ((currentData[ i + 5 ] & 0x0f)<<8)|((currentData[ i + 6 ] & 0xff));
                    double aspect = aspectRatioTable[ (currentData[ i + 7 ] >> 4) & 0x0f ];
                    frameRate = (float)frameRateTable[ currentData[ i + 7 ] & 0x0f ] / 1001;
//                    System.out.println( width + "," + height + " " + aspect + " " + frameRate + " " + currentData[ i + 7 ] );
                }
            }
        }

        /* Split data into frames */
        if ( numberOfFrames != oldNumberOfFrames ) {
            numberOfFrames = ( numberOfFrames - 1 ) & FRAME_BUFFER_MASK;
            for ( int framePointerNumber = oldNumberOfFrames; 
                  framePointerNumber != numberOfFrames; 
                  framePointerNumber = (framePointerNumber+1) & FRAME_BUFFER_MASK ) {
                setBuffer( frameBuffer[ framePointerNumber ], currentData, framePointer[ framePointerNumber ],
                           framePointer[ (framePointerNumber + 1)& FRAME_BUFFER_MASK ] - framePointer[ framePointerNumber ] );
                totalNumberOfFrames++;
            }
//            System.out.println( "totalNumberOfFrames " + totalNumberOfFrames );
        }
        /**
         * Leftover data goes into the partialFrame
         */
        setBuffer( partialFrame, currentData, framePointer[ numberOfFrames ], currentDataLength - framePointer[ numberOfFrames ] );
        setNumberOfFrames( numberOfFrames );
    }
    
    /**
     * Dump all data pending seek.  Note thread safety isn't the best
     */
    public synchronized void drop() {
        setNumberOfFrames( 1 );
        currentFrameNumber = 0;
        totalNumberOfFrames = 0;
        super.drop();
        
        /** 
         * Re-initialise buffers (should help reduce memory allocation)
         */
        for ( int i = 0; i < frameBuffer.length; i++ ) {
            frameBuffer[ i ] = new Buffer();
            frameBuffer[ i ].setData( new byte[ 1000 ] );
        }
        partialFrame.setData( new byte[ 1000 ] );
        partialFrame.setLength( 0 );

    }
}
