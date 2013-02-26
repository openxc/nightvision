#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>

#define LOG_TAG "Nightvision"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t alpha;
} argb;

void Java_com_ford_openxc_nightvision_NightvisionView_detectEdges(JNIEnv* env,
        jobject thiz, jobject bitmapcolor, jobject bitmapedges);
void Java_com_ford_openxc_nightvision_NightvisionView_grayscaleToRGBA(JNIEnv* env,
        jobject thiz, jobject bitmapedge, jobject bitmapout);
void Java_com_ford_openxc_nightvision_NightvisionView_rgbaToGrayscale(JNIEnv* env,
        jobject thiz, jobject bitmapcolor, jobject bitmapgray);
