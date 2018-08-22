package com.marvhong.videoeffect.filter;

import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * Adjusts the contrast of the video.
 *
 * @author sheraz.khilji
 */
public class ContrastEffect extends GlFilter {

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"
        + "uniform samplerExternalOES sTexture;\n"
        + " float contrast;\n" + "varying vec2 vTextureCoord;\n"
        + "void main() {\n" + "  contrast =" + 2.0f + ";\n"
        + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
        + "  color -= 0.5;\n" + "  color *= contrast;\n"
        + "  color += 0.5;\n" + "  gl_FragColor = color;\n" + "}\n";

    private float contrast;

    public ContrastEffect() {
        this(0.8f);
    }

    /**
     * Initialize Effect
     *
     * @param contrast Range should be between 0.1- 2.0 with 1.0 being normal.
     */
    public ContrastEffect(float contrast) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        if (contrast < 0.1f) {
            contrast = 0.1f;
        }
        if (contrast > 2.0f) {
            contrast = 2.0f;
        }
        this.contrast = contrast;
    }
}
