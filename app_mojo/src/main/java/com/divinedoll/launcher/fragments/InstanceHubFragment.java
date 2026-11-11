package com.divinedoll.launcher.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.ColorPicker;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.InstanceFiles;
import com.divinedoll.launcher.utils.InstanceLook;
import com.divinedoll.launcher.utils.InstanceTools;
import com.divinedoll.launcher.utils.Modrinth;
import com.divinedoll.launcher.utils.ThemeColors;
import com.divinedoll.launcher.utils.UiStyle;
import com.divinedoll.launcher.utils.WorldBackup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** One screen per instance with tabs: mods, worlds, logs and look. */
public class InstanceHubFragment extends Fragment {
    public static final String TAG = "InstanceHubFragment";
    public static final String ARG_NAME = "name";
    private static final int TAB_MODS = 0, TAB_WORLDS = 1, TAB_LOGS = 2, TAB_LOOK = 3;

    private Instance mInstance;
    private File mGameDir;
    private LinearLayout mTabs;
    private LinearLayout mBody;
    private int mTab = TAB_MODS;
    private int mLoadId = 0;

    /** Opens the screen for the instance with that name. */
    public static void open(androidx.fragment.app.FragmentActivity act, String name) {
        Bundle b = new Bundle();
        b.putString(ARG_NAME, name);
        Tools.swapFragment(act, InstanceHubFragment.class, TAG, b);
    }

    private final ActivityResultLauncher<String> mImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
                if(uri == null || mInstance == null || !isAdded()) return;
                try {
                    InstanceLook.setImage(requireContext(), String.valueOf(mInstance.name), uri);
                    select(TAB_LOOK);
                } catch(Exception e) {
                    toast(getString(R.string.fork_op_error, String.valueOf(e.getMessage())));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        Context c = requireContext();
        String name = getArguments() == null ? null : getArguments().getString(ARG_NAME);
        try {
            mInstance = name != null ? InstanceTools.find(name) : Instances.loadSelectedInstance();
        } catch(Exception e) {
            mInstance = Instances.loadSelectedInstance();
        }
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());
        if(mInstance == null) {
            TextView none = ForkUi.text(c, getString(R.string.no_instance), 16, false);
            none.setPadding(ForkUi.dp(c, 16), ForkUi.dp(c, 16), 0, 0);
            root.addView(none);
            return root;
        }
        mGameDir = mInstance.getGameDirectory();
        int pad = ForkUi.dp(c, 12);

        LinearLayout header = ForkUi.row(c);
        header.setPadding(pad, pad, pad, ForkUi.dp(c, 6));
        Button back = ForkUi.compact(ForkUi.button(c, "‹", false));
        back.setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));
        header.addView(back);
        ImageView icon = new ImageView(c);
        icon.setImageBitmap(InstallationsFragment.iconOf(c, mInstance));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ForkUi.dp(c, 44), ForkUi.dp(c, 44));
        ilp.setMargins(ForkUi.dp(c, 10), 0, ForkUi.dp(c, 10), 0);
        header.addView(icon, ilp);
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = ForkUi.text(c, String.valueOf(mInstance.name), 18, true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView sub = ForkUi.text(c, String.valueOf(mInstance.versionId), 12, false);
        sub.setTextColor(0xFFAAAAAA);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(title);
        texts.addView(sub);
        header.addView(texts, ForkUi.weight(1f));
        Button all = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_all_files), false));
        all.setOnClickListener(v -> InstanceFiles.show(requireActivity(), mGameDir, mInstance.versionId));
        header.addView(all);
        root.addView(header);

        HorizontalScrollView tabScroll = new HorizontalScrollView(c);
        tabScroll.setHorizontalScrollBarEnabled(false);
        mTabs = new LinearLayout(c);
        mTabs.setPadding(pad, 0, pad, ForkUi.dp(c, 6));
        tabScroll.addView(mTabs);
        root.addView(tabScroll);

        ScrollView scroll = new ScrollView(c);
        mBody = new LinearLayout(c);
        mBody.setOrientation(LinearLayout.VERTICAL);
        mBody.setPadding(pad, ForkUi.dp(c, 4), pad, pad);
        scroll.addView(mBody);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        select(mTab);
        return root;
    }

    private void toast(String text) {
        if(isAdded()) Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    private void select(int tab) {
        if(!isAdded() || mInstance == null) return;
        mTab = tab;
        mLoadId++;
        Context c = requireContext();
        mTabs.removeAllViews();
        int[] labels = {R.string.fork_hub_mods, R.string.fork_hub_worlds, R.string.fork_hub_logs, R.string.fork_hub_look};
        for(int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView t = ForkUi.text(c, getString(labels[i]), 14, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(ForkUi.dp(c, 14), ForkUi.dp(c, 8), ForkUi.dp(c, 14), ForkUi.dp(c, 8));
            t.setBackground(DivineTheme.tabPill(c, i == tab));
            t.setTextColor(i == tab ? AccentColor.light() : ForkUi.TAB_IDLE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(ForkUi.dp(c, 6));
            t.setLayoutParams(lp);
            if(i != tab) t.setOnClickListener(v -> select(index));
            mTabs.addView(t);
        }
        mBody.removeAllViews();
        if(tab == TAB_MODS) buildMods(c);
        else if(tab == TAB_WORLDS) buildWorlds(c);
        else if(tab == TAB_LOGS) buildLogs(c);
        else buildLook(c);
        DivineTheme.styleTree(mBody);
    }

    private LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(ForkUi.box(c, ThemeColors.sidebar(), 0x22FFFFFF, 1));
        int p = ForkUi.dp(c, 12);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = ForkUi.dp(c, 8);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView muted(Context c, String text) {
        TextView t = ForkUi.text(c, text, 13, false);
        t.setTextColor(0xFFBBBBBB);
        t.setPadding(0, ForkUi.dp(c, 4), 0, ForkUi.dp(c, 8));
        return t;
    }

    // Mods

    private void buildMods(Context c) {
        LinearLayout bar = ForkUi.row(c);
        Button add = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_add), true));
        add.setOnClickListener(v -> Modrinth.search(requireActivity(), mGameDir, mInstance.versionId, "mod", "mods", () -> select(TAB_MODS)));
        Button update = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_update_all), false));
        update.setOnClickListener(v -> Modrinth.updateAll(requireActivity(), mGameDir, mInstance.versionId, () -> select(TAB_MODS)));
        bar.addView(add);
        bar.addView(update);
        mBody.addView(bar);

        TextView status = muted(c, getString(R.string.fork_hub_checking));
        mBody.addView(status);
        final int id = mLoadId;
        new Thread(() -> {
            int n = Modrinth.countUpdates(mGameDir, mInstance.versionId);
            Tools.runOnUiThread(() -> {
                if(!isAdded() || id != mLoadId) return;
                if(n < 0) status.setText(R.string.fork_hub_check_failed);
                else if(n == 0) status.setText(R.string.fork_hub_uptodate);
                else {
                    status.setText(getString(R.string.fork_hub_updates_n, n));
                    status.setTextColor(AccentColor.light());
                }
            });
        }).start();

        File[] files = new File(mGameDir, "mods").listFiles(f -> f.isFile() && (f.getName().endsWith(".jar") || f.getName().endsWith(".jar.disabled")));
        if(files == null || files.length == 0) {
            mBody.addView(muted(c, getString(R.string.fork_hub_no_mods)));
            return;
        }
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for(File mod : files) {
            boolean enabled = mod.getName().endsWith(".jar");
            LinearLayout card = card(c);
            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            String shown = mod.getName().replace(".jar.disabled", "").replace(".jar", "");
            TextView name = ForkUi.text(c, shown, 14, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setAlpha(enabled ? 1f : 0.5f);
            TextView size = ForkUi.text(c, Formatter.formatShortFileSize(c, mod.length()), 11, false);
            size.setTextColor(0xFFAAAAAA);
            texts.addView(name);
            texts.addView(size);
            card.addView(texts, ForkUi.weight(1f));
            SwitchCompat sw = new SwitchCompat(c);
            sw.setChecked(enabled);
            sw.setOnCheckedChangeListener((b, on) -> {
                File target = new File(mod.getParentFile(), on ? shown + ".jar" : shown + ".jar.disabled");
                if(!mod.renameTo(target)) toast(getString(R.string.fork_op_error, "rename"));
                select(TAB_MODS);
            });
            card.addView(sw);
            Button del = ForkUi.compact(ForkUi.button(c, "✕", false));
            del.setOnClickListener(v -> new AlertDialog.Builder(c)
                    .setMessage(getString(R.string.fork_hub_delete_confirm, shown))
                    .setPositiveButton(android.R.string.ok, (d, w) -> { mod.delete(); select(TAB_MODS); })
                    .setNegativeButton(android.R.string.cancel, null).show());
            card.addView(del);
            mBody.addView(card);
        }
    }

    // Worlds

    private static long sizeOf(File f) {
        if(f.isFile()) return f.length();
        long n = 0;
        File[] kids = f.listFiles();
        if(kids != null) for(File k : kids) n += sizeOf(k);
        return n;
    }

    private void buildWorlds(Context c) {
        LinearLayout autoRow = card(c);
        TextView label = ForkUi.text(c, getString(R.string.fork_hub_auto_backup), 14, true);
        autoRow.addView(label, ForkUi.weight(1f));
        SwitchCompat sw = new SwitchCompat(c);
        sw.setChecked(WorldBackup.enabled());
        sw.setOnCheckedChangeListener((b, on) -> WorldBackup.setEnabled(on));
        autoRow.addView(sw);
        mBody.addView(autoRow);

        File[] worlds = new File(mGameDir, "saves").listFiles(File::isDirectory);
        if(worlds == null || worlds.length == 0) {
            mBody.addView(muted(c, getString(R.string.fork_no_worlds)));
            return;
        }
        Arrays.sort(worlds, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for(File world : worlds) {
            LinearLayout card = card(c);
            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView name = ForkUi.text(c, world.getName(), 14, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            String safe = world.getName().replaceAll("[^A-Za-z0-9._-]", "_");
            File[] copies = new File(mGameDir, "backups").listFiles((d, n) -> n.startsWith(safe + "-") && n.endsWith(".zip"));
            TextView info = ForkUi.text(c, Formatter.formatShortFileSize(c, sizeOf(world)) + " · "
                    + DateUtils.getRelativeTimeSpanString(world.lastModified(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    + " · " + getString(R.string.fork_hub_backups_n, copies == null ? 0 : copies.length), 11, false);
            info.setTextColor(0xFFAAAAAA);
            texts.addView(name);
            texts.addView(info);
            card.addView(texts, ForkUi.weight(1f));
            Button save = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_backup), false));
            save.setOnClickListener(v -> {
                save.setEnabled(false);
                new Thread(() -> {
                    String msg;
                    try {
                        WorldBackup.backup(mGameDir, world, true);
                        msg = getString(R.string.fork_hub_backup_done, world.getName());
                    } catch(Exception e) {
                        msg = getString(R.string.fork_op_error, String.valueOf(e.getMessage()));
                    }
                    final String text = msg;
                    Tools.runOnUiThread(() -> { toast(text); select(TAB_WORLDS); });
                }).start();
            });
            card.addView(save);
            mBody.addView(card);
        }
    }

    // Logs

    private void buildLogs(Context c) {
        File log = new File(mGameDir, "logs/latest.log");
        if(!log.isFile()) {
            mBody.addView(muted(c, getString(R.string.fork_hub_no_logs)));
            return;
        }
        final String text = tail(log, 250);
        LinearLayout bar = ForkUi.row(c);
        Button copy = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_copy), true));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm != null) cm.setPrimaryClip(ClipData.newPlainText("latest.log", text));
            toast(getString(R.string.fork_hub_copied));
        });
        bar.addView(copy);
        mBody.addView(bar);
        TextView t = new TextView(c);
        t.setText(text);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextSize(11);
        t.setTextColor(0xFFD0D0D0);
        t.setTextIsSelectable(true);
        int p = ForkUi.dp(c, 10);
        t.setPadding(p, p, p, p);
        t.setBackground(ForkUi.box(c, 0xFF101114, 0x22FFFFFF, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = ForkUi.dp(c, 8);
        mBody.addView(t, lp);
        File[] crashes = new File(mGameDir, "crash-reports").listFiles();
        if(crashes != null && crashes.length > 0) mBody.addView(muted(c, getString(R.string.fork_hub_crashes_n, crashes.length)));
    }

    private static String tail(File file, int lines) {
        Deque<String> last = new ArrayDeque<>();
        try(BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = in.readLine()) != null) {
                last.addLast(line);
                if(last.size() > lines) last.removeFirst();
            }
        } catch(Exception e) {
            return String.valueOf(e.getMessage());
        }
        StringBuilder sb = new StringBuilder();
        for(String l : last) sb.append(l).append('\n');
        return sb.toString();
    }

    // Look

    private void buildLook(Context c) {
        final String name = String.valueOf(mInstance.name);
        LinearLayout colorRow = card(c);
        int current = InstanceLook.color(name);
        View swatch = new View(c);
        swatch.setBackground(ForkUi.box(c, current == 0 ? AccentColor.get() : current, Color.WHITE, 1));
        colorRow.addView(swatch, new LinearLayout.LayoutParams(ForkUi.dp(c, 28), ForkUi.dp(c, 28)));
        TextView label = ForkUi.text(c, getString(R.string.fork_hub_color), 14, true);
        label.setPadding(ForkUi.dp(c, 12), 0, 0, 0);
        colorRow.addView(label, ForkUi.weight(1f));
        Button pick = ForkUi.compact(ForkUi.button(c, getString(R.string.global_select), false));
        pick.setOnClickListener(v -> ColorPicker.show(c, R.string.fork_hub_color, current == 0 ? AccentColor.get() : current, AccentColor.get(),
                color -> { InstanceLook.setColor(name, color); select(TAB_LOOK); }));
        colorRow.addView(pick);
        Button reset = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_reset), false));
        reset.setOnClickListener(v -> { InstanceLook.setColor(name, 0); select(TAB_LOOK); });
        colorRow.addView(reset);
        mBody.addView(colorRow);

        LinearLayout imgRow = card(c);
        TextView il = ForkUi.text(c, getString(R.string.fork_hub_image), 14, true);
        imgRow.addView(il, ForkUi.weight(1f));
        Button choose = ForkUi.compact(ForkUi.button(c, getString(R.string.global_select), false));
        choose.setOnClickListener(v -> mImagePicker.launch("image/*"));
        imgRow.addView(choose);
        if(InstanceLook.hasImage(c, name)) {
            Button off = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_hub_reset), false));
            off.setOnClickListener(v -> { InstanceLook.clearImage(c, name); select(TAB_LOOK); });
            imgRow.addView(off);
        }
        mBody.addView(imgRow);
        android.graphics.Bitmap bg = InstanceLook.image(c, name);
        if(bg != null) {
            ImageView preview = new ImageView(c);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setImageBitmap(bg);
            if(UiStyle.rounded()) DivineTheme.roundCorners(preview, 14);
            mBody.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 150)));
        }
        mBody.addView(muted(c, getString(R.string.fork_hub_look_help)));
    }
}
