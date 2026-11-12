package com.divinedoll.launcher.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;

import com.divinedoll.launcher.BuildConfig;
import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.prefs.LauncherPreferences;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Once a day asks GitHub for the latest release and says so when it is a new one. Silent when there is none or no internet. */
public final class UpdateCheck {
    private UpdateCheck() {}

    private static final String API = "https://api.github.com/repos/DivineDoll/DivineLauncher/releases/latest";
    private static final String PREF_LAST = "update_last_check", PREF_SEEN = "update_seen_tag", PREF_OFF = "update_check_off";
    private static boolean sDone;

    public static void run(Activity act) {
        if(sDone) return;
        sDone = true;
        SharedPreferences prefs = LauncherPreferences.DEFAULT_PREF;
        if(prefs.getBoolean(PREF_OFF, false)) return;
        long now = System.currentTimeMillis();
        if(now - prefs.getLong(PREF_LAST, 0) < 24L * 3600 * 1000) return;
        new Thread(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(API).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                con.setRequestProperty("Accept", "application/vnd.github+json");
                con.setRequestProperty("User-Agent", "DivineDoll/DivineLauncher");
                if(con.getResponseCode() != 200) { prefs.edit().putLong(PREF_LAST, now).apply(); return; }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try(InputStream in = con.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                JsonObject json = new Gson().fromJson(out.toString("UTF-8"), JsonObject.class);
                prefs.edit().putLong(PREF_LAST, now).apply();
                if(json == null || !json.has("tag_name")) return;
                String tag = json.get("tag_name").getAsString();
                String url = json.has("html_url") ? json.get("html_url").getAsString() : "https://github.com/DivineDoll/DivineLauncher/releases";
                String bare = tag.replaceFirst("^[vV]", "");
                if(bare.isEmpty() || BuildConfig.VERSION_NAME.contains(bare) || tag.equals(prefs.getString(PREF_SEEN, ""))) return;
                prefs.edit().putString(PREF_SEEN, tag).apply();
                Tools.runOnUiThread(() -> {
                    if(act.isFinishing() || act.isDestroyed()) return;
                    new AlertDialog.Builder(act).setTitle(R.string.fork_update_title)
                            .setMessage(act.getString(R.string.fork_update_message, tag))
                            .setPositiveButton(R.string.fork_update_view, (d, w) -> Tools.openURL(act, url))
                            .setNegativeButton(R.string.fork_update_later, null).show();
                });
            } catch(Exception ignored) {
            }
        }).start();
    }
}
