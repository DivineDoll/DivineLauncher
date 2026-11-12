package com.divinedoll.launcher.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.prefs.LauncherPreferences;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.ServerPing;
import com.divinedoll.launcher.utils.ThemeColors;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Favorite servers with their status. Joining is done from the game: the address can be copied from here. */
public class ServersFragment extends Fragment {
    public static final String TAG = "ServersFragment";
    private static final String PREF = "fav_servers";

    private LinearLayout mList;
    private int mLoadId;

    private static JsonArray load() {
        try {
            return new Gson().fromJson(LauncherPreferences.DEFAULT_PREF.getString(PREF, "[]"), JsonArray.class);
        } catch(Exception e) {
            return new JsonArray();
        }
    }

    private static void save(JsonArray list) {
        LauncherPreferences.DEFAULT_PREF.edit().putString(PREF, list.toString()).apply();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        Context c = requireContext();
        int pad = ForkUi.dp(c, 12);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());

        LinearLayout header = ForkUi.row(c);
        header.setPadding(pad, pad, pad, ForkUi.dp(c, 6));
        Button back = ForkUi.compact(ForkUi.button(c, "‹", false));
        back.setOnClickListener(v -> Tools.removeCurrentFragment(requireActivity()));
        header.addView(back);
        TextView title = ForkUi.text(c, getString(R.string.fork_servers_title), 18, true);
        title.setPadding(ForkUi.dp(c, 10), 0, 0, 0);
        header.addView(title, ForkUi.weight(1f));
        Button refresh = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_servers_refresh), false));
        refresh.setOnClickListener(v -> render());
        header.addView(refresh);
        root.addView(header);

        LinearLayout add = new LinearLayout(c);
        add.setOrientation(LinearLayout.HORIZONTAL);
        add.setGravity(Gravity.CENTER_VERTICAL);
        add.setPadding(pad, 0, pad, ForkUi.dp(c, 6));
        EditText name = field(c, R.string.fork_servers_name);
        EditText host = field(c, R.string.fork_servers_address);
        host.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        add.addView(name, new LinearLayout.LayoutParams(0, ForkUi.dp(c, 44), 1f));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0, ForkUi.dp(c, 44), 1.4f);
        hlp.setMarginStart(ForkUi.dp(c, 6));
        add.addView(host, hlp);
        Button plus = ForkUi.compact(ForkUi.button(c, "+", true));
        plus.setOnClickListener(v -> {
            String h = host.getText().toString().trim();
            if(h.isEmpty()) return;
            String n = name.getText().toString().trim();
            JsonArray list = load();
            JsonObject o = new JsonObject();
            o.addProperty("n", n.isEmpty() ? h : n);
            o.addProperty("h", h);
            list.add(o);
            save(list);
            name.setText("");
            host.setText("");
            render();
        });
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ForkUi.dp(c, 44));
        plp.setMarginStart(ForkUi.dp(c, 6));
        add.addView(plus, plp);
        root.addView(add);

        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        mList.setPadding(pad, ForkUi.dp(c, 4), pad, pad);
        scroll.addView(mList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        render();
        return root;
    }

    private EditText field(Context c, int hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(0xFF777777);
        e.setTextColor(0xFFFFFFFF);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setBackground(ForkUi.box(c, ThemeColors.sidebar(), 0x33FFFFFF, 1));
        e.setPadding(ForkUi.dp(c, 10), 0, ForkUi.dp(c, 10), 0);
        return e;
    }

    private void render() {
        if(!isAdded()) return;
        Context c = requireContext();
        mLoadId++;
        final int id = mLoadId;
        mList.removeAllViews();
        JsonArray list = load();
        if(list.size() == 0) {
            TextView empty = ForkUi.text(c, getString(R.string.fork_servers_empty), 13, false);
            empty.setTextColor(0xFFBBBBBB);
            mList.addView(empty);
            return;
        }
        for(int i = 0; i < list.size(); i++) {
            final int index = i;
            JsonObject o = list.get(i).getAsJsonObject();
            String address = o.get("h").getAsString();
            LinearLayout card = new LinearLayout(c);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(ForkUi.box(c, ThemeColors.sidebar(), 0x22FFFFFF, 1));
            int p = ForkUi.dp(c, 12);
            card.setPadding(p, p, p, p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = ForkUi.dp(c, 8);
            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView name = ForkUi.text(c, o.get("n").getAsString(), 15, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView addr = ForkUi.text(c, address, 11, false);
            addr.setTextColor(0xFFAAAAAA);
            TextView status = ForkUi.text(c, getString(R.string.fork_servers_checking), 12, false);
            status.setTextColor(0xFFBBBBBB);
            texts.addView(name);
            texts.addView(addr);
            texts.addView(status);
            card.addView(texts, ForkUi.weight(1f));
            Button copy = ForkUi.compact(ForkUi.button(c, getString(R.string.fork_servers_copy), false));
            copy.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                if(cm != null) cm.setPrimaryClip(ClipData.newPlainText("server", address));
                Toast.makeText(c, R.string.fork_servers_copied, Toast.LENGTH_SHORT).show();
            });
            card.addView(copy);
            Button del = ForkUi.compact(ForkUi.button(c, "✕", false));
            del.setOnClickListener(v -> {
                JsonArray now = load();
                if(index < now.size()) now.remove(index);
                save(now);
                render();
            });
            card.addView(del);
            mList.addView(card, lp);
            new Thread(() -> {
                ServerPing.Result r = ServerPing.ping(address, 4000);
                Tools.runOnUiThread(() -> {
                    if(!isAdded() || id != mLoadId) return;
                    if(r.online) {
                        status.setText(getString(R.string.fork_servers_online, r.players, r.max, r.latencyMs) + (r.motd.isEmpty() ? "" : " · " + r.motd.replaceAll("§.", "").replace('\n', ' ')));
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
