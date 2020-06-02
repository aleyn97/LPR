package com.pcl.ocr.scanner;

import android.content.Context;
import android.util.TypedValue;

/**
 *
 */
public final class Scanner {
    public static final int OCR_SUCCEEDED = 0;
    public static final int OCR_FAILED = 1;

    public static class color {
        public static final int VIEWFINDER_MASK = 0x60000000;
        public static final int RESULT_VIEW = 0xb0000000;
        public static final int VIEWFINDER_LASER = 0xff00ff00;
        public static final int POSSIBLE_RESULT_POINTS = 0xc0ffbd21;
        public static final int RESULT_POINTS = 0xc099cc00;
    }

    public static int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue
                , context.getResources().getDisplayMetrics());
    }

    public static int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue
                , context.getResources().getDisplayMetrics());
    }
}
