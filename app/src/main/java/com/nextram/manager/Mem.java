package com.nextram.manager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public final class Mem {

    public static long totalKb;
    public static long availKb;
    public static long cachedKb;
    public static long swapTotalKb;
    public static long swapFreeKb;

    private Mem() {
    }

    public static void read() {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    totalKb = num(line);
                } else if (line.startsWith("MemAvailable:")) {
                    availKb = num(line);
                } else if (line.startsWith("Cached:")) {
                    cachedKb = num(line);
                } else if (line.startsWith("SwapTotal:")) {
                    swapTotalKb = num(line);
                } else if (line.startsWith("SwapFree:")) {
                    swapFreeKb = num(line);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static long num(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) {
            return 0L;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            return 0L;
        }
    }

    public static float usedFraction() {
        if (totalKb <= 0) {
            return 0f;
        }
        float f = (totalKb - availKb) / (float) totalKb;
        return f < 0f ? 0f : (f > 1f ? 1f : f);
    }

    public static String gb(long kb) {
        return String.format(Locale.US, "%.1f ГБ", kb / 1048576.0);
    }
}
