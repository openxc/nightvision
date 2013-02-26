#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>

#define LOG_TAG "NightVisionJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define OBJECT_DETECT_BLOCK_SIZE_X 8
#define OBJECT_DETECT_BLOCK_SIZE_Y 8
#define OBJECT_DETECT_BLOCK_AREA (OBJECT_DETECT_BLOCK_SIZE_X * \
        OBJECT_DETECT_BLOCK_SIZE_Y)

// The minimum number of edge pixels required to find an object
#define OBJECT_EDGE_THRESHOLD OBJECT_DETECT_BLOCK_AREA * .4

// The percentage to cut off top/bottom/left/right of the image before running
// object and edge detection
#define DETECTION_WINDOW_SIZE (.5 / 2)

typedef struct {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t alpha;
} argb;

void Java_com_ford_openxc_nightvision_NightVisionView_detectEdges(JNIEnv* env,
        jobject thiz, jobject bitmapcolor, jobject bitmapedges);
jboolean Java_com_ford_openxc_nightvision_NightVisionView_detectObjects(
        JNIEnv* env, jobject thiz, jobject bitmapedge, jobject bitmapoverlay);
