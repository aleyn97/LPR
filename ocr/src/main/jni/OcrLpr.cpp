#include <jni.h>
#include <string>
#include <android/log.h>
#include "include/Pipeline.h"


#define JAVA_CLASS "com/pcl/ocr/utils/PlateRecognition"
#define  LOG_TAG  "NATIVE_LOG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace std;

string jstring2str(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    string stemp(rtn);
    free(rtn);
    return stemp;
}


jlong InitPlateRecognizer(
        JNIEnv *env, jclass obj,
        jstring detector_filename,
        jstring finemapping_prototxt, jstring finemapping_caffemodel,
        jstring segmentation_prototxt, jstring segmentation_caffemodel,
        jstring charRecognization_proto, jstring charRecognization_caffemodel,
        jstring segmentationfree_proto, jstring segmentationfree_caffemodel) {

    string detector_path = jstring2str(env, detector_filename);
    string finemapping_prototxt_path = jstring2str(env, finemapping_prototxt);
    string finemapping_caffemodel_path = jstring2str(env, finemapping_caffemodel);
    string segmentation_prototxt_path = jstring2str(env, segmentation_prototxt);
    string segmentation_caffemodel_path = jstring2str(env, segmentation_caffemodel);
    string charRecognization_proto_path = jstring2str(env, charRecognization_proto);
    string charRecognization_caffemodel_path = jstring2str(env, charRecognization_caffemodel);
    string segmentationfree_proto_path = jstring2str(env, segmentationfree_proto);
    string segmentationfree_caffemodel_path = jstring2str(env, segmentationfree_caffemodel);

    pr::PipelinePR *PR = new pr::PipelinePR(detector_path,
                                            finemapping_prototxt_path, finemapping_caffemodel_path,
                                            segmentation_prototxt_path,
                                            segmentation_caffemodel_path,
                                            charRecognization_proto_path,
                                            charRecognization_caffemodel_path,
                                            segmentationfree_proto_path,
                                            segmentationfree_caffemodel_path);
    return (jlong) PR;
}


jstring SimpleRecognization(
        JNIEnv *env, jclass obj,
        jlong matPtr, jlong object_pr) {
    pr::PipelinePR *PR = (pr::PipelinePR *) object_pr;
    cv::Mat &mRgb = *(cv::Mat *) matPtr;
    cv::Mat rgb;
//    cv::cvtColor(mRgb,rgb,cv::COLOR_RGBA2GRAY);
    cv::cvtColor(mRgb, rgb, cv::COLOR_RGBA2BGR);
    LOGD("Jni 开始识别");
    try {
        //1表示SEGMENTATION_BASED_METHOD在方法里有说明
        vector<pr::PlateInfo> list_res = PR->RunPiplineAsImage(rgb,
                                                               pr::SEGMENTATION_FREE_METHOD);
        string concat_results;
        for (auto one:list_res) {
            if (one.confidence > 0.7)
                concat_results += one.getPlateName() + ",";
        }
        concat_results = concat_results.substr(0, concat_results.size() - 1);
        LOGD("Jni 成功");
        return env->NewStringUTF(concat_results.c_str());
    } catch (exception e) {
        LOGD("Jni 未成功");
        return env->NewStringUTF("");
    }
}

void ReleasePlateRecognizer(JNIEnv *env, jclass obj,
                            jlong object_re) {
//    pr::PipelinePR *PR = (pr::PipelinePR *) object_re;
//    delete PR;
    LOGD("ReleasePlateRecognizer");
}

int registerNatives(JNIEnv *env, const char *name,
                    JNINativeMethod *method,
                    jint nMethod) {
    jclass jcls;
    jcls = env->FindClass(name);
    if (jcls == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(jcls, method, nMethod) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static JNINativeMethod getMethods[]{
        {"InitPlateRecognizer",    "(Ljava/lang/String;Ljava/lang/String;"
                                   "Ljava/lang/String;Ljava/lang/String;"
                                   "Ljava/lang/String;Ljava/lang/String;"
                                   "Ljava/lang/String;Ljava/lang/String;"
                                   "Ljava/lang/String;)J",   (void *) (InitPlateRecognizer)},
        {"SimpleRecognization",    "(JJ)Ljava/lang/String;", (void *) (SimpleRecognization)},
        {"ReleasePlateRecognizer", "(J)V",                   (void *) (ReleasePlateRecognizer)}
};


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_FALSE;
    }
    jint size = sizeof(getMethods) / sizeof(JNINativeMethod);
    registerNatives(env, JAVA_CLASS, getMethods, size);
    LOGD("Methods: %d", size);
    return JNI_VERSION_1_6;
}
