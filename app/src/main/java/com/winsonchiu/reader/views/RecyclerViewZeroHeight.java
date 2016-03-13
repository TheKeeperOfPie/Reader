/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by TheKeeperOfPie on 12/28/2015.
 */
public class RecyclerViewZeroHeight extends RecyclerView {

    public RecyclerViewZeroHeight(Context context) {
        super(context);
    }

    public RecyclerViewZeroHeight(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewZeroHeight(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public int getMinimumHeight() {
        return 0;
    }
}
