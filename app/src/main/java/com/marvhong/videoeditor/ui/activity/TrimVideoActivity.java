package com.marvhong.videoeditor.ui.activity;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.OnClick;
import com.bumptech.glide.Glide;
import com.cjt2325.cameralibrary.util.FileUtil;
import com.iceteck.silicompressorr.SiliCompressor;
import com.marvhong.videoeditor.App;
import com.marvhong.videoeditor.R;
import com.marvhong.videoeditor.adapter.TrimVideoAdapter;
import com.marvhong.videoeditor.base.BaseActivity;
import com.marvhong.videoeditor.helper.ToolbarHelper;
import com.marvhong.videoeditor.model.FilterModel;
import com.marvhong.videoeditor.model.VideoEditInfo;
import com.marvhong.videoeditor.utils.ExtractFrameWorkThread;
import com.marvhong.videoeditor.utils.ExtractVideoInfoUtil;
import com.marvhong.videoeditor.utils.UIUtils;
import com.marvhong.videoeditor.utils.VideoUtil;
import com.marvhong.videoeditor.view.NormalProgressDialog;
import com.marvhong.videoeditor.view.RangeSeekBar;
import com.marvhong.videoeditor.view.VideoThumbSpacingItemDecoration;
import com.marvhong.videoeffect.FillMode;
import com.marvhong.videoeffect.GlVideoView;
import com.marvhong.videoeffect.IVideoSurface;
import com.marvhong.videoeffect.composer.Mp4Composer;
import com.marvhong.videoeffect.composer.Mp4Composer.Listener;
import com.marvhong.videoeffect.helper.MagicFilterFactory;
import com.marvhong.videoeffect.helper.MagicFilterType;
import com.marvhong.videoeffect.utils.ConfigUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LLhon
 * @Project Android-Video-Editor
 * @Package com.marvhong.videoeditor
 * @Date 2018/8/21 17:51
 * @description 裁剪视频界面
 */
public class TrimVideoActivity extends BaseActivity {

    @BindView(R.id.glsurfaceview)
    GlVideoView mSurfaceView;
    @BindView(R.id.video_shoot_tip)
    TextView mTvShootTip;
    @BindView(R.id.video_thumb_listview)
    RecyclerView mRecyclerView;
    @BindView(R.id.positionIcon)
    ImageView mIvPosition;
    @BindView(R.id.id_seekBarLayout)
    LinearLayout seekBarLayout;
    @BindView(R.id.layout_surface_view)
    RelativeLayout mRlVideo;
    @BindView(R.id.view_trim_indicator)
    View mViewTrimIndicator;
    @BindView(R.id.view_effect_indicator)
    View mViewEffectIndicator;
    @BindView(R.id.ll_trim_container)
    LinearLayout mLlTrimContainer;
    @BindView(R.id.hsv_effect)
    HorizontalScrollView mHsvEffect;
    @BindView(R.id.ll_effect_container)
    LinearLayout mLlEffectContainer;
    private RangeSeekBar seekBar;

    private static final String TAG = TrimVideoActivity.class.getSimpleName();
    private static final long MIN_CUT_DURATION = 3 * 1000L;// 最小剪辑时间3s
    private static final long MAX_CUT_DURATION = 10 * 1000L;//视频最多剪切多长时间
    private static final int MAX_COUNT_RANGE = 10;//seekBar的区域内一共有多少张图片
    private static final int MARGIN = UIUtils.dp2Px(56); //左右两边间距
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private int mMaxWidth; //可裁剪区域的最大宽度
    private long duration; //视频总时长
    private TrimVideoAdapter videoEditAdapter;
    private float averageMsPx;//每毫秒所占的px
    private float averagePxMs;//每px所占用的ms毫秒
    private String OutPutFileDirPath;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private long leftProgress, rightProgress; //裁剪视频左边区域的时间位置, 右边时间位置
    private long scrollPos = 0;
    private int mScaledTouchSlop;
    private int lastScrollX;
    private boolean isSeeking;
    private String mVideoPath;
    private int mOriginalWidth; //视频原始宽度
    private int mOriginalHeight; //视频原始高度
    private List<FilterModel> mVideoEffects = new ArrayList<>(); //视频滤镜效果
    private MagicFilterType[] mMagicFilterTypes;
    private ValueAnimator mEffectAnimator;
    private SurfaceTexture mSurfaceTexture;
    private MediaPlayer mMediaPlayer;
    private Mp4Composer mMp4Composer;

    public static void startActivity(Context context, String videoPath) {
        Intent intent = new Intent(context, TrimVideoActivity.class);
        intent.putExtra("videoPath", videoPath);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_trim_video;
    }

    @Override
    protected void init() {
        mVideoPath = getIntent().getStringExtra("videoPath");

        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(mVideoPath);
        mMaxWidth = UIUtils.getScreenWidth() - MARGIN * 2;
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) {
                e.onNext(mExtractVideoInfoUtil.getVideoLength());
                e.onComplete();
            }
        })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<String>() {
                @Override
                public void onSubscribe(Disposable d) {
                   subscribe(d);
                }

                @Override
                public void onNext(String s) {
                    duration = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
                    initEditVideo();
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onComplete() {

                }
            });
    }

    @Override
    protected void initToolbar(ToolbarHelper toolbarHelper) {
        toolbarHelper.setTitle("裁剪");
        toolbarHelper.setMenuTitle("发布", v -> {
            trimmerVideo();
        });
    }

    @Override
    protected void initView() {
        mRecyclerView
            .setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoEditAdapter = new TrimVideoAdapter(this, mMaxWidth / 10);
        mRecyclerView.setAdapter(videoEditAdapter);
        mRecyclerView.addOnScrollListener(mOnScrollListener);

        mSurfaceView.init(new IVideoSurface() {
            @Override
            public void onCreated(SurfaceTexture surfaceTexture) {
                mSurfaceTexture = surfaceTexture;
                initMediaPlayer(surfaceTexture);
            }
        });

        //滤镜效果集合
        mMagicFilterTypes = new MagicFilterType[]{
            MagicFilterType.NONE, MagicFilterType.INVERT,
            MagicFilterType.SEPIA, MagicFilterType.BLACKANDWHITE,
            MagicFilterType.TEMPERATURE, MagicFilterType.OVERLAY,
            MagicFilterType.BARRELBLUR, MagicFilterType.POSTERIZE,
            MagicFilterType.CONTRAST, MagicFilterType.GAMMA,
            MagicFilterType.HUE, MagicFilterType.CROSSPROCESS,
            MagicFilterType.GRAYSCALE, MagicFilterType.CGACOLORSPACE,
        };

        for (int i = 0; i < mMagicFilterTypes.length; i++) {
            FilterModel model = new FilterModel();
            model.setName(
                UIUtils.getString(MagicFilterFactory.filterType2Name(mMagicFilterTypes[i])));
            mVideoEffects.add(model);
        }

        addEffectView();
    }

    @OnClick({R.id.ll_trim_tab, R.id.ll_effect_tab})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_trim_tab: //裁切tab
                mViewTrimIndicator.setVisibility(View.VISIBLE);
                mViewEffectIndicator.setVisibility(View.GONE);
                mLlTrimContainer.setVisibility(View.VISIBLE);
                mHsvEffect.setVisibility(View.GONE);
                break;
            case R.id.ll_effect_tab: //滤镜tab
                mViewTrimIndicator.setVisibility(View.GONE);
                mViewEffectIndicator.setVisibility(View.VISIBLE);
                mLlTrimContainer.setVisibility(View.GONE);
                mHsvEffect.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 动态添加滤镜效果View
     */
    private void addEffectView() {
        mLlEffectContainer.removeAllViews();
        for (int i = 0; i < mVideoEffects.size(); i++) {
            View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_video_effect, mLlEffectContainer, false);
            TextView tv = itemView.findViewById(R.id.tv);
            ImageView iv = itemView.findViewById(R.id.iv);
            FilterModel model = mVideoEffects.get(i);
            int thumbId = MagicFilterFactory.filterType2Thumb(mMagicFilterTypes[i]);
            Glide.with(App.sApplication)
                .load(thumbId)
                .into(iv);
            tv.setText(model.getName());
            int index = i;
            itemView.setOnClickListener(v -> {
                for (int j = 0; j < mLlEffectContainer.getChildCount(); j++) {
                    View tempItemView = mLlEffectContainer.getChildAt(j);
                    TextView tempTv = tempItemView.findViewById(R.id.tv);
                    FilterModel tempModel = mVideoEffects.get(j);
                    if (j == index) {
                        //选中的滤镜效果
                        if (!tempModel.isChecked()) {
                            openEffectAnimation(tempTv, tempModel, true);
                        }
                        ConfigUtils.getInstance().setMagicFilterType(mMagicFilterTypes[j]);
                        mSurfaceView.setFilter(MagicFilterFactory.getFilter());
                    } else {
                        //未选中的滤镜效果
                        if (tempModel.isChecked()) {
                            openEffectAnimation(tempTv, tempModel, false);
                        }
                    }
                }
            });
            mLlEffectContainer.addView(itemView);
        }
    }

    private void openEffectAnimation(TextView tv, FilterModel model, boolean isExpand) {
        model.setChecked(isExpand);
        int startValue = UIUtils.dp2Px(30);
        int endValue = UIUtils.dp2Px(100);
        if (!isExpand) {
            startValue = UIUtils.dp2Px(100);
            endValue = UIUtils.dp2Px(30);
        }
        mEffectAnimator = ValueAnimator.ofInt(startValue, endValue);
        mEffectAnimator.setDuration(300);
        mEffectAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, value, Gravity.BOTTOM);
                tv.setLayoutParams(params);
                tv.requestLayout();
            }
        });
        mEffectAnimator.start();
    }

    private void initEditVideo() {
        //for video edit
        long startPosition = 0;
        long endPosition = duration;
        int thumbnailsCount;
        int rangeWidth;
        boolean isOver_10_s;
        if (endPosition <= MAX_CUT_DURATION) {
            isOver_10_s = false;
            thumbnailsCount = MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;
        } else {
            isOver_10_s = true;
            thumbnailsCount = (int) (endPosition * 1.0f / (MAX_CUT_DURATION * 1.0f)
                * MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / MAX_COUNT_RANGE * thumbnailsCount;
        }
        mRecyclerView
            .addItemDecoration(new VideoThumbSpacingItemDecoration(MARGIN, thumbnailsCount));

        //init seekBar
        if (isOver_10_s) {
            seekBar = new RangeSeekBar(this, 0L, MAX_CUT_DURATION);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(MAX_CUT_DURATION);
        } else {
            seekBar = new RangeSeekBar(this, 0L, endPosition);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(endPosition);
        }
        seekBar.setMin_cut_time(MIN_CUT_DURATION);//设置最小裁剪时间
        seekBar.setNotifyWhileDragging(true);
        seekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        seekBarLayout.addView(seekBar);

        Log.d(TAG, "-------thumbnailsCount--->>>>" + thumbnailsCount);
        averageMsPx = duration * 1.0f / rangeWidth * 1.0f;
        Log.d(TAG, "-------rangeWidth--->>>>" + rangeWidth);
        Log.d(TAG, "-------localMedia.getDuration()--->>>>" + duration);
        Log.d(TAG, "-------averageMsPx--->>>>" + averageMsPx);
        OutPutFileDirPath = VideoUtil.getSaveEditThumbnailDir(this);
        int extractW = mMaxWidth / MAX_COUNT_RANGE;
        int extractH = UIUtils.dp2Px(62);
        mExtractFrameWorkThread = new ExtractFrameWorkThread(extractW, extractH, mUIHandler,
            mVideoPath,
            OutPutFileDirPath, startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();

        //init pos icon start
        leftProgress = 0;
        if (isOver_10_s) {
            rightProgress = MAX_CUT_DURATION;
        } else {
            rightProgress = endPosition;
        }
        mTvShootTip.setText(rightProgress / 1000 + "");
        averagePxMs = (mMaxWidth * 1.0f / (rightProgress - leftProgress));
        Log.d(TAG, "------averagePxMs----:>>>>>" + averagePxMs);
    }

    /**
     * 初始化MediaPlayer
     */
    private void initMediaPlayer(SurfaceTexture surfaceTexture) {
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(mVideoPath);
            Surface surface = new Surface(surfaceTexture);
            mMediaPlayer.setSurface(surface);
            surface.release();
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    float videoProportion = (float) videoWidth / (float) videoHeight;
                    int screenWidth = mRlVideo.getWidth();
                    int screenHeight = mRlVideo.getHeight();
                    float screenProportion = (float) screenWidth / (float) screenHeight;
                    if (videoProportion > screenProportion) {
                        lp.width = screenWidth;
                        lp.height = (int) ((float) screenWidth / videoProportion);
                    } else {
                        lp.width = (int) (videoProportion * (float) screenHeight);
                        lp.height = screenHeight;
                    }
                    mSurfaceView.setLayoutParams(lp);

                    mOriginalWidth = videoWidth;
                    mOriginalHeight = videoHeight;
                    Log.e("videoView", "videoWidth:" + videoWidth + ", videoHeight:" + videoHeight);

                    //设置MediaPlayer的OnSeekComplete监听
                    mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            Log.d(TAG, "------ok----real---start-----");
                            Log.d(TAG, "------isSeeking-----" + isSeeking);
                            if (!isSeeking) {
                                videoStart();
                            }
                        }
                    });
                }
            });
            mMediaPlayer.prepare();
            videoStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 视频裁剪
     */
    private void trimmerVideo() {
        NormalProgressDialog
            .showLoading(this, getResources().getString(R.string.in_process), false);
        videoPause();
        Log.e(TAG, "trimVideo...startSecond:" + leftProgress + ", endSecond:"
            + rightProgress); //start:44228, end:48217
        //裁剪后的小视频第一帧图片
        // /storage/emulated/0/haodiaoyu/small_video/picture_1524055390067.jpg
//        Bitmap bitmap = mExtractVideoInfoUtil.extractFrame(leftProgress);
//        String firstFrame = FileUtil.saveBitmap("small_video", bitmap);
//        if (bitmap != null && !bitmap.isRecycled()) {
//            bitmap.recycle();
//            bitmap = null;
//        }
        VideoUtil
            .cutVideo(mVideoPath, VideoUtil.getTrimmedVideoPath(this, "small_video/trimmedVideo",
                "trimmedVideo_"), leftProgress / 1000,
                rightProgress / 1000)
            .subscribe(new Observer<String>() {
                @Override
                public void onSubscribe(Disposable d) {
                    subscribe(d);
                }

                @Override
                public void onNext(String outputPath) {
                    // /storage/emulated/0/Android/data/com.kangoo.diaoyur/files/small_video/trimmedVideo_20180416_153217.mp4
                    Log.e(TAG, "cutVideo---onSuccess");
                    try {
                        startMediaCodec(outputPath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    Log.e(TAG, "cutVideo---onError:" + e.toString());
                    NormalProgressDialog.stopLoading();
                    Toast.makeText(TrimVideoActivity.this, "视频裁剪失败", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onComplete() {
                }
            });
    }

    /**
     * 视频添加滤镜效果
     */
    private void startMediaCodec(String srcPath) {
        final String outputPath = VideoUtil.getTrimmedVideoPath(this, "small_video/trimmedVideo",
            "filterVideo_");

        mMp4Composer = new Mp4Composer(srcPath, outputPath)
            // .rotation(Rotation.ROTATION_270)
            //.size(720, 1280)
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(MagicFilterFactory.getFilter())
            .mute(false)
            .flipHorizontal(false)
            .flipVertical(false)
            .listener(new Listener() {
                @Override
                public void onProgress(double progress) {
                    Log.d(TAG, "filterVideo---onProgress: " + (int) (progress * 100));
                    runOnUiThread(() -> {
                        //show progress
                    });
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "filterVideo---onCompleted");
                    runOnUiThread(() -> {
                        compressVideo(outputPath);
                    });
                }

                @Override
                public void onCanceled() {
                    NormalProgressDialog.stopLoading();
                }

                @Override
                public void onFailed(Exception exception) {
                    Log.e(TAG, "filterVideo---onFailed()");
                    NormalProgressDialog.stopLoading();
                    Toast.makeText(TrimVideoActivity.this, "视频处理失败", Toast.LENGTH_SHORT).show();
                }
            })
            .start();
    }

    /**
     * 视频压缩
     */
    private void compressVideo(String srcPath) {
        String destDirPath = VideoUtil.getTrimmedVideoDir(this, "small_video");
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) {
                try {
                    int outWidth = 0;
                    int outHeight = 0;
                    if (mOriginalWidth > mOriginalHeight) {
                        //横屏
                        outWidth = 720;
                        outHeight = 480;
                    } else {
                        //竖屏
                        outWidth = 480;
                        outHeight = 720;
                    }
                    String compressedFilePath = SiliCompressor.with(TrimVideoActivity.this)
                        .compressVideo(srcPath, destDirPath, outWidth, outHeight, 900000);
                    emitter.onNext(compressedFilePath);
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<String>() {
                @Override
                public void onSubscribe(Disposable d) {
                    subscribe(d);
                }

                @Override
                public void onNext(String outputPath) {
                    //源路径: /storage/emulated/0/Android/data/com.kangoo.diaoyur/cache/small_video/trimmedVideo_20180514_163858.mp4
                    //压缩路径: /storage/emulated/0/Android/data/com.kangoo.diaoyur/cache/small_video/VIDEO_20180514_163859.mp4
                    Log.e(TAG, "compressVideo---onSuccess");
                    //获取视频第一帧图片
                    mExtractVideoInfoUtil = new ExtractVideoInfoUtil(outputPath);
                    Bitmap bitmap = mExtractVideoInfoUtil.extractFrame();
                    String firstFrame = FileUtil.saveBitmap("small_video", bitmap);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                        bitmap = null;
                    }
                    NormalProgressDialog.stopLoading();

                    VideoPreviewActivity.startActivity(TrimVideoActivity.this, outputPath, firstFrame);
                    finish();
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    Log.e(TAG, "compressVideo---onError:" + e.toString());
                    NormalProgressDialog.stopLoading();
                    Toast.makeText(TrimVideoActivity.this, "视频压缩失败", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onComplete() {
                }
            });
    }

    private boolean isOverScaledTouchSlop;

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "-------newState:>>>>>" + newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                isSeeking = false;
//                videoStart();
            } else {
                isSeeking = true;
                if (isOverScaledTouchSlop) {
                    videoPause();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = getScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            Log.d(TAG, "-------scrollX:>>>>>" + scrollX);
            //初始状态,why ? 因为默认的时候有56dp的空白！
            if (scrollX == -MARGIN) {
                scrollPos = 0;
            } else {
                // why 在这里处理一下,因为onScrollStateChanged早于onScrolled回调
                videoPause();
                isSeeking = true;
                scrollPos = (long) (averageMsPx * (MARGIN + scrollX));
                Log.d(TAG, "-------scrollPos:>>>>>" + scrollPos);
                leftProgress = seekBar.getSelectedMinValue() + scrollPos;
                rightProgress = seekBar.getSelectedMaxValue() + scrollPos;
                Log.d(TAG, "-------leftProgress:>>>>>" + leftProgress);
                mMediaPlayer.seekTo((int) leftProgress);
            }
            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     *
     * @return int px
     */
    private int getScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private ValueAnimator animator;

    private void anim() {
        Log.d(TAG, "--anim--onProgressUpdate---->>>>>>>" + mMediaPlayer.getCurrentPosition());
        if (mIvPosition.getVisibility() == View.GONE) {
            mIvPosition.setVisibility(View.VISIBLE);
        }
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mIvPosition
            .getLayoutParams();
        int start = (int) (MARGIN
            + (leftProgress/*mVideoView.getCurrentPosition()*/ - scrollPos) * averagePxMs);
        int end = (int) (MARGIN + (rightProgress - scrollPos) * averagePxMs);
        animator = ValueAnimator
            .ofInt(start, end)
            .setDuration(
                (rightProgress - scrollPos) - (leftProgress/*mVideoView.getCurrentPosition()*/
                    - scrollPos));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                mIvPosition.setLayoutParams(params);
            }
        });
        animator.start();
    }

    private final MainHandler mUIHandler = new MainHandler(this);

    private static class MainHandler extends Handler {

        private final WeakReference<TrimVideoActivity> mActivity;

        MainHandler(TrimVideoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TrimVideoActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                    if (activity.videoEditAdapter != null) {
                        VideoEditInfo info = (VideoEditInfo) msg.obj;
                        activity.videoEditAdapter.addItemVideoInfo(info);
                    }
                }
            }
        }
    }

    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue,
            int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {
            Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            leftProgress = minValue + scrollPos;
            rightProgress = maxValue + scrollPos;
            Log.d(TAG, "-----leftProgress----->>>>>>" + leftProgress);
            Log.d(TAG, "-----rightProgress----->>>>>>" + rightProgress);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "-----ACTION_DOWN---->>>>>>");
                    isSeeking = false;
                    videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "-----ACTION_MOVE---->>>>>>");
                    isSeeking = true;
                    mMediaPlayer.seekTo((int) (pressedThumb == RangeSeekBar.Thumb.MIN ?
                        leftProgress : rightProgress));
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "-----ACTION_UP--leftProgress--->>>>>>" + leftProgress);
                    isSeeking = false;
                    //从minValue开始播
                    mMediaPlayer.seekTo((int) leftProgress);
//                    videoStart();
                    mTvShootTip
                        .setText((rightProgress - leftProgress) / 1000 + "");
                    break;
                default:
                    break;
            }
        }
    };

    private void videoStart() {
        Log.d(TAG, "----videoStart----->>>>>>>");
        mMediaPlayer.start();
        mIvPosition.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
        handler.removeCallbacks(run);
        handler.post(run);
    }

    private void videoProgressUpdate() {
        long currentPosition = mMediaPlayer.getCurrentPosition();
        Log.d(TAG, "----onProgressUpdate-cp---->>>>>>>" + currentPosition);
        if (currentPosition >= (rightProgress)) {
            mMediaPlayer.seekTo((int) leftProgress);
            mIvPosition.clearAnimation();
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            anim();
        }
    }

    private void videoPause() {
        isSeeking = false;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            handler.removeCallbacks(run);
        }
        Log.d(TAG, "----videoPause----->>>>>>>");
        if (mIvPosition.getVisibility() == View.VISIBLE) {
            mIvPosition.setVisibility(View.GONE);
        }
        mIvPosition.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo((int) leftProgress);
//            videoStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPause();
    }

    private Handler handler = new Handler();
    private Runnable run = new Runnable() {

        @Override
        public void run() {
            videoProgressUpdate();
            handler.postDelayed(run, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        NormalProgressDialog.stopLoading();
        ConfigUtils.getInstance().setMagicFilterType(MagicFilterType.NONE);
        if (animator != null) {
            animator.cancel();
        }
        if (mEffectAnimator != null) {
            mEffectAnimator.cancel();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        if (mMp4Composer != null) {
            mMp4Composer.cancel();
        }
        if (mExtractVideoInfoUtil != null) {
            mExtractVideoInfoUtil.release();
        }
        if (mExtractFrameWorkThread != null) {
            mExtractFrameWorkThread.stopExtract();
        }
        mRecyclerView.removeOnScrollListener(mOnScrollListener);
        mUIHandler.removeCallbacksAndMessages(null);
        handler.removeCallbacksAndMessages(null);
        //删除视频每一帧的预览图
        if (!TextUtils.isEmpty(OutPutFileDirPath)) {
            VideoUtil.deleteFile(new File(OutPutFileDirPath));
        }
        //删除裁剪后的视频，滤镜视频
        String trimmedDirPath = VideoUtil.getTrimmedVideoDir(this, "small_video/trimmedVideo");
        if (!TextUtils.isEmpty(trimmedDirPath)) {
            VideoUtil.deleteFile(new File(trimmedDirPath));
        }
        super.onDestroy();
    }
}
