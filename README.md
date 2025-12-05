Automatically turns on WiFi and reconnects to the saved network.
For Android 9.

```
adb install -r -g StartWiFi_v1.0.apk
```

```
adb shell am start-foreground-service -n com.sanchezmobiled.startwifi/.NetWatcherService
```

```
adb uninstall com.sanchezmobiled.startwifi
```
