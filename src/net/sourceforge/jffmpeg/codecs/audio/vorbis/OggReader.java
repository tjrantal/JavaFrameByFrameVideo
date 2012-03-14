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

public class OggReader {
    byte[] data;
    int    offset;
    int    bitPointer;

    public void setData( byte[] data, int offset ) {
        this.data   = data;
        this.offset = offset;
        bitPointer = 0;
    }

    public void skipBits( int bits ) {
        offset += (bitPointer + bits)/8;
        bitPointer = (bitPointer + bits) & 7;
    }

    public long showBits( int bits ) {
        long mask = maskBits[ bits ];
        bits += bitPointer;
        long val = (data[ offset ]&0xff) >> bitPointer;
        if ( bits > 8 ) val |= (data[ offset + 1 ]&0xff)<< ( 8 - bitPointer );
        if ( bits > 16) val |= (data[ offset + 2 ]&0xff)<< ( 16- bitPointer );
        if ( bits > 24) val |= (data[ offset + 3 ]&0xff)<< ( 24- bitPointer );
        if ( bits > 32) val |= (data[ offset + 4 ]&0xff)<< ( 32- bitPointer );

        return val & mask;
    }

    public long getBits( int bits ) {
        long mask = maskBits[ bits ];
        bits += bitPointer;
        long val = (data[ offset ]&0xff) >> bitPointer;
        if ( bits > 8 ) val |= (data[ offset + 1 ]&0xff)<< ( 8 - bitPointer );
        if ( bits > 16) val |= (data[ offset + 2 ]&0xff)<< ( 16- bitPointer );
        if ( bits > 24) val |= (data[ offset + 3 ]&0xff)<< ( 24- bitPointer );
        if ( bits > 32) val |= (data[ offset + 4 ]&0xff)<< ( 32- bitPointer );

        offset += bits/8;
        bitPointer = bits & 7;
        return val & mask;
    }

    private static long[] maskBits = new long[] {
        0x00000000,0x00000001,0x00000003,0x00000007,0x0000000f,
        0x0000001f,0x0000003f,0x0000007f,0x000000ff,0x000001ff,
        0x000003ff,0x000007ff,0x00000fff,0x00001fff,0x00003fff,
        0x00007fff,0x0000ffff,0x0001ffff,0x0003ffff,0x0007ffff,
        0x000fffff,0x001fffff,0x003fffff,0x007fffff,0x00ffffff,
        0x01ffffff,0x03ffffff,0x07ffffff,0x0fffffff,0x1fffffff,
        0x3fffffff,0x7fffffff,(long)0xffffffff 
    };
}
