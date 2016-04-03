/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

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

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }
}
