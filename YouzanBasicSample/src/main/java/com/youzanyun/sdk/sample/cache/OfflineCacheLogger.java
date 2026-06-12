package com.youzanyun.sdk.sample.cache;

import android.util.Log;

public final class OfflineCacheLogger {

    public static final String TAG = "OfflineCache";

    private OfflineCacheLogger() {
    }

    public static void log(String stage, String detail) {
        Log.d(TAG, "离线缓存 | " + stage + " | " + detail);
    }
}
