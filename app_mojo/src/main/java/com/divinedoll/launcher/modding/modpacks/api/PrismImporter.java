package com.divinedoll.launcher.modding.modpacks.api;

import com.divinedoll.launcher.R;
import com.divinedoll.launcher.Tools;
import com.divinedoll.launcher.modding.fabric.FabriclikeUtils;
import com.divinedoll.launcher.modding.forge.ForgelikeUtils;
import com.divinedoll.launcher.modding.modpacks.api.modloader.FabriclikeLoaderInstaller;
import com.divinedoll.launcher.modding.modpacks.api.modloader.ForgelikeLoaderInstaller;
import com.divinedoll.launcher.modding.modpacks.api.modloader.LoaderInstaller;
import com.kdt.mcgui.ProgressLayout;
import com.divinedoll.launcher.utils.ZipUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Imports the .zip that Prism Launcher and MultiMC export (mmc-pack.json + .minecraft folder). */
public final class PrismImporter {
    private PrismImporter() {}

    /** Path of mmc-pack.json inside the zip (it may sit under a top folder), or null. */
    public static String findManifest(ZipFile zip) {
        Enumeration<? extends ZipEntry> e = zip.entries();
        String best = null;
        while(e.hasMoreElements()) {
            String n = e.nextElement().getName();
            if(n.equals("mmc-pack.json") || n.endsWith("/mmc-pack.json")) {
                if(best == null || n.length() < best.length()) best = n;
            }
        }
        return best;
    }

    public static LoaderInstaller install(String name, File zipFile, String icon) throws IOException {
        return ModpackInstaller.installModpack(name, name, zipFile, icon, PrismImporter::extract);
    }

    private static LoaderInstaller extract(File zipFile, File destination) throws IOException {
        try(ZipFile zip = new ZipFile(zipFile)) {
            String manifest = findManifest(zip);
            if(manifest == null) throw new IOException("mmc-pack.json not found");
            String root = manifest.substring(0, manifest.length() - "mmc-pack.json".length());

            String json;
            try(InputStream in = ZipUtils.getEntryStream(zip, manifest)) {
                json = Tools.read(in);
            }
            JsonArray components = Tools.GLOBAL_GSON.fromJson(json, JsonObject.class).getAsJsonArray("components");
            String mc = null, forge = null, neoforge = null, fabric = null, quilt = null;
            for(JsonElement el : components) {
                JsonObject c = el.getAsJsonObject();
                String uid = c.get("uid").getAsString();
                String ver = c.has("version") ? c.get("version").getAsString() : null;
                if(ver == null) continue;
                switch (uid) {
                    case "net.minecraft": mc = ver; break;
                    case "net.minecraftforge": forge = ver; break;
                    case "net.neoforged": neoforge = ver; break;
                    case "net.fabricmc.fabric-loader": fabric = ver; break;
                    case "org.quiltmc.quilt-loader": quilt = ver; break;
                    default: break;
                }
            }
            if(mc == null) throw new IOException("Minecraft version not found in mmc-pack.json");

            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.modpack_download_applying_overrides, 1, 1);
            ZipUtils.zipExtract(zip, root + ".minecraft/", destination);
            ZipUtils.zipExtract(zip, root + "minecraft/", destination);

            if(forge != null) return new ForgelikeLoaderInstaller(ForgelikeUtils.FORGE_UTILS, mc, forge);
            if(neoforge != null) return new ForgelikeLoaderInstaller(ForgelikeUtils.NEOFORGE_UTILS, mc, neoforge);
            if(fabric != null) return new FabriclikeLoaderInstaller(FabriclikeUtils.FABRIC_UTILS, mc, fabric);
            if(quilt != null) return new FabriclikeLoaderInstaller(FabriclikeUtils.QUILT_UTILS, mc, quilt);
            return null; // vanilla pack: no loader to install
        }
    }
}
