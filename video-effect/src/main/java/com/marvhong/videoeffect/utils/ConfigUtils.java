package com.marvhong.videoeffect.utils;

import android.os.Environment;
import com.marvhong.videoeffect.helper.MagicFilterType;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author LLhon
 * @Project diaoyur_android
 * @Package com.marvhong.videoeffect.utils
 * @Date 2018/8/8 19:56
 * @description
 */
public class ConfigUtils {

    private static ConfigUtils sInstance;
    private MagicFilterType mMagicFilterType = MagicFilterType.NONE;
    private String mOutPutFilterVideoPath;

    private ConfigUtils() {

    }

    public static ConfigUtils getInstance() {
        if (sInstance == null) {
            synchronized (ConfigUtils.class) {
                if (sInstance == null) {
                    sInstance = new ConfigUtils();
                }
            }
        }
        return sInstance;
    }

    public void setMagicFilterType(MagicFilterType type) {
        mMagicFilterType = type;
    }

    public MagicFilterType getMagicFilterType() {
        return mMagicFilterType;
    }

    public String getOutPutFilterVideoPath() {
        return getAndroidMoviesFolder().getAbsolutePath() + "/" + new SimpleDateFormat(
            "yyyyMM_dd-HHmmss").format(new Date()) + "filter-effect.mp4";
    }

    public File getAndroidMoviesFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }
}
