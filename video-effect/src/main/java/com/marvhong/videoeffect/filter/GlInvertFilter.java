package com.marvhong.videoeffect.filter;

import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * 反色
 * Created by sudamasayuki on 2018/01/06.
 */

public class GlInvertFilter extends GlFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 vTextureCoord;" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {" +
                    "lowp vec4 color = texture2D(sTexture, vTextureCoord);" +
                    "gl_FragColor = vec4((1.0 - color.rgb), color.w);" +
                    "}";

    public GlInvertFilter() {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

