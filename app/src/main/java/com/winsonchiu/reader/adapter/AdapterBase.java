/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.adapter;

import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 4/3/2016.
 */
public abstract class AdapterBase<ViewHolderType extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<ViewHolderType> {

    protected RecyclerView recyclerView;
    protected AdapterCallback adapterCallback = new AdapterCallback() {
        @Nullable
        @Override
        public RecyclerView getRecyclerView() {
            return recyclerView;
        }
    };

    protected int loadMoreThreshold = 5;
    protected AdapterLoadMoreListener loadMoreListener;

    @Override
    @CallSuper
    public void onBindViewHolder(ViewHolderType holder, int position) {
        if (position > getItemCount() - loadMoreThreshold && loadMoreListener != null) {
            loadMoreListener.requestMore();
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    public void setAdapterLoadMoreListener(AdapterLoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }
}
