package com.example.spotnear;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.locationlibrary.MyLocation;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView locationText;
    private MyLocation myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationText = findViewById(R.id.locationText);
        Button updateLocationButton = findViewById(R.id.updateLocationButton);

        myLocation = MyLocation.getInstance();
        myLocation.initializeApp(getApplication(), true);

        updateLocationButton.setOnClickListener(v -> requestLocationUpdate());

        // Check for SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // Start the SpotNear service
        startService(new Intent(this, SpotNearService.class));

        // Schedule periodic location updates for testing
        schedulePeriodicLocationUpdates();
    }

    private void schedulePeriodicLocationUpdates() {
        // Schedule a location update every 2 minutes for testing
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestLocationUpdate();
                schedulePeriodicLocationUpdates();
            }
        }, 30 * 1000); // 0.5 minute
    }

    private void requestLocationUpdate() {
        Log.d(TAG, "Requesting location update");
        myLocation.checkLocationAndRequestUpdates(this, (latitude, longitude) -> {
            String locationStr = "Lat: " + latitude + ", Lon: " + longitude;
            locationText.setText(locationStr);
            Log.d(TAG, "Location updated: " + locationStr);

            // Send location to the service
            Intent intent = new Intent(this, SpotNearService.class);
            intent.setAction(SpotNearService.ACTION_UPDATE_LOCATION);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startService(intent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        myLocation.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        myLocation.onActivityResult(this, requestCode, resultCode);
    }
}