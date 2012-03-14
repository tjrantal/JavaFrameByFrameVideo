/*
 * Java port of ffmpeg mp3 decoder.
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
package net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3;

import java.awt.Dimension;

import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.Buffer;

import net.sourceforge.jffmpeg.JMFCodec;

import net.sourceforge.jffmpeg.codecs.utils.BitStream;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;

import net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3.data.Table;

import java.io.*;

/**
 * Mp3 Codec
 */
public class MP3 implements Codec, JMFCodec {
    public static final boolean debug = false;
    
    public final static int MPA_STEREO  = 0;
    public final static int MPA_JSTEREO = 1;
    public final static int MPA_DUAL    = 2;
    public final static int MPA_MONO    = 3;
    
    public final static int MODE_EXT_I_STEREO = 1;
    public final static int MODE_EXT_MS_STEREO = 2;
    
    protected BitStream in = new BitStream();
    protected BitStream granuleIn = new BitStream();

    /**
     * Expected header
     */
    protected int headerMask    = 0;
    protected int streamHeader = 0;
    
    /**
     * MP3 Packet header
     */
    protected boolean gotHeader = false;
    
    protected boolean lsf;
    protected int     layer;
    protected boolean mpeg25;
    protected boolean error_protection;
    protected int     bitrate_index;
    protected int     sample_rate_index;
    protected boolean padding;
    protected boolean extension;
    protected int     mode;
    protected int     mode_ext;
    protected boolean copyright;
    protected boolean original;
    protected int     emphasis;
    
    protected int     frame_size;
    protected int     bitRate;
    
    /**
     * ID3 headers
     */
    public static int ID3 = 0x494433;
    public static int LAME = 0x4c414d45;
    private boolean lameFile;
    
    /**
     * Internal variables
     */
    protected int nb_channels;
    protected int[][][] mpa_bitrate_tab = Table.getBitrateTable();
    protected int[] mpa_freq_tab = new int[] { 44100, 48000, 32000 };
    
    protected Granule[][] granules;
    
    /**
     * IMDCT
     **/
    private int[][][] sb_samples  = new int[2][2][ 18 * Granule.SBLIMIT ];
    private int[][]   mdct_buffer = new int[2][ Granule.SBLIMIT * 18 ];

    private SoundOutput soundOutput = new SoundOutput();
    
    /**
     * Search for header
     */
    private final boolean isHeader( int header ) {
        return ( (header & 0xffe00000) == 0xffe00000 )
            && ( (header & (3  << 17)) != 0 )
            && ( (header & (15 << 12)) != 15 )
            && ( (header & (3  << 10)) != 3 );
    }
    public MP3() {
        granules = new Granule[ 2 ][ 2 ];
        granules[ 0 ][ 0 ] = new Granule();
        granules[ 0 ][ 1 ] = new Granule();
        granules[ 1 ][ 0 ] = new Granule();
        granules[ 1 ][ 1 ] = new Granule();
    }
    
    private int decodeHeader() {
        int header = in.getBits( 16 ) << 16 | in.getBits( 16 );

//        System.out.println( "Header " + Integer.toHexString( header ) );
        
        /**
         * Header
         */
        int sync = (header >> 21) & 0x7ff;       // bits 21-31
        if ( (header & (1 << 20)) != 0 ) {       // bit 20
            lsf = (header & (1 << 19)) == 0;     // bit 19
            mpeg25 = false;
        } else {
            lsf = true;
            mpeg25 = true;
        }
        layer = 4 - ((header >> 17) & 3);              // bit 17-18
        error_protection = ((header >> 16 ) & 1) == 0; // bit 16
        bitrate_index = (header >> 12) & 0xf;          // bit 12-15
        sample_rate_index = (header >> 10) & 3;        // bit 10-11
        padding = ((header >> 9) & 1) != 0;            // bit 9
        extension = ((header >> 8) & 1) != 0;          // bit 8
        mode = ((header >> 6) & 3);                    // bit 6-7
        mode_ext = ((header >> 4) & 3);                // bit 4-5
        copyright = ((header >> 3) & 1) != 0;          // bit 3
        original = ((header >> 2) & 1) != 0;           // bit 2
        emphasis = header & 3;                         // bit 0-1
        
        
        /**
         * Calculate internal variables
         */
        nb_channels = (mode == MPA_MONO) ? 1 : 2;
        
        
        
        if ( (((((0xffe00000 | (3 << 17) | (0xf << 12) | (3 << 10) | (3 << 19))) & currentHeader) != streamHeader )
             && streamHeader != 0)
             || (sync != 0x7ff)
             || (layer == 4)
             || (bitrate_index == 0xf)
             || (sample_rate_index == 3) ) {   //Should code this :)
    
            
             in.seek( (in.getPos() - 24) & ~0x07 );
             while ( in.availableBits() > 32 ) {
                 if (in.showBits( 11 ) ==0x7ff ) {
                     break;
                 }
                 in.getBits( 8 );
             }
             return -1;

                 /*
             if ( (header & 0xffffff00) == ID3 || lameFile ) {
                 /* Skip into file *
                 in.seek( in.getPos() + 0x500 );
             }
             /*
                 lameFile = true;
                  /* Find LAME *
                  while ( header != LAME || ((header & 0xff000000)==0x55000000)||((header & 0xff000000)==0x00000000)) {
                      if ( in.availableBits() < 32 ) return -1;
                      in.seek( (in.getPos() - 24) & ~0x07 );
                      header = in.getBits( 16 ) << 16 | in.getBits( 16 );
                  }
              }
                  */

              //            throw new Error( "Not suitable header" );
        }
        if ( debug ) {
            System.out.println( "Decoded header..." );
            System.out.println( "layer:         " + layer );
            System.out.println( "nb_channels:   " + nb_channels );
            System.out.println( "lsf:           " + (lsf? 1:0) );
        }
        
        streamHeader = currentHeader & (0xffe00000 | (3 << 17) | (0xf << 12) | (3 << 10) | (3 << 19));

        /* Calculate frame size */
        if ( bitrate_index != 0 ) {
            int sample_rate = mpa_freq_tab[ sample_rate_index ] >> ((lsf?1:0) + (mpeg25?1:0));
            sample_rate_index += 3*((lsf?1:0) + (mpeg25?1:0));
            frame_size = mpa_bitrate_tab[ lsf ? 1:0 ][ layer - 1 ][ bitrate_index ];
            bitRate = frame_size * 1000;
            switch( layer ) {
                case 1: {
                    frame_size = (frame_size * 12000) / sample_rate;
                    frame_size = (frame_size + (padding ? 1:0) ) * 4;
                    break;
                }
                case 2: {
                    frame_size = (frame_size * 144000) / sample_rate;
                    frame_size += padding ? 1:0;
                    break;
                }
                default:
                case 3: {
                    frame_size = (frame_size * 144000) / ( sample_rate << (lsf?1:0));
                    frame_size += padding ? 1:0;
                    break;
                }
            }
        } else {
            throw new Error( "Free format frame size" );
        }
if ( debug )        System.out.println( "Frame size " + frame_size );
        return header;
    }
        
    private void decodeMP3( Buffer outputBuffer, int frameStart ) throws FFMpegException {
        if ( error_protection ) {
             in.getBits( 16 );    //Error handling
        }
        
        int main_data_begin;
        int private_bits;
        
        int nb_granules;
        if ( lsf ) {
            main_data_begin = in.getBits( 8 );
            private_bits    = in.getBits( (nb_channels == 2) ? 2 : 1 );
            nb_granules = 1;
        } else {
            main_data_begin = in.getBits( 9 );
            private_bits    = in.getBits( (nb_channels == 2) ? 3 : 5 );
            nb_granules = 2;
            for ( int channel = 0; channel < nb_channels; channel++ ) {
                granules[ channel ][ 0 ].setScfsi( 0 );
                granules[ channel ][ 1 ].setScfsi( in.getBits( 4 ) );
            }
        }
 if ( debug ) {
        System.out.println( "main_data_begin " + main_data_begin );
        System.out.println( "private_bits    " + private_bits );
 }
        
        for ( int granuleNumber = 0; granuleNumber < nb_granules; granuleNumber++ ) {
            for ( int channel = 0; channel < nb_channels; channel++ ) {
                granules[ channel ][ granuleNumber ].read( in, lsf, mode_ext );
if ( debug )                System.out.println( granules[ channel ][ granuleNumber ] );
            }
        }
        /* SEEK TO MAINDATA */
//        System.out.println( "seekback: " + main_data_begin );
        int seekPos = granuleIn.getPos() + granuleIn.availableBits() - main_data_begin * 8;
        
        granuleIn.seek( seekPos );
        
        /* Add new data */
        byte[] newGranuleData = in.getDataArray();
        int startNewData = (in.getPos() + 7)/8;
        granuleIn.addData( newGranuleData, startNewData, frame_size - (startNewData - frameStart/8)  );
        in.seek( frameStart + frame_size * 8  );
        
        /* Don't read data we don't yet have */
        if ( seekPos < 0 ) {
            return;
        }
        
//        System.out.println( "read granules" );
        
        for ( int granuleNumber = 0; granuleNumber < nb_granules; granuleNumber++ ) {
            for ( int channel = 0; channel < nb_channels; channel++ ) {
                granules[ channel ][ granuleNumber ].readScaleFactors( granuleIn, lsf, granules[ channel ][ 0 ], channel, mode_ext );
if (debug)                granules[ channel ][ granuleNumber ].dumpScaleFactors();
                
if ( debug )                System.out.println( "exponents from scale factors" );
                granules[ channel ][ granuleNumber ].exponents_from_scale_factors( sample_rate_index );

if ( debug )                System.out.println( "Decode Huffman" );
                granules[ channel ][ granuleNumber ].huffman_decode( granuleIn, sample_rate_index );
            }
            
            /* Compute Stereo */
            if ( nb_channels == 2 ) { 
if ( debug )                System.out.println( "Compute Stereo" );
                granules[ 1 ][ granuleNumber ].computeStereo( this, granules[ 0 ][ granuleNumber ] );
            }
            for ( int channel = 0; channel < nb_channels; channel++ ) {
                /* Reorder block */
if ( debug )                System.out.println( "Reorder Block" );
                granules[ channel ][ granuleNumber ].reorderBlock( this );
                
                /* Compute antialias */
if ( debug )                System.out.println( "Antialias" );
                granules[ channel ][ granuleNumber ].antialias( this );
if ( debug )                granules[ channel ][ granuleNumber ].dumpHybrid();
                
                /* Compute imdct */
                soundOutput.computeImdct( granules[ channel ][ granuleNumber ],
                                          sb_samples[ channel ][ granuleNumber ],
                                          mdct_buffer[ channel ] );
            }
        }        
        /* Synth_finter */
        byte[] out = (byte[])outputBuffer.getData();
        int outputPointer = outputBuffer.getLength();
        if ( out == null ) { out = new byte[ 0 ]; outputPointer = 0; }
        if ( out.length - outputPointer < nb_channels * nb_granules * 18 * 32 * 2 ) {
            byte[] tmp = out;
            out = new byte[ 4 * ( out.length + nb_channels * nb_granules * 18 * 32 ) ];
            System.arraycopy( tmp, 0, out, 0, outputPointer );
            outputBuffer.setData( out );
        }

        for ( int channel = 0; channel < nb_channels; channel++ ) {
            int samplePointer = outputPointer + channel * 2;
            for ( int frameNumber = 0; frameNumber < nb_granules * 18; frameNumber++ ) {
                soundOutput.synth_filter( channel, nb_channels,
                                          out, samplePointer, 
                                          sb_samples[ channel ][ frameNumber / 18 ], (frameNumber % 18) * Granule.SBLIMIT );
                samplePointer += 32 * nb_channels * 2;
            }
        }
        
        outputBuffer.setLength( outputPointer + nb_channels * nb_granules * 18 * 32 * 2);
        /*
        System.out.println( "Output " + (18 * nb_granules) + " " + nb_channels );
        for ( int i = 0; i < nb_channels * nb_granules * 18 * 32; i++ ) {
            System.out.print( out[ i ] + " " );
            if ( (i % 18) == 17 ) System.out.println();
        }
        System.out.println( "end");
         */
    }
        
        
        /**
         * Caluclate from header
         */
        
    
    /**
     * Codec management
     */
    public Format[] getSupportedInputFormats() {
if ( debug )        System.out.println( "getSupportedInputFormats" );
        return new Format[] { new AudioFormat( "mpeglayer3" ) };
    }
    
    public Format[] getSupportedOutputFormats(Format format) {
if ( debug )        System.out.println( "getSupportedOutputFormats" );
        return new Format[] { new AudioFormat( "LINEAR" ) };
    }
    
    private AudioFormat inputFormat;
    public Format setInputFormat( Format format ) {
if ( debug )        System.out.println( "Input Format: " + format );
        
        inputFormat = (AudioFormat)format;
        return format;
    }
    
    public Format setOutputFormat( Format format ) {
if ( debug )        System.out.println( "Output Format: " + format );
        return new AudioFormat("LINEAR", inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits() > 0 ? inputFormat.getSampleSizeInBits() : 16,
                                         inputFormat.getChannels(),
                                         0, 1); // endian, int signed
    }
    
//    private OutputStream outtemp;
    private int currentHeader = -1;
    public int process( Buffer input, Buffer output ) {
        /* Flush buffer */
        if ( (input.getFlags() & Buffer.FLAG_FLUSH) != 0 ) reset();

        output.setFlags( input.getFlags() );
        output.setTimeStamp( input.getTimeStamp() );
        output.setDuration( input.getDuration() );
        
        try {
            byte[] data = (byte[])input.getData();  //in.getLength
            int    length = input.getLength();
/*
            try {
                outtemp.write( data, 0, length );
            } catch ( IOException e ) {
            }
  */          
            /*
            System.out.println( "Parsing packet" );            
            for ( int i = 0; i < length; i++ ) {
                System.out.print( Integer.toHexString( data[i] & 0xff ) + " " );
            }
            */
            
            /**
             * Parse data
             */
            in.addData( data, 0, length );
            do {
                if ( currentHeader == -1 ) {
                    /*
                     * Read header (need 32 bits)
                     */
                    if ( in.availableBits() < 32 ) break;
                    currentHeader = decodeHeader( );
                    if ( currentHeader == -1 ) continue;
                }

                /* 
                 * Do we have a complete packet?
                 */
                if ( frame_size * 8 < in.availableBits() - 32 ) {
                    decodeMP3( output, in.getPos() - 32 );
                    currentHeader = -1;
                }
            } while ( currentHeader == -1 && in.availableBits() >= 128);

            /*
            int[]  out = (int[])output.getData();
            int outLength = output.getLength();
            byte[] a = new byte[ outLength * 2 ];
            for ( int k = 0; k < outLength; k++) {
                a[ k * 2 ]     = (byte)(out[k] & 0xff);
                a[ k * 2 + 1 ] = (byte)((out[k] & 0xff00) >> 8);
            }
            output.setData( a );
            output.setLength( outLength * 2 );
             */
            /*
            outStream.write( data, 0, length );
            System.out.println();
             */
    } catch ( Exception e ) {
            reset();
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
    } catch( Error e ) {
            reset();
            e.printStackTrace();
            currentHeader = -1;
    }
        return BUFFER_PROCESSED_OK;
    }
        
    
    public void open() {
        /*
        try {
        outtemp = new FileOutputStream( "MpegData.mp3" );
        } catch (IOException e ) {
        }
         */
    }
    
    public void close() {
       /*
        try {
        outtemp.close();
        } catch (IOException e ) {
        }
        **/
    }
    
    public void reset() {
        sb_samples  = new int[2][2][ 18 * Granule.SBLIMIT ];
        mdct_buffer = new int[2][ Granule.SBLIMIT * 18 ];
        soundOutput = new SoundOutput();
        in = new BitStream();
        granuleIn = new BitStream();
        granules[ 0 ][ 0 ] = new Granule();
        granules[ 0 ][ 1 ] = new Granule();
        granules[ 1 ][ 0 ] = new Granule();
        granules[ 1 ][ 1 ] = new Granule();
    }
    
    public String getName() {
        return "mpeglayer3";
    }
    
    public Object[] getControls() {
        return new Object[ 0 ];
    }
    
    public Object getControl( String type ) {
        return null;
    }

    /**
     * Implement the Jffmpeg codec interface
     */
    public boolean isCodecAvailable() {
        return true;
    }

    /**
     * Outofbands video size
     */
    public void setVideoSize( Dimension size ) {
    }

    public void setEncoding( String encoding ) {
    }

    public void setIsRtp( boolean isRtp ) {
    }

    public void setIsTruncated( boolean isTruncated ) {
    }
}
