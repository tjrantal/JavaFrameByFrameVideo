package net.sourceforge.jffmpeg;

import javax.media.ResourceUnavailableException;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.AudioFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import javax.media.Buffer;

import java.awt.Dimension;

import net.sourceforge.jffmpeg.ffmpegnative.NativeDecoder;
import net.sourceforge.jffmpeg.JMFCodec;

/**
 * This class manages all ffmpeg audio codecs
 */
public class AudioDecoder implements Codec {
    private CodecManager codecManager = new CodecManager();
    private JMFCodec peer = null;

    /**
     * Retrieve the supported input formats.
     *
     * @return Format[] the supported input formats
     */
    public Format[] getSupportedInputFormats() {
        return codecManager.getSupportedAudioFormats();
    }
    
    /**
     * Retrieve the supported output formats.  Currently RGBVideo
     * for Java codecs.
     *
     * @return Format[] the supported output formats
     */
    public Format[] getSupportedOutputFormats(Format format) {
        /* Sanity check */
        if ( format == null || !(format instanceof AudioFormat ) ) {
            return new Format[ 0 ];
        }
        AudioFormat inputFormat = (AudioFormat)format;

        /* Get corresponding Jffmpeg Format */
        JffmpegAudioFormat audioCodec = codecManager.getAudioCodec( format.getEncoding() );
        return new AudioFormat[] { 
            new AudioFormat( "LINEAR", inputFormat.getSampleRate(),
                             inputFormat.getSampleSizeInBits() > 0 ? 
                                  inputFormat.getSampleSizeInBits() : 16,
                             inputFormat.getChannels(),
                             0, 1) // endian, int signed
        };
    }
            
    /**
     * Negotiate the format for the input data.  
     *
     * @return Format the negotiated input format
     */
    public Format setInputFormat( Format format ) {
        JffmpegAudioFormat audioCodec = codecManager.getAudioCodec( format.getEncoding() );
        if ( audioCodec == null ) return null;

        /* Construct Codec */
        try {
            peer = (JMFCodec)Class.forName( audioCodec.getCodecClass() ).newInstance();
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

        peer.setEncoding( audioCodec.getFFMpegCodecName() );
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
        AudioFormat inputFormat = (AudioFormat)format;
        return new AudioFormat( "LINEAR", inputFormat.getSampleRate(),
                             inputFormat.getSampleSizeInBits() > 0 ? 
                                  inputFormat.getSampleSizeInBits() : 16,
                             inputFormat.getChannels(),
                             0, 1); // endian, int signed
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
     * Retrives the name of this video codec: "JFFMPEG audio decoder"
     * @return Codec name
     */
    public String getName() {
        return "JFFMPEG audio decoder";
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
