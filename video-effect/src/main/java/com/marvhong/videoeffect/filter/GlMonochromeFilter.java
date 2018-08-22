package com.marvhong.videoeffect.filter;

import android.opengl.GLES20;
import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * 单色
 * Created by sudamasayuki on 2018/01/06.
 */

public class GlMonochromeFilter extends GlFilter {

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision lowp float;" +

                    "varying vec2 vTextureCoord;" +
                    "uniform samplerExternalOES sTexture;\n" +

                    "uniform float intensity;" +

                    "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);" +
                    "const mediump vec3 filterColor = vec3(0.6, 0.45, 0.3);" +

                    "void main() {" +

                    "lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);" +
                    "float luminance = dot(textureColor.rgb, luminanceWeighting);" +

                    "lowp vec4 desat = vec4(vec3(luminance), 1.0);" +

                    "lowp vec4 outputColor = vec4(" +
                    "(desat.r < 0.5 ? (2.0 * desat.r * filterColor.r) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - filterColor.r)))," +
                    "(desat.g < 0.5 ? (2.0 * desat.g * filterColor.g) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - filterColor.g)))," +
                    "(desat.b < 0.5 ? (2.0 * desat.b * filterColor.b) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - filterColor.b)))," +
                    "1.0" +
                    ");" +

                    "gl_FragColor = vec4(mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);" +
                    "}";

    private float intensity = 1.0f;

    public GlMonochromeFilter() {
        this(1.0f);
    }

    public GlMonochromeFilter(float intensity) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        this.intensity = intensity;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    @Override
    public void onDraw() {
        GLES20.glUniform1f(getHandle("intensity"), intensity);
    }

}

