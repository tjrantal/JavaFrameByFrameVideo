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

import java.io.*;
import javax.media.Track;
import javax.media.Time;
import javax.media.Format;
import javax.media.Buffer;
import javax.media.TrackListener;
import javax.media.format.AudioFormat;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.jffmpeg.GPLLicense;

/**
 * This class handles audio data read from a VOB file
 */
public class AudioTrack extends DataBuffer implements Track, GPLLicense {
    VobDemux demux;
    int streamNumber;
    boolean enabled = true;
    
    /** Creates a new instance of AudioTrack */
    public AudioTrack( VobDemux demux, int streamNumber ) {
        this.demux = demux;
        this.streamNumber = streamNumber;
    }
    
    public Time getDuration() {
        System.out.println( "Get Duration" );
        return new Time( 500 );
    }

    /**
     * Return Audio format of ac3
     */
    public Format getFormat() {
         return new AudioFormat("ac3", 48000,
                                       16,
                                       2,
                                       0, 1); // endian, int signed
    }

    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Time mapFrameToTime(int param) {
        System.out.println( "Map Frame To Time" );
        return new Time( param );
    }
    
    public int mapTimeToFrame(javax.media.Time time) {
        System.out.println( "Map Time to Frame" );
        return 0;
    }
    
    public static final int[] halfRate = {0,0,0,0,0,0,0,0,0,1,2,3};
    public static final int[] rate     = { 32,  40,  48,  56,  64,  80,  96, 112,
			                   128, 160, 192, 224, 256, 320, 384, 448,
			                   512, 576, 640};
    private int offset = 0;
    private int sample_rate;
    private int frame_length;
    private long time = 0;    // Using microseconds

    /**
     * Return a buffer containing audio data
     */
    public void readFrame(Buffer buffer) {
        try {
            demux.readAudio( streamNumber, buffer );
            if ( !enabled ) buffer.setLength(0);
            if ( timeStamp != 0 ) demux.setAudioTimeStamp( timeStamp );
            timeStamp = 0;

            byte[] data   = (byte[])buffer.getData();
            int    length = buffer.getLength();
            /*
            byte[] data   = (byte[])buffer.getData();
            int    length = buffer.getLength();
            
            while ( offset < length - 6 ) {
                if ( !parseHeader( data, offset ) ) {
//                    System.out.println( "NOT sync block" );
                    offset++;
                    continue;
                }
                offset += frame_length;
            }
            offset -= length;
            if ( offset < 0 ) offset = 0;
            
//            System.out.println( sample_rate);
             */
        } catch (IOException e) {
        }
    }

//    private boolean parseHeader( byte[] data, int offset ) {
//        /**
//         * Sync header
//         */
//        if ( data[ offset ] == 0x0b && data[ offset + 1 ] == 0x77 ) {
//            int frmsizecod = data[ offset + 4 ] & 63;
//            
//            int halfRateIndex = (data[ offset + 5 ] >> 3) &0xff;
//            if ( halfRateIndex >= halfRate.length ) return false;
//            int half = halfRate[ halfRateIndex ];
//
//            if ( frmsizecod >= 38 ) return false;
//            int bit_rate = (rate[ frmsizecod >> 1 ] * 1000) >> half;
//
//            switch ( data[ offset + 4 ] & 0xc0 ) {
//                case 0x00: {
//                    sample_rate = 48000 >> half;
//                    frame_length = 4 * rate[ frmsizecod >> 1 ];
//                    break;
//                }
//                case 0x40: {
//                    sample_rate = 44100 >> half;
//                    frame_length = 2 * ( 320 * rate[ frmsizecod >> 1 ] / 147 + (frmsizecod & 1) );
//                    break;
//                }
//                case 0x80: {
//                    sample_rate = 32000 >> half;
//                    frame_length = 6 * rate[ frmsizecod >> 1 ];
//                    break;
//                }
//                default: {
//                    return false;
//                }
//            }
//        } else {
//            return false;
//        }
//        return true;
//    }
            
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Time getStartTime() {
        System.out.println( "Get Start time" );
        return new Time( 2000 );
    }

    public void setTrackListener(TrackListener trackListener) {
    }
    
    private volatile long timeStamp;
    
    /**
     * Data received from vob file
     */
    public void readData( long timeStamp, InputStream in, int length ) throws IOException {
        super.readData( timeStamp, in, length );
        if ( this.timeStamp == 0 ) this.timeStamp = timeStamp;
    }
}
