package com.marvhong.videoeditor.ui.activity;

import android.Manifest.permission;
import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.marvhong.videoeditor.R;
import com.marvhong.videoeditor.base.BaseActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class MainActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private RxPermissions mRxPermissions;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        mRxPermissions = new RxPermissions(this);
        ButterKnife.bind(this);
        setupToolbar(mToolbar, "视频编辑");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    /**
     * 拍照
     * @param view
     */
    public void takeCamera(View view) {
        mRxPermissions
            .request(permission.WRITE_EXTERNAL_STORAGE, permission.RECORD_AUDIO, permission.CAMERA)
            .subscribe(granted -> {
                if (granted) { //已获取权限
                    Intent intent = new Intent(MainActivity.this, VideoCameraActivity.class);
                    startActivityForResult(intent, 100);
                } else {
                    Toast.makeText(MainActivity.this, "给点权限行不行？", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * 相册
     * @param view
     */
    public void takeAlbum(View view) {
        mRxPermissions
            .request(permission.WRITE_EXTERNAL_STORAGE,permission.READ_EXTERNAL_STORAGE)
            .subscribe(granted -> {
                if (granted) { //已获取权限
                    Intent intent = new Intent(MainActivity.this, VideoAlbumActivity.class);
                    startActivityForResult(intent, 100);
                } else {
                    Toast.makeText(MainActivity.this, "给点权限行不行？", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
