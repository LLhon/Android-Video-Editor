package com.marvhong.videoeditor.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.marvhong.videoeditor.App;


public class UIUtils {

    public static Context getContext() {
        return App.sApplication;
    }

    public static Resources getResources() {
        return getContext().getResources();
    }

    public static String getString(int resId) {
        return getResources().getString(resId);
    }

    public static String[] getStringArr(int resId) {
        return getResources().getStringArray(resId);
    }

    public static int getColor(int resId) {
        return getResources().getColor(resId);
    }

    public static int getDimens(int resId) {
        return getResources().getDimensionPixelSize(resId);
    }

    public static String getPackageName() {
        return getContext().getPackageName();

    }

    public static int dp2Px(int dip) {
        //        dp<-->px
        //1. px/dp = density
        //2. px / (ppi/160) = dp;

        float density = UIUtils.getResources().getDisplayMetrics().density;
        int px = (int) (dip * density + .5f);
        return px;
    }

    public static int px2Dip(int px) {
        //1. px/dp = density
        float density = UIUtils.getResources().getDisplayMetrics().density;
        int dp = (int) (px / density + .5f);
        return dp;
    }

    public static boolean isFullScreen(final Activity activity) {
        return (activity.getWindow().getAttributes().flags &
            WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isTranslucentStatus(final Activity activity) {
        //noinspection SimplifiableIfStatement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return (activity.getWindow().getAttributes().flags &
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) != 0;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isFitsSystemWindows(final Activity activity) {
        //noinspection SimplifiableIfStatement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0).
                getFitsSystemWindows();
        }
        return false;
    }

    public static int getScreenWidth() {
        DisplayMetrics dm = UIUtils.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public  static int getScreenWidth(Activity activity){
        WindowManager manager = activity.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        return width;
    }

    public static int getScreenHeight() {
        DisplayMetrics dm = UIUtils.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    public static int getScreenHeight(Activity activity){
        WindowManager manager = activity.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int height = outMetrics.heightPixels;
        return height;
    }
}
