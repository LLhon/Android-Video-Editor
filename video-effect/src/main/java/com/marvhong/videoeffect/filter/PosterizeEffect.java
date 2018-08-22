package com.marvhong.videoeffect.filter;

import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;


/**
 * Applies Posterization effect to video.
 *
 * @author sheraz.khilji
 */
public class PosterizeEffect extends GlFilter {

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"
        + "uniform samplerExternalOES sTexture;\n"
        + "varying vec2 vTextureCoord;\n" + "void main() {\n"
        + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
        + "  vec3 pcolor;\n"
        + "  pcolor.r = (color.r >= 0.5) ? 0.75 : 0.25;\n"
        + "  pcolor.g = (color.g >= 0.5) ? 0.75 : 0.25;\n"
        + "  pcolor.b = (color.b >= 0.5) ? 0.75 : 0.25;\n"
        + "  gl_FragColor = vec4(pcolor, color.a);\n" + "}\n";

    /**
     * Initialize Effect
     */
    public PosterizeEffect() {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }
}
