/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.adapter;

import com.winsonchiu.reader.utils.DisallowListener;

/**
 * Created by TheKeeperOfPie on 4/16/2016.
 */
public interface AdapterListener extends AdapterLoadMoreListener, DisallowListener {
    void scrollAndCenter(int position, int height);
    void hideToolbar();
    void clearDecoration();
}
