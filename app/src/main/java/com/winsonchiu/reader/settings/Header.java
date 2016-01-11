/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 7/2/2015.
 */
public enum Header {

    DISPLAY(R.drawable.ic_palette_white_24dp, com.winsonchiu.reader.R.string.prefs_category_display, R.string.prefs_category_display_summary),
    BEHAVIOR(R.drawable.ic_build_white_24dp, R.string.prefs_category_behavior, R.string.prefs_category_behavior_summary),
    MAIL(R.drawable.ic_mail_white_24dp, R.string.prefs_category_mail, R.string.prefs_category_mail_summary),
    ABOUT(R.drawable.ic_help_outline_white_24dp, R.string.prefs_category_about, R.string.prefs_category_about_summary);

    private int iconResourceId;
    private int titleResourceId;
    private int summaryResourceId;

    Header(int iconResourceId, int titleResourceId, int summaryResourceId) {
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
