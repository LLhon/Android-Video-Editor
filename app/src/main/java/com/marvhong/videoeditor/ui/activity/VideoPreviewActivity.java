package com.marvhong.videoeditor.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import butterknife.BindView;
import butterknife.OnClick;
import com.bumptech.glide.Glide;
import com.marvhong.videoeditor.App;
import com.marvhong.videoeditor.R;
import com.marvhong.videoeditor.base.BaseActivity;

/**
 * @author LLhon
 * @Project Android-Video-Editor
 * @Package com.marvhong.videoeditor.ui.activity
 * @Date 2018/8/23 15:31
 * @description 视频预览界面
 */
public class VideoPreviewActivity extends BaseActivity {

    @BindView(R.id.fl)
    FrameLayout mFlVideo;
    @BindView(R.id.videoView)
    VideoView mVideoView;
    @BindView(R.id.iv_thumb)
    ImageView mIvThumb;
    @BindView(R.id.iv_play)
    ImageView mIvPlay;

    private String mVideoPath;
    private String mVideoThumb;

    public static void startActivity(Context context, String videoPath, String videoThumb) {
        Intent intent = new Intent(context, VideoPreviewActivity.class);
        intent.putExtra("path", videoPath);
        intent.putExtra("thumb", videoThumb);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_video_preview;
    }

    @Override
    protected void init() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mVideoPath = getIntent().getStringExtra("path");
        mVideoThumb = getIntent().getStringExtra("thumb");
    }

    @Override
    protected void initView() {
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                float videoProportion = (float) videoWidth / (float) videoHeight;
                int screenWidth = mFlVideo.getWidth();
                int screenHeight = mFlVideo.getHeight();
                float screenProportion = (float) screenWidth / (float) screenHeight;
                if (videoProportion > screenProportion) {
                    lp.width = screenWidth;
                    lp.height = (int) ((float) screenWidth / videoProportion);
                } else {
                    lp.width = (int) (videoProportion * (float) screenHeight);
                    lp.height = screenHeight;
                }
                mVideoView.setLayoutParams(lp);

                Log.e("videoView",
                    "videoWidth:" + videoWidth + ", videoHeight:" + videoHeight);
            }
        });
        mVideoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mIvPlay.setVisibility(View.VISIBLE);
                mIvThumb.setVisibility(View.VISIBLE);
                Glide.with(App.sApplication)
                    .load(mVideoThumb)
                    .into(mIvThumb);
            }
        });
        videoStart();
    }

    @OnClick({R.id.iv_play})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_play:
                mIvThumb.setVisibility(View.GONE);
                mIvPlay.setVisibility(View.GONE);
                videoStart();
                break;
        }
    }

    public void videoStart() {
        mVideoView.start();
    }

    public void videoPause() {
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.pause();
        }
    }

    public void videoDestroy() {
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏显示
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoDestroy();
    }
}
