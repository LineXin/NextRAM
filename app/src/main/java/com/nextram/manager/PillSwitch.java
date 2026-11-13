package com.nextram.manager;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class PillSwitch extends View {

    public interface Listener {
        void onChanged(boolean on);
    }

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean checked;
    private float anim;
    private Listener listener;
    private ValueAnimator running;

    public PillSwitch(Context c) {
        super(c);
        setClickable(true);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    public void setChecked(boolean on) {
        checked = on;
        anim = on ? 1f : 0f;
        invalidate();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    private void animateTo(float target) {
        if (running != null) {
            running.cancel();
        }
        running = ValueAnimator.ofFloat(anim, target);
        running.setDuration(160L);
        running.setInterpolator(new DecelerateInterpolator());
        running.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                anim = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        running.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(dp(48), dp(28));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP) {
            checked = !checked;
            animateTo(checked ? 1f : 0f);
            if (listener != null) {
                listener.onChanged(checked);
            }
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float r = h / 2f;
        int accent = Prefs.accent(getContext());
        int accent2 = Prefs.accent2(getContext());
        int trackColor = Ui.blend(Ui.SURFACE_HI, Ui.blend(accent, accent2, 0.5f), anim);
        track.setColor(trackColor);
        canvas.drawRoundRect(0f, 0f, w, h, r, r, track);

        if (anim > 0.02f) {
            int stroke = Ui.blend(0x0026262B, 0x5526262B, anim);
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(dp(1));
            track.setColor(Ui.blend(Ui.STROKE, Ui.blend(accent, accent2, 0.4f), anim));
            canvas.drawRoundRect(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f), r, r, track);
            track.setStyle(Paint.Style.FILL);
            track.setColor(stroke);
        }

        float margin = dp(3);
        float radius = r - margin;
        float cx = margin + radius + (w - 2 * margin - 2 * radius) * anim;
        thumb.setColor(Ui.blend(0xFFE9E9EC, 0xFFFFFFFF, anim));
        canvas.drawCircle(cx, r, radius, thumb);
    }
}
