/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import com.winsonchiu.reader.data.reddit.Subreddit;

/**
 * Created by TheKeeperOfPie on 7/28/2015.
 */
public interface ControllerSearchBase {

    Subreddit getSubreddit(int position);
    int getSubredditCount();
}
