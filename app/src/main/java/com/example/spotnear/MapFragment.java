package com.example.spotnear;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean mapReady = false;
    private LatLng initialLocation = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the map
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Zoom to a specific location on the map
     *
     * @param latitude  Latitude of the location
     * @param longitude Longitude of the location
     */
    public void zoom(double latitude, double longitude) {
        if (!mapReady) {
            initialLocation = new LatLng(latitude, longitude);
            return;
        }

        mMap.clear();

        LatLng poiLocation = new LatLng(latitude, longitude);

        MarkerOptions marker = new MarkerOptions()
                .position(poiLocation);

        mMap.addMarker(marker);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(poiLocation)
                .zoom(16)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Clear all markers from the map
     */
    public void refreshMap() {
        if (mapReady) {
            mMap.clear();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;
        initializeMap();

        if (initialLocation != null) {
            zoom(initialLocation.latitude, initialLocation.longitude);
            initialLocation = null;
        } else {
            refreshMap();
        }
    }

    /**
     * Initialize map settings
     */
    private void initializeMap() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
    }

    /**
     * Set the initial location to zoom to when the map is ready
     *
     * @param latitude  Latitude of the initial location
     * @param longitude Longitude of the initial location
     */
    public void setInitialLocation(double latitude, double longitude) {
        this.initialLocation = new LatLng(latitude, longitude);
    }

    /**
     * Check if the map is ready
     *
     * @return true if the map is ready, false otherwise
     */
    public boolean isMapReady() {
        return mapReady;
    }
}