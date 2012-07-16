package com.camera.simplewebcam;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class Main extends Activity {
    private static final String TAG = "Activity";
	CameraPreview cp;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Trying to load OpenCV library");
        
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
		setContentView(R.layout.main);
		cp = new CameraPreview(this);
		setContentView(cp);
	}
	 private BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
	       
	        @Override
	        public void onManagerConnected(int status) {
	            switch (status) {
	                case LoaderCallbackInterface.SUCCESS:
	                    Log.i(TAG, "OpenCV loaded successfully");
	                    // Load native libs after OpenCV initialization
	                    System.loadLibrary("ImageProc");
	                    break;
	                
	                default:
	                    super.onManagerConnected(status);
	                    break;
	            }
	        }       
	    };
	
}
