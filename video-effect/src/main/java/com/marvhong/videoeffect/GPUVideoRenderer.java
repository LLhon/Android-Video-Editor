/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marvhong.videoeffect;


import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import com.marvhong.videoeffect.filter.base.GPUVideoFilter;
import com.marvhong.videoeffect.utils.TextureRotationUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@TargetApi(11)
public class GPUVideoRenderer implements Renderer,SurfaceTexture.OnFrameAvailableListener {

    private final float[] vertexData = {
        1f, -1f, 0f,
        -1f, -1f, 0f,
        1f, 1f, 0f,
        -1f, 1f, 0f
    };
    private final float[] textureVertexData = {
        1f, 0f,
        0f, 0f,
        1f, 1f,
        0f, 1f
    };

    private static final String TAG = GPUVideoRenderer.class.getSimpleName();
    public static final int NO_VIDEO = -1;
    private GPUVideoFilter mFilter;
    private int mGLTextureId = NO_VIDEO;
    private SurfaceTexture mSurfaceTexture = null;
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGlTextureVertexBuffer;
    private IntBuffer mGLRgbBuffer;
    private boolean mUpdateSurface = false;

    private int mOutputWidth;
    private int mOutputHeight;

    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    private float mBackgroundRed = 0;
    private float mBackgroundGreen = 0;
    private float mBackgroundBlue = 0;

    private IVideoSurface mVideoSurface;

    private final float[] mStMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];

    public GPUVideoRenderer(final GPUVideoFilter filter, IVideoSurface videoSurface) {
        mFilter = filter;
        mRunOnDraw = new LinkedList<>();
        mRunOnDrawEnd = new LinkedList<>();

        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);

        mGlTextureVertexBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TextureRotationUtil.TEXTURE_NO_ROTATION);

        mVideoSurface = videoSurface;
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        mGLTextureId = initTex();
        mSurfaceTexture = new SurfaceTexture(mGLTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this); //监听是否有新的一帧数据到来
        mFilter.init();

        if(mVideoSurface != null) {
            mVideoSurface.onCreated(mSurfaceTexture);
        }

        Matrix.setIdentityM(mStMatrix, 0);
    }

    public int initTex() {
        final int[] tex = new int[1];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return tex[0];
    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUseProgram(mFilter.getProgram());
        mFilter.onOutputSizeChanged(width, height);

    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Matrix.setIdentityM(mMVPMatrix, 0);

        runAll(mRunOnDraw);
        mFilter.onDraw(mGLTextureId, mGLCubeBuffer, mGlTextureVertexBuffer);
//        mFilter.draw(mSurfaceTexture, mStMatrix, mMVPMatrix);
        runAll(mRunOnDrawEnd);

        synchronized (this) {
            if (mUpdateSurface) {
                mSurfaceTexture.updateTexImage();
                //让新的纹理和纹理坐标系能够正确的对应，mSTMatrix的定义是和projectionMatrix完全一样的
                mSurfaceTexture.getTransformMatrix(mStMatrix);
                mUpdateSurface = false;
            }
        }
    }

    /**
     * Sets the background color
     *
     * @param red red color value
     * @param green green color value
     * @param blue red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        mBackgroundRed = red;
        mBackgroundGreen = green;
        mBackgroundBlue = blue;
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }


    public void setFilter(final GPUVideoFilter filter) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                final GPUVideoFilter oldFilter = mFilter;
                mFilter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mFilter.init();
                GLES20.glUseProgram(mFilter.getProgram());
                mFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    public GPUVideoFilter getFilter(){
        return mFilter;
    }


    protected int getFrameWidth() {
        return mOutputWidth;
    }

    protected int getFrameHeight() {
        return mOutputHeight;
    }



    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "new frame available");
        mUpdateSurface = true;
    }
}
