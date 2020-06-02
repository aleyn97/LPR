package com.pcl.lpr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pcl.ocr.ui.LPRActivity;

import static com.pcl.ocr.ui.LPRActivity.REQUEST_LPR_CODE;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestDangerousPermissions();
        findViewById(R.id.brn_lpr).setOnClickListener(v -> {
            startActivityForResult(new Intent(this, LPRActivity.class), REQUEST_LPR_CODE);
//            startActivity(new Intent(this, TestActivity.class));
        });
    }

    @SuppressLint("ShowToast")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 识别成功回调，车牌识别
        if (requestCode == REQUEST_LPR_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String card = data.getStringExtra("card");
                new AlertDialog.Builder(this)
                        .setMessage(card)
                        .setNegativeButton("OK", (dialog, which) -> {
                        })
                        .show();
            }
        }
    }

    /**
     * 请求权限
     */
    public void requestDangerousPermissions() {
        String[] strings = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, strings, 100);
    }
}
