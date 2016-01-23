/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterSearchLinkList extends AdapterLinkList {

    private final Source source;

    public AdapterSearchLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            Source source,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(activity, controllerLinks, eventListenerHeader, eventListenerBase, disallowListener,
                recyclerCallback);
        this.source = source;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        return new AdapterLinkList.ViewHolder(activity,
                LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false),
                eventListenerBase,
                source,
                disallowListener,
                recyclerCallback,
                this) {
            @Override
            public void onClickThumbnail() {
                InputMethodManager inputManager = (InputMethodManager) activity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(itemView.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                super.onClickThumbnail();
            }
        };
    }
}
