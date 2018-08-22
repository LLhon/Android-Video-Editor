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

package com.marvhong.videoeffect;

public enum Rotation {

    NORMAL(0),

    ROTATION_90(90),

    ROTATION_180(180),

    ROTATION_270(270);

    private final int rotation;

    Rotation(int rotation) {
        this.rotation = rotation;
    }

    public int getRotation() {
        return rotation;
    }

    public static Rotation fromInt(int rotate) {
        for (Rotation rotation : Rotation.values()) {
            if (rotate == rotation.getRotation())
                return rotation;
        }
        return NORMAL;
    }
}
