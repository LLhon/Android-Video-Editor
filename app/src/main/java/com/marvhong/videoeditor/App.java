package com.marvhong.videoeditor;

import android.app.Application;
import android.content.Context;

/**
 * @author LLhon
 * @Project Android-Video-Editor
 * @Package com.marvhong.videoeditor
 * @Date 2018/8/21 16:00
 * @description
 */
public class App extends Application {

    public static Context sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = getApplicationContext();
    }
}
