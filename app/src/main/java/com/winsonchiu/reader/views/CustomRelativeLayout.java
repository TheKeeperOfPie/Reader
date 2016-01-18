/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.winsonchiu.reader.utils.OnSizeChangedListener;

/**
 * Created by TheKeeperOfPie on 7/3/2015.
 */
public class CustomRelativeLayout extends RelativeLayout {

    private static final String TAG = CustomRelativeLayout.class.getCanonicalName();
    private float xFraction = 0;
    private float yFraction = 0;
    private ViewTreeObserver.OnPreDrawListener preDrawListener = null;
    private OnSizeChangedListener onSizeChangedListener;
    private OnTouchListener onTouchListener;

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRelativeLayout(Context context) {
        super(context);
    }

    public float getYFraction() {
        if (getHeight() == 0) {
            return 0;
        }
        return getTranslationY() / getHeight();
    }

    public void setYFraction(float fraction) {
        this.yFraction = fraction;

        if (getHeight() == 0) {
            if (preDrawListener == null) {
                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                        setYFraction(yFraction);
                        setXFraction(xFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
            return;
        }

        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    public float getXFraction() {
        if (getWidth() == 0) {
            return 0;
        }
        return getTranslationX() / getWidth();
    }

    public void setXFraction(float fraction) {
        this.xFraction = fraction;

        if (getWidth() == 0) {
            if (preDrawListener == null) {
                preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                        setYFraction(yFraction);
                        setXFraction(xFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
            return;
        }

        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }

    public void setDispatchTouchListener(OnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (onTouchListener != null) {
            onTouchListener.onTouch(this, ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (onSizeChangedListener != null) {
            onSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener onSizeChangedListener) {
        this.onSizeChangedListener = onSizeChangedListener;
    }
}
