/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.annotation.Nullable;

import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.utils.UtilsList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 8/19/2016.
 */

public abstract class LinksController {

    protected Listing listing = new Listing();

    public LinksController() {
        super();
    }

    public void clear() {

    }

    public void reload() {

    }

    public int numberOfLinks() {
        return listing.getChildren() == null ? 0 : listing.getChildren().size();
    }

    @Nullable
    public Link getPreviousLink(Link linkCurrent, int offset) {
        int index = indexOfLink(linkCurrent) - offset;
        if (index >= 0 && !listing.getChildren().isEmpty()) {
            return (Link) listing.getChildren().get(index);
        }

        return null;
    }

    @Nullable
    public Link getNextLink(Link linkCurrent, int offset) {
        int index = indexOfLink(linkCurrent) + offset;
        if (index < listing.getChildren().size() && !listing.getChildren().isEmpty()) {
            return (Link) listing.getChildren().get(index);
        }

        return null;
    }

    public int indexOfLink(Link link) {
        String name = link.getName();
        return UtilsList.indexOf(listing.getChildren(), thing -> thing.getName().equals(name));
    }

    public void update(Link link) {
        int index = indexOfLink(link);

        if (index > -1) {
//            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
        }

        publishUpdate();
    }

    public void remove(Link link) {
        int index = indexOfLink(link);

        if (index > -1) {
            listing.getChildren().remove(index);
        }

        publishUpdate();
    }

    public void clearViewed(Historian historian) {
        List<Integer> indexesToRemove = new ArrayList<>();

        for (int index = 0; index < listing.getChildren().size(); index++) {
            Link link = (Link) listing.getChildren().get(index);
            if (historian.contains(link.getName())) {
                indexesToRemove.add(0, index);
            }
        }

        for (int index : indexesToRemove) {
            listing.getChildren().remove(index);
        }

        publishUpdate();
    }

    public abstract void publishUpdate();
}
