package com.example.spotnear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class SpotNearServiceRestarter extends BroadcastReceiver {

    private static final String TAG = "SpotNearServiceRestarter";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast to start service");
        Intent serviceIntent = new Intent(context, SpotNearService.class);

        if (SpotNearService.ACTION_UPDATE_LOCATION.equals(intent.getAction())) {
            serviceIntent.setAction(SpotNearService.ACTION_UPDATE_LOCATION);
        } else {
            serviceIntent.setAction(SpotNearService.ACTION_START_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}