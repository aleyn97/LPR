package com.pcl.lpr.utils;

/**
 * @auther : Aleyn
 * time   : 2019/04/24
 */
public class PlateRecognition {

    static {
        System.loadLibrary("lpr");
    }

    static native long InitPlateRecognizer(String casacde_detection,
                                           String finemapping_prototxt, String finemapping_caffemodel,
                                           String segmentation_prototxt, String segmentation_caffemodel,
                                           String charRecognization_proto, String charRecognization_caffemodel,
                                           String segmentation_free_prototxt, String segmentation_free_caffemodel);

    static native void ReleasePlateRecognizer(long object);

    public static native String SimpleRecognization(long inputMat, long object);

}
