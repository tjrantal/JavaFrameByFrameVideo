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
 *
 */
/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *
 * $Id: NativeEncoder.java,v 1.1 2004/11/01 01:52:15 davidstuart Exp $
 *
 * Description
 * ============
 * This is an Encoder which takes YUV420P input and generates either
 * H263 frames, or H263_RTP(rfc2190) packets. This will eventually
 * also support the generation of MPEG frames.
 *++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
/*
 * Revision 1.2  2004/02/19 Guilhem Tardy (gravsten@yahoo.com)
 * Major rewrite, now fully supports H.263/RTP (ffmpeg 0.4.7).
 */

package net.sourceforge.jffmpeg.ffmpegnative;

import java.awt.Component;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.Control;
import javax.media.Owned;
import javax.media.TimeBase;
import javax.media.SystemTimeBase;

import java.awt.Dimension;

import com.sun.media.BasicCodec;

import net.sourceforge.jffmpeg.CodecManager;

public class NativeEncoder extends BasicCodec
{
    private final static String PLUGIN_NAME = "FFMPEG Encoder";

    private final static int DEF_WIDTH = 352;
    private final static int DEF_HEIGHT = 288;

    private final static int INPUT_BUFFER_PADDING_SIZE = 8;

    private final static Format [] defOutputFormats = {
        new VideoFormat(VideoFormat.H263),
        new VideoFormat(VideoFormat.H263_RTP)
        // new VideoFormat(VideoFormat.H263-1998),
        // new VideoFormat(VideoFormat.H263-1998_RTP)
    };

    private int inputYuvLength;
    private int outputH263Length;

    private Control [] controls = null;

    // Variables associated with the H263 control

    // Variables associated with the BitRate control
    private final static int MIN_BIT_RATE = 20000;
    private final static int MAX_BIT_RATE = 1000000;
    protected int bitRate = 128000;

    // Variables associated with the FrameProcessing control
    protected boolean goSouth = false;
    protected int framesBehind = 0;
    protected int framesDropped = 0;

    // Variables associated with the FrameRate control
    private final static float MIN_FRAME_RATE = 1.0f;
    private final static float MAX_FRAME_RATE = 30.0f;
    protected float sourceFrameRate = 0.0f;
    protected float targetFrameRate = 10.0f;
    protected int frameDecimation = 1;
    private int frames2Skip = 0;

    // Variables associated with the KeyFrame control
    protected int keyFrameInterval = 10;

    // Variables associated with the Quality control
    protected float quality = 4.0f;

    // Variables associated with the PacketSize control
    /* When coding a YUV frame into RTP packets, each input buffer
     * will usually be broken into multiple output buffers that should
     * not exceed the maximum RTP payload size (MAX_RTP_MTU).
     * The ffmpeg codec always cuts frames at the next macroblock boundary
     * after a suggested RTP payload size that we artificially set lower
     * by 128 bytes for payload headers and the last macroblock
     * (macroblocks are 16x16 pixels, ranging from 1 bit to ??? bytes).
     */
    protected final static int MIN_RTP_MTU = 320;
    protected final static int MAX_RTP_MTU = (1500-8-64-20-40-8-12);
    protected int targetPacketSize = 984;
    private final static int MAX_PAYLOAD_SIZE = 2048;

    // true if some parameter was changed and a reset is required,
    // false otherwise.
    protected boolean resetRequired = false;

    // If RTP is enabled, some portions of the encoding need to be
    // treated differently. Set this variable to true if we detect
    // an RTP protocol has been selected.
    private boolean rtpActive = false;

    // Sequence number used for RTP. For all outgoing buffers
    // (after a successful encoding), this should increase by 1.
    private long seqNum = 0;

    // Frame number
    private long frameNum = 0;

    // Constantly ticking source of time
    private TimeBase masterTimeBase = new SystemTimeBase();

    // Time of last frame completion (used to update ctx->frame_rate)
    private long lastFrameTime = 0;

    // Bit rate shaping variables
    protected boolean shapingActive = true;
    private long lastPacketTime;
    private long lastPacketBits;

    // Compatibility mode for JMF "stock" codecs
    protected boolean compatibility = false;

    // CPU Load Management
    protected boolean cpuActive = false;
    protected long targetFrameTime;

    //--------------------------------------------------------------------------------
    // NATIVE METHODS
    //--------------------------------------------------------------------------------

    // Native method prototypes
    private native boolean open_encoder(String codec, int width, int height,
                                        int bitRate, int frameRate, int keyFrameInterval,
                                        float quality, boolean dynQuality,
                                        int rtpPayloadSize, boolean compatibility);
    private native boolean close_encoder(int peer);

    protected native boolean set_frameRate(int peer, int frameRate);
    protected native boolean set_quality(int peer, float quality);
    protected native boolean set_rtpPayloadSize(int peer, int rtpPayloadSize);
    protected native boolean set_compatibility(int peer, boolean compatibility);

    // returns the size of the output buffer after encoding, or negative if error
    private native boolean convert(int peer, Object inData, long inDataBytes, int inBufSize, int inOffset, int inLength,
                                   Object outData, long outDataBytes, int outLength);

    //--------------------------------------------------------------------------------
    // NATIVE VARIABLES
    //--------------------------------------------------------------------------------

    // Native structure pointer
    public int peer = 0;

    // Set to true or false by the native code if it is finished processing
    // the input buffer.
    public boolean inputDone = true;

    // Set by the native code to the size of the output buffer
    public int outputSize = 0;

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
    public NativeEncoder() {
// System.out.println("Encoder:: Constructor");

        inputFormats = new Format [] {
            new YUVFormat(YUVFormat.YUV_420)
        };

        inputFormat  = null;
        outputFormat = null;
    }

    public void finalize() {
// System.out.println("Encoder:: finalize()");
    }

    private Format [] getMatchingOutputFormats(Format in) {
        VideoFormat videoIn = (VideoFormat)in;
        Dimension inSize = videoIn.getSize();

// System.out.println("Encoder:: getMatchingOutputFormats(" + in.getEncoding() + ", size:" + inSize + ", fps:" + videoIn.getFrameRate() + ")");

        outputFormats = new VideoFormat [] {
            new VideoFormat(
                VideoFormat.H263,
                inSize,
                Format.NOT_SPECIFIED,
                Format.byteArray,
                videoIn.getFrameRate()),
            new VideoFormat(
                VideoFormat.H263_RTP,
                inSize,
                Format.NOT_SPECIFIED,
                Format.byteArray,
                videoIn.getFrameRate())
        };

        return outputFormats;
    }

    /**
     * Return the list of formats supported at the output.
     */
    public Format [] getSupportedOutputFormats(Format in) {

        // null input format
        if (in == null)
            return defOutputFormats;

// System.out.println("Encoder:: getSupportedOutputFormats(" + in.getEncoding() + ")");

        // mismatch input format
        if (!(in instanceof VideoFormat) ||
            null == matches(in, inputFormats))
            return new Format[0];

        return getMatchingOutputFormats(in);
    }

    public Format setInputFormat(Format in) {
// System.out.println("Encoder:: setInputFormat(" + in.getEncoding() + ")");

        // mismatch input format
        if (!(in instanceof VideoFormat) ||
            null == matches(in, inputFormats))
            return null;

        VideoFormat videoIn = (VideoFormat)in;
        Dimension inSize = videoIn.getSize();

// System.out.println("Encoder:: from: " + videoIn.getClass().getName() + "::" + videoIn.toString());

        if (inSize == null)
            inSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);

        YUVFormat yuv = (YUVFormat)videoIn;

        if (yuv.getOffsetU() > yuv.getOffsetV())
            return null;

        // TODO : Make other safety checks

        int strideY  = inSize.width;
        int strideUV = strideY / 2;
        int offsetU  = strideY * inSize.height;
        int offsetV  = offsetU + strideUV * inSize.height / 2;

        inputYuvLength = (strideY + strideUV) * inSize.height;
        sourceFrameRate = videoIn.getFrameRate();
        if (targetFrameRate < sourceFrameRate)
            frameDecimation = (int)(sourceFrameRate / targetFrameRate);
        else
            frameDecimation = 1;
        inputFormat = new YUVFormat(inSize,
            inputYuvLength + INPUT_BUFFER_PADDING_SIZE,
            Format.byteArray,
            sourceFrameRate,
            YUVFormat.YUV_420,
            strideY,
            strideUV,
            0,
            offsetU,
            offsetV);

// System.out.println("Encoder:: inputFormat is now: " + inputFormat.toString());

        // Return the selected inputFormat
        return inputFormat;
    }

    public Format setOutputFormat(Format out) {
// System.out.println("Encoder:: setOutputFormat(" + out.getEncoding() + ")");

        // mismatch output format
        if (!(out instanceof VideoFormat) ||
            null == matches(out, getMatchingOutputFormats(inputFormat)))
            return null;

        VideoFormat videoOut = (VideoFormat)out;
        Dimension outSize = videoOut.getSize();

// System.out.println("Encoder:: from: " + videoOut.getClass().getName() + "::" + videoOut.toString());

        if (outSize == null) {
            Dimension inSize = ((VideoFormat)inputFormat).getSize();
            if (inSize == null)
                outSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);
            else
                outSize = inSize;
        }

        // Set rtp active boolean based on output format
        rtpActive = isRTPFormat(videoOut);

        outputH263Length = rtpActive ? MAX_PAYLOAD_SIZE : outSize.width * outSize.height;
        targetFrameRate = videoOut.getFrameRate();
        if (targetFrameRate < sourceFrameRate)
            frameDecimation = (int)(sourceFrameRate / targetFrameRate);
        else
            frameDecimation = 1;
        targetFrameTime = 1250000000 / (long)targetFrameRate; // relax by 25%
        outputFormat = new VideoFormat(videoOut.getEncoding(),
            outSize,
            outputH263Length,
            Format.byteArray,
            videoOut.getFrameRate());

// System.out.println("Encoder:: outputFormat is now: " + outputFormat.toString());

        // Return the selected outputFormat
        return outputFormat;
    }

    public int process(Buffer inBuffer, Buffer outBuffer) {
        if (inputDone) {
            if (isEOM(inBuffer)) {
                propagateEOM(outBuffer);
                reset();
                return BUFFER_PROCESSED_OK;
            }

            if (inBuffer.isDiscard()) {
                outBuffer.setDiscard(true);
                reset();
                return BUFFER_PROCESSED_OK;
            }

            frames2Skip++;
            if (frames2Skip < frameDecimation) {
// System.out.println("Encoder:: frame " + frames2Skip + "/" + frameDecimation + " skipped");

                outBuffer.setOffset(0);
                outBuffer.setLength(0);
                outBuffer.setFormat(outputFormat);
                return OUTPUT_BUFFER_NOT_FILLED;
            }
            frames2Skip=0;

            if (frameNum > 0) {
                long deltaTime = masterTimeBase.getNanoseconds() - lastFrameTime + 1;
                int frameRate = (int)(1000000000 / deltaTime);
                set_frameRate(peer, Math.max(frameRate,(int)targetFrameRate) + 1); // MPI<0 workaround (must be non-zero)
                if (cpuActive && (deltaTime > targetFrameTime)) {
// System.out.println("Encoder:: can't keep up, add " + (deltaTime / targetFrameTime) + " frames behind");

                    framesBehind += (int)(deltaTime / targetFrameTime);
                }
            }
            lastFrameTime = masterTimeBase.getNanoseconds();

            if (framesBehind > 0) {
                framesBehind--;
                framesDropped++;

// System.out.println("Encoder:: frame skipped (" + framesDropped + " total, " + framesBehind + " remaining)");

                outBuffer.setOffset(0);
                outBuffer.setLength(0);
                outBuffer.setFormat(outputFormat);
                return OUTPUT_BUFFER_NOT_FILLED;
            }

            if (goSouth) {
                framesDropped++;

// System.out.println("Encoder:: frame skipped (" + framesDropped + " total)");

                outBuffer.setOffset(0);
                outBuffer.setLength(0);
                outBuffer.setFormat(outputFormat);
                return OUTPUT_BUFFER_NOT_FILLED;
            }
        }

        Format inFormat = inBuffer.getFormat();
        if (inFormat != inputFormat && !(inFormat.matches(inputFormat))) {
            setInputFormat(inFormat);
        }

        if (inBuffer.getLength() < 10) {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        int inOffset = inBuffer.getOffset();
        int inLength = inBuffer.getLength();

        // The codec might read up to INPUT_BUFFER_PADDING_SIZE additional bytes
        // before checking for EOS, which can cause ArrayOutOfBounds in Java code.
        byte[] curArray = (byte[])inBuffer.getData();
        int inBufSize = curArray.length;

        Object inData = getInputData(inBuffer);
        long inDataBytes = getNativeData(inData);

        Object outData = getOutputData(outBuffer);

        if (outData == null ||
            outBuffer.getFormat() != outputFormat ||
            !outBuffer.getFormat().equals(outputFormat)) {
// System.out.println("Encoder:: mismatch: " + (outData == null ? "NULL" : outData.toString()) + ", " + outBuffer.toString());

            outBuffer.setLength(outputH263Length);
            outBuffer.setFormat(outputFormat);
        }

        outData = validateData(outBuffer, outputH263Length, true /*allow native*/);
        long outDataBytes = getNativeData(outData);

        // Call the native method to convert input buffer to the
        // output buffer. Note that this call can potentially set
        // some of the instance variables marked as native above.
        boolean result = convert(peer, inData, inDataBytes, inBufSize, inOffset, inLength, outData, outDataBytes, outputH263Length);

        if (!result)
            return BUFFER_PROCESSED_FAILED;

        // outBuffer.setTimeStamp(inBuffer.getTimeStamp());
        outBuffer.setOffset(0);
        outBuffer.setLength(outputSize);

        if (rtpActive)
            outBuffer.setSequenceNumber(seqNum);

        if (shapingActive) {
            long timeout = (1000000000 * lastPacketBits) / (long)bitRate;
            long deltaTime = masterTimeBase.getNanoseconds() - lastPacketTime;
            if (timeout > deltaTime) {
// System.out.println("Encoder:: waiting " + ((timeout - deltaTime)/1000000) + " ms");

                try {
                    wait(timeout - deltaTime);
                }
                catch (Throwable e) { }
            }
        }
        lastPacketTime = masterTimeBase.getNanoseconds();
        lastPacketBits = outputSize * 8;

        if (!inputDone) {
// System.out.println("Encoder:: packet #" + seqNum + " (" + outputSize + " bytes)");

            seqNum++;

            return INPUT_BUFFER_NOT_CONSUMED;
        }

// System.out.println("Encoder:: packet #" + seqNum + " (" + outputSize + " bytes), end of frame #" + frameNum);

        if (rtpActive) {
            // Set the RTP marker (last packet in a video frame).
            int flags = outBuffer.getFlags();
            outBuffer.setFlags(flags | Buffer.FLAG_RTP_MARKER);
        }

        seqNum++;
        frameNum++; // increment number of frames encoded

        // At this point, the input and output frames buffers are done
        reset();
        return BUFFER_PROCESSED_OK;
    }

    public synchronized void open() throws ResourceUnavailableException {
        if (!opened) {
// System.out.println("Encoder:: open()");

            super.open();

            if (inputFormat == null)
                throw new ResourceUnavailableException("No input format selected");
            if (outputFormat == null)
                throw new ResourceUnavailableException("No output format selected");

            Dimension size = ((VideoFormat)inputFormat).getSize();
            if (!open_encoder(outputFormat.getEncoding(), size.width, size.height,
                bitRate, (int)targetFrameRate, keyFrameInterval, quality, bitRate < MAX_BIT_RATE,
                (compatibility ? targetPacketSize : targetPacketSize - 128), compatibility))
                throw new ResourceUnavailableException("Couldn't open codec for " + inputFormat.toString());

            resetRequired = false;
        }
    }

    public synchronized void close() {
        if (opened) {
// System.out.println("Encoder:: close()");

            close_encoder(peer);

            super.close();
        }
    }

    public synchronized void reset() {
        if (resetRequired && opened) {
// System.out.println("Encoder:: reset()");

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

    public java.lang.Object [] getControls() {
        if (controls == null) {
             controls = new Control [] {
                 new H263Adapter(this, false /*ap*/, false /*ac*/, false /*ec*/, false /*pb*/, false /*uv*/, 0 /*hrdB*/, 1000 /*bppMaxKb*/, false /*settable*/),
                 new BitRateAdapter(this,bitRate,MIN_BIT_RATE,MAX_BIT_RATE,true),
                 new FrameProcessingAdapter(this),
                 new FrameRateAdapter(this,targetFrameRate,MIN_FRAME_RATE,MAX_FRAME_RATE,true),
                 new KeyFrameAdapter(this,keyFrameInterval,true),
                 new QualityAdapter(this,(31.0f - quality) / 27.0f,0.0f,1.0f,false,true),
                 new PacketSizeAdapter(this,targetPacketSize,true),
                 new BitRateShapingAdapter(this),
                 new CompatibilityAdapter(this),
                 new CpuLoadMgtAdapter(this)
             };
        }

        return (Object [])controls;
    }
}

class H263Adapter extends com.sun.media.controls.H263Adapter implements Owned {
    NativeEncoder owner;

    public H263Adapter(NativeEncoder owner, boolean advancedPrediction, boolean ArithmeticCoding,
                       boolean ErrorCompensation, boolean PBFrames, boolean UnrestrictedVector,
                       int HRD_B, int BppMaxKb, boolean settable) {
        super(owner, advancedPrediction, ArithmeticCoding, ErrorCompensation, PBFrames,
              UnrestrictedVector, HRD_B, BppMaxKb, settable);
        this.owner = owner;
    }

// FIXME, we don't yet make use of it.

    public java.lang.Object getOwner() {
        return (Object) owner;
    }
}

class BitRateAdapter extends com.sun.media.controls.BitRateAdapter implements Owned {
    NativeEncoder owner;

    public BitRateAdapter(NativeEncoder owner, int initialBitRate, int minBitRate,
                          int maxBitRate, boolean settable) {
        super(initialBitRate, minBitRate, maxBitRate, settable);
        this.owner = owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public int setBitRate(int newValue) {
        owner.bitRate = super.setBitRate(newValue);
        owner.resetRequired = true;
        return owner.bitRate;

    }
}

class FrameProcessingAdapter implements javax.media.control.FrameProcessingControl, Owned {
    NativeEncoder owner;

    public FrameProcessingAdapter(NativeEncoder owner) {
        this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public boolean setMinimalProcessing(boolean minimal) {
        owner.goSouth = minimal;
        return owner.goSouth;
    }

    public void setFramesBehind(float frames) {
        owner.framesBehind = (int)frames;
    }

    public int getFramesDropped() {
        int fd = owner.framesDropped;
        owner.framesDropped = 0;
        return fd;
    }

    public Component getControlComponent() {
        return null;
    }
}

class FrameRateAdapter extends com.sun.media.controls.FrameRateAdapter implements Owned {
    NativeEncoder owner;

    public FrameRateAdapter(NativeEncoder owner, float initialFrameRate,
                            float minFrameRate, float maxFrameRate, boolean settable) {
        super(owner, initialFrameRate, minFrameRate, maxFrameRate, settable);
        this.owner = owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public float setFrameRate(float frameRate) {
        owner.targetFrameRate = super.setFrameRate(frameRate);
        if (owner.targetFrameRate < owner.sourceFrameRate)
            owner.frameDecimation = (int)(owner.sourceFrameRate / owner.targetFrameRate);
        else
            owner.frameDecimation = 1;
        owner.targetFrameTime = 1250000000 / (long)owner.targetFrameRate; // relax by 25%
        owner.set_frameRate(owner.peer, (int)owner.targetFrameRate);
        owner.resetRequired = true;
        return owner.targetFrameRate;
    }
}

class KeyFrameAdapter extends com.sun.media.controls.KeyFrameAdapter implements Owned {
    NativeEncoder owner;

    public KeyFrameAdapter(NativeEncoder owner, int preferredInterval, boolean settable) {
        super(preferredInterval, settable);
        this.owner = owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public int setKeyFrameInterval(int newValue) {
        owner.keyFrameInterval = super.setKeyFrameInterval(newValue);
        owner.resetRequired = true;
        return owner.keyFrameInterval;
    }
}

class QualityAdapter extends com.sun.media.controls.QualityAdapter implements Owned {
    NativeEncoder owner;

    public QualityAdapter(NativeEncoder owner, float preferredQuality, float minQuality, float maxQuality,
                          boolean isTSsupported, boolean settable) {
        super(preferredQuality, minQuality, maxQuality, isTSsupported, settable);
        this.owner = owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public float setQuality(float newQuality) {
        float quality = super.setQuality(newQuality);

        // quality settings range from 0.0 (low) to 1.0 (high),
        // and must be mapped to quantiser settings from 31 (low) to 4 (high).
        owner.quality = 31.0f - (quality * 27.0f);
        owner.set_quality(owner.peer, owner.quality);
        return quality;
    }
}

class PacketSizeAdapter extends com.sun.media.controls.PacketSizeAdapter implements Owned {
    NativeEncoder owner;

    public PacketSizeAdapter(NativeEncoder owner, int packetSize, boolean settable) {
        super(owner, packetSize, settable);
        this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public int setPacketSize(int numBytes) {
        if (numBytes < owner.MIN_RTP_MTU)
            numBytes = owner.MIN_RTP_MTU;

        if (numBytes > owner.MAX_RTP_MTU)
            numBytes = owner.MAX_RTP_MTU;

        owner.targetPacketSize = super.setPacketSize(numBytes);
        owner.set_rtpPayloadSize(owner.peer, (owner.compatibility ? owner.targetPacketSize : owner.targetPacketSize - 128));
        return owner.targetPacketSize;
    }
}

class BitRateShapingAdapter implements BitRateShapingControl, Owned {
    NativeEncoder owner;

    public BitRateShapingAdapter(NativeEncoder owner) {
        this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public boolean setBitRateShaping(boolean bitRateShaping) {
        owner.shapingActive = bitRateShaping;
        return owner.shapingActive;
    }

    public Component getControlComponent() {
        return null;
    }
}

class CpuLoadMgtAdapter implements CpuLoadMgtControl, Owned {
    NativeEncoder owner;

    public CpuLoadMgtAdapter(NativeEncoder owner) {
        this.owner=owner;
    }

    public java.lang.Object getOwner() {
        return (Object) owner;
    }

    public boolean setCpuLoadMgt(boolean CpuLoadMgt) {
        owner.cpuActive = CpuLoadMgt;
        return owner.cpuActive;
    }

    public Component getControlComponent() {
        return null;
    }
}
