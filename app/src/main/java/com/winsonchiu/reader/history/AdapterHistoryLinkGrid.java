/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;

import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterHistoryLinkGrid extends AdapterLinkGrid {

    public AdapterHistoryLinkGrid(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.Listener listenerLink) {
        super(activity, adapterListener, eventListenerHeader, listenerLink);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            return super.onCreateViewHolder(parent, viewType);
        }

        return new AdapterLinkGrid.ViewHolder(
                activity,
                parent,
                adapterCallback,
                adapterListener,
                listenerLink,
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
