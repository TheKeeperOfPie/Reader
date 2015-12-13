/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Subreddit;

import rx.Observable;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerLinksBase {

    // TODO: Include default implementations

    Link getLink(int position);
    int sizeLinks();
    boolean isLoading();
    Observable<Listing> loadMoreLinks();
    Subreddit getSubreddit();
    boolean showSubreddit();
    boolean setReplyText(String name, String text, boolean collapsed);
    void setNsfw(String name, boolean over18);
}