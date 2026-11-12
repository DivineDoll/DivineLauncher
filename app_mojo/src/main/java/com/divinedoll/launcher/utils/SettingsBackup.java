package com.divinedoll.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.prefs.LauncherPreferences;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Saves the launcher settings (look, controls, profiles) to a JSON file and loads them back. No accounts, no keys. */
public final class SettingsBackup {
    private SettingsBackup() {}

    private static boolean skip(String key) {
        String k = key.toLowerCase();
        return k.contains("token") || k.contains("secret") || k.contains("password") || k.contains("account")
                || k.contains("profile") || key.equals(BedrockContent.PREF_KEY);
    }

    public static File write(Context c) throws Exception {
        JsonObject root = new JsonObject();
        for(Map.Entry<String, ?> en : LauncherPreferences.DEFAULT_PREF.getAll().entrySet()) {
            if(skip(en.getKey()) || en.getValue() == null) continue;
            JsonObject item = new JsonObject();
            Object v = en.getValue();
            if(v instanceof Boolean) { item.addProperty("t", "b"); item.addProperty("v", (Boolean) v); }
            else if(v instanceof Integer) { item.addProperty("t", "i"); item.addProperty("v", (Integer) v); }
            else if(v instanceof Long) { item.addProperty("t", "l"); item.addProperty("v", (Long) v); }
            else if(v instanceof Float) { item.addProperty("t", "f"); item.addProperty("v", (Float) v); }
            else if(v instanceof String) { item.addProperty("t", "s"); item.addProperty("v", (String) v); }
            else if(v instanceof Set) {
                item.addProperty("t", "S");
                JsonArray arr = new JsonArray();
                for(Object o : (Set<?>) v) arr.add(String.valueOf(o));
                item.add("v", arr);
            } else continue;
            root.add(en.getKey(), item);
        }
        JsonObject out = new JsonObject();
        out.addProperty("app", "DivineLauncher");
        out.addProperty("format", 1);
        out.add("prefs", root);
        File dir = new File(c.getCacheDir(), "bedrock");
        if(!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("cache");
        File file = new File(dir, "divinelauncher-settings.json");
        try(FileOutputStream os = new FileOutputStream(file)) {
            os.write(out.toString().getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    /** Returns how many settings were loaded. */
    public static int read(Context c, Uri uri) throws Exception {
        String text;
        try(InputStream in = c.getContentResolver().openInputStream(uri)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            text = bos.toString("UTF-8");
        }
        JsonObject root = new Gson().fromJson(text, JsonObject.class);
        if(root == null || !root.has("prefs") || !"DivineLauncher".equals(root.get("app").getAsString()))
            throw new IllegalArgumentException(c.getString(R.string.fork_settings_invalid));
        SharedPreferences.Editor ed = LauncherPreferences.DEFAULT_PREF.edit();
        int count = 0;
        for(Map.Entry<String, JsonElement> en : root.getAsJsonObject("prefs").entrySet()) {
            if(skip(en.getKey())) continue;
            JsonObject item = en.getValue().getAsJsonObject();
            JsonElement v = item.get("v");
            switch(item.get("t").getAsString()) {
                case "b": ed.putBoolean(en.getKey(), v.getAsBoolean()); break;
                case "i": ed.putInt(en.getKey(), v.getAsInt()); break;
                case "l": ed.putLong(en.getKey(), v.getAsLong()); break;
                case "f": ed.putFloat(en.getKey(), v.getAsFloat()); break;
                case "s": ed.putString(en.getKey(), v.getAsString()); break;
                case "S":
                    Set<String> set = new HashSet<>();
                    for(JsonElement e : v.getAsJsonArray()) set.add(e.getAsString());
                    ed.putStringSet(en.getKey(), set);
                    break;
                default: continue;
            }
            count++;
        }
        ed.apply();
        LauncherPreferences.loadPreferences(c.getApplicationContext());
        return count;
    }
}
