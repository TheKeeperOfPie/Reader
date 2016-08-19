/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by TheKeeperOfPie on 8/14/2016.
 */

public enum Likes {

    @JsonProperty("null")
    NONE(0),

    @JsonProperty("true")
    UPVOTE(1),

    @JsonProperty("false")
    DOWNVOTE(-1);

    private final int scoreValue;

    Likes(int scoreValue) {
        this.scoreValue = scoreValue;
    }

    public int getScoreValue() {
        return scoreValue;
    }
}
