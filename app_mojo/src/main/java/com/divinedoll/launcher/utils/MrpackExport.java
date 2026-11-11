package com.divinedoll.launcher.utils;

import android.content.Context;
import android.net.Uri;

import com.divinedoll.launcher.instances.Instance;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports an instance as a .mrpack. Mods Modrinth knows are listed with their download link,
 * everything else (other mods, config, resource packs, shaders, options) goes in the overrides.
 * Worlds, logs and backups are never included.
 */
public final class MrpackExport {
    private MrpackExport() {}

    private static final String[] OVERRIDE_DIRS = {"config", "resourcepacks", "shaderpacks"};

    /** Returns how many mods were linked to Modrinth. Blocking. */
    public static int export(Context c, Instance instance, Uri target) throws Exception {
        File gameDir = instance.getGameDirectory();
        File modsDir = new File(gameDir, "mods");
        File[] jars = modsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        Map<String, File> byHash = new HashMap<>();
        JsonArray hashes = new JsonArray();
        if(jars != null) for(File j : jars) { String h = Modrinth.sha1Of(j); byHash.put(h, j); hashes.add(h); }

        Map<String, JsonObject> found = new HashMap<>();
        if(hashes.size() > 0) {
            try {
                JsonObject body = new JsonObject();
                body.add("hashes", hashes);
                body.addProperty("algorithm", "sha1");
                JsonObject res = new Gson().fromJson(Modrinth.api("POST", "/version_files", body.toString()), JsonObject.class);
                for(Map.Entry<String, JsonElement> en : res.entrySet()) {
                    JsonArray files = en.getValue().getAsJsonObject().getAsJsonArray("files");
                    if(files == null) continue;
                    for(JsonElement fe : files) {
                        JsonObject file = fe.getAsJsonObject();
                        JsonObject h = file.getAsJsonObject("hashes");
                        if(h != null && h.has("sha1") && en.getKey().equalsIgnoreCase(h.get("sha1").getAsString())) found.put(en.getKey(), file);
                    }
                }
            } catch(Exception ignored) {
                // Offline or blocked: everything goes in the overrides, the pack still works
            }
        }

        JsonObject index = new JsonObject();
        index.addProperty("formatVersion", 1);
        index.addProperty("game", "minecraft");
        index.addProperty("versionId", "1.0.0");
        index.addProperty("name", String.valueOf(instance.name));
        index.addProperty("summary", "Exported from DivineLauncher");
        JsonArray list = new JsonArray();
        for(Map.Entry<String, JsonObject> en : found.entrySet()) {
            JsonObject file = en.getValue();
            File local = byHash.get(en.getKey());
            JsonObject o = new JsonObject();
            o.addProperty("path", "mods/" + local.getName());
            JsonObject h = new JsonObject();
            h.addProperty("sha1", en.getKey());
            JsonObject remote = file.getAsJsonObject("hashes");
            h.addProperty("sha512", remote.has("sha512") ? remote.get("sha512").getAsString() : sha512(local));
            o.add("hashes", h);
            JsonArray dl = new JsonArray();
            dl.add(file.get("url").getAsString());
            o.add("downloads", dl);
            o.addProperty("fileSize", local.length());
            list.add(o);
        }
        index.add("files", list);
        JsonObject deps = new JsonObject();
        String gv = Modrinth.gameVersionOf(instance.versionId);
        if(gv != null) deps.addProperty("minecraft", gv);
        String loader = Modrinth.loaderOf(instance.versionId);
        String loaderVersion = loaderVersion(instance.versionId, loader, gv);
        if(loader != null && loaderVersion != null) {
            String key = "fabric".equals(loader) ? "fabric-loader" : "quilt".equals(loader) ? "quilt-loader" : loader;
            deps.addProperty(key, loaderVersion);
        }
        index.add("dependencies", deps);

        try(OutputStream raw = c.getContentResolver().openOutputStream(target, "wt");
            ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.putNextEntry(new ZipEntry("modrinth.index.json"));
            zip.write(index.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            if(jars != null) for(File j : jars) {
                if(!found.containsKey(Modrinth.sha1Of(j))) addFile(zip, j, "overrides/mods/" + j.getName());
            }
            for(String dir : OVERRIDE_DIRS) addTree(zip, new File(gameDir, dir), "overrides/" + dir + "/");
            File options = new File(gameDir, "options.txt");
            if(options.isFile()) addFile(zip, options, "overrides/options.txt");
        }
        return found.size();
    }

    private static String loaderVersion(String versionId, String loader, String gameVersion) {
        if(versionId == null || loader == null) return null;
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+){1,3})").matcher(versionId);
        while(m.find()) {
            String v = m.group(1);
            if(!v.equals(gameVersion)) return v;
        }
        return null;
    }

    private static String sha512(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        try(FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for(byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static void addTree(ZipOutputStream zip, File dir, String path) throws IOException {
        File[] kids = dir.listFiles();
        if(kids == null) return;
        for(File k : kids) {
            if(k.isDirectory()) addTree(zip, k, path + k.getName() + "/");
            else addFile(zip, k, path + k.getName());
        }
    }

    private static void addFile(ZipOutputStream zip, File f, String path) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        try(FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while((n = in.read(buf)) > 0) zip.write(buf, 0, n);
        }
        zip.closeEntry();
    }
}
