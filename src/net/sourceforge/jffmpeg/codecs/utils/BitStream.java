/*
 * Bitstream reader.
 * Copyright (c) 2003 Jonathan Hueber.
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
 */
package net.sourceforge.jffmpeg.codecs.utils;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * This class provides functionality to read bits 
 * from a byte array.  The basic functionality is 
 * provided by the methods getBits/showBits and getVLC.
 */
public class BitStream {
    public static final int KEEP_HISTORIC_BITS = 512 * 8;
    
    /**
     * Byte data with pointers to bytes and bits
     */
    private byte[] data = new byte[ 2048 ];
    private int bitIndex;
    private int sizeInBits;

    /**
     * Set frame data - pass in data to read
     */
    public final void setData( byte[] data, int dataLength ) {
        this.data = data;
        bitIndex = 0;
        sizeInBits = dataLength * 8;
    }

    public void addData( byte[] extraData, int start, int length ) {
        if ( getPos() > KEEP_HISTORIC_BITS ) {
            binData( (getPos() - KEEP_HISTORIC_BITS)/8 );
        }
        if ( (data.length - (sizeInBits/8)) < length + 8 ) {
            byte[] newArray = new byte[ ( data.length + length ) * 2 ];
            System.arraycopy( data, 0, newArray, 0, sizeInBits/8 );
            System.arraycopy( extraData, start, newArray, sizeInBits/8, length );
            data = newArray;
        } else {
            System.arraycopy( extraData, start, data, sizeInBits/8, length );
        }
        sizeInBits += length * 8;
    }
    
    public final byte[] getDataArray() {
        return data;
    }
    
    public final void binData( int numberOfBytes ) {
        System.arraycopy( data, numberOfBytes, data, 0, data.length - numberOfBytes );
        sizeInBits -= numberOfBytes * 8;
        bitIndex -= numberOfBytes * 8;
    }
    
    public void seek( int bitNumber ) {
        bitIndex = bitNumber;
    }

    /**
     * The number of bits remaining in this stream
     */
    public final int availableBits() {
        return sizeInBits - bitIndex;
    }
    
    public final int getPos() {
        return bitIndex;
    }
    
    /**
     * Return a single bit as a true or false
     *   array access + 2 shift + 2 and + inc + comparison
     */
    public final boolean getTrueFalse() {
        return ((data[ bitIndex >> 3 ] << ( bitIndex++ & 0x07 )) & 0x80) != 0;
    }

    /**
     * Read up to 24 bits - note must handle read 0 bits == 0
     */
    public final int getBits( int numberOfBits ) {
        int numberOfBitsToGo = numberOfBits;
        
        /* Temporary byte pointer for speed */
        int byteIndex = bitIndex >> 3;
        
        /* Read the number of bits in byte 0 */
        int value = data[ byteIndex++ ];
        numberOfBitsToGo -= (8 - (bitIndex & 0x07));
        
        /* Until we have enough, read bytes */
        for ( ; numberOfBitsToGo > 0; numberOfBitsToGo -= 8 ) {
            value = (value << 8)|(data[ byteIndex++ ] & 0xff);
        }
        /* number Of bits <=0 "too many" + mask */
        value = (value >>  -numberOfBitsToGo ) & ( (1 << numberOfBits) - 1);

        /* skip data */
        bitIndex += numberOfBits;
        if (availableBits() < 0 ) throw new Error( "Buffer underflow" );
        return value;
    } 

    /**
     * Read up to 24 bits
     */
    public final int showBits( int numberOfBits ) {
        int numberOfBitsToGo = numberOfBits;
        
        /* Temporary byte pointer for speed */
        int byteIndex = bitIndex >> 3;
        
        /* Read the number of bits in byte 0 */
        int value = data[ byteIndex++ ];
        numberOfBitsToGo -= (8 - (bitIndex & 0x07));
        
        /* Until we have enough, read bytes */
        for ( ; numberOfBitsToGo > 0; numberOfBitsToGo -= 8 ) {
            value = (value << 8)|(data[ byteIndex++ ] & 0xff);
        }
        /* number Of bits <=0 "too many" + mask */
        value = (value >>  -numberOfBitsToGo ) & ( (1 << numberOfBits) - 1);

        return value;
    } 

    /**
     * Show 12 bits.  
     * This value is chosen as the optimal read value
     */
    public final int show12Bits() {
        if ( availableBits() < 12 ) {
            if (availableBits() < 0 ) throw new Error( "Buffer underflow" );
            return showBits( availableBits() ) << ( 12 - availableBits() );
        }
        
        /* Temporary byte pointer for speed */
        int byteIndex = bitIndex >> 3;
        int bitNumber = (bitIndex & 0x07);
        
        /* Read the number of bits in byte 0 */
        int value = (data[ byteIndex++ ] << 16)|(data[ byteIndex++ ] & 0xff)<<8|(data[ byteIndex++ ] & 0xff);
        
        /* shift right + mask */
        return (value >> (24 - 12 - bitNumber) ) & 0xfff;
    } 
    
    /**
     * Read 0,1,2 for 0 / 10 / 11
     */
    public final int decode012() {
        return getTrueFalse() ? (getTrueFalse() ? 2 : 1) : 0;
    }    

    /**
     * Read a variable length code
     */
    public final int getVLC( VLCTable table ) throws FFMpegException {
        int code = show12Bits();
        int length = table.getCodeLength( code );
        if ( length > 0 ) {
            bitIndex += length;
            if (availableBits() < 0 ) throw new FFMpegException( "Buffer underflow" );
            return table.getCodeValue( code );
        } else if ( length < 0 ) {
            bitIndex += 12;
            return getVLC( table.getNextLevel( code ) );
        }
        if (availableBits() < 12 ) throw new FFMpegException( "Buffer underflow" );
        throw new FFMpegException( "Illegal VLC code " + table );
    }
}
