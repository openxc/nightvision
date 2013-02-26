#include "nightvision.h"
#include <stdbool.h>

void Java_com_ford_openxc_nightvision_NightvisionView_rgbaToGrayscale(JNIEnv* env,
        jobject thiz, jobject bitmapcolor, jobject bitmapgray) {
    int result;
    AndroidBitmapInfo infocolor;
    if((result = AndroidBitmap_getInfo(env, bitmapcolor, &infocolor)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", result);
        return;
    }

    AndroidBitmapInfo infogray;
    if((result = AndroidBitmap_getInfo(env, bitmapgray, &infogray)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", result);
        return;
    }

    void* pixelscolor;
    if((result = AndroidBitmap_lockPixels(env, bitmapcolor, &pixelscolor)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", result);
        return;
    }

    void* pixelsgray;
    if((result = AndroidBitmap_lockPixels(env, bitmapgray, &pixelsgray)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", result);
        AndroidBitmap_unlockPixels(env, bitmapcolor);
        return;
    }

    // modify pixels with image processing algorithm
    for(int y = 0; y < infocolor.height; y++) {
        argb* colorLine = (argb*) (pixelscolor + infocolor.stride * y);
        uint8_t* grayscaleLine = (uint8_t*) (pixelsgray + infogray.stride * y);

        for(int x = 0; x < infocolor.width; x++) {
            grayscaleLine[x] = 0.3 * colorLine[x].red
                + 0.59 * colorLine[x].green
                + 0.11 * colorLine[x].blue;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapcolor);
    AndroidBitmap_unlockPixels(env, bitmapgray);
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
jboolean Java_com_ford_openxc_nightvision_NightvisionView_detectObjects(JNIEnv* env,
        jobject thiz, jobject bitmapedge, jobject bitmapoverlay) {
    AndroidBitmapInfo infoedge;
    int result;
    if((result = AndroidBitmap_getInfo(env, bitmapedge, &infoedge)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", result);
        return false;
    }

    AndroidBitmapInfo infooverlay;
    if((result = AndroidBitmap_getInfo(env, bitmapoverlay, &infooverlay)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", result);
        return false;
    }

    void* pixelsedge;
    if((result = AndroidBitmap_lockPixels(env, bitmapedge, &pixelsedge)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", result);
        return false;
    }

    void* pixelsoverlay;
    if((result = AndroidBitmap_lockPixels(env, bitmapoverlay, &pixelsoverlay)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", result);
        AndroidBitmap_unlockPixels(env, bitmapedge);
        return false;
    }

    const int OBJECT_DETECT_BLOCK_SIZE_X = 8;
    const int OBJECT_DETECT_BLOCK_SIZE_Y = 8;
    const int OBJECT_DETECT_BLOCK_AREA = OBJECT_DETECT_BLOCK_SIZE_X
            * OBJECT_DETECT_BLOCK_SIZE_Y;
    int OBJECT_DETECT_BLOCK[OBJECT_DETECT_BLOCK_AREA];

    bool objectDetected = false;
    for(int y = (int) (infoedge.height * .25); y < infoedge.height * .75;
            y += (OBJECT_DETECT_BLOCK_SIZE_Y / 2)) {
        uint8_t* edgeline = (uint8_t*) (pixelsedge + infoedge.stride * y);
        uint8_t* overlayline = (uint8_t*) (pixelsoverlay + infooverlay.stride * y);
        for(int x = (int) (infoedge.width * .25); x < infoedge.width * .75;
                x += (OBJECT_DETECT_BLOCK_SIZE_X / 2)) {
            int sum = 0;
            for(int i = 0; i < OBJECT_DETECT_BLOCK_SIZE_X; i++) {
                for(int j = 0; j < OBJECT_DETECT_BLOCK_SIZE_Y; j++) {
                    uint8_t pixel = *(edgeline + x + i + j * infooverlay.stride);
                    if(pixel == 255) {
                        sum++;
                    }
                }
            }

            // TODO this is skipping lines because of the outer loop, so the
            // overlay box is a little sketchy
            if (sum > OBJECT_DETECT_BLOCK_AREA * .4) {
                for(int i = 0; i < OBJECT_DETECT_BLOCK_SIZE_X; i++) {
                    for(int j = 0 ; j < OBJECT_DETECT_BLOCK_SIZE_Y; j++) {
                        uint8_t pixel = *(edgeline + x + i + j * infooverlay.stride);
                        *(overlayline + x + i + j) = 255;
                    }
                }
                objectDetected = true;
            }
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapedge);
    AndroidBitmap_unlockPixels(env, bitmapoverlay);

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
 */
void Java_com_ford_openxc_nightvision_NightvisionView_detectEdges(JNIEnv* env,
        jobject thiz, jobject bitmapgray, jobject bitmapedges) {
    int ret;
    int Gx[3][3];
    int Gy[3][3];

    Gx[0][0] = -1;Gx[0][1] = 0;Gx[0][2] = 1;
    Gx[1][0] = -2;Gx[1][1] = 0;Gx[1][2] = 2;
    Gx[2][0] = -1;Gx[2][1] = 0;Gx[2][2] = 1;

    Gy[0][0] = 1;Gy[0][1] = 2;Gy[0][2] = 1;
    Gy[1][0] = 0;Gy[1][1] = 0;Gy[1][2] = 0;
    Gy[2][0] = -1;Gy[2][1] = -2;Gy[2][2] = -1;

    uint8_t* pixelsgray;
    if((ret = AndroidBitmap_lockPixels(env, bitmapgray, (void*)&pixelsgray)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    uint8_t* pixelsedge;
    if((ret = AndroidBitmap_lockPixels(env, bitmapedges, (void*)&pixelsedge)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    AndroidBitmapInfo infogray;
    if((ret = AndroidBitmap_getInfo(env, bitmapgray, &infogray)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        AndroidBitmap_unlockPixels(env, bitmapedges);
        return;
    }

    for(int y = 1; y < infogray.height - 2; y++) {
        for(int x = 1; x < infogray.width - 2; x++) {
            int sumX = 0, sumY = 0;
            // calc X and Y gradients
            for(int i = -1; i <= 1; i++) {
                for(int j = -1; j <= 1; j++) {
                    uint8_t pixel = *(pixelsgray + x + i +
                            ((y + j) * infogray.stride));
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
            *(pixelsedge + x + y * infogray.stride) = (uint8_t) sum;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapgray);
    AndroidBitmap_unlockPixels(env, bitmapedges);
}
