package com.pcl.lpr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pcl.ocr.ui.LPRActivity;

import static com.pcl.ocr.ui.LPRActivity.REQUEST_LPR_CODE;

public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_CODE = 100;

    private final String[] PERMISSION_LPR = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.brn_lpr).setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this, PERMISSION_LPR, PERMISSION_CODE);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] < 0) {
                String alent = "";
                if (i == 0) alent = "相机";
                if (i == 1) alent = "读写";
                Toast.makeText(this, alent + "权限被拒绝，请到设置中开启", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (requestCode == PERMISSION_CODE) {
            startActivityForResult(new Intent(this, LPRActivity.class), REQUEST_LPR_CODE);
        }
    }

}
