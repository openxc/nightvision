#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <string.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <fcntl.h>              /* low-level i/o */
#include <unistd.h>
#include <errno.h>
#include <malloc.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/mman.h>
#include <sys/ioctl.h>

#include <stddef.h>
#include <asm/types.h>          /* for videodev2.h */

#include <linux/videodev2.h>
#include <linux/usbdevice_fs.h>

#define LOG_TAG "Nightvision"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
    uint8_t alpha;
} argb;

void Java_com_ford_openxc_nightvision_NightvisionView_detectEdges( JNIEnv* env,jobject thiz,jobject bitmapcolor, jobject bitmapedges);
void Java_com_ford_openxc_nightvision_NightvisionView_showBitmap( JNIEnv* env,jobject thiz,jobject bitmapedge, jobject bitmapout);
void Java_com_ford_openxc_nightvision_NightvisionView_toGrayscale( JNIEnv* env,jobject thiz,jobject bitmapcolor, jobject bitmapgray);
