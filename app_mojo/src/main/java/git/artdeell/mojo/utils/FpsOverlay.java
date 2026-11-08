package git.artdeell.mojo.utils;

import android.app.Activity;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
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
    public static final String PREF_BATTERY = "show_battery";
    private static final int INTERVAL_MS = 500;
    private static final AtomicInteger sFrames = new AtomicInteger();

    /** Called for each frame the game presents. */
    public static void onFrame() {
        sFrames.incrementAndGet();
        sLastFrameNanos = System.nanoTime();
    }

    private static volatile long sLastFrameNanos;

    /** True when the game has presented a frame in the last second, so the count can be trusted. */
    public static boolean counting() {
        return System.nanoTime() - sLastFrameNanos < 1_000_000_000L;
    }

    /** The alternate SurfaceView gives no frame callbacks, so it is not used while the readout is on. */
    public static boolean useSurfaceView() {
        return LauncherPreferences.PREF_USE_ALTERNATE_SURFACE && !fpsEnabled();
    }

    public static boolean fpsEnabled() {
        return pref(PREF);
    }

    public static boolean batteryEnabled() {
        return pref(PREF_BATTERY);
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

    private static TextView sText;
    private static boolean sFps, sMemory, sBattery;

    /** Changes what the readout shows right away, without waiting for the preferences to be saved. */
    public static void setLive(boolean fps, boolean memory, boolean battery) {
        sFps = fps;
        sMemory = memory;
        sBattery = battery;
        TextView text = sText;
        if(text != null) text.setVisibility(fps || memory || battery ? View.VISIBLE : View.GONE);
    }

    public static void attach(Activity activity) {
        sFps = fpsEnabled();
        sMemory = memoryEnabled();
        sBattery = pref(PREF_BATTERY);
        TextView text = new TextView(activity);
        sText = text;
        text.setVisibility(sFps || sMemory || sBattery ? View.VISIBLE : View.GONE);
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
        long[] last = {System.nanoTime()};
        int[] ticks = {0};
        String[] memory = {""};
        sFrames.set(0);
        Runnable update = new Runnable() {
            @Override
            public void run() {
                long now = System.nanoTime();
                int frames = sFrames.getAndSet(0);
                float fps = frames * 1_000_000_000f / Math.max(1L, now - last[0]);
                last[0] = now;
                if(sFps || sMemory || sBattery) {
                    if(sMemory && ticks[0]++ % 2 == 0) {
                        Debug.MemoryInfo info = new Debug.MemoryInfo();
                        Debug.getMemoryInfo(info);
                        memory[0] = activity.getString(R.string.fork_fps_memory, info.getTotalPss() / 1024);
                    }
                    StringBuilder sb = new StringBuilder();
                    if(sFps) sb.append(counting() ? activity.getString(R.string.fork_fps_line, Math.round(fps)) : "FPS: --");
                    if(sMemory) {
                        if(sb.length() > 0) sb.append('\n');
                        sb.append(memory[0]);
                    }
                    if(sBattery) {
                        if(sb.length() > 0) sb.append('\n');
                        sb.append(activity.getString(R.string.fork_fps_battery, batteryPercent(activity)));
                    }
                    text.setText(sb);
                }
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
                if(sText == text) sText = null;
            }
        });
    }

    private static int batteryPercent(Activity activity) {
        try {
            android.os.BatteryManager manager = (android.os.BatteryManager) activity.getSystemService(Activity.BATTERY_SERVICE);
            return manager == null ? 0 : manager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception e) {
            return 0;
        }
    }
}
