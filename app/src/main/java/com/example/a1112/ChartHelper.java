package com.example.a1112;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ChartHelper extends View {
    private float[] values = new float[0];
    private Paint linePaint;
    private Paint axisPaint;

    public ChartHelper(Context context) {
        super(context);
        init();
    }

    public ChartHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(0xFF1976D2);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStrokeWidth(2f);
        axisPaint.setColor(0xFF999999);
    }

    public void setValues(float[] values) {
        if (values == null) {
            this.values = new float[0];
        } else {
            this.values = values;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        if (values == null || values.length == 0) {
            return;
        }
        float bottom = h * 0.8f;
        canvas.drawLine(0, bottom, w, bottom, axisPaint);

        float max = 0f;
        for (float v : values) {
            if (v > max) max = v;
        }
        if (max <= 0f) {
            max = 1f;
        }

        int n = values.length;
        float stepX = (n == 1) ? w : (float) w / (n - 1);
        float maxHeight = h * 0.6f;

        float lastX = 0;
        float lastY = bottom - (values[0] / max) * maxHeight;

        for (int i = 1; i < n; i++) {
            float x = stepX * i;
            float y = bottom - (values[i] / max) * maxHeight;
            canvas.drawLine(lastX, lastY, x, y, linePaint);
            lastX = x;
            lastY = y;
        }
    }
}

