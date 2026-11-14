package com.divinedoll.launcher.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.fragments.InstallationsFragment;
import com.divinedoll.launcher.fragments.PatchNotesFragment;
import com.divinedoll.launcher.fragments.SkinsFragment;

/** Small helpers to build the launcher-style (dark + green) screens in code. */
public class ForkUi {
    public static final int GREEN = 0xFF3C8527;
    public static final int CARD = 0xFF3A3A3A;
    public static final int CARD_SELECTED = 0xFF4E4E4E;
    public static final int PANEL = 0xFF2B2B2B;
    public static final int BG = 0xFF1E1E1E;
    public static final int SIDEBAR = 0xFF262626;
    public static final int TAB_IDLE = 0xFFB8B8B8;

    public static final int TAB_PLAY = 0, TAB_INSTALLS = 1, TAB_SKINS = 2, TAB_PATCH = 3;

    public static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable box(Context c, int fill, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if(UiStyle.rounded()) g.setCornerRadius(dp(c, 10));
        if(strokeDp > 0) g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    public static GradientDrawable dashed(Context c, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if(UiStyle.rounded()) g.setCornerRadius(dp(c, 10));
        g.setStroke(dp(c, 2), stroke, dp(c, 6), dp(c, 4));
        return g;
    }

    public static TextView text(Context c, CharSequence content, int sp, boolean bold) {
        TextView t = new TextView(c);
        t.setText(content);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if(bold) McStyle.headingText(t); else McStyle.text(t);
        return t;
    }

    /** In-game style button. green = the main action (Play color), otherwise gray. */
    public static Button button(Context c, CharSequence label, boolean green) {
        Button b = new ClickSound.SoundButton(c);
        b.setText(label);
        McStyle.button(b, green);
        b.setMinHeight(dp(c, 40));
        b.setMinimumHeight(dp(c, 40));
        return b;
    }

    /** Small button that does not force the wide default minimum width. */
    public static Button compact(Button b) {
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(b.getContext(), 36));
        b.setMinimumHeight(dp(b.getContext(), 36));
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setPadding(dp(b.getContext(), 12), 0, dp(b.getContext(), 12), 0);
        return b;
    }

    public static LinearLayout.LayoutParams match(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Moves to a tab of the launcher header, replacing the current tab screen instead of stacking them. */
    public static void openTab(FragmentActivity act, int tab) {
        Class<? extends Fragment> cls;
        String tag;
        switch (tab) {
            case TAB_INSTALLS: cls = InstallationsFragment.class; tag = InstallationsFragment.TAG; break;
            case TAB_SKINS: cls = SkinsFragment.class; tag = SkinsFragment.TAG; break;
            case TAB_PATCH: cls = PatchNotesFragment.class; tag = PatchNotesFragment.TAG; break;
            default: Tools.backToMainMenu(act); return;
        }
        Tools.backToMainMenu(act);
        Tools.swapFragment(act, cls, tag, null);
    }

    /** Title of a main screen. Moving between screens is done with the navigation bar and drawer. */
    public static LinearLayout pageHeader(Context c, FragmentActivity act, int selected) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int label;
        switch (selected) {
            case TAB_INSTALLS: label = R.string.fork_installs_title; break;
            case TAB_SKINS: label = R.string.fork_skin_title; break;
            case TAB_PATCH: label = R.string.fork_patch_title; break;
            default: label = R.string.fork_nav_home; break;
        }
        TextView title = text(c, c.getString(label), 22, true);
        title.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 2));
        box.addView(title);
        View line = new View(c);
        line.setBackgroundColor(AccentColor.get());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 36), dp(c, 3));
        lp.leftMargin = dp(c, 16);
        lp.bottomMargin = dp(c, 8);
        box.addView(line, lp);
        return box;
    }
}
