package git.artdeell.mojo.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/** Small switches that do not fit in the color dialogs, and the performance profiles. */
public class ExtraOptions {
    /** Resolution scale in percent, sustained performance and vsync for each profile. */
    private static final int[] SCALE = {60, 80, 100};
    private static final boolean[] SUSTAINED = {true, true, false};
    private static final boolean[] VSYNC = {false, true, true};

    public static void show(Context c, Runnable onChanged) {
        String[] items = {c.getString(R.string.fork_extra_switches), c.getString(R.string.fork_perf_title)};
        new AlertDialog.Builder(c).setTitle(R.string.fork_extra_menu).setItems(items, (d, which) -> {
            if(which == 0) switches(c, onChanged);
            else performance(c);
        }).show();
    }

    private static void switches(Context c, Runnable onChanged) {
        String[] items = {c.getString(R.string.fork_extra_banner_auto), c.getString(R.string.fork_extra_click_sound)};
        boolean[] checked = {
                LauncherPreferences.DEFAULT_PREF.getBoolean(BannerManager.PREF_AUTO, false),
                LauncherPreferences.DEFAULT_PREF.getBoolean(ClickSound.PREF, true)
        };
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_extra_switches)
                .setMultiChoiceItems(items, checked, (d, which, on) -> checked[which] = on)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putBoolean(BannerManager.PREF_AUTO, checked[0])
                            .putBoolean(ClickSound.PREF, checked[1]).apply();
                    if(onChanged != null) onChanged.run();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Changes only the resolution scale, sustained performance and vsync. The renderer and the RAM stay as they are. */
    private static void performance(Context c) {
        String[] names = {c.getString(R.string.fork_perf_fast), c.getString(R.string.fork_perf_balanced), c.getString(R.string.fork_perf_quality)};
        new AlertDialog.Builder(c).setTitle(R.string.fork_perf_title).setItems(names, (d, which) -> {
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putInt("resolutionRatio", SCALE[which])
                    .putBoolean("sustainedPerformance", SUSTAINED[which])
                    .putBoolean("force_vsync", VSYNC[which]).apply();
            LauncherPreferences.loadPreferences(c.getApplicationContext());
            Toast.makeText(c, c.getString(R.string.fork_perf_applied, names[which]), Toast.LENGTH_LONG).show();
        }).show();
    }
}
