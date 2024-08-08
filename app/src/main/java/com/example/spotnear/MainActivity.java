package com.example.spotnear;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.locationlibrary.MyLocation;

public class MainActivity extends AppCompatActivity {

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

        // Start the SpotNear service
        startService(new Intent(this, SpotNearService.class));
    }

    private void requestLocationUpdate() {
        myLocation.checkLocationAndRequestUpdates(this, (latitude, longitude) -> {
            String locationStr = "Lat: " + latitude + ", Lon: " + longitude;
            locationText.setText(locationStr);

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