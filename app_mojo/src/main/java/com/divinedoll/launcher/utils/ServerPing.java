package com.divinedoll.launcher.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Minimal "server list ping": asks a Java server for its player count and message of the day. */
public final class ServerPing {
    private ServerPing() {}

    public static final class Result {
        public boolean online;
        public int players = -1, max = -1;
        public long latencyMs;
        public String motd = "";
    }

    /** host may carry a port ("host:25565"). Blocking. */
    public static Result ping(String address, int timeoutMs) {
        Result r = new Result();
        String host = address.trim();
        int port = 25565;
        int colon = host.lastIndexOf(':');
        if(colon > 0 && colon == host.indexOf(':')) {
            try { port = Integer.parseInt(host.substring(colon + 1)); host = host.substring(0, colon); } catch(NumberFormatException ignored) {}
        }
        try(Socket socket = new Socket()) {
            long start = System.currentTimeMillis();
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hs);
            writeVarInt(h, 0);
            writeVarInt(h, -1);
            byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
            writeVarInt(h, hostBytes.length);
            h.write(hostBytes);
            h.writeShort(port);
            writeVarInt(h, 1);
            writeVarInt(out, hs.size());
            out.write(hs.toByteArray());
            writeVarInt(out, 1);
            writeVarInt(out, 0);
            out.flush();

            readVarInt(in);
            int id = readVarInt(in);
            if(id != 0) return r;
            int len = readVarInt(in);
            if(len <= 0 || len > 1 << 20) return r;
            byte[] data = new byte[len];
            in.readFully(data);
            r.latencyMs = System.currentTimeMillis() - start;
            JsonObject json = new Gson().fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
            if(json.has("players")) {
                JsonObject p = json.getAsJsonObject("players");
                r.players = p.get("online").getAsInt();
                r.max = p.get("max").getAsInt();
            }
            if(json.has("description")) {
                com.google.gson.JsonElement d = json.get("description");
                r.motd = d.isJsonPrimitive() ? d.getAsString() : d.isJsonObject() && d.getAsJsonObject().has("text") ? d.getAsJsonObject().get("text").getAsString() : "";
            }
            r.online = true;
        } catch(Exception ignored) {
        }
        return r;
    }

    private static void writeVarInt(DataOutputStream out, int value) throws java.io.IOException {
        while((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws java.io.IOException {
        int result = 0, shift = 0;
        while(true) {
            int b = in.readUnsignedByte();
            result |= (b & 0x7F) << shift;
            if((b & 0x80) == 0) return result;
            shift += 7;
            if(shift > 35) throw new java.io.IOException("VarInt too big");
        }
    }
}
