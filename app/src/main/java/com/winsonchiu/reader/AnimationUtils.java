package com.winsonchiu.reader;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v4.graphics.ColorUtils;
import android.text.Layout;
import android.text.StaticLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public class AnimationUtils {

    public static final long EXPAND_ACTION_DURATION = 150;
    public static final long ALPHA_DURATION = 500;
    public static final long BACKGROUND_DURATION = 500;
    public static final long MOVE_DURATION = 350;
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

    public static void animateBackgroundColorHsv(final View view, int start, int end) {

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
        if (viewGroup.isShown()) {
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

    public static void animateExpand(final View view, final int height) {

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

    public static void animateExpand(final View view) {

        final int height = getMeasuredHeight(view);

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

    /*
     * Code taken from http://stackoverflow.com/questions/19908003/getting-height-of-text-view-before-rendering-to-layout
     *
     * by support_ms and Hugo Gresse
     */
    public static int getMeasuredHeight(View view) {
        WindowManager windowManager =
                (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        int deviceWidth = size.x;

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        return view.getMeasuredHeight();
    }
}
