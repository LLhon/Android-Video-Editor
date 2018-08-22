package com.marvhong.videoeffect.composer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/QueuedMuxer.java

class MuxRender {
    private static final String TAG = "MuxRender";
    private static final int BUFFER_SIZE = 64 * 1024; // I have no idea whether this value is appropriate or not...
    private final MediaMuxer muxer;
    private MediaFormat videoFormat;
    private MediaFormat audioFormat;
    private int videoTrackIndex;
    private int audioTrackIndex;
    private ByteBuffer byteBuffer;
    private final List<SampleInfo> sampleInfoList;
    private boolean started;

    MuxRender(MediaMuxer muxer) {
        this.muxer = muxer;
        sampleInfoList = new ArrayList<>();
    }

    void setOutputFormat(SampleType sampleType, MediaFormat format) {
        switch (sampleType) {
            case VIDEO:
                videoFormat = format;
                break;
            case AUDIO:
                audioFormat = format;
                break;
            default:
                throw new AssertionError();
        }
    }

    void onSetOutputFormat() {

        if (videoFormat != null && audioFormat != null) {

            videoTrackIndex = muxer.addTrack(videoFormat);
            Log.v(TAG, "Added track #" + videoTrackIndex + " with " + videoFormat.getString(
                MediaFormat.KEY_MIME) + " to muxer");
            audioTrackIndex = muxer.addTrack(audioFormat);
            Log.v(TAG, "Added track #" + audioTrackIndex + " with " + audioFormat.getString(
                MediaFormat.KEY_MIME) + " to muxer");

        } else if (videoFormat != null) {

            videoTrackIndex = muxer.addTrack(videoFormat);
            Log.v(TAG, "Added track #" + videoTrackIndex + " with " + videoFormat.getString(
                MediaFormat.KEY_MIME) + " to muxer");

        }

        muxer.start();
        started = true;

        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(0);
        }
        byteBuffer.flip();
        Log.v(TAG, "Output format determined, writing " + sampleInfoList.size() +
                " samples / " + byteBuffer.limit() + " bytes to muxer.");
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int offset = 0;
        for (SampleInfo sampleInfo : sampleInfoList) {
            sampleInfo.writeToBufferInfo(bufferInfo, offset);
            muxer.writeSampleData(getTrackIndexForSampleType(sampleInfo.sampleType), byteBuffer, bufferInfo);
            offset += sampleInfo.size;
        }
        sampleInfoList.clear();
        byteBuffer = null;


    }

    void writeSampleData(SampleType sampleType, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (started) {
            muxer.writeSampleData(getTrackIndexForSampleType(sampleType), byteBuf, bufferInfo);
            return;
        }
        byteBuf.limit(bufferInfo.offset + bufferInfo.size);
        byteBuf.position(bufferInfo.offset);
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
        }
        byteBuffer.put(byteBuf);
        sampleInfoList.add(new SampleInfo(sampleType, bufferInfo.size, bufferInfo));
    }

    private int getTrackIndexForSampleType(SampleType sampleType) {
        switch (sampleType) {
            case VIDEO:
                return videoTrackIndex;
            case AUDIO:
                return audioTrackIndex;
            default:
                throw new AssertionError();
        }
    }

    public enum SampleType {VIDEO, AUDIO}

    private static class SampleInfo {
        private final SampleType sampleType;
        private final int size;
        private final long presentationTimeUs;
        private final int flags;

        private SampleInfo(SampleType sampleType, int size, MediaCodec.BufferInfo bufferInfo) {
            this.sampleType = sampleType;
            this.size = size;
            presentationTimeUs = bufferInfo.presentationTimeUs;
            flags = bufferInfo.flags;
        }

        private void writeToBufferInfo(MediaCodec.BufferInfo bufferInfo, int offset) {
            bufferInfo.set(offset, size, presentationTimeUs, flags);
        }
    }

}
