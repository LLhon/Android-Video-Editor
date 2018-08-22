package com.marvhong.videoeffect.filter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * Created by sudamasayuki on 2018/03/12.
 */

public class GlLutFilter extends GlFilter {

    private int hTex;
    private final int NO_TEXTURE = -1;
    private Bitmap lutTexture;

    private final static String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform mediump sampler2D lutTexture; \n" +
                    "uniform samplerExternalOES sTexture; \n" +
                    "varying vec2 vTextureCoord; \n" +
                    "vec4 sampleAs3DTexture(vec3 uv) {\n" +
                    "    float width = 16.;\n" +
                    "    float sliceSize = 1.0 / width;\n" +
                    "    float slicePixelSize = sliceSize / width;\n" +
                    "    float sliceInnerSize = slicePixelSize * (width - 1.0);\n" +
                    "    float zSlice0 = min(floor(uv.z * width), width - 1.0);\n" +
                    "    float zSlice1 = min(zSlice0 + 1.0, width - 1.0);\n" +
                    "    float xOffset = slicePixelSize * 0.5 + uv.x * sliceInnerSize;\n" +
                    "    float s0 = xOffset + (zSlice0 * sliceSize);\n" +
                    "    float s1 = xOffset + (zSlice1 * sliceSize);\n" +
                    "    vec4 slice0Color = texture2D(lutTexture, vec2(s0, uv.y));\n" +
                    "    vec4 slice1Color = texture2D(lutTexture, vec2(s1, uv.y));\n" +
                    "    float zOffset = mod(uv.z * width, 1.0);\n" +
                    "    vec4 result = mix(slice0Color, slice1Color, zOffset);\n" +
                    "    return result;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "   vec4 pixel = texture2D(sTexture, vTextureCoord);\n" +
                    "   vec4 gradedPixel = sampleAs3DTexture(pixel.rgb);\n" +
                    "   gradedPixel.a = pixel.a;\n" +
                    "   pixel = gradedPixel;\n" +
                    "   gl_FragColor = pixel;\n " +
                    "}";

    public GlLutFilter(Bitmap bitmap) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        this.lutTexture = bitmap;
        hTex = NO_TEXTURE;
    }


    public GlLutFilter(Resources resources, int fxID) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        this.lutTexture = BitmapFactory.decodeResource(resources, fxID);
        hTex = NO_TEXTURE;
    }


    @Override
    public void setUpSurface() {
        super.setUpSurface();
        if (hTex == NO_TEXTURE) {
            hTex = OpenGlUtils.loadTexture(lutTexture, NO_TEXTURE, false);
        }

    }

    @Override
    public void onDraw() {
        int offsetDepthMapTextureUniform = getHandle("lutTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTex);
        GLES20.glUniform1i(offsetDepthMapTextureUniform, 3);
    }


}
