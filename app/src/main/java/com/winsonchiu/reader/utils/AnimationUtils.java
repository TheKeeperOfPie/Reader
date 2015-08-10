/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public class AnimationUtils {

    public static final long EXPAND_ACTION_DURATION = 150;
    public static final long ALPHA_DURATION = 500;
    public static final long BACKGROUND_DURATION = 500;
    private static final String TAG = AnimationUtils.class.getCanonicalName();

    public static void animateBackgroundColor(final View view, final int start, final int end) {

        final int alphaTarget = Color.alpha(end);

        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, alphaTarget);
        valueAnimator.setDuration(BACKGROUND_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int foreground = ColorUtils.setAlphaComponent(end,
                        (Integer) animation.getAnimatedValue());
                int background = ColorUtils.setAlphaComponent(start,
                        alphaTarget - (Integer) animation.getAnimatedValue());

                view.setBackgroundColor(ColorUtils.compositeColors(foreground, background));
            }
        });

        valueAnimator.start();
    }

    public static void animateExpandActionsWithHeight(final ViewGroup viewGroup,
            boolean skipFirst,
            final int height) {

        final List<View> children = new ArrayList<>(viewGroup.getChildCount());

        for (int index = skipFirst ? 1 : 0; index < viewGroup.getChildCount(); index++) {
            children.add(viewGroup.getChildAt(index));
        }

        final boolean isShown = viewGroup.isShown();
        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                viewGroup.getContext()
                        .getResources()
                        .getDisplayMetrics());

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                interpolatedTime = isShown ? 1.0f - interpolatedTime : interpolatedTime;

                for (View view : children) {
                    view.setAlpha(interpolatedTime);
                }
                viewGroup.getLayoutParams().height = (int) (interpolatedTime * height);

                viewGroup.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isShown) {
                    viewGroup.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (!isShown) {
            viewGroup.getLayoutParams().height = 0;
            viewGroup.setVisibility(View.VISIBLE);
        }

        animation.setDuration((long) (height / speed * 2));
        viewGroup.startAnimation(animation);
        viewGroup.requestLayout();

    }

    public static void animateExpand(final View view,
            float widthRatio,
            OnAnimationEndListener listener) {
        animateExpandWithHeight(view, getMeasuredHeight(view, widthRatio), listener);
    }

    public static void animateExpandWithHeight(final View view,
            final float height,
            final OnAnimationEndListener listener) {

        final boolean isShown = view.isShown();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                interpolatedTime = isShown ? 1.0f - interpolatedTime : interpolatedTime;
                view.getLayoutParams().height = (int) (interpolatedTime * height);
                view.requestLayout();

            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "onAnimationStart");
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isShown) {
                    view.setVisibility(View.GONE);
                }
                if (listener != null) {
                    listener.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (!isShown) {
            view.getLayoutParams().height = 0;
            view.requestLayout();
            view.setVisibility(View.VISIBLE);
        }

        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                view.getContext()
                        .getResources()
                        .getDisplayMetrics());

        animation.setDuration((long) (height / speed * 2));
        view.setVisibility(View.VISIBLE);
        view.requestLayout();
        view.startAnimation(animation);
        view.invalidate();
    }

    /*
     * Code taken from http://stackoverflow.com/questions/19908003/getting-height-of-text-view-before-rendering-to-layout
     *
     * by support_ms and Hugo Gresse
     */
    public static int getMeasuredHeight(View view, float widthRatio) {
        WindowManager windowManager =
                (WindowManager) view.getContext()
                        .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        int deviceWidth = (int) (size.x * widthRatio);

        Log.d(TAG, "deviceWidth: " + deviceWidth);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceWidth,
                View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        return view.getMeasuredHeight();
    }

    public static ViewPropertyAnimatorCompat shrinkAndFadeOut(View view, long duration) {
        return ViewCompat.animate(view)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {

                    }

                    @Override
                    public void onAnimationEnd(View view) {

                    }

                    @Override
                    public void onAnimationCancel(View view) {

                    }
                });
    }

    /**
     * Code taken from com.android.systemui.recents.misc.Utilities to properly
     * set recents card icon color
     * @param background
     * @param foreground
     * @return
     */
    public static float computeContrast(int background, int foreground) {

        float bgR = Color.red(background) / 255f;
        float bgG = Color.green(background) / 255f;
        float bgB = Color.blue(background) / 255f;
        bgR = (bgR < 0.03928f) ? bgR / 12.92f : (float) Math.pow((bgR + 0.055f) / 1.055f, 2.4f);
        bgG = (bgG < 0.03928f) ? bgG / 12.92f : (float) Math.pow((bgG + 0.055f) / 1.055f, 2.4f);
        bgB = (bgB < 0.03928f) ? bgB / 12.92f : (float) Math.pow((bgB + 0.055f) / 1.055f, 2.4f);
        float bgL = 0.2126f * bgR + 0.7152f * bgG + 0.0722f * bgB;

        float fgR = Color.red(foreground) / 255f;
        float fgG = Color.green(foreground) / 255f;
        float fgB = Color.blue(foreground) / 255f;
        fgR = (fgR < 0.03928f) ? fgR / 12.92f : (float) Math.pow((fgR + 0.055f) / 1.055f, 2.4f);
        fgG = (fgG < 0.03928f) ? fgG / 12.92f : (float) Math.pow((fgG + 0.055f) / 1.055f, 2.4f);
        fgB = (fgB < 0.03928f) ? fgB / 12.92f : (float) Math.pow((fgB + 0.055f) / 1.055f, 2.4f);
        float fgL = 0.2126f * fgR + 0.7152f * fgG + 0.0722f * fgB;

        return Math.abs((fgL + 0.05f) / (bgL + 0.05f));
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }

}
