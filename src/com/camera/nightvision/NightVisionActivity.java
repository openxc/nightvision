package com.camera.nightvision;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class NightVisionActivity extends Activity {
    private static final String TAG = "NightVisionActivity";
    private static boolean activityRunning;
    private static boolean audioOption=true;
    private static boolean videoOption =true;
    private static boolean objectDetectionOption=true;
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
        Log.w(TAG, "Starting Service from NightVisionActivity");

        registerCloseReceiver();
        registerUsbReceiver();
    }


    BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };


    BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w("USB ERROR", "Usb device detached");
            usbError();   
        }
    };


    public void finish() {
        super.finish();
        activityRunning =false;
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
        registerCloseReceiver();
        registerUsbReceiver();
    }


    public void usbError() {
        if(activityRunning == true) {
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
        else if (activityRunning == false ) {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(2000);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        activityRunning = false;
        unregisterReceiver(usbReceiver);
        unregisterReceiver(closeReceiver);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_settings, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {   
        switch (item.getItemId()) {

        case R.id.audioAlertOption:
            if (item.isChecked()) {
                item.setChecked(false);
                audioOption = false;
            }
            else {
                item.setChecked(true);
                audioOption = true;
            }
            return true;

        case R.id.videoFilterOption:
            if (item.isChecked()) {
                item.setChecked(false);
                videoOption = false;
            }
            else {
                item.setChecked(true);
                videoOption = true;
            }
            return true;

        case R.id.objectDetectionOption:
            if (item.isChecked()) {
                item.setChecked(false);
                objectDetectionOption = false;
            }
            else {
                item.setChecked(true);
                objectDetectionOption = true;
            }
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }


    public static boolean isRunning() {
        return activityRunning;
    }


    public static boolean getAudioOption() {
        return audioOption;
    }


    public static boolean getVideoOption() {
        return videoOption;
    }


    public static boolean getObjectDetectionOption() {
        return objectDetectionOption;
    }


    public void registerCloseReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ford.openxc.HEADLAMPS_OFF");
        registerReceiver(closeReceiver, filter);
    }


    public void registerUsbReceiver() {
        IntentFilter usbfilter = new IntentFilter();
        usbfilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbfilter.addAction("com.ford.openxc.NO_CAMERA_DETECTED");
        registerReceiver(usbReceiver, usbfilter);
    }


}
