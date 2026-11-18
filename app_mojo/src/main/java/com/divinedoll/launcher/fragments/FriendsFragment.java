package com.divinedoll.launcher.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.social.Social;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.BottomSheet;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.EmptyState;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.ThemeColors;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Friends list: requests, friends and a way into the chat. Needs the Firebase setup (see docs/FIREBASE_SETUP.md). */
public class FriendsFragment extends Fragment {
    public static final String TAG = "FriendsFragment";
    private static final String GUIDE_URL = "https://github.com/DivineDoll/DivineLauncher/blob/main/docs/FIREBASE_SETUP.md";

    private LinearLayout mList;
    private LinearLayout mHeader;
    private ListenerRegistration mRequestsReg, mFriendsReg;
    private final List<DocumentSnapshot> mRequests = new ArrayList<>();
    private final List<DocumentSnapshot> mFriends = new ArrayList<>();

    public static void open(FragmentActivity act) {
        Tools.swapFragment(act, FriendsFragment.class, TAG, null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());
        mHeader = ForkUi.screenHeader(c, requireActivity(), getString(R.string.fork_friends_title),
                R.drawable.ic_dl_plus, v -> promptAddFriend());
        mHeader.getChildAt(2).setVisibility(View.GONE); // the add chip stays hidden until the account is ready
        root.addView(mHeader);
        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        mList.setPadding(ForkUi.dp(c, 16), 0, ForkUi.dp(c, 16), ForkUi.dp(c, 24));
        scroll.addView(mList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context c = requireContext();
        if (!Social.configured(c)) {
            mList.addView(EmptyState.view(c, R.drawable.ic_dl_user, getString(R.string.fork_friends_setup_title),
                    getString(R.string.fork_friends_setup_text), getString(R.string.fork_friends_setup_action),
                    v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GUIDE_URL)))));
            return;
        }
        Social.signIn(new Social.Done() {
            @Override public void ok() { if (isAdded()) ensureName(); }
            @Override public void fail(String m) { if (isAdded()) toast(m); }
        });
    }

    @Override
    public void onDestroyView() {
        if (mRequestsReg != null) mRequestsReg.remove();
        if (mFriendsReg != null) mFriendsReg.remove();
        mRequestsReg = mFriendsReg = null;
        super.onDestroyView();
    }

    private void toast(String text) {
        Context c = getContext();
        if (c != null) Toast.makeText(c, text, Toast.LENGTH_LONG).show();
    }

    private String explain(String code) {
        switch (code) {
            case "taken": return getString(R.string.fork_friends_name_taken);
            case "notfound": return getString(R.string.fork_friends_notfound);
            case "self": return getString(R.string.fork_friends_self);
            case "invalid": return getString(R.string.fork_friends_name_invalid);
            default: return code;
        }
    }

    private void ensureName() {
        if (Social.myName() != null && Social.uid() != null) { startListening(); return; }
        String suggestion = "";
        try {
            suggestion = Social.sanitize(com.divinedoll.launcher.authenticator.accounts.Accounts.getCurrent().username);
        } catch (Exception ignored) {
        }
        promptName(suggestion);
    }

    private void promptName(String suggestion) {
        Context c = requireContext();
        EditText input = new EditText(c);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        input.setHint(R.string.fork_friends_name_hint);
        input.setText(suggestion);
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_friends_name_title)
                .setMessage(R.string.fork_friends_name_text)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (d, w) -> Social.claimName(input.getText().toString(), new Social.Done() {
                    @Override public void ok() { if (isAdded()) startListening(); }
                    @Override public void fail(String m) {
                        if (!isAdded()) return;
                        toast(explain(m));
                        promptName(input.getText().toString());
                    }
                }))
                .setNegativeButton(android.R.string.cancel, (d, w) -> Tools.backToMainMenu(requireActivity()))
                .show();
    }

    private void startListening() {
        if (mHeader != null) mHeader.getChildAt(2).setVisibility(View.VISIBLE);
        String me = Social.uid();
        mRequestsReg = Social.db().collection("requests").whereEqualTo("to", me).addSnapshotListener((snap, e) -> {
            if (e != null || snap == null || !isAdded()) return;
            mRequests.clear();
            mRequests.addAll(snap.getDocuments());
            render();
        });
        mFriendsReg = Social.db().collection("friends").document(me).collection("list").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null || !isAdded()) return;
            mFriends.clear();
            mFriends.addAll(snap.getDocuments());
            render();
        });
        render();
    }

    private void render() {
        Context c = getContext();
        if (c == null || mList == null) return;
        mList.removeAllViews();

        TextView you = ForkUi.text(c, getString(R.string.fork_friends_you, String.valueOf(Social.myName())), 13, false);
        you.setTextColor(AccentColor.get());
        you.setPadding(ForkUi.dp(c, 4), 0, 0, ForkUi.dp(c, 10));
        mList.addView(you);

        if (!mRequests.isEmpty()) {
            mList.addView(section(c, getString(R.string.fork_friends_requests)));
            for (DocumentSnapshot r : mRequests) {
                final String fromUid = r.getString("from");
                final String fromName = r.getString("fromName");
                if (fromUid == null || fromName == null) continue;
                LinearLayout actions = new LinearLayout(c);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.addView(ForkUi.iconChip(c, R.drawable.ic_dl_plus, true, v ->
                        Social.accept(fromUid, fromName, quiet())), new LinearLayout.LayoutParams(ForkUi.dp(c, 34), ForkUi.dp(c, 34)));
                View decline = ForkUi.iconChip(c, R.drawable.ic_dl_back, false, v ->
                        Social.declineOrCancel(r.getId(), quiet()));
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ForkUi.dp(c, 34), ForkUi.dp(c, 34));
                dlp.leftMargin = ForkUi.dp(c, 8);
                actions.addView(decline, dlp);
                mList.addView(row(c, fromName, getString(R.string.fork_friends_wants), actions, null, null));
            }
        }

        mList.addView(section(c, getString(R.string.fork_friends_list)));
        if (mFriends.isEmpty()) {
            TextView empty = ForkUi.text(c, getString(R.string.fork_friends_empty), 14, false);
            empty.setTextColor(0xFF9AA1AD);
            empty.setPadding(ForkUi.dp(c, 4), ForkUi.dp(c, 6), 0, ForkUi.dp(c, 6));
            mList.addView(empty);
        }
        for (DocumentSnapshot f : mFriends) {
            final String uid = f.getId();
            final String name = String.valueOf(f.getString("name"));
            mList.addView(row(c, name, getString(R.string.fork_friends_tap_chat), null,
                    v -> ChatFragment.open(requireActivity(), uid, name),
                    v -> {
                        BottomSheet.menu(c, name, new int[]{R.drawable.ic_dl_back}, new CharSequence[]{getString(R.string.fork_friends_remove)},
                                new boolean[]{true}, i -> Social.removeFriend(uid, quiet()));
                        return true;
                    }));
        }

        TextView delete = ForkUi.text(c, getString(R.string.fork_friends_delete), 13, false);
        delete.setTextColor(0xFFE5484D);
        delete.setPadding(ForkUi.dp(c, 4), ForkUi.dp(c, 24), 0, 0);
        delete.setOnClickListener(v -> new AlertDialog.Builder(c)
                .setTitle(R.string.fork_friends_delete)
                .setMessage(R.string.fork_friends_delete_text)
                .setPositiveButton(android.R.string.ok, (d, w) -> Social.deleteAccount(new Social.Done() {
                    @Override public void ok() { if (isAdded()) Tools.backToMainMenu(requireActivity()); }
                    @Override public void fail(String m) { toast(m); }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        mList.addView(delete);
    }

    private Social.Done quiet() {
        return new Social.Done() {
            @Override public void ok() {}
            @Override public void fail(String m) { toast(explain(m)); }
        };
    }

    private void promptAddFriend() {
        Context c = requireContext();
        EditText input = new EditText(c);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        input.setHint(R.string.fork_friends_add_hint);
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_friends_add)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> Social.sendRequest(input.getText().toString(), new Social.Done() {
                    @Override public void ok() { toast(getString(R.string.fork_friends_sent)); }
                    @Override public void fail(String m) { toast(explain(m)); }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private TextView section(Context c, String title) {
        TextView t = ForkUi.text(c, title.toUpperCase(), 12, true);
        t.setTextColor(AccentColor.get());
        t.setPadding(ForkUi.dp(c, 4), ForkUi.dp(c, 14), 0, ForkUi.dp(c, 6));
        return t;
    }

    private View row(Context c, String name, String sub, @Nullable View right, @Nullable View.OnClickListener click,
                     @Nullable View.OnLongClickListener longClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 10), ForkUi.dp(c, 12), ForkUi.dp(c, 10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeColors.sidebar());
        bg.setCornerRadius(ForkUi.dp(c, 16));
        bg.setStroke(ForkUi.dp(c, 1), 0x14FFFFFF);
        row.setBackground(bg);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = ForkUi.dp(c, 8);
        row.setLayoutParams(rlp);

        TextView avatar = ForkUi.text(c, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), 16, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(DivineTheme.onColor(AccentColor.get()));
        GradientDrawable ag = new GradientDrawable();
        ag.setShape(GradientDrawable.OVAL);
        ag.setColor(AccentColor.get());
        avatar.setBackground(ag);
        row.addView(avatar, new LinearLayout.LayoutParams(ForkUi.dp(c, 40), ForkUi.dp(c, 40)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(ForkUi.dp(c, 12), 0, ForkUi.dp(c, 8), 0);
        texts.addView(ForkUi.text(c, name, 15, true));
        TextView s = ForkUi.text(c, sub, 12, false);
        s.setTextColor(0xFF9AA1AD);
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (right != null) row.addView(right);
        if (click != null) { row.setClickable(true); row.setOnClickListener(click); }
        if (longClick != null) row.setOnLongClickListener(longClick);
        return row;
    }
}
