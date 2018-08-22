package com.marvhong.videoeffect.filter;

import com.marvhong.videoeffect.filter.base.GlFilter;
import com.marvhong.videoeffect.utils.OpenGlUtils;

/**
 * @author LLhon
 * @Project diaoyur_android
 * @Package com.marvhong.videoeffect.filter
 * @Date 2018/8/16 17:32
 * @description 素描
 */
public class SketchEffect extends GlFilter {

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"
        + "uniform samplerExternalOES sTexture;\n"
        + "varying highp vec2 vTextureCoord;\n"
        + "uniform vec2 singleStepOffset; \n"
        + "uniform float strength;\n"
        + "const highp vec3 W = vec3(0.299,0.587,0.114);\n"

        + "void main()\n"
        + "{ \n"
        + "  float threshold = 0.0;\n"
        + "  vec4 oralColor = texture2D(sTexture, vTextureCoord);\n"
        + "  vec3 maxValue = vec3(0.,0.,0.);\n"
        + "  for(int i = -2; i<=2; i++)\n"
        + "  {\n"
        + "    for(int j = -2; j<=2; j++)\n"
        + "    {\n"
        + "      vec4 tempColor = texture2D(sTexture, vTextureCoord+singleStepOffset*vec2(i,j));\n"
        + "      maxValue.r = max(maxValue.r,tempColor.r);\n"
        + "      maxValue.g = max(maxValue.g,tempColor.g);\n"
        + "      maxValue.b = max(maxValue.b,tempColor.b);\n"
        + "      threshold += dot(tempColor.rgb, W);\n"
        + "    }\n"
        + "  }\n"
        + "  float gray1 = dot(oralColor.rgb, W);\n"
        + "  float gray2 = dot(maxValue, W);\n"
        + "  float contour = gray1 / gray2;\n"
        + "  threshold = threshold / 25.;\n"
        + "  float alpha = max(1.0,gray1>threshold?1.0:(gray1/threshold));\n"
        + "  float result = contour * alpha + (1.0-alpha)*gray1;\n"
        + "  gl_FragColor = vec4(vec3(result,result,result), oralColor.w);\n"
        + "} ";

    public SketchEffect() {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }
}
