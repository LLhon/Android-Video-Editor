package com.marvhong.videoeffect.filter;

import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * 重叠
 * Created by guoshuyu on 2017/9/17.
 */

public class OverlayEffect extends GlFilter {

    float fstep = 0.0015f;

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "             uniform samplerExternalOES sTexture; \n" +
            "             varying vec2 vTextureCoord; \n" +
            "  \n" +
            "             void main() \n" +
            "             { \n" +
            "              vec4 sample0,sample1,sample2,sample3; \n" +
            "              float fstep=" + 0.0015f + "; \n" +
            "              sample0=texture2D(sTexture,vec2(vTextureCoord.x-fstep,vTextureCoord.y-fstep)); \n"
            +
            "              sample1=texture2D(sTexture,vec2(vTextureCoord.x+fstep,vTextureCoord.y-fstep)); \n"
            +
            "              sample2=texture2D(sTexture,vec2(vTextureCoord.x+fstep,vTextureCoord.y+fstep)); \n"
            +
            "              sample3=texture2D(sTexture,vec2(vTextureCoord.x-fstep,vTextureCoord.y+fstep)); \n"
            +
            "              vec4 color=(sample0+sample1+sample2+sample3) / 4.0; \n" +
            "              gl_FragColor=color; \n" +
            "             } ";

    public OverlayEffect() {
        this(0.0015f);
    }

    public OverlayEffect(float fstep) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        this.fstep = fstep;
    }
}
