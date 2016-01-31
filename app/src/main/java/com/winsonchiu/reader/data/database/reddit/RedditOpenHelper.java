/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.winsonchiu.reader.data.database.Table;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public class RedditOpenHelper extends SQLiteOpenHelper {

    public static final int VERSION = 0;
    public static final String NAME = "Reader.db";

    private final Table[] tables;

    public RedditOpenHelper(Context context, Table[] tables) {
        super(context, NAME, null, VERSION);
        this.tables = tables;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Table table : tables) {
            table.setDatabase(db);
            table.onCreate();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (Table table : tables) {
            table.setDatabase(db);
            table.onUpgrade(oldVersion, newVersion);
        }
    }
}
