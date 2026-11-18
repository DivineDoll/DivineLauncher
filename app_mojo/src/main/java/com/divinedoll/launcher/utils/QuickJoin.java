package com.divinedoll.launcher.utils;

import androidx.fragment.app.FragmentActivity;

import com.divinedoll.launcher.extra.ExtraConstants;
import com.divinedoll.launcher.extra.ExtraCore;
import com.divinedoll.launcher.instances.Instance;
import com.divinedoll.launcher.instances.Instances;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Starts the game and joins a server straight away, like the server shortcuts of other launchers. */
public final class QuickJoin {
    private static final long VALID_MS = 10 * 60 * 1000L;
    private static String sAddress;
    private static long sSince;

    private QuickJoin() {
    }

    /** Selects the instance, remembers the address and starts the launch. Returns false when the instance is missing. */
    public static boolean join(FragmentActivity act, String instanceName, String address) {
        try {
            Instance instance = instanceName == null || instanceName.isEmpty()
                    ? Instances.loadSelectedInstance() : InstanceTools.find(instanceName);
            if (instance == null) return false;
            Instances.setSelectedInstance(instance);
            sAddress = address;
            sSince = System.currentTimeMillis();
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Normal launches call this so an old pending server is not used by mistake. */
    public static void clear() {
        sAddress = null;
    }

    /** Adds the arguments that make the game join the pending server, then forgets it. */
    public static void addArgs(List<String> args, String versionName) {
        String address = sAddress;
        long since = sSince;
        sAddress = null;
        if (address == null || address.isEmpty() || System.currentTimeMillis() - since > VALID_MS) return;
        String host = address.trim();
        String port = "25565";
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            if (end > 0) {
                if (host.length() > end + 2 && host.charAt(end + 1) == ':') port = host.substring(end + 2);
                host = host.substring(1, end);
            }
        } else if (host.indexOf(':') > 0 && host.indexOf(':') == host.lastIndexOf(':')) {
            port = host.substring(host.indexOf(':') + 1);
            host = host.substring(0, host.indexOf(':'));
        }
        if (usesQuickPlay(versionName)) {
            args.add("--quickPlayMultiplayer");
            args.add(host.contains(":") ? "[" + host + "]:" + port : host + ":" + port);
        } else {
            args.add("--server");
            args.add(host);
            args.add("--port");
            args.add(port);
        }
    }

    /** 1.20 and newer (and the new year-based names) use quick play; older versions use --server and --port. */
    static boolean usesQuickPlay(String versionName) {
        if (versionName == null) return true;
        Matcher m = Pattern.compile("(?<![0-9])1\\.(\\d+)").matcher(versionName);
        int minor = -1;
        while (m.find()) minor = Integer.parseInt(m.group(1));
        if (minor >= 0) return minor >= 20;
        Matcher snap = Pattern.compile("^(\\d{2})w\\d{2}").matcher(versionName);
        if (snap.find()) return Integer.parseInt(snap.group(1)) >= 23;
        return true;
    }
}
