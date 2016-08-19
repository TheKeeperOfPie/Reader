/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 6/5/2015.
 */
public enum Sort {

    @JsonProperty("confidence")
    CONFIDENCE(R.id.item_sort_confidence),

    @JsonProperty("hot")
    HOT(R.id.item_sort_hot),

    @JsonProperty("new")
    NEW(R.id.item_sort_new),

    @JsonProperty("rising")
    RISING(R.id.item_sort_rising),

    @JsonProperty("controversial")
    CONTROVERSIAL(R.id.item_sort_controversial),

    @JsonProperty("top")
    TOP(R.id.item_sort_top),

//  GILDED(R.id.item_sort_gilded), TODO: Add support for gilded sort, mixes comments and links

    @JsonProperty("relevance")
    RELEVANCE(R.id.item_sort_relevance),

    @JsonProperty("activity")
    ACTIVITY(R.id.item_sort_activity),

    @JsonProperty("old")
    OLD(R.id.item_sort_old),

    @JsonProperty("random")
    RANDOM(R.id.item_sort_random),

    @JsonProperty("qa")
    QA(R.id.item_sort_qa),

    @JsonProperty("alphabetical")
    ALPHABETICAL(R.id.item_sort_alphabetical),

    @JsonProperty("subscribers")
    SUBSCRIBERS(R.id.item_sort_subscribers),

    ;

    private int menuId;

    Sort(int menuId) {
        this.menuId = menuId;
    }

    public int getMenuId() {
        return menuId;
    }

    public static Sort fromMenuId(int id) {
        for (Sort sort : values()) {
            if (sort.getMenuId() == id) {
                return sort;
            }
        }

        return null;
    }

    // String returned is lowercase for use in URL and formatting
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static Sort fromString(String string) {

        for (Sort sort : values()) {
            if (sort.name().equalsIgnoreCase(string)) {
                return sort;
            }
        }

        return CONFIDENCE;
    }
}
