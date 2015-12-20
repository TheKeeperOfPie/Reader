/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 6/5/2015.
 */
public enum Sort {

    CONFIDENCE(R.id.item_sort_confidence),
    HOT(R.id.item_sort_hot),
    NEW(R.id.item_sort_new),
    RISING(R.id.item_sort_rising),
    CONTROVERSIAL(R.id.item_sort_controversial),
    TOP(R.id.item_sort_top),
//    GILDED(R.id.item_sort_gilded), TODO: Add support for gilded sort, mixes comments and links
    RELEVANCE(R.id.item_sort_relevance),
    ACTIVITY(R.id.item_sort_activity),
    OLD(R.id.item_sort_old),
    RANDOM(R.id.item_sort_random),
    QA(R.id.item_sort_qa),
    ALPHABETICAL(R.id.item_sort_alphabetical),
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
