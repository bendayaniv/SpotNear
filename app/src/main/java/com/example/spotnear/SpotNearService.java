package com.example.spotnear;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotNearService extends Service {

    private static final String CHANNEL_ID = "SpotNearChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_SEARCH_NOTIFICATION_CLICKED = "com.example.spotnear.SEARCH_NOTIFICATION_CLICKED";
    public static final String ACTION_PLACE_NOTIFICATION_CLICKED = "com.example.spotnear.PLACE_NOTIFICATION_CLICKED";

    private static final int FOREGROUND_SERVICE_ID = 1000;

    private static final int SEARCH_NOTIFICATION_ID = 1001;
    private static final int PLACE_NOTIFICATION_ID = 1002;
    public static final String ACTION_START_SERVICE = "com.example.spotnear.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.example.spotnear.STOP_SERVICE";
    public static final String ACTION_UPDATE_LOCATION = "com.example.spotnear.UPDATE_LOCATION";
    public static final String ACTION_NOTIFICATION_CLICKED = "com.example.spotnear.NOTIFICATION_CLICKED";
    private static final String TAG = "SpotNearService";

    private NotificationManager notificationManager;
    private OkHttpClient client;
    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;
    private FusedLocationProviderClient fusedLocationClient;
    private PowerManager.WakeLock wakeLock;

    private boolean isSearching = true;
    private JSONObject lastFoundPlace = null;

    // Test mode flag and interval
    private static final boolean TEST_MODE = true;
    private static final long TEST_INTERVAL = 5 * 1000; // 5 seconds
    private static final long NORMAL_INTERVAL = AlarmManager.INTERVAL_HOUR; // 1 hour

    private boolean hasFoundPlace = false;

    private PreferencesManager preferencesManager;

    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SpotNearService onCreate");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpotNear:WakeLock");
        createNotificationChannel();
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        preferencesManager = new PreferencesManager(this);
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SpotNearService onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_SEARCH_NOTIFICATION_CLICKED.equals(action)) {
                handleSearchNotificationClick();
            } else if (ACTION_PLACE_NOTIFICATION_CLICKED.equals(action)) {
                handlePlaceNotificationClick();
            } else if (ACTION_START_SERVICE.equals(action)) {
                startForeground(FOREGROUND_SERVICE_ID, createSearchNotification());
                isSearching = true;
                if (!TEST_MODE) {
                    showInitialNotification();
                }
                scheduleAlarm();
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                Log.d(TAG, "Received stop service command");
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_UPDATE_LOCATION.equals(action)) {
                if (isSearching) {
                    requestLocationUpdate();
                } else {
                    scheduleAlarm();
                }
            }
        } else {
            // Service was restarted by the system
            startForeground(FOREGROUND_SERVICE_ID, createSearchNotification());
            isSearching = true;
            scheduleAlarm();
        }
        return START_STICKY;
    }

    private Notification createSearchNotification() {
        Intent notificationIntent = new Intent(this, SpotNearService.class);
        notificationIntent.setAction(ACTION_SEARCH_NOTIFICATION_CLICKED);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contentText = isSearching ? "Discovering interesting places nearby" : "Click to search for new places";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SpotNear is running")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private Notification createPlaceFoundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(ACTION_PLACE_NOTIFICATION_CLICKED);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SpotNear found a place!")
                .setContentText("We found something near you!")
                .setSmallIcon(R.drawable.notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private void showInitialNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SpotNear Started")
                .setContentText("We've started looking for interesting places nearby")
                .setSmallIcon(R.drawable.notification)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(SEARCH_NOTIFICATION_ID, notification);
    }

    private void handleSearchNotificationClick() {
        Log.d(TAG, "Search notification clicked");
        isSearching = true;
        hasFoundPlace = false;  // Reset this flag to allow finding a new place
        updateSearchNotification();
        requestLocationUpdate();
    }

    private void handlePlaceNotificationClick() {
        Log.d(TAG, "Place notification clicked");
        notificationManager.cancel(PLACE_NOTIFICATION_ID);
        if (!TEST_MODE) {
            // In non-test mode, clicking the place notification stops the service from searching
            isSearching = false;
            updateSearchNotification();
        }
        // Schedule the next automatic search
        scheduleNextAutomaticSearch();
    }

    private void scheduleNextAutomaticSearch() {
        long delay = TEST_MODE ? TEST_INTERVAL : NORMAL_INTERVAL;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isSearching = true;
                updateSearchNotification();
                requestLocationUpdate();
            }
        }, delay);
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAlarm() {
        Intent intent = new Intent(this, SpotNearServiceRestarter.class);
        intent.setAction(ACTION_UPDATE_LOCATION);
        alarmPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long interval;
        if (TEST_MODE) {
            interval = TEST_INTERVAL;
        } else {
            interval = hasFoundPlace ? NORMAL_INTERVAL : (5 * 60 * 1000); // 5 minutes if no place found, 1 hour otherwise
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, alarmPendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, alarmPendingIntent);
        }
        Log.d(TAG, "Scheduled next update in " + (interval / 1000) + " seconds");
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdate() {
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
                            findNearbyPOI(location.getLatitude(), location.getLongitude());
                        } else {
                            Log.d(TAG, "Location is null");
                            scheduleAlarm();
                        }
                        wakeLock.release();
                    }
                });
    }

    private void findNearbyPOI(double latitude, double longitude) {
        Log.d(TAG, "Finding nearby POI for Lat " + latitude + ", Lon " + longitude);
        String query = "[out:json];(" +
                "node[\"leisure\"=\"park\"](around:1000," + latitude + "," + longitude + ");" +
                "node[\"amenity\"=\"cafe\"](around:1000," + latitude + "," + longitude + ");" +
                "node[\"amenity\"=\"restaurant\"](around:1000," + latitude + "," + longitude + ");" +
                "node[\"tourism\"](around:1000," + latitude + "," + longitude + ");" +
                "way[\"leisure\"=\"park\"](around:1000," + latitude + "," + longitude + ");" +
                "way[\"amenity\"=\"cafe\"](around:1000," + latitude + "," + longitude + ");" +
                "way[\"amenity\"=\"restaurant\"](around:1000," + latitude + "," + longitude + ");" +
                "way[\"tourism\"](around:1000," + latitude + "," + longitude + ");" +
                ");out center;";

        String url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching POI data", e);
                scheduleAlarm();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    parseAndNotify(jsonData);
                } else {
                    scheduleAlarm();
                }
            }
        });
    }

    private void parseAndNotify(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);
            JSONArray elements = json.getJSONArray("elements");
            if (elements.length() > 0) {
                int randomIndex = (int) (Math.random() * elements.length());
                JSONObject poi = elements.getJSONObject(randomIndex);

                Log.d(TAG, "POI data: " + poi.toString());

                String name = poi.has("tags") ? poi.getJSONObject("tags").optString("name", "Interesting place") : "Interesting place";
                String type = getPoiType(poi);
                String location = poi.has("lat") && poi.has("lon") ? poi.getDouble("lat") + ", " + poi.getDouble("lon") : "Unknown location";

                Log.d(TAG, "Selected POI: " + name + " (" + type + "), " + location);

                if (TEST_MODE || !hasFoundPlace) {
                    preferencesManager.savePlaceDetails(poi);
                    showPlaceFoundNotification();
                    hasFoundPlace = true;
                }

                isSearching = false;
                updateSearchNotification();
                // Schedule the next automatic search
                scheduleNextAutomaticSearch();
            } else {
                Log.d(TAG, "No POIs found in the area");
                scheduleAlarm();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing POI data", e);
            scheduleAlarm();
        }
    }

    private void handleNotificationClick() {
        Log.d(TAG, "Notification clicked");

        // Start MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(ACTION_NOTIFICATION_CLICKED);
        startActivity(intent);

        // Cancel the notification to remove it from the tray
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(FOREGROUND_SERVICE_ID);

        // Reset the search state
        isSearching = true;
        // preferencesManager.clearPlaceDetails();

        // Update notification to show we're searching again
        updateForegroundNotification();

        // Immediately request a location update to start searching
        requestLocationUpdate();
    }

    private void updateForegroundNotification() {
        Notification notification = createSearchNotification();
        notificationManager.notify(FOREGROUND_SERVICE_ID, notification);
    }

    private void updateSearchNotification() {
        Notification notification = createSearchNotification();
        notificationManager.notify(FOREGROUND_SERVICE_ID, notification);
    }

    private void showPlaceFoundNotification() {
        Notification notification = createPlaceFoundNotification();
        notificationManager.notify(PLACE_NOTIFICATION_ID, notification);
    }

    private String getPoiType(JSONObject poi) {
        try {
            JSONObject tags = poi.getJSONObject("tags");
            if (tags.has("leisure") && "park".equals(tags.getString("leisure"))) {
                return "park";
            } else if (tags.has("amenity")) {
                return tags.getString("amenity");
            } else if (tags.has("tourism")) {
                return tags.getString("tourism");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting POI type", e);
        }
        return "interesting place";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SpotNear Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SpotNearService onDestroy");
        if (alarmManager != null && alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent);
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // Remove any pending automatic search
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}