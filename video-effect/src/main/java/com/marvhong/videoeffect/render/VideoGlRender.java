package com.marvhong.videoeffect.render;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import com.marvhong.videoeffect.IVideoSurface;
import com.marvhong.videoeffect.filter.base.GlFilter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author LLhon
 * @Project diaoyur_android
 * @Package com.marvhong.videoeffect.render
 * @Date 2018/8/13 17:49
 * @description GlSurfaceView渲染器
 */

public class VideoGlRender implements Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = VideoGlRender.class.getSimpleName();

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;

    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;

    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private final String mVertexShader = "uniform mat4 uMVPMatrix;\n"
        + "uniform mat4 uSTMatrix;\n"
        + "attribute vec4 aPosition;\n"
        + "attribute vec4 aTextureCoord;\n"
        + "varying vec2 vTextureCoord;\n"
        + "void main() {\n"
        + "  gl_Position = uMVPMatrix * aPosition;\n"
        + "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n"
        + "}\n";

    private final float[] mTriangleVerticesData = {
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0, 0.f, 0.f,
        1.0f, -1.0f, 0, 1.f, 0.f,
        -1.0f, 1.0f, 0, 0.f, 1.f,
        1.0f, 1.0f, 0, 1.f, 1.f,
    };

    private GlFilter mFilter;

    private IVideoSurface mVideoSurface;

    private SurfaceTexture mSurfaceTexture = null;

    private FloatBuffer mTriangleVertices;

    protected boolean mChangeProgram = false;

    protected boolean mChangeProgramSupportError = false;

    protected float[] mMVPMatrix = new float[16];

    protected float[] mSTMatrix = new float[16];

    private int uMatrixHandle;

    private int mProgram;

    private int mTextureID[] = new int[2];

    private int muMVPMatrixHandle;

    private int muSTMatrixHandle;

    private int maPositionHandle;

    private int maTextureHandle;

    private boolean mUpdateSurface = false;

    protected int mCurrentViewWidth = 0;

    protected int mCurrentViewHeight = 0;

    protected int mCurrentVideoWidth = 0;

    protected int mCurrentVideoHeight = 0;

    protected Handler mHandler = new Handler();

    public VideoGlRender(final GlFilter filter, IVideoSurface videoSurface) {
        mFilter = filter;
        mVideoSurface = videoSurface;

        mTriangleVertices = ByteBuffer
            .allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    public void setFilter(final GlFilter filter) {
        final GlFilter oldFilter = mFilter;
        mFilter = filter;
        if (oldFilter != null) {
            oldFilter.destroy();
        }
        mChangeProgram = true;
        mChangeProgramSupportError = true;
    }

    public void sendSurfaceForPlayer(final Surface surface) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mVideoSurface != null) {
                    mVideoSurface.onCreated(mSurfaceTexture);
                }
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        setupSurface();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    protected void setupSurface() {
        mProgram = createProgram(mFilter.getVertexShader(), mFilter.getFragmentShader());
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException(
                "Could not get attrib location for aPosition");
        }

        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException(
                "Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException(
                "Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException(
                "Could not get attrib location for uSTMatrix");
        }

        GLES20.glGenTextures(2, mTextureID, 0);

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID[0]);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        mSurfaceTexture = new SurfaceTexture(mTextureID[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        Surface surface = new Surface(mSurfaceTexture);
        sendSurfaceForPlayer(surface);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (mUpdateSurface) {
                mSurfaceTexture.updateTexImage(); //获取新数据
                mSurfaceTexture.getTransformMatrix(mSTMatrix); //让新的纹理和纹理坐标系能够正确的对应
                mUpdateSurface = false;
            }
        }
        initDrawFrame();

        bindDrawFrameTexture();

        initPointerAndDraw();

        GLES20.glFinish();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mUpdateSurface = true;
    }

    protected void initDrawFrame() {
        if (mChangeProgram) {
            mProgram = createProgram(mFilter.getVertexShader(), mFilter.getFragmentShader());
            mChangeProgram = false;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT
            | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
    }

    protected void bindDrawFrameTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID[0]);
    }

    protected void initPointerAndDraw() {
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
            false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT,
            false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        if (mFilter != null) {
            mFilter.onDraw();
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");

    }

    protected int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS,
                compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    protected int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
            fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS,
                linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    protected void checkGlError(final String op) {
        final int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
    }

    public GlFilter getFilter() {
        return mFilter;
    }
}
