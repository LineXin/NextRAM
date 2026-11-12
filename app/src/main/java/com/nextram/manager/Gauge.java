package com.nextram.manager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

public class Gauge extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float pct = 0f;
    private String caption = "0%";
    private String sub = "";

    public Gauge(Context context) {
        super(context);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setColor(Color.parseColor("#1B2331"));
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        big.setColor(Color.parseColor("#E8ECF3"));
        small.setTextAlign(Paint.Align.CENTER);
        small.setColor(Color.parseColor("#8C97A8"));
    }

    public void setData(float percent, String caption, String sub) {
        this.pct = Math.max(0f, Math.min(100f, percent));
        this.caption = caption;
        this.sub = sub;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float size = Math.min(w, h * 1.3f);
        float pad = size * 0.10f;
        float left = (w - size) / 2f + pad;
        float top = (h - size) / 2f + pad;
        oval.set(left, top, left + size - 2f * pad, top + size - 2f * pad);

        float stroke = size * 0.075f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);

        c.drawArc(oval, 135f, 270f, false, track);

        if (pct > 0.4f) {
            arc.setShader(new LinearGradient(oval.left, oval.bottom, oval.right, oval.top,
                    Color.parseColor("#4C8DFF"), Color.parseColor("#29D3A8"), Shader.TileMode.CLAMP));
            c.drawArc(oval, 135f, 270f * (pct / 100f), false, arc);
        }

        big.setTextSize(size * 0.20f);
        small.setTextSize(size * 0.070f);
        float cx = oval.centerX();
        float cy = oval.centerY();
        c.drawText(caption, cx, cy + big.getTextSize() * 0.30f, big);
        c.drawText(sub, cx, cy + big.getTextSize() * 0.30f + small.getTextSize() * 2.0f, small);
    }
}
