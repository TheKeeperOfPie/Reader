/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.content.res.Resources;

import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 4/23/2016.
 */
public enum Report {

    SPAM("spam", R.string.report_reason_spam),
    VOTE_MANIPULATION("vote manipulation", R.string.report_reason_vote_manipulation),
    PERSONAL_INFORMATION("personal information", R.string.report_reason_personal_information),
    SEXUALIZING_MINORS("sexualizing minors", R.string.report_reason_sexualizing_minors),
    BREAKING_REDDIT("breaking reddit", R.string.report_reason_breaking_reddit),
    OTHER("other", R.string.report_reason_other);

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

    public static String[] getDisplayReasons(Resources resources) {
        Report[] reports = values();
        String[] reasons = new String[reports.length];

        for (int index = 0; index < reports.length; index++) {
            reasons[index] = resources.getString(reports[index].getResourceId());
        }

        return reasons;
    }
}
