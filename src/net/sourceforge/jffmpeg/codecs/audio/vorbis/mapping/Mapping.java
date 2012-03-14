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

package net.sourceforge.jffmpeg.codecs.audio.vorbis.mapping;

import net.sourceforge.jffmpeg.codecs.audio.vorbis.VorbisDecoder;
import net.sourceforge.jffmpeg.codecs.audio.vorbis.OggReader;

import javax.media.Buffer;

public abstract class Mapping {
    public abstract void unpack( OggReader oggRead, int channels );
//    public abstract void forward();
    public abstract void inverse( OggReader oggRead, VorbisDecoder vorbis );
    public abstract void soundOutput( Buffer buffer );
    public abstract void vorbis_synthesis_blockin( VorbisDecoder vorbis );
}
