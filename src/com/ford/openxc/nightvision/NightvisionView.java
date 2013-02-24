package com.ford.openxc.nightvision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;

import com.ford.openxc.webcam.WebcamPreview;

public class NightvisionView extends WebcamPreview {

    private static String TAG = "NightvisionView";
    public static final String NO_CAMERA_DETECTED = "com.ford.openxc.NO_CAMERA_DETECTED";

    // TODO these are in so many places...
    static final int IMG_WIDTH = 640;
    static final int IMG_HEIGHT = 480;

    private Bitmap mBitmapVideo = null;
    private Bitmap mBitmapGray = null;
    private Bitmap mBitmapEdges = null;
    private Bitmap mBitmapObjectOverlay = null;

    private boolean everyOtherFrame = true;
    private boolean objectDetected = false;
    private boolean objectInPrevFrame = false;
    private float rate;

    public native void toGrayscale(Bitmap bitmapcolor, Bitmap bitmapgray);
    public native void pixeltomBitmapVideo(Bitmap bitmap);
    public native void detectEdges(Bitmap bitmapgray, Bitmap bitmapedges);
    public native void showBitmap(Bitmap bitmapedges, Bitmap bitmapout);

    static {
        System.loadLibrary("nightvision");
    }

    public NightvisionView(Context context) {
        super(context);
    }

    @Override
    public void run() {
        MediaPlayer mp = MediaPlayer.create(mContext, R.raw.alert);
        while(mRunning) {
            synchronized(mServiceSyncToken) {
                if(mWebcamManager == null) {
                    try {
                        mServiceSyncToken.wait();
                    } catch(InterruptedException e) {
                        break;
                    }
                }

                if(!mWebcamManager.cameraAttached()) {
                    mRunning = false;
                }

                mBitmapVideo = mWebcamManager.getImage();

                toGrayscale(mBitmapVideo, mBitmapGray);
                detectEdges(mBitmapGray, mBitmapEdges);
                showBitmap(mBitmapEdges, mBitmapVideo);

                Paint overlayPaint = new Paint();
                overlayPaint.setAlpha(0);

                if(NightVisionActivity.getAudioOption()) {
                    if (!objectInPrevFrame && objectDetected) {
                        mp.start();
                        objectInPrevFrame = true;
                    }
                    else if (objectInPrevFrame && !objectDetected) objectInPrevFrame = false;
                }

                if (NightVisionActivity.getObjectDetectionOption()) {
                    overlayPaint.setAlpha(130);
                    if (everyOtherFrame == true) {
                        everyOtherFrame = false;
                        objectDetect();
                    }
                    else everyOtherFrame = true;
                }

                if (!NightVisionActivity.getVideoOption()) {
                    showBitmap(mBitmapGray, mBitmapVideo);
                }

                Canvas canvas = mHolder.lockCanvas();
                if(canvas != null) {
                    canvas.drawColor(Color.BLACK);
                    canvas.drawBitmap(mBitmapVideo, null, mViewWindow, null);
                    canvas.drawBitmap(mBitmapObjectOverlay, null, mViewWindow, overlayPaint);
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        mBitmapObjectOverlay = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
        mBitmapGray = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ALPHA_8);
        mBitmapEdges = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ALPHA_8);
    }

    /** Simple Object Detection
     *
     * This object detection limits the detection to a bounded area of interest
     * that is the middle 50% of the screen. It then iterates through the bounded
     * area looking at a smaller area of xBound*yBound. If that area contains more
     * than 40% white pixels (edges) then the area is considered part of an object
     * and it is marked so in the mBitmapObjectOverlay.
     *
     * */
    public void objectDetect() {
        int x, y, i, j, sum;
        int xBound = 8;
        int yBound = 8;
        int[] overlaySection = new int [xBound * yBound];
        mBitmapObjectOverlay.eraseColor(Color.TRANSPARENT);
        objectDetected = false;

        for (y=(int) (IMG_HEIGHT*.25); y < IMG_HEIGHT*.75; y+=(yBound/2)) {
            for (x=(int) (IMG_WIDTH*.25); x < IMG_WIDTH*.75; x+=(xBound/2)) {
                sum =0;
                for (i=0; i < xBound; i++) {
                    for (j=0 ;j < yBound; j++) {
                        if (mBitmapVideo.getPixel((x+i),(y+j)) == Color.WHITE) {
                            sum++;
                            overlaySection[i+ (j*(yBound))] = -256;
                        }
                    }
                }

                if (sum > (yBound*xBound*.4)) {
                    mBitmapObjectOverlay.setPixels(overlaySection, 0, xBound, x, y, xBound, yBound);
                    objectDetected = true;
                }
            }
        }
    }
}
