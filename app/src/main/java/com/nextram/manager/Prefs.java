package com.nextram.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class Prefs {

    private static final String FILE = "nextram_manager";
    private static final String ACCENT = "accent";

    public static final int[] PRESETS = {
            0xFFFF7A1A,
            0xFFFF3B30,
            0xFFE5322B,
            0xFFFFB020,
            0xFFFF4D6D,
            0xFF3DD68C,
            0xFF4C9AFF,
            0xFFB6B6BC
    };

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int accent(Context c) {
        return sp(c).getInt(ACCENT, PRESETS[0]);
    }

    public static void setAccent(Context c, int color) {
        sp(c).edit().putInt(ACCENT, color).apply();
    }

    public static int accent2(Context c) {
        float[] hsv = new float[3];
        Color.colorToHSV(accent(c), hsv);
        hsv[0] = (hsv[0] + 346f) % 360f;
        hsv[1] = Math.min(1f, hsv[1] * 1.06f);
        hsv[2] = Math.max(0.55f, hsv[2] * 0.9f);
        return Color.HSVToColor(hsv);
    }

    public static int accentSoft(Context c) {
        int a = accent(c);
        return Color.argb(38, Color.red(a), Color.green(a), Color.blue(a));
    }

    public static int accentLine(Context c) {
        int a = accent(c);
        return Color.argb(70, Color.red(a), Color.green(a), Color.blue(a));
    }
}
