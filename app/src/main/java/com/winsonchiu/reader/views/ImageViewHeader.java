/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by TheKeeperOfPie on 3/15/2015.
 */
public class ImageViewHeader extends ImageView {

    public ImageViewHeader(Context context) {
        super(context);
    }

    public ImageViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImageViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, (int) (widthMeasureSpec / 16f * 9f));
        setMeasuredDimension(widthMeasureSpec, (int) (widthMeasureSpec / 16f * 9f));
    }
}
