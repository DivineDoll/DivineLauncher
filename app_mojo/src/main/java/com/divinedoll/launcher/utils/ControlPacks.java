package com.divinedoll.launcher.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.prefs.LauncherPreferences;

import java.io.File;

/** Button packs: the normal launcher layout or one of the Bedrock-style presets. Sets the default control layout. */
public final class ControlPacks {
    private ControlPacks() {}

    public static final String NORMAL = "default.json";
    public static final String[] BEDROCK = {"Bedrock_Tocar.json", "Bedrock_Cruz_filar.json", "Bedrock_Clasico.json"};

    public static int labelOf(String file) {
        switch(file) {
            case "Bedrock_Tocar.json": return R.string.fork_pack_bedrock_touch;
            case "Bedrock_Cruz_filar.json": return R.string.fork_pack_bedrock_dpad;
            case "Bedrock_Clasico.json": return R.string.fork_pack_bedrock_classic;
            default: return R.string.fork_pack_normal;
        }
    }

    /** File name of the pack that is the default now. */
    public static String current() {
        String path = LauncherPreferences.PREF_DEFAULTCTRL_PATH;
        return path == null ? NORMAL : new File(path).getName();
    }

    public static boolean isBedrock(String file) {
        for(String b : BEDROCK) if(b.equals(file)) return true;
        return false;
    }

    /** Copies the pack from the app when it is not in the controls folder. */
    public static void ensure(Context c, String file) throws java.io.IOException {
        File target = new File(Tools.CTRLMAP_PATH, file);
        if(!target.exists()) Tools.copyAssetFile(c, NORMAL.equals(file) ? file : "controls/" + file, Tools.CTRLMAP_PATH, true);
    }

    /** Makes the pack the default layout, copying it from the app if it is missing. */
    public static boolean apply(Context c, String file) {
        try {
            File target = new File(Tools.CTRLMAP_PATH, file);
            if(!target.exists()) {
                String asset = NORMAL.equals(file) ? file : "controls/" + file;
                Tools.copyAssetFile(c, asset, Tools.CTRLMAP_PATH, true);
            }
            LauncherPreferences.PREF_DEFAULTCTRL_PATH = target.getAbsolutePath();
            LauncherPreferences.DEFAULT_PREF.edit().putString("defaultCtrl", target.getAbsolutePath()).apply();
            return true;
        } catch(Exception e) {
            Toast.makeText(c, c.getString(R.string.fork_pack_error, String.valueOf(e.getMessage())), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /** Simple chooser, for places without room for the full cards. */
    public static void show(Context c, Runnable after) {
        String[] files = new String[BEDROCK.length + 1];
        files[0] = NORMAL;
        System.arraycopy(BEDROCK, 0, files, 1, BEDROCK.length);
        CharSequence[] labels = new CharSequence[files.length];
        int checked = 0;
        String now = current();
        for(int i = 0; i < files.length; i++) {
            labels[i] = c.getString(labelOf(files[i]));
            if(files[i].equals(now)) checked = i;
        }
        new AlertDialog.Builder(c).setTitle(R.string.fork_pack_title)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    if(apply(c, files[which]) && after != null) after.run();
                    d.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }
}
