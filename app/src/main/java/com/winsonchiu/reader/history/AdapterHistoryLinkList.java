/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 7/9/2015.
 */
public class AdapterHistoryLinkList extends AdapterLinkList {

    public AdapterHistoryLinkList(FragmentActivity activity,
                                  ControllerLinksBase controllerLinks,
                                  ViewHolderHeader.EventListener eventListenerHeader,
                                  ViewHolderLink.EventListener eventListenerBase,
                                  DisallowListener disallowListener,
                                  RecyclerCallback recyclerCallback) {
        super(activity, controllerLinks, eventListenerHeader, eventListenerBase,
                disallowListener, recyclerCallback);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == TYPE_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        return new AdapterLinkList.ViewHolder(activity,
                LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false),
                eventListenerBase,
                Source.HISTORY,
                disallowListener,
                recyclerCallback,
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
