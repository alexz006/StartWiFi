package com.sanchezmobiled.startwifi;

import android.app.Application;
import android.content.Intent;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Intent i = new Intent(this, NetWatcherService.class);

        startForegroundService(i);
    }
}
