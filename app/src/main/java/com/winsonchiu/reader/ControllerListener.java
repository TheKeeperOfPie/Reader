package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 */
public interface ControllerListener {
    RecyclerView.Adapter getAdapter();
    void setToolbarTitle(CharSequence title);
    void setRefreshing(boolean refreshing);
}
