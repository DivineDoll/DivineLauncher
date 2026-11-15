package com.divinedoll.launcher.fragments;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.progresskeeper.ProgressKeeper;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.EmptyState;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.ThemeColors;
import com.kdt.mcgui.ProgressLayout;

import java.util.List;

/**
 * Downloads screen: what the launcher is installing right now (game files, Forge, modpacks...).
 * Installers send you here and keep running in the background.
 */
public class DownloadsFragment extends Fragment {
    public static final String TAG = "DownloadsFragment";

    private LinearLayout mList;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mSawTasks;
    private final Runnable mTick = new Runnable() {
        @Override public void run() {
            refresh();
            mHandler.postDelayed(this, 400);
        }
    };

    public static void open(FragmentActivity act) {
        Tools.swapFragment(act, DownloadsFragment.class, TAG, null);
    }

    private static int titleOf(String key) {
        switch (key) {
            case ProgressLayout.DOWNLOAD_GAME: return R.string.fork_dl_game;
            case ProgressLayout.INSTANCE_INSTALL: return R.string.fork_dl_install;
            case ProgressLayout.INSTALL_MODPACK: return R.string.fork_dl_modpack;
            case ProgressLayout.UNPACK_RUNTIME:
            case ProgressLayout.EXTRACT_COMPONENTS:
            case ProgressLayout.EXTRACT_SINGLE_FILES: return R.string.fork_dl_unpack;
            case ProgressLayout.DOWNLOAD_VERSION_LIST: return R.string.fork_dl_versions;
            case ProgressLayout.AUTHENTICATE: return R.string.fork_dl_login;
            default: return R.string.fork_dl_other;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());
        root.addView(ForkUi.screenHeader(c, requireActivity(), getString(R.string.fork_dl_title), 0, null));
        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        mList.setPadding(ForkUi.dp(c, 16), 0, ForkUi.dp(c, 16), ForkUi.dp(c, 16));
        scroll.addView(mList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    @Override public void onResume() { super.onResume(); mHandler.post(mTick); }
    @Override public void onPause() { mHandler.removeCallbacks(mTick); super.onPause(); }

    private void refresh() {
        Context c = getContext();
        if (c == null || mList == null) return;
        List<Object[]> rows = ProgressKeeper.snapshot(c);
        mList.removeAllViews();
        if (rows.isEmpty()) {
            mList.addView(EmptyState.view(c, R.drawable.ic_dl_globe,
                    getString(mSawTasks ? R.string.fork_dl_done : R.string.fork_dl_none),
                    getString(R.string.fork_dl_none_hint), getString(R.string.fork_nav_home),
                    v -> Tools.backToMainMenu(requireActivity())));
            return;
        }
        mSawTasks = true;
        boolean first = true;
        for (Object[] row : rows) {
            String key = (String) row[0];
            int progress = (Integer) row[1];
            String text = (String) row[2];
            mList.addView(card(c, getString(titleOf(key)), text, progress, first));
            first = false;
        }
    }

    private View card(Context c, String title, String sub, int progress, boolean big) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = ForkUi.dp(c, big ? 16 : 14);
        card.setPadding(pad, pad, pad, pad);
        int accent = AccentColor.get();
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                big ? new int[]{(accent & 0x00FFFFFF) | 0x55000000, ThemeColors.sidebar()} : new int[]{ThemeColors.sidebar(), ThemeColors.sidebar()});
        bg.setCornerRadius(ForkUi.dp(c, 18));
        bg.setStroke(ForkUi.dp(c, 1), big ? ((accent & 0x00FFFFFF) | 0x66000000) : 0x14FFFFFF);
        card.setBackground(bg);

        TextView t = ForkUi.text(c, title, big ? 18 : 15, true);
        card.addView(t);
        TextView s = ForkUi.text(c, progress >= 0 && progress <= 100 ? sub + (sub.isEmpty() ? "" : " \u00B7 ") + progress + " %" : sub, 12, false);
        s.setAlpha(0.7f);
        s.setPadding(0, ForkUi.dp(c, 4), 0, ForkUi.dp(c, 8));
        card.addView(s);

        ProgressBar bar = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        boolean known = progress >= 0 && progress <= 100;
        bar.setIndeterminate(!known);
        bar.setMax(100);
        bar.setProgress(known ? progress : 0);
        GradientDrawable track = new GradientDrawable();
        track.setColor(0x33FFFFFF);
        track.setCornerRadius(ForkUi.dp(c, 4));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(accent);
        fill.setCornerRadius(ForkUi.dp(c, 4));
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                track, new android.graphics.drawable.ClipDrawable(fill, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL)});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        bar.setProgressDrawable(layers);
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(accent));
        card.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 8)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = ForkUi.dp(c, 10);
        card.setLayoutParams(lp);
        return card;
    }
}
