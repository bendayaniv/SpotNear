package com.example.spotnear;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotNearService extends Service {

    private static final String CHANNEL_ID = "SpotNearChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_UPDATE_LOCATION = "com.example.spotnear.UPDATE_LOCATION";
    public static final String ACTION_STOP_SERVICE = "com.example.spotnear.STOP_SERVICE";
    private static final String TAG = "SpotNearService";

    private NotificationManager notificationManager;
    private OkHttpClient client;
    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;

    // Test mode flag and interval
    private static final boolean TEST_MODE = true;
    private static final long TEST_INTERVAL = 30 * 1000; // 0.5 minute

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SpotNearService onCreate");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
        client = new OkHttpClient();
        scheduleNextUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SpotNearService onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_UPDATE_LOCATION.equals(action)) {
                double latitude = intent.getDoubleExtra("latitude", 0);
                double longitude = intent.getDoubleExtra("longitude", 0);
                Log.d(TAG, "Received location update: Lat " + latitude + ", Lon " + longitude);
                findNearbyPOI(latitude, longitude);
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                Log.d(TAG, "Received stop service command");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    private void scheduleNextUpdates() {
        if (TEST_MODE) {
            Log.d(TAG, "Scheduling test mode updates every 0.5 minutes");
            scheduleTestUpdate();
        } else {
            Log.d(TAG, "Scheduling regular updates");
            scheduleUpdate(9, 0);
            scheduleUpdate(14, 0);
            scheduleUpdate(19, 0);
        }
    }

    private void scheduleTestUpdate() {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(ACTION_UPDATE_LOCATION);
        alarmPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long nextUpdateTime = System.currentTimeMillis() + TEST_INTERVAL;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextUpdateTime, alarmPendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextUpdateTime, alarmPendingIntent);
        }
        Log.d(TAG, "Scheduled next test update in 0.5 minutes");
    }

    private void scheduleUpdate(int hourOfDay, int minute) {
        Intent intent = new Intent(this, SpotNearService.class);
        intent.setAction(ACTION_UPDATE_LOCATION);
        PendingIntent pendingIntent = PendingIntent.getService(this, hourOfDay, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.w(TAG, "Exact alarm permission not granted. Using inexact alarm.");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void findNearbyPOI(double latitude, double longitude) {
        Log.d(TAG, "Finding nearby POI for Lat " + latitude + ", Lon " + longitude);
        String query = "[out:json];(" +
                "node[\"amenity\"](around:1000," + latitude + "," + longitude + ");" +
                "way[\"amenity\"](around:1000," + latitude + "," + longitude + ");" +
                ");out center;";

        String url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching POI data", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    Log.d(TAG, "Received POI data: " + jsonData);
                    parseAndNotify(jsonData);
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
                String name = poi.has("tags") ? poi.getJSONObject("tags").optString("name", "Interesting place") : "Interesting place";
                String type = poi.has("tags") ? poi.getJSONObject("tags").optString("amenity", "place") : "place";

                Log.d(TAG, "Selected POI: " + name + " (" + type + ")");
                sendNotification("Spot Near: " + name, "There's a " + type + " near you. Why not check it out?");
            } else {
                Log.d(TAG, "No POIs found in the area");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing POI data", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SpotNear Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String title, String content) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
        Log.d(TAG, "Notification sent: " + title + " - " + content);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SpotNearService onDestroy");
        if (alarmManager != null && alarmPendingIntent != null) {
            alarmManager.cancel(alarmPendingIntent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}