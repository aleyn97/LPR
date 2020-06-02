package com.pcl.ocr.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.pcl.ocr.utils.DeepAssetUtil;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static androidx.camera.core.AspectRatio.RATIO_4_3;

/**
 * @auther : Aleyn
 * time   : 2020/05/27
 */
public class ScannerView extends RelativeLayout {

    private PreviewView mPreviewView;

    private Preview preview;

    private ImageAnalysis imageAnalyzer;

    private Camera camera;

    private ExecutorService cameraExecutor;

    private ViewFinderView mViewfinderView;

    private ScannerOptions mScannerOptions;

    private ScannerViewHandler mScannerViewHandler;

    private Rect framingRectInPreview;

    private CameraAnalyzer mCameraAnalyzer;

    public ScannerView(Context context) {
        this(context, null);
    }

    public ScannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPreviewView = new PreviewView(context);
        LayoutParams cameraParams = new LayoutParams(context, attrs);
        cameraParams.width = LayoutParams.MATCH_PARENT;
        cameraParams.height = LayoutParams.MATCH_PARENT;
        mPreviewView.setId(android.R.id.list);
        addView(mPreviewView, cameraParams);

        mViewfinderView = new ViewFinderView(context, attrs);
        LayoutParams layoutParams = new LayoutParams(context, attrs);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, mPreviewView.getId());
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mPreviewView.getId());
        addView(mViewfinderView, layoutParams);

        cameraExecutor = Executors.newSingleThreadExecutor();

        mScannerViewHandler = new ScannerViewHandler();

        mCameraAnalyzer = new CameraAnalyzer(this);

    }

    public void initCamera() {
        // 获取 ProcessCameraProvider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            // 初始化 UseCase
            initUseCase();
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // 绑定 UseCase 到相机
                camera = cameraProvider.bindToLifecycle((LifecycleOwner) getContext(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer);

                // 开始预览
                preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }


    @SuppressLint("RestrictedApi")
    private void initUseCase() {
        // 1. preview
        int screenAspectRatio = getPreviewRatio();
        int rotation = mPreviewView.getDisplay().getRotation();
        preview = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, mCameraAnalyzer);
    }


    private int getPreviewRatio() {
        DisplayMetrics metrics = new DisplayMetrics();
        mPreviewView.getDisplay().getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public void setScannerOptions(ScannerOptions scannerOptions) {
        this.mScannerOptions = scannerOptions;
        mViewfinderView.setVisibility(mScannerOptions.isViewfinderHide() ? View.GONE : View.VISIBLE);
        mViewfinderView.setScannerOptions(mScannerOptions);
        initCamera();
        start();
    }


    synchronized Rect getFramingRectInPreview(int width, int height) {
        if (framingRectInPreview == null) {
            Rect framingRect = mViewfinderView.getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = new Point(width, height);
            Point screenResolution = mViewfinderView.getScreenResolution();
            if (screenResolution == null) {
                return null;
            }
            //竖屏识别一维
            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }

    /**
     * 设置识别成功监听器
     *
     * @param listener 监听器
     */
    public void setOnScannerOCRListener(OnScannerOCRListener listener) {
        mScannerViewHandler.setOCRListener(listener);
    }

    /**
     *
     */
    public void start() {
        mCameraAnalyzer.setHandle(mScannerViewHandler);
    }

    long getPRAddress() {
        return DeepAssetUtil.getPRAddress(getContext());
    }

    public interface OnScannerOCRListener {
        void onOCRSuccess(String cardNum);
    }

}
