/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.winsonchiu.reader.adapter.AdapterCallback;

/**
 * Created by TheKeeperOfPie on 3/30/2016.
 */
public abstract class ViewHolderBase extends RecyclerView.ViewHolder {

    public static final String TAG = ViewHolderBase.class.getCanonicalName();

    protected Resources resources;
    protected AdapterCallback adapterCallback;

    public ViewHolderBase(View itemView, AdapterCallback adapterCallback) {
        super(itemView);
        this.adapterCallback = adapterCallback;
        this.resources = itemView.getResources();
    }

    public void onPause() {

    }

    public void onRecycle() {

    }

    public int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(itemView.getContext(), colorRes);
    }

}
