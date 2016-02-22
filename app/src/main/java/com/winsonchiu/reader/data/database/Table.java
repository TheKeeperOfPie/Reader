/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.Arrays;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public abstract class Table<Thing> implements BaseColumns {
    public static final String TYPE_TEXT = "TEXT";
    public static final String PRIMARY_KEY = "PRIMARY KEY";
    public static final String SPACE = ",";
    public static final String COMMA = ",";

    public static final String COLUMN_ACCESSED = "accessed";
    public static final String COLUMN_CREATED = "created";

    public static final String TABLE_COLUMNS_PREFIX = "(" + _ID + ", " + COLUMN_ACCESSED + ", " + COLUMN_CREATED;

    protected SQLiteDatabase sqLiteDatabase;

    public abstract String getName();
    public abstract String[] getColumns();
    public abstract void onUpgrade(int versionPrevious, int versionCurrent);

    public void onCreate() {
        sqLiteDatabase.execSQL(createTableStatement());
    }

    private String createTableStatement() {
        StringBuilder builder = new StringBuilder();

        builder.append("CREATE TABLE ")
                .append(getName())
                .append(" (")
                .append(_ID)
                .append(" TEXT PRIMARY KEY, ")
                .append(COLUMN_ACCESSED)
                .append(" INTEGER, ")
                .append(COLUMN_CREATED)
                .append(" INTEGER");

        for (String column : getColumns()) {
            builder.append(", ")
                    .append(column)
                    .append(" TEXT");
        }

        builder.append(")");

        return builder.toString();
    }

    public TransactionInsertOrReplace getInsertOrReplace(SQLiteDatabase sqLiteDatabase, final String... columns) {
        return new TransactionInsertOrReplace(sqLiteDatabase) {
            @Override
            public String createInsertOrReplaceStatement() {
                StringBuilder builder = new StringBuilder();

                String[] bindings = new String[columns.length + 3];
                Arrays.fill(bindings, "?");

                builder.append("INSERT OR REPLACE INTO ")
                        .append(getName())
                        .append(TABLE_COLUMNS_PREFIX);

                for (String column : columns) {
                    builder.append(", ")
                            .append(column);
                }

                builder.append(") VALUES (")
                        .append(TextUtils.join(", ", bindings))
                        .append(");");

                return builder.toString();
            }
        };
    }

    public abstract void insertOrUpdate(Thing thing);

    public SQLiteDatabase getDatabase() {
        return sqLiteDatabase;
    }

    public void setDatabase(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
    }
}
