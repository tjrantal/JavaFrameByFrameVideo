/*
 * BitStream variable length code management table.
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

/**
 * This class manages the Variable length codes
 */
public class VLCTable {
    /**
     * Constructor
     */
    protected VLCTable() {
    }
    
    /**
     * This is the list of vlcCodes in {value, bitlength} pairs
     */
    protected long[][] vlcCodes;

    /**
     * This are the fast lookup tables (0-0x1ff ranges)
     */
    protected int[] codeLength     = new int[ 0x1000 ];
    protected int[] codeValue      = new int[ 0x1000 ];
    protected VLCTable[] nextLevel = new VLCTable[ 0x1000 ];
    
    /**
     * Getters for highspeed table
     */
    public final int getCodeLength( int i ) {
        return codeLength[ i ];
    }

    /**
     * Getters for highspeed table
     */
    public final int getCodeValue( int i ) {
        return codeValue[ i ];
    }
    
    /**
     * Getters for highspeed table
     */
    public final VLCTable getNextLevel( int i ) {
        return nextLevel[ i ];
    }

    
    /**
     * Create indexed lookup tables
     */
    protected void createHighSpeedTable() {
        for ( int value = 0; value < vlcCodes.length; value++ ) {
            int code   = (int)vlcCodes[value][0];
            int length = (int)vlcCodes[value][1];
            if ( length == 0 ) continue;       // Skip this value

            writeTable( value, code, length );
        }
    }

    /**
     * Write this value into the index table
     */
    protected void writeTable( int value, int code, int length ) {
        if (length < 13) {
            int top12Bits      =  code << (12 - length);
            int range         = ((code + 1) << (12 - length));
            for ( int j = top12Bits; j < range; j++ ) {
                codeLength[ j ] = length;
                codeValue[ j ]  = value;
            }
        } else {
            int top12Bits      = code >> (length - 12);
            int remainingBits = code & ~(0xffff << (length - 12));
            
            codeLength[ top12Bits ] = -1;
            if (nextLevel[ top12Bits ] == null) nextLevel[ top12Bits ] = new VLCTable();
            nextLevel[ top12Bits ].writeTable( value, remainingBits, length - 12 );
        }
    }
    
    /**
     * Returns decoded VLC or -1 if there is match
     * Throws an exception if the numberOfBits is too great
     */
    public int decode( int value, int numberOfBits ) throws FFMpegException {
        boolean exception = true;
        for (int i = 0; i < vlcCodes.length; i++ ) {
            exception &= (vlcCodes[i][1] < numberOfBits);

            if ( vlcCodes[i][1] == numberOfBits && vlcCodes[i][0] == value ) {
                return i;
            }
        }
        if (exception) throw new FFMpegException( "Illegal VLC code" );
        return -1;
    }
}
