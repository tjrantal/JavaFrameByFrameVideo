package net.sourceforge.jffmpeg;

import javax.media.ResourceUnavailableException;
import javax.media.Codec;
import javax.media.Format;
import javax.media.Buffer;

import net.sourceforge.jffmpeg.ffmpegnative.NativeEncoder;

/**
 * This class manages all jffmpeg video encoders
 */
public class VideoEncoder implements Codec {
    private NativeEncoder peer = new NativeEncoder();

    /**
     * Retrieve the supported input formats. 
     *
     * @return Format[] the supported input formats
     */
    public Format[] getSupportedInputFormats() {
        return peer.getSupportedInputFormats();
    }
    
    /**
     * Retrieve the supported output formats.  Currently RGBVideo
     *
     * @return Format[] the supported output formats
     */
    public Format[] getSupportedOutputFormats(Format format) {
        return peer.getSupportedOutputFormats( format );
    }
    
    /**
     * Negotiate the format for the input data.  
     *
     * Only the width and height entries are used
     *
     * @return Format the negotiated input format
     */
    public Format setInputFormat( Format format ) {
        return peer.setInputFormat( format );
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
     * Retrives the name of this video codec: "FFMPEG video encoder"
     * @return Codec name
     */
    public String getName() {
        return "FFMPEG video encoder";
    }
    
    /**
     * This method returns the interfaces that can be used
     * to control this codec.  Currently no interfaces are defined.
     */
    public Object[] getControls() {
        return peer.getControls();
    }
    
    /**
     * This method returns an interface that can be used
     * to control this codec.  Currently no interfaces are defined.
     */
    public Object getControl( String type ) {
        return peer.getControl( type );
    }
}
