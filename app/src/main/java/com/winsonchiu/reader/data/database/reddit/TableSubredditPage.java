/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.database.TransactionInsertOrReplace;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Time;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public final class TableSubredditPage extends Table<Subreddit> {
    public static final String NAME = "subreddit_page";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LINKS = "links";

    public static final String[] COLUMNS = new String[] {
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

    public Action1<SQLiteDatabase> storePage(final Subreddit subreddit, final List<String> ids, Sort sort, Time time) {
        return new Action1<SQLiteDatabase>() {
            @Override
            public void call(SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.beginTransaction();

                try {
                    String json = ComponentStatic.getObjectMapper().writeValueAsString(ids);

                    TransactionInsertOrReplace transaction = getInsertOrReplace(sqLiteDatabase, COLUMNS);

                    transaction.insertOrReplace(subreddit.getUrl(), System.currentTimeMillis(), subreddit.getCreatedUtc(), subreddit.getDisplayName(), json);

                    sqLiteDatabase.setTransactionSuccessful();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sqLiteDatabase.endTransaction();
            }
        };
    }

    public Action1<SQLiteDatabase> appendPage(final Subreddit subreddit, final List<String> ids, Sort sort, Time time) {
        return new Action1<SQLiteDatabase>() {
            @Override
            public void call(SQLiteDatabase sqLiteDatabase) {
                Cursor query = sqLiteDatabase.query(NAME, COLUMNS, _ID + " = ? LIMIT 1", new String[]{subreddit.getUrl()}, null, null, null);

                if (query != null) {
                    if (query.moveToFirst()) {
                        String json = query.getString(query.getColumnIndex(COLUMN_LINKS));
                        try {
                            LinkedHashSet<String> idsCurrent = new LinkedHashSet<>(Arrays.asList(ComponentStatic.getObjectMapper().readValue(json, String[].class)));
                            idsCurrent.addAll(ids);

                            ids.clear();
                            ids.addAll(idsCurrent);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    query.close();
                }

                sqLiteDatabase.beginTransaction();

                try {
                    String json = ComponentStatic.getObjectMapper().writeValueAsString(ids);

                    TransactionInsertOrReplace transaction = getInsertOrReplace(sqLiteDatabase, COLUMNS);

                    transaction.insertOrReplace(subreddit.getUrl(), System.currentTimeMillis(), subreddit.getCreatedUtc(), subreddit.getDisplayName(), json);

                    sqLiteDatabase.setTransactionSuccessful();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sqLiteDatabase.endTransaction();
            }
        };
    }

    public List<String> getLinksForSubreddit(Subreddit subreddit) {
        Cursor query = sqLiteDatabase.query(NAME, COLUMNS, _ID + " = ?", new String[] {subreddit.getUrl()}, null, null, null, "1");

        List<String> ids = new ArrayList<>();

        if (query != null) {
            if (query.moveToFirst()) {
                String json = query.getString(query.getColumnIndex(COLUMN_LINKS));
                try {
                    ids =  Arrays.asList(ComponentStatic.getObjectMapper().readValue(json, String[].class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            query.close();
        }

        return ids;
    }
}
