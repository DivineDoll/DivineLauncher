package com.divinedoll.launcher.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
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

import com.divinedoll.launcher.MojoApplication;
import com.divinedoll.launcher.R;
import com.divinedoll.launcher.authenticator.accounts.Account;
import com.divinedoll.launcher.authenticator.accounts.Accounts;
import com.divinedoll.launcher.lan.TerracottaManager;

/** LAN room (host or join) from inside the game, the same engine as the LAN screen. */
public final class LanQuickDialog implements TerracottaManager.Listener {
    private final Activity mActivity;
    private TextView mStatus, mCode, mAddress;
    private Button mCopyCode, mCopyAddress, mStop;
    private AlertDialog mDialog;

    private LanQuickDialog(Activity a) { mActivity = a; }

    public static void show(Activity a) {
        new LanQuickDialog(a).open();
    }

    private int dp(int v) { return ForkUi.dp(mActivity, v); }

    private Button button(int text, boolean primary) {
        Button b = ForkUi.button(mActivity, mActivity.getString(text), primary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView text(CharSequence t, int sp, boolean heading) {
        TextView v = ForkUi.text(mActivity, t, sp, heading);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        v.setLayoutParams(lp);
        return v;
    }

    private void open() {
        Context c = mActivity;
        ScrollView scroll = new ScrollView(c);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(ThemeColors.panel());
        scroll.addView(root);

        TextView title = text(c.getString(R.string.fork_lan_title), 18, true);
        title.setTextColor(AccentColor.light());
        root.addView(title);
        root.addView(text(c.getString(R.string.fork_lan_host_steps), 12, false));
        Button create = button(R.string.fork_lan_host_create, true);
        root.addView(create);
        mStatus = text("", 13, false);
        root.addView(mStatus);
        mCode = text("", 24, true);
        mCode.setGravity(Gravity.CENTER);
        mCode.setTextIsSelectable(true);
        mCode.setBackground(ForkUi.box(c, 0xFF101114, 0x33FFFFFF, 1));
        mCode.setPadding(dp(8), dp(8), dp(8), dp(8));
        mCode.setVisibility(View.GONE);
        root.addView(mCode);
        mCopyCode = button(R.string.fork_lan_copy_code, false);
        root.addView(mCopyCode);

        TextView guestTitle = text(c.getString(R.string.fork_lan_guest_tab), 14, true);
        guestTitle.setTextColor(AccentColor.light());
        ((LinearLayout.LayoutParams) guestTitle.getLayoutParams()).topMargin = dp(16);
        root.addView(guestTitle);
        EditText input = new EditText(c);
        input.setHint(R.string.fork_lan_code_hint);
        input.setHintTextColor(0xFF777777);
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setBackground(ForkUi.box(c, 0xFF101114, 0x55FFFFFF, 1));
        input.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        ilp.topMargin = dp(8);
        root.addView(input, ilp);
        Button join = button(R.string.fork_lan_guest_join, true);
        root.addView(join);
        mAddress = text("", 20, true);
        mAddress.setGravity(Gravity.CENTER);
        mAddress.setTextIsSelectable(true);
        mAddress.setBackground(ForkUi.box(c, 0xFF101114, 0x33FFFFFF, 1));
        mAddress.setPadding(dp(8), dp(8), dp(8), dp(8));
        mAddress.setVisibility(View.GONE);
        root.addView(mAddress);
        mCopyAddress = button(R.string.fork_lan_copy_address, false);
        root.addView(mCopyAddress);
        mStop = button(R.string.fork_lan_stop, false);
        root.addView(mStop);

        create.setOnClickListener(v -> run(() -> TerracottaManager.get().startHost(playerName())));
        join.setOnClickListener(v -> {
            String code = input.getText().toString().trim();
            if(code.isEmpty()) { Toast.makeText(c, R.string.fork_lan_code_empty, Toast.LENGTH_LONG).show(); return; }
            run(() -> {
                if(!TerracottaManager.get().startGuest(code, playerName()))
                    throw new IllegalArgumentException(c.getString(R.string.fork_lan_code_invalid));
            });
        });
        mStop.setOnClickListener(v -> TerracottaManager.get().stop());
        mCopyCode.setOnClickListener(v -> copy(mCode.getText().toString(), R.string.fork_lan_copied_code));
        mCopyAddress.setOnClickListener(v -> copy(mAddress.getText().toString(), R.string.fork_lan_copied_address));

        DivineTheme.styleTree(root);
        mDialog = new AlertDialog.Builder(c).setView(scroll).setNegativeButton(android.R.string.cancel, null).create();
        mDialog.setOnDismissListener(d -> TerracottaManager.get().removeListener(this));
        TerracottaManager.get().addListener(this);
        render(TerracottaManager.get().getState());
        mDialog.show();
    }

    private String playerName() {
        Account account = Accounts.getCurrent();
        return account != null && account.username != null ? account.username : "Jugador";
    }

    private void copy(String value, int toast) {
        if(value.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("lan", value));
        Toast.makeText(mActivity, toast, Toast.LENGTH_SHORT).show();
    }

    private interface Call { void run() throws Exception; }

    private void run(Call call) {
        if(!TerracottaManager.isSupported()) {
            Toast.makeText(mActivity, R.string.fork_lan_needs_android8, Toast.LENGTH_LONG).show();
            return;
        }
        Context app = mActivity.getApplicationContext();
        MojoApplication.sExecutorService.execute(() -> {
            try {
                TerracottaManager manager = TerracottaManager.get();
                manager.ensureInitialized(app);
                TerracottaManager.State state = manager.getState();
                if(state != null && !state.isWaiting()) {
                    manager.stop();
                    Thread.sleep(800);
                }
                call.run();
            } catch(Throwable t) {
                String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                mActivity.runOnUiThread(() -> Toast.makeText(app, app.getString(R.string.fork_lan_error, message), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onStateChanged(TerracottaManager.State state) {
        mActivity.runOnUiThread(() -> render(state));
    }

    private void render(TerracottaManager.State state) {
        if(mStatus == null || mDialog == null) return;
        boolean error = state != null && state.name.equals("exception");
        boolean hostOk = state != null && state.name.equals("host-ok") && state.room != null;
        boolean guestOk = state != null && state.name.equals("guest-ok") && state.url != null;
        boolean active = state != null && !state.isWaiting();
        mStatus.setText(active || error ? TerracottaManager.describe(mActivity, state) : "");
        mStatus.setTextColor(error ? 0xFFE06060 : hostOk || guestOk ? 0xFF7CD65A : Color.WHITE);
        mCode.setText(hostOk ? state.room : "");
        mCode.setVisibility(hostOk ? View.VISIBLE : View.GONE);
        mCopyCode.setVisibility(hostOk ? View.VISIBLE : View.GONE);
        mAddress.setText(guestOk ? state.url : "");
        mAddress.setVisibility(guestOk ? View.VISIBLE : View.GONE);
        mCopyAddress.setVisibility(guestOk ? View.VISIBLE : View.GONE);
        mStop.setVisibility(active ? View.VISIBLE : View.GONE);
    }
}
