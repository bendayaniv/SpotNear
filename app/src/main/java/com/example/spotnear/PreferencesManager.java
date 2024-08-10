package com.example.spotnear;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages shared preferences for the SpotNear application
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "SpotNearPrefs";
    private static final String PREF_PLACE_DETAILS = "placeDetails";
    private static final String PREF_SERVICE_RUNNING = "isServiceRunning";
    private static final String PREF_POI_SEARCH_RADIUS = "poiSearchRadius";
    private static final int DEFAULT_SEARCH_RADIUS = 1000; // 1 km default

    private final SharedPreferences prefs;

    /**
     * Constructor
     *
     * @param context The context used to access SharedPreferences
     */
    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save place details to SharedPreferences
     *
     * @param placeDetails JSONObject containing place details
     */
    public void savePlaceDetails(JSONObject placeDetails) {
        prefs.edit().putString(PREF_PLACE_DETAILS, placeDetails.toString()).apply();
    }

    /**
     * Retrieve place details from SharedPreferences
     *
     * @return JSONObject containing place details, or null if not found
     */
    public JSONObject getPlaceDetails() {
        String placeDetailsStr = prefs.getString(PREF_PLACE_DETAILS, null);
        if (placeDetailsStr != null) {
            try {
                return new JSONObject(placeDetailsStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Set the running state of the service
     *
     * @param isRunning true if the service is running, false otherwise
     */
    public void setServiceRunningState(boolean isRunning) {
        prefs.edit().putBoolean(PREF_SERVICE_RUNNING, isRunning).apply();
    }

    /**
     * Get the running state of the service
     *
     * @return true if the service is running, false otherwise
     */
    public boolean getServiceRunningState() {
        return prefs.getBoolean(PREF_SERVICE_RUNNING, false);
    }

    /**
     * Clear saved place details from SharedPreferences
     */
    public void clearPlaceDetails() {
        prefs.edit().remove(PREF_PLACE_DETAILS).apply();
    }

    /**
     * Set the POI search radius
     *
     * @param radius The search radius in meters
     */
    public void setPoiSearchRadius(int radius) {
        prefs.edit().putInt(PREF_POI_SEARCH_RADIUS, radius).apply();
    }

    /**
     * Get the POI search radius
     *
     * @return The search radius in meters, or the default value if not set
     */
    public int getPoiSearchRadius() {
        return prefs.getInt(PREF_POI_SEARCH_RADIUS, DEFAULT_SEARCH_RADIUS);
    }

    /**
     * Stores the last known location as a string in SharedPreferences.
     *
     * @param location A string representation of the location, typically in the format "Lat: X, Lon: Y"
     */
    public void setLastKnownLocation(String location) {
        prefs.edit().putString("last_known_location", location).apply();
    }

    /**
     * Retrieves the last known location from SharedPreferences.
     *
     * @return The last known location as a string, or null if no location has been stored
     */
    public String getLastKnownLocation() {
        return prefs.getString("last_known_location", null);
    }

    /**
     * Stores the timestamp of the last location update in SharedPreferences.
     *
     * @param time The timestamp of the last location update in milliseconds since epoch
     */
    public void setLastLocationUpdateTime(long time) {
        prefs.edit().putLong("last_location_update_time", time).apply();
    }

    /**
     * Retrieves the timestamp of the last location update from SharedPreferences.
     *
     * @return The timestamp of the last location update in milliseconds since epoch,
     * or 0 if no update time has been stored
     */
    public long getLastLocationUpdateTime() {
        return prefs.getLong("last_location_update_time", 0);
    }
}