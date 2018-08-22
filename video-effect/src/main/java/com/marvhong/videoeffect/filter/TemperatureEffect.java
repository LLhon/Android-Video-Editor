package com.marvhong.videoeffect.filter;

import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * Adjusts color temperature of the video.
 *
 * @author sheraz.khilji
 */
public class TemperatureEffect extends GlFilter {

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"
        + "uniform samplerExternalOES sTexture;\n"
        + " float scale;\n"
        + "varying vec2 vTextureCoord;\n"
        + "void main() {\n" // Parameters that were created above
        + "scale = " + (2.0f * 0.8f - 1.0f) + ";\n"
        + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
        + "  vec3 new_color = color.rgb;\n"
        + "  new_color.r = color.r + color.r * ( 1.0 - color.r) * scale;\n"
        + "  new_color.b = color.b - color.b * ( 1.0 - color.b) * scale;\n"
        + "  if (scale > 0.0) { \n"
        + "    new_color.g = color.g + color.g * ( 1.0 - color.g) * scale * 0.25;\n"
        + "  }\n"
        + "  float max_value = max(new_color.r, max(new_color.g, new_color.b));\n"
        + "  if (max_value > 1.0) { \n"
        + "     new_color /= max_value;\n" + "  } \n"
        + "  gl_FragColor = vec4(new_color, color.a);\n" + "}\n";


    private float scale = 0f;

    public TemperatureEffect() {
        this(0.8f);
    }

    /**
     * Initialize Effect
     *
     * @param scale Float, between 0 and 1, with 0 indicating cool, and 1
     *              indicating warm. A value of of 0.5 indicates no change.
     */
    public TemperatureEffect(float scale) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        if (scale < 0.0f)
            scale = 0.0f;
        if (scale > 1.0f)
            scale = 1.0f;
        this.scale = scale;
    }
}
