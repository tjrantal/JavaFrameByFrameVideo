package net.sourceforge.jffmpeg;

import javax.media.ResourceUnavailableException;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import javax.media.Buffer;

import java.awt.Dimension;

import net.sourceforge.jffmpeg.ffmpegnative.NativeDecoder;
import net.sourceforge.jffmpeg.JMFCodec;

/**
 * This class manages all ffmpeg native video codecs
 */
public class VideoDecoder implements Codec {
    private CodecManager codecManager = new CodecManager();
    private JMFCodec peer = null;

    /**
     * Retrieve the supported input formats.
     *
     * @return Format[] the supported input formats
     */
    public Format[] getSupportedInputFormats() {
        return codecManager.getSupportedVideoFormats();
    }
    
    /**
     * Retrieve the supported output formats.  Currently RGBVideo
     * for Java codecs.
     *
     * @return Format[] the supported output formats
     */
    public Format[] getSupportedOutputFormats(Format format) {
        /* Sanity check */
        if ( format == null ) return new Format[ 0 ];

        /* Get corresponding Jffmpeg Format */
        JffmpegVideoFormat videoCodec = codecManager.getVideoCodec( format.getEncoding() );
        if ( format instanceof VideoFormat && videoCodec.isNative() ) {
            /* Video format */
             VideoFormat videoIn = (VideoFormat)format;
             Dimension inSize = videoIn.getSize();

             // System.out.println("Decoder:: getMatchingOutputFormats(" + in.getEncoding() + ", size:" + inSize + ", fps:" + videoIn.getFrameRate() + ")");

             int strideY  = inSize.width;
             int strideUV = strideY / 2;
             int offsetU  = strideY * inSize.height;
             int offsetV  = offsetU + strideUV * inSize.height / 2;

            return new VideoFormat [] {
                new YUVFormat(inSize,
                    (strideY + strideUV) * inSize.height,
                    Format.byteArray,
                    videoIn.getFrameRate(),
                    YUVFormat.YUV_420,
                    strideY,
                    strideUV,
                    0,
                    offsetU,
                    offsetV),
                new RGBFormat(inSize,
                    inSize.width * inSize.height,
                    Format.shortArray,
                    videoIn.getFrameRate(),
                    15,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    1,
                    inSize.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height,
                    Format.shortArray,
                    videoIn.getFrameRate(),
                    16,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    1,
                    inSize.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height * 3,
                    Format.byteArray,
                    videoIn.getFrameRate(),
                    24,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    3,
                    inSize.width * 3,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height,
                    Format.intArray,
                    videoIn.getFrameRate(),
                    32,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    1,
                    inSize.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height,
                    Format.shortArray,
                    videoIn.getFrameRate(),
                    16,
                    0x7c00, 0x3e0, 0x1f,
                    1,
                    inSize.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height,
                    Format.shortArray,
                    videoIn.getFrameRate(),
                    16,
                    0xf800, 0x3e0, 0x1f,
                    1,
                    inSize.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height * 3,
                    Format.byteArray,
                    videoIn.getFrameRate(),
                    24,
                    3, 2, 1,
                    3,
                    inSize.width * 3,
                    Format.FALSE,
                    Format.NOT_SPECIFIED),
                new RGBFormat(inSize,
                    inSize.width * inSize.height,
                    Format.intArray,
                    videoIn.getFrameRate(),
                    32,
                    0xff0000, 0x00ff00, 0x0000ff,
                    1,
                    inSize.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED)
            };
        } else if ( format instanceof VideoFormat && !videoCodec.isNative() ) {
            /* Java video codec */
            return new Format[] { new RGBFormat() };
        } else {
            /* Audio format */
            return new Format[ 0 ];
        }
    }
            
    /**
     * Negotiate the format for the input data.  
     *
     * Only the width and height entries are used
     *
     * @return Format the negotiated input format
     */
    public Format setInputFormat( Format format ) {
        JffmpegVideoFormat videoCodec = codecManager.getVideoCodec( format.getEncoding() );
        if ( videoCodec == null ) return null;

        Dimension videoSize = ((VideoFormat)format).getSize();
        if ( videoSize == null ) return null;

        /* Construct Codec */
        try {
            peer = (JMFCodec)Class.forName( videoCodec.getCodecClass() ).newInstance();
            if ( !peer.isCodecAvailable() ) return null;
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch ( InstantiationException e ) {
            e.printStackTrace();
            return null;
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
            return null;
        }

        peer.setVideoSize( videoSize );
        peer.setEncoding( videoCodec.getFFMpegCodecName() );
        peer.setIsRtp( videoCodec.isRtp() );
        peer.setIsTruncated( videoCodec.isTruncated() );
        
        return format;
    }
    
    /**
     * Negotiate the format for screen display renderer.  
     *
     * Only the frame rate entry is used.  All the other 
     * values are populated using the negotiated input formnat.
     *
     * @return Format RGBFormat to supply to display renderer.
     */
    public Format setOutputFormat( Format format ) {
        if ( peer == null ) throw new IllegalArgumentException( "Must set Input Format first" );
        return peer.setOutputFormat( format );
    }
    
    /**
     * Convert data using this codec
     *
     * @return BUFFER_PROCESSED_OK The output buffer contains a valid frame
     * @return BUFFER_PROCESSED_FAILED A decoding problem was encountered
     */
    public int process( Buffer in, Buffer out ) {
        return peer.process( in, out );
    }
    
    /**
     * Initialise the video codec for use.
     */
    public void open() throws ResourceUnavailableException {
        peer.open();
    }
    
    /**
     * Deallocate resources, and shutdown.
     */
    public void close() {
        peer.close();
    }
    
    /**
     * Reset the internal state of the video codec.
     */
    public void reset() {
        peer.reset();
    }
    
    /**
     * Retrives the name of this video codec: "FFMPEG video decoder"
     * @return Codec name
     */
    public String getName() {
        return "FFMPEG video decoder";
    }
    
    /**
     * This method returns the interfaces that can be used
     * to control this codec.  Currently no interfaces are defined.
     */
    public Object[] getControls() {
        return null; //peer.getControls();
    }
    
    /**
     * This method returns an interface that can be used
     * to control this codec.  Currently no interfaces are defined.
     */
    public Object getControl( String type ) {
        return null; //peer.getControl( type );
    }
}
