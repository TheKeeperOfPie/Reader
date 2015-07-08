/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.views;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {

    private float minScrollY;
    private float initialY;
    private boolean measured;
    private boolean refreshing;

    public CustomSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public CustomSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        minScrollY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80,
                context.getResources()
                        .getDisplayMetrics());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:

                float distanceY = Math.abs(event.getY() - initialY);

                if (distanceY < minScrollY) {
                    return false;
                }
        }

        return super.onInterceptTouchEvent(event);
    }

    public void setMinScrollY(float minScrollY) {
        this.minScrollY = minScrollY;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!measured) {
            measured = true;
            setRefreshing(refreshing);
        }
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        if (measured) {
            super.setRefreshing(refreshing);
        }
        else {
            this.refreshing = refreshing;
        }
    }
}
