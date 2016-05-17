/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;

/**
 * Created by TheKeeperOfPie on 5/15/2016.
 */
public class CommentsModel {

    private Link link = new Link();
    private Listing listingComments = new Listing();

    public CommentsModel() {

    }

    public CommentsModel(Link link, Listing listingComments) {
        this.link = link;
        this.listingComments = listingComments;
    }

    public Link getLink() {
        return link;
    }

    public Listing getListingComments() {
        return listingComments;
    }
}
