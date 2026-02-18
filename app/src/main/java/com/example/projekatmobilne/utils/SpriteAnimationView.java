package com.example.projekatmobilne.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import android.view.View;

public class SpriteAnimationView extends View {

    private Bitmap idleSheet;
    private Bitmap currentSheet;
    private int frameCount;
    private int currentFrame = 0;
    private int idleFrameCount; // ← NOVO

    private int frameWidth;
    private int frameHeight;
    private Paint paint;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int frameDurationMs = 150;
    private boolean isAnimating = false;
    private boolean isPendingFrame = false;
    private float scaleFactor = 1.5f;

    // Callback kad se one-shot animacija završi
    public interface AnimationCallback {
        void onAnimationFinished();
    }

    public SpriteAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setScaleFactor(float scale) {
        this.scaleFactor = scale;
        invalidate();
    }

    // Postavlja idle sheet i startuje loop
    public void setIdleSheet(Bitmap bitmap, int frames) {
        this.idleSheet = bitmap;
        this.currentSheet = bitmap;
        this.frameCount = frames;
        this.frameWidth = bitmap.getWidth() / frames;
        this.idleFrameCount = frames; // ← NOVO

        this.frameHeight = bitmap.getHeight();
        invalidate();
    }

    public void startIdleAnimation() {
        if (isAnimating) return;
        currentSheet = idleSheet;
        frameCount = idleFrameCount; // ← koristi sačuvani broj
        frameWidth = idleSheet.getWidth() / idleFrameCount;
        frameHeight = idleSheet.getHeight();
        isAnimating = true;
        currentFrame = 0;
        scheduleNextFrame();
    }

    // Pokreće one-shot animaciju, pa se vraća na idle
    public void playOneShotAnimation(Bitmap sheet, int frames, AnimationCallback callback) {
        // Zaustavi trenutnu animaciju
        stopAnimation();

        currentSheet = sheet;
        frameCount = frames;
        frameWidth = sheet.getWidth() / frames;
        frameHeight = sheet.getHeight();
        currentFrame = 0;
        isAnimating = true;

        runOneShotFrame(frames, callback);
    }

    private void runOneShotFrame(int totalFrames, AnimationCallback callback) {
        if (!isAnimating) return;
        invalidate();

        if (currentFrame >= totalFrames - 1) {
            // Animacija gotova, vrati se na idle
            handler.postDelayed(() -> {
                currentFrame = 0;
                if (callback != null) callback.onAnimationFinished();
                stopAnimation();
                startIdleAnimation();
            }, frameDurationMs);
            return;
        }

        handler.postDelayed(() -> {
            currentFrame++;
            runOneShotFrame(totalFrames, callback);
        }, frameDurationMs);
    }

    public void stopAnimation() {
        isAnimating = false;
        isPendingFrame = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void scheduleNextFrame() {
        if (!isAnimating || isPendingFrame) return;
        isPendingFrame = true;
        handler.postDelayed(() -> {
            isPendingFrame = false;
            if (!isAnimating) return;
            currentFrame = (currentFrame + 1) % frameCount;
            invalidate();
            scheduleNextFrame();
        }, frameDurationMs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentSheet == null) return;

        Rect src = new Rect(
                currentFrame * frameWidth, 0,
                (currentFrame + 1) * frameWidth, frameHeight
        );

        float scaledWidth = getWidth() * scaleFactor;
        float scaledHeight = getHeight() * scaleFactor;
        float offsetX = (getWidth() - scaledWidth) / 2f;
        float offsetY = (getHeight() - scaledHeight) / 2f;

        RectF dst = new RectF(offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight);
        canvas.drawBitmap(currentSheet, src, dst, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}