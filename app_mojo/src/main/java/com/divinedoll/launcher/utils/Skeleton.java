package com.divinedoll.launcher.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** Gray bars that pulse while a list loads, instead of an empty screen. */
public final class Skeleton {
    private Skeleton() {
    }

    public static View rows(Context c, int count) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < count; i++) {
            View bar = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(ForkUi.dp(c, 16));
            g.setColor(ThemeColors.panel());
            bar.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 62));
            lp.bottomMargin = ForkUi.dp(c, 8);
            box.addView(bar, lp);
        }
        ValueAnimator pulse = ValueAnimator.ofFloat(0.45f, 1f);
        pulse.setDuration(800);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(a -> box.setAlpha((float) a.getAnimatedValue()));
        box.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { pulse.start(); }
            @Override public void onViewDetachedFromWindow(View v) { pulse.cancel(); }
        });
        return box;
    }
}
