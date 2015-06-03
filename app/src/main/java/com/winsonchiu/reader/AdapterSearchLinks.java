package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class AdapterSearchLinks extends AdapterLink {
    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return null;
    }

    @Override
    public float getItemWidth() {
        return 0;
    }

    @Override
    public ControllerCommentsBase getControllerComments() {
        return null;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }
}
