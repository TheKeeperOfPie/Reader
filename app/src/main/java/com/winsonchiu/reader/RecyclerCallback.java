/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 6/24/2015.
 */
public interface RecyclerCallback {
    void scrollTo(int position);
    int getRecyclerHeight();
    RecyclerView.LayoutManager getLayoutManager();
}
