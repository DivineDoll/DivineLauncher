package com.divinedoll.launcher.modding.modpacks;

import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.modding.modpacks.models.ModItem;
import com.divinedoll.launcher.prefs.LauncherPreferences;

import java.util.HashSet;
import java.util.Set;

/** Remembers which mods, shaders and packs were installed from the launcher, per instance. */
public final class InstalledMods {
    private static final String PREF = "installed_mods_set";
    /** Called on the UI thread when something new was installed, so lists can refresh. */
    public static volatile Runnable sListener;

    private InstalledMods() {}

    private static String key(ModItem item) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) return null;
        return instance.getGameDirectory().getAbsolutePath() + "|" + item.apiSource + "_" + item.id;
    }

    public static boolean has(ModItem item) {
        if(item == null || item.isModpack || item.id == null) return false;
        try {
            String k = key(item);
            return k != null && LauncherPreferences.DEFAULT_PREF.getStringSet(PREF, new HashSet<>()).contains(k);
        } catch (Exception e) {
            return false;
        }
    }

    public static void mark(ModItem item) {
        try {
            String k = key(item);
            if(k == null) return;
            Set<String> set = new HashSet<>(LauncherPreferences.DEFAULT_PREF.getStringSet(PREF, new HashSet<>()));
            set.add(k);
            LauncherPreferences.DEFAULT_PREF.edit().putStringSet(PREF, set).apply();
            Runnable l = sListener;
            if(l != null) Tools.runOnUiThread(l);
        } catch (Exception ignored) {
        }
    }
}
