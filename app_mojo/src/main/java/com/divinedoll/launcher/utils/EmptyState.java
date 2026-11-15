package com.divinedoll.launcher.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Icon, a line that says what is missing and a button with the action. Never a blank screen. */
public final class EmptyState {
    private EmptyState() {
    }

    public static View view(Context c, int iconRes, CharSequence title, CharSequence text, CharSequence action, View.OnClickListener click) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(ForkUi.dp(c, 24), ForkUi.dp(c, 40), ForkUi.dp(c, 24), ForkUi.dp(c, 24));

        FrameLayout big = new FrameLayout(c);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(ForkUi.dp(c, 20));
        g.setColor(ThemeColors.panel());
        g.setStroke(ForkUi.dp(c, 1), 0x33FFFFFF, ForkUi.dp(c, 3), ForkUi.dp(c, 3));
        big.setBackground(g);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setColorFilter(AccentColor.get());
        big.addView(icon, new FrameLayout.LayoutParams(ForkUi.dp(c, 28), ForkUi.dp(c, 28), Gravity.CENTER));
        box.addView(big, new LinearLayout.LayoutParams(ForkUi.dp(c, 64), ForkUi.dp(c, 64)));

        TextView t = ForkUi.text(c, title.toString(), 17, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, ForkUi.dp(c, 14), 0, ForkUi.dp(c, 4));
        box.addView(t);
        TextView d = ForkUi.text(c, text.toString(), 13, false);
        d.setGravity(Gravity.CENTER);
        d.setTextColor(0xFF9AA1AD);
        box.addView(d, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (action != null && click != null) {
            TextView b = ForkUi.text(c, action.toString(), 14, true);
            b.setGravity(Gravity.CENTER);
            b.setTextColor(DivineTheme.onColor(AccentColor.get()));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(ForkUi.dp(c, 30));
            bg.setColor(AccentColor.get());
            b.setBackground(bg);
            b.setPadding(ForkUi.dp(c, 22), ForkUi.dp(c, 11), ForkUi.dp(c, 22), ForkUi.dp(c, 11));
            b.setOnClickListener(click);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = ForkUi.dp(c, 16);
            box.addView(b, lp);
        }
        return box;
    }
}
