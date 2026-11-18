package com.divinedoll.launcher.progresskeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ProgressKeeper {
    private static final HashMap<String, List<ProgressListener>> sProgressListeners = new HashMap<>();
    private static final HashMap<String, ProgressState> sProgressStates = new HashMap<>();
    private static final List<TaskCountListener> sTaskCountListeners = new ArrayList<>();

    public static synchronized void submitProgress(String progressRecord, int progress, int resid, Object... va) {
        ProgressState progressState = sProgressStates.get(progressRecord);
        boolean shouldCallStarted = progressState == null;
        boolean shouldCallEnded = resid == -1 && progress == -1;
        if(shouldCallEnded) {
            shouldCallStarted = false;
            sProgressStates.remove(progressRecord);
        }else if(shouldCallStarted){
            sProgressStates.put(progressRecord, (progressState = new ProgressState()));
        }
        if(shouldCallEnded || shouldCallStarted) updateTaskCount(sProgressStates.size());
        if(progressState != null) {
            progressState.progress = progress;
            progressState.resid = resid;
            progressState.varArg = va;
        }

        List<ProgressListener> progressListeners = sProgressListeners.get(progressRecord);
        if(progressListeners != null)
            for(ProgressListener listener : progressListeners) {
                    if(shouldCallStarted) listener.onProgressStarted();
                    else if(shouldCallEnded) listener.onProgressEnded();
                    else listener.onProgressUpdated(progress, resid, va);
            }
    }

    private static void updateTaskCount(int count) {
        synchronized (sTaskCountListeners) {
            Iterator<TaskCountListener> iterator = sTaskCountListeners.iterator();
            while(iterator.hasNext()) {
                if(iterator.next().onUpdateTaskCount(count)) iterator.remove();
            }
        }
    }

    /** Current tasks as {key, progress, text} rows, for the downloads screen. */
    private static final java.util.Map<String, double[]> sLastBytes = new java.util.HashMap<>();

    /** "12.3 / 45.6 MB · 2.1 MB/s" for downloads that report megabytes, else "". */
    private static String megabytes(String key, Object[] va) {
        if(va == null || va.length < 2) return "";
        Object a = va[va.length - 2], b = va[va.length - 1];
        if(!(a instanceof Float || a instanceof Double) || !(b instanceof Float || b instanceof Double)) return "";
        double cur = ((Number) a).doubleValue(), max = ((Number) b).doubleValue();
        if(max <= 0 || cur > max + 0.01) return "";
        long now = android.os.SystemClock.elapsedRealtime();
        double[] last = sLastBytes.get(key); // {cur, time, speed}
        double speed = last == null ? 0 : last[2];
        if(last != null && cur > last[0] && now > (long) last[1]) {
            speed = (cur - last[0]) / ((now - (long) last[1]) / 1000.0);
            sLastBytes.put(key, new double[]{cur, now, speed});
        } else if(last == null || cur < last[0]) {
            sLastBytes.put(key, new double[]{cur, now, 0});
        }
        return String.format(java.util.Locale.US, "%.1f / %.1f MB", cur, max)
                + (speed > 0 ? String.format(java.util.Locale.US, " \u00B7 %.1f MB/s", speed) : "");
    }

    public static synchronized List<Object[]> snapshot(android.content.Context c) {
        List<Object[]> rows = new ArrayList<>();
        for(java.util.Map.Entry<String, ProgressState> e : sProgressStates.entrySet()) {
            ProgressState st = e.getValue();
            String text = "";
            try {
                if(st.resid != -1) text = c.getString(st.resid, st.varArg == null ? new Object[0] : st.varArg);
                else if(st.varArg != null && st.varArg.length > 0 && st.varArg[0] != null) text = String.valueOf(st.varArg[0]);
            } catch (Exception ignored) {}
            rows.add(new Object[]{e.getKey(), st.progress, text, megabytes(e.getKey(), st.varArg)});
        }
        return rows;
    }

    public static synchronized boolean hasProgressKey(String key) {
        return sProgressStates.get(key) != null;
    }

    public static synchronized void addListener(String progressRecord, ProgressListener listener) {
        ProgressState state = sProgressStates.get(progressRecord);
        if(state != null && (state.resid != -1 || state.progress != -1)) {
            listener.onProgressStarted();
            listener.onProgressUpdated(state.progress, state.resid, state.varArg);
        }else{
            listener.onProgressEnded();
        }
        List<ProgressListener> listenerWeakReferenceList = sProgressListeners.get(progressRecord);
        if(listenerWeakReferenceList == null) sProgressListeners.put(progressRecord, (listenerWeakReferenceList = new ArrayList<>()));
        listenerWeakReferenceList.add(listener);
    }

    public static synchronized void removeListener(String progressRecord, ProgressListener listener) {
        List<ProgressListener> listenerWeakReferenceList = sProgressListeners.get(progressRecord);
        if(listenerWeakReferenceList != null) listenerWeakReferenceList.remove(listener);
    }

    public static void addTaskCountListener(TaskCountListener listener) {
        addTaskCountListener(listener, true);
    }
    public static void addTaskCountListener(TaskCountListener listener, boolean runUpdate) {
        if(runUpdate) synchronized (ProgressKeeper.class) {
            listener.onUpdateTaskCount(sProgressStates.size());
        }
        synchronized (sTaskCountListeners) {
            if(!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener);
        }
    }
    public static void removeTaskCountListener(TaskCountListener listener) {
        synchronized (sTaskCountListeners) {
            sTaskCountListeners.remove(listener);
        }
    }

    /**
     * Waits until all tasks are done and runs the runnable, or if there were no pending process remaining
     * The runnable runs from the thread that updated the task count last, and it might be the UI thread,
     * so don't put long running processes in it
     * @param runnable the runnable to run when no tasks are remaining
     */
    public static void waitUntilDone(final Runnable runnable) {
        // If we do it the other way the listener would be removed before it was added, which will cause a listener object leak
        if(getTaskCount() == 0) {
            runnable.run();
            return;
        }
        TaskCountListener listener = taskCount -> {
            if(taskCount == 0) {
                runnable.run();
                return true;
            }
            return false;
        };
        addTaskCountListener(listener);
    }

    public static synchronized int getTaskCount() {
        return sProgressStates.size();
    }

    public static boolean hasOngoingTasks() {
        return getTaskCount() > 0;
    }
}
