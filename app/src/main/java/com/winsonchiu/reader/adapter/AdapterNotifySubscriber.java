/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.adapter;

import android.support.v7.widget.RecyclerView;

import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 4/16/2016.
 */
public class AdapterNotifySubscriber<DataType, AdapterType extends RecyclerView.Adapter & AdapterDataListener<DataType>> implements Action1<RxAdapterEvent<DataType>> {

    private AdapterType adapter;

    public AdapterNotifySubscriber(AdapterType adapter) {
        this.adapter = adapter;
    }

    @Override
    public void call(RxAdapterEvent<DataType> event) {
        adapter.setData(event.getData());

        switch (event.getType()) {
            case CHANGE:
                adapter.notifyItemRangeChanged(event.getPositionStart(), event.getSize(), event.getPayload());
                break;
            case INSERT:
                adapter.notifyItemRangeInserted(event.getPositionStart(), event.getSize());
                break;
            case REMOVE:
                adapter.notifyItemRangeRemoved(event.getPositionStart(), event.getSize());
                break;
            case RESET:
                adapter.notifyDataSetChanged();
                break;
        }
    }
}
