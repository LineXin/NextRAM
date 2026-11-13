package com.nextram.manager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.view.View;

import java.util.Locale;

public class RingGauge extends View {

    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint foreground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float value = 0f;
    private String percent = "0%";
    private String captionText = "";
    private String subText = "";
    private int c1 = 0xFFFF7A1A;
    private int c2 = 0xFFE5322B;

    public RingGauge(Context c) {
        super(c);
        background.setStyle(Paint.Style.STROKE);
        background.setStrokeCap(Paint.Cap.ROUND);
        background.setColor(Ui.STROKE);

        foreground.setStyle(Paint.Style.STROKE);
        foreground.setStrokeCap(Paint.Cap.ROUND);

        big.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        big.setTextAlign(Paint.Align.CENTER);

        small.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        small.setTextAlign(Paint.Align.CENTER);

        sub.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        sub.setTextAlign(Paint.Align.CENTER);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    public void setColors(int from, int to) {
        c1 = from;
        c2 = to;
        invalidate();
    }

    public void setData(float fraction, String percentText, String caption, String subTextValue) {
        value = fraction;
        percent = percentText;
        captionText = caption;
        subText = subTextValue;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float thickness = dp(13);
        float size = Math.min(w, h) - thickness - dp(10);
        float cx = w / 2f;
        float cy = h / 2f;
        float r = size / 2f;

        oval.set(cx - r, cy - r, cx + r, cy + r);

        background.setStrokeWidth(thickness);
        canvas.drawArc(oval, 135f, 270f, false, background);

        if (value > 0.002f) {
            SweepGradient g = new SweepGradient(cx, cy, new int[]{c2, c1}, new float[]{0.0f, 1.0f});
            Matrix m = new Matrix();
            m.setRotate(135f, cx, cy);
            g.setLocalMatrix(m);
            foreground.setShader(g);
            foreground.setStrokeWidth(thickness);
            canvas.drawArc(oval, 135f, 270f * Math.min(1f, value), false, foreground);
        }

        big.setTextSize(dp(34));
        big.setColor(Ui.TEXT);
        small.setTextSize(dp(10));
        small.setColor(Ui.TEXT_DIM);
        sub.setTextSize(dp(11));
        sub.setColor(Ui.TEXT_MUTE);

        canvas.drawText(percent, cx, cy + dp(6), big);
        canvas.drawText(captionText, cx, cy - dp(22), small);
        canvas.drawText(subText, cx, cy + dp(26), sub);
    }
}
