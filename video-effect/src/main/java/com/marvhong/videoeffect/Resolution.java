package com.marvhong.videoeffect;

/**
 * Created by sudamasayuki on 2017/11/15.
 */

public class Resolution {
    private final int width;
    private final int height;

    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
