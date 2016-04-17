/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;

import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterSearchLinkList extends AdapterLinkList {

    private final Source source;

    public AdapterSearchLinkList(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase,
            Source source) {
        super(activity,  adapterListener, eventListenerHeader, eventListenerBase);
        this.source = source;
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            return super.onCreateViewHolder(parent, viewType);
        }

        return new AdapterLinkList.ViewHolder(activity,
                parent,
                adapterCallback,
                adapterListener,
                eventListenerBase,
                source,
                this);
    }
}
