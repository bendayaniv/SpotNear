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
    private static final String TAG = "SpotNearService";

    private NotificationManager notificationManager;
    private OkHttpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        client = new OkHttpClient();
        scheduleNextUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE_LOCATION.equals(intent.getAction())) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            findNearbyPOI(latitude, longitude);
        }
        return START_STICKY;
    }

    private void scheduleNextUpdates() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Schedule three updates: at 9 AM, 2 PM, and 7 PM
        scheduleUpdate(alarmManager, 9, 0);
        scheduleUpdate(alarmManager, 14, 0);
        scheduleUpdate(alarmManager, 19, 0);
    }

    private void scheduleUpdate(AlarmManager alarmManager, int hourOfDay, int minute) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void findNearbyPOI(double latitude, double longitude) {
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

                sendNotification("Spot Near: " + name, "There's a " + type + " near you. Why not check it out?");
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
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}