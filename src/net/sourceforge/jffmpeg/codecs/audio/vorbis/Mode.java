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

import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;

public class Mode {
    private int blockflag;
    private int windowtype;
    private int transformtype;
    private int mapping;

    public void unpack( OggReader oggRead ) {
        blockflag     = (int)oggRead.getBits( 1 );
        windowtype    = (int)oggRead.getBits( 16 );
        transformtype = (int)oggRead.getBits( 16 );
        mapping       = (int)oggRead.getBits( 8 );
    }

    public boolean getBlockFlag() {
        return blockflag != 0;
    }

    public int getMapping() {
        return mapping;
    }

//    public abstract void look();
//    public abstract void inverse1();
//    public abstract void inverse2();
}
