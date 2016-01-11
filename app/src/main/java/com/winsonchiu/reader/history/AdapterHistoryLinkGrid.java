/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterHistoryLinkGrid extends AdapterLinkGrid {

    public AdapterHistoryLinkGrid(Activity activity,
                                  ControllerLinksBase controllerLinks,
                                  ViewHolderHeader.EventListener eventListenerHeader,
                                  ViewHolderBase.EventListener eventListenerBase,
                                  DisallowListener disallowListener,
                                  RecyclerCallback recyclerCallback) {
        super(activity, controllerLinks, eventListenerHeader, eventListenerBase, disallowListener,
                recyclerCallback);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        return new AdapterLinkGrid.ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false),
                eventListenerBase,
                Source.HISTORY,
                disallowListener,
                recyclerCallback,
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
