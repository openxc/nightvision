package com.ford.openxc.nightvision;

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

    private static boolean mRunning;
    private static boolean mAudioOption = true;
    private static boolean mVideoOption = true;
    private static boolean mObjectDetectionOption = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mRunning = true;

        Intent MonitoringServiceIntent = new Intent(NightVisionActivity.this,
                VehicleMonitoringService.class);
        startService(MonitoringServiceIntent);
        Log.w(TAG, "Starting Service from NightVisionActivity");
    }

    @Override
    public void onPause() {
        super.onPause();
        mRunning = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRunning = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.ford.openxc.HEADLAMPS_OFF");
        registerReceiver(closeReceiver, filter);

        IntentFilter usbfilter = new IntentFilter();
        usbfilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbfilter.addAction("com.ford.openxc.NO_CAMERA_DETECTED");
        registerReceiver(usbReceiver, usbfilter);
    }

    public void usbError() {
        if(mRunning == true) {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(2000);

            new AlertDialog.Builder(this)
            .setTitle("USB Device Unplugged!")
            .setMessage("Nightvision is closing. Please insert a USB web camera.")
            .setCancelable(false)
            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mRunning = false;
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }).show();
        }
        else if (mRunning == false ) {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(2000);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;
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
                mAudioOption = false;
            }
            else {
                item.setChecked(true);
                mAudioOption = true;
            }
            return true;

        case R.id.videoFilterOption:
            if (item.isChecked()) {
                item.setChecked(false);
                mVideoOption = false;
            }
            else {
                item.setChecked(true);
                mVideoOption = true;
            }
            return true;

        case R.id.objectDetectionOption:
            if (item.isChecked()) {
                item.setChecked(false);
                mObjectDetectionOption = false;
            }
            else {
                item.setChecked(true);
                mObjectDetectionOption = true;
            }
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public static boolean isRunning() {
        return mRunning;
    }

    public static boolean getAudioOption() {
        return mAudioOption;
    }

    public static boolean getVideoOption() {
        return mVideoOption;
    }

    public static boolean getObjectDetectionOption() {
        return mObjectDetectionOption;
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
}
