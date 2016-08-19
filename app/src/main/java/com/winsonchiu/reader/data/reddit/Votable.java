/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

/**
 * Created by TheKeeperOfPie on 5/15/2016.
 */
public interface Votable {

    String getName();
    Likes getLikes();
    void setLikes(Likes likes);

}
