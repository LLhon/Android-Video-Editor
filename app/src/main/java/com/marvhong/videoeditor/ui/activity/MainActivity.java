package com.marvhong.videoeditor.ui.activity;

import android.Manifest.permission;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import com.marvhong.videoeditor.R;
import com.marvhong.videoeditor.base.BaseActivity;
import com.marvhong.videoeditor.helper.ToolbarHelper;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class MainActivity extends BaseActivity {

    private RxPermissions mRxPermissions;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initToolbar(ToolbarHelper toolbarHelper) {
        toolbarHelper.setTitle("视频编辑");
        toolbarHelper.hideBackArrow();
    }

    @Override
    protected void initView() {
        mRxPermissions = new RxPermissions(this);
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
