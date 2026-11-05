package git.artdeell.mojo.fragments;

import static git.artdeell.mojo.Tools.openPath;
import static git.artdeell.mojo.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import git.artdeell.mojo.CustomControlsActivity;
import git.artdeell.mojo.R;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.extra.ExtraConstants;
import git.artdeell.mojo.extra.ExtraCore;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.progresskeeper.ProgressKeeper;
import git.artdeell.mojo.utils.BannerManager;
import git.artdeell.mojo.utils.FileUtils;

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
        if(mBannerTitle != null) BannerManager.setTitleVisible(mBannerTitle, custom);
    }

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(git.artdeell.mojo.utils.ThemeColors.background());
        View sidebar = view.findViewById(R.id.sidebar_scroll);
        if(sidebar != null) sidebar.setBackgroundColor(git.artdeell.mojo.utils.ThemeColors.sidebar());
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
        git.artdeell.mojo.utils.McStyle.headingText(mPlayButton);
        mPlayButton.setBackground(git.artdeell.mojo.utils.PlayColor.background(requireContext()));
        mPlayButton.setOnLongClickListener(v -> {
            git.artdeell.mojo.utils.PlayColor.choose(requireContext(), () ->
                    mPlayButton.setBackground(git.artdeell.mojo.utils.PlayColor.background(requireContext())));
            return true;
        });
        mBannerView = view.findViewById(R.id.bedrock_banner);
        mBannerTitle = view.findViewById(R.id.bedrock_banner_title);
        refreshBanner();
        mBannerView.setOnClickListener(v -> BannerManager.showChooser(requireContext(),
                () -> mBannerPicker.launch("image/*"), this::refreshBanner));
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

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
            tabsContainer.addView(git.artdeell.mojo.utils.ForkUi.pageHeader(requireContext(), requireActivity(),
                    git.artdeell.mojo.utils.ForkUi.TAB_PLAY), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        Button javaButton = view.findViewById(R.id.java_button);
        if(javaButton != null) javaButton.setBackgroundColor(0xFF343434);
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

    private static final String BEDROCK_PACKAGE = "com.mojang.minecraftpe";

    private boolean isBedrockInstalled(Context context) {
        return context.getPackageManager().getLaunchIntentForPackage(BEDROCK_PACKAGE) != null;
    }

    /** Shows "Installed" or "Not installed" under the Bedrock button. */
    private void updateBedrockStatus() {
        View root = getView();
        Context context = getContext();
        if(root == null || context == null) return;
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
                boolean ok = git.artdeell.mojo.utils.ThemeManager.importFrom(requireContext(), uri);
                Toast.makeText(requireContext(), ok ? R.string.fork_theme_imported : R.string.fork_theme_error, Toast.LENGTH_LONG).show();
                if(ok) requireActivity().recreate();
            });

    private void showAppearanceDialog(Button playButton) {
        String[] items = {getString(R.string.fork_banner_pick_title), getString(R.string.fork_play_color_title),
                getString(R.string.fork_accent_title), getString(R.string.fork_bg_color_title),
                getString(R.string.fork_sidebar_color_title), getString(R.string.fork_pixel_menu),
                getString(R.string.fork_theme_menu), getString(R.string.fork_colors_reset_all)};
        Runnable recreate = () -> requireActivity().recreate();
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.fork_appearance_title)
                .setItems(items, (d, which) -> {
                    Context c = requireContext();
                    if(which == 0) {
                        BannerManager.showChooser(c, () -> mBannerPicker.launch("image/*"), this::refreshBanner);
                    } else if(which == 1) {
                        git.artdeell.mojo.utils.PlayColor.choose(c, () ->
                                playButton.setBackground(git.artdeell.mojo.utils.PlayColor.background(c)));
                    } else if(which == 2) {
                        git.artdeell.mojo.utils.AccentColor.choose(c, recreate);
                    } else if(which == 3) {
                        git.artdeell.mojo.utils.ColorPicker.show(c, R.string.fork_bg_color_title,
                                git.artdeell.mojo.utils.ThemeColors.background(), git.artdeell.mojo.utils.ThemeColors.DEFAULT_BACKGROUND,
                                color -> { git.artdeell.mojo.utils.ThemeColors.set(git.artdeell.mojo.utils.ThemeColors.PREF_BACKGROUND, color); recreate.run(); });
                    } else if(which == 4) {
                        git.artdeell.mojo.utils.ColorPicker.show(c, R.string.fork_sidebar_color_title,
                                git.artdeell.mojo.utils.ThemeColors.sidebar(), git.artdeell.mojo.utils.ThemeColors.DEFAULT_SIDEBAR,
                                color -> { git.artdeell.mojo.utils.ThemeColors.set(git.artdeell.mojo.utils.ThemeColors.PREF_SIDEBAR, color); recreate.run(); });
                    } else if(which == 5) {
                        git.artdeell.mojo.utils.PixelArtEditor.show(c);
                    } else if(which == 6) {
                        git.artdeell.mojo.utils.ThemeManager.show(c, recreate, () -> mThemePicker.launch("*/*"));
                    } else {
                        git.artdeell.mojo.utils.ThemeColors.resetAll();
                        recreate.run();
                    }
                })
                .show();
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
        updateBedrockStatus();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
