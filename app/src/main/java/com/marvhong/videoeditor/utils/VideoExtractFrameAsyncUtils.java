package com.marvhong.videoeditor.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.marvhong.videoeditor.model.VideoEditInfo;

/**
 * ================================================
 * 作    者：顾修忠-guxiuzhong@youku.com/gfj19900401@163.com
 * 版    本：
 * 创建日期：2017/3/2-下午7:06
 * 描    述：
 * 修订历史：
 * ================================================
 */

public class VideoExtractFrameAsyncUtils {

    private Handler mHandler;
    private  int extractW;
    private  int extractH;

    public VideoExtractFrameAsyncUtils(int extractW, int extractH, Handler mHandler) {
        this.mHandler = mHandler;
        this.extractW=extractW;
        this.extractH=extractH;
    }

    public void getVideoThumbnailsInfoForEdit(String videoPath, String OutPutFileDirPath, long startPosition, long endPosition, int thumbnailsCount) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(videoPath);
        long interval = (endPosition - startPosition) / (thumbnailsCount - 1);
        for (int i = 0; i < thumbnailsCount; i++) {
            if (stop) {
                Log.d("ExtractFrame", "-------ok-stop-stop-->>>>>>>>>");
                metadataRetriever.release();
                break;
            }
            long time = startPosition + interval * i;
            if (i == thumbnailsCount - 1) {
                if (interval > 1000) {
                    String path = extractFrame(metadataRetriever, endPosition - 800, OutPutFileDirPath);
                    sendAPic(path, endPosition - 800);
                } else {
                    String path = extractFrame(metadataRetriever, endPosition, OutPutFileDirPath);
                    sendAPic(path, endPosition);
                }
            } else {
                String path = extractFrame(metadataRetriever, time, OutPutFileDirPath);
                sendAPic(path, time);
            }
        }
        metadataRetriever.release();
    }

    /**
     * 成功一张add一张
     *
     * @param path path
     * @param time time
     */
    private void sendAPic(String path, long time) {
        VideoEditInfo info = new VideoEditInfo();
        info.path = path;
        info.time = time;
        Message msg = mHandler.obtainMessage(ExtractFrameWorkThread.MSG_SAVE_SUCCESS);
        msg.obj = info;
        mHandler.sendMessage(msg);
    }

    private String extractFrame(MediaMetadataRetriever metadataRetriever, long time, String OutPutFileDirPath) {
        Bitmap bitmap = metadataRetriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        if (bitmap != null) {
            //如果对bitmap进行缩放处理,得到的图片大小很小(1kb左右)但会非常模糊,不缩放图片大小60kb左右图片很清晰
//            Bitmap bitmapNew= scaleImage(bitmap);
            Bitmap bitmapNew = bitmap;
            String path= VideoUtil.saveImageToSDForEdit(bitmapNew, OutPutFileDirPath, System.currentTimeMillis() + "_" + time + VideoUtil.POSTFIX);
            if (bitmapNew!=null &&!bitmapNew.isRecycled()) {
                bitmapNew.recycle();
                bitmapNew = null;
            }
            return path;
        }
        return null;
    }

    /**
     * 设置固定的宽度，高度随之变化，使图片不会变形
     *
     * @param bm Bitmap
     * @return Bitmap
     */
    private Bitmap scaleImage(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = extractW * 1.0f / width;
//        float scaleHeight =extractH*1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        Bitmap newBm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        if (!bm.isRecycled()) {
            bm.recycle();
            bm = null;
        }
        return newBm;
    }


    private volatile boolean stop;

    public void stopExtract() {
        stop = true;
    }
}
