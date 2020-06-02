package com.pcl.ocr.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DeepAssetUtil {

    private static long prAddress = 0;

    private static final String ApplicationDir = "lpr";
    private static final String CASCADE_FILENAME = "cascade.xml";
    private static final String HORIZONAL_FINEMAPPING_PROTOTXT = "HorizonalFinemapping.prototxt";
    private static final String HORIZONAL_FINEMAPPING_CAFFEMODEL = "HorizonalFinemapping.caffemodel";
    private static final String SEGMENTATION_PROTOTXT = "Segmentation.prototxt";
    private static final String SEGMENTATION_CAFFEMODEL = "Segmentation.caffemodel";
    private static final String RECOGNIZATION_PROTOTXT = "CharacterRecognization.prototxt";
    private static final String RECOGNIZATION_CAFFEMODEL = "CharacterRecognization.caffemodel";
    private static final String FREE_INCEPTION_PROTOTXT = "SegmenationFree-Inception.prototxt";
    private static final String FREE_INCEPTION_CAFFEMODEL = "SegmenationFree-Inception.caffemodel";
    private static String SDCARD_DIR = "";

    private static String getExternalFilesDir(Context context) {
        if (SDCARD_DIR.equals(""))
            SDCARD_DIR = context.getExternalFilesDir(File.separator + ApplicationDir).getAbsolutePath();
        return SDCARD_DIR;
    }

    private static void CopyAssets(Context context, String assetDir, String dir) {
        String[] files;
        try {
            // 获得Assets一共有几多文件
            files = context.getAssets().list(assetDir);
        } catch (IOException e1) {
            return;
        }
        File mWorkingPath = new File(getExternalFilesDir(context));
        Log.d("CopyAssets", mWorkingPath.getAbsolutePath());
        // 如果文件路径不存在
        if (!mWorkingPath.exists()) {
            // 创建文件夹
            if (!mWorkingPath.mkdirs()) {
                throw new IllegalArgumentException("文件夹创建未成功");
            }
        }

        for (String file : files) {
            try {
                // 根据路径判断是文件夹还是文件
                if (!file.contains(".")) {
                    if (0 == assetDir.length()) {
                        CopyAssets(context, file, dir + file + "/");
                    } else {
                        CopyAssets(context, assetDir + "/" + file, dir + "/" + file + "/");
                    }
                    continue;
                }
                File outFile = new File(mWorkingPath, file);
                if (outFile.exists())
                    continue;
                InputStream in;
                if (0 != assetDir.length()) {
                    in = context.getAssets().open(assetDir + "/" + file);
                } else {
                    in = context.getAssets().open(file);
                }

                OutputStream out = new FileOutputStream(outFile);
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //初始化识别资源
    private static long initRecognizer(Context context) {
        String baseDir = getExternalFilesDir(context) + File.separator;
        CopyAssets(context, ApplicationDir, SDCARD_DIR);
        String cascade_filename = baseDir + File.separator + CASCADE_FILENAME;
        String finemapping_prototxt = baseDir + File.separator + HORIZONAL_FINEMAPPING_PROTOTXT;
        String finemapping_caffemodel = baseDir + File.separator + HORIZONAL_FINEMAPPING_CAFFEMODEL;
        String segmentation_prototxt = baseDir + File.separator + SEGMENTATION_PROTOTXT;
        String segmentation_caffemodel = baseDir + File.separator + SEGMENTATION_CAFFEMODEL;
        String character_prototxt = baseDir + File.separator + RECOGNIZATION_PROTOTXT;
        String character_caffemodel = baseDir + File.separator + RECOGNIZATION_CAFFEMODEL;
        String segmentation_free_prototxt = baseDir + File.separator + FREE_INCEPTION_PROTOTXT;
        String segmentation_free_caffemodel = baseDir + File.separator + FREE_INCEPTION_CAFFEMODEL;
        //调用JNI 加载资源函数
        Log.d("initRecognizer", "调用JNI 加载资源函数");
        return PlateRecognition.InitPlateRecognizer(cascade_filename,
                finemapping_prototxt, finemapping_caffemodel,
                segmentation_prototxt, segmentation_caffemodel,
                character_prototxt, character_caffemodel,
                segmentation_free_prototxt, segmentation_free_caffemodel);
    }

    public static long getPRAddress(Context context) {
        if (prAddress == 0) {
            prAddress = initRecognizer(context);
        }
        return prAddress;
    }
}