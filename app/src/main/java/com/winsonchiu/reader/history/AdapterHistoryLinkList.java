/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;

import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 7/9/2015.
 */
public class AdapterHistoryLinkList extends AdapterLinkList {

    public AdapterHistoryLinkList(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase) {
        super(activity, adapterListener, eventListenerHeader, eventListenerBase);
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
                Source.HISTORY,
                this) {
            @Override
            public boolean isInHistory() {
                return false;
            }

            @Override
            public void addToHistory() {
                // Override to prevent moving to top
            }
        };
    }
}
