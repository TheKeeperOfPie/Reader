/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterHistoryLinkGrid extends AdapterLinkGrid {

    public AdapterHistoryLinkGrid(FragmentActivity activity,
                                  ControllerLinksBase controllerLinks,
                                  ViewHolderHeader.EventListener eventListenerHeader,
                                  ViewHolderLink.EventListener eventListenerBase,
                                  DisallowListener disallowListener,
                                  RecyclerCallback recyclerCallback) {
        super(activity, controllerLinks, eventListenerHeader, eventListenerBase, disallowListener,
                recyclerCallback);
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
                eventListenerBase,
                Source.HISTORY,
                disallowListener,
                recyclerCallback,
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
