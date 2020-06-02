package com.pcl.ocr.scanner;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author Aleyn
 */
final class ScannerViewHandler extends Handler {

    private ScannerView.OnScannerOCRListener mScannerOCRListener;

    void setOCRListener(ScannerView.OnScannerOCRListener handleDecodeListener) {
        this.mScannerOCRListener = handleDecodeListener;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case Scanner.OCR_SUCCEEDED:
                if (mScannerOCRListener != null)
                    mScannerOCRListener.onOCRSuccess((String) message.obj);
                break;
            case Scanner.OCR_FAILED:
                Log.d("OCR_FAILED", "未识别成功");
                if (mScannerOCRListener != null)
//                    mScannerOCRListener.onFramBitmap((Bitmap) message.obj);
                break;
        }
    }
}
