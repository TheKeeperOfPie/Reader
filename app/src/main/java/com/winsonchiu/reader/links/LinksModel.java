/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.annotation.Nullable;

import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/24/2016.
 */
public class LinksModel {

    private Subreddit subreddit = new Subreddit();
    private List<Link> links = new ArrayList<>(0);
    private boolean showSubreddit;
    @Nullable private User user;

    public LinksModel() {

    }

    public LinksModel(Subreddit subreddit, List<Link> links, boolean showSubreddit, @Nullable User user) {
        this.subreddit = subreddit;
        this.links = links;
        this.showSubreddit = showSubreddit;
        this.user = user;
    }

    public Subreddit getSubreddit() {
        return subreddit;
    }

    public List<Link> getLinks() {
        return links;
    }

    @Nullable
    public User getUser() {
        return user;
    }

    public boolean isShowSubreddit() {
        return showSubreddit;
    }
}
