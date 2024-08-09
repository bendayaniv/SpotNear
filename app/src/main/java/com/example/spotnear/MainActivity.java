package com.example.spotnear;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.locationlibrary.MyLocation;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "SpotNearPrefs";
    private static final String PREF_SERVICE_RUNNING = "isServiceRunning";

    private TextView locationText;
    private TextView placeDetailsText;
    private MyLocation myLocation;
    private Button startServiceButton;
    private Button stopServiceButton;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationText = findViewById(R.id.locationText);
        placeDetailsText = findViewById(R.id.placeDetailsText);
        Button updateLocationButton = findViewById(R.id.updateLocationButton);
        startServiceButton = findViewById(R.id.startServiceButton);
        stopServiceButton = findViewById(R.id.stopServiceButton);

        myLocation = MyLocation.getInstance();
        myLocation.initializeApp(getApplication(), true);

        updateLocationButton.setOnClickListener(v -> requestLocationUpdate());
        startServiceButton.setOnClickListener(v -> startSpotNearService());
        stopServiceButton.setOnClickListener(v -> stopSpotNearService());

        // Check for SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // Check if service is already running
        isServiceRunning = getServiceRunningState();
        updateButtonStates();

        if (isServiceRunning && !isServiceRunning(SpotNearService.class)) {
            startSpotNearService();
        }

        // Handle place details if sent from notification click
        handlePlaceDetails(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        handlePlaceDetails(intent);
    }

    private void handlePlaceDetails(Intent intent) {
        Log.d(TAG, "handlePlaceDetails called");
        if (intent != null && intent.hasExtra("placeDetails")) {
            Log.d(TAG, "Received place details from notification");
            String placeDetailsJson = intent.getStringExtra("placeDetails");
            try {
                JSONObject placeDetails = new JSONObject(placeDetailsJson);
                displayPlaceDetails(placeDetails);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing place details", e);
            }
        } else {
            Log.d(TAG, "No place details in intent");
        }
    }

    private void displayPlaceDetails(JSONObject placeDetails) {
        try {
            Log.d(TAG, "Displaying place details: " + placeDetails.toString(2));
            JSONObject tags = placeDetails.getJSONObject("tags");
            String name = tags.optString("name", "Unnamed Place");
            String type = getPoiType(placeDetails);
            String details = "Name: " + name + "\nType: " + type;
            placeDetailsText.setText(details);
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying place details", e);
            placeDetailsText.setText("Error displaying place details");
        }
    }

    private String getPoiType(JSONObject poi) throws JSONException {
        JSONObject tags = poi.getJSONObject("tags");
        if (tags.has("leisure") && "park".equals(tags.getString("leisure"))) {
            return "Park";
        } else if (tags.has("amenity")) {
            return tags.getString("amenity");
        } else if (tags.has("tourism")) {
            return tags.getString("tourism");
        }
        return "Interesting place";
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Update service state when activity resumes
        isServiceRunning = isServiceRunning(SpotNearService.class);
        setServiceRunningState(isServiceRunning);
        updateButtonStates();
    }

    private void startSpotNearService() {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(SpotNearService.ACTION_START_SERVICE);
        ContextCompat.startForegroundService(this, intent);
        isServiceRunning = true;
        setServiceRunningState(true);
        updateButtonStates();
        Log.d(TAG, "SpotNear service started");
    }

    private void stopSpotNearService() {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(SpotNearService.ACTION_STOP_SERVICE);
        startService(intent);
        isServiceRunning = false;
        setServiceRunningState(false);
        updateButtonStates();
        Log.d(TAG, "SpotNear service stopped");
    }

    private void updateButtonStates() {
        startServiceButton.setEnabled(!isServiceRunning);
        stopServiceButton.setEnabled(isServiceRunning);
    }

    private void requestLocationUpdate() {
        Log.d(TAG, "Requesting location update");
        myLocation.checkLocationAndRequestUpdates(this, (latitude, longitude) -> {
            String locationStr = "Lat: " + latitude + ", Lon: " + longitude;
            locationText.setText(locationStr);
            Log.d(TAG, "Location updated: " + locationStr);
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean getServiceRunningState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(PREF_SERVICE_RUNNING, false);
    }

    private void setServiceRunningState(boolean isRunning) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_SERVICE_RUNNING, isRunning);
        editor.apply();
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