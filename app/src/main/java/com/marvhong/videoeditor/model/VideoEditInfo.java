package com.marvhong.videoeditor.model;

import java.io.Serializable;

/**
 * @author LLhon
 * @Project diaoyur_android
 * @Package com.kangoo.diaoyur.model
 * @Date 2018/4/21 10:59
 * @description
 */
public class VideoEditInfo implements Serializable {

    public String path; //图片的sd卡路径
    public long time;//图片所在视频的时间  毫秒

    public VideoEditInfo() {
    }


    @Override
    public String toString() {
        return "VideoEditInfo{" +
            "path='" + path + '\'' +
            ", time='" + time + '\'' +
            '}';
    }
}
