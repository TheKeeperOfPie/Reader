/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.winsonchiu.reader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public class UtilsAnimation {

    public static final String TAG = UtilsAnimation.class.getCanonicalName();

    public static final int SNACKBAR_DURATION = 2000;
    public static final long EXPAND_ACTION_DURATION = 150;
    public static final long ALPHA_DURATION = 500;
    public static final long BACKGROUND_DURATION = 500;

    private static final double ANIMATION_MIN_DURATION = 150;
    private static final double ANIMATION_MAX_DURATION = 1000;
    public static final long DELAY_RIPPLE = 250;

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

                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.requestLayout();

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

    public static void waitForRipple(View view, Runnable runnable) {
        view.postOnAnimationDelayed(runnable, DELAY_RIPPLE);
    }

    public static void waitForRipple(Handler handler, Runnable runnable) {
        handler.postDelayed(runnable, DELAY_RIPPLE);
    }

    public static ViewPropertyAnimatorCompat shrinkAndFadeOut(View view, long duration) {
        return ViewCompat.animate(view)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .withLayer()
                .setDuration(duration)
                .setInterpolator(new FastOutSlowInInterpolator())
                .setListener(null);
    }

    public static void scrollToPositionWithCentering(final int position,
            final RecyclerView recyclerView,
            boolean animateRipple) {
        scrollToPositionWithCentering(position, recyclerView, 0, 0, 0, animateRipple);
    }

    public static void scrollToPositionWithCentering(final int position,
            final RecyclerView recyclerView,
            final int paddingTop,
            final int paddingBottom,
            final boolean animateRipple) {
        scrollToPositionWithCentering(position, recyclerView, 0, paddingTop, paddingBottom, animateRipple);
    }

    public static void scrollToPositionWithCentering(final int position,
            final RecyclerView recyclerView,
            final int targetHeight,
            final int paddingTop,
            final int paddingBottom,
            final boolean animateRipple) {
        recyclerView.requestLayout();

        Runnable runnable = () -> {
            final RecyclerView.ViewHolder viewHolder = recyclerView
                    .findViewHolderForAdapterPosition(position);
            int offset = paddingTop;

            if (viewHolder != null) {
                int viewHeight = targetHeight > 0 ? targetHeight : viewHolder.itemView.getHeight();

                int difference = recyclerView.getHeight() - paddingBottom - viewHeight;
                if (difference > 0) {
                    offset = difference / 2;
                }
                if (animateRipple) {
                    viewHolder.itemView.setPressed(true);
                    recyclerView.postOnAnimationDelayed(() -> viewHolder.itemView.setPressed(false), 150);
                }

                recyclerView.smoothScrollBy(0, viewHolder.itemView.getTop() - offset);
            }
        };

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (recyclerView.findViewHolderForAdapterPosition(position) == null
                && layoutManager != null) {
            layoutManager.scrollToPosition(position);
        }

        recyclerView.post(runnable);
    }

    public static void clearAnimation(View... views) {
        for (View view : views) {
            if (view.getAnimation() != null) {
                view.getAnimation().cancel();
            }

            view.clearAnimation();
        }
    }

    public static void animateExpandRecyclerItemView(final View view,
            final View viewParent,
            final View viewMaskStart,
            final View viewMaskEnd,
            final int targetWidth,
            final long duration,
            @Nullable final OnAnimationEndListener callback) {
        Log.d(TAG, "animateExpandRecyclerItemView() called with: " + "view = [" + view + "], viewParent = [" + viewParent + "], viewMaskStart = [" + viewMaskStart + "], viewMaskEnd = [" + viewMaskEnd + "], targetWidth = [" + targetWidth + "], duration = [" + duration + "], callback = [" + callback + "]", new Exception());
        Object tag = view.getTag(R.id.key_value_animator);

        if (tag instanceof ValueAnimator) {
            ((ValueAnimator) tag).cancel();
        }

        ValueAnimator valueAnimator = getExpandRecyclerItemViewAnimatorInternal(view, viewParent, viewMaskStart, viewMaskEnd, targetWidth, duration, callback);
        view.setTag(valueAnimator);
        valueAnimator.start();
    }

    private static ValueAnimator getExpandRecyclerItemViewAnimatorInternal(final View view,
            final View viewParent,
            final View viewMaskStart,
            final View viewMaskEnd,
            final int targetWidth,
            final long duration,
            @Nullable final OnAnimationEndListener callback) {

        final int startWidth = view.getVisibility() == View.GONE ? 0 : view.getWidth();
        final float startX = view.getTranslationX();
        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, view.getContext().getResources().getDisplayMetrics());

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);

        if (startWidth == targetWidth) {
            return valueAnimator;
        }

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float interpolatedTime = animation.getAnimatedFraction();
                if (interpolatedTime >= 0.99f) {
                    resetValues();
                }
                else {
                    int translationX = (int) ((1f - interpolatedTime) * startX);
                    int width = (int) (startWidth + interpolatedTime * (targetWidth - startWidth));
                    if (viewMaskStart != null) {
                        Log.d(TAG, "onAnimationUpdate() called with: " + "viewMaskEnd = [" + (viewParent.getWidth() - width - translationX) + "]");
                        viewMaskStart.getLayoutParams().width = translationX;
                        viewMaskEnd.getLayoutParams().width = viewParent.getWidth() - width - translationX;
                    }

                    view.getLayoutParams().width = width;
                    view.setTranslationX(translationX);
                    view.requestLayout();
                }
            }

            private void resetValues() {
                if (viewMaskStart != null) {
                    Log.d(TAG, "resetValues() called with: " + "viewMaskEnd = [" + 0 + "]");
                    viewMaskStart.getLayoutParams().width = 0;
                    viewMaskEnd.getLayoutParams().width = 0;
                }

                view.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setTranslationX(0);
                view.requestLayout();

                view.setTag(R.id.key_value_animator, null);
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onEnd();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onEnd();
            }

            private void onEnd() {
                if (viewMaskStart != null) {
                    Log.d(TAG, "onEnd() called with: " + "viewMaskEnd = [" + 0 + "]");
                    viewMaskStart.getLayoutParams().width = 0;
                    viewMaskEnd.getLayoutParams().width = 0;
                }

                view.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setTranslationX(0);
                view.requestLayout();

                if (callback != null) {
                    callback.onAnimationEnd();
                }

                view.setTag(R.id.key_value_animator, null);
            }
        });

        valueAnimator.setDuration(duration > 0 ? duration : (long) ((targetWidth - startWidth) / speed * 2));

        view.setTag(R.id.key_value_animator, valueAnimator);

        return valueAnimator;
    }

    /**
     * Animate a view's height from its current height to its WRAP_CONTENT measured height
     * @param widthAtMost the maximum width of the view to be measured in
     * @param duration 0 for size scaled duration, or just plain duration otherwise
     */
    public static void animateExpandHeight(final View view,
            final int widthAtMost,
            final long duration,
            @Nullable final OnAnimationEndListener callback) {
        if (view.getAnimation() != null && !view.getAnimation().hasEnded()) {
            view.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.startAnimation(getExpandHeightAnimationInternal(view, widthAtMost, duration, callback));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        } else {
            view.startAnimation(getExpandHeightAnimationInternal(view, widthAtMost, duration, callback));
        }
    }

    private static Animation getExpandHeightAnimationInternal(final View view,
            final int widthAtMost,
            final long duration,
            @Nullable final OnAnimationEndListener callback) {
        final int startHeight = view.getVisibility() == View.GONE ? 0 : view.getHeight();
        final int targetHeight = getMeasuredHeightWithWidth(view, widthAtMost);
        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, view.getContext().getResources().getDisplayMetrics());

        if (startHeight == targetHeight) {
            return new Animation() {};
        }

        Animation animationExpand = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime >= 0.99f) {
                    view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                else {
                    view.getLayoutParams().height = (int) (startHeight + interpolatedTime * (targetHeight - startHeight));
                }

                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animationExpand.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.requestLayout();

                if (callback != null) {
                    callback.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (view.getVisibility() == View.GONE) {
            view.getLayoutParams().height = 0;
        }
        view.setVisibility(View.VISIBLE);

        animationExpand.setDuration(duration > 0 ? duration : calculateDuration(targetHeight - startHeight, speed));

        return animationExpand;
    }

    /**
     * Animate a view's height from its current height to 0
     * @param duration 0 for size scaled duration, or just plain duration otherwise
     */
    public static void animateCollapseHeight(final View view,
            final long duration,
            @Nullable final OnAnimationEndListener callback) {
        if (view.getAnimation() != null && !view.getAnimation().hasEnded()) {
            view.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.startAnimation(getCollapseHeightAnimationInternal(view, duration, callback));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        } else {
            view.startAnimation(getCollapseHeightAnimationInternal(view, duration, callback));
        }
    }

    private static Animation getCollapseHeightAnimationInternal(final View view,
            final long duration,
            @Nullable final OnAnimationEndListener callback) {
        final int height = view.getHeight();
        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, view.getContext().getResources().getDisplayMetrics());

        if (height == 0) {
            return new Animation() {};
        }

        Animation animationCollapse = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime >= 0.99f) {
                    view.getLayoutParams().height = 0;
                }
                else {
                    view.getLayoutParams().height = (int) ((1f - interpolatedTime) * height);
                }

                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animationCollapse.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.getLayoutParams().height = 0;
                view.setVisibility(View.GONE);

                if (callback != null) {
                    callback.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animationCollapse.setDuration(duration > 0 ? duration : calculateDuration(height, speed));

        view.setVisibility(View.VISIBLE);

        return animationCollapse;
    }

    private static long calculateDuration(int height, float speed) {
        return (long) Math.max(0, Math.min(Math.max(height / speed * 2, ANIMATION_MIN_DURATION), ANIMATION_MAX_DURATION));
    }

    public static int getMeasuredHeightWithWidth(View view, int width) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        return view.getMeasuredHeight();
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }

}
