package com.example.spotnear;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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

        initializeViews();
        setupListeners();
        checkAlarmPermission();
        setupMapFragment();
        updateServiceState();
        checkExistingPlaceDetails();
        handleIntent(getIntent());
    }

    private void initializeViews() {
        locationText = findViewById(R.id.locationText);
        placeDetailsText = findViewById(R.id.placeDetailsText);
        startServiceButton = findViewById(R.id.startServiceButton);
        stopServiceButton = findViewById(R.id.stopServiceButton);
        searchRadiusInput = findViewById(R.id.searchRadiusInput);

        myLocation = MyLocation.getInstance();
        myLocation.initializeApp(getApplication(), true);

        preferencesManager = new PreferencesManager(this);
        searchRadiusInput.setText(String.valueOf(preferencesManager.getPoiSearchRadius()));
    }

    private void setupListeners() {
        startServiceButton.setOnClickListener(v -> checkRadiusAndStartService());
        stopServiceButton.setOnClickListener(v -> stopSpotNearService());
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void setupMapFragment() {
        mapFragment = new MapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainPageMapFragment, mapFragment)
                .commit();
    }

    private void updateServiceState() {
        isServiceRunning = preferencesManager.getServiceRunningState();
        updateButtonStates();
        if (isServiceRunning) {
            if (!isServiceRunning(SpotNearService.class)) {
                startSpotNearService(false);
            } else {
                updateLocationIfNeeded();
            }
        }
    }

    private void updateLocationIfNeeded() {
        if (isServiceRunning && checkPermissions()) {
            long lastUpdateTime = preferencesManager.getLastLocationUpdateTime();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > 60000) { // Update if last update was more than 1 minute ago
                requestLocationUpdate(false);
            } else {
                // Use the last known location
                String lastKnownLocation = preferencesManager.getLastKnownLocation();
                if (lastKnownLocation != null) {
                    locationText.setText(lastKnownLocation);
                }
            }
        }
    }

    private void checkExistingPlaceDetails() {
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
                    startSpotNearService(true);
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

                if (mapFragment != null && mapFragment.isMapReady()) {
                    mapFragment.zoom(Double.parseDouble(latitude), Double.parseDouble(longitude));
                } else {
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
        isServiceRunning = isServiceRunning(SpotNearService.class);
        preferencesManager.setServiceRunningState(isServiceRunning);
        updateButtonStates();
        updateLocationIfNeeded();
    }

    private void startSpotNearService(boolean updateLocation) {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(SpotNearService.ACTION_START_SERVICE);
        ContextCompat.startForegroundService(this, intent);
        isServiceRunning = true;
        preferencesManager.setServiceRunningState(true);
        updateButtonStates();
        if (updateLocation) {
            requestLocationUpdate(false);
        }
        Log.d(TAG, "SpotNear service started");
    }

    private void stopSpotNearService() {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(SpotNearService.ACTION_STOP_SERVICE);
        startService(intent);
        isServiceRunning = false;
        preferencesManager.setServiceRunningState(false);
        updateButtonStates();
        Log.d(TAG, "SpotNear service stopped");
    }

    private void updateButtonStates() {
        startServiceButton.setEnabled(!isServiceRunning);
        stopServiceButton.setEnabled(isServiceRunning);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void checkRadiusAndStartService() {
        String radiusStr = searchRadiusInput.getText().toString();
        if (!radiusStr.isEmpty()) {
            int radius = Integer.parseInt(radiusStr);
            preferencesManager.setPoiSearchRadius(radius);
            checkPermissionsAndStartService();
        } else {
            Toast.makeText(this, "Please enter a search radius", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissionsAndStartService() {
        if (checkPermissions()) {
            requestLocationUpdate(true);
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
                requestLocationUpdate(true);
            } else {
                Log.d(TAG, "Some permissions were not granted");
            }
        }
        myLocation.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void requestLocationUpdate(boolean startService) {
        Log.d(TAG, "Requesting location update");
        myLocation.checkLocationAndRequestUpdates(this, (latitude, longitude) -> {
            String locationStr = "Lat: " + latitude + ", Lon: " + longitude;
            locationText.setText(locationStr);
            Log.d(TAG, "Location updated: " + locationStr);

            preferencesManager.setLastKnownLocation(locationStr);
            preferencesManager.setLastLocationUpdateTime(System.currentTimeMillis());

            if (startService && !isServiceRunning) {
                startSpotNearService(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        myLocation.onActivityResult(this, requestCode, resultCode);
    }
}