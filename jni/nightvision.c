#include "nightvision.h"
#include <stdbool.h>

uint8_t rgbToGrayscale(argb* pixel) {
    return 0.3 * pixel->red + 0.59 * pixel->green + 0.11 * pixel->blue;
}

/** Simple Object Detection
 *
 * This object detection limits the detection to a bounded area of interest
 * that is the middle 50% of the screen. It then iterates through the
 * bounded area looking at a smaller area of OBJECT_DETECT_BLOCK_SIZE_X *
 * OBJECT_DETECT_BLOCK_SIZE_Y. If that area contains more than 40% white
 * pixels (edges) then the area is considered part of an object and it is
 * marked so in the mBitmapObjectOverlay.
 */
jboolean Java_com_ford_openxc_nightvision_NightvisionView_detectObjects(
        JNIEnv* env, jobject thiz, jobject edgeBitmap, jobject overlayBitmap) {
    AndroidBitmapInfo edgeInfo;
    int result;
    if((result = AndroidBitmap_getInfo(env, edgeBitmap, &edgeInfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", result);
        return false;
    }

    AndroidBitmapInfo overlayInfo;
    if((result = AndroidBitmap_getInfo(env, overlayBitmap, &overlayInfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", result);
        return false;
    }

    void* edgePixels;
    if((result = AndroidBitmap_lockPixels(env, edgeBitmap, &edgePixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", result);
        return false;
    }

    void* overlayPixels;
    if((result = AndroidBitmap_lockPixels(env, overlayBitmap,
                    &overlayPixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", result);
        AndroidBitmap_unlockPixels(env, edgeBitmap);
        return false;
    }

    const int OBJECT_DETECT_BLOCK_SIZE_X = 8;
    const int OBJECT_DETECT_BLOCK_SIZE_Y = 8;
    const int OBJECT_DETECT_BLOCK_AREA = OBJECT_DETECT_BLOCK_SIZE_X
            * OBJECT_DETECT_BLOCK_SIZE_Y;

    bool objectDetected = false;
    for(int y = (int) (edgeInfo.height * DETECTION_WINDOW_SIZE);
            y < edgeInfo.height * (1 - DETECTION_WINDOW_SIZE);
            y += (OBJECT_DETECT_BLOCK_SIZE_Y / 2)) {
        uint8_t* edgeline = (uint8_t*) (edgePixels + edgeInfo.stride * y);
        uint8_t* overlayline = (uint8_t*) (overlayPixels + overlayInfo.stride *
                y);
        for(int x = (int) (edgeInfo.width * DETECTION_WINDOW_SIZE);
                x < edgeInfo.width * (1 - DETECTION_WINDOW_SIZE);
                x += (OBJECT_DETECT_BLOCK_SIZE_X / 2)) {
            int sum = 0;
            for(int i = 0; i < OBJECT_DETECT_BLOCK_SIZE_X; i++) {
                for(int j = 0; j < OBJECT_DETECT_BLOCK_SIZE_Y; j++) {
                    uint8_t pixel = *(edgeline + x + i + j *
                            overlayInfo.stride);
                    if(pixel == 255) {
                        sum++;
                    }
                }
            }

            if (sum > OBJECT_DETECT_BLOCK_AREA * .4) {
                for(int i = 0; i < OBJECT_DETECT_BLOCK_SIZE_X; i++) {
                    for(int j = 0; j < OBJECT_DETECT_BLOCK_SIZE_Y; j++) {
                        uint8_t pixel = *(edgeline + x + i + j *
                                edgeInfo.stride);
                        *(overlayline + x + i + j * overlayInfo.stride) = 255;
                    }
                }
                objectDetected = true;
            }
        }
    }

    AndroidBitmap_unlockPixels(env, edgeBitmap);
    AndroidBitmap_unlockPixels(env, overlayBitmap);

    return objectDetected;
}

/** This function uses a variant of the Sobel operator to detect the edges in
 * the bitmap. It calculates the gradient of the image intensity at each point.
 * The result therefore shows how "abruptly" or "smoothly" the image changes at
 * that point, and therefore how likely it is that that part of the image
 * represents an edge, as well as how that edge is likely to be oriented. The
 * operator uses two 3×3 matrices which are convolved with the original image to
 * calculate approximations of the derivatives - one for horizontal changes, and
 * one for vertical.
 *
 * The image bitmap is assumed to be RGB, and this converts each pixel to
 * grayscale before running the algorithm.
 */
void Java_com_ford_openxc_nightvision_NightvisionView_detectEdges(JNIEnv* env,
        jobject thiz, jobject imageBitmap, jobject edgeBitmap) {
    int Gx[3][3] = {
        {-1, 0, 1},
        {-2, 0, 2},
        {-1, 0, 1}
    };

    int Gy[3][3]  = {
        {1, 2, 1},
        {0, 0, 0},
        {-1, -2, -1}
    };

    uint8_t* imagePixels;
    int ret;
    if((ret = AndroidBitmap_lockPixels(env, imageBitmap,
                    (void*)&imagePixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    AndroidBitmapInfo imageInfo;
    if((ret = AndroidBitmap_getInfo(env, imageBitmap, &imageInfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    uint8_t* edgePixels;
    if((ret = AndroidBitmap_lockPixels(env, edgeBitmap,
                    (void*)&edgePixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    AndroidBitmapInfo edgeInfo;
    if((ret = AndroidBitmap_getInfo(env, edgeBitmap, &edgeInfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    for(int y = (int) (imageInfo.height * DETECTION_WINDOW_SIZE);
            y < imageInfo.height * (1 - DETECTION_WINDOW_SIZE);
            y++) {
        for(int x = (int) (edgeInfo.width * DETECTION_WINDOW_SIZE);
                x < edgeInfo.width * (1 - DETECTION_WINDOW_SIZE);
                x++) {
            int sumX = 0, sumY = 0;
            // calc X and Y gradients
            for(int i = -1; i <= 1; i++) {
                for(int j = -1; j <= 1; j++) {
                    argb* imageLine = (argb*)(imagePixels +
                            imageInfo.stride * (y + i));
                    uint8_t pixel = rgbToGrayscale(&imageLine[x]);
                    sumX += pixel * Gx[i + 1][j + 1];
                    sumY += pixel * Gy[i + 1][j + 1];
                }
            }

            int sum = abs(sumX) + abs(sumY);
            if(sum > 255) {
                sum = 255;
            } else if(sum < 0) {
                sum = 0;
            }
            *(edgePixels + x + y * edgeInfo.stride) = (uint8_t) sum;
        }
    }

    AndroidBitmap_unlockPixels(env, imageBitmap);
    AndroidBitmap_unlockPixels(env, edgeBitmap);
}
