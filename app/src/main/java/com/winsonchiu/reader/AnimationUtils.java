/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.support.v4.graphics.ColorUtils;
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

        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 255);
        valueAnimator.setDuration(BACKGROUND_DURATION);

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int foreground = ColorUtils.setAlphaComponent(end,
                        (Integer) animation.getAnimatedValue());
                int background = ColorUtils.setAlphaComponent(start,
                        255 - (Integer) animation.getAnimatedValue());

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
        view.startAnimation(animation);
        view.requestLayout();
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

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceWidth,
                View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        return view.getMeasuredHeight();
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }

}
