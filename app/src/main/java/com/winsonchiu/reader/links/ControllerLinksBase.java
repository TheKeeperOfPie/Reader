/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Subreddit;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerLinksBase {

    // TODO: Include default implementations

    Link getLink(int position);
    int sizeLinks();
    boolean isLoading();
    void loadMoreLinks();
    Subreddit getSubreddit();
    boolean showSubreddit();
    Link remove(int position);
}