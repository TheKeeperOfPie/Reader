/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.database.sqlite.SQLiteDatabase;

import com.winsonchiu.reader.data.database.TransactionInsertOrReplace;
import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.reddit.Link;

import java.util.List;

import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public final class TableLink extends Table<Link> {
    public static final String NAME = "link";
    public static final String COLUMN_JSON = "json";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AUTHOR = "author";

    public static final String[] COLUMNS = new String[] {
            COLUMN_JSON,
            COLUMN_TITLE,
            COLUMN_AUTHOR
    };

    public TableLink() {

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
    public void insertOrUpdate(Link link) {

    }

    public Action1<SQLiteDatabase> storeLinks(final List<Link> links) {
        return new Action1<SQLiteDatabase>() {
            @Override
            public void call(SQLiteDatabase sqLiteDatabase) {
                TransactionInsertOrReplace transaction = getInsertOrReplace(sqLiteDatabase);

                transaction.begin();

                for (Link link : links) {
                    transaction.insertOrReplace(link.getName(), System.currentTimeMillis(), link.getCreatedUtc(), link.getJson(), link.getTitle(), link.getAuthor());
                }

                transaction.end();
            }
        };
    }
}
