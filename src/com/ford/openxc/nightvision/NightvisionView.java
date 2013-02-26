package com.ford.openxc.nightvision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;

import com.ford.openxc.webcam.WebcamPreview;

public class NightvisionView extends WebcamPreview {
    private final static String TAG = "NightvisionView";

    private static Paint sOverlayPaint = new Paint();
    private Bitmap mBitmapEdges;
    private Bitmap mBitmapObjectOverlay;
    private boolean mObjectInPreviousFrame = false;
    private MediaPlayer mMediaPlayer;

    public native void detectEdges(Bitmap imageBitmap, Bitmap edgeBitmap);
    public native boolean detectObjects(Bitmap edgeBitmap, Bitmap overlayBitmap);

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

    void initializeBitmaps(Bitmap videoBitmap) {
        if(mBitmapEdges == null ||
                mBitmapEdges.getWidth() != videoBitmap.getWidth() ||
                mBitmapEdges.getHeight() != videoBitmap.getHeight()) {
            mBitmapEdges = Bitmap.createBitmap(videoBitmap.getWidth(),
                    videoBitmap.getHeight(), Bitmap.Config.ALPHA_8);
        }

        if(mBitmapObjectOverlay == null ||
                mBitmapObjectOverlay.getWidth() != videoBitmap.getWidth() ||
                mBitmapObjectOverlay.getHeight() != videoBitmap.getHeight()) {
            mBitmapObjectOverlay = Bitmap.createBitmap(videoBitmap.getWidth(),
                    videoBitmap.getHeight(), Bitmap.Config.ALPHA_8);
        }
    }

    protected void drawOnCanvas(Canvas canvas, Bitmap videoBitmap) {
        initializeBitmaps(videoBitmap);
        detectEdges(videoBitmap, mBitmapEdges);

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
