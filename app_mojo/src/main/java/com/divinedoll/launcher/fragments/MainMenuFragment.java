package com.divinedoll.launcher.fragments;

import static com.divinedoll.launcher.Tools.openPath;
import static com.divinedoll.launcher.Tools.shareLog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import com.divinedoll.launcher.CustomControlsActivity;
import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.contracts.OpenDocumentWithExtension;
import com.divinedoll.launcher.extra.ExtraConstants;
import com.divinedoll.launcher.extra.ExtraCore;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.progresskeeper.ProgressKeeper;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.AppearancePanel;
import com.divinedoll.launcher.utils.BannerManager;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.FileUtils;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.InstanceLook;
import com.divinedoll.launcher.utils.McStyle;
import com.divinedoll.launcher.utils.PlayColor;
import com.divinedoll.launcher.utils.ThemeColors;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Home screen. One big card for the installation that will be played, quick tools, the last world,
 * counters and news. Moving to other screens is done with the navigation bar and drawer (NavBar).
 */
public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private View mPlay;
    private ImageView mHeroImage, mHeroIcon;
    private TextView mKicker, mHeroName, mHeroInfo;
    private LinearLayout mContinueBox, mNewsBox;
    private TextView mChipMods, mChipWorlds, mChipPacks;
    private List<String[]> mNews = new ArrayList<>();
    private boolean mNewsLoading, mNewsTried;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data) -> {
                if (data != null) Tools.launchModInstaller(requireContext(), data);
            });

    private final ActivityResultLauncher<String> mBannerPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
                if (uri == null) return;
                try {
                    BannerManager.saveCustom(requireContext(), uri);
                    refreshHero();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.fork_banner_error, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> mThemePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                boolean ok = com.divinedoll.launcher.utils.ThemeManager.importFrom(requireContext(), uri);
                Toast.makeText(requireContext(), ok ? R.string.fork_theme_imported : R.string.fork_theme_error, Toast.LENGTH_LONG).show();
                if (ok) requireActivity().recreate();
            });

    // ------------------------------------------------------------------ small builders

    private static int dp(Context c, int v) {
        return ForkUi.dp(c, v);
    }

    private static GradientDrawable shape(Context c, int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(dp(c, 1), stroke);
        return g;
    }

    private static LinearLayout.LayoutParams lpMatch(Context c, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        return lp;
    }

    private TextView sectionTitle(Context c, int label) {
        TextView t = ForkUi.text(c, getString(label), 12, true);
        t.setAllCaps(true);
        t.setLetterSpacing(0.08f);
        t.setTextColor(AccentColor.get());
        t.setPadding(dp(c, 4), 0, 0, dp(c, 6));
        return t;
    }

    private LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        l.setBackground(shape(c, ThemeColors.panel(), 0x1FFFFFFF, 16));
        return l;
    }

    private View toolPill(Context c, int iconRes, int label, Runnable action) {
        LinearLayout pill = new LinearLayout(c);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(c, 12), dp(c, 8), dp(c, 14), dp(c, 8));
        pill.setBackground(shape(c, ThemeColors.panel(), 0x1FFFFFFF, 20));
        pill.setClickable(true);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setColorFilter(AccentColor.get());
        pill.addView(icon, new LinearLayout.LayoutParams(dp(c, 18), dp(c, 18)));
        TextView t = ForkUi.text(c, label == 0 ? "" : getString(label), 13, false);
        t.setPadding(dp(c, 8), 0, 0, 0);
        pill.addView(t);
        pill.setOnClickListener(v -> action.run());
        return pill;
    }

    private TextView chip(Context c, LinearLayout parent, int label) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(c, 8), dp(c, 10), dp(c, 8), dp(c, 10));
        box.setBackground(shape(c, ThemeColors.panel(), 0x1FFFFFFF, 16));
        TextView value = ForkUi.text(c, "0", 22, true);
        value.setTextColor(AccentColor.get());
        value.setGravity(Gravity.CENTER);
        box.addView(value);
        TextView name = ForkUi.text(c, getString(label), 11, false);
        name.setTextColor(0xFFB8B8B8);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(c, 8);
        parent.addView(box, lp);
        return value;
    }

    // ------------------------------------------------------------------ screen

    /** Image that never asks for height, so the card keeps the size of its content. */
    private static class CoverImage extends ImageView {
        CoverImage(Context c) {
            super(c);
            setScaleType(ScaleType.CENTER_CROP);
        }

        @Override
        protected void onMeasure(int w, int h) {
            int height = View.MeasureSpec.getMode(h) == View.MeasureSpec.EXACTLY ? View.MeasureSpec.getSize(h) : 0;
            setMeasuredDimension(View.MeasureSpec.getSize(w), height);
        }
    }

    private View mPlayFab;
    private LinearLayout mServersRow;
    private TextView mServersTitle;
    private View mServersScroll;

    private static int mix(int a, int b, float amountOfA) {
        return android.graphics.Color.argb(255,
                Math.round(android.graphics.Color.red(a) * amountOfA + android.graphics.Color.red(b) * (1 - amountOfA)),
                Math.round(android.graphics.Color.green(a) * amountOfA + android.graphics.Color.green(b) * (1 - amountOfA)),
                Math.round(android.graphics.Color.blue(a) * amountOfA + android.graphics.Color.blue(b) * (1 - amountOfA)));
    }

    /** Pill with the play icon, used in the card and as the floating button. */
    private LinearLayout playPill(Context c) {
        LinearLayout pill = new LinearLayout(c);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(c, 18), dp(c, 10), dp(c, 20), dp(c, 10));
        pill.setClickable(true);
        ImageView icon = new ImageView(c);
        icon.setImageResource(R.drawable.ic_dl_play);
        pill.addView(icon, new LinearLayout.LayoutParams(dp(c, 14), dp(c, 14)));
        TextView t = ForkUi.text(c, getString(R.string.fork_nav_play), 14, true);
        t.setPadding(dp(c, 8), 0, 0, 0);
        pill.addView(t);
        stylePlay(c, pill);
        pill.setOnClickListener(v -> {
            try {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            } catch (Exception ignored) {
            }
            com.divinedoll.launcher.utils.QuickJoin.clear();
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        });
        pill.setOnLongClickListener(v -> {
            showPlayMenu(c);
            return true;
        });
        return pill;
    }

    /** Long press on Play: pick a server to join, the main menu, or the button color. */
    private void showPlayMenu(Context c) {
        List<ServersFragment.Entry> servers = ServersFragment.all();
        int n = servers.size();
        CharSequence[] labels = new CharSequence[n + 2];
        int[] icons = new int[n + 2];
        for (int i = 0; i < n; i++) {
            ServersFragment.Entry e = servers.get(i);
            labels[i] = e.name + (e.instance.isEmpty() ? "" : "  ·  " + e.instance);
            icons[i] = R.drawable.ic_dl_globe;
        }
        labels[n] = getString(R.string.fork_play_menu);
        icons[n] = R.drawable.ic_dl_play;
        labels[n + 1] = getString(R.string.fork_play_color_title);
        icons[n + 1] = R.drawable.ic_dl_shirt;
        com.divinedoll.launcher.utils.BottomSheet.menu(c, getString(R.string.fork_play_on), icons, labels, null, pick -> {
            if (pick < n) {
                ServersFragment.join(requireActivity(), servers.get(pick));
            } else if (pick == n) {
                com.divinedoll.launcher.utils.QuickJoin.clear();
                ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            } else {
                PlayColor.choose(c, () -> {
                    stylePlay(c, mPlay);
                    stylePlay(c, mPlayFab);
                });
            }
        });
    }

    private static void stylePlay(Context c, View pill) {
        if (pill == null) return;
        pill.setBackground(shape(c, PlayColor.get(), 0, 30));
        int on = DivineTheme.onColor(PlayColor.get());
        LinearLayout l = (LinearLayout) pill;
        ((ImageView) l.getChildAt(0)).setColorFilter(on);
        ((TextView) l.getChildAt(1)).setTextColor(on);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DivineTheme.applyOnce();
        Context c = requireContext();
        boolean land = !DivineTheme.isPortrait(c);

        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(ThemeColors.background());

        LinearLayout page = new LinearLayout(c);
        page.setOrientation(LinearLayout.VERTICAL);

        // Top bar: title and settings chip
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(c, 18), dp(c, 12), dp(c, 14), dp(c, 8));
        TextView title = ForkUi.text(c, getString(R.string.fork_nav_home), 17, true);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(ForkUi.iconChip(c, R.drawable.ic_dl_gear, false, v -> Tools.swapFragment(requireActivity(),
                com.divinedoll.launcher.prefs.screens.LauncherPreferenceFragment.class,
                com.divinedoll.launcher.LauncherActivity.SETTING_FRAGMENT_TAG, null)), new LinearLayout.LayoutParams(dp(c, 30), dp(c, 30)));
        page.addView(bar);

        View hero = buildHero(c, inflater);
        View tools = buildTools(c);
        LinearLayout sections = buildSections(c);

        if (land) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(c, 14), 0, dp(c, 14), dp(c, 12));
            LinearLayout left = new LinearLayout(c);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(hero, lpMatch(c, 0));
            left.addView(tools, lpMatch(c, 12));
            ScrollView leftScroll = new ScrollView(c);
            leftScroll.addView(left);
            LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            leftLp.rightMargin = dp(c, 14);
            row.addView(leftScroll, leftLp);
            ScrollView scroll = new ScrollView(c);
            scroll.addView(sections);
            row.addView(scroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            page.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            LinearLayout column = new LinearLayout(c);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setPadding(dp(c, 14), 0, dp(c, 14), dp(c, 84));
            column.addView(hero, lpMatch(c, 0));
            column.addView(tools, lpMatch(c, 10));
            column.addView(sections, lpMatch(c, 10));
            ScrollView scroll = new ScrollView(c);
            scroll.addView(column);
            page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }
        root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (!land) {
            mPlayFab = playPill(c);
            mPlayFab.setElevation(dp(c, 8));
            FrameLayout.LayoutParams fab = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
            fab.setMargins(0, 0, dp(c, 16), dp(c, 16));
            root.addView(mPlayFab, fab);
        }
        return root;
    }

    private View buildHero(Context c, LayoutInflater inflater) {
        int accent = AccentColor.get();
        int card = ThemeColors.panel();
        FrameLayout hero = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{mix(accent, card, 0.34f), card, card});
        bg.setCornerRadius(dp(c, 20));
        bg.setStroke(dp(c, 1), 0x1FFFFFFF);
        hero.setBackground(bg);
        DivineTheme.roundCorners(hero, 20);

        mHeroImage = new CoverImage(c);
        mHeroImage.setAlpha(0.35f);
        mHeroImage.setOnClickListener(v -> BannerManager.showChooser(c, () -> mBannerPicker.launch("image/*"), this::refreshHero));
        hero.addView(mHeroImage, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14));
        hero.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mKicker = ForkUi.text(c, "", 11, true);
        mKicker.setTextColor(accent);
        mKicker.setAllCaps(true);
        mKicker.setLetterSpacing(0.08f);
        mKicker.setSingleLine(true);
        mKicker.setEllipsize(TextUtils.TruncateAt.END);
        mKicker.setPadding(0, 0, 0, dp(c, 8));
        body.addView(mKicker);

        LinearLayout titleRow = new LinearLayout(c);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        mHeroIcon = new ImageView(c);
        mHeroIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mHeroIcon.setBackground(shape(c, accent, 0, 14));
        mHeroIcon.setPadding(dp(c, 7), dp(c, 7), dp(c, 7), dp(c, 7));
        titleRow.addView(mHeroIcon, new LinearLayout.LayoutParams(dp(c, 46), dp(c, 46)));
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(c, 12), 0, 0, 0);
        mHeroName = ForkUi.text(c, "", 17, true);
        mHeroName.setSingleLine(true);
        mHeroName.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(mHeroName);
        mHeroInfo = ForkUi.text(c, "", 12, false);
        mHeroInfo.setTextColor(0xFF9AA1AD);
        mHeroInfo.setSingleLine(true);
        mHeroInfo.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(mHeroInfo);
        titleRow.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(titleRow);

        // Installation selector: the same behavior as before, inside a pill
        LinearLayout selector = new LinearLayout(c);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setGravity(Gravity.CENTER_VERTICAL);
        selector.setBackground(shape(c, 0x66000000, 0x1FFFFFFF, 24));
        View spinnerView = inflater.inflate(R.layout.view_instance_spinner, selector, false);
        mVersionSpinner = spinnerView instanceof mcVersionSpinner ? (mcVersionSpinner) spinnerView : null;
        selector.addView(spinnerView, new LinearLayout.LayoutParams(0, dp(c, 44), 1f));
        View edit = ForkUi.iconChip(c, R.drawable.ic_dl_gear, false, v -> {
            if (mVersionSpinner != null) mVersionSpinner.openProfileEditor(requireActivity());
        });
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(dp(c, 30), dp(c, 30));
        editLp.rightMargin = dp(c, 8);
        selector.addView(edit, editLp);
        body.addView(selector, lpMatch(c, 12));
        if (mVersionSpinner != null) {
            mVersionSpinner.setTextColor(0xFFFFFFFF);
            mVersionSpinner.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void afterTextChanged(Editable s) {
                    mVersionSpinner.post(() -> refreshHome());
                }
            });
        }

        mPlay = playPill(c);
        body.addView(mPlay, lpWrap(c, 12));
        return hero;
    }

    private static LinearLayout.LayoutParams lpWrap(Context c, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        return lp;
    }

    private View buildTools(Context c) {
        HorizontalScrollView scroll = new HorizontalScrollView(c);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(row);
        Object[][] tools = {
                {R.drawable.ic_dl_puzzle, R.string.fork_chip_mods, (Runnable) () -> openHub(InstanceHubFragment.TAB_MODS)},
                {R.drawable.ic_dl_globe, R.string.fork_chip_worlds, (Runnable) () -> openHub(InstanceHubFragment.TAB_WORLDS)},
                {R.drawable.ic_dl_img, R.string.fork_hub_shots, (Runnable) () -> openHub(InstanceHubFragment.TAB_SHOTS)},
                {R.drawable.ic_dl_cube, R.string.fork_home_edit, (Runnable) this::editInstance},
                {R.drawable.ic_dl_user, R.string.fork_home_servers, (Runnable) () ->
                        Tools.swapFragment(requireActivity(), ServersFragment.class, ServersFragment.TAG, null)},
                {R.drawable.ic_dl_more, R.string.fork_home_logs, (Runnable) () -> shareLog(requireContext())},
                {R.drawable.ic_dl_plus, R.string.main_install_jar_file, (Runnable) this::installJar},
                {R.drawable.ic_dl_gear, R.string.mcl_option_customcontrol, (Runnable) () ->
                        startActivity(new android.content.Intent(requireContext(), CustomControlsActivity.class))},
                {R.drawable.ic_dl_shirt, R.string.fork_appearance_title, (Runnable) this::showAppearanceDialog},
        };
        for (Object[] t : tools) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(c, 8);
            row.addView(toolPill(c, (Integer) t[0], (Integer) t[1], (Runnable) t[2]), lp);
        }
        return scroll;
    }

    private LinearLayout buildSections(Context c) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);

        mServersTitle = sectionTitle(c, R.string.fork_servers_quick);
        mServersTitle.setVisibility(View.GONE);
        box.addView(mServersTitle);
        HorizontalScrollView serversScroll = new HorizontalScrollView(c);
        serversScroll.setHorizontalScrollBarEnabled(false);
        mServersRow = new LinearLayout(c);
        mServersRow.setOrientation(LinearLayout.HORIZONTAL);
        serversScroll.addView(mServersRow);
        mServersScroll = serversScroll;
        serversScroll.setVisibility(View.GONE);
        box.addView(serversScroll, lpMatch(c, 0));

        mContinueBox = card(c);
        box.addView(mContinueBox, lpMatch(c, 0));

        LinearLayout chips = new LinearLayout(c);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        mChipMods = chip(c, chips, R.string.fork_chip_mods);
        mChipWorlds = chip(c, chips, R.string.fork_chip_worlds);
        mChipPacks = chip(c, chips, R.string.fork_chip_packs);
        box.addView(chips, lpMatch(c, 12));

        TextView news = sectionTitle(c, R.string.fork_news);
        news.setPadding(dp(c, 4), dp(c, 14), 0, dp(c, 6));
        box.addView(news);
        mNewsBox = card(c);
        box.addView(mNewsBox, lpMatch(c, 0));

        String[] tips = getResources().getStringArray(R.array.fork_tips);
        if (tips.length > 0) {
            TextView tip = ForkUi.text(c, tips[new java.util.Random().nextInt(tips.length)], 12, false);
            tip.setTextColor(0xFFB8B8B8);
            tip.setPadding(dp(c, 6), dp(c, 14), dp(c, 6), 0);
            int[] index = {Arrays.asList(tips).indexOf(tip.getText().toString())};
            tip.setOnClickListener(v -> {
                index[0] = (index[0] + 1) % tips.length;
                tip.setText(tips[index[0]]);
            });
            box.addView(tip);
        }
        return box;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        com.divinedoll.launcher.utils.UpdateCheck.run(requireActivity());
        view.post(this::runSetupWizard);
        refreshHome();
    }

    // ------------------------------------------------------------------ data

    private static int countEntries(File dir, boolean onlyDirs, String suffix) {
        File[] list = dir.listFiles();
        if (list == null) return 0;
        int n = 0;
        for (File f : list) {
            if (onlyDirs ? f.isDirectory() : (suffix == null || f.getName().endsWith(suffix))) n++;
        }
        return n;
    }

    private void refreshHero() {
        if (mHeroImage == null || getContext() == null) return;
        boolean custom = BannerManager.apply(requireContext(), mHeroImage);
        try {
            Instance selected = Instances.loadSelectedInstance();
            Bitmap own = selected == null ? null : InstanceLook.image(requireContext(), String.valueOf(selected.name));
            if (own != null) {
                mHeroImage.setImageBitmap(own);
                custom = true;
            }
        } catch (Exception ignored) {
        }
        mHeroImage.setVisibility(custom ? View.VISIBLE : View.GONE);
    }

    private void refreshHome() {
        if (getContext() == null || mHeroName == null) return;
        Context c = requireContext();
        refreshHero();

        String kicker = AppearancePanel.launcherName(c);
        try {
            com.divinedoll.launcher.authenticator.accounts.Account account = com.divinedoll.launcher.authenticator.accounts.Accounts.getCurrent();
            if (account != null) kicker = kicker + "  ·  " + getString(R.string.fork_playing_as, account.username);
        } catch (Exception ignored) {
        }
        mKicker.setText(kicker);

        fillServers(c);
        Instance instance = Instances.loadSelectedInstance();
        mContinueBox.removeAllViews();
        if (instance == null) {
            mHeroName.setText(R.string.fork_home_no_instance);
            mHeroInfo.setText("");
            mHeroIcon.setImageBitmap(null);
            mChipMods.setText("0");
            mChipWorlds.setText("0");
            mChipPacks.setText("0");
            addEmpty(c, getString(R.string.fork_home_no_instance));
            loadNews();
            renderNews(c);
            return;
        }
        mHeroIcon.setImageBitmap(InstallationsFragment.iconOf(c, instance));
        mHeroName.setText(String.valueOf(instance.name));
        mHeroInfo.setText(instance.versionId + "  ·  " + com.divinedoll.launcher.utils.InstanceTools.stats(c, String.valueOf(instance.name)));

        File dir = instance.getGameDirectory();
        mChipMods.setText(String.valueOf(countEntries(new File(dir, "mods"), false, ".jar")));
        File[] worlds = new File(dir, "saves").listFiles(File::isDirectory);
        mChipWorlds.setText(String.valueOf(worlds == null ? 0 : worlds.length));
        mChipPacks.setText(String.valueOf(countEntries(new File(dir, "resourcepacks"), false, null)));

        if (worlds == null || worlds.length == 0) {
            addEmpty(c, getString(R.string.fork_no_worlds));
        } else {
            Arrays.sort(worlds, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            final String name = String.valueOf(instance.name);
            for (int i = 0; i < Math.min(3, worlds.length); i++) {
                LinearLayout row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(c, 6), 0, dp(c, 6));
                row.setClickable(true);
                row.setOnClickListener(v -> InstanceHubFragment.open(requireActivity(), name));
                ImageView dot = new ImageView(c);
                dot.setImageResource(R.drawable.ic_dl_globe);
                dot.setColorFilter(i == 0 ? AccentColor.get() : 0xFF8A8F98);
                row.addView(dot, new LinearLayout.LayoutParams(dp(c, 18), dp(c, 18)));
                TextView t = ForkUi.text(c, worlds[i].getName(), i == 0 ? 15 : 13, i == 0);
                t.setSingleLine(true);
                t.setEllipsize(TextUtils.TruncateAt.END);
                t.setPadding(dp(c, 10), 0, dp(c, 8), 0);
                row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView when = ForkUi.text(c, String.valueOf(android.text.format.DateUtils.getRelativeTimeSpanString(
                        worlds[i].lastModified(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)), 11, false);
                when.setTextColor(0xFFB8B8B8);
                row.addView(when);
                mContinueBox.addView(row);
            }
        }
        loadNews();
        renderNews(c);
    }

    /** Quick join pills for the saved servers. */
    private void fillServers(Context c) {
        if (mServersRow == null) return;
        mServersRow.removeAllViews();
        List<ServersFragment.Entry> servers = ServersFragment.all();
        int vis = servers.isEmpty() ? View.GONE : View.VISIBLE;
        mServersTitle.setVisibility(vis);
        mServersScroll.setVisibility(vis);
        for (ServersFragment.Entry e : servers) {
            View pill = toolPill(c, R.drawable.ic_dl_globe, 0, () -> ServersFragment.join(requireActivity(), e));
            ((TextView) ((LinearLayout) pill).getChildAt(1)).setText(e.name);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(c, 8);
            mServersRow.addView(pill, lp);
        }
        mServersRow.setPadding(0, 0, 0, servers.isEmpty() ? 0 : dp(c, 12));
    }

    private void addEmpty(Context c, String message) {
        TextView t = ForkUi.text(c, message, 13, false);
        t.setTextColor(0xFFB8B8B8);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 8), 0, dp(c, 8));
        mContinueBox.addView(t);
    }

    private void renderNews(Context c) {
        if (mNewsBox == null) return;
        mNewsBox.removeAllViews();
        if (mNews.isEmpty()) {
            TextView t = ForkUi.text(c, "—", 13, false);
            t.setTextColor(0xFFB8B8B8);
            mNewsBox.addView(t);
            return;
        }
        for (String[] item : mNews) {
            TextView t = ForkUi.text(c, item[0], 13, false);
            t.setPadding(0, dp(c, 6), 0, dp(c, 6));
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            if (item[1] != null && !item[1].isEmpty()) t.setOnClickListener(v -> Tools.openURL(requireActivity(), item[1]));
            mNewsBox.addView(t);
        }
    }

    /** Latest Minecraft news, fetched once per screen. Fails silently when offline. */
    private void loadNews() {
        if (mNewsLoading || mNewsTried || !mNews.isEmpty()) return;
        mNewsLoading = true;
        mNewsTried = true;
        new Thread(() -> {
            List<String[]> found = new ArrayList<>();
            try {
                java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL("https://launchercontent.mojang.com/v2/news.json").openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                con.setRequestProperty("User-Agent", "DivineDoll/DivineLauncher");
                try (java.io.InputStream in = con.getInputStream()) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    com.google.gson.JsonObject root = new com.google.gson.Gson().fromJson(out.toString("UTF-8"), com.google.gson.JsonObject.class);
                    com.google.gson.JsonArray entries = root.getAsJsonArray("entries");
                    if (entries != null) for (com.google.gson.JsonElement e : entries) {
                        com.google.gson.JsonObject o = e.getAsJsonObject();
                        if (!o.has("title") || o.get("title").isJsonNull()) continue;
                        String link = o.has("readMoreLink") && !o.get("readMoreLink").isJsonNull() ? o.get("readMoreLink").getAsString() : "";
                        found.add(new String[]{o.get("title").getAsString(), link});
                        if (found.size() >= 4) break;
                    }
                }
            } catch (Exception ignored) {
            }
            Tools.runOnUiThread(() -> {
                mNewsLoading = false;
                if (!isAdded()) return;
                mNews = found;
                renderNews(requireContext());
            });
        }).start();
    }

    // ------------------------------------------------------------------ actions

    /** First start: welcome, color and button pack; then, after the screen is rebuilt, the language. */
    private void runSetupWizard() {
        if (!isAdded()) return;
        Context c = requireContext();
        android.content.SharedPreferences prefs = com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF;
        int stage = prefs.getInt("setup_stage", 0);
        if (stage >= 2) return;
        if (stage == 1) {
            prefs.edit().putInt("setup_stage", 2).apply();
            com.divinedoll.launcher.utils.LanguagePicker.show(c, () -> requireActivity().recreate());
            return;
        }
        new android.app.AlertDialog.Builder(c).setCancelable(false)
                .setTitle(R.string.fork_setup_title).setMessage(R.string.fork_setup_message)
                .setPositiveButton(R.string.fork_setup_start, (d, w) -> {
                    prefs.edit().putInt("setup_stage", 1).apply();
                    com.divinedoll.launcher.utils.ControlPacks.show(c, () ->
                            AccentColor.choose(c, () -> requireActivity().recreate()));
                })
                .setNegativeButton(R.string.fork_setup_skip, (d, w) -> prefs.edit().putInt("setup_stage", 2).apply())
                .show();
    }

    private void editInstance() {
        if (Instances.loadSelectedInstance() == null) {
            Toast.makeText(requireContext(), R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
    }

    private void openHub(int tab) {
        Context context = requireContext();
        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        InstanceHubFragment.open(requireActivity(), String.valueOf(instance.name), tab);
    }

    private void installJar() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    private void showAppearanceDialog() {
        Context c = requireContext();
        AppearancePanel.show(requireActivity(), new AppearancePanel.Hooks() {
            @Override public void pickBanner() {
                BannerManager.showChooser(c, () -> mBannerPicker.launch("image/*"), MainMenuFragment.this::refreshHero);
            }
            @Override public void refreshBanner() {
                MainMenuFragment.this.refreshHero();
            }
            @Override public void refreshPlay() {
                stylePlay(c, mPlay);
                stylePlay(c, mPlayFab);
            }
            @Override public void pickThemeFile() {
                mThemePicker.launch("*/*");
            }
            @Override public void recreate() {
                requireActivity().recreate();
            }
        });
    }

    /** Opens the game folder in a file manager. Kept for the files tool in the hub. */
    public static void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if (FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        } else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHome();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
    }
}
