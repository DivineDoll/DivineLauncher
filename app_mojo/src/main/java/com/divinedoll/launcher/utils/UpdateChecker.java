package com.divinedoll.launcher.utils;

import com.divinedoll.launcher.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks at the newest GitHub release of the project. Releases are tagged debug-r&lt;run&gt;-&lt;sha&gt;,
 * and the app's version code is the build run number, so a bigger number means a newer build.
 */
public final class UpdateChecker {
    public static final String API = "https://api.github.com/repos/DivineDoll/DivineLauncher/releases?per_page=5";
    private static final Pattern TAG = Pattern.compile("debug-r(\\d+)-");
    private static boolean sChecked;
    private static String[] sResult; // {tag, url} when an update exists

    public interface Callback { void onUpdate(String tag, String url); }

    private UpdateChecker() {}

    public static void check(Callback cb) {
        if(sChecked) {
            if(sResult != null) cb.onUpdate(sResult[0], sResult[1]);
            return;
        }
        sChecked = true;
        new Thread(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(API).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                con.setRequestProperty("Accept", "application/vnd.github+json");
                try(InputStream in = con.getInputStream()) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    JSONArray arr = new JSONArray(out.toString("UTF-8"));
                    int best = BuildConfig.VERSION_CODE;
                    for(int i = 0; i < arr.length(); i++) {
                        JSONObject rel = arr.getJSONObject(i);
                        Matcher m = TAG.matcher(rel.optString("tag_name"));
                        if(!m.find()) continue;
                        int run = Integer.parseInt(m.group(1));
                        if(run > best) {
                            best = run;
                            sResult = new String[]{rel.getString("tag_name"), rel.getString("html_url")};
                        }
                    }
                }
                if(sResult != null) {
                    final String[] r = sResult;
                    com.divinedoll.launcher.Tools.runOnUiThread(() -> cb.onUpdate(r[0], r[1]));
                }
            } catch (Exception ignored) {
                sChecked = false; // no internet: try again next time
            }
        }).start();
    }
}
