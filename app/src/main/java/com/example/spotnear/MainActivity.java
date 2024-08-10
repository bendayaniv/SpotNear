package com.example.spotnear;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.locationlibrary.MyLocation;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "SpotNearPrefs";
    private static final String PREF_SERVICE_RUNNING = "isServiceRunning";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView locationText;
    private TextView placeDetailsText;
    private MyLocation myLocation;
    private TextInputEditText searchRadiusInput;
    private Button startServiceButton;
    private Button stopServiceButton;
    private boolean isServiceRunning = false;

    private PreferencesManager preferencesManager;

    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set direction on all devices from LEFT to RIGHT
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        preferencesManager = new PreferencesManager(this);

        locationText = findViewById(R.id.locationText);
        placeDetailsText = findViewById(R.id.placeDetailsText);
        startServiceButton = findViewById(R.id.startServiceButton);
        stopServiceButton = findViewById(R.id.stopServiceButton);

        myLocation = MyLocation.getInstance();
        myLocation.initializeApp(getApplication(), true);

        startServiceButton.setOnClickListener(v -> checkPermissionsAndStartService());
        stopServiceButton.setOnClickListener(v -> stopSpotNearService());

        // Check for SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        searchRadiusInput = findViewById(R.id.searchRadiusInput);

        // Set the initial value from preferences
        searchRadiusInput.setText(String.valueOf(preferencesManager.getPoiSearchRadius()));

        startServiceButton.setOnClickListener(v -> checkRadiusAndStartService());

        mapFragment = new MapFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainPageMapFragment, mapFragment)
                .commit();

        // Check if service is already running
        isServiceRunning = getServiceRunningState();
        updateButtonStates();

        if (isServiceRunning && !isServiceRunning(SpotNearService.class)) {
            startSpotNearService();
        }

        // Check for existing place details and display them if available
        checkAndDisplayExistingPlaceDetails();

        // Handle intent if the activity was started from a notification
        handleIntent(getIntent());
    }

    private void checkAndDisplayExistingPlaceDetails() {
        JSONObject existingPlaceDetails = preferencesManager.getPlaceDetails();
        if (existingPlaceDetails != null) {
            displayPlaceDetails();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "Handling intent with action: " + action);
            if (SpotNearService.ACTION_PLACE_NOTIFICATION_CLICKED.equals(action)) {
                Log.d(TAG, "Place notification clicked intent received");
                displayPlaceDetails();
            } else if (SpotNearService.ACTION_SEARCH_NOTIFICATION_CLICKED.equals(action)) {
                Log.d(TAG, "Search notification clicked intent received");
                if (!isServiceRunning(SpotNearService.class)) {
                    startSpotNearService();
                } else {
                    Intent searchIntent = new Intent(SpotNearService.ACTION_SEARCH_NOTIFICATION_CLICKED);
                    sendBroadcast(searchIntent);
                }
            }
        }
    }

    private void displayPlaceDetails() {
        JSONObject placeDetails = preferencesManager.getPlaceDetails();
        if (placeDetails != null) {
            try {
                Log.d(TAG, "Displaying place details: " + placeDetails.toString(2));
                JSONObject tags = placeDetails.getJSONObject("tags");
                String name = tags.optString("name", "Unnamed Place");
                String type = getPoiType(placeDetails);
                String latitude = placeDetails.getString("lat");
                String longitude = placeDetails.getString("lon");
                String details = "Name: " + name + "\nType: " + type + "\nLatitude: " + latitude + "\nLongitude: " + longitude;

                // Ensure the map fragment is ready before zooming
                if (mapFragment != null && mapFragment.isMapReady()) {
                    mapFragment.zoom(Double.parseDouble(latitude), Double.parseDouble(longitude));
                } else {
                    // If the map is not ready, set a flag to zoom when it's ready
                    mapFragment.setInitialLocation(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }

                placeDetailsText.setText(details);
            } catch (JSONException e) {
                Log.e(TAG, "Error displaying place details", e);
                placeDetailsText.setText("Error displaying place details");
            }
        } else {
            placeDetailsText.setText("No place details available");
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
        preferencesManager.setServiceRunningState(true);
        updateButtonStates();
        Log.d(TAG, "SpotNear service started");
    }

    private void stopSpotNearService() {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(SpotNearService.ACTION_STOP_SERVICE);
        startService(intent);
        isServiceRunning = false;
        preferencesManager.setServiceRunningState(false);
//        preferencesManager.clearPlaceDetails();
        updateButtonStates();
        Log.d(TAG, "SpotNear service stopped");
    }

    private void updateButtonStates() {
        startServiceButton.setEnabled(!isServiceRunning);
        stopServiceButton.setEnabled(isServiceRunning);
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

    private void checkPermissionsAndStartService() {
        if (checkPermissions()) {
            requestLocationUpdate();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQUEST_CODE);
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
    }

    private void checkRadiusAndStartService() {
        String radiusStr = searchRadiusInput.getText().toString();
        if (!radiusStr.isEmpty()) {
            int radius = Integer.parseInt(radiusStr);
            preferencesManager.setPoiSearchRadius(radius);
            checkPermissionsAndStartService();
        } else {
            // Show an error message to the user
            Toast.makeText(this, "Please enter a search radius", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                requestLocationUpdate();
            } else {
                // Handle the case where permissions are not granted
                Log.d(TAG, "Some permissions were not granted");
                // You might want to show a dialog explaining why permissions are needed
            }
        }
        myLocation.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void requestLocationUpdate() {
        Log.d(TAG, "Requesting location update");
        myLocation.checkLocationAndRequestUpdates(this, (latitude, longitude) -> {
            String locationStr = "Lat: " + latitude + ", Lon: " + longitude;
            locationText.setText(locationStr);
            Log.d(TAG, "Location updated: " + locationStr);

            startSpotNearService();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        myLocation.onActivityResult(this, requestCode, resultCode);
    }
}