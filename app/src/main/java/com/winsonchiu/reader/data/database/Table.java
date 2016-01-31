/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public abstract class Table<Thing> implements BaseColumns {
    public static final String TYPE_TEXT = "TEXT";
    public static final String PRIMARY_KEY = "PRIMARY KEY";
    public static final String SPACE = ",";
    public static final String COMMA = ",";

    protected SQLiteDatabase sqLiteDatabase;

    public abstract void onCreate();
    public abstract void onUpgrade(int versionPrevious, int versionCurrent);

    public abstract void insertOrUpdate(Thing thing);

    public SQLiteDatabase getDatabase() {
        return sqLiteDatabase;
    }

    public void setDatabase(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
    }
}
