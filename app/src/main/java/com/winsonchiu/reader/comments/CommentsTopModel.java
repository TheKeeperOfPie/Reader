/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import com.winsonchiu.reader.data.reddit.Link;

/**
 * Created by TheKeeperOfPie on 5/15/2016.
 * TODO: Think of a better name
 */
public class CommentsTopModel {

    private Link link = new Link();
    private Source source = Source.NONE;

    public CommentsTopModel() {

    }

    public CommentsTopModel(Link link, Source source) {
        this.link = link;
        this.source = source;
    }

    public Link getLink() {
        return link;
    }

    public Source getSource() {
        return source;
    }
}
