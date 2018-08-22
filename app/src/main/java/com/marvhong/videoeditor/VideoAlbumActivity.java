package com.marvhong.videoeditor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.marvhong.videoeditor.adapter.VideoGridAdapter;
import com.marvhong.videoeditor.model.LocalVideoModel;
import com.marvhong.videoeditor.utils.VideoUtil;
import com.marvhong.videoeditor.view.DividerGridItemDecoration;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LLhon
 * @Project Android-Video-Editor
 * @Package com.marvhong.videoeditor
 * @Date 2018/8/21 15:16
 * @description 视频相册界面
 */
public class VideoAlbumActivity extends AppCompatActivity implements VideoGridAdapter.OnItemClickListener {

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    private CompositeDisposable mDisposables = new CompositeDisposable();
    private List<LocalVideoModel> mLocalVideoModels = new ArrayList<>();
    private VideoGridAdapter mAdapter;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, VideoAlbumActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_album);
        initView();
        initData();
    }

    private void initView() {
        ButterKnife.bind(this);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerGridItemDecoration(this));
        mAdapter = new VideoGridAdapter(this, mLocalVideoModels);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(this);
    }

    private void initData() {
        VideoUtil.getLocalVideoFiles(this)
            .subscribe(new Observer<ArrayList<LocalVideoModel>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    mDisposables.add(d);
                }

                @Override
                public void onNext(ArrayList<LocalVideoModel> localVideoModels) {
                    mLocalVideoModels = localVideoModels;
                    mAdapter.setData(mLocalVideoModels);
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onComplete() {

                }
            });
    }

    @Override
    public void onItemClick(int position, LocalVideoModel model) {
        Intent intent = new Intent(this, TrimVideoActivity.class);
        intent.putExtra("videoPath", model.getVideoPath());
        startActivityForResult(intent, 100);
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocalVideoModels = null;
    }
}
