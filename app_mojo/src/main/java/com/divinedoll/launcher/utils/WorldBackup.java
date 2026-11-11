package com.divinedoll.launcher.utils;

import com.divinedoll.launcher.prefs.LauncherPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Zips the worlds of an instance into its backups folder, only the ones that changed since the last copy. */
public final class WorldBackup {
    private WorldBackup() {}

    public static final String PREF = "auto_world_backup";
    private static final int KEEP = 3;
    private static final long MAX_BYTES = 800L * 1024 * 1024;

    public static boolean enabled() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF, true);
    }

    public static void setEnabled(boolean on) {
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF, on).apply();
    }

    /** Copies every changed world. Returns how many were saved. Never throws. */
    public static int run(File gameDir) {
        int saved = 0;
        try {
            File[] worlds = new File(gameDir, "saves").listFiles(File::isDirectory);
            if(worlds == null) return 0;
            for(File world : worlds) {
                try {
                    if(backup(gameDir, world, false)) saved++;
                } catch(Exception ignored) {}
            }
        } catch(Exception ignored) {}
        return saved;
    }

    /** One world. With force it also copies it when nothing changed. */
    public static boolean backup(File gameDir, File world, boolean force) throws IOException {
        File out = new File(gameDir, "backups");
        if(!out.isDirectory() && !out.mkdirs()) throw new IOException("backups");
        String safe = world.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        File[] old = out.listFiles((d, n) -> n.startsWith(safe + "-") && n.endsWith(".zip"));
        long last = 0;
        if(old != null) for(File f : old) last = Math.max(last, f.lastModified());
        long changed = newest(world);
        if(!force && changed <= last) return false;
        if(!force && size(world) > MAX_BYTES) return false;
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File tmp = new File(out, safe + "-" + stamp + ".tmp");
        try(ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmp))) {
            add(zip, world, world.getName() + "/");
        } catch(IOException e) {
            tmp.delete();
            throw e;
        }
        File done = new File(out, safe + "-" + stamp + ".zip");
        if(!tmp.renameTo(done)) { tmp.delete(); throw new IOException("rename"); }
        old = out.listFiles((d, n) -> n.startsWith(safe + "-") && n.endsWith(".zip"));
        if(old != null && old.length > KEEP) {
            Arrays.sort(old, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for(int i = KEEP; i < old.length; i++) old[i].delete();
        }
        return true;
    }

    private static long newest(File f) {
        long t = f.lastModified();
        File[] kids = f.listFiles();
        if(kids != null) for(File k : kids) t = Math.max(t, newest(k));
        return t;
    }

    private static long size(File f) {
        if(f.isFile()) return f.length();
        long n = 0;
        File[] kids = f.listFiles();
        if(kids != null) for(File k : kids) n += size(k);
        return n;
    }

    private static void add(ZipOutputStream zip, File f, String path) throws IOException {
        if(f.isDirectory()) {
            File[] kids = f.listFiles();
            if(kids != null) for(File k : kids) add(zip, k, path + k.getName() + (k.isDirectory() ? "/" : ""));
            return;
        }
        if(f.getName().equals("session.lock")) return;
        zip.putNextEntry(new ZipEntry(path));
        try(FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while((n = in.read(buf)) > 0) zip.write(buf, 0, n);
        }
        zip.closeEntry();
    }
}
