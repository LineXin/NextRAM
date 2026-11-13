package com.nextram.manager;

import android.util.Base64;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class Config {

    private static final String[] FALLBACK = {
            "ZRAM_ENABLED=true",
            "ZRAM_RATIO=50",
            "ZRAM_ALGO=lz4",
            "SWAP_ENABLED=false",
            "SWAPPINESS=60",
            "DYNAMIC_SWAPPINESS=true",
            "CACHE_PRESSURE=100",
            "DIRTY_RATIO=20",
            "PLAY_ENABLED=false"
    };

    private final List<String> lines = new ArrayList<String>();
    private final LinkedHashMap<String, Integer> index = new LinkedHashMap<String, Integer>();

    public String dir = "";
    public boolean found = false;

    private Config() {
    }

    public static Config load() {
        Config c = new Config();
        if (Shell.hasRoot()) {
            Shell.Result r = Shell.run(
                    "for d in /data/adb/modules/NextRAM /data/adb/modules/nextram; do "
                            + "if [ -f \"$d/config.conf\" ]; then echo \"DIR=$d\"; cat \"$d/config.conf\"; break; fi; done");
            if (r.out != null && r.out.trim().length() > 0) {
                c.parse(r.out);
            }
        }
        if (!c.found) {
            c.lines.clear();
            c.index.clear();
            for (int i = 0; i < FALLBACK.length; i++) {
                c.lines.add(FALLBACK[i]);
                c.index.put(keyOf(FALLBACK[i]), Integer.valueOf(c.lines.size() - 1));
            }
        }
        return c;
    }

    private void parse(String out) {
        String[] arr = out.split("\n");
        for (int i = 0; i < arr.length; i++) {
            String line = arr[i];
            String t = line.trim();
            if (t.startsWith("DIR=")) {
                dir = t.substring(4).trim();
                found = true;
                continue;
            }
            lines.add(line);
            if (t.length() > 0 && !t.startsWith("#") && t.indexOf('=') > 0) {
                index.put(keyOf(t), Integer.valueOf(lines.size() - 1));
            }
        }
    }

    private static String keyOf(String line) {
        int p = line.indexOf('=');
        return p > 0 ? line.substring(0, p).trim() : line.trim();
    }

    public List<String> keys() {
        return new ArrayList<String>(index.keySet());
    }

    public String value(String key, String def) {
        Integer i = index.get(key);
        if (i == null) {
            return def;
        }
        String line = lines.get(i.intValue());
        int p = line.indexOf('=');
        return p > 0 ? line.substring(p + 1).trim() : "";
    }

    public int intValue(String key, int def) {
        try {
            return Integer.parseInt(value(key, String.valueOf(def)).trim());
        } catch (Exception e) {
            return def;
        }
    }

    public boolean boolValue(String key, boolean def) {
        String v = value(key, def ? "true" : "false").toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    public boolean isBool(String key) {
        String v = value(key, "").toLowerCase();
        return "true".equals(v) || "false".equals(v) || "1".equals(v) || "0".equals(v);
    }

    public boolean isNumber(String key) {
        try {
            Integer.parseInt(value(key, "x").trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void set(String key, String val) {
        Integer i = index.get(key);
        if (i == null) {
            lines.add(key + "=" + val);
            index.put(key, Integer.valueOf(lines.size() - 1));
        } else {
            lines.set(i.intValue(), key + "=" + val);
        }
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i)).append('\n');
        }
        return sb.toString();
    }

    public boolean save() {
        if (!found || dir.length() == 0) {
            return false;
        }
        String b64 = Base64.encodeToString(text().getBytes(), Base64.NO_WRAP);
        String path = dir + "/config.conf";
        Shell.Result r = Shell.run(
                "echo '" + b64 + "' | base64 -d > '" + path + "' && chmod 644 '" + path + "'");
        if (!r.ok()) {
            return false;
        }
        Shell.run("cd '" + dir + "' && nohup sh service.sh >/dev/null 2>&1 &");
        return true;
    }

    public static String pretty(String key) {
        String s = key.replace('_', ' ').toLowerCase();
        if (s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
