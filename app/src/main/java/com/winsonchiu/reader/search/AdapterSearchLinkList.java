/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterSearchLinkList extends AdapterLinkList {

    private final Source source;

    public AdapterSearchLinkList(FragmentActivity activity,
            ControllerLinksBase controllerLinks,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase,
            Source source) {
        super(activity, controllerLinks, adapterListener, eventListenerHeader, eventListenerBase);
        this.source = source;
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == TYPE_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        return new AdapterLinkList.ViewHolder(activity,
                LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false),
                adapterCallback,
                adapterListener,
                eventListenerBase,
                source,
                this);
    }
}
