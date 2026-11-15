package com.divinedoll.launcher.utils;

import com.divinedoll.launcher.prefs.LauncherPreferences;

/** Launcher-wide text size: small, normal or large. Applied by ForkUi when it builds text. */
public final class TextScale {
    private TextScale() {}
    private static final String PREF = "ui_text_level";
    private static final float[] FACTORS = {0.88f, 1f, 1.14f};

    public static int level() {
        try {
            return Math.max(0, Math.min(2, LauncherPreferences.DEFAULT_PREF.getInt(PREF, 1)));
        } catch (Exception e) { return 1; }
    }

    public static void set(int level) {
        LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF, level).apply();
    }

    public static float factor() { return FACTORS[level()]; }
}
