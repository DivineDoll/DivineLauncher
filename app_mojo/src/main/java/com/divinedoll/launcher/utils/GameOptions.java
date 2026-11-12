package com.divinedoll.launcher.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads and changes a few lines of an instance's options.txt without touching the rest. Do it while the game is closed. */
public final class GameOptions {
    private GameOptions() {}

    public static String get(File gameDir, String key) {
        File f = new File(gameDir, "options.txt");
        if(!f.isFile()) return null;
        try(BufferedReader in = new BufferedReader(new FileReader(f))) {
            String line;
            while((line = in.readLine()) != null) {
                if(line.startsWith(key + ":")) return line.substring(key.length() + 1);
            }
        } catch(IOException ignored) {}
        return null;
    }

    public static int getInt(File gameDir, String key, int fallback) {
        try { return Integer.parseInt(get(gameDir, key)); } catch(Exception e) { return fallback; }
    }

    public static void set(File gameDir, Map<String, String> values) throws IOException {
        File f = new File(gameDir, "options.txt");
        List<String> lines = new ArrayList<>();
        if(f.isFile()) {
            try(BufferedReader in = new BufferedReader(new FileReader(f))) {
                String line;
                while((line = in.readLine()) != null) lines.add(line);
            }
        }
        for(Map.Entry<String, String> en : values.entrySet()) {
            boolean done = false;
            for(int i = 0; i < lines.size(); i++) {
                if(lines.get(i).startsWith(en.getKey() + ":")) { lines.set(i, en.getKey() + ":" + en.getValue()); done = true; break; }
            }
            if(!done) lines.add(en.getKey() + ":" + en.getValue());
        }
        if(!gameDir.isDirectory() && !gameDir.mkdirs()) throw new IOException("game folder");
        try(FileWriter out = new FileWriter(f)) {
            for(String l : lines) out.write(l + "\n");
        }
    }
}
