package com.divinedoll.launcher.fragments;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;
import com.divinedoll.launcher.prefs.LauncherPreferences;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.BottomSheet;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.EmptyState;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.QuickJoin;
import com.divinedoll.launcher.utils.ServerPing;
import com.divinedoll.launcher.utils.Skeleton;
import com.divinedoll.launcher.utils.ThemeColors;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Servers saved by the player. Each one belongs to an installation: tapping it opens the game with
 * that installation and joins the server straight away.
 */
public class ServersFragment extends Fragment {
    public static final String TAG = "ServersFragment";
    private static final String PREF = "fav_servers";

    private LinearLayout mList;
    private int mLoadId;

    /** One saved server: name, address and the installation it belongs to (empty = the selected one). */
    public static final class Entry {
        public final String name, address, instance;

        Entry(String name, String address, String instance) {
            this.name = name;
            this.address = address;
            this.instance = instance;
        }
    }

    private static JsonArray load() {
        try {
            JsonArray a = new Gson().fromJson(LauncherPreferences.DEFAULT_PREF.getString(PREF, "[]"), JsonArray.class);
            return a == null ? new JsonArray() : a;
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    private static void save(JsonArray list) {
        LauncherPreferences.DEFAULT_PREF.edit().putString(PREF, list.toString()).apply();
    }

    /** All saved servers, used by the home screen for quick joining. */
    public static List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        JsonArray list = load();
        for (int i = 0; i < list.size(); i++) {
            JsonObject o = list.get(i).getAsJsonObject();
            out.add(new Entry(str(o, "n"), str(o, "h"), str(o, "i")));
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    /** Joins a saved server from anywhere. */
    public static void join(androidx.fragment.app.FragmentActivity act, Entry e) {
        if (!QuickJoin.join(act, e.instance, e.address)) {
            Toast.makeText(act, R.string.fork_servers_join_fail, Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        Context c = requireContext();
        int pad = ForkUi.dp(c, 14);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());
        root.addView(ForkUi.screenHeader(c, requireActivity(), getString(R.string.fork_servers_title),
                R.drawable.ic_dl_plus, v -> showEditor(-1)));

        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        mList.setPadding(pad, ForkUi.dp(c, 2), pad, pad);
        scroll.addView(mList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        render();
        return root;
    }

    private EditText field(Context c, int hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(0xFF777777);
        e.setTextColor(0xFFFFFFFF);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setText(value);
        e.setBackground(ForkUi.box(c, ThemeColors.panel(), 0x33FFFFFF, 1));
        e.setPadding(ForkUi.dp(c, 12), 0, ForkUi.dp(c, 12), 0);
        return e;
    }

    private static List<String> instanceNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Instance i : Instances.loadAllInstances()) if (i.name != null) names.add(i.name);
        } catch (Exception ignored) {
        }
        return names;
    }

    /** Sheet to add (index -1) or edit a server. */
    private void showEditor(int index) {
        if (!isAdded()) return;
        Context c = requireContext();
        JsonArray list = load();
        JsonObject current = index >= 0 && index < list.size() ? list.get(index).getAsJsonObject() : null;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView title = ForkUi.text(c, getString(current == null ? R.string.fork_servers_add : R.string.fork_servers_edit), 16, true);
        title.setPadding(ForkUi.dp(c, 4), 0, 0, ForkUi.dp(c, 10));
        box.addView(title);
        EditText name = field(c, R.string.fork_servers_name, current == null ? "" : str(current, "n"));
        EditText host = field(c, R.string.fork_servers_address, current == null ? "" : str(current, "h"));
        host.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 46));
        flp.bottomMargin = ForkUi.dp(c, 8);
        box.addView(name, flp);
        box.addView(host, flp);

        final String[] chosen = {current == null ? "" : str(current, "i")};
        if (chosen[0].isEmpty()) {
            Instance selected = Instances.loadSelectedInstance();
            if (selected != null && selected.name != null) chosen[0] = selected.name;
        }
        TextView instance = ForkUi.text(c, getString(R.string.fork_servers_instance) + ": " + chosen[0], 14, false);
        instance.setPadding(ForkUi.dp(c, 12), 0, ForkUi.dp(c, 12), 0);
        instance.setGravity(Gravity.CENTER_VERTICAL);
        instance.setBackground(ForkUi.box(c, ThemeColors.panel(), 0x33FFFFFF, 1));
        instance.setOnClickListener(v -> {
            List<String> names = instanceNames();
            if (names.isEmpty()) return;
            BottomSheet.menu(c, getString(R.string.fork_servers_instance), null, names.toArray(new CharSequence[0]), null, i -> {
                chosen[0] = names.get(i);
                instance.setText(getString(R.string.fork_servers_instance) + ": " + chosen[0]);
            });
        });
        box.addView(instance, flp);

        final Dialog[] holder = new Dialog[1];
        TextView saveButton = ForkUi.text(c, getString(R.string.fork_servers_save), 14, true);
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setTextColor(DivineTheme.onColor(AccentColor.get()));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setCornerRadius(ForkUi.dp(c, 30));
        sbg.setColor(AccentColor.get());
        saveButton.setBackground(sbg);
        saveButton.setOnClickListener(v -> {
            String h = host.getText().toString().trim();
            if (h.isEmpty()) return;
            String n = name.getText().toString().trim();
            JsonArray now = load();
            JsonObject o = new JsonObject();
            o.addProperty("n", n.isEmpty() ? h : n);
            o.addProperty("h", h);
            o.addProperty("i", chosen[0]);
            if (index >= 0 && index < now.size()) now.set(index, o); else now.add(o);
            save(now);
            if (holder[0] != null) holder[0].dismiss();
            render();
        });
        box.addView(saveButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 46)));
        holder[0] = BottomSheet.show(c, box);
    }

    private void showMenu(int index, String address) {
        Context c = requireContext();
        BottomSheet.menu(c, null,
                new int[]{R.drawable.ic_dl_cube, R.drawable.ic_dl_plus, 0},
                new CharSequence[]{getString(R.string.fork_servers_edit), getString(R.string.fork_servers_copy), getString(R.string.fork_servers_delete)},
                new boolean[]{false, false, true}, pick -> {
                    if (pick == 0) {
                        showEditor(index);
                    } else if (pick == 1) {
                        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("server", address));
                        Toast.makeText(c, R.string.fork_servers_copied, Toast.LENGTH_SHORT).show();
                    } else {
                        JsonArray now = load();
                        if (index < now.size()) now.remove(index);
                        save(now);
                        render();
                    }
                });
    }

    private void render() {
        if (!isAdded()) return;
        Context c = requireContext();
        mLoadId++;
        final int id = mLoadId;
        mList.removeAllViews();
        JsonArray list = load();
        if (list.size() == 0) {
            mList.addView(EmptyState.view(c, R.drawable.ic_dl_globe, getString(R.string.fork_servers_empty_title),
                    getString(R.string.fork_servers_empty), getString(R.string.fork_servers_add), v -> showEditor(-1)));
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            JsonObject o = list.get(i).getAsJsonObject();
            final Entry entry = new Entry(str(o, "n"), str(o, "h"), str(o, "i"));
            LinearLayout card = new LinearLayout(c);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(ForkUi.dp(c, 16));
            bg.setColor(ThemeColors.panel());
            bg.setStroke(ForkUi.dp(c, 1), 0x1FFFFFFF);
            card.setBackground(bg);
            int p = ForkUi.dp(c, 12);
            card.setPadding(p, p, p, p);
            card.setClickable(true);
            card.setOnClickListener(v -> join(requireActivity(), entry));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = ForkUi.dp(c, 8);

            FrameLayout iconBox = new FrameLayout(c);
            GradientDrawable ibg = new GradientDrawable();
            ibg.setCornerRadius(ForkUi.dp(c, 11));
            ibg.setColor(AccentColor.get());
            iconBox.setBackground(ibg);
            ImageView icon = new ImageView(c);
            icon.setImageResource(R.drawable.ic_dl_globe);
            icon.setColorFilter(DivineTheme.onColor(AccentColor.get()));
            iconBox.addView(icon, new FrameLayout.LayoutParams(ForkUi.dp(c, 20), ForkUi.dp(c, 20), Gravity.CENTER));
            card.addView(iconBox, new LinearLayout.LayoutParams(ForkUi.dp(c, 38), ForkUi.dp(c, 38)));

            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(ForkUi.dp(c, 12), 0, ForkUi.dp(c, 8), 0);
            TextView name = ForkUi.text(c, entry.name, 14, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView addr = ForkUi.text(c, entry.address + (entry.instance.isEmpty() ? "" : "  ·  " + entry.instance), 11, false);
            addr.setTextColor(0xFF9AA1AD);
            addr.setSingleLine(true);
            addr.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView status = ForkUi.text(c, getString(R.string.fork_servers_checking), 12, false);
            status.setTextColor(0xFF9AA1AD);
            status.setSingleLine(true);
            status.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(name);
            texts.addView(addr);
            texts.addView(status);
            card.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView join = ForkUi.text(c, getString(R.string.fork_servers_join), 12, true);
            join.setTextColor(DivineTheme.onColor(AccentColor.get()));
            GradientDrawable jbg = new GradientDrawable();
            jbg.setCornerRadius(ForkUi.dp(c, 30));
            jbg.setColor(AccentColor.get());
            join.setBackground(jbg);
            join.setPadding(ForkUi.dp(c, 14), ForkUi.dp(c, 7), ForkUi.dp(c, 14), ForkUi.dp(c, 7));
            card.addView(join);
            View more = ForkUi.iconChip(c, R.drawable.ic_dl_more, false, v -> showMenu(index, entry.address));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ForkUi.dp(c, 30), ForkUi.dp(c, 30));
            mlp.setMarginStart(ForkUi.dp(c, 8));
            card.addView(more, mlp);
            mList.addView(card, lp);

            new Thread(() -> {
                ServerPing.Result r = ServerPing.ping(entry.address, 4000);
                Tools.runOnUiThread(() -> {
                    if (!isAdded() || id != mLoadId) return;
                    if (r.online) {
                        status.setText(getString(R.string.fork_servers_online, r.players, r.max, r.latencyMs)
                                + (r.motd.isEmpty() ? "" : " · " + r.motd.replaceAll("§.", "").replace('\n', ' ')));
                        status.setTextColor(0xFF7CD65A);
                    } else {
                        status.setText(R.string.fork_servers_offline);
                        status.setTextColor(0xFFE06060);
                    }
                });
            }).start();
        }
        DivineTheme.styleTree(mList);
    }
}
