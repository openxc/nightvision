package com.camera.simplewebcam;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


public class Main extends Activity {
    private static final String TAG = "Activity";
	CameraPreview cp;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		cp = new CameraPreview(this);
		setContentView(cp);
	}
	
}
