/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by TheKeeperOfPie on 2/15/2016.
 */
public abstract class TransactionInsertOrReplace {

    private static final String TAG = TransactionInsertOrReplace.class.getCanonicalName();
    private SQLiteDatabase sqLiteDatabase;
    private SQLiteStatement statement;

    public TransactionInsertOrReplace(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
        statement = sqLiteDatabase.compileStatement(createInsertOrReplaceStatement());
    }

    public abstract String createInsertOrReplaceStatement();

    public void insertOrReplace(String id, long accessed, long created, String... values) {
        statement.clearBindings();

        statement.bindString(1, id);
        statement.bindLong(2, accessed);
        statement.bindLong(3, created);

        for (int index = 0; index < values.length; index++) {
            if (values[index] != null) {
                statement.bindString(index + 4, values[index]);
            }
        }

        statement.execute();
    }
}
