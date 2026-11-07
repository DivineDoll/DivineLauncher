package git.artdeell.mojo.utils;

import android.app.Activity;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicInteger;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/**
 * Small FPS and memory readout drawn over the game. Frames are counted when the game posts a new frame
 * to its TextureView, so the game uses a TextureView while the readout is on, even if the alternate surface is chosen.
 */
public class FpsOverlay {
    public static final String PREF = "show_fps";
    public static final String PREF_MEMORY = "show_memory";
    private static final int INTERVAL_MS = 500;
    private static final AtomicInteger sFrames = new AtomicInteger();

    /** Called for each frame the game presents. */
    public static void onFrame() {
        sFrames.incrementAndGet();
    }

    /** The alternate SurfaceView gives no frame callbacks, so it is not used while the readout is on. */
    public static boolean useSurfaceView() {
        return LauncherPreferences.PREF_USE_ALTERNATE_SURFACE && !fpsEnabled();
    }

    public static boolean fpsEnabled() {
        return pref(PREF);
    }

    public static boolean memoryEnabled() {
        return pref(PREF_MEMORY);
    }

    private static boolean pref(String key) {
        try {
            return LauncherPreferences.DEFAULT_PREF.getBoolean(key, false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void attach(Activity activity) {
        final boolean showFps = fpsEnabled();
        final boolean showMemory = memoryEnabled();
        if(!showFps && !showMemory) return;
        TextView text = new TextView(activity);
        text.setTypeface(McStyle.font(activity));
        text.setTextSize(12);
        text.setTextColor(0xFFFFFFFF);
        text.setShadowLayer(2f, 1f, 1f, 0xFF000000);
        text.setBackgroundColor(0x66000000);
        int pad = (int) (4 * activity.getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad);
        text.setClickable(false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        activity.addContentView(text, params);

        Handler handler = new Handler(Looper.getMainLooper());
        long[] last = {SystemClock.uptimeMillis()};
        int[] ticks = {0};
        String[] memory = {""};
        sFrames.set(0);
        Runnable update = new Runnable() {
            @Override
            public void run() {
                long now = SystemClock.uptimeMillis();
                int frames = sFrames.getAndSet(0);
                float fps = frames * 1000f / Math.max(1, now - last[0]);
                last[0] = now;
                if(showMemory && ticks[0]++ % 2 == 0) {
                    Debug.MemoryInfo info = new Debug.MemoryInfo();
                    Debug.getMemoryInfo(info);
                    memory[0] = activity.getString(R.string.fork_fps_memory, info.getTotalPss() / 1024);
                }
                StringBuilder sb = new StringBuilder();
                if(showFps) sb.append(activity.getString(R.string.fork_fps_line, Math.round(fps)));
                if(showMemory) {
                    if(sb.length() > 0) sb.append('\n');
                    sb.append(memory[0]);
                }
                text.setText(sb);
                handler.postDelayed(this, INTERVAL_MS);
            }
        };
        text.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                handler.post(update);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                handler.removeCallbacks(update);
            }
        });
    }
}
