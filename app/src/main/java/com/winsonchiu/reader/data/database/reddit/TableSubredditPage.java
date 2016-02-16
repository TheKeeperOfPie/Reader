/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.reddit.Subreddit;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public final class TableSubredditPage extends Table<Subreddit> {
    public static final String NAME = "subreddit_page";
    public static final String COLUMN_JSON = "json";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LINKS = "links";

    public static final String[] COLUMNS = new String[] {
            COLUMN_JSON,
            COLUMN_NAME,
            COLUMN_LINKS
    };

    public TableSubredditPage() {

    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getColumns() {
        return COLUMNS;
    }

    @Override
    public void onUpgrade(int versionPrevious, int versionCurrent) {

    }

    @Override
    public void insertOrUpdate(Subreddit subreddit) {

    }
}
