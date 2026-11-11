package com.divinedoll.launcher.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.divinedoll.launcher.prefs.LauncherPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Own color and background image of an instance. */
public final class InstanceLook {
    private InstanceLook() {}

    private static String key(String name) { return "inst_color_" + name; }

    /** The color chosen for the instance, or 0 when it has none. */
    public static int color(String name) {
        return LauncherPreferences.DEFAULT_PREF.getInt(key(name), 0);
    }

    public static void setColor(String name, int color) {
        if(color == 0) LauncherPreferences.DEFAULT_PREF.edit().remove(key(name)).apply();
        else LauncherPreferences.DEFAULT_PREF.edit().putInt(key(name), color | 0xFF000000).apply();
    }

    private static File imageFile(Context c, String name) {
        File dir = new File(c.getFilesDir(), "instance_bg");
        return new File(dir, name.replaceAll("[^A-Za-z0-9_-]", "_") + ".jpg");
    }

    public static Bitmap image(Context c, String name) {
        File f = imageFile(c, name);
        return f.isFile() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    public static boolean hasImage(Context c, String name) {
        return imageFile(c, name).isFile();
    }

    public static void clearImage(Context c, String name) {
        imageFile(c, name).delete();
    }

    /** Saves a picture scaled to at most 1280 px wide. */
    public static void setImage(Context c, String name, Uri uri) throws Exception {
        Bitmap src;
        try(InputStream in = c.getContentResolver().openInputStream(uri)) { src = BitmapFactory.decodeStream(in); }
        if(src == null) throw new java.io.IOException("image");
        if(src.getWidth() > 1280) {
            int h = src.getHeight() * 1280 / src.getWidth();
            src = Bitmap.createScaledBitmap(src, 1280, Math.max(1, h), true);
        }
        File f = imageFile(c, name);
        f.getParentFile().mkdirs();
        try(FileOutputStream out = new FileOutputStream(f)) { src.compress(Bitmap.CompressFormat.JPEG, 88, out); }
    }
}
