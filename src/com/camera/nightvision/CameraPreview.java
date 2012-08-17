package com.camera.nightvision;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final String NO_CAMERA_DETECTED = "com.ford.openxc.NO_CAMERA_DETECTED";
    private static String TAG = "CameraPreview";
    private static final boolean DEBUG = true;
    protected Context context;
    private SurfaceHolder holder;
    Thread mainLoop = null;
    private Bitmap bmp = null;
    private Bitmap bmpgray = null;
    private Bitmap bmpedges = null;
    private Bitmap objectOverlaybmp = null;

    private boolean everyOtherFrame = true;
    private boolean objectDetected = false;
    private boolean objectInPrevFrame = false;
    private int xStartBound = (int) (IMG_WIDTH *.25);
    private int yStartBound = (int) (IMG_HEIGHT *.25);
    private int xEndBound = (int) (IMG_WIDTH *.75);
    private int yEndBound = (int) (IMG_HEIGHT *.75);

    private boolean cameraExists = false;
    private boolean shouldStop = false;

    // /dev/videox (x=cameraId+cameraBase) is used.
    // In some omap devices, system uses /dev/video[0-3],
    // so users must use /dev/video[4-].
    // In such a case, try cameraId=0 and cameraBase=4
    private int cameraId=0;
    private int cameraBase=0;

    // This definition also exists in ImageProc.h.
    // Webcam must support the resolution 640x480 with YUYV format. 
    static final int IMG_WIDTH=320;
    static final int IMG_HEIGHT=240;

    // The following variables are used to draw camera images.
    private int winWidth=0;
    private int winHeight=0;
    private Rect rect;
    private int dw, dh;
    private float rate;

    // JNI functions
    public native int prepareCamera(int videoid);
    public native int prepareCameraWithBase(int videoid, int camerabase);
    public native void processCamera();
    public native void stopCamera();
    public native void toGrayscale(Bitmap bitmapcolor, Bitmap bitmapgray);
    public native void pixeltobmp(Bitmap bitmap);
    public native void detectEdges(Bitmap bitmapgray, Bitmap bitmapedges);
    public native void showBitmap(Bitmap bitmapedges, Bitmap bitmapout);
    static {
        System.loadLibrary("ImageProc");
    }

    
    CameraPreview(Context context) {
        super(context);
        this.context = context;
        if(DEBUG) Log.d(TAG,"CameraPreview constructed");
        setFocusable(true);

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);	
    }

    
    @Override
    public void run() {
        if(cameraExists) {
            MediaPlayer mp = MediaPlayer.create(this.context, R.raw.alert);  
            while (true && cameraExists) {

                if(winWidth==0) {
                    winWidth=this.getWidth();
                    winHeight=this.getHeight();

                    if(winWidth*3/4<=winHeight) {
                        dw = 0;
                        dh = (winHeight-winWidth*3/4)/2;
                        rate = ((float)winWidth)/IMG_WIDTH;
                        rect = new Rect(dw,dh,dw+winWidth-1,dh+winWidth*3/4-1);
                    }
                    else {
                        dw = (winWidth-winHeight*4/3)/2;
                        dh = 0;
                        rate = ((float)winHeight)/IMG_HEIGHT;
                        rect = new Rect(dw,dh,dw+winHeight*4/3 -1,dh+winHeight-1);
                    }
                }

                processCamera();
                pixeltobmp(bmp);
                toGrayscale(bmp, bmpgray);
                detectEdges(bmpgray, bmpedges);
                showBitmap(bmpedges, bmp);

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

                if (!NightVisionActivity.getVideoOption()) showBitmap(bmpgray, bmp);

                Canvas canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK);
                    canvas.drawBitmap(bmp,null,rect,null);
                    canvas.drawBitmap(objectOverlaybmp, null, rect, overlayPaint);
                    getHolder().unlockCanvasAndPost(canvas);
                }

                if(shouldStop) {
                    shouldStop = false;  
                    break;
                }	        
            }
            mp.release();
        }

        else {      
            Intent noCameraDetectedIntent = new Intent(NO_CAMERA_DETECTED);
            context.sendBroadcast(noCameraDetectedIntent);
            Log.i(TAG, "No Camera Detected Intent Sent");
        }
    }

    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(DEBUG) Log.d(TAG, "surfaceCreated");

        if(bmp==null) bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);

        if(objectOverlaybmp==null) objectOverlaybmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);

        if(bmpgray==null) bmpgray = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ALPHA_8);

        if(bmpedges==null) bmpedges = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ALPHA_8);

        // /dev/videox (x=cameraId + cameraBase) is used
        int ret = prepareCameraWithBase(cameraId, cameraBase);
        if(ret!=-1) cameraExists = true;

        mainLoop = new Thread(this);
        mainLoop.start();		
    }

    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(DEBUG) Log.d(TAG, "surfaceChanged");
    }

    
    /**
     * A known memory leak exists when the surface is destroyed because 
     * bitmap.recycle() is never called on all of the bitmaps used in the class.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(DEBUG) Log.d(TAG, "surfaceDestroyed");
        if(cameraExists) {
            shouldStop = true;
            while(shouldStop) {
                try { 
                    Thread.sleep(100); // wait for thread stopping
                } catch(Exception e){}
            }
        }
        stopCamera();
    }   
    
    
    /** Simple Object Detection 
     * 
     * This object detection limits the detection to a bounded area of interest
     * that is the middle 50% of the screen. It then iterates through the bounded
     * area looking at a smaller area of xBound*yBound. If that area contains more 
     * than 40% white pixels (edges) then the area is considered part of an object
     * and it is marked so in the objectOverlaybmp.
     *  
     * */
    public void objectDetect() {
        int x, y, i, j, sum;
        int xBound = 8;
        int yBound = 8;
        int[] overlaySection = new int [xBound * yBound];
        objectOverlaybmp.eraseColor(Color.TRANSPARENT);
        objectDetected = false;

        for (y=(int) (IMG_HEIGHT*.25); y < IMG_HEIGHT*.75; y+=(yBound/2)) {
            for (x=(int) (IMG_WIDTH*.25); x < IMG_WIDTH*.75; x+=(xBound/2)) {
                sum =0;
                for (i=0; i < xBound; i++) {
                    for (j=0 ;j < yBound; j++) {
                        if (bmp.getPixel((x+i),(y+j)) == Color.WHITE) {
                            sum++;
                            overlaySection[i+ (j*(yBound))] = -256;
                        }
                    }
                }

                if (sum > (yBound*xBound*.4)) {
                    objectOverlaybmp.setPixels(overlaySection, 0, xBound, x, y, xBound, yBound);
                    objectDetected = true;
                }

            }
        }
    }
}
