/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import com.winsonchiu.reader.data.reddit.Link;

/**
 * Created by TheKeeperOfPie on 12/28/2015.
 */
public interface YouTubeListener {
    void loadYouTubeVideo(Link link, String id, int timeInMillis);
    boolean hideYouTube();
}
