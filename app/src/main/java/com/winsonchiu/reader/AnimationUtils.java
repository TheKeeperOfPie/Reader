package com.winsonchiu.reader;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.TextView;

import java.lang.reflect.Method;
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

    public static void animateBackgroundColor(final View view, int start, int end) {

        final float[] startHsv = new float[3];
        final float[] endHsv = new float[3];

        Color.colorToHSV(start, startHsv);
        Color.colorToHSV(end, endHsv);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimator.setDuration(BACKGROUND_DURATION);

        final float[] hsv = new float[3];

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                hsv[0] = startHsv[0] + (endHsv[0] - startHsv[0]) * animation.getAnimatedFraction();
                hsv[1] = startHsv[1] + (endHsv[1] - startHsv[1]) * animation.getAnimatedFraction();
                hsv[2] = startHsv[2] + (endHsv[2] - startHsv[2]) * animation.getAnimatedFraction();

                view.setBackgroundColor(Color.HSVToColor(hsv));
            }
        });

        valueAnimator.start();
    }

    public static void animateAlpha(View view, float start, float end) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", start, end);
        objectAnimator.setDuration(ALPHA_DURATION);
        objectAnimator.start();
    }


    public static void animateExpandActions(final ViewGroup viewGroup, boolean skipFirst) {

        final List<View> children = new ArrayList<>(viewGroup.getChildCount());

        for (int index = skipFirst ? 1 : 0; index < viewGroup.getChildCount(); index++) {
            children.add(viewGroup.getChildAt(index));
        }

        Animation animation;
        final int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                viewGroup.getContext()
                        .getResources()
                        .getDisplayMetrics());
        if (viewGroup.getVisibility() == View.VISIBLE) {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

                    for (View view : children) {
                        view.setAlpha(1.0f - interpolatedTime);
                    }

                    viewGroup.getLayoutParams().height = (int) (height * (1.0f - interpolatedTime));
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
                    viewGroup.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
        else {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

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
            viewGroup.getLayoutParams().height = 0;
            viewGroup.requestLayout();
            viewGroup.setVisibility(View.VISIBLE);
        }
        animation.setDuration(EXPAND_ACTION_DURATION);
        animation.setInterpolator(new DecelerateInterpolator());
        viewGroup.startAnimation(animation);
        viewGroup.requestLayout();
    }

    public static void animateExpand(final View view) {

        view.measure(View.MeasureSpec.AT_MOST, View.MeasureSpec.UNSPECIFIED);
        final int height = view.getMeasuredHeight();

        Log.d(TAG, "animatedExpand target height: " + height);

        Animation animation;
        if (view.isShown()) {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = (int) (height * (1.0f - interpolatedTime));
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
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
        else {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = (int) (interpolatedTime * height);
                    view.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            view.getLayoutParams().height = 0;
            view.requestLayout();
            view.setVisibility(View.VISIBLE);
        }
        animation.setDuration(EXPAND_ACTION_DURATION);
        animation.setInterpolator(new DecelerateInterpolator());
        view.startAnimation(animation);
        view.requestLayout();
    }
}
