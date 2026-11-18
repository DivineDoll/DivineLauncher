package com.divinedoll.launcher.fragments;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.social.Social;
import com.divinedoll.launcher.utils.AccentColor;
import com.divinedoll.launcher.utils.BottomSheet;
import com.divinedoll.launcher.utils.DivineTheme;
import com.divinedoll.launcher.utils.ForkUi;
import com.divinedoll.launcher.utils.ThemeColors;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

/** One-to-one chat with a friend. Messages live in Firestore and update in real time. */
public class ChatFragment extends Fragment {
    public static final String TAG = "ChatFragment";
    private static final String ARG_UID = "uid", ARG_NAME = "name";

    private String mFriendUid, mFriendName;
    private LinearLayout mMessages;
    private ScrollView mScroll;
    private ListenerRegistration mReg;
    private int mOldSoftInput = -1;

    public static void open(FragmentActivity act, String friendUid, String friendName) {
        Bundle b = new Bundle();
        b.putString(ARG_UID, friendUid);
        b.putString(ARG_NAME, friendName);
        Tools.swapFragment(act, ChatFragment.class, TAG, b);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        Bundle args = getArguments();
        mFriendUid = args == null ? null : args.getString(ARG_UID);
        mFriendName = args == null ? "" : args.getString(ARG_NAME, "");

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeColors.background());
        LinearLayout header = ForkUi.screenHeader(c, requireActivity(), mFriendName, 0, null);
        header.getChildAt(0).setOnClickListener(v -> FriendsFragment.open(requireActivity()));
        root.addView(header);

        mScroll = new ScrollView(c);
        mMessages = new LinearLayout(c);
        mMessages.setOrientation(LinearLayout.VERTICAL);
        mMessages.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 4), ForkUi.dp(c, 12), ForkUi.dp(c, 8));
        mScroll.addView(mMessages);
        root.addView(mScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 8), ForkUi.dp(c, 12), ForkUi.dp(c, 12));
        EditText input = new EditText(c);
        input.setHint(R.string.fork_chat_hint);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        input.setMaxLines(4);
        input.setTextColor(ThemeColors.text());
        input.setHintTextColor(0xFF9AA1AD);
        GradientDrawable ibg = new GradientDrawable();
        ibg.setColor(ThemeColors.sidebar());
        ibg.setCornerRadius(ForkUi.dp(c, 20));
        ibg.setStroke(ForkUi.dp(c, 1), 0x22FFFFFF);
        input.setBackground(ibg);
        input.setPadding(ForkUi.dp(c, 16), ForkUi.dp(c, 10), ForkUi.dp(c, 16), ForkUi.dp(c, 10));
        bar.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View send = ForkUi.iconChip(c, R.drawable.ic_dl_play, true, v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty() || mFriendUid == null) return;
            input.setText("");
            Social.send(mFriendUid, text, new Social.Done() {
                @Override public void ok() {}
                @Override public void fail(String m) { toast(m); }
            });
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ForkUi.dp(c, 40), ForkUi.dp(c, 40));
        slp.leftMargin = ForkUi.dp(c, 8);
        bar.addView(send, slp);
        root.addView(bar);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mFriendUid == null || Social.uid() == null) return;
        mReg = Social.db().collection("chats").document(Social.chatId(Social.uid(), mFriendUid))
                .collection("messages").orderBy("ts", Query.Direction.ASCENDING).limitToLast(100)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { toast(String.valueOf(e.getMessage())); return; }
                    if (snap == null || !isAdded()) return;
                    render(snap.getDocuments());
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowManager.LayoutParams lp = requireActivity().getWindow().getAttributes();
        mOldSoftInput = lp.softInputMode;
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onPause() {
        if (mOldSoftInput != -1 && getActivity() != null) getActivity().getWindow().setSoftInputMode(mOldSoftInput);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mReg != null) mReg.remove();
        mReg = null;
        super.onDestroyView();
    }

    private void toast(String text) {
        Context c = getContext();
        if (c != null) Toast.makeText(c, text, Toast.LENGTH_LONG).show();
    }

    private void render(java.util.List<DocumentSnapshot> docs) {
        Context c = getContext();
        if (c == null) return;
        mMessages.removeAllViews();
        String me = Social.uid();
        int accent = AccentColor.get();
        int maxWidth = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.72f);
        for (DocumentSnapshot d : docs) {
            final String text = d.getString("text");
            final String from = d.getString("from");
            if (text == null || from == null) continue;
            boolean mine = from.equals(me);
            TextView bubble = ForkUi.text(c, text, 14, false);
            bubble.setMaxWidth(maxWidth);
            bubble.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 8), ForkUi.dp(c, 12), ForkUi.dp(c, 8));
            bubble.setTextColor(mine ? DivineTheme.onColor(accent) : ThemeColors.text());
            GradientDrawable g = new GradientDrawable();
            g.setColor(mine ? accent : ThemeColors.sidebar());
            g.setCornerRadius(ForkUi.dp(c, 16));
            bubble.setBackground(g);
            if (!mine) {
                bubble.setOnLongClickListener(v -> {
                    BottomSheet.menu(c, mFriendName, new int[]{R.drawable.ic_dl_more},
                            new CharSequence[]{getString(R.string.fork_chat_report)}, new boolean[]{true}, i ->
                                    Social.report(mFriendUid, text, new Social.Done() {
                                        @Override public void ok() { toast(getString(R.string.fork_chat_reported)); }
                                        @Override public void fail(String m) { toast(m); }
                                    }));
                    return true;
                });
            }
            LinearLayout wrap = new LinearLayout(c);
            wrap.setGravity(mine ? Gravity.END : Gravity.START);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wrap.addView(bubble, blp);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wlp.topMargin = ForkUi.dp(c, 4);
            mMessages.addView(wrap, wlp);
        }
        mScroll.post(() -> mScroll.fullScroll(View.FOCUS_DOWN));
    }
}
