package com.marvhong.videoeffect.composer;


import android.media.MediaCodec;
import android.os.Build;
import java.nio.ByteBuffer;

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/compat/MediaCodecBufferCompatWrapper.java

/**
 * A Wrapper to MediaCodec that facilitates the use of API-dependent get{Input/Output}Buffer methods,
 * in order to prevent: http://stackoverflow.com/q/30646885
 */

class MediaCodecBufferCompatWrapper {
    private final MediaCodec mediaCodec;
    private final ByteBuffer[] inputBuffers;
    private final ByteBuffer[] putputBuffers;

    MediaCodecBufferCompatWrapper(MediaCodec mediaCodec) {
        this.mediaCodec = mediaCodec;

        if (Build.VERSION.SDK_INT < 21) {
            inputBuffers = mediaCodec.getInputBuffers();
            putputBuffers = mediaCodec.getOutputBuffers();
        } else {
            inputBuffers = putputBuffers = null;
        }
    }

    ByteBuffer getInputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mediaCodec.getInputBuffer(index);
        }
        return inputBuffers[index];
    }

    ByteBuffer getOutputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mediaCodec.getOutputBuffer(index);
        }
        return putputBuffers[index];
    }

}
