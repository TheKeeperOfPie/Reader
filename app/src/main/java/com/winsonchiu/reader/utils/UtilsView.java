/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 4/3/2016.
 */
public class UtilsView {

    public static int getContentWidth(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.getWidth() - layoutManager.getPaddingStart() - layoutManager.getPaddingEnd();
    }

}
