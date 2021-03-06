/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 */
public interface ControllerListener {
    RecyclerView.Adapter getAdapter();
    void setToolbarTitle(CharSequence title);
    void setRefreshing(boolean refreshing);
    void post(Runnable runnable);
}
