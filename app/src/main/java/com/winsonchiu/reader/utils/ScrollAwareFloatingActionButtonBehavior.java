/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;
/*
    Taken from CheeseSquare demo app, located at
    https://github.com/ianhanniballake/cheesesquare/blob/aefa8b57e61266e4ad51bef36e669d69f7fd749c/app/src/main/java/com/support/android/designlibdemo/ScrollAwareFABBehavior.java
 */

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

public class ScrollAwareFloatingActionButtonBehavior extends FloatingActionButton.Behavior {

    public static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();
    public static final long DURATION = 300;
    private static final String TAG = ScrollAwareFloatingActionButtonBehavior.class.getCanonicalName();

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
            // User scrolled down and the FAB is currently visible -> hide the FAB
            animateOut(child);
        }
        else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            // User scrolled up and the FAB is currently not visible -> show the FAB
            animateIn(child);
        }
    }

    // Same animation that FloatingActionButton.Behavior uses to hide the FAB when the AppBarLayout exits
    private void animateOut(final FloatingActionButton button) {
        if (listener != null) {
            listener.onStartHideFromScroll();
        }
        ViewCompat.animate(button)
                .scaleX(0.0F)
                .scaleY(0.0F)
                .alpha(0.0F)
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
                })
                .start();
    }

    // Same animation that FloatingActionButton.Behavior uses to show the FAB when the AppBarLayout enters
    private void animateIn(FloatingActionButton button) {
        button.setVisibility(View.VISIBLE);
        ViewCompat.animate(button)
                .scaleX(1.0F)
                .scaleY(1.0F)
                .alpha(1.0F)
                .setDuration(DURATION)
                .setInterpolator(INTERPOLATOR)
                .setListener(null)
                .withLayer()
                .start();
    }

    public interface OnVisibilityChangeListener {

        void onStartHideFromScroll();
        void onEndHideFromScroll();

    }
}
