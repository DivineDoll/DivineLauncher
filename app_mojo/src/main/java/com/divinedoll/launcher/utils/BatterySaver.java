package com.divinedoll.launcher.utils;

import android.app.Activity;
import android.view.WindowManager;

import com.divinedoll.launcher.prefs.LauncherPreferences;

/** One switch in the in-game quick settings that dims the screen to spare the battery. */
public final class BatterySaver {
    private BatterySaver() {}

    public static final String PREF = "battery_saver_mode";
    private static final float DIMMED = 0.35f;

    public static boolean enabled() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF, false);
    }

    public static void setEnabled(Activity a, boolean on) {
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF, on).apply();
        apply(a);
    }

    public static void apply(Activity a) {
        WindowManager.LayoutParams lp = a.getWindow().getAttributes();
        lp.screenBrightness = enabled() ? DIMMED : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        a.getWindow().setAttributes(lp);
    }
}
