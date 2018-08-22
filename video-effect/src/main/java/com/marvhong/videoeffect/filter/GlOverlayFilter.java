package com.marvhong.videoeffect.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.marvhong.videoeffect.Resolution;
import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * 重叠
 * Created by sudamasayuki on 2018/01/07.
 */

public abstract class GlOverlayFilter extends GlFilter implements IResolutionFilter {

    private int[] textures = new int[1];

    private Bitmap bitmap = null;

    protected Resolution inputResolution = new Resolution(720, 1280);

    public GlOverlayFilter() {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public final static String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform lowp sampler2D oTexture;\n" +
                    "void main() {\n" +
                    "     lowp vec4 c2 = texture2D(sTexture, vTextureCoord);\n" +
                    "     lowp vec4 c1 = texture2D(oTexture, vTextureCoord);\n" +
                    "     \n" +
                    "     lowp vec4 outputColor;\n" +
                    "     \n" +
                    "     outputColor.r = c1.r + c2.r * c2.a * (1.0 - c1.a);\n" +
                    "\n" +
                    "     outputColor.g = c1.g + c2.g * c2.a * (1.0 - c1.a);\n" +
                    "     \n" +
                    "     outputColor.b = c1.b + c2.b * c2.a * (1.0 - c1.a);\n" +
                    "     \n" +
                    "     outputColor.a = c1.a + c2.a * (1.0 - c1.a);\n" +
                    "     \n" +
                    "     gl_FragColor = outputColor;\n" +
                    "}\n";


    @Override
    public void setResolution(Resolution resolution) {
        this.inputResolution = resolution;
    }


    private void createBitmap() {
        if (bitmap == null || bitmap.getWidth() != inputResolution.width() || bitmap.getHeight() != inputResolution.height()) {
            // BitmapUtil.releaseBitmap(bitmap);
            bitmap = Bitmap.createBitmap(inputResolution.width(), inputResolution.height(), Bitmap.Config.ARGB_8888);
        }
    }

    @Override
    public void setUpSurface() {
        super.setUpSurface();// 1
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        getHandle("oTexture");
        createBitmap();
    }

    @Override
    public void onDraw() {
        createBitmap();

        bitmap.eraseColor(Color.argb(0, 0, 0, 0));
        Canvas bitmapCanvas = new Canvas(bitmap);
        drawCanvas(bitmapCanvas);

        int offsetDepthMapTextureUniform = getHandle("oTexture");// 3

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        if (bitmap != null && !bitmap.isRecycled()) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);
        }

        GLES20.glUniform1i(offsetDepthMapTextureUniform, 3);
    }

    protected abstract void drawCanvas(Canvas canvas);

}
