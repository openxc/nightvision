package com.camera.simplewebcam;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;


public class Main extends Activity {
    private static final String TAG = "Main: Camera";
    static boolean activityRunning;
    CameraPreview cp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        activityRunning = true;
        cp = new CameraPreview(this);
        setContentView(cp);

        Intent MonitoringServiceIntent = new Intent(Main.this, VehicleMonitoringService.class);
        startService(MonitoringServiceIntent);  
        Log.w(TAG, "Starting Service from BootupReceiver");

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ford.openxc.HEADLAMPS_OFF");
        registerReceiver(receiver, filter);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    
    public void finish() {
        super.finish();
    }

    @Override
    public void onPause() {   
        super.onPause();
        activityRunning = false;

    }
    
    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        
    }

}
