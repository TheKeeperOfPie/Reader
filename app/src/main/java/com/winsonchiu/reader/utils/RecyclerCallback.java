/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.v7.widget.RecyclerView;

import com.bumptech.glide.RequestManager;

/**
 * Created by TheKeeperOfPie on 6/24/2015.
 */
public interface RecyclerCallback {
    int getRecyclerHeight();
    RecyclerView.LayoutManager getLayoutManager();
    void scrollTo(int position);
    void scrollAndCenter(int position, int height);
    void hideToolbar();
    void onReplyShown();
    RequestManager getRequestManager();
}
