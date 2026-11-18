package com.divinedoll.launcher.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Panel that slides up from the bottom, used instead of centered dialogs and menus. */
public final class BottomSheet {
    private BottomSheet() {
    }

    public interface OnPick {
        void pick(int index);
    }

    private static int dp(Context c, int v) {
        return ForkUi.dp(c, v);
    }

    /** Shows the content inside a rounded sheet with a handle. The dialog is returned so it can be dismissed. */
    public static Dialog show(Context c, View content) {
        Dialog dialog = new Dialog(c);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = new LinearLayout(c);
        sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        float r = dp(c, 24);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        bg.setColor(ThemeColors.sidebar());
        bg.setStroke(dp(c, 1), 0x1FFFFFFF);
        sheet.setBackground(bg);
        sheet.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 18));

        View handle = new View(c);
        GradientDrawable h = new GradientDrawable();
        h.setCornerRadius(dp(c, 3));
        h.setColor(0x33FFFFFF);
        handle.setBackground(h);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(c, 38), dp(c, 4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(c, 10);
        sheet.addView(handle, hlp);

        ScrollView scroll = new ScrollView(c);
        scroll.addView(content);
        sheet.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout host = new FrameLayout(c);
        host.setOnClickListener(v -> dialog.dismiss());
        int width = Math.min(c.getResources().getDisplayMetrics().widthPixels, dp(c, 520));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        sheet.setClickable(true);
        host.addView(sheet, slp);
        dialog.setContentView(host);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setDimAmount(0.6f);
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.show();
        sheet.post(() -> {
            sheet.setTranslationY(sheet.getHeight());
            sheet.animate().translationY(0).setDuration(180).start();
        });
        DivineTheme.styleTree(content);
        return dialog;
    }

    /** One tappable row: icon, text, optionally in red. */
    public static View row(Context c, int iconRes, CharSequence label, boolean danger, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 14));
        g.setColor(ThemeColors.panel());
        row.setBackground(g);
        row.setClickable(true);
        int color = danger ? 0xFFFF8A8A : Color.WHITE;
        if (iconRes != 0) {
            ImageView icon = new ImageView(c);
            icon.setImageResource(iconRes);
            icon.setColorFilter(danger ? color : AccentColor.get());
            row.addView(icon, new LinearLayout.LayoutParams(dp(c, 20), dp(c, 20)));
        }
        TextView t = ForkUi.text(c, label.toString(), 14, false);
        t.setTextColor(color);
        t.setPadding(iconRes != 0 ? dp(c, 12) : 0, 0, 0, 0);
        row.addView(t);
        row.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 8);
        row.setLayoutParams(lp);
        return row;
    }

    /** A sheet with a title and a list of actions. */
    public static Dialog menu(Context c, CharSequence title, int[] icons, CharSequence[] labels, boolean[] danger, OnPick onPick) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        if (title != null && title.length() > 0) {
            TextView t = ForkUi.text(c, title.toString(), 16, true);
            t.setPadding(dp(c, 4), 0, 0, dp(c, 10));
            box.addView(t);
        }
        final Dialog[] holder = new Dialog[1];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            box.addView(row(c, icons == null ? 0 : icons[i], labels[i], danger != null && danger[i], v -> {
                if (holder[0] != null) holder[0].dismiss();
                onPick.pick(index);
            }));
        }
        holder[0] = show(c, box);
        return holder[0];
    }
}
