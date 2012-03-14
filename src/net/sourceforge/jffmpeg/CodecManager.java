package net.sourceforge.jffmpeg;

import java.util.ResourceBundle;

import java.util.StringTokenizer;

import javax.media.format.VideoFormat;
import javax.media.format.AudioFormat;
import javax.media.Format;

import net.sourceforge.jffmpeg.ffmpegnative.NativeDecoder;

/**
 * Manage the available Codecs.
 *
 * These are loaded from a .properties file.
 */
public class CodecManager {
    /**
     * Resource bundle with configuration information
     */
    private ResourceBundle resources;

    /**
     * Location of Jffmpeg resource file
     */
    private static final String JFFMPEG_RESOURCE = "net.sourceforge.jffmpeg.Jffmpeg";

    /**
     * List of supported video formats
     */
    private static final String SUPPORTED_VIDEO_FORMATS = "SupportedVideoFormats";

    /**
     * List of supported video formats
     */
    private static final String SUPPORTED_AUDIO_FORMATS = "SupportedAudioFormats";

    /**
     * Override native library name
     */
    private static final String LIBRARY_NAME = "FFMpegNativeLibrary";
    private static final String SYS_LIBRARY_NAME = "net.sourceforge.jffmpeg.FFMpegNativeLibrary";


    /**
     * Load the CodecManager class
     */
    public CodecManager() {
        resources = ResourceBundle.getBundle( JFFMPEG_RESOURCE );
    }

    /**
     * Read the native library name.
     *  This can be overriden using a system property.
     */
    public String getNativeLibraryName() {
        String libraryName = null;
        try {
            libraryName = System.getProperty( SYS_LIBRARY_NAME );
        } catch ( Exception e ) {
            /* SecurityException */
        }
 
        try {
            if ( libraryName == null || libraryName.length() == 0 ) {
                libraryName = resources.getString( LIBRARY_NAME );
            }
        } catch ( Exception e ) {
            /* ResourceNotFoundException */
        }
 
        return libraryName;
    }
     
    /**
     * Return list of supported VideoFormats
     */
    public VideoFormat[] getSupportedVideoFormats() {
        String formats = resources.getString( SUPPORTED_VIDEO_FORMATS );
        StringTokenizer tokenizer = new StringTokenizer( formats, "," );

        VideoFormat[] videoFormats = new VideoFormat[ tokenizer.countTokens() ];
        int i = 0;
        while ( tokenizer.hasMoreTokens() ) {
            videoFormats[ i++ ] = new VideoFormat( tokenizer.nextToken() );
        }
        return videoFormats;
    }

    /**
     * Return list of supported VideoFormats
     */
    public AudioFormat[] getSupportedAudioFormats() {
        String formats = resources.getString( SUPPORTED_AUDIO_FORMATS );
        StringTokenizer tokenizer = new StringTokenizer( formats, "," );

        AudioFormat[] audioFormats = new AudioFormat[ tokenizer.countTokens() ];
        int i = 0;
        while ( tokenizer.hasMoreTokens() ) {
            audioFormats[ i++ ] = new AudioFormat( tokenizer.nextToken() );
        }
        return audioFormats;
    }

    /**
     * Return the Codec information for this encoding
     */
    public JffmpegVideoFormat getVideoCodec( String encoding ) {
        return new JffmpegVideoFormat( encoding, resources );
    }

    /**
     * Return the Codec information for this encoding
     */
    public JffmpegAudioFormat getAudioCodec( String encoding ) {
        return new JffmpegAudioFormat( encoding, resources );
    }

    /**
     * Store if the native codec is available
     */
    private static volatile int isNativeLoaded = -1;
    public synchronized static boolean isNativeAvailable() {
        if ( isNativeLoaded == -1 ) {
            try {
                NativeDecoder d = new NativeDecoder();
                isNativeLoaded = d.isCodecAvailable() ? 1 : 0;
            } catch ( Throwable e ) {
                isNativeLoaded = 0;
            }
        }
        return (isNativeLoaded > 0);
    }        
}

/**
 * This class is used to describe an individual 
 * Video codec.  All parameters are loaded from
 * a resource bundle.
 */
class JffmpegVideoFormat extends VideoFormat {
    private static final String RTP_STRING = ".RTP";
    private static final String NATIVE_STRING = ".Native";
    private static final String CLASS_STRING = ".Class";
    private static final String JAVA_CLASS_STRING = ".JavaClass";
    private static final String FFMPEG_NAME = ".FFMpegName";
    private static final String TRUNCATE_STRING = ".IsTruncated";

    private String name;
    ResourceBundle configuration;

    /**
     * Construct a Video codec description
     */
    public JffmpegVideoFormat( String name, ResourceBundle configuration ) {
        super( name );
        this.name = name;
        this.configuration = configuration;
    }

    /**
     * Returns true if this is an RTP format
     */
    public boolean isRtp() {
        return "true".equalsIgnoreCase( configuration.getString( name + RTP_STRING) );
    }

    /**
     * Returns true if this Codec is managed by the JNI layer
     */ 
    public boolean isNative() {
        return CodecManager.isNativeAvailable() && "true".equalsIgnoreCase( configuration.getString( name + NATIVE_STRING) );
    }

    /**
     * Return the class set to handle this codec
     */
    public String getCodecClass() {
        return configuration.getString( name + (isNative() ? CLASS_STRING : JAVA_CLASS_STRING) );
    }

    /**
     * Returns the identifier used by FFMpeg for this Codec
     */
    public String getFFMpegCodecName() {
        return configuration.getString( name + FFMPEG_NAME );
    }

    public boolean isTruncated() {
        return "true".equalsIgnoreCase( configuration.getString( name + TRUNCATE_STRING) );
    }
}

/**
 * This class is used to describe an individual 
 * Audio codec.  All parameters are loaded from
 * a resource bundle.
 */
class JffmpegAudioFormat extends AudioFormat {
    private static final String NATIVE_STRING = ".Native";
    private static final String CLASS_STRING = ".Class";
    private static final String JAVA_CLASS_STRING = ".JavaClass";
    private static final String FFMPEG_NAME = ".FFMpegName";

    private String name;
    ResourceBundle configuration;

    /**
     * Construct a Video codec description
     */
    public JffmpegAudioFormat( String name, ResourceBundle configuration ) {
        super( name );
        this.name = name;
        this.configuration = configuration;
    }

    /**
     * Returns true if this Codec is managed by the JNI layer
     */ 
    public boolean isNative() {
        return CodecManager.isNativeAvailable() && "true".equalsIgnoreCase( configuration.getString( name + NATIVE_STRING) );
    }

    /**
     * Return the class set to handle this codec
     */
    public String getCodecClass() {
        return configuration.getString( name + (isNative() ? CLASS_STRING : JAVA_CLASS_STRING) );
    }

    /**
     * Returns the identifier used by FFMpeg for this Codec
     */
    public String getFFMpegCodecName() {
        return configuration.getString( name + FFMPEG_NAME );
    }
}
