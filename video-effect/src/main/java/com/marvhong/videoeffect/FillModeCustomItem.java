package com.marvhong.videoeffect;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sudamasayuki2 on 2018/01/08.
 */

public class FillModeCustomItem implements Parcelable {
    private final float scale;
    private final float rotate;
    private final float translateX;
    private final float translateY;
    private final float videoWidth;
    private final float videoHeight;

    public FillModeCustomItem(float scale, float rotate, float translateX, float translateY, float videoWidth, float videoHeight) {
        this.scale = scale;
        this.rotate = rotate;
        this.translateX = translateX;
        this.translateY = translateY;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public float getScale() {
        return scale;
    }

    public float getRotate() {
        return rotate;
    }

    public float getTranslateX() {
        return translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    public float getVideoWidth() {
        return videoWidth;
    }

    public float getVideoHeight() {
        return videoHeight;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.scale);
        dest.writeFloat(this.rotate);
        dest.writeFloat(this.translateX);
        dest.writeFloat(this.translateY);
        dest.writeFloat(this.videoWidth);
        dest.writeFloat(this.videoHeight);
    }

    protected FillModeCustomItem(Parcel in) {
        this.scale = in.readFloat();
        this.rotate = in.readFloat();
        this.translateX = in.readFloat();
        this.translateY = in.readFloat();
        this.videoWidth = in.readFloat();
        this.videoHeight = in.readFloat();
    }

    public static final Parcelable.Creator<FillModeCustomItem> CREATOR = new Parcelable.Creator<FillModeCustomItem>() {
        @Override
        public FillModeCustomItem createFromParcel(Parcel source) {
            return new FillModeCustomItem(source);
        }

        @Override
        public FillModeCustomItem[] newArray(int size) {
            return new FillModeCustomItem[size];
        }
    };
}
