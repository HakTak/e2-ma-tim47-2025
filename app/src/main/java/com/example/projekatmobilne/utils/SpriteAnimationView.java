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

import android.util.Log;
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
    private int frameDurationMs = 200;
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
        setLayerType(LAYER_TYPE_SOFTWARE, null);
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
            handler.postDelayed(() -> {
                stopAnimation();
                startIdleAnimation(); // ← prvo vrati idle
                if (callback != null) callback.onAnimationFinished(); // ← callback može ga pregaziti
            }, frameDurationMs);
            return;
        }

        handler.postDelayed(() -> {
            currentFrame++;
            runOneShotFrame(totalFrames, callback);
        }, frameDurationMs);
    }

    // One-shot BEZ povratka na idle (za death animaciju)
    private Handler finalAnimHandler = new Handler(Looper.getMainLooper());

    public void playFinalAnimation(Bitmap sheet, int frames, AnimationCallback callback) {
        // Zaustavi SVE - i idle i bilo sta drugo
        isAnimating = false;
        isPendingFrame = false;
        handler.removeCallbacksAndMessages(null);
        finalAnimHandler.removeCallbacksAndMessages(null);

        currentSheet = sheet;
        frameCount = frames;
        frameWidth = sheet.getWidth() / frames;
        frameHeight = sheet.getHeight();
        currentFrame = 0;

        runFinalFrame(frames, callback);
    }

    private void runFinalFrame(int totalFrames, AnimationCallback callback) {
       // Log.d("SPRITE", "runFinalFrame: currentFrame=" + currentFrame + "/" + (totalFrames-1));

        invalidate(); // crta TRENUTNI frejm

        if (currentFrame >= totalFrames - 1) {
     //       Log.d("SPRITE", "Animacija gotova, pozivam callback");
            finalAnimHandler.postDelayed(() -> {
                if (callback != null) callback.onAnimationFinished();
            }, 1000);
            return;
        }

        finalAnimHandler.postDelayed(() -> {
            currentFrame++;
            runFinalFrame(totalFrames, callback);
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

      //  Log.d("SPRITE_DRAW", "onDraw: frame=" + currentFrame + " frameWidth=" + frameWidth + " sheetWidth=" + currentSheet.getWidth());

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