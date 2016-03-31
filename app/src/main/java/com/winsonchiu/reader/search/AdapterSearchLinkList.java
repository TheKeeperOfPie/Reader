/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterSearchLinkList extends AdapterLinkList {

    private final Source source;

    public AdapterSearchLinkList(FragmentActivity activity,
            ControllerLinksBase controllerLinks,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase,
            Source source,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(activity, controllerLinks, eventListenerHeader, eventListenerBase, disallowListener,
                recyclerCallback);
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
