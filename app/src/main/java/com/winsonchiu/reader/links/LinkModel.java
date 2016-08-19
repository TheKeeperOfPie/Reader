/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;

/**
 * Created by TheKeeperOfPie on 8/14/2016.
 */

public class LinkModel {

    private Link link;

    private Listing comments = new Listing();
    private Album album;
    private int backgroundColor;
    private int textTitleColor;
    private int textBodyColor;

    private int contextLevel;
    private String commentId;

    private String youTubeId;
    private int youTubeTime;

    private String jsonComments;

    public LinkModel(Link link) {
        this.link = link;
    }

    public LinkModel() {

    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public Listing getComments() {
        return comments;
    }

    public void setComments(Listing comments) {
        this.comments = comments;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getTextTitleColor() {
        return textTitleColor;
    }

    public void setTextTitleColor(int textTitleColor) {
        this.textTitleColor = textTitleColor;
    }

    public int getTextBodyColor() {
        return textBodyColor;
    }

    public void setTextBodyColor(int textBodyColor) {
        this.textBodyColor = textBodyColor;
    }

    public int getContextLevel() {
        return contextLevel;
    }

    public void setContextLevel(int contextLevel) {
        this.contextLevel = contextLevel;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getYouTubeId() {
        return youTubeId;
    }

    public void setYouTubeId(String youTubeId) {
        this.youTubeId = youTubeId;
    }

    public int getYouTubeTime() {
        return youTubeTime;
    }

    public void setYouTubeTime(int youTubeTime) {
        this.youTubeTime = youTubeTime;
    }

    public String getJsonComments() {
        return jsonComments;
    }

    public void setJsonComments(String jsonComments) {
        this.jsonComments = jsonComments;
    }
}
