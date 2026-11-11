package com.divinedoll.launcher.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.prefs.LauncherPreferences;

/** Three one-tap profiles that set the memory and the render resolution. */
public final class PerformanceProfiles {
    private PerformanceProfiles() {}

    public static void show(Context c, Runnable after) {
        CharSequence[] items = {c.getString(R.string.fork_pp_saver), c.getString(R.string.fork_pp_balanced), c.getString(R.string.fork_pp_max)};
        new AlertDialog.Builder(c).setTitle(R.string.fork_pp_title).setItems(items, (d, which) -> {
            apply(c, which);
            Toast.makeText(c, c.getString(R.string.fork_pp_applied, items[which]), Toast.LENGTH_LONG).show();
            if(after != null) after.run();
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    /** 0 saver, 1 balanced, 2 max. */
    public static void apply(Context c, int profile) {
        int total = Tools.getTotalDeviceMemory(c);
        int base;
        if(total < 2048) base = 656; else if(total < 3064) base = 936; else if(total < 4096) base = 1144;
        else if(total < 6144) base = 1536; else base = 2048;
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        int minSide = Math.min(m.widthPixels, m.heightPixels);
        int step = Math.max(1, c.getResources().getInteger(R.integer.resolution_seekbar_increment));
        int ram, ratio;
        if(profile == 0) {
            ram = Math.max(512, base * 7 / 10);
            ratio = Math.min(100, (int) (Math.ceil(100f * 540 / minSide / step) * step));
        } else if(profile == 2) {
            ram = Math.min(total * 6 / 10, Math.max(base, base * 13 / 10));
            ratio = 100;
        } else {
            ram = base;
            ratio = Math.min(100, (int) (Math.ceil(100f * 720 / minSide / step) * step));
        }
        ratio = Math.max(step, ratio);
        LauncherPreferences.DEFAULT_PREF.edit().putInt("allocation", ram).putInt("resolutionRatio", ratio).apply();
        LauncherPreferences.loadPreferences(c.getApplicationContext());
    }
}
