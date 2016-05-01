/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 4/23/2016.
 */
public enum Report {

    SPAM("spam", R.string.report_spam),
    VOTE_MANIPULATION("vote manipulation", R.string.report_vote_manipulation),
    PERSONAL_INFORMATION("personal information", R.string.report_personal_information),
    SEXUALIZING_MINORS("sexualizing minors", R.string.item_report_sexualizing_minors),
    BREAKING_REDDIT("breaking reddit", R.string.report_breaking_reddit),
    OTHER("other", R.string.report_other);

    private final String reason;
    private final int resourceId;

    Report(String reason, int resourceId) {
        this.reason = reason;
        this.resourceId = resourceId;
    }

    public String getReason() {
        return reason;
    }

    public int getResourceId() {
        return resourceId;
    }
}
