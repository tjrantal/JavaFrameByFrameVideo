package net.sourceforge.jffmpeg;

import javax.media.Format;
import javax.media.Buffer;
import javax.media.ResourceUnavailableException;

import java.awt.Dimension;

public interface JMFCodec {
    /* These methods abstract the input format */
    public boolean isCodecAvailable();
    public void setVideoSize( Dimension size );
    public void setEncoding( String encoding );
    public void setIsRtp( boolean isRtp );
    public void setIsTruncated( boolean isTruncated );
    public Format setOutputFormat( Format outputFormat );
    public int process( Buffer in, Buffer out );
    public void open() throws ResourceUnavailableException;
    public void close();
    public void reset();
}
