/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

/**
 * Created by TheKeeperOfPie on 7/2/2015.
 */
public class Header {

    private int iconResourceId;
    private int titleResourceId;
    private int summaryResourceId;

    public Header(int iconResourceId, int titleResourceId, int summaryResourceId) {
        this.iconResourceId = iconResourceId;
        this.titleResourceId = titleResourceId;
        this.summaryResourceId = summaryResourceId;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public int getTitleResourceId() {
        return titleResourceId;
    }

    public int getSummaryResourceId() {
        return summaryResourceId;
    }
}
