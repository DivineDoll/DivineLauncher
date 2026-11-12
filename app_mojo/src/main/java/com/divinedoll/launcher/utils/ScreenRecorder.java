package com.divinedoll.launcher.utils;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

import androidx.core.content.ContextCompat;

import com.divinedoll.launcher.services.RecorderService;

/** Small helper around {@link RecorderService}. */
public final class ScreenRecorder {
    private ScreenRecorder() {}

    public static final String PREF_MIC = "rec_mic";

    public static boolean micEnabled() {
        return com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_MIC, false);
    }

    public static void setMicEnabled(boolean on) {
        com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_MIC, on).apply();
    }

    public static boolean isRecording() {
        return RecorderService.sRecording;
    }

    public static Intent permissionIntent(Context c) {
        return ((MediaProjectionManager) c.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent();
    }

    public static void start(Context c, int resultCode, Intent data) {
        Intent i = new Intent(c, RecorderService.class).setAction(RecorderService.ACTION_START)
                .putExtra(RecorderService.EXTRA_CODE, resultCode)
                .putExtra(RecorderService.EXTRA_DATA, data)
                .putExtra(RecorderService.EXTRA_MIC, micEnabled() && c.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        ContextCompat.startForegroundService(c, i);
    }

    public static void stop(Context c) {
        c.startService(new Intent(c, RecorderService.class).setAction(RecorderService.ACTION_STOP));
    }
}
