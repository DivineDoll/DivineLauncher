package com.divinedoll.launcher.utils;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.authenticator.accounts.Account;

/** Shows the head of an account (Microsoft, Ely.by or offline). Downloads it once and caches it. */
public final class AccountHead {
    private AccountHead() {}

    /** The head of the skin picked in the Skins tab, or null if none was picked. */
    private static Bitmap chosenSkinHead(android.content.Context c) {
        try {
            String name = com.divinedoll.launcher.prefs.LauncherPreferences.DEFAULT_PREF.getString("current_skin_file", null);
            if(name == null) return null;
            java.io.File f = new java.io.File(new java.io.File(c.getFilesDir(), "skin_library"), name);
            if(!f.exists()) return null;
            Bitmap skin = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
            if(skin == null) return null;
            Bitmap head = new com.divinedoll.launcher.authenticator.accounts.SkinHeadRenderer().render(100, skin);
            skin.recycle();
            return head;
        } catch (Exception e) {
            return null;
        }
    }

    public static void load(ImageView view, Account account) {
        if(account == null) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        Bitmap chosen = chosenSkinHead(view.getContext());
        if(chosen != null) {
            view.clearColorFilter();
            view.setImageBitmap(chosen);
            return;
        }
        Bitmap cached = account.getSkinFace();
        if(cached != null) {
            view.clearColorFilter();
            view.setImageBitmap(cached);
            return;
        }
        view.setImageResource(R.drawable.ic_dl_user);
        view.setColorFilter(AccentColor.get());
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            account.updateSkinFace();
            Bitmap face = account.getSkinFace();
            if(face != null) main.post(() -> {
                view.clearColorFilter();
                view.setImageBitmap(face);
            });
        }).start();
    }
}
