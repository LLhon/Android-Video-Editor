package com.marvhong.videoeditor;

import android.Manifest.permission;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class MainActivity extends AppCompatActivity {

    private RxPermissions mRxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
