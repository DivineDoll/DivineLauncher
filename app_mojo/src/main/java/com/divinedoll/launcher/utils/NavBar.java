package com.divinedoll.launcher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.divinedoll.launcher.CustomControlsActivity;
import com.divinedoll.launcher.LauncherActivity;
import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.fragments.InstallationsFragment;
import com.divinedoll.launcher.fragments.LanFragment;
import com.divinedoll.launcher.fragments.MainMenuFragment;
import com.divinedoll.launcher.fragments.SearchModFragment;
import com.divinedoll.launcher.fragments.ServersFragment;
import com.divinedoll.launcher.fragments.SkinsFragment;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.prefs.screens.LauncherPreferenceFragment;

/**
 * Navigation shared by every launcher screen.
 * Portrait: a bottom bar with five destinations. Landscape: the same destinations as an icon rail on the left.
 * "More" opens a side drawer with every destination.
 */
public final class NavBar {
    private static final int HOME = 0, INSTALLS = 1, MODS = 2, SKINS = 4, SERVERS = 5, LAN = 6,
            CONTROLS = 7, SETTINGS = 8, PATCH = 9, DOWNLOADS = 10, DISCORD = 11, REPORT = 12, FRIENDS = 13;
    private static final int IDLE = 0xFFB8B8B8;

    private static final int[] DRAWER = {HOME, INSTALLS, MODS, SKINS, SERVERS, DOWNLOADS, SETTINGS, LAN, CONTROLS, PATCH, FRIENDS, DISCORD, REPORT};
    private static final int[] BAR = {HOME, INSTALLS, MODS, SKINS};

    private final LauncherActivity act;
    private final boolean landscape;
    private final View[] barItems = new View[BAR.length];
    private final View[] drawerItems = new View[DRAWER.length];
    private View moreItem;
    private FrameLayout overlay;
    private View scrim;
    private LinearLayout panel;
    private TextView panelSub, panelTitle;
    private int current = HOME;
    private boolean open;

    private NavBar(LauncherActivity act) {
        this.act = act;
        this.landscape = act.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /** Adds the bar and the drawer to the launcher activity. Safe to call once per activity. */
    public static NavBar install(LauncherActivity act) {
        NavBar nav = new NavBar(act);
        try {
            nav.build();
        } catch (Exception e) {
            android.util.Log.w("NavBar", "Could not build navigation", e);
        }
        return nav;
    }

    private static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    private static int icon(int dest) {
        switch (dest) {
            case HOME: return R.drawable.ic_dl_home;
            case INSTALLS: return R.drawable.ic_dl_grid;
            case MODS: return R.drawable.ic_dl_puzzle;
            case SKINS: return R.drawable.ic_dl_shirt;
            case SERVERS: return R.drawable.ic_dl_globe;
            case DOWNLOADS: return R.drawable.ic_dl_grid;
            case LAN: return R.drawable.ic_dl_user;
            case CONTROLS: return R.drawable.ic_dl_cube;
            case PATCH: return R.drawable.ic_dl_img;
            case DISCORD: return R.drawable.ic_dl_discord;
            case REPORT: return R.drawable.ic_dl_more;
            case FRIENDS: return R.drawable.ic_dl_user;
            default: return R.drawable.ic_dl_gear;
        }
    }

    private static int label(int dest) {
        switch (dest) {
            case HOME: return R.string.fork_nav_home;
            case INSTALLS: return R.string.fork_installs_title;
            case MODS: return R.string.fork_mods_button;
            case SKINS: return R.string.fork_skin_title;
            case SERVERS: return R.string.fork_nav_servers;
            case DOWNLOADS: return R.string.fork_dl_title;
            case LAN: return R.string.fork_lan_button;
            case CONTROLS: return R.string.mcl_option_customcontrol;
            case PATCH: return R.string.fork_patch_title;
            case DISCORD: return R.string.fork_discord_title;
            case REPORT: return R.string.fork_report_title;
            case FRIENDS: return R.string.fork_friends_title;
            default: return R.string.fork_settings_title;
        }
    }

    private void build() {
        ConstraintLayout root = (ConstraintLayout) ((ViewGroup) act.findViewById(android.R.id.content)).getChildAt(0);
        int navId = View.generateViewId();

        LinearLayout bar = new LinearLayout(act);
        bar.setId(navId);
        bar.setOrientation(landscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        android.graphics.drawable.LayerDrawable bg = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                new android.graphics.drawable.ColorDrawable(ThemeColors.sidebar()),
                new android.graphics.drawable.ColorDrawable(0x1FFFFFFF)});
        bg.setLayerGravity(1, landscape ? Gravity.END : Gravity.TOP);
        if (landscape) bg.setLayerWidth(1, dp(act, 1)); else bg.setLayerHeight(1, dp(act, 1));
        bar.setBackground(bg);
        bar.setElevation(dp(act, 6));
        bar.setGravity(landscape ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
        int pad = dp(act, landscape ? 8 : 4);
        bar.setPadding(pad, pad, pad, pad);

        for (int i = 0; i < BAR.length; i++) {
            int dest = BAR[i];
            View item = barItem(dest);
            barItems[i] = item;
            bar.addView(item, itemParams());
        }
        moreItem = barItemRaw(R.drawable.ic_dl_more, act.getString(R.string.fork_nav_more));
        moreItem.setOnClickListener(v -> {
            tap(v);
            toggleDrawer();
        });
        bar.addView(moreItem, itemParams());

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                landscape ? dp(act, 64) : 0, landscape ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(bar, lp);

        ConstraintSet cs = new ConstraintSet();
        cs.clone(root);
        int spinner = R.id.account_spinner, container = R.id.container_fragment, progress = R.id.progress_layout;
        if (landscape) {
            cs.connect(navId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            cs.connect(navId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(navId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            for (int id : new int[]{spinner, container, progress}) {
                cs.constrainWidth(id, 0);
                cs.connect(id, ConstraintSet.START, navId, ConstraintSet.END);
                cs.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            }
        } else {
            cs.connect(navId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            cs.connect(navId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            cs.connect(navId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.connect(progress, ConstraintSet.BOTTOM, navId, ConstraintSet.TOP);
        }
        cs.applyTo(root);

        buildDrawer(root);
        refresh();
    }

    private LinearLayout.LayoutParams itemParams() {
        return landscape
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View barItem(int dest) {
        View v = barItemRaw(icon(dest), act.getString(dest == INSTALLS ? R.string.fork_nav_installs_short : label(dest)));
        v.setOnClickListener(x -> {
            tap(x);
            go(dest);
        });
        return v;
    }

    private View barItemRaw(int iconRes, String text) {
        LinearLayout item = new LinearLayout(act);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(act, 2), dp(act, 6), dp(act, 2), dp(act, 6));
        item.setClickable(true);
        ImageView iv = new ImageView(act);
        iv.setImageResource(iconRes);
        iv.setAdjustViewBounds(true);
        item.addView(iv, new LinearLayout.LayoutParams(dp(act, 24), dp(act, 24)));
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(act, 2), 0, 0);
        item.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return item;
    }

    private void paint(View item, boolean selected) {
        int color = selected ? AccentColor.get() : IDLE;
        LinearLayout l = (LinearLayout) item;
        ((ImageView) l.getChildAt(0)).setColorFilter(color);
        ((TextView) l.getChildAt(1)).setTextColor(color);
        if (selected) {
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(dp(act, 14));
            pill.setColor(Color.argb(46, Color.red(color), Color.green(color), Color.blue(color)));
            l.setBackground(pill);
        } else {
            l.setBackground(null);
        }
    }

    private void refresh() {
        for (int i = 0; i < BAR.length; i++) paint(barItems[i], BAR[i] == current);
        paint(moreItem, false);
        for (int i = 0; i < DRAWER.length; i++) {
            if (drawerItems[i] != null) paintRow(drawerItems[i], DRAWER[i] == current);
        }
    }

    private static void tap(View v) {
        try {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        } catch (Exception ignored) {
        }
    }

    // ---- drawer ----

    private void buildDrawer(ConstraintLayout root) {
        overlay = new FrameLayout(act);
        overlay.setVisibility(View.GONE);
        overlay.setElevation(dp(act, 24));
        ConstraintLayout.LayoutParams olp = new ConstraintLayout.LayoutParams(0, 0);
        olp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        olp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        olp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        olp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        root.addView(overlay, olp);

        scrim = new View(act);
        scrim.setBackgroundColor(0xB3000000);
        scrim.setOnClickListener(v -> closeDrawer());
        overlay.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(ThemeColors.panel());
        panel.setClickable(true);
        int w = Math.min(dp(act, 290), Math.round(act.getResources().getDisplayMetrics().widthPixels * 0.82f));
        overlay.addView(panel, new FrameLayout.LayoutParams(w, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(act, 18), dp(act, 22), dp(act, 18), dp(act, 14));
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View avatar = new View(act);
        GradientDrawable av = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{AccentColor.get(), 0xFF7A5A00});
        av.setShape(GradientDrawable.OVAL);
        avatar.setBackground(av);
        head.addView(avatar, new LinearLayout.LayoutParams(dp(act, 34), dp(act, 34)));
        LinearLayout names = new LinearLayout(act);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(act, 12), 0, 0, 0);
        panelTitle = new TextView(act);
        panelTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        panelTitle.setTextColor(Color.WHITE);
        panelTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        panelTitle.setSingleLine(true);
        names.addView(panelTitle);
        head.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelSub = new TextView(act);
        panelSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        panelSub.setTextColor(IDLE);
        panelSub.setSingleLine(true);
        panelSub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        names.addView(panelSub);
        panel.addView(head);

        View line = new View(act);
        line.setBackgroundColor(0x22FFFFFF);
        panel.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 1)));

        ScrollView scroll = new ScrollView(act);
        LinearLayout list = new LinearLayout(act);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(act, 8), dp(act, 8), dp(act, 8), dp(act, 8));
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        for (int i = 0; i < DRAWER.length; i++) {
            int dest = DRAWER[i];
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(act, 12), 0, dp(act, 12), 0);
            row.setClickable(true);
            ImageView iv = new ImageView(act);
            iv.setImageResource(icon(dest));
            row.addView(iv, new LinearLayout.LayoutParams(dp(act, 22), dp(act, 22)));
            TextView t = new TextView(act);
            t.setText(label(dest));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            t.setPadding(dp(act, 14), 0, 0, 0);
            row.addView(t);
            row.setOnClickListener(v -> {
                tap(v);
                closeDrawer();
                go(dest);
            });
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 48));
            rp.bottomMargin = dp(act, 2);
            list.addView(row, rp);
            drawerItems[i] = row;
        }
    }

    private void paintRow(View row, boolean selected) {
        int color = selected ? AccentColor.get() : Color.WHITE;
        LinearLayout l = (LinearLayout) row;
        ((ImageView) l.getChildAt(0)).setColorFilter(selected ? color : IDLE);
        ((TextView) l.getChildAt(1)).setTextColor(color);
        if (selected) {
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(dp(act, 12));
            pill.setColor(Color.argb(46, Color.red(color), Color.green(color), Color.blue(color)));
            l.setBackground(pill);
        } else {
            l.setBackground(null);
        }
    }

    public void toggleDrawer() {
        if (open) closeDrawer(); else openDrawer();
    }

    private void openDrawer() {
        if (overlay == null) return;
        try {
            Instance selected = Instances.loadSelectedInstance();
            panelSub.setText(selected != null ? String.valueOf(selected.name) : "");
        } catch (Exception e) {
            panelSub.setText("");
        }
        String who = act.getString(R.string.fork_nav_title);
        try {
            com.divinedoll.launcher.authenticator.accounts.Account account = com.divinedoll.launcher.authenticator.accounts.Accounts.getCurrent();
            if (account != null) who = account.username;
        } catch (Exception ignored) {
        }
        panelTitle.setText(who);
        refresh();
        open = true;
        overlay.setVisibility(View.VISIBLE);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(160).start();
        panel.setTranslationX(-panel.getLayoutParams().width);
        panel.animate().translationX(0).setDuration(180).start();
    }

    private void closeDrawer() {
        if (overlay == null || !open) return;
        open = false;
        scrim.animate().alpha(0f).setDuration(140).start();
        panel.animate().translationX(-panel.getLayoutParams().width).setDuration(160)
                .withEndAction(() -> overlay.setVisibility(View.GONE)).start();
    }

    /** Returns true when the back button only closed the drawer. */
    public boolean onBack() {
        if (!open) return false;
        closeDrawer();
        return true;
    }

    // ---- going places ----

    public static void openDiscord(android.content.Context c) {
        String url = c.getString(R.string.fork_discord_url);
        if (url.startsWith("https://")) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
            } catch (Exception e) {
                android.widget.Toast.makeText(c, url, android.widget.Toast.LENGTH_LONG).show();
            }
        } else {
            android.widget.Toast.makeText(c, R.string.fork_discord_soon, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void go(int dest) {
        FragmentManager fm = act.getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        if (dest == current && dest != CONTROLS) return;
        switch (dest) {
            case HOME: Tools.backToMainMenu(act); break;
            case INSTALLS: ForkUi.openTab(act, ForkUi.TAB_INSTALLS); break;
            case SKINS: ForkUi.openTab(act, ForkUi.TAB_SKINS); break;
            case PATCH: ForkUi.openTab(act, ForkUi.TAB_PATCH); break;
            case MODS: {
                Bundle args = new Bundle();
                args.putBoolean(SearchModFragment.ARG_MODPACK, false);
                swap(SearchModFragment.class, SearchModFragment.TAG, args);
                break;
            }
            case SERVERS: swap(ServersFragment.class, ServersFragment.TAG, null); break;
            case DOWNLOADS: swap(com.divinedoll.launcher.fragments.DownloadsFragment.class, com.divinedoll.launcher.fragments.DownloadsFragment.TAG, null); break;
            case LAN: swap(LanFragment.class, LanFragment.TAG, null); break;
            case FRIENDS: swap(com.divinedoll.launcher.fragments.FriendsFragment.class, com.divinedoll.launcher.fragments.FriendsFragment.TAG, null); break;
            case SETTINGS: swap(LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null); break;
            case CONTROLS: act.startActivity(new Intent(act, CustomControlsActivity.class)); break;
            case DISCORD: openDiscord(act); break;
            case REPORT:
                try { act.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/DivineDoll/DivineLauncher/issues/new"))); }
                catch (Exception ignored) {}
                break;
            default: break;
        }
    }

    private void swap(Class<? extends Fragment> cls, String tag, Bundle args) {
        Tools.backToMainMenu(act);
        Tools.swapFragment(act, cls, tag, args);
    }

    /** Called when a screen becomes visible so the right item lights up. */
    public void onScreen(Fragment f) {
        int dest = HOME;
        if (f instanceof MainMenuFragment) dest = HOME;
        else if (f instanceof InstallationsFragment) dest = INSTALLS;
        else if (f instanceof SearchModFragment) dest = MODS;
        else if (f instanceof SkinsFragment) dest = SKINS;
        else if (f instanceof ServersFragment) dest = SERVERS;
        else if (f instanceof LanFragment) dest = LAN;
        else if (f instanceof com.divinedoll.launcher.fragments.DownloadsFragment) dest = DOWNLOADS;
        else if (f instanceof com.divinedoll.launcher.fragments.FriendsFragment || f instanceof com.divinedoll.launcher.fragments.ChatFragment) dest = FRIENDS;
        else if (f instanceof LauncherPreferenceFragment) dest = SETTINGS;
        else if (f instanceof com.divinedoll.launcher.fragments.PatchNotesFragment) dest = PATCH;
        else dest = -1; // sub screens keep no selection
        current = dest;
        refresh();
    }
}
