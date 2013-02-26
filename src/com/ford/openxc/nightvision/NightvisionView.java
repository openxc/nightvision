package com.ford.openxc.nightvision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.ford.openxc.webcam.WebcamPreview;

public class NightvisionView extends WebcamPreview {
    private final static String TAG = "NightvisionView";

    // TODO these are duplicated from the android-webcam library
    private final static int IMG_WIDTH = 640;
    private final static int IMG_HEIGHT = 480;

    private static Paint sOverlayPaint = new Paint();

    private Bitmap mBitmapGray;
    private Bitmap mBitmapEdges;
    private Bitmap mBitmapObjectOverlay;

    private boolean mObjectInPreviousFrame = false;
    private MediaPlayer mMediaPlayer;

    public native void rgbaToGrayscale(Bitmap bitmapcolor, Bitmap bitmapgray);
    public native void detectEdges(Bitmap bitmapgray, Bitmap bitmapedges);
    public native boolean detectObjects(Bitmap bitmapedge, Bitmap bitmapoverlay);

    static {
        System.loadLibrary("nightvision");
        sOverlayPaint.setAlpha(130);
        sOverlayPaint.setColor(Color.RED);
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
                Bitmap.Config.ALPHA_8);
        mBitmapGray = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                Bitmap.Config.ALPHA_8);
        mBitmapEdges = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                Bitmap.Config.ALPHA_8);
    }

    protected void drawOnCanvas(Canvas canvas, Bitmap videoBitmap) {
        rgbaToGrayscale(videoBitmap, mBitmapGray);
        // TODO this is EXTREMELY sensitive, basically everything gets whited
        // out as an edge
        detectEdges(mBitmapGray, mBitmapEdges);

        mBitmapObjectOverlay.eraseColor(Color.TRANSPARENT);
        boolean objectDetected = detectObjects(mBitmapEdges,
                mBitmapObjectOverlay);
        if (!mObjectInPreviousFrame && objectDetected) {
            mMediaPlayer.start();
            mObjectInPreviousFrame = true;
            Log.d(TAG, "Object detected");
        } else if(mObjectInPreviousFrame && !objectDetected) {
            mObjectInPreviousFrame = false;
            Log.d(TAG, "Object left frame");
        }

        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(videoBitmap, null, getViewingWindow(), null);
        canvas.drawBitmap(mBitmapObjectOverlay, null, getViewingWindow(),
                sOverlayPaint);
    }
}
