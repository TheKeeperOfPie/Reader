/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.reddit.Link;

/**
 * Created by TheKeeperOfPie on 5/1/2016.
 */
public abstract class LinksListenerBase implements AdapterLink.ViewHolderLink.Listener {

    private AdapterLink.ViewHolderLink.EventListener eventListener;

    public LinksListenerBase(AdapterLink.ViewHolderLink.EventListener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public void onSubmitComment(Link link, String text) {
        eventListener.sendComment(link.getName(), text);
    }

    @Override
    public void onDownloadImage(Link link) {
        eventListener.downloadImage(link.getTitle(), link.getTitle(), link.getUrl());
    }

    @Override
    public void onDownloadImage(Link link, String title, String fileName, String url) {
        eventListener.downloadImage(title, fileName, url);
    }

    @Override
    public void onLoadUrl(Link link, boolean forceExternal) {
        if (forceExternal) {
            eventListener.loadWebFragment(link.getUrl());
        }
        else {
            eventListener.loadUrl(link.getUrl());
        }
    }

    @Override
    public void onShowFullEditor(Link link) {
        eventListener.showReplyEditor(link);
    }

    @Override
    public void onCopyText(Link link) {
        eventListener.copyText(link.getSelfText());
    }

    @Override
    public void onEdit(Link link) {
        eventListener.editLink(link);
    }

    @Override
    public void onShowComments(Link link, AdapterLink.ViewHolderLink viewHolderLink, Source source) {
        eventListener.onClickComments(link, viewHolderLink, source);
    }

    @Override
    public void onShowError(String error) {
        eventListener.toast(error);
    }
}
