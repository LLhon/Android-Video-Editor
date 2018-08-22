package com.marvhong.videoeffect.composer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.IOException;

// Refer:  https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/AudioTrackTranscoder.java

/**
 * Created by sudamasayuki2 on 2018/02/22.
 */

class RemixAudioComposer implements IAudioComposer {
    private static final MuxRender.SampleType SAMPLE_TYPE = MuxRender.SampleType.AUDIO;

    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaExtractor extractor;
    private final MuxRender muxer;
    private long writtenPresentationTimeUs;

    private final int trackIndex;
    private int muxCount = 1;

    private final MediaFormat outputFormat;

    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec decoder;
    private MediaCodec encoder;
    private MediaFormat actualOutputFormat;

    private MediaCodecBufferCompatWrapper decoderBuffers;
    private MediaCodecBufferCompatWrapper encoderBuffers;

    private boolean isExtractorEOS;
    private boolean isDecoderEOS;
    private boolean isEncoderEOS;
    private boolean decoderStarted;
    private boolean encoderStarted;

    private AudioChannel audioChannel;
    private final int timeScale;

    public RemixAudioComposer(MediaExtractor extractor, int trackIndex,
                              MediaFormat outputFormat, MuxRender muxer, int timeScale) {
        this.extractor = extractor;
        this.trackIndex = trackIndex;
        this.outputFormat = outputFormat;
        this.muxer = muxer;
        this.timeScale = timeScale;
    }

    @Override
    public void setup() {
        extractor.selectTrack(trackIndex);
        try {
            encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        encoderStarted = true;
        encoderBuffers = new MediaCodecBufferCompatWrapper(encoder);

        final MediaFormat inputFormat = extractor.getTrackFormat(trackIndex);
        try {
            decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        decoderStarted = true;
        decoderBuffers = new MediaCodecBufferCompatWrapper(decoder);

        audioChannel = new AudioChannel(decoder, encoder, outputFormat);
    }

    @Override
    public boolean stepPipeline() {
        boolean busy = false;

        int status;
        while (drainEncoder(0) != DRAIN_STATE_NONE) busy = true;
        do {
            status = drainDecoder(0);
            if (status != DRAIN_STATE_NONE) busy = true;
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);

        while (audioChannel.feedEncoder(0)) busy = true;
        while (drainExtractor(0) != DRAIN_STATE_NONE) busy = true;

        return busy;
    }

    private int drainExtractor(long timeoutUs) {
        if (isExtractorEOS) return DRAIN_STATE_NONE;
        int trackIndex = extractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != this.trackIndex) {
            return DRAIN_STATE_NONE;
        }

        final int result = decoder.dequeueInputBuffer(timeoutUs);
        if (result < 0) return DRAIN_STATE_NONE;
        if (trackIndex < 0) {
            isExtractorEOS = true;
            decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }

        final int sampleSize = extractor.readSampleData(decoderBuffers.getInputBuffer(result), 0);
        final boolean isKeyFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        decoder.queueInputBuffer(result, 0, sampleSize, extractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        extractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    private int drainDecoder(long timeoutUs) {
        if (isDecoderEOS) return DRAIN_STATE_NONE;

        int result = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                audioChannel.setActualDecodedFormat(decoder.getOutputFormat());
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isDecoderEOS = true;
            audioChannel.drainDecoderBufferAndQueue(AudioChannel.BUFFER_INDEX_END_OF_STREAM, 0);
        } else if (bufferInfo.size > 0) {
            audioChannel.drainDecoderBufferAndQueue(result, bufferInfo.presentationTimeUs / timeScale);
        }

        return DRAIN_STATE_CONSUMED;
    }

    private int drainEncoder(long timeoutUs) {
        if (isEncoderEOS) return DRAIN_STATE_NONE;

        int result = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                if (actualOutputFormat != null) {
                    throw new RuntimeException("Audio output format changed twice.");
                }
                actualOutputFormat = encoder.getOutputFormat();
                muxer.setOutputFormat(SAMPLE_TYPE, actualOutputFormat);
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                encoderBuffers = new MediaCodecBufferCompatWrapper(encoder);
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if (actualOutputFormat == null) {
            throw new RuntimeException("Could not determine actual output format.");
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isEncoderEOS = true;
            bufferInfo.set(0, 0, 0, bufferInfo.flags);
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
            encoder.releaseOutputBuffer(result, false);
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if (muxCount == 1) {
            muxer.writeSampleData(SAMPLE_TYPE, encoderBuffers.getOutputBuffer(result), bufferInfo);
        }
        if (muxCount < timeScale) {
            muxCount++;
        } else {
            muxCount = 1;
        }

        writtenPresentationTimeUs = bufferInfo.presentationTimeUs;
        encoder.releaseOutputBuffer(result, false);
        return DRAIN_STATE_CONSUMED;
    }


    @Override
    public long getWrittenPresentationTimeUs() {
        return writtenPresentationTimeUs;
    }

    @Override
    public boolean isFinished() {
        return isEncoderEOS;
    }

    @Override
    public void release() {
        if (decoder != null) {
            if (decoderStarted) decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (encoder != null) {
            if (encoderStarted) encoder.stop();
            encoder.release();
            encoder = null;
        }
    }


}
