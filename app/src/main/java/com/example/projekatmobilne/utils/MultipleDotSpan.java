package com.example.projekatmobilne.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

public class MultipleDotSpan implements LineBackgroundSpan {
    private final float radius;
    private final int[] colors;

    public MultipleDotSpan(float radius, int[] colors) {
        this.radius = radius;
        this.colors = colors;
    }

    @Override
    public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
        int total = colors.length;
        int leftOffset = 0;
        if (total > 1) {
            leftOffset = (int) (radius * 1.5f * (total - 1));
        }

        int oldColor = paint.getColor();
        for (int i = 0; i < total; i++) {
            paint.setColor(colors[i]);
            canvas.drawCircle((left + right) / 2 - leftOffset + i * radius * 3, bottom + radius, radius, paint);
        }
        paint.setColor(oldColor);
    }
}