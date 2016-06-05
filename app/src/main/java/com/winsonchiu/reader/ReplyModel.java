/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 6/5/2016.
 */
public class ReplyModel {

    private String nameParent;
    private String text;
    private boolean collapsed;

    public ReplyModel(String nameParent, String text, boolean collapsed) {
        this.nameParent = nameParent;
        this.text = text;
        this.collapsed = collapsed;
    }

    public String getNameParent() {
        return nameParent;
    }

    public String getText() {
        return text;
    }

    public boolean isCollapsed() {
        return collapsed;
    }
}
