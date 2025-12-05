package com.sanchezmobiled.startwifi;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetWatcherService extends Service {

    private static final String TAG = "NetWatcherService";
    private static final long CHECK_INTERVAL_MS = 15_000;
    private static final String CHANNEL_ID = "net_watcher_channel";
    private static final int NOTIFICATION_ID = 1;

    private WifiManager wifiManager;
    private Handler handler;
    private Runnable checkRunnable;

    private static boolean hasStarted = false;

    private Boolean lastHasInternet = null;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        // foreground
        startForeground(NOTIFICATION_ID, buildNotification(false));

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        handler = new Handler();

        checkRunnable = () -> new Thread(() -> {
            boolean hasInternet = hasInternetConnection();
            Log.d(TAG, "Internet: " + hasInternet);

            if (!hasInternet) {
                reconnectWifi();
            }

            // уведомление если состояние изменилось
            if (lastHasInternet == null || lastHasInternet != hasInternet) {
                lastHasInternet = hasInternet;
                updateNotification(hasInternet);
            }

            // следующий тик
            handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS);
        }).start();

        handler.post(checkRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (hasStarted) {
            Log.d(TAG, "onStartCommand: already started, ignoring extra startId=" + startId);
            return START_STICKY;
        }

        hasStarted = true;
        Log.d(TAG, "onStartCommand: first start, startId=" + startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        hasStarted = false;

        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------- INTERNET CHECK ----------

    private boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null || !ni.isConnected()) {
                return false;
            }
        }

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("https://clients3.google.com/generate_204");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(2000);
            urlConnection.setReadTimeout(2000);
            urlConnection.setInstanceFollowRedirects(false);
            int code = urlConnection.getResponseCode();
            return (code == 204 || code == 200);
        } catch (IOException e) {
            Log.e(TAG, "hasInternetConnection error", e);
            return false;
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
    }

    private void reconnectWifi() {
        if (wifiManager == null) return;

        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "WiFi disabled, enabling...");
            wifiManager.setWifiEnabled(true);
            return;
        }

        WifiInfo info = wifiManager.getConnectionInfo();
        int networkId = (info != null) ? info.getNetworkId() : -1;

        if (networkId == -1) {
            Log.d(TAG, "No current WiFi network, calling reconnect()");
            wifiManager.reconnect();
        } else {
            Log.d(TAG, "Current WiFi networkId=" + networkId + ", reconnecting...");
            wifiManager.disconnect();
            wifiManager.reconnect();
        }
    }

    // ---------- NOTIFICATION ----------

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.net_watcher),
                NotificationManager.IMPORTANCE_LOW
        );
        ch.setDescription(getString(R.string.net_controll_monitoring));

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(boolean hasInternet) {
        String title = getString(R.string.net_controll);

        String text;
        if (hasInternet) {
            text = getString(R.string.net_controll_on);
        } else {
            text = getString(R.string.net_controll_off);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(boolean hasInternet) {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(hasInternet));
        }
    }
}
