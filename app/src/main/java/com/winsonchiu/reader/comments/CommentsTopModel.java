/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import com.winsonchiu.reader.links.LinkModel;

/**
 * Created by TheKeeperOfPie on 5/15/2016.
 * TODO: Think of a better name
 */
public class CommentsTopModel {

    private LinkModel linkModel = new LinkModel();
    private Source source = Source.NONE;

    public CommentsTopModel() {

    }

    public CommentsTopModel(CommentsTopModel other) {
        this.linkModel = other.linkModel;
        this.source = other.source;
    }

    public LinkModel getLinkModel() {
        return linkModel;
    }

    public void setLinkModel(LinkModel linkModel) {
        this.linkModel = linkModel;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
