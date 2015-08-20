package com.winsonchiu.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ScrollView;

/**
 * Created by TheKeeperOfPie on 8/19/2015.
 */
public class ScrollViewHeader extends ScrollView {

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {int result = 0;
        int statusBarHeight = 0;
        int heightResourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (heightResourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(heightResourceId);
        }

        super.onMeasure(widthMeasureSpec, (int) (widthMeasureSpec / 16f * 9f) + statusBarHeight);
        setMeasuredDimension(widthMeasureSpec, (int) (widthMeasureSpec / 16f * 9f) + statusBarHeight);
    }

}
