package com.camera.simplewebcam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;


public class NightVisionActivity extends Activity {
    private static final String TAG = "NightVisionActivity";
    private static boolean activityRunning;
    CameraPreview cp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        activityRunning = true;
        cp = new CameraPreview(this);
        setContentView(cp);

        Intent MonitoringServiceIntent = new Intent(NightVisionActivity.this, VehicleMonitoringService.class);
        startService(MonitoringServiceIntent);  
        Log.w(TAG, "Starting Service from BootupReceiver");

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ford.openxc.HEADLAMPS_OFF");
        registerReceiver(receiver, filter);

        IntentFilter usbfilter = new IntentFilter();
        usbfilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbfilter.addAction("com.ford.openxc.NO_CAMERA_DETECTED");
        registerReceiver(mUsbReceiver, usbfilter);
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };


    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w("USB ERROR", "Usb device detached");
            usbError();   
        }
    };


    public void finish() {
        super.finish();
    }


    @Override
    public void onPause() {   
        super.onPause();
        activityRunning = false;
        android.os.Process.killProcess(android.os.Process.myPid()); 
    }


    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
    }


    public void usbError(){
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(2000);

        new AlertDialog.Builder(this)
        .setTitle("USB Device Unplugged!")
        .setMessage("Ford Night Vision is closing. Please insert a USB web camera.")
        .setCancelable(false)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                activityRunning = false;
                android.os.Process.killProcess(android.os.Process.myPid()); 
            }
        }).show();
    }


    public static boolean isRunning(){
        return activityRunning;
    }
}
