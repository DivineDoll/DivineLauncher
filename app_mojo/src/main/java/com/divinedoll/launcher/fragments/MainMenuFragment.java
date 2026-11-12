package com.divinedoll.launcher.fragments;

import static com.divinedoll.launcher.Tools.openPath;
import static com.divinedoll.launcher.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;
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
import com.divinedoll.launcher.utils.BannerManager;
import com.divinedoll.launcher.utils.FileUtils;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    private ImageView mBannerView;
    private View mBannerTitle;

    private final ActivityResultLauncher<String> mBannerPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
                if(uri == null) return;
                try {
                    BannerManager.saveCustom(requireContext(), uri);
                    refreshBanner();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.fork_banner_error, Toast.LENGTH_LONG).show();
                }
            });

    private void refreshBanner() {
        if(mBannerView == null) return;
        boolean custom = BannerManager.apply(requireContext(), mBannerView);
        try {
            Instance selected = Instances.loadSelectedInstance();
            android.graphics.Bitmap own = selected == null ? null
                    : com.divinedoll.launcher.utils.InstanceLook.image(requireContext(), String.valueOf(selected.name));
            if(own != null) { mBannerView.setImageBitmap(own); custom = true; }
        } catch(Exception ignored) {}
        if(mBannerTitle != null) BannerManager.setTitleVisible(mBannerTitle, custom);
    }

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        com.divinedoll.launcher.utils.DivineTheme.applyOnce();
        view.setBackgroundColor(com.divinedoll.launcher.utils.ThemeColors.background());
        View sidebar = view.findViewById(R.id.sidebar_scroll);
        if(sidebar != null) sidebar.setBackgroundColor(com.divinedoll.launcher.utils.ThemeColors.sidebar());
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mModsButton = view.findViewById(R.id.mods_button);
        Button mLanButton = view.findViewById(R.id.lan_button);
        Button mBedrockButton = view.findViewById(R.id.bedrock_button);
        Button mAppearanceButton = view.findViewById(R.id.appearance_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        com.divinedoll.launcher.utils.McStyle.headingText(mPlayButton);
        mPlayButton.setBackground(com.divinedoll.launcher.utils.PlayColor.background(requireContext()));
        mPlayButton.setOnLongClickListener(v -> {
            com.divinedoll.launcher.utils.PlayColor.choose(requireContext(), () ->
                    mPlayButton.setBackground(com.divinedoll.launcher.utils.PlayColor.background(requireContext())));
            return true;
        });
        mPlayRef = mPlayButton;
        com.divinedoll.launcher.utils.UpdateCheck.run(requireActivity());
        view.post(this::runSetupWizard);
        buildHomeCard(view);
        buildContentPanel(view);
        mBannerView = view.findViewById(R.id.bedrock_banner);
        mBannerTitle = view.findViewById(R.id.bedrock_banner_title);
        if(mBannerTitle instanceof TextView) ((TextView) mBannerTitle).setText(com.divinedoll.launcher.utils.AppearancePanel.launcherName(requireContext()));
        refreshBanner();
        mBannerView.setOnClickListener(v -> BannerManager.showChooser(requireContext(),
                () -> mBannerPicker.launch("image/*"), this::refreshBanner));
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);
        TextView tipView = view.findViewById(R.id.launcher_tip);
        if(tipView != null) {
            String[] tips = getResources().getStringArray(R.array.fork_tips);
            int[] index = {new java.util.Random().nextInt(tips.length)};
            tipView.setText(tips[index[0]]);
            tipView.setOnClickListener(v -> {
                index[0] = (index[0] + 1) % tips.length;
                tipView.setText(tips[index[0]]);
            });
        }

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mModsButton.setOnClickListener(v -> {
            Bundle modsArgs = new Bundle();
            modsArgs.putBoolean(SearchModFragment.ARG_MODPACK, false);
            Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, modsArgs);
        });
        mBedrockButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), BedrockFragment.class, BedrockFragment.TAG, null));
        // Skins, Installations and Patch notes are tabs in the header
        ViewGroup tabsContainer = view.findViewById(R.id.launcher_tabs_container);
        if(tabsContainer != null) {
            tabsContainer.addView(com.divinedoll.launcher.utils.ForkUi.pageHeader(requireContext(), requireActivity(),
                    com.divinedoll.launcher.utils.ForkUi.TAB_PLAY), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        Button javaButton = view.findViewById(R.id.java_button);
        if(javaButton != null && !com.divinedoll.launcher.utils.UiStyle.rounded()) javaButton.setBackgroundColor(0xFF343434);
        com.divinedoll.launcher.utils.DivineTheme.styleLauncher(view, mPlayButton, javaButton);
        com.divinedoll.launcher.utils.DivineTheme.applyRail(view);
        mAppearanceButton.setOnClickListener(v -> showAppearanceDialog(mPlayButton));
        mLanButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), LanFragment.class, LanFragment.TAG, null));
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));

        mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));


        mNewsButton.setOnLongClickListener((v)->{
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
    }

    private Button mPlayRef;
    private android.widget.LinearLayout mContentPanel;
    private TextView mChipMods, mChipWorlds, mChipPacks;
    private android.widget.LinearLayout mWorldList;
    private TextView mPlayingAs;
    private java.util.List<String[]> mNews = new java.util.ArrayList<>();
    private boolean mNewsLoading, mNewsTried;

    private boolean wideLayout() {
        return com.divinedoll.launcher.utils.UiStyle.rounded() && !com.divinedoll.launcher.utils.DivineTheme.isPortrait(requireContext());
    }

    private TextView chip(Context c, android.widget.LinearLayout parent, int label) {
        android.widget.LinearLayout box = new android.widget.LinearLayout(c);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER);
        int pad = com.divinedoll.launcher.utils.ForkUi.dp(c, 8);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(com.divinedoll.launcher.utils.ForkUi.box(c, com.divinedoll.launcher.utils.ThemeColors.background(), 0x22FFFFFF, 1));
        TextView value = com.divinedoll.launcher.utils.ForkUi.text(c, "0", 20, true);
        value.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
        value.setGravity(android.view.Gravity.CENTER);
        box.addView(value);
        TextView name = com.divinedoll.launcher.utils.ForkUi.text(c, getString(label), 11, false);
        name.setTextColor(0xFFBBBBBB);
        name.setGravity(android.view.Gravity.CENTER);
        box.addView(name);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = com.divinedoll.launcher.utils.ForkUi.dp(c, 6);
        parent.addView(box, lp);
        return value;
    }

    /** Landscape: card under the header with counters and the latest worlds of the selected installation. */
    private void buildContentPanel(View root) {
        if(!(root instanceof androidx.constraintlayout.widget.ConstraintLayout) || !wideLayout()) return;
        Context c = requireContext();
        com.divinedoll.launcher.utils.DivineTheme.shortenBanner(root, 0.44f);
        mContentPanel = new android.widget.LinearLayout(c);
        mContentPanel.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = com.divinedoll.launcher.utils.ForkUi.dp(c, 12);
        mContentPanel.setPadding(pad, pad, pad, pad);
        mContentPanel.setBackground(com.divinedoll.launcher.utils.ForkUi.box(c, com.divinedoll.launcher.utils.ThemeColors.sidebar(), 0x22FFFFFF, 1));
        mPlayingAs = com.divinedoll.launcher.utils.ForkUi.text(c, "", 13, true);
        mPlayingAs.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
        mPlayingAs.setPadding(0, 0, 0, com.divinedoll.launcher.utils.ForkUi.dp(c, 8));
        mContentPanel.addView(mPlayingAs);
        android.widget.LinearLayout chips = new android.widget.LinearLayout(c);
        chips.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        mChipMods = chip(c, chips, R.string.fork_chip_mods);
        mChipWorlds = chip(c, chips, R.string.fork_chip_worlds);
        mChipPacks = chip(c, chips, R.string.fork_chip_packs);
        mContentPanel.addView(chips, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = com.divinedoll.launcher.utils.ForkUi.text(c, getString(R.string.fork_recent_worlds), 12, true);
        title.setAllCaps(true);
        title.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
        title.setPadding(0, com.divinedoll.launcher.utils.ForkUi.dp(c, 10), 0, com.divinedoll.launcher.utils.ForkUi.dp(c, 4));
        mContentPanel.addView(title);
        android.widget.ScrollView scroll = new android.widget.ScrollView(c);
        mWorldList = new android.widget.LinearLayout(c);
        mWorldList.setOrientation(android.widget.LinearLayout.VERTICAL);
        scroll.addView(mWorldList);
        mContentPanel.addView(scroll, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(0, 0);
        params.startToStart = R.id.bedrock_banner;
        params.endToEnd = R.id.bedrock_banner;
        params.topToBottom = R.id.bedrock_banner;
        params.bottomToTop = R.id._background_display_view;
        params.bottomMargin = com.divinedoll.launcher.utils.ForkUi.dp(c, 8);
        params.setMarginStart(com.divinedoll.launcher.utils.ForkUi.dp(c, 12));
        params.setMarginEnd(com.divinedoll.launcher.utils.ForkUi.dp(c, 12));
        ((ViewGroup) root).addView(mContentPanel, params);
        refreshContentPanel();
    }

    private static int countEntries(java.io.File dir, boolean onlyDirs, String suffix) {
        java.io.File[] list = dir.listFiles();
        if(list == null) return 0;
        int n = 0;
        for(java.io.File f : list) {
            if(onlyDirs ? f.isDirectory() : (suffix == null || f.getName().endsWith(suffix))) n++;
        }
        return n;
    }

    private void refreshContentPanel() {
        if(mContentPanel == null || getContext() == null) return;
        Context c = requireContext();
        Instance instance = Instances.loadSelectedInstance();
        mWorldList.removeAllViews();
        try {
            com.divinedoll.launcher.authenticator.accounts.Account account = com.divinedoll.launcher.authenticator.accounts.Accounts.getCurrent();
            mPlayingAs.setText(account == null ? "" : getString(R.string.fork_playing_as, account.username));
            mPlayingAs.setVisibility(account == null ? View.GONE : View.VISIBLE);
        } catch(Exception e) {
            mPlayingAs.setVisibility(View.GONE);
        }
        loadNews();
        if(instance == null) {
            mChipMods.setText("0");
            mChipWorlds.setText("0");
            mChipPacks.setText("0");
            return;
        }
        java.io.File dir = instance.getGameDirectory();
        mChipMods.setText(String.valueOf(countEntries(new java.io.File(dir, "mods"), false, ".jar")));
        java.io.File[] worlds = new java.io.File(dir, "saves").listFiles(java.io.File::isDirectory);
        mChipWorlds.setText(String.valueOf(worlds == null ? 0 : worlds.length));
        mChipPacks.setText(String.valueOf(countEntries(new java.io.File(dir, "resourcepacks"), false, null)));
        if(worlds == null || worlds.length == 0) {
            TextView none = com.divinedoll.launcher.utils.ForkUi.text(c, getString(R.string.fork_no_worlds), 13, false);
            none.setTextColor(0xFFBBBBBB);
            mWorldList.addView(none);
            return;
        }
        java.util.Arrays.sort(worlds, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for(int i = 0; i < Math.min(5, worlds.length); i++) {
            android.widget.LinearLayout row = com.divinedoll.launcher.utils.ForkUi.row(c);
            int p = com.divinedoll.launcher.utils.ForkUi.dp(c, 6);
            row.setPadding(p, p, p, p);
            TextView name = com.divinedoll.launcher.utils.ForkUi.text(c, worlds[i].getName(), 14, false);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(name, com.divinedoll.launcher.utils.ForkUi.weight(1f));
            TextView when = com.divinedoll.launcher.utils.ForkUi.text(c, String.valueOf(android.text.format.DateUtils.getRelativeTimeSpanString(
                    worlds[i].lastModified(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)), 11, false);
            when.setTextColor(0xFFBBBBBB);
            row.addView(when);
            mWorldList.addView(row);
        }
        renderNews(c);
    }

    /** First start: welcome, color and button pack; then, after the screen is rebuilt, the language. */
    private void runSetupWizard() {
        if(!isAdded()) return;
        Context c = requireContext();
        android.content.SharedPreferences prefs = com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF;
        int stage = prefs.getInt("setup_stage", 0);
        if(stage >= 2) return;
        if(stage == 1) {
            prefs.edit().putInt("setup_stage", 2).apply();
            com.divinedoll.launcher.utils.LanguagePicker.show(c, () -> requireActivity().recreate());
            return;
        }
        new android.app.AlertDialog.Builder(c).setCancelable(false)
                .setTitle(R.string.fork_setup_title).setMessage(R.string.fork_setup_message)
                .setPositiveButton(R.string.fork_setup_start, (d, w) -> {
                    prefs.edit().putInt("setup_stage", 1).apply();
                    // Button pack first, then the color, then the screen is rebuilt and the language is asked
                    com.divinedoll.launcher.utils.ControlPacks.show(c, () ->
                            com.divinedoll.launcher.utils.AccentColor.choose(c, () -> requireActivity().recreate()));
                })
                .setNegativeButton(R.string.fork_setup_skip, (d, w) -> prefs.edit().putInt("setup_stage", 2).apply())
                .show();
    }

    private void renderNews(Context c) {
        if(mNews.isEmpty() || mWorldList == null) return;
        TextView title = com.divinedoll.launcher.utils.ForkUi.text(c, getString(R.string.fork_news), 12, true);
        title.setAllCaps(true);
        title.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
        title.setPadding(0, com.divinedoll.launcher.utils.ForkUi.dp(c, 12), 0, com.divinedoll.launcher.utils.ForkUi.dp(c, 4));
        mWorldList.addView(title);
        for(String[] item : mNews) {
            TextView t = com.divinedoll.launcher.utils.ForkUi.text(c, item[0], 13, false);
            t.setPadding(com.divinedoll.launcher.utils.ForkUi.dp(c, 6), com.divinedoll.launcher.utils.ForkUi.dp(c, 6), 0, com.divinedoll.launcher.utils.ForkUi.dp(c, 6));
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if(item[1] != null && !item[1].isEmpty()) t.setOnClickListener(v -> Tools.openURL(requireActivity(), item[1]));
            mWorldList.addView(t);
        }
    }

    /** Latest Minecraft news, fetched once per screen. Fails silently when offline. */
    private void loadNews() {
        if(mNewsLoading || mNewsTried || !mNews.isEmpty()) return;
        mNewsLoading = true;
        mNewsTried = true;
        new Thread(() -> {
            java.util.List<String[]> found = new java.util.ArrayList<>();
            try {
                java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL("https://launchercontent.mojang.com/v2/news.json").openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                con.setRequestProperty("User-Agent", "DivineDoll/DivineLauncher");
                try(java.io.InputStream in = con.getInputStream()) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    com.google.gson.JsonObject root = new com.google.gson.Gson().fromJson(out.toString("UTF-8"), com.google.gson.JsonObject.class);
                    com.google.gson.JsonArray entries = root.getAsJsonArray("entries");
                    if(entries != null) for(com.google.gson.JsonElement e : entries) {
                        com.google.gson.JsonObject o = e.getAsJsonObject();
                        if(!o.has("title") || o.get("title").isJsonNull()) continue;
                        String link = o.has("readMoreLink") && !o.get("readMoreLink").isJsonNull() ? o.get("readMoreLink").getAsString() : "";
                        found.add(new String[]{o.get("title").getAsString(), link});
                        if(found.size() >= 4) break;
                    }
                }
            } catch(Exception ignored) {
            }
            Tools.runOnUiThread(() -> {
                mNewsLoading = false;
                if(!isAdded()) return;
                mNews = found;
                refreshContentPanel();
            });
        }).start();
    }

    private TextView mHomeName, mHomeInfo, mHomeWorld;
    private android.widget.ImageView mHomeIcon;

    /** Wide panel over the bottom of the banner: icon, the instance that will be played, play time, last world and quick actions. */
    private void buildHomeCard(View root) {
        if(!(root instanceof androidx.constraintlayout.widget.ConstraintLayout)) return;
        Context c = requireContext();
        android.widget.LinearLayout card = new android.widget.LinearLayout(c);
        card.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = com.divinedoll.launcher.utils.ForkUi.dp(c, 8);
        card.setPadding(pad, pad, pad, pad);
        if(com.divinedoll.launcher.utils.UiStyle.rounded()) {
            card.setBackground(com.divinedoll.launcher.utils.ForkUi.box(c, com.divinedoll.launcher.utils.ThemeColors.sidebar(), 0x33FFFFFF, 1));
        } else {
            card.setBackground(com.divinedoll.launcher.utils.ForkUi.box(c, 0xCC000000, com.divinedoll.launcher.utils.AccentColor.get(), 2));
        }

        mHomeIcon = new android.widget.ImageView(c);
        mHomeIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        card.addView(mHomeIcon, new android.widget.LinearLayout.LayoutParams(
                com.divinedoll.launcher.utils.ForkUi.dp(c, 44), com.divinedoll.launcher.utils.ForkUi.dp(c, 44)));

        android.widget.LinearLayout texts = new android.widget.LinearLayout(c);
        texts.setOrientation(android.widget.LinearLayout.VERTICAL);
        texts.setPadding(com.divinedoll.launcher.utils.ForkUi.dp(c, 10), 0, com.divinedoll.launcher.utils.ForkUi.dp(c, 8), 0);
        mHomeName = com.divinedoll.launcher.utils.ForkUi.text(c, "", 15, true);
        mHomeName.setSingleLine(true);
        mHomeName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(mHomeName);
        mHomeInfo = com.divinedoll.launcher.utils.ForkUi.text(c, "", 11, false);
        mHomeInfo.setTextColor(0xFFCCCCCC);
        mHomeInfo.setSingleLine(true);
        mHomeInfo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(mHomeInfo);
        mHomeWorld = com.divinedoll.launcher.utils.ForkUi.text(c, "", 10, false);
        mHomeWorld.setTextColor(com.divinedoll.launcher.utils.AccentColor.light());
        mHomeWorld.setSingleLine(true);
        mHomeWorld.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(mHomeWorld);
        card.addView(texts, new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int[] labels = {R.string.fork_home_files, R.string.fork_home_logs, R.string.fork_home_edit, R.string.fork_home_servers};
        for(int i = 0; i < labels.length; i++) {
            final int which = i;
            Button b = com.divinedoll.launcher.utils.ForkUi.button(c, getString(labels[i]), false);
            b.setTextSize(11);
            b.setMinWidth(0);
            b.setMinimumWidth(0);
            b.setMinHeight(com.divinedoll.launcher.utils.ForkUi.dp(c, 36));
            b.setMinimumHeight(com.divinedoll.launcher.utils.ForkUi.dp(c, 36));
            b.setPadding(com.divinedoll.launcher.utils.ForkUi.dp(c, 10), 0, com.divinedoll.launcher.utils.ForkUi.dp(c, 10), 0);
            b.setOnClickListener(v -> onHomeAction(which));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = com.divinedoll.launcher.utils.ForkUi.dp(c, 4);
            card.addView(b, lp);
        }

        if(wideLayout() && mPlayRef != null) {
            Button playHere = com.divinedoll.launcher.utils.ForkUi.button(c, getString(R.string.main_play), true);
            playHere.setMinWidth(com.divinedoll.launcher.utils.ForkUi.dp(c, 96));
            playHere.setOnClickListener(v -> mPlayRef.performClick());
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = com.divinedoll.launcher.utils.ForkUi.dp(c, 8);
            card.addView(playHere, lp);
        }

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.startToStart = R.id.bedrock_banner;
        params.endToEnd = R.id.bedrock_banner;
        params.bottomToTop = R.id.launcher_tip;
        params.setMarginStart(com.divinedoll.launcher.utils.ForkUi.dp(c, 12));
        params.setMarginEnd(com.divinedoll.launcher.utils.ForkUi.dp(c, 12));
        params.bottomMargin = com.divinedoll.launcher.utils.ForkUi.dp(c, 4);
        ((ViewGroup) root).addView(card, params);
        refreshHomeCard();
    }

    private static String lastWorld(java.io.File gameDir) {
        java.io.File[] worlds = new java.io.File(gameDir, "saves").listFiles(java.io.File::isDirectory);
        if(worlds == null || worlds.length == 0) return null;
        java.io.File newest = worlds[0];
        for(java.io.File w : worlds) if(w.lastModified() > newest.lastModified()) newest = w;
        return newest.getName();
    }

    private void refreshHomeCard() {
        refreshContentPanel();
        if(mHomeName == null || getContext() == null) return;
        Context c = requireContext();
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            mHomeName.setText(R.string.fork_home_no_instance);
            mHomeInfo.setText("");
            mHomeWorld.setText("");
            mHomeIcon.setImageBitmap(null);
            return;
        }
        mHomeIcon.setImageBitmap(InstallationsFragment.iconOf(c, instance));
        mHomeName.setText(String.valueOf(instance.name));
        mHomeInfo.setText(instance.versionId + "  ·  " + com.divinedoll.launcher.utils.InstanceTools.stats(c, String.valueOf(instance.name)));
        String world = lastWorld(instance.getGameDirectory());
        mHomeWorld.setText(world == null ? "" : getString(R.string.fork_home_world, world));
    }

    private void onHomeAction(int which) {
        Context c = requireContext();
        Instance instance = Instances.loadSelectedInstance();
        if(which == 3) {
            Tools.swapFragment(requireActivity(), ServersFragment.class, ServersFragment.TAG, null);
        } else if(which == 1) {
            shareLog(c);
        } else if(instance == null) {
            Toast.makeText(c, R.string.no_instance, Toast.LENGTH_LONG).show();
        } else if(which == 0) {
            InstanceHubFragment.open(requireActivity(), String.valueOf(instance.name));
        } else {
            Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
        }
    }

    private static final String BEDROCK_PACKAGE = "com.mojang.minecraftpe";

    private boolean isBedrockInstalled(Context context) {
        return context.getPackageManager().getLaunchIntentForPackage(BEDROCK_PACKAGE) != null;
    }

    /** Shows "Installed" or "Not installed" under the Bedrock button. */
    private void updateBedrockStatus() {
        View root = getView();
        Context context = getContext();
        if(root == null || context == null || (com.divinedoll.launcher.utils.UiStyle.iconRail()
                && context.getResources().getConfiguration().orientation != android.content.res.Configuration.ORIENTATION_PORTRAIT)) return;
        Button button = root.findViewById(R.id.bedrock_button);
        if(button == null) return;
        boolean installed = isBedrockInstalled(context);
        String title = getString(R.string.fork_bedrock_button);
        String status = getString(installed ? R.string.fork_bedrock_installed : R.string.fork_bedrock_not_installed);
        android.text.SpannableStringBuilder text = new android.text.SpannableStringBuilder(title + "\n" + status);
        int start = title.length() + 1;
        text.setSpan(new android.text.style.RelativeSizeSpan(0.75f), start, text.length(), 0);
        text.setSpan(new android.text.style.ForegroundColorSpan(installed ? 0xFF7CD65A : 0xFFE08A5A), start, text.length(), 0);
        button.setText(text);
    }

    /** Bedrock is a separate app: open it if installed, otherwise offer its store page. */
    private void openBedrock() {
        Context context = requireContext();
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(BEDROCK_PACKAGE);
        if(launch != null) {
            try {
                startActivity(launch);
            } catch (Exception e) {
                Toast.makeText(context, R.string.fork_bedrock_missing, Toast.LENGTH_LONG).show();
            }
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(R.string.fork_bedrock_button)
                .setMessage(R.string.fork_bedrock_missing)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.fork_bedrock_open_store, (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BEDROCK_PACKAGE)));
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + BEDROCK_PACKAGE)));
                        } catch (Exception ignored) {
                            Toast.makeText(context, R.string.fork_bedrock_missing, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private final androidx.activity.result.ActivityResultLauncher<String> mThemePicker =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if(uri == null) return;
                boolean ok = com.divinedoll.launcher.utils.ThemeManager.importFrom(requireContext(), uri);
                Toast.makeText(requireContext(), ok ? R.string.fork_theme_imported : R.string.fork_theme_error, Toast.LENGTH_LONG).show();
                if(ok) requireActivity().recreate();
            });

    private void showAppearanceDialog(Button playButton) {
        Context c = requireContext();
        com.divinedoll.launcher.utils.AppearancePanel.show(requireActivity(), new com.divinedoll.launcher.utils.AppearancePanel.Hooks() {
            @Override public void pickBanner() {
                BannerManager.showChooser(c, () -> mBannerPicker.launch("image/*"), MainMenuFragment.this::refreshBanner);
            }
            @Override public void refreshBanner() {
                MainMenuFragment.this.refreshBanner();
            }
            @Override public void refreshPlay() {
                playButton.setBackground(com.divinedoll.launcher.utils.PlayColor.background(c));
            }
            @Override public void pickThemeFile() {
                mThemePicker.launch("*/*");
            }
            @Override public void recreate() {
                requireActivity().recreate();
            }
        });
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHomeCard();
        updateBedrockStatus();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
