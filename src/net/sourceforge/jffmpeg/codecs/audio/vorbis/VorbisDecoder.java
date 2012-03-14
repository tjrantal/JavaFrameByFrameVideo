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

package net.sourceforge.jffmpeg.codecs.audio.vorbis;

import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.Buffer;

import net.sourceforge.jffmpeg.JMFCodec;
import java.awt.Dimension;

import net.sourceforge.jffmpeg.codecs.audio.vorbis.floor.*;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.residue.*;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.mapping.*;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.mapping.Mdct;

/**
 * Vorbis Codec
 */
public class VorbisDecoder implements Codec, JMFCodec {
    public static final boolean debug = false;
    

    /* Input data format */
    private AudioFormat inputFormat;
    
    /* Header information */
    private int headersRequired = 3;

    /* Audio information */
    private int channels;
    private int[] blocksize = new int[ 2 ];

    /* Pluggable Modules */
    private CodeBook[] codeBooks;
    private Floor[] floor_param;
    private Residue[] residue_param;
    private Mapping[] mapping_param;
    private Mode[]    modes_param;

    /* Modified discrete cosine transforms */
    private Mdct[]    mdct = new Mdct[ 2 ];

    /* Bitreader */
    private OggReader oggRead = new OggReader();

    /* Retrieve modules */
    public CodeBook getCodeBook( int i ) {
        return codeBooks[i];
    }

    public Floor getFloor( int i ) {
        return floor_param[i];
    }
  
    public Residue getResidue( int i ) {
        return residue_param[i];
    }

    public Mapping getMapping( int i ) {
        return mapping_param[i];
    }

    public Mode getMode( int i ) {
        return modes_param[i];
    }

    public Mdct getMdct() {
        return mdct[ W?1:0 ];
    }

    /**
     * Codec management
     */
    public Format[] getSupportedInputFormats() {
        return new Format[] { new AudioFormat( "vorbis" ) };
    }
    
    public Format[] getSupportedOutputFormats(Format format) {
        return new Format[] { new AudioFormat( "LINEAR" ) };
    }
    
    public Format setInputFormat( Format format ) {
        inputFormat = (AudioFormat)format;
        return format;
    }
    
    public Format setOutputFormat( Format format ) {
        return new AudioFormat("LINEAR", inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits() > 0 ? inputFormat.getSampleSizeInBits() : 16,
                                         inputFormat.getChannels(),
                                         0, 1);
    }

    private static final int ilog2( int v ) {
        int c = 0;
        for ( ; v > 0; v >>= 1 ) c++;
        return c;
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

    /* Read an OGG 24 bit int */
    private int read24Bits( byte[] buffer, int offset ) {
        int ret = 0;
        ret = (ret << 8 ) | (buffer[ offset + 2 ] & 0xff);
        ret = (ret << 8 ) | (buffer[ offset + 1 ] & 0xff);
        ret = (ret << 8 ) | (buffer[ offset + 0 ] & 0xff);
        return ret;
    }
    /* Read an OGG 16 bit int */
    private int read16Bits( byte[] buffer, int offset ) {
        int ret = 0;
        ret = (ret << 8 ) | (buffer[ offset + 1 ] & 0xff);
        ret = (ret << 8 ) | (buffer[ offset + 0 ] & 0xff);
        return ret;
    }

    private void vorbis_unpack_info( byte[] data, int offset, int length ) {
        int version         = readInt( data, offset );
        channels            = data[ offset + 4 ] & 0xff;
        int rate            = readInt( data, offset + 5 );
        int bitrate_upper   = readInt( data, offset + 9 );
        int bitrate_nominal = readInt( data, offset + 13 );
        int bitrate_lower   = readInt( data, offset + 17 );
        blocksize[0]        = 1 <<  (data[ offset + 21 ]     & 0xf);
        blocksize[1]        = 1 << ((data[ offset + 21 ]>>4) & 0xf);
        int pad             = data[ offset + 22 ] & 1;

        if ( version != 0 ) throw new Error( "Unsupported Vorbis version" );
        if ( channels < 1 ) throw new Error( "Illegal number of channels" );
        if ( rate < 1 )     throw new Error( "Illegal rate" );
        if ( blocksize[0] < 8 || blocksize[1] < blocksize[0] ) throw new Error ( "Illegal Block Size" );
        if ( pad !=1 )      throw new Error( "Illegal pad" );

        mdct[ 0 ] = new Mdct( blocksize[0] );
        mdct[ 1 ] = new Mdct( blocksize[1] );

//        System.out.println( "Rate: " + rate );
    }

    private void vorbis_unpack_comment( byte[] data, int offset, int length ) {
        int vendorLength = readInt( data, offset );
        String vendor = new String( data, offset + 4, vendorLength );
//        System.out.println( "Vendor: " + vendor );
        offset += 4 + vendorLength;

        String[] comments = new String[ readInt( data, offset ) ];
        offset += 4;
        for ( int i = 0; i < comments.length; i++ ) {
            int stringLength = readInt( data, offset );
            comments[ i ] = new String( data, offset + 4, stringLength );
            offset += 4 + stringLength;
//            System.out.println( "Comment:  " + comments[ i ] );
        }
        int pad = data[ offset ] & 1;

        if ( pad != 1 ) throw new Error ( "Illegal pad" );
    }

    private int modebits;
    private void vorbis_unpack_books( byte[] data, int offset, int length ) {
        oggRead.setData( data, offset );

        /* Extract codebooks */
        int books = (int)oggRead.getBits(8) + 1;
        codeBooks = new CodeBook[ books ];
        for ( int i = 0; i < books; i++ ) {
            codeBooks[ i ] = new CodeBook();
            codeBooks[ i ].unpack( oggRead );
        }

        /* Parse times */
        int times = (int)oggRead.getBits(6) + 1;
        for( int i = 0; i < times; i++ ) {
            oggRead.getBits(16);
        }

        /* Floors */
        int floors = (int)oggRead.getBits(6) + 1;
        floor_param = new Floor[ floors ];
        for ( int i = 0; i < floors; i++ ) {
            int floor_type = (int)oggRead.getBits(16);
//    System.out.println( "floortype " + floor_type );
            switch( floor_type ) {
                case 0: {
                    /* floor0_exportbundle */
                    floor_param[ i ] = new Floor0();
                    break;
                }
                case 1: {
                    /* floor1_exportbundle */
                    floor_param[ i ] = new Floor1();
                    break;
                }
                default: {
                    throw new Error( "Unrecognised Floor" );
                }
            }
            floor_param[ i ].unpack( oggRead );
        }

        /* Residues */
        int residues = (int)oggRead.getBits(6) + 1;
        residue_param = new Residue[ residues ];
        for ( int i = 0; i < residues; i++ ) {
            int residue_type = (int)oggRead.getBits( 16 );
//    System.out.println( "residuetype " + residue_type );
            switch ( residue_type ) {
                case 0: {
                    /* residue0_exportbundle */
                    residue_param[ i ] = new Residue0();
                    break;
                }
                case 1: {
                    /* residue1_exportbundle */
                    residue_param[ i ] = new Residue1();
                    break;
                }
                case 2: {
                    /* residue2_exportbundle */
                    residue_param[ i ] = new Residue2();
                    break;
                }
                default: {
                    throw new Error( "Unrecognised Residue" );
                }
            }
            residue_param[ i ].unpack( oggRead );
        }

        /* Mapping */
        int mappings = (int)oggRead.getBits(6) + 1;
        mapping_param = new Mapping[ residues ];
        for ( int i = 0; i < mappings; i++ ) {
            int mapping_type = (int)oggRead.getBits( 16 );
//    System.out.println( "mappingtype " + mapping_type );
            switch ( mapping_type ) {
                case 0: {
                    /* mapping0_exportbundle */
                    mapping_param[ i ] = new Mapping0( this );
                    break;
                }
                default: {
                    throw new Error( "Unrecognised Mapping" );
                }
            }
            mapping_param[ i ].unpack( oggRead, channels );
        }

        /* Modes */
        int modes = (int)oggRead.getBits(6) + 1;
        modebits = ilog2( modes - 1 );
        modes_param = new Mode[ residues ];
        for ( int i = 0; i < modes; i++ ) {
//    System.out.println( "Mode" );
            modes_param[ i ] = new Mode();
            modes_param[ i ].unpack( oggRead );
        }

        /* Padding */
        if ( oggRead.getBits( 1 ) != 1 ) throw new Error ( "Padding error" );


        /* Decode Books */
        for ( int i = 0; i < books; i++ ) {
            codeBooks[ i ].initDecode();
        }

        /* Initialize Floors and Residues */
        for ( int i = 0; i < floor_param.length; i++ ) {
            floor_param[ i ].look();
        }

        for ( int i = 0; i < residue_param.length; i++ ) {
            residue_param[ i ].look( this );
        }
    }

    /* Retrieve blockflag */
    public boolean getW() {
        return W;
    }

    public int getlW() {
        return lW;
    }

    public int getnW() {
        return nW;
    }

    /* Retrieve blockSize */
    public  int getBlockSize( int i ) {
        return blocksize[i];
    }

    /* Block management */
    private boolean W;
    private int lW;
    private int nW;


    private void vorbis_synthesis( byte[] data, int offset, int length, Buffer output ) {
        oggRead.setData( data, offset );

        /* Check the packet type */
        if( oggRead.getBits(1) != 0 ) {
            return;
	    //throw new Error( "Not Audio" );
        }

        /* read our mode and pre/post windowsize */
        int mode = (int)oggRead.getBits( modebits );
//	System.out.println( "Mode " + modebits + " " + mode );
        W = modes_param[ mode ].getBlockFlag();
        lW = 0;
        nW = 0;
        if ( W ) {
            lW = (int)oggRead.getBits(1);
            nW = (int)oggRead.getBits(1);
//    System.out.println( "lnW " + nW + " " + lW );
        }

        // TODO handle Packet headers, etc.

        /* unpack_header enforces range checking */
        int type = modes_param[ mode ].getMapping();
        mapping_param[ type ].inverse( oggRead, this );
        mapping_param[ type ].vorbis_synthesis_blockin( this );
        mapping_param[ type ].soundOutput( output );
    }
    
    private static final int HEADER_INFO    = 1;
    private static final int HEADER_COMMENT = 3;
    private static final int HEADER_BOOKS   = 5;

    private void decodeSegment( byte[] data, int offset, int length, Buffer output ) {
//        System.out.println( "Decoding segment" + headersRequired);
        if ( headersRequired != 0 ) {
            int packType = data[ offset ];
            if (    data[ offset + 1 ] != 'v'
                 || data[ offset + 2 ] != 'o'
                 || data[ offset + 3 ] != 'r'
                 || data[ offset + 4 ] != 'b'
                 || data[ offset + 5 ] != 'i'
                 || data[ offset + 6 ] != 's' ) {
//                throw new Error( "Not a VORBIS header" );
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
                    vorbis_unpack_comment( data, offset, length );
                    break;
                }
                case HEADER_BOOKS: {
                    vorbis_unpack_books( data, offset, length);
                    break;
                }
                default: {
                    throw new Error( "Invalid header ID: " + packType );
                }
            }
            headersRequired--;
            return;
        }
        
        vorbis_synthesis( data, offset, length, output );
    }

    private byte[] packetBuffer = new byte[ 0 ];
    private int packetBufferLength = 0;

    public int process( Buffer input, Buffer output ) {
        output.setFlags( input.getFlags() );
        output.setTimeStamp( input.getTimeStamp() );
        output.setDuration( input.getDuration() );
        output.setLength( 0 );
        
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
//		System.out.println( "Segment length " + packetBufferLength );
                decodeSegment( packetBuffer, 0, packetBufferLength, output );
                dataPointer += length;
                packetBufferLength = 0;
            }
//	    System.out.println( "End of packet" );
        } catch ( Exception e ) {
            System.out.println( e );
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
        } catch( Error e ) {
            System.out.println( e );
            e.printStackTrace();
        }
        return BUFFER_PROCESSED_OK;
    }
        
    
    public void open() {
    }
    
    public void close() {
    }
    
    public void reset() {
    }
    
    public String getName() {
        return "Vorbis";
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
