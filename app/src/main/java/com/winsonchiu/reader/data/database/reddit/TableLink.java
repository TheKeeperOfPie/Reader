/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.database.TransactionInsertOrReplace;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Thing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public final class TableLink extends Table<Link> {

    public static final String TAG = TableLink.class.getCanonicalName();

    public static final String NAME = "link";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_JSON = "json";
    public static final String COLUMN_COMMENTS = "comments";

    public static final String[] COLUMNS = new String[] {
            COLUMN_TITLE,
            COLUMN_AUTHOR,
            COLUMN_JSON,
            COLUMN_COMMENTS,
    };;

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
                List<Link> linksWithComments = new ArrayList<>();

                for (int index = links.size() - 1; index >= 0; index--) {
                    if (!links.get(index).getComments().getChildren().isEmpty()) {
                        linksWithComments.add(links.remove(index));
                    }
                }

                TransactionInsertOrReplace transaction = getInsertOrReplace(sqLiteDatabase, COLUMN_TITLE, COLUMN_AUTHOR, COLUMN_JSON);
                sqLiteDatabase.beginTransaction();

                for (Link link : links) {
                    transaction.insertOrReplace(link.getId(), System.currentTimeMillis(), link.getCreatedUtc(), link.getTitle(), link.getAuthor(), link.getJson());
                }

                sqLiteDatabase.setTransactionSuccessful();
                sqLiteDatabase.endTransaction();

                TransactionInsertOrReplace transactionWithComments = getInsertOrReplace(sqLiteDatabase, COLUMN_TITLE, COLUMN_AUTHOR, COLUMN_JSON, COLUMN_COMMENTS);
                sqLiteDatabase.beginTransaction();

                for (Link link : linksWithComments) {
                    transactionWithComments.insertOrReplace(link.getId(), System.currentTimeMillis(), link.getCreatedUtc(), link.getTitle(), link.getAuthor(), link.getJson(), link.getJsonComments());
                }

                sqLiteDatabase.setTransactionSuccessful();
                sqLiteDatabase.endTransaction();
            }
        };
    }

    public Action1<SQLiteDatabase> storeLink(final Link link) {
        return new Action1<SQLiteDatabase>() {
            @Override
            public void call(SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.beginTransaction();

                if (link.getComments().getChildren().isEmpty()) {
                    TransactionInsertOrReplace transaction = getInsertOrReplace(sqLiteDatabase, COLUMN_TITLE, COLUMN_AUTHOR, COLUMN_JSON);
                    transaction.insertOrReplace(link.getId(), System.currentTimeMillis(), link.getCreatedUtc(), link.getTitle(), link.getAuthor(), link.getJson());
                }
                else {
                    TransactionInsertOrReplace transaction = getInsertOrReplace(sqLiteDatabase, COLUMN_TITLE, COLUMN_AUTHOR, COLUMN_JSON, COLUMN_COMMENTS);
                    transaction.insertOrReplace(link.getId(), System.currentTimeMillis(), link.getCreatedUtc(), link.getTitle(), link.getAuthor(), link.getJson(), link.getJsonComments());
                }

                sqLiteDatabase.setTransactionSuccessful();
                sqLiteDatabase.endTransaction();
            }
        };
    }

    public List<Thing> queryListing(final List<String> ids) {
//        StringBuilder statement = new StringBuilder("SELECT * from " + NAME + " inner join (select " + ids.get(0) + " as " + _ID);
//
//        String union = " union all select ";
//
//        for (int index = 1; index < ids.size(); index++) {
//            statement.append(union)
//                    .append(DatabaseUtils.sqlEscapeString(ids.get(index)));
//        }
//
//        statement.append(") as x on " + NAME + "." + _ID + " = x." + _ID);

        List<String> parameters = new ArrayList<>(ids.size());
        for (int index = 0; index < ids.size(); index++) {
            parameters.add("?");
        }

        List<Thing> links = new ArrayList<>();
        ObjectMapper objectMapper = ComponentStatic.getObjectMapper();
        Cursor query = sqLiteDatabase.query(NAME, new String[] {COLUMN_JSON, COLUMN_COMMENTS}, _ID + " IN (" + TextUtils.join(",", parameters) + ")", (String[]) ids.toArray(), null, null, null);

        if (query != null) {
            if (query.moveToFirst()) {
                do {
                    String json = query.getString(query.getColumnIndex(COLUMN_JSON));
                    String comments = query.getString(query.getColumnIndex(COLUMN_COMMENTS));
                    try {
                        Link link = Link.fromJson(objectMapper.readValue(json, JsonNode.class));

                        if (!TextUtils.isEmpty(comments)) {
                            link.setComments(Listing.fromJson(objectMapper.readValue(comments, JsonNode.class)));
                        }

                        links.add(link);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                while (query.moveToNext());
            }

            query.close();
        }

        Collections.sort(links, new Comparator<Thing>() {
            @Override
            public int compare(Thing lhs, Thing rhs) {
                int first = ids.indexOf(lhs.getId());
                int second = ids.indexOf(rhs.getId());
                return first < second ? -1 : (first == second ? 0 : 1);
            }
        });

        return links;
    }

    public Thing query(String id) {
        Link link = null;
        ObjectMapper objectMapper = ComponentStatic.getObjectMapper();
        Cursor query = sqLiteDatabase.query(NAME, new String[] {COLUMN_JSON, COLUMN_COMMENTS}, _ID + " IN (?) LIMIT 1", new String[] {id}, null, null, null);

        if (query != null) {
            if (query.moveToFirst()) {
                String json = query.getString(query.getColumnIndex(COLUMN_JSON));
                String comments = query.getString(query.getColumnIndex(COLUMN_COMMENTS));

                try {
                    link = Link.fromJson(objectMapper.readValue(json, JsonNode.class));

                    if (!TextUtils.isEmpty(comments)) {
                        link.setComments(Listing.fromJson(objectMapper.readValue(comments, JsonNode.class)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            query.close();
        }

        return link;
    }
}
