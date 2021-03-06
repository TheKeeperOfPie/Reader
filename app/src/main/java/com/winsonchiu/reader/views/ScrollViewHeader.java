package com.winsonchiu.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Created by TheKeeperOfPie on 8/19/2015.
 */
public class ScrollViewHeader extends ScrollView {

    private OnTouchListener onTouchListener;

    public ScrollViewHeader(Context context) {
        super(context);
    }

    public ScrollViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ScrollViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int statusBarHeight = 0;
        int heightResourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (heightResourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(heightResourceId);
        }

        super.onMeasure(widthMeasureSpec, (int) (widthMeasureSpec / 16f * 9f) + statusBarHeight);
        setMeasuredDimension(widthMeasureSpec, (int) (widthMeasureSpec / 16f * 9f) + statusBarHeight);
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
}
