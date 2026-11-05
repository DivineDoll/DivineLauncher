package git.artdeell.mojo.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** One .zip with the current theme, the saved themes and the pixel arts, to keep or move to another phone. */
public class BackupManager {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9 _.-]{1,60}");
    private static final int MAX_ENTRY = 5 * 1024 * 1024;

    /** Builds the backup and copies it to Downloads. Returns the file name. Blocking. */
    public static String export(Context c) throws Exception {
        File zip = new File(c.getCacheDir(), "mojo-backup.zip");
        try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            put(out, "theme.json", ThemeManager.currentAsJson().getBytes(StandardCharsets.UTF_8));
            addFolder(out, ThemeManager.directory(c), "themes/", ".json");
            addFolder(out, PixelArtEditor.directory(c), "pixelart/", ".png");
        }
        String name = OnlineSkins.exportToDownloads(c, zip, "application/zip");
        zip.delete();
        return name;
    }

    private static void addFolder(ZipOutputStream out, File dir, String prefix, String ext) throws IOException {
        File[] files = dir.listFiles((d, n) -> n.endsWith(ext));
        if(files == null) return;
        for(File f : files) {
            byte[] data = new byte[(int) f.length()];
            try(FileInputStream in = new FileInputStream(f)) {
                int read = 0;
                while(read < data.length) {
                    int n = in.read(data, read, data.length - read);
                    if(n < 0) break;
                    read += n;
                }
            }
            put(out, prefix + f.getName(), data);
        }
    }

    private static void put(ZipOutputStream out, String name, byte[] data) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(data);
        out.closeEntry();
    }

    /** Restores a backup. Only the known folders and plain file names are accepted. Blocking. */
    public static void restore(Context c, InputStream raw) throws Exception {
        String theme = null;
        try(ZipInputStream zip = new ZipInputStream(raw)) {
            for(ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                String name = e.getName();
                byte[] data = readLimited(zip);
                if(name.equals("theme.json")) {
                    theme = new String(data, StandardCharsets.UTF_8);
                } else if(name.startsWith("themes/") && name.endsWith(".json")) {
                    write(ThemeManager.directory(c), name.substring(7), data);
                } else if(name.startsWith("pixelart/") && name.endsWith(".png")) {
                    write(PixelArtEditor.directory(c), name.substring(9), data);
                }
            }
        }
        if(theme != null) ThemeManager.applyJson(theme);
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if(out.size() > MAX_ENTRY) throw new IOException("Entry too large");
        }
        return out.toByteArray();
    }

    private static void write(File dir, String fileName, byte[] data) throws IOException {
        if(!NAME.matcher(fileName).matches() || fileName.contains("..")) return;
        try(FileOutputStream out = new FileOutputStream(new File(dir, fileName))) {
            out.write(data);
        }
    }
}
