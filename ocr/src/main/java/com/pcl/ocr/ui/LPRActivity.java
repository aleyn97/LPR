package com.pcl.ocr.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pcl.ocr.R;
import com.pcl.ocr.scanner.ScannerOptions;
import com.pcl.ocr.scanner.ScannerView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class LPRActivity extends AppCompatActivity {

    private ScannerView scannerView;

    public static final int REQUEST_LPR_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lpr);
        scannerView = findViewById(R.id.scanner_view);
        startCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @SuppressLint("RestrictedApi")
    private void startCamera() {
        ScannerOptions builder = new ScannerOptions.Builder()
                .setTipText("请将识别车牌放入框内")
                .setFrameCornerColor(0xFF26CEFF)
                .setLaserLineColor(0xFF26CEFF)
                .build();

        scannerView.setScannerOptions(builder);
        scannerView.setOnScannerOCRListener(cardNum -> {
            Log.d("OCRListener", cardNum);
            Log.d("OCRListener", Thread.currentThread().getName());
            new AlertDialog.Builder(LPRActivity.this)
                    .setMessage(cardNum)
                    .setNegativeButton("重新识别", (dialogInterface, i) -> {
                        scannerView.start();
                    })
                    .setPositiveButton("确定", (dialogInterface, i) -> {
                        finishValue(cardNum);
                    })
                    .show();
        });
    }

    private void finishValue(String card) {
        Intent intent = new Intent();
        intent.putExtra("card", card);
        setResult(RESULT_OK, intent);
        finish();
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @SuppressLint("StaticFieldLeak")
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                System.loadLibrary("lpr");
            } else {
                super.onManagerConnected(status);
            }
        }
    };
}