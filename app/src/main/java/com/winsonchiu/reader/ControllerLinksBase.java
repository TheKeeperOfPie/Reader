/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;

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