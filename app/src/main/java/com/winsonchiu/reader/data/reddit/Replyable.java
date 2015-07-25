/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

/**
 * Created by TheKeeperOfPie on 7/24/2015.
 */
public class Replyable extends Thing {


    private boolean replyExpanded;
    private CharSequence replyText;

    public boolean isReplyExpanded() {
        return replyExpanded;
    }

    public void setReplyExpanded(boolean replyExpanded) {
        this.replyExpanded = replyExpanded;
    }

    public CharSequence getReplyText() {
        return replyText;
    }

    public void setReplyText(CharSequence replyText) {
        this.replyText = replyText;
    }

}
