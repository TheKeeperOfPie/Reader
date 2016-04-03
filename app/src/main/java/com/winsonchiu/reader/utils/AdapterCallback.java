/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 4/3/2016.
 */
public interface AdapterCallback {
    @Nullable RecyclerView getRecyclerView();
}
