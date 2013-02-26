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
    private final int DETECT_FRAME_FREQUENCY = 4;

    private static Paint sOverlayPaint = new Paint();
    private Bitmap mBitmapEdges;
    private Bitmap mBitmapObjectOverlay;
    private boolean mObjectInPreviousFrame = false;
    private MediaPlayer mMediaPlayer;
    private int mFrameCount;

    /** Detect edges in the image using the Sobel operator and store the result
     * in a new alpha bitmap.
     *
     * The Sobel operator calculates the gradient of the image intensity at each
     * point. The result therefore shows how "abruptly" or "smoothly" the image
     * changes at that point, and therefore how likely it is that that part of
     * the image represents an edge, as well as how that edge is likely to be
     * oriented. The operator uses two 3x3 matrices which are convolved with the
     * original image to calculate approximations of the derivatives - one for
     * horizontal changes, and one for vertical.
     *
     * @param imageBitmap Source image for edge detection, assumed to be ARGB.
     * @param edgeBitmap Target for edge detection output, will be modified.
     */
    public native void detectEdges(final Bitmap imageBitmap, Bitmap edgeBitmap);

    /** Detect clusters of edges in a bitmap and highlight the output in a
     * separate bitmap to show objects.
     *
     * This function performs rudimentary object detection using a bitmap
     * of edges identified in some unseen source image. The detection is bounded
     * within an area in the middle of the image (see DETECTION_WINDOW_SIZE in
     * the JNI part) for performance and to minimize false positives, since the
     * area of interest for app is the road ahead at the horizon.
     *
     * Blocks of a reasonable size within these bounds that contain more than
     * 40% edges are considered to be "objects". The block is shaded in the
     * output bitmap, which can then be used as an overlay for the source image.
     *
     * @param edgeBitmap Input bitmap that highlights edges in pure white, and
     *      everything else black.
     * @param overlayBitmap Bitmap to store object highlight output.
     * @return true if at least one object is detected
     */
    public native boolean detectObjects(final Bitmap edgeBitmap,
            Bitmap overlayBitmap);

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

    protected void drawOnCanvas(Canvas canvas, Bitmap videoBitmap) {
        initializeBitmaps(videoBitmap);
        canvas.drawColor(Color.BLACK);

        if(mFrameCount == DETECT_FRAME_FREQUENCY) {
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

            mFrameCount = 0;
        } else {
            mFrameCount++;
        }

        canvas.drawBitmap(videoBitmap, null, getViewingWindow(), null);
        canvas.drawBitmap(mBitmapObjectOverlay, null, getViewingWindow(),
                sOverlayPaint);
    }

    private void init() {
        mMediaPlayer = MediaPlayer.create(getContext(), R.raw.alert);
    }

    private void initializeBitmaps(Bitmap videoBitmap) {
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
}
