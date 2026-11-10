package com.divinedoll.launcher.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Own icon for every installation: a rounded tile whose colors come from the name, with its first letter. */
public final class InstanceAvatar {
    private InstanceAvatar() {}

    public static Bitmap make(String name, int size) {
        String text = name == null ? "" : name.trim();
        int hash = Math.abs(text.toLowerCase(java.util.Locale.ROOT).hashCode());
        float hue = hash % 360;
        int top = Color.HSVToColor(new float[]{hue, 0.50f, 0.98f});
        int bottom = Color.HSVToColor(new float[]{(hue + 28f) % 360f, 0.78f, 0.62f});
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, 0, size, size, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0, 0, size, size), size * 0.24f, size * 0.24f, paint);
        Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        shine.setColor(0x22FFFFFF);
        canvas.drawRoundRect(new RectF(size * 0.06f, size * 0.06f, size * 0.94f, size * 0.46f), size * 0.2f, size * 0.2f, shine);
        String letter = text.isEmpty() ? "?" : text.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setColor(0xFFFFFFFF);
        t.setTextSize(size * 0.54f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextAlign(Paint.Align.CENTER);
        t.setShadowLayer(size * 0.03f, 0, size * 0.02f, 0x66000000);
        canvas.drawText(letter, size / 2f, size / 2f - (t.ascent() + t.descent()) / 2f, t);
        return bitmap;
    }
}
