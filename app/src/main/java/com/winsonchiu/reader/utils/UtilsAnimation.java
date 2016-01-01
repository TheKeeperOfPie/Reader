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
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
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
public class UtilsAnimation {

    public static final int SNACKBAR_DURATION = 2000;
    public static final long EXPAND_ACTION_DURATION = 150;
    public static final long ALPHA_DURATION = 500;
    public static final long BACKGROUND_DURATION = 500;
    private static final String TAG = UtilsAnimation.class.getCanonicalName();

    public static ValueAnimator animateBackgroundColor(final View view, final int start, final int end) {

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

        return valueAnimator;
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
        animateExpandWithHeight(view, getMeasuredHeight(view, widthRatio), listener, 0);
    }

    public static void animateExpand(final View view,
            float widthRatio,
            OnAnimationEndListener listener,
            long duration) {
        animateExpandWithHeight(view, getMeasuredHeight(view, widthRatio), listener, duration);
    }

    public static void animateExpandWithHeight(final View view,
            final float height,
            final OnAnimationEndListener listener,
            long duration) {

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

        if (duration > 0) {
            animation.setDuration(duration);
        }
        else {
            animation.setDuration((long) (height / speed * 2));
        }
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
                .withLayer()
                .setDuration(duration)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(null)
                ;
    }

    public static void scrollToPositionWithCentering(final int position,
            final RecyclerView recyclerView,
            final RecyclerView.LayoutManager layoutManager,
            boolean animateRipple) {
        scrollToPositionWithCentering(position, recyclerView, layoutManager, 0, 0, animateRipple);
    }

    public static void scrollToPositionWithCentering(final int position,
            final RecyclerView recyclerView,
            final RecyclerView.LayoutManager layoutManager,
            final int paddingTop,
            final int paddingBottom,
            final boolean animateRipple) {
        recyclerView.requestLayout();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final RecyclerView.ViewHolder viewHolder = recyclerView
                        .findViewHolderForAdapterPosition(position);
                int offset = paddingTop;
                if (viewHolder != null) {
                    int difference = recyclerView
                            .getHeight() - paddingBottom - viewHolder.itemView
                            .getHeight();
                    if (difference > 0) {
                        offset = difference / 2;
                    }
                    if (animateRipple) {
                        viewHolder.itemView.setPressed(true);
                        recyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                viewHolder.itemView.setPressed(false);
                            }
                        }, 150);
                    }
                }
                if (layoutManager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
                }
                else {
                    if (layoutManager instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
                    }
                    else {
                        layoutManager.scrollToPosition(position);
                    }
                }
            }
        };

        if (recyclerView.findViewHolderForAdapterPosition(position) == null) {
            layoutManager.scrollToPosition(position);
        }

        recyclerView.post(runnable);
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }

}
