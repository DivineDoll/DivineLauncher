package git.artdeell.mojo.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.List;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.authenticator.accounts.SkinLibrary;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.AccentColor;
import git.artdeell.mojo.utils.BedrockContent;
import git.artdeell.mojo.utils.ForkUi;
import git.artdeell.mojo.utils.McStyle;
import git.artdeell.mojo.utils.OnlineSkins;
import git.artdeell.mojo.utils.ThemeColors;

/** Bedrock section laid out like the Java one: play, add-ons, textures and skins. */
public class BedrockFragment extends Fragment {
    public static final String TAG = "BedrockFragment";
    private static final int TAB_PLAY = 0, TAB_ADDONS = 1, TAB_TEXTURES = 2, TAB_SKINS = 3;

    private FrameLayout mContent;
    private LinearLayout mTabs;
    private int mTab = TAB_PLAY;
    private int mLoadId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());
        root.setPadding(ForkUi.dp(c, 16), ForkUi.dp(c, 12), ForkUi.dp(c, 16), ForkUi.dp(c, 12));

        LinearLayout top = ForkUi.row(c);
        TextView title = ForkUi.text(c, getString(R.string.fork_header_bedrock), 12, true);
        title.setAllCaps(true);
        title.setLetterSpacing(0.06f);
        title.setPadding(ForkUi.dp(c, 8), ForkUi.dp(c, 6), 0, 0);
        top.addView(title, ForkUi.weight(1f));
        Button back = ForkUi.button(c, getString(R.string.fork_bedrock_back), false);
        back.setOnClickListener(v -> Tools.backToMainMenu(requireActivity()));
        top.addView(back);
        root.addView(top);

        mTabs = new LinearLayout(c);
        mTabs.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(mTabs);

        mContent = new FrameLayout(c);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cp.topMargin = ForkUi.dp(c, 8);
        root.addView(mContent, cp);

        select(TAB_PLAY);
        return root;
    }

    private void select(int tab) {
        mTab = tab;
        mLoadId++;
        Context c = requireContext();
        mTabs.removeAllViews();
        int[] labels = {R.string.fork_tab_play, R.string.fork_bedrock_addons, R.string.fork_bedrock_textures, R.string.fork_skin_title};
        for(int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView t = ForkUi.text(c, getString(labels[i]), 15, false);
            t.setTextColor(i == tab ? Color.WHITE : ForkUi.TAB_IDLE);
            t.setPadding(ForkUi.dp(c, 14), ForkUi.dp(c, 8), ForkUi.dp(c, 14), ForkUi.dp(c, 8));
            if(i == tab) {
                android.graphics.drawable.GradientDrawable line = new android.graphics.drawable.GradientDrawable();
                line.setColor(AccentColor.get());
                android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{line});
                layers.setLayerInset(0, ForkUi.dp(c, 8), ForkUi.dp(c, 34), ForkUi.dp(c, 8), 0);
                t.setBackground(layers);
            } else {
                t.setOnClickListener(v -> select(index));
            }
            mTabs.addView(t);
        }
        mContent.removeAllViews();
        if(tab == TAB_PLAY) mContent.addView(buildPlay(c));
        else if(tab == TAB_SKINS) mContent.addView(buildSkins(c));
        else mContent.addView(buildBrowser(c, tab == TAB_ADDONS ? "addon" : "texture"));
    }

    private LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(ForkUi.box(c, ThemeColors.panel(), 0, 0));
        int p = ForkUi.dp(c, 12);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = ForkUi.dp(c, 8);
        card.setLayoutParams(lp);
        return card;
    }

    private void toast(int res) {
        if(isAdded()) Toast.makeText(requireContext(), res, Toast.LENGTH_LONG).show();
    }

    private void toast(String text) {
        if(isAdded()) Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    // Play

    private View buildPlay(Context c) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        boolean installed = BedrockContent.isInstalled(c);
        TextView status = ForkUi.text(c, getString(installed ? R.string.fork_bedrock_installed : R.string.fork_bedrock_not_installed), 16, false);
        status.setTextColor(installed ? 0xFF7CD65A : 0xFFE08A5A);
        status.setGravity(Gravity.CENTER);
        box.addView(status);
        TextView info = ForkUi.text(c, getString(R.string.fork_bedrock_info), 13, false);
        info.setTextColor(ForkUi.TAB_IDLE);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, ForkUi.dp(c, 8), 0, ForkUi.dp(c, 16));
        box.addView(info);
        Button play = ForkUi.button(c, getString(installed ? R.string.fork_bedrock_open : R.string.fork_bedrock_open_store), true);
        play.setTextSize(18);
        play.setOnClickListener(v -> openBedrockOrStore());
        box.addView(play, new LinearLayout.LayoutParams(ForkUi.dp(c, 280), ForkUi.dp(c, 56)));
        return box;
    }

    private void openBedrockOrStore() {
        Context context = requireContext();
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(BedrockContent.BEDROCK_PACKAGE);
        try {
            if(launch != null) {
                startActivity(launch);
            } else {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BedrockContent.BEDROCK_PACKAGE)));
                } catch(Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + BedrockContent.BEDROCK_PACKAGE)));
                }
            }
        } catch(Exception e) {
            toast(R.string.fork_bedrock_missing);
        }
    }

    // Add-ons and textures

    private View buildBrowser(Context c, String kind) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = ForkUi.row(c);
        EditText query = new EditText(c);
        query.setHint(R.string.fork_bedrock_search_hint);
        query.setHintTextColor(0xFF888888);
        query.setTextColor(Color.WHITE);
        query.setSingleLine();
        query.setInputType(InputType.TYPE_CLASS_TEXT);
        McStyle.text(query);
        bar.addView(query, ForkUi.weight(1f));
        Button search = ForkUi.button(c, getString(R.string.fork_bedrock_search), true);
        Button key = ForkUi.button(c, getString(R.string.fork_bedrock_key), false);
        bar.addView(search);
        bar.addView(key);
        box.addView(bar);

        TextView status = ForkUi.text(c, "", 14, false);
        status.setPadding(0, ForkUi.dp(c, 6), 0, ForkUi.dp(c, 6));
        box.addView(status);
        ScrollView scroll = new ScrollView(c);
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        box.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        key.setOnClickListener(v -> askKey());
        Runnable run = () -> runSearch(kind, query.getText().toString().trim(), status, list);
        search.setOnClickListener(v -> run.run());
        query.setOnEditorActionListener((v, a, e) -> { run.run(); return true; });
        run.run();
        return box;
    }

    private void askKey() {
        Context c = requireContext();
        EditText input = new EditText(c);
        input.setHint("CurseForge API key");
        input.setSingleLine();
        input.setText(LauncherPreferences.DEFAULT_PREF.getString(BedrockContent.PREF_KEY, ""));
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_bedrock_key)
                .setMessage(R.string.fork_bedrock_key_help)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.fork_bedrock_key_get, (d, w) -> Tools.openURL(requireActivity(), "https://console.curseforge.com/"))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    LauncherPreferences.DEFAULT_PREF.edit().putString(BedrockContent.PREF_KEY, input.getText().toString().trim()).apply();
                    select(mTab);
                })
                .show();
    }

    private void runSearch(String kind, String query, TextView status, LinearLayout list) {
        final int id = ++mLoadId;
        Context c = requireContext();
        list.removeAllViews();
        status.setText(R.string.fork_bedrock_loading);
        MojoApplication.sExecutorService.execute(() -> {
            try {
                List<BedrockContent.Item> items = BedrockContent.search(c, kind, query);
                Tools.runOnUiThread(() -> {
                    if(!isAdded() || id != mLoadId) return;
                    status.setText(items.isEmpty() ? getString(R.string.fork_bedrock_no_results) : "");
                    for(BedrockContent.Item item : items) list.addView(itemCard(c, item));
                });
            } catch(Exception e) {
                boolean noKey = "NO_KEY".equals(e.getMessage());
                Tools.runOnUiThread(() -> {
                    if(!isAdded() || id != mLoadId) return;
                    status.setText(noKey ? getString(R.string.fork_bedrock_need_key)
                            : getString(R.string.fork_bedrock_error, String.valueOf(e.getMessage())));
                });
            }
        });
    }

    private View itemCard(Context c, BedrockContent.Item item) {
        LinearLayout card = card(c);
        card.addView(ForkUi.text(c, item.name, 15, true));
        if(!item.author.isEmpty()) {
            TextView by = ForkUi.text(c, item.author + "  -  " + item.downloads + " downloads", 11, false);
            by.setTextColor(ForkUi.TAB_IDLE);
            card.addView(by);
        }
        TextView summary = ForkUi.text(c, item.summary, 12, false);
        summary.setPadding(0, ForkUi.dp(c, 4), 0, ForkUi.dp(c, 6));
        card.addView(summary);
        Button get = ForkUi.button(c, getString(R.string.fork_bedrock_install), true);
        get.setOnClickListener(v -> {
            get.setEnabled(false);
            get.setText(R.string.fork_bedrock_loading);
            MojoApplication.sExecutorService.execute(() -> {
                try {
                    File file = BedrockContent.download(c, item);
                    Tools.runOnUiThread(() -> {
                        get.setEnabled(true);
                        get.setText(R.string.fork_bedrock_install);
                        if(isAdded() && !BedrockContent.openWithBedrock(requireContext(), file)) toast(R.string.fork_bedrock_open_failed);
                    });
                } catch(Exception e) {
                    Tools.runOnUiThread(() -> {
                        get.setEnabled(true);
                        get.setText(R.string.fork_bedrock_install);
                        toast(getString(R.string.fork_bedrock_error, String.valueOf(e.getMessage())));
                    });
                }
            });
        });
        card.addView(get);
        return card;
    }

    // Skins

    private View buildSkins(Context c) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = ForkUi.row(c);
        EditText name = new EditText(c);
        name.setHint(R.string.fork_bedrock_skin_hint);
        name.setHintTextColor(0xFF888888);
        name.setTextColor(Color.WHITE);
        name.setSingleLine();
        McStyle.text(name);
        bar.addView(name, ForkUi.weight(1f));
        Button get = ForkUi.button(c, getString(R.string.fork_bedrock_skin_download), true);
        bar.addView(get);
        box.addView(bar);

        TextView hint = ForkUi.text(c, getString(R.string.fork_bedrock_skin_info), 12, false);
        hint.setTextColor(ForkUi.TAB_IDLE);
        hint.setPadding(0, ForkUi.dp(c, 6), 0, ForkUi.dp(c, 6));
        box.addView(hint);

        ScrollView scroll = new ScrollView(c);
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        box.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        for(SkinLibrary.Entry entry : SkinLibrary.list(c)) {
            LinearLayout card = card(c);
            LinearLayout row = ForkUi.row(c);
            row.addView(ForkUi.text(c, entry.name, 14, false), ForkUi.weight(1f));
            Button install = ForkUi.button(c, getString(R.string.fork_bedrock_install), false);
            install.setOnClickListener(v -> installSkin(entry.name, SkinLibrary.load(entry)));
            row.addView(install);
            card.addView(row);
            list.addView(card);
        }

        get.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if(n.isEmpty()) return;
            get.setEnabled(false);
            MojoApplication.sExecutorService.execute(() -> {
                try {
                    SkinLibrary.Entry entry = OnlineSkins.download(c, n);
                    Bitmap bmp = SkinLibrary.load(entry);
                    Tools.runOnUiThread(() -> {
                        get.setEnabled(true);
                        installSkin(n, bmp);
                    });
                } catch(Exception e) {
                    Tools.runOnUiThread(() -> {
                        get.setEnabled(true);
                        toast(getString(R.string.fork_bedrock_error, String.valueOf(e.getMessage())));
                    });
                }
            });
        });
        return box;
    }

    private void installSkin(String name, Bitmap skin) {
        if(skin == null) {
            toast(R.string.fork_bedrock_open_failed);
            return;
        }
        Context c = requireContext();
        MojoApplication.sExecutorService.execute(() -> {
            try {
                File pack = BedrockContent.buildSkinPack(c, name, skin, SkinLibrary.isSlim(skin));
                Tools.runOnUiThread(() -> {
                    if(isAdded() && !BedrockContent.openWithBedrock(requireContext(), pack)) toast(R.string.fork_bedrock_open_failed);
                });
            } catch(Exception e) {
                Tools.runOnUiThread(() -> toast(getString(R.string.fork_bedrock_error, String.valueOf(e.getMessage()))));
            }
        });
    }
}
