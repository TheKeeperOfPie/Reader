/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 6/5/2015.
 */
public enum Sort {

    CONFIDENCE(R.id.item_sort_confidence),
    HOT(R.id.item_sort_hot),
    NEW(R.id.item_sort_new),
    TOP(R.id.item_sort_top),
    CONTROVERSIAL(R.id.item_sort_controversial),
    RELEVANCE(R.id.item_sort_relevance),
    ACTIVITY(R.id.item_sort_activity),
    OLD(R.id.item_sort_old),
    RANDOM(R.id.item_sort_random),
    QA(R.id.item_sort_qa);

    private int menuId;

    Sort(int menuId) {
        this.menuId = menuId;
    }

    public int getMenuId() {
        return menuId;
    }

    // String returned is lowercase for use in URL and formatting
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
