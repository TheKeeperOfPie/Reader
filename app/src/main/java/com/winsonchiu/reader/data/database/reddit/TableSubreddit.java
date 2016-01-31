/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.reddit.Subreddit;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public final class TableSubreddit extends Table<Subreddit> {
    public static final String TABLE_NAME = "subreddit";
    public static final String COLUMN_NAME_JSON = "json";

    public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME
            + "("
            + _ID + SPACE + TYPE_TEXT + SPACE + PRIMARY_KEY + COMMA
            + COLUMN_NAME_JSON + SPACE + TYPE_TEXT + COMMA
            + ")";

    private static final String SQL_CREATE_VERBOSE =
            "CREATE TABLE subreddit" +
                    "(" +
                    "_id TEXT PRIMARY KEY," +
                    "json TEXT," +
                    ")";

    public TableSubreddit() {
        if (!SQL_CREATE.equals(SQL_CREATE_VERBOSE)) {
            throw new IllegalStateException("SQL_CREATE and SQL_CREATE_VERBOSE do not match");
        }
    }

    @Override
    public void onCreate() {
        sqLiteDatabase.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(int versionPrevious, int versionCurrent) {

    }

    @Override
    public void insertOrUpdate(Subreddit subreddit) {

    }
}
