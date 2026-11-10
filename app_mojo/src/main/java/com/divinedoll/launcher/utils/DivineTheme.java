package com.divinedoll.launcher.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.prefs.LauncherPreferences;

/**
 * "Divine" look: flat dark surfaces, rounded cards, pill tabs and a gold accent, in the spirit of
 * modern mod managers. Everything is customizable from Appearance; this class only holds the
 * defaults and the shape helpers.
 */
public final class DivineTheme {
    private DivineTheme() {}

    public static final int GOLD = 0xFFFFC107;
    public static final int BG = 0xFF16181C;
    public static final int SIDEBAR = 0xFF1B1D21;
    private static final String PREF_APPLIED = "divine_theme_v1";
    public static final float RAIL_PERCENT = 0.09f;

    /** First run of this look: gold colors, rounded shapes. The user can change all of it afterwards. */
    public static void applyOnce() {
        try {
            if(LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_APPLIED, false)) return;
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putInt(AccentColor.PREF, GOLD)
                    .putInt(PlayColor.PREF, GOLD)
                    .putInt(ThemeColors.PREF_BACKGROUND, BG)
                    .putInt(ThemeColors.PREF_SIDEBAR, SIDEBAR)
                    .putBoolean(UiStyle.PREF, true)
                    .putBoolean(PREF_APPLIED, true)
                    .apply();
        } catch (Exception ignored) {}
    }

    /** Black or white, whichever reads better on top of the given color. */
    public static int onColor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF1B1D21 : 0xFFFFFFFF;
    }

    private static GradientDrawable pill(Context c, int fill, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(ForkUi.dp(c, radiusDp));
        return g;
    }

    /** Sidebar entry: transparent, accent tint when pressed, stronger tint when it is the current one. */
    public static Drawable sideItem(Context c, boolean selected) {
        int accent = AccentColor.get() & 0x00FFFFFF;
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, inset(c, pill(c, accent | 0x55000000, 12)));
        s.addState(new int[]{}, inset(c, pill(c, selected ? (accent | 0x30000000) : Color.TRANSPARENT, 12)));
        return s;
    }

    private static Drawable inset(Context c, Drawable d) {
        return new InsetDrawable(d, ForkUi.dp(c, 6), ForkUi.dp(c, 2), ForkUi.dp(c, 6), ForkUi.dp(c, 2));
    }

    /** Tab pill: filled with a soft accent when selected. */
    public static Drawable tabPill(Context c, boolean selected) {
        return pill(c, selected ? ((AccentColor.get() & 0x00FFFFFF) | 0x33000000) : Color.TRANSPARENT, 18);
    }

    public static void roundCorners(View v, int radiusDp) {
        final float r = ForkUi.dp(v.getContext(), radiusDp);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        v.setClipToOutline(true);
    }

    /** Restyles the main screen: rounded banner card, pill sidebar, flat bottom bar, dark text on light Play. */
    public static void styleLauncher(View root, TextView playButton, View currentSidebarItem) {
        Context c = root.getContext();
        if(!UiStyle.rounded()) return;
        View bar = root.findViewById(R.id.sidebar_scroll);
        if(bar instanceof android.widget.FrameLayout && ((android.widget.FrameLayout) bar).getChildCount() > 0) {
            ViewGroup list = (ViewGroup) ((android.widget.FrameLayout) bar).getChildAt(0);
            for(int i = 0; i < list.getChildCount(); i++) {
                View item = list.getChildAt(i);
                item.setBackground(sideItem(c, item == currentSidebarItem));
            }
        }
        if(isPortrait(c)) gridify(root, currentSidebarItem);
        View banner = root.findViewById(R.id.bedrock_banner);
        if(banner != null) {
            roundCorners(banner, 16);
            if(banner.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) banner.getLayoutParams();
                lp.setMargins(ForkUi.dp(c, 12), ForkUi.dp(c, 4), ForkUi.dp(c, 12), ForkUi.dp(c, 8));
                banner.setLayoutParams(lp);
            }
        }
        View bottom = root.findViewById(R.id._background_display_view);
        if(bottom != null) bottom.setBackground(new ColorDrawable(ThemeColors.sidebar()));
        if(playButton != null) playButton.setTextColor(onColor(PlayColor.get()));
    }

    public static boolean isPortrait(Context c) {
        return c.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
    }

    /** Portrait: the long menu list becomes a grid of two columns of rounded tiles. */
    private static void gridify(View root, View current) {
        Context c = root.getContext();
        View bar = root.findViewById(R.id.sidebar_scroll);
        if(!(bar instanceof android.widget.FrameLayout) || ((android.widget.FrameLayout) bar).getChildCount() == 0) return;
        android.widget.FrameLayout scroll = (android.widget.FrameLayout) bar;
        View first = scroll.getChildAt(0);
        if(!(first instanceof ViewGroup)) return;
        ViewGroup list = (ViewGroup) first;
        java.util.List<View> items = new java.util.ArrayList<>();
        for(int i = 0; i < list.getChildCount(); i++) items.add(list.getChildAt(i));
        android.widget.GridLayout grid = new android.widget.GridLayout(c);
        grid.setColumnCount(2);
        grid.setPadding(ForkUi.dp(c, 8), ForkUi.dp(c, 8), ForkUi.dp(c, 8), ForkUi.dp(c, 8));
        list.removeAllViews();
        for(View item : items) {
            if(!(item instanceof TextView)) continue;
            TextView t = (TextView) item;
            GradientDrawable tile = new GradientDrawable();
            boolean selected = item == current;
            tile.setColor(selected ? ((AccentColor.get() & 0x00FFFFFF) | 0x30000000) : ThemeColors.panel());
            tile.setCornerRadius(ForkUi.dp(c, 14));
            tile.setStroke(ForkUi.dp(c, 1), selected ? AccentColor.get() : 0x22FFFFFF);
            t.setBackground(tile);
            t.setTextSize(13);
            t.setMaxLines(2);
            t.setPaddingRelative(ForkUi.dp(c, 10), 0, ForkUi.dp(c, 6), 0);
            t.setCompoundDrawablePadding(ForkUi.dp(c, 8));
            android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams(
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.height = ForkUi.dp(c, 54);
            lp.setMargins(ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4));
            grid.addView(t, lp);
        }
        scroll.removeAllViews();
        scroll.addView(grid, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Landscape: the banner becomes a shorter header so a content panel fits under it. */
    public static void shortenBanner(View root, float percent) {
        View banner = root.findViewById(R.id.bedrock_banner);
        if(banner != null && banner.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) banner.getLayoutParams();
            lp.height = 0;
            lp.matchConstraintDefaultHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_PERCENT;
            lp.matchConstraintPercentHeight = percent;
            lp.verticalBias = 0f;
            banner.setLayoutParams(lp);
        }
    }

    // ------------------------------------------------------------------ widgets

    /** Slim sliders with an accent fill and a round thumb; round switches. Applied to a whole view tree. */
    public static void styleTree(View v) {
        if(v == null || !UiStyle.rounded()) return;
        if(v instanceof android.widget.SeekBar) {
            styleSeek((android.widget.SeekBar) v);
        } else if(v instanceof androidx.appcompat.widget.SwitchCompat) {
            styleSwitch((androidx.appcompat.widget.SwitchCompat) v);
        } else if(v instanceof androidx.recyclerview.widget.RecyclerView) {
            hookRecycler((androidx.recyclerview.widget.RecyclerView) v);
        }
        if(v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for(int i = 0; i < group.getChildCount(); i++) styleTree(group.getChildAt(i));
        }
    }

    private static void hookRecycler(androidx.recyclerview.widget.RecyclerView list) {
        if(list.getTag(R.id.divine_styled) != null) return;
        list.setTag(R.id.divine_styled, Boolean.TRUE);
        list.addOnChildAttachStateChangeListener(new androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener() {
            @Override public void onChildViewAttachedToWindow(View view) { styleTree(view); }
            @Override public void onChildViewDetachedFromWindow(View view) {}
        });
    }

    private static GradientDrawable bar(Context c, int color, int heightDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(ForkUi.dp(c, heightDp) / 2f);
        g.setSize(1, ForkUi.dp(c, heightDp));
        return g;
    }

    private static void styleSeek(android.widget.SeekBar seek) {
        Context c = seek.getContext();
        int accent = AccentColor.get();
        android.graphics.drawable.ClipDrawable fill = new android.graphics.drawable.ClipDrawable(
                bar(c, accent, 4), android.view.Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);
        android.graphics.drawable.LayerDrawable track = new android.graphics.drawable.LayerDrawable(
                new Drawable[]{bar(c, 0x33FFFFFF, 4), fill});
        track.setId(0, android.R.id.background);
        track.setId(1, android.R.id.progress);
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(accent);
        thumb.setStroke(ForkUi.dp(c, 2), 0xFFFFFFFF);
        thumb.setSize(ForkUi.dp(c, 20), ForkUi.dp(c, 20));
        seek.setProgressDrawable(track);
        seek.setThumb(thumb);
        seek.setSplitTrack(false);
    }

    private static void styleSwitch(androidx.appcompat.widget.SwitchCompat sw) {
        Context c = sw.getContext();
        int accent = AccentColor.get();
        GradientDrawable on = new GradientDrawable();
        on.setColor(accent);
        on.setCornerRadius(ForkUi.dp(c, 12));
        on.setSize(ForkUi.dp(c, 44), ForkUi.dp(c, 24));
        GradientDrawable off = new GradientDrawable();
        off.setColor(0x44FFFFFF);
        off.setCornerRadius(ForkUi.dp(c, 12));
        off.setSize(ForkUi.dp(c, 44), ForkUi.dp(c, 24));
        StateListDrawable track = new StateListDrawable();
        track.addState(new int[]{android.R.attr.state_checked}, on);
        track.addState(new int[]{}, off);
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(0xFFFFFFFF);
        thumb.setSize(ForkUi.dp(c, 18), ForkUi.dp(c, 18));
        sw.setTrackDrawable(track);
        sw.setThumbDrawable(new InsetDrawable(thumb, ForkUi.dp(c, 3)));
    }

    /** Bedrock screen: same rounded banner, pill sidebar and flat bottom bar as the Java one. */
    public static void styleBedrock(View root, TextView playButton) {
        if(!UiStyle.rounded()) return;
        Context c = root.getContext();
        View bar = root.findViewById(R.id.bd_sidebar_scroll);
        if(bar != null) bar.setBackground(new ColorDrawable(ThemeColors.sidebar()));
        if(bar instanceof android.widget.FrameLayout && ((android.widget.FrameLayout) bar).getChildCount() > 0
                && ((android.widget.FrameLayout) bar).getChildAt(0) instanceof ViewGroup) {
            ViewGroup list = (ViewGroup) ((android.widget.FrameLayout) bar).getChildAt(0);
            for(int i = 0; i < list.getChildCount(); i++) list.getChildAt(i).setBackground(sideItem(c, false));
        }
        View banner = root.findViewById(R.id.bd_banner);
        if(banner != null) {
            roundCorners(banner, 16);
            if(banner.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) banner.getLayoutParams();
                lp.setMargins(ForkUi.dp(c, 12), ForkUi.dp(c, 4), ForkUi.dp(c, 12), ForkUi.dp(c, 8));
                banner.setLayoutParams(lp);
            }
        }
        View bottom = root.findViewById(R.id._background_display_view);
        if(bottom != null) bottom.setBackground(new ColorDrawable(ThemeColors.sidebar()));
        if(playButton != null) playButton.setTextColor(onColor(PlayColor.get()));
    }

    /** Icons-only sidebar, like a narrow rail. Labels stay as content descriptions. */
    public static void applyRail(View root) {
        if(!UiStyle.iconRail()) return;
        Context c = root.getContext();
        // The rail only makes sense next to the content (landscape); in portrait the menu is a list under the banner
        if(c.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) return;
        View g = root.findViewById(R.id.sidebar_guideline);
        if(g instanceof Guideline) ((Guideline) g).setGuidelinePercent(RAIL_PERCENT);
        View bar = root.findViewById(R.id.sidebar_scroll);
        if(!(bar instanceof android.widget.FrameLayout) || ((android.widget.FrameLayout) bar).getChildCount() == 0) return;
        ViewGroup list = (ViewGroup) ((android.widget.FrameLayout) bar).getChildAt(0);
        int railPx = Math.round(c.getResources().getDisplayMetrics().widthPixels * RAIL_PERCENT);
        int iconPx = c.getResources().getDimensionPixelSize(R.dimen._30sdp);
        int pad = Math.max(0, (railPx - iconPx) / 2);
        for(int i = 0; i < list.getChildCount(); i++) {
            View item = list.getChildAt(i);
            if(item instanceof TextView) {
                TextView t = (TextView) item;
                t.setContentDescription(t.getText());
                t.setText("");
                t.setCompoundDrawablePadding(0);
                t.setPaddingRelative(pad, 0, 0, 0);
            }
        }
    }
}
