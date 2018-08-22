package com.marvhong.videoeffect.filter.base;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import com.marvhong.videoeffect.utils.OpenGlUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by sudamasayuki on 2017/11/14.
 */

public class GlFilter {

    private static final String TAG = GlFilter.class.getSimpleName();
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private final float[] triangleVerticesData = {
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0, 0.f, 0.f,
        1.0f, -1.0f, 0, 1.f, 0.f,
        -1.0f, 1.0f, 0, 0.f, 1.f,
        1.0f, 1.0f, 0, 1.f, 1.f,
    };
    private FloatBuffer triangleVertices;

    private String vertexShaderSource;

    private String fragmentShaderSource;

    private int program;

    private int textureID = -12345;

    protected float[] clearColor = new float[]{0f, 0f, 0f, 1f};

    private final HashMap<String, Integer> handleMap = new HashMap<>();

    protected int mOutputWidth;

    protected int mOutputHeight;

    private final LinkedList<Runnable> mRunOnDraw;

    private boolean mChangeProgram = false;

    public GlFilter() {
        this(OpenGlUtils.DEFAULT_VERTEX_SHADER, OpenGlUtils.DEFAULT_FRAGMENT_SHADER);
    }

    public GlFilter(final Resources res, final int vertexShaderSourceResId, final int fragmentShaderSourceResId) {
        this(res.getString(vertexShaderSourceResId), res.getString(fragmentShaderSourceResId));
    }

    public GlFilter(final String vertexShaderSource, final String fragmentShaderSource) {
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
        this.mRunOnDraw = new LinkedList<>();

        triangleVertices = ByteBuffer.allocateDirect(
                triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);
    }

    public void setUpSurface() {
        final int vertexShader = OpenGlUtils
            .loadShader(vertexShaderSource, GLES20.GL_VERTEX_SHADER);
        final int fragmentShader = OpenGlUtils
            .loadShader(fragmentShaderSource, GLES20.GL_FRAGMENT_SHADER);
        program = OpenGlUtils.createProgram(vertexShader, fragmentShader);
        if (program == 0) {
            throw new RuntimeException("failed creating program");
        }

        getHandle("aPosition");
        getHandle("aTextureCoord");
        getHandle("uMVPMatrix");
        getHandle("uSTMatrix");

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        OpenGlUtils.checkGlError("glBindTexture textureID");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE);
        OpenGlUtils.checkGlError("glTexParameter");
    }

    public int getTextureId() {
        return textureID;
    }

    public int getProgram() {
        return program;
    }

    public void draw(SurfaceTexture surfaceTexture, float[] STMatrix, float[] MVPMatrix) {
        OpenGlUtils.checkGlError("onDrawFrame start");

        if (mChangeProgram) {
            final int vertexShader = OpenGlUtils
                .loadShader(vertexShaderSource, GLES20.GL_VERTEX_SHADER);
            final int fragmentShader = OpenGlUtils
                .loadShader(fragmentShaderSource, GLES20.GL_FRAGMENT_SHADER);
            program = OpenGlUtils.createProgram(vertexShader, fragmentShader);
            mChangeProgram = false;
            Log.e(TAG, "change---program:" + program);
        }

        GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);
        OpenGlUtils.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(getHandle("aPosition"), 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"));

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(getHandle("aTextureCoord"), 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        OpenGlUtils.checkGlError("glVertexAttribPointer aTextureHandle");
        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"));
        OpenGlUtils.checkGlError("glEnableVertexAttribArray aTextureHandle");

        surfaceTexture.getTransformMatrix(STMatrix);

        GLES20.glUniformMatrix4fv(getHandle("uMVPMatrix"), 1, false, MVPMatrix, 0);
        GLES20.glUniformMatrix4fv(getHandle("uSTMatrix"), 1, false, STMatrix, 0);

        onDraw();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        OpenGlUtils.checkGlError("glDrawArrays");

        GLES20.glFinish();
    }

    public void onDraw() {
    }

    public final int getHandle(final String name) {
        final Integer value = handleMap.get(name);
        if (value != null) {
            return value;
        }

        int location = GLES20.glGetAttribLocation(program, name);
        if (location == -1) {
            location = GLES20.glGetUniformLocation(program, name);
        }
        if (location == -1) {
            throw new IllegalStateException("Could not get attrib or uniform location for " + name);
        }
        handleMap.put(name, location);
        return location;
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected void onDrawArraysPre() {
    }

    public String getVertexShader() {
        return vertexShaderSource;
    }

    public String getFragmentShader() {
        return fragmentShaderSource;
    }

    public void setChangeProgram(boolean changeProgram) {
        mChangeProgram = changeProgram;
    }

    public boolean isChangeProgram() {
        return mChangeProgram;
    }

    public void release() {
    }

    public void setClearColor(float red,
                              float green,
                              float blue,
                              float alpha) {
        this.clearColor = new float[]{red, green, blue, alpha};
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    public void setTriangleVertices(FloatBuffer value) {
        triangleVertices = value;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public final void destroy() {
        GLES20.glDeleteProgram(program);
        onDestroy();
    }

    public void onDestroy() {

    }
}
