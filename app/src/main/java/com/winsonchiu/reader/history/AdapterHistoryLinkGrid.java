/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterHistoryLinkGrid extends AdapterLinkGrid {

    public AdapterHistoryLinkGrid(FragmentActivity activity,
            ControllerLinksBase controllerLinks,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase) {
        super(activity, controllerLinks, adapterListener, eventListenerHeader, eventListenerBase);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == TYPE_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        return new AdapterLinkGrid.ViewHolder(
                activity,
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.cell_link, viewGroup, false),
                adapterCallback,
                adapterListener,
                eventListenerBase,
                Source.HISTORY,
                this,
                thumbnailSize) {
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
