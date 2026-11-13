package com.nextram.manager;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {

    public static final int BG = 0xFF0B0B0C;
    public static final int SURFACE = 0xFF141416;
    public static final int SURFACE_HI = 0xFF1B1B1E;
    public static final int STROKE = 0xFF26262B;
    public static final int TEXT = 0xFFF2F2F3;
    public static final int TEXT_DIM = 0xFF9A9AA2;
    public static final int TEXT_MUTE = 0xFF6A6A72;
    public static final int GREEN = 0xFF43C463;
    public static final int DANGER = 0xFFE5322B;

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static Typeface sans() {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    public static Typeface medium() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface mono() {
        return Typeface.create("monospace", Typeface.NORMAL);
    }

    public static GradientDrawable round(int color, float radius, int stroke, int strokeW) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(color);
        g.setCornerRadius(radius);
        if (strokeW > 0) {
            g.setStroke(strokeW, stroke);
        }
        return g;
    }

    public static GradientDrawable gradient(int from, int to, float radius) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{from, to});
        g.setShape(GradientDrawable.RECTANGLE);
        g.setCornerRadius(radius);
        return g;
    }

    public static TextView text(Context c, CharSequence s, float size, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(SURFACE, dp(c, 18), STROKE, Math.max(1, dp(c, 1))));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView caption(Context c, String s) {
        TextView t = text(c, s, 10, TEXT_MUTE, medium());
        t.setLetterSpacing(0.18f);
        return t;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(STROKE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(c, 1)));
        lp.topMargin = dp(c, 13);
        lp.bottomMargin = dp(c, 13);
        v.setLayoutParams(lp);
        return v;
    }

    public static View vspace(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(c, heightDp)));
        return v;
    }

    public static int blend(int a, int b, float t) {
        if (t < 0f) {
            t = 0f;
        }
        if (t > 1f) {
            t = 1f;
        }
        return Color.argb(
                (int) (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t),
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
