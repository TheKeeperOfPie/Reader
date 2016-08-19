/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

public class ScrollAwareFloatingActionButtonBehavior extends FixedFloatingActionButtonBehavior {

    public static final String TAG = ScrollAwareFloatingActionButtonBehavior.class.getCanonicalName();

    public static final Interpolator INTERPOLATOR = new FastOutLinearInInterpolator();
    public static final long DURATION = 300;

    private boolean mIsAnimatingOut = false;
    private OnVisibilityChangeListener listener;

    public ScrollAwareFloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        this(context, attrs, null);
    }

    public ScrollAwareFloatingActionButtonBehavior(Context context, AttributeSet attrs, OnVisibilityChangeListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout,
            final FloatingActionButton child,
            final View directTargetChild,
            final View target,
            final int nestedScrollAxes) {
        // Ensure we react to vertical scrolling
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
                || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                nestedScrollAxes);
    }

    @Override
    public void onNestedScroll(final CoordinatorLayout coordinatorLayout,
            final FloatingActionButton child,
            final View target,
            final int dxConsumed,
            final int dyConsumed,
            final int dxUnconsumed,
            final int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed);

        if (dyConsumed > 0 && !this.mIsAnimatingOut && child.getVisibility() == View.VISIBLE) {
            animateOut(child);
        }
        else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            animateIn(child);
        }
    }

    public void animateOut(final FloatingActionButton button) {
        if (listener != null) {
            listener.onStartHideFromScroll();
        }

        ViewCompat.animate(button)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(DURATION)
                .setInterpolator(INTERPOLATOR)
                .withLayer()
                .setListener(new ViewPropertyAnimatorListener() {
                    public void onAnimationStart(View view) {
                        ScrollAwareFloatingActionButtonBehavior.this.mIsAnimatingOut = true;
                    }

                    public void onAnimationCancel(View view) {
                        ScrollAwareFloatingActionButtonBehavior.this.mIsAnimatingOut = false;
                    }

                    public void onAnimationEnd(View view) {
                        ScrollAwareFloatingActionButtonBehavior.this.mIsAnimatingOut = false;
                        view.setVisibility(View.GONE);
                        if (listener != null) {
                            listener.onEndHideFromScroll();
                        }
                    }
                });
    }

    public void animateIn(FloatingActionButton button) {
        button.setVisibility(View.VISIBLE);

        ViewCompat.animate(button)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(DURATION)
                .setInterpolator(INTERPOLATOR)
                .setListener(null)
                .withLayer();
    }

    public interface OnVisibilityChangeListener {

        void onStartHideFromScroll();
        void onEndHideFromScroll();

    }
}
