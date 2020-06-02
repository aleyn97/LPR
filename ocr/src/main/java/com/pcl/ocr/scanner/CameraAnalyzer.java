package com.pcl.ocr.scanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.pcl.ocr.utils.PlateRecognition;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @auther : Aleyn
 * time   : 2020/05/27
 */
public class CameraAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = CameraAnalyzer.class.getSimpleName();

    private long prAddress = 0;

    private final ScannerView scannerView;

    private Handler previewHandler;

    CameraAnalyzer(ScannerView scannerView) {
        this.scannerView = scannerView;
    }

    void setHandle(Handler previewHandler) {
        this.previewHandler = previewHandler;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (previewHandler != null) {
            Mat mat = ImagetoMat(image);
            if (mat != null) {
                if (prAddress == 0) {
                    prAddress = scannerView.getPRAddress();
                    image.close();
                    return;
                }
                String res = PlateRecognition.SimpleRecognization(mat.getNativeObjAddr(), prAddress);
                Message message;
                if (!"".equals(res)) {
                    message = Message.obtain(previewHandler, Scanner.OCR_SUCCEEDED, res);
                    previewHandler = null;
                } else {
                    message = Message.obtain(previewHandler, Scanner.OCR_FAILED);
                }
                message.sendToTarget();
            } else Log.d("analyze", "Mat is null");
        } else {
            Log.d(TAG, "previewHandler is null");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
            image.close();
        }
        image.close();
    }

    private Mat ImagetoMat(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] plane = imageProxy.getPlanes();
        ByteBuffer yBuffer = plane[0].getBuffer();  // Y
        ByteBuffer uBuffer = plane[1].getBuffer();  // U
        ByteBuffer vBuffer = plane[2].getBuffer();  // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        try {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream(nv21.length);
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 90, stream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Rect rect = scannerView.getFramingRectInPreview(bitmap.getWidth(), bitmap.getHeight());
            bitmap = Bitmap.createBitmap(bitmap, rect.top, rect.left, rect.height(), rect.width(), matrix, true);
            stream.close();
            Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(bitmap, mat);
            return mat;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
