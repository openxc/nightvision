package com.ford.openxc.nightvision;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class NightVisionActivity extends Activity {
    private static final String TAG = "NightVisionActivity";

    private static boolean mRunning;

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;
        unregisterReceiver(closeReceiver);
    }

    public static boolean isRunning() {
        return mRunning;
    }

    BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
}
