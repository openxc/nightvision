#include "ImageProc.h"

void Java_com_camera_nightvision_CameraPreview_toGrayscale( JNIEnv* env,jobject thiz,jobject bitmapcolor, jobject bitmapgray){
    AndroidBitmapInfo    infocolor;
    void*                pixelscolor;
    AndroidBitmapInfo    infogray;
    void*                pixelsgray;
    int                    ret;
    int                    y;
    int                    x;

    if ((ret = AndroidBitmap_getInfo(env, bitmapcolor, &infocolor)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_getInfo(env, bitmapgray, &infogray)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmapcolor, &pixelscolor)) < 0)
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    if ((ret = AndroidBitmap_lockPixels(env, bitmapgray, &pixelsgray)) < 0)
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    // modify pixels with image processing algorithm
    for (y=0;y<infocolor.height;y++) {
        argb * line = (argb *) pixelscolor;
        uint8_t * grayline = (uint8_t *) pixelsgray;
        for (x=0;x<infocolor.width;x++) {
            grayline[x] = 0.3 * line[x].red + 0.59 * line[x].green + 0.11*line[x].blue;
        }

        pixelscolor = (char *)pixelscolor + infocolor.stride;
        pixelsgray = (char *) pixelsgray + infogray.stride;
    }

    AndroidBitmap_unlockPixels(env, bitmapcolor);
    AndroidBitmap_unlockPixels(env, bitmapgray);

}

//This function uses a variant of the Sobel operator to detect the edges in the bitmap. It
//calculates the gradient of the image intensity at each point.
//The result therefore shows how "abruptly" or "smoothly" the image changes at that point,
//and therefore how likely it is that that part of the image represents an edge, as well
//as how that edge is likely to be oriented. The operator uses two 3×3 matrices which are
//convolved with the original image to calculate approximations of the derivatives - one
//for horizontal changes, and one for vertical.
void Java_com_camera_nightvision_CameraPreview_detectEdges( JNIEnv* env,jobject thiz,jobject bitmapgray, jobject bitmapedges){

    AndroidBitmapInfo    infogray;
    void*                pixelsgray;
    AndroidBitmapInfo    infoedges;
    void*                pixelsedge;
    int                    ret;
    int                    y;
    int                    x;
    int                    sumX,sumY,sum;
    int                    i,j;
    int                    Gx[3][3];
    int                    Gy[3][3];
    uint8_t                *graydata;
    uint8_t                *edgedata;

    Gx[0][0] = -1;Gx[0][1] = 0;Gx[0][2] = 1;
    Gx[1][0] = -2;Gx[1][1] = 0;Gx[1][2] = 2;
    Gx[2][0] = -1;Gx[2][1] = 0;Gx[2][2] = 1;

    Gy[0][0] = 1;Gy[0][1] = 2;Gy[0][2] = 1;
    Gy[1][0] = 0;Gy[1][1] = 0;Gy[1][2] = 0;
    Gy[2][0] = -1;Gy[2][1] = -2;Gy[2][2] = -1;

    if ((ret = AndroidBitmap_getInfo(env, bitmapgray, &infogray)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_getInfo(env, bitmapedges, &infoedges)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmapgray, &pixelsgray)) < 0)
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    if ((ret = AndroidBitmap_lockPixels(env, bitmapedges, &pixelsedge)) < 0)
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    // modify pixels with image processing algorithm
    graydata = (uint8_t *) pixelsgray;
    edgedata = (uint8_t *) pixelsedge;

    for (y=0;y<=infogray.height - 1;y++) {
        for (x=0;x<infogray.width -1;x++) {
            sumX = 0;
            sumY = 0;

            // check boundaries
            if (y==0 || y == infogray.height-1) sum = 0;
            else if (x == 0 || x == infogray.width -1) sum = 0;
            else {
                // calc X gradient
                for (i=-1;i<=1;i++) {
                    for (j=-1;j<=1;j++) {
                        sumX += (int) ( (*(graydata + x + i + (y + j)
                                * infogray.stride)) * Gx[i+1][j+1]);
                    }
                }

                // calc Y gradient
                for (i=-1;i<=1;i++) {
                    for (j=-1;j<=1;j++) {
                        sumY += (int) ( (*(graydata + x + i + (y + j)
                                * infogray.stride)) * Gy[i+1][j+1]);
                    }
                }

                sum = abs(sumX) + abs(sumY);
            }

            if (sum>200) sum = 255;
            if (sum<50) sum = 0;
            *(edgedata + x + y*infogray.width) = 0+ (uint8_t) sum;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapgray);
    AndroidBitmap_unlockPixels(env, bitmapedges);
}

//This method converts the ALPHA_8 bitmap back to the RGBA format (image is
// grayscale) for Android to be able to show it on the canvas
void Java_com_camera_nightvision_CameraPreview_showBitmap( JNIEnv* env,jobject thiz,jobject bitmapedge, jobject bitmapshow){
    AndroidBitmapInfo    infoedge;
    void*                pixelsedge;
    AndroidBitmapInfo    infoshow;
    void*                pixelsshow;
    int                    ret;
    int                    y;
    int                    x;
    uint8_t                *edgeline;
    argb                *showline;

    if ((ret = AndroidBitmap_getInfo(env, bitmapedge, &infoedge)) < 0)  {
        LOGE("AndroidBitmap_getInfo() failed 1 ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_getInfo(env, bitmapshow, &infoshow)) < 0)  {
        LOGE("AndroidBitmap_getInfo() failed 2 ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmapedge, &pixelsedge)) < 0)
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    if ((ret = AndroidBitmap_lockPixels(env, bitmapshow, &pixelsshow)) < 0)
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

    for(y = 0; y < infoedge.height; y++) {
        edgeline = (uint8_t *)pixelsedge;
        showline = (argb *)pixelsshow;

        for(x = 0; x < infoedge.width; x++) {
            showline[x].alpha = edgeline[x];
            showline[x].red = edgeline[x];
            showline[x].green = edgeline[x];
            showline[x].blue = edgeline[x];
        }

        pixelsedge = (char *)pixelsedge + infoedge.stride;
        pixelsshow = (char *) pixelsshow + infoshow.stride;
    }

    AndroidBitmap_unlockPixels(env, bitmapshow);
    AndroidBitmap_unlockPixels(env, bitmapedge);
}
