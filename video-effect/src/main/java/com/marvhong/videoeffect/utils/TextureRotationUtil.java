/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marvhong.videoeffect.utils;

import android.opengl.Matrix;
import com.marvhong.videoeffect.Rotation;
import java.nio.FloatBuffer;

public class TextureRotationUtil {

    public static final float TEXTURE_NO_ROTATION[] = {
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
    };

    public static final float TEXTURE_ROTATED_90[] = {
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
    };
    public static final float TEXTURE_ROTATED_180[] = {
        1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
    };
    public static final float TEXTURE_ROTATED_270[] = {
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
    };

    public static final float CUBE[] = {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f,
    };

    private TextureRotationUtil() {
    }

    public static float[] getRotation(final Rotation rotation, final boolean flipHorizontal,
        final boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case ROTATION_90:
                rotatedTex = TEXTURE_ROTATED_90;
                break;
            case ROTATION_180:
                rotatedTex = TEXTURE_ROTATED_180;
                break;
            case ROTATION_270:
                rotatedTex = TEXTURE_ROTATED_270;
                break;
            case NORMAL:
            default:
                rotatedTex = TEXTURE_NO_ROTATION;
                break;
        }
        if (flipHorizontal) {
            rotatedTex = new float[]{
                flip(rotatedTex[0]), rotatedTex[1],
                flip(rotatedTex[2]), rotatedTex[3],
                flip(rotatedTex[4]), rotatedTex[5],
                flip(rotatedTex[6]), rotatedTex[7],
            };
        }
        if (flipVertical) {
            rotatedTex = new float[]{
                rotatedTex[0], flip(rotatedTex[1]),
                rotatedTex[2], flip(rotatedTex[3]),
                rotatedTex[4], flip(rotatedTex[5]),
                rotatedTex[6], flip(rotatedTex[7]),
            };
        }
        return rotatedTex;
    }


    public static void adjustSize(ScaleType scaleType, int imageWidth, int imageHeight,
        int outputWidth, int outputHeight, int rotation, boolean flipHorizontal,
        boolean flipVertical, FloatBuffer gLCubeBuffer, FloatBuffer gLTextureBuffer) {
        float[] textureCords = TextureRotationUtil.getRotation(Rotation.fromInt(rotation),
            flipHorizontal, flipVertical);
        float[] cube = TextureRotationUtil.CUBE;
        float ratio1 = (float) outputWidth / imageWidth;
        float ratio2 = (float) outputHeight / imageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);

        float ratioWidth = imageWidthNew / (float) outputWidth;
        float ratioHeight = imageHeightNew / (float) outputHeight;

        if (scaleType == ScaleType.CENTER_INSIDE) {
            cube = new float[]{
                TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
                TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
                TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
                TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
            };
        } else if (scaleType == ScaleType.FIT_XY) {

        } else if (scaleType == ScaleType.CENTER_CROP) {
            float distVertical, distHorizontal;
            if (Rotation.fromInt(rotation) != Rotation.ROTATION_90
                && Rotation.fromInt(rotation) != Rotation.ROTATION_270) {
                distVertical = (1 - 1 / ratioWidth) / 2;
                distHorizontal = (1 - 1 / ratioHeight) / 2;
            } else {
                distHorizontal = (1 - 1 / ratioWidth) / 2;
                distVertical = (1 - 1 / ratioHeight) / 2;
            }
            textureCords = new float[]{
                addDistance(textureCords[0], distVertical),
                addDistance(textureCords[1], distHorizontal),
                addDistance(textureCords[2], distVertical),
                addDistance(textureCords[3], distHorizontal),
                addDistance(textureCords[4], distVertical),
                addDistance(textureCords[5], distHorizontal),
                addDistance(textureCords[6], distVertical),
                addDistance(textureCords[7], distHorizontal),
            };
        }
        gLCubeBuffer.clear();
        gLCubeBuffer.put(cube).position(0);
        gLTextureBuffer.clear();
        gLTextureBuffer.put(textureCords).position(0);
    }

    private static float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }


    private static float flip(final float i) {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }

    public enum ScaleType {
        CENTER_INSIDE,
        CENTER_CROP,
        FIT_XY
    }


    private static int STANDARD_SIZE = 1080;

    public static void getMatrixByPosition(float[] matrix, int imageWidth, int imageHeight,
        int offsetX, int offsetY) {
        float[] projection = new float[16];

        Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);

        float[] camera = new float[16];
        Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);

        float[] mMatrixCurrent =     //原始矩阵
            {1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1};

        Matrix.scaleM(mMatrixCurrent, 0, 1f * STANDARD_SIZE / imageWidth,
            1f * STANDARD_SIZE / imageHeight, 1);
        Matrix.translateM(mMatrixCurrent, 0, -(offsetX - (float) imageWidth / 2) / STANDARD_SIZE,
            -(offsetY - (float) imageHeight / 2) / STANDARD_SIZE, 0);

        Matrix.multiplyMM(matrix, 0, camera, 0, mMatrixCurrent, 0);
        Matrix.multiplyMM(matrix, 0, projection, 0, matrix, 0);
    }
}
