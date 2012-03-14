/*
 * Copyright (c) 2002 Francisco Javier Cabello
 * Copyright (c) 2004 Guilhem Tardy (www.salyens.com)
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
 */
/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *
 * $Id: NativeDecoder.java,v 1.1 2004/11/01 01:52:15 davidstuart Exp $
 *
 * Description
 * ============
 * This is a Decoder which takes H263 frames, or H263_RTP(rfc2190) packets
 * and generates either YUV420P or RGB output. This will eventually
 * also support other input format such as MPEG.
 *++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
/*
 * Revision 1.2  2004/02/19 Guilhem Tardy (gravsten@yahoo.com)
 * Major rewrite, now fully supports H.263/RTP (ffmpeg 0.4.7) and conversion to RGB24.
 */

package net.sourceforge.jffmpeg.ffmpegnative;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;

import java.awt.Dimension;

import com.sun.media.BasicCodec;

import net.sourceforge.jffmpeg.JMFCodec;
import net.sourceforge.jffmpeg.CodecManager;

/**
 * A Codec to convert YUVFormat buffer to RGBFormat buffer.
 */
public class NativeDecoder implements JMFCodec {
    private boolean opened;
    private Format[] inputFormats;
    private Format[] outputFormats;
    private Format inputFormat;
    private Format outputFormat;


    private final static String PLUGIN_NAME = "FFMPEG Decoder";

    private final static int DEF_WIDTH = 352;
    private final static int DEF_HEIGHT = 288;

    private final static int INPUT_BUFFER_PADDING_SIZE = 8;

    private int inputH263Length;
    private int outputLength;

    private final static int MAX_PAYLOAD_SIZE = 2048;


    private String encoding;
    /**
     * Video size
     */
    private Dimension videoSize;

    // true if some parameter was changed and a reset is required,
    // false otherwise.
    protected boolean resetRequired = false;

    // If RTP is enabled, some portions of the decoding need to be
    // treated differently. Set this variable to true if we detect
    // an RTP protocol has been selected.
    private boolean rtpActive = false;

    /**
     * Do we need to provide the "Set Truncated" flag to FFMPEG?
     */
    private boolean truncatedFlag = false;

    // Sequence number used for RTP. Gaps between the sequence numbers
    // of incoming buffer indicate a packet loss or reordering.
    private long seqNum = 0;

    // Frame number
    private long frameNum = 0;

    // Timestamp of the last incoming buffer, used in case of packet loss or reordering.
    private long timestamp = 0;

    private float frameRate = 0;

    // Variables used for YUV420P to RGB24 conversion
    private boolean yuv2rgb = false;
    private int depth;
    private int rMask, gMask, bMask;

    //--------------------------------------------------------------------------------
    // NATIVE METHODS
    //--------------------------------------------------------------------------------

    private native boolean open_decoder(String codec, boolean rtp, boolean setTruncted, boolean yuv2rgb, int depth, int rMask, int gMask, int bMask, int width, int height);
    private native boolean close_decoder(int peer);

    // returns the number of bytes consumed, or negative if error
    private native int convert(int peer,
                               Object inData, int inBufSize, int inOffset, int inLength,
                               Object outData, int outLength, int eof);

    private native float extractFrameRate( int peer );

    //--------------------------------------------------------------------------------
    // NATIVE VARIABLES
    //--------------------------------------------------------------------------------

    // Native structure pointer
    public int peer = 0;

    /* Set to true if/when the native library is loaded */
    private static boolean nativeLibraryLoaded = false;
    static {
        CodecManager codecManager = new CodecManager();
        String libraryName = codecManager.getNativeLibraryName();
        if ( libraryName == null ) libraryName = "jffmpeg";
        try {
            System.loadLibrary( libraryName );
            nativeLibraryLoaded = true;
        }
        catch (UnsatisfiedLinkError e) {
            nativeLibraryLoaded = false;
        }
    }

    /**
     * Returns false if the native library failed to load
     */
    public boolean isCodecAvailable() {
        return nativeLibraryLoaded;
    }

    // Constructor
    public NativeDecoder() {
        // Specify the input formats. In this list we can be "general". The
        // framework will narrow down the options by itself and by use of the
        // other methods.
        inputFormats = new Format [] {
            new VideoFormat(VideoFormat.H263),
            new VideoFormat(VideoFormat.H263_RTP),
            new VideoFormat(VideoFormat.MPEG),
            new VideoFormat("mpeg video"),
            new VideoFormat("DIV3"),
            new VideoFormat("DIVX"),
            new VideoFormat("MP42"),
            new VideoFormat("MPG4"),
            new VideoFormat("WMV1"),
            new VideoFormat("WMV2"),
            new VideoFormat("MJPG"),
        };

        // Specify the list of generic output formats. This list will also be
        // "general" in nature.
        outputFormats = new Format[] {
            new YUVFormat(YUVFormat.YUV_420),
            new RGBFormat()
        };

        // Initialize parent members
        inputFormat  = null;
        outputFormat = null;
    }

    public void setYuv2rgb( boolean yuv2rgb ) {
        this.yuv2rgb = yuv2rgb;
    }

    public void setEncoding( String encoding ) {
        this.encoding = encoding;
    }

    public void setVideoSize( Dimension videoSize ) {
        this.videoSize = videoSize;
    }

    public void setIsRtp( boolean isRtp ) {
        this.rtpActive = isRtp;
    }

    public void setIsTruncated( boolean truncatedFlag ) {
        this.truncatedFlag = truncatedFlag;
    }

    /**
     * JMF calls this method when it has decided on an output format. This
     * format will be the result of the call to getSupportedOutputFormats().
     */
    public Format setOutputFormat(Format out) {

        VideoFormat videoOut = (VideoFormat)out;
        Dimension outSize = videoOut.getSize();

        // System.out.println("Decoder:: setOutputFormat(" + out.getEncoding() + ")");
        // System.out.println("\tfrom: " + videoOut.toString() + " size=" + outSize);

        if (outSize == null) {
            Dimension inSize = videoSize;
            if (inSize == null)
                outSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);
            else
                outSize = inSize;
        }

        if (out instanceof YUVFormat) {
            yuv2rgb = false;
            YUVFormat yuv = (YUVFormat) out;

            if (yuv.getYuvType() != YUVFormat.YUV_420)
                return null;

            if (yuv.getOffsetU() > yuv.getOffsetV())
                return null;

            // TODO : Make other safety checks

            int strideY  = outSize.width;
            int strideUV = strideY / 2;
            int offsetU  = strideY * outSize.height;
            int offsetV  = offsetU + strideUV * outSize.height / 2;

            outputLength = (outSize.width * outSize.height * 3) / 2;
            outputFormat = new YUVFormat(outSize,
                outputLength,
                Format.byteArray,
                videoOut.getFrameRate(),
                YUVFormat.YUV_420,
                strideY,
                strideUV,
                0,
                offsetU,
                offsetV);
        } else if (out instanceof RGBFormat) {
            yuv2rgb = true;
            RGBFormat rgb = (RGBFormat) out;

            int bitsPerPixel = rgb.getBitsPerPixel();
            Class dataType = rgb.getDataType();

            int pixelStride = 1;

            switch (bitsPerPixel) {
            case 15:
            case 16:
                if (dataType != Format.byteArray &&
                    dataType != Format.shortArray)
                    return null;
                if (dataType == Format.byteArray)
                    pixelStride = 2;
                break;
            case 24:
                if (dataType != Format.byteArray)
                    return null;
                pixelStride = 3;
                break;
            case 32:
                if (dataType != Format.byteArray &&
                    dataType != Format.intArray)
                    return null;
                if (dataType == Format.byteArray)
                    pixelStride = 4;
                break;
            default:
                return null;
            }

            // TODO : Make other safety checks

            if (dataType == Format.byteArray) {
                rMask = 0x000000ff << ((rgb.getRedMask() - 1) * 8);
                gMask = 0x000000ff << ((rgb.getGreenMask() - 1) * 8);
                bMask = 0x000000ff << ((rgb.getBlueMask() - 1) * 8);
            } else {
                rMask = rgb.getRedMask();
                gMask = rgb.getGreenMask();
                bMask = rgb.getBlueMask();
            }
            depth = bitsPerPixel;

            int bytesPerPixel = (bitsPerPixel + 7) / 8;

            outputLength = outSize.width * outSize.height * bytesPerPixel;
            outputFormat = new RGBFormat(outSize,
                outputLength,
                dataType,
                videoOut.getFrameRate(),
                bitsPerPixel,
                rgb.getRedMask(), rgb.getGreenMask(), rgb.getBlueMask(),
                pixelStride,
                outSize.width * pixelStride,
                Format.FALSE, // flipped
                Format.NOT_SPECIFIED); // endian
        } else
            return null;

// System.out.println("Decoder:: outputFormat is now: " + outputFormat.toString());

        // Return the selected outputFormat
        return outputFormat;
    }

    /**
     * Get frame rate from Codec
     */
    public float getFrameRate( int peer ) {
        if ( frameRate <= 0 ) frameRate = extractFrameRate( peer );
        return frameRate;
    }

    /**
     * This flag can be used to allow data to start
     * in one buffer and end in the next.
     */
    private boolean quirkIncompatibleBuffering = false;
    private byte[] leftOver = null;

    /**
     * The last timestamp and the relative frame number
     */
    private long lastTime = 10;
    private int frames = 0;

    public int process(Buffer inBuffer, Buffer outBuffer) {
        int result;

        /* Do we need to set the time in the output buffer? */
        outBuffer.setFlags( inBuffer.getFlags() );
        if ( (inBuffer.getFlags() & Buffer.FLAG_NO_WAIT) == 0 ) {
            /* Has the input buffer specified a new timestamp */
            if ( lastTime != inBuffer.getTimeStamp() ) {
                lastTime = inBuffer.getTimeStamp();
                frames = 0;
            } else {
                /* Calculate time depending on frame rate */
                outBuffer.setFlags( inBuffer.getFlags() | Buffer.FLAG_RELATIVE_TIME | Buffer.FLAG_NO_DROP );
            }
            float currentFrameRate = ((VideoFormat)inBuffer.getFormat()).getFrameRate();
            if ( currentFrameRate <=0 ) {
                currentFrameRate = getFrameRate(peer);
            }
            outBuffer.setTimeStamp( inBuffer.getTimeStamp() + (long)(((long)1000000000)/currentFrameRate) * frames);
        }

        if ( quirkIncompatibleBuffering && leftOver != null ) {
            byte[] data = new byte[ leftOver.length + inBuffer.getLength() ];
            System.arraycopy( leftOver, 0, data, 0, leftOver.length );
            byte[] in = (byte[])inBuffer.getData();
            System.arraycopy( in, inBuffer.getOffset(), data, leftOver.length,
                              inBuffer.getLength() );
            inBuffer.setData( data );
            inBuffer.setOffset( 0 );
            inBuffer.setLength( data.length );
            leftOver = null;
        }
        // System.out.println("#"+inBuffer.getSequenceNumber()+", timestamp="+inBuffer.getTimeStamp());
        if ((inBuffer.getFlags() & Buffer.FLAG_EOM) != 0) {
            frames = 0;
            outBuffer.setFlags( outBuffer.getFlags() | Buffer.FLAG_EOM );
            reset();
            //System.out.println("return BUFFER_PROCESSED_OK");
            return Codec.BUFFER_PROCESSED_OK;
        }

        if (inBuffer.isDiscard() ) {
            outBuffer.setDiscard(true);
            reset();
            //System.out.println("return BUFFER_PROCESSED_OK");
            return Codec.BUFFER_PROCESSED_OK;
        }

        Format inFormat = inBuffer.getFormat();
//        if (inFormat != inputFormat && !(inFormat.matches(inputFormat))) {
            // System.out.println("format on inBuffer is " + inFormat.toString() + ", updating codec format");
//            setInputFormat(inFormat);
//        }

        if (inBuffer.getLength() < 5) {
            outBuffer.setDiscard(true);
            reset();
            //System.out.println("return BUFFER_PROCESSED_OK");
            return Codec.BUFFER_PROCESSED_OK;
        }

        int inOffset = inBuffer.getOffset();
        int inLength = inBuffer.getLength();

        // The codec might read up to INPUT_BUFFER_PADDING_SIZE additional bytes
        // before checking for EOS, which can cause ArrayOutOfBounds in Java code.
        byte[] curArray = (byte[])inBuffer.getData();
        int inBufSize = curArray.length;

        Object inData = (Object)inBuffer.getData();  //getInputData(inBuffer);  //
        long inDataBytes = 0; //getNativeData(inData);

        /* TODO - check type is correct */
        int[] outData = (int[])outBuffer.getData(); //getOutputData(outBuffer); 

        if (outData == null || outData.length < outputLength ||
            outBuffer.getFormat() != outputFormat ||
            !outBuffer.getFormat().equals(outputFormat)) {
// System.out.println("Decoder:: mismatch: " + (outData == null ? "NULL" : outData.toString()) + ", " + outBuffer.toString());

            outData = new int[ outputLength ];
            outBuffer.setLength(outputLength);
            outBuffer.setFormat(outputFormat);
            outBuffer.setData( outData );
        }

//        outData = validateData(outBuffer, outputLength, true /*allow native*/);
        long outDataBytes = 0; //getNativeData(outData);

        int eof; // end of frame, used only if RTP mode

        if (rtpActive) {
            eof = inBuffer.getFlags() & Buffer.FLAG_RTP_MARKER;

            // Initialize seqNum on first use
            if (seqNum == 0)
                seqNum = inBuffer.getSequenceNumber();

            // Try to detect loss of video packets by seqNum. Skip this
            // whole business if the timestamp is not set on the input
            // buffer.
            /** @todo find out how the timestamp is set on buffer in video decoder case */
            if (seqNum != inBuffer.getSequenceNumber() &&
                inBuffer.getTimeStamp() > 0 && timestamp > 0 ) {

// System.out.println("Decoder:: packet loss, #" + seqNum + " ->#" + inBuffer.getSequenceNumber() + (timestamp != inBuffer.getTimeStamp() ? ", across frames (try to recover)" : ", within a single frame"));

                if (timestamp != inBuffer.getTimeStamp()) {
                    // Try to recover last received frame
                    result = convert(peer, inData, 0, 0, 0, outData, outputLength, 0);
                    if ( result > 0 ) {
//                        outBuffer.setTimeStamp(timestamp);
                        outBuffer.setOffset(0);
                        outBuffer.setLength(outputLength);
                        frameNum++; // increment number of frames decoded
                        // System.out.println("return INPUT_BUFFER_NOT_CONSUMED");
                        seqNum = inBuffer.getSequenceNumber(); // prevent packet loss code from being triggered a second time
                        return Codec.INPUT_BUFFER_NOT_CONSUMED;
                    }
                }
            }

            // Update seqNum to the values from inBuffer
            timestamp = inBuffer.getTimeStamp();
            seqNum = inBuffer.getSequenceNumber();
        } else {
            eof = 1;
        }

        result = convert(peer, inData, inBufSize, inOffset, inLength, 
                         outData, outputLength, eof);

        if ( result > 0 ) {
            inBuffer.setOffset( inOffset + result );
            inBuffer.setLength( inLength - result );
	}

        if ( result < 0 ) {
            //System.out.println("return BUFFER_PROCESSED_FAILED");
            seqNum++;
            return Codec.BUFFER_PROCESSED_FAILED;
        }

        /** Not enough data */
        if ( result == 0 ) {
//            System.out.println("Not enough data " + inLength );
            if ( quirkIncompatibleBuffering ) {
                leftOver = new byte[ inLength ];
                System.arraycopy( inData, inOffset, leftOver, 0, inLength );
	    }
            frames++;
            return Codec.BUFFER_PROCESSED_OK;
        }

        if (eof == 0) {
// System.out.println("Decoder:: packet #" + seqNum + " (" + inLength + " bytes)");

            seqNum++;

            outBuffer.setDiscard(true);
            //System.out.println("return BUFFER_PROCESSED_OK");
            frames++;
            return Codec.BUFFER_PROCESSED_OK;
        }

// System.out.println("Decoder:: packet #" + seqNum + " (" + inLength + " bytes), end of frame #" + frameNum);

        // outBuffer.setTimeStamp(inBuffer.getTimeStamp());
        outBuffer.setOffset(0);
        outBuffer.setLength(outputLength);

        seqNum++;
        frameNum++; // increment number of frames decoded

        // At this point, the input and output frames buffers are done
        if (resetRequired && opened) reset();
        
        frames++;
        if ( inBuffer.getLength() > 0 ) {
            return Codec.INPUT_BUFFER_NOT_CONSUMED;
        }

        //System.out.println("return BUFFER_PROCESSED_OK");
        return Codec.BUFFER_PROCESSED_OK;
    }

    public synchronized void open() throws ResourceUnavailableException {
        if (!opened) {
// System.out.println("Decoder:: open()");

            synchronized (getClass()) {
//                if (inputFormat == null)
//                    throw new ResourceUnavailableException("No input format selected");
                if (outputFormat == null)
                    throw new ResourceUnavailableException("No output format selected");
		Dimension size = videoSize;
                if (!open_decoder(encoding, rtpActive, truncatedFlag, yuv2rgb, depth, rMask, gMask, bMask, 
  (int)size.getWidth(), (int)size.getHeight()))
                    throw new ResourceUnavailableException("Couldn't open codec for " + encoding);
            }

            resetRequired = false;
        }
    }

    public synchronized void close() {
        if (opened) {
// System.out.println("Decoder:: close()");
            close_decoder(peer);
        }
    }

    public synchronized void reset() {
        frames = 0;
        if (resetRequired && opened) {
// System.out.println("Decoder:: reset()");

            try {
                close();
                open();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return PLUGIN_NAME;
    }

    // Private method for encapsulating the RTP format check
    // Ideally each RTP protocol that we support should be
    // added here.
    private boolean isRTPFormat(Format format) {
        if (format.getEncoding().equals(VideoFormat.H263_RTP))
            return true;

        return false;
    }

    // private method for finding the list of matching output formats
    // based on the input format.
    public Format [] getMatchingOutputFormats(Format in) {
        VideoFormat videoIn = (VideoFormat)in;
        Dimension inSize = videoIn.getSize();

        // System.out.println("Decoder:: getMatchingOutputFormats(" + in.getEncoding() + ", size:" + inSize + ", fps:" + videoIn.getFrameRate() + ")");

        int strideY  = inSize.width;
        int strideUV = strideY / 2;
        int offsetU  = strideY * inSize.height;
        int offsetV  = offsetU + strideUV * inSize.height / 2;

        VideoFormat[] result = new VideoFormat [] {
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
        return result;
    }

    public Format[] getSupportedOutputFormats(Format i ) {
        return null;
    }
}
