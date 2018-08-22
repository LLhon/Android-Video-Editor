package com.marvhong.videoeffect.filter;

import android.opengl.GLES20;
import com.marvhong.videoeffect.Resolution;
import com.marvhong.videoeffect.filter.base.GlFilter;

/**
 * 锐化
 * Created by sudamasayuki on 2018/01/07.
 */

public class GlSharpenFilter extends GlFilter implements IResolutionFilter {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +

                    "highp vec2 vTextureCoord;\n" +

                    "uniform float imageWidthFactor;" +
                    "uniform float imageHeightFactor;" +
                    "uniform float sharpness;" +

                    "varying highp vec2 textureCoordinate;" +
                    "varying highp vec2 leftTextureCoordinate;" +
                    "varying highp vec2 rightTextureCoordinate;" +
                    "varying highp vec2 topTextureCoordinate;" +
                    "varying highp vec2 bottomTextureCoordinate;" +

                    "varying float centerMultiplier;" +
                    "varying float edgeMultiplier;" +

                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +

                    "mediump vec2 widthStep = vec2(imageWidthFactor, 0.0);" +
                    "mediump vec2 heightStep = vec2(0.0, imageHeightFactor);" +

                    "textureCoordinate       = vTextureCoord.xy;" +
                    "leftTextureCoordinate   = textureCoordinate - widthStep;" +
                    "rightTextureCoordinate  = textureCoordinate + widthStep;" +
                    "topTextureCoordinate    = textureCoordinate + heightStep;" +
                    "bottomTextureCoordinate = textureCoordinate - heightStep;" +

                    "centerMultiplier = 1.0 + 4.0 * sharpness;" +
                    "edgeMultiplier = sharpness;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision highp float;" +

                    "uniform samplerExternalOES sTexture;\n" +

                    "varying highp vec2 textureCoordinate;" +
                    "varying highp vec2 leftTextureCoordinate;" +
                    "varying highp vec2 rightTextureCoordinate;" +
                    "varying highp vec2 topTextureCoordinate;" +
                    "varying highp vec2 bottomTextureCoordinate;" +

                    "varying float centerMultiplier;" +
                    "varying float edgeMultiplier;" +

                    "void main() {" +
                    "mediump vec3 textureColor       = texture2D(sTexture, textureCoordinate).rgb;" +
                    "mediump vec3 leftTextureColor   = texture2D(sTexture, leftTextureCoordinate).rgb;" +
                    "mediump vec3 rightTextureColor  = texture2D(sTexture, rightTextureCoordinate).rgb;" +
                    "mediump vec3 topTextureColor    = texture2D(sTexture, topTextureCoordinate).rgb;" +
                    "mediump vec3 bottomTextureColor = texture2D(sTexture, bottomTextureCoordinate).rgb;" +

                    "gl_FragColor = vec4((textureColor * centerMultiplier - (leftTextureColor * edgeMultiplier + rightTextureColor * edgeMultiplier + topTextureColor * edgeMultiplier + bottomTextureColor * edgeMultiplier)), texture2D(sTexture, bottomTextureCoordinate).w);" +
                    "}";

    private float imageWidthFactor = 0.004f;
    private float imageHeightFactor = 0.004f;
    private float sharpness = 1.f;

    public GlSharpenFilter() {
        this(0.f);
    }

    public GlSharpenFilter(float sharpness) {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        this.sharpness = sharpness;
    }

    public float getSharpness() {
        return sharpness;
    }

    public void setSharpness(final float sharpness) {
        this.sharpness = sharpness;
    }

    @Override
    public void setResolution(Resolution resolution) {
        imageWidthFactor = 1f / resolution.width();
        imageHeightFactor = 1f / resolution.height();
    }


    @Override
    public void onDraw() {
        GLES20.glUniform1f(getHandle("imageWidthFactor"), imageWidthFactor);
        GLES20.glUniform1f(getHandle("imageHeightFactor"), imageHeightFactor);
        GLES20.glUniform1f(getHandle("sharpness"), sharpness);
    }

}

