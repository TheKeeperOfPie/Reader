/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.views;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.Scroller;

import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.winsonchiu.reader.utils.UtilsImage;

/**
 * Created by TheKeeperOfPie on 3/5/2016.
 */
public class ImageViewZoom extends ImageView {

    private static final String TAG = ImageViewZoom.class.getCanonicalName();

    private Matrix matrix = new Matrix();

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetectorCompat gestureDetector;
    private Scroller scroller;

    private float focusX;
    private float focusY;
    private float scaleStart;
    private float scaleCurrent;
    private float translationX;
    private float translationY;
    private float translationMinX;
    private float translationMaxX;
    private float translationMinY;
    private float translationMaxY;
    private float contentWidth;
    private float contentHeight;
    private Runnable runnableScroll = new Runnable() {
        @Override
        public void run() {
            boolean animating = scroller.computeScrollOffset();

            setTranslation(scroller.getCurrX(), scroller.getCurrY());

            if (animating) {
                post(this);
            }
        }
    };

    private Listener listener;

    public ImageViewZoom(Context context) {
        super(context);
        initialize();
    }

    public ImageViewZoom(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ImageViewZoom(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImageViewZoom(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void initialize() {
        setClickable(true);
        scroller = new Scroller(getContext());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.d(TAG, "onScale() called with: " + "detector = [" + detector + "]");

                Log.d(TAG, "onScale() called with: " + "focusX = [" + detector.getFocusX() + "], focusY = [" + detector.getFocusY() + "], width = [" + getWidth() + "], height = [" + getHeight() + "]");

//                float translationX = detector.getFocusX() * (translationMaxX - translationMinX) / getWidth() + translationMinX;
//                float translationY = detector.getFocusY() * (translationMaxY - translationMinY) / getHeight() + translationMinY;
//                setTranslation(translationX, translationY);

                applyScaleFactor(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
                return true;
            }
        });

        gestureDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener(){

            private int pointerCount;

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.d(TAG, "onScroll() called with: " + "translationX = [" + translationX + "], translationMinX = [" + translationMinX + "], translationMaxX = [" + translationMaxX + "], scaleStart = [" + scaleStart + "], scaleCurrent = [" + scaleCurrent + "], contentWidth = [" + contentWidth + "]");
                Log.d(TAG, "onScroll() called with relative: " + "x = [" + getRelativeX() + "], y = [" + getRelativeY() + "]");

                scroller.forceFinished(true);

                if (pointerCount == 1) {
                    applyTranslation(-distanceX, -distanceY);
                }

                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                scroller.forceFinished(true);

                if (pointerCount == 1) {
                    Log.d(TAG, "onFling() called with: " + "e1 = [" + e1 + "], e2 = [" + e2 + "], velocityX = [" + velocityX + "], velocityY = [" + velocityY + "]");
                    scroller.fling((int) translationX, (int) translationY, (int) velocityX, (int) velocityY, (int) translationMinX, (int) translationMaxX, (int) translationMinY, (int) translationMaxY);
                    post(runnableScroll);
                }

                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onDown(MotionEvent e) {
                pointerCount = e.getPointerCount();

                return super.onDown(e);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setImageDrawable(final Drawable drawable) {
        if (drawable instanceof BitmapDrawable || drawable instanceof GlideDrawable) {
            contentWidth = drawable.getIntrinsicWidth();
            contentHeight = drawable.getIntrinsicHeight();

            UtilsImage.checkMaxTextureSize(getHandler(), new Runnable() {
                @Override
                public void run() {
                    if (contentWidth > UtilsImage.getMaxTextureSize()
                            || contentHeight > UtilsImage.getMaxTextureSize()) {
                        listener.onTextureSizeExceeded();
                        return;
                    }

                    int startHeight = 0;
                    int targetHeight = (int) (getWidth() * contentHeight / contentWidth);

                    listener.onBeforeContentLoad(getWidth(), targetHeight);

                    setScaleType(ScaleType.MATRIX);
                    ImageViewZoom.super.setImageDrawable(drawable);

                    scaleStart = getWidth() / contentWidth;
                    translationX = (scaleCurrent - 1) * contentWidth / 2;
                    translationY = (scaleCurrent - 1) * contentHeight / 2;
                    setScaleFactor(0, 0, scaleStart);

                    if (getParent() instanceof View) {
                        startHeight = ((View) getParent()).getHeight();
                    }

                    ValueAnimator valueAnimator = ValueAnimator.ofInt(startHeight, targetHeight);
                    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            getLayoutParams().height = (int) animation.getAnimatedValue();
                            requestLayout();
                        }
                    });
                    valueAnimator.start();
                }
            });
        }
        else {
            super.setImageDrawable(drawable);
        }
    }

    private void applyTranslation(float translationX, float translationY) {
        this.translationX += translationX;
        this.translationY += translationY;
        setTranslation(this.translationX, this.translationY);
    }

    private void setTranslation(float translationX, float translationY) {
        this.translationX = translationX;
        this.translationY = translationY;
        calculateMatrix();
    }

    private void applyScaleFactor(float focusX, float focusY, float scaleFactor) {
        if (scaleCurrent * scaleFactor > scaleStart) {
            scaleCurrent *= scaleFactor;
            setScaleFactor(focusX, focusY, scaleCurrent);
        }
    }

    private void setScaleFactor(float focusX, float focusY, float scaleFactor) {
        this.focusX = focusX;
        this.focusY = focusY;
        scaleCurrent = scaleFactor;
        translationMaxX = (scaleCurrent - 1) * contentWidth / 2;
        translationMinX = translationMaxX - (scaleCurrent - scaleStart) * contentWidth;
        translationMaxY = (scaleCurrent - 1) * contentHeight / 2;
        translationMinY = translationMaxY - (scaleCurrent - scaleStart) * contentHeight;
        calculateMatrix();
    }

    private void calculateMatrix() {
        restrictVariables();

        matrix.reset();
        matrix.postTranslate(-contentWidth / 2, -contentHeight / 2);
        matrix.postScale(scaleCurrent, scaleCurrent);
        matrix.postTranslate(contentWidth / 2, contentHeight / 2);
        matrix.postTranslate(translationX, translationY);
        setImageMatrix(matrix);
    }

    private void restrictVariables() {
        scaleCurrent = scaleCurrent < scaleStart ? scaleStart : scaleCurrent;
        translationX = translationX > translationMinX ? translationX : translationMinX;
        translationX = translationX < translationMaxX ? translationX : translationMaxX;
        translationY = translationY > translationMinY ? translationY : translationMinY;
        translationY = translationY < translationMaxY ? translationY : translationMaxY;
    }

    public float getRelativeX() {
        return (translationMaxX - translationX) / (scaleCurrent * contentWidth);
    }

    public float getRelativeY() {
        return (translationMaxY - translationY) / (scaleCurrent * contentHeight);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return super.canScrollHorizontally(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (scaleCurrent - scaleStart < 0.01f) {
            return false;
        }

        if (direction > 0) {
            return translationY > translationMinY + 1;
        }
        else {
            return translationY < translationMaxY - 1;
        }
    }

    public interface Listener {
        void onTextureSizeExceeded();
        void onBeforeContentLoad(int width, int height);
    }
}
