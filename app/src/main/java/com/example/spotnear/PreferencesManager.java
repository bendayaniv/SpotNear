package com.example.spotnear;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class PreferencesManager {
    private static final String PREFS_NAME = "SpotNearPrefs";
    private static final String PREF_PLACE_DETAILS = "placeDetails";
    private static final String PREF_SERVICE_RUNNING = "isServiceRunning";

    private static final String PREF_POI_SEARCH_RADIUS = "poiSearchRadius";
    private static final int DEFAULT_SEARCH_RADIUS = 1000; // 1 km default

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void savePlaceDetails(JSONObject placeDetails) {
        prefs.edit().putString(PREF_PLACE_DETAILS, placeDetails.toString()).apply();
    }

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

    public void setServiceRunningState(boolean isRunning) {
        prefs.edit().putBoolean(PREF_SERVICE_RUNNING, isRunning).apply();
    }

    public boolean getServiceRunningState() {
        return prefs.getBoolean(PREF_SERVICE_RUNNING, false);
    }

    public void clearPlaceDetails() {
        prefs.edit().remove(PREF_PLACE_DETAILS).apply();
    }

    public void setPoiSearchRadius(int radius) {
        prefs.edit().putInt(PREF_POI_SEARCH_RADIUS, radius).apply();
    }

    public int getPoiSearchRadius() {
        return prefs.getInt(PREF_POI_SEARCH_RADIUS, DEFAULT_SEARCH_RADIUS);
    }
}