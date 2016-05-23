/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.support.annotation.Nullable;

import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 5/15/2016.
 */
public class CommentsModel {

    private Link link = new Link();
    private List<Comment> comments = new ArrayList<>();
    private boolean showSubreddit;
    private User user;

    public CommentsModel() {

    }

    public CommentsModel(Link link, List<Comment> comments, boolean showSubreddit, User user) {
        this.link = link;
        this.comments = comments;
        this.showSubreddit = showSubreddit;
        this.user = user;
    }

    public Link getLink() {
        return link;
    }

    public List<Comment> getComments() {
        return comments;
    }

    @Nullable
    public User getUser() {
        return user;
    }

    public boolean isShowSubreddit() {
        return showSubreddit;
    }
}
