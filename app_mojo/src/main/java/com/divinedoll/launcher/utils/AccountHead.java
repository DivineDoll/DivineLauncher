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

    public static void load(ImageView view, Account account) {
        if(account == null) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
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
