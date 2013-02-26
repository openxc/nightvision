package com.ford.openxc.nightvision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.ford.openxc.webcam.WebcamPreview;

public class NightvisionView extends WebcamPreview {
    private final static String TAG = "NightvisionView";
    private final static int OBJECT_DETECT_BLOCK_SIZE_X = 8;
    private final static int OBJECT_DETECT_BLOCK_SIZE_Y = 8;

    // TODO these are duplicated from the android-webcam library
    private final static int IMG_WIDTH = 640;
    private final static int IMG_HEIGHT = 480;

    private Bitmap mBitmapGray = null;
    private Bitmap mBitmapEdges = null;
    private Bitmap mBitmapObjectOverlay = null;

    private boolean objectInPrevFrame = false;
    private MediaPlayer mMediaPlayer;

    public native void toGrayscale(Bitmap bitmapcolor, Bitmap bitmapgray);
    public native void detectEdges(Bitmap bitmapgray, Bitmap bitmapedges);
    public native void showBitmap(Bitmap bitmapedges, Bitmap bitmapout);

    static {
        System.loadLibrary("nightvision");
    }

    public NightvisionView(Context context) {
        super(context);
        init();
    }

    public NightvisionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mMediaPlayer = MediaPlayer.create(getContext(), R.raw.alert);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int winWidth,
            int winHeight) {
        super.surfaceChanged(holder, format, winWidth, winHeight);
        mBitmapObjectOverlay = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                Bitmap.Config.ARGB_8888);
        mBitmapGray = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                Bitmap.Config.ALPHA_8);
        mBitmapEdges = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                Bitmap.Config.ALPHA_8);
    }

    protected void drawOnCanvas(Canvas canvas, Bitmap videoBitmap) {
        toGrayscale(videoBitmap, mBitmapGray);
        detectEdges(mBitmapGray, mBitmapEdges);
        showBitmap(mBitmapEdges, videoBitmap);

        Paint overlayPaint = new Paint();
        overlayPaint.setAlpha(0);

        boolean objectDetected = detectObjects(videoBitmap,
                mBitmapObjectOverlay);
        if (!objectInPrevFrame && objectDetected) {
            mMediaPlayer.start();
            objectInPrevFrame = true;
        } else if (objectInPrevFrame && !objectDetected) {
            objectInPrevFrame = false;
        }

        overlayPaint.setAlpha(130);
        canvas.drawBitmap(videoBitmap, null, getViewingWindow(), overlayPaint);
        canvas.drawBitmap(mBitmapObjectOverlay, null, getViewingWindow(),
                overlayPaint);
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
    public boolean detectObjects(Bitmap videoBitmap, Bitmap overlayBitmap) {
        overlayBitmap.eraseColor(Color.TRANSPARENT);

        boolean objectDetected = false;
        int[] overlaySection = new int [OBJECT_DETECT_BLOCK_SIZE_X *
                OBJECT_DETECT_BLOCK_SIZE_Y];
        for(int y = (int) (IMG_HEIGHT * .25); y < IMG_HEIGHT * .75;
                    y += (OBJECT_DETECT_BLOCK_SIZE_Y / 2)) {
            for(int x = (int) (IMG_WIDTH * .25); x < IMG_WIDTH * .75;
                    x += (OBJECT_DETECT_BLOCK_SIZE_X / 2)) {
                int sum = 0;
                for(int i = 0; i < OBJECT_DETECT_BLOCK_SIZE_X; i++) {
                    for(int j=0 ; j < OBJECT_DETECT_BLOCK_SIZE_Y; j++) {
                        if(videoBitmap.getPixel((x + i), (y + j))
                                == Color.WHITE) {
                            sum++;
                            overlaySection[i + j *
                                    OBJECT_DETECT_BLOCK_SIZE_Y] = -256;
                        }
                    }
                }

                if (sum > (OBJECT_DETECT_BLOCK_SIZE_Y *
                            OBJECT_DETECT_BLOCK_SIZE_X * .4)) {
                    overlayBitmap.setPixels(overlaySection, 0,
                            OBJECT_DETECT_BLOCK_SIZE_X,
                            x, y,
                            OBJECT_DETECT_BLOCK_SIZE_X,
                            OBJECT_DETECT_BLOCK_SIZE_Y);
                    objectDetected = true;
                }
            }
        }
        return objectDetected;
    }
}
