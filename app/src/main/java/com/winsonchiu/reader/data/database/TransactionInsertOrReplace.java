/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Created by TheKeeperOfPie on 2/15/2016.
 */
public abstract class TransactionInsertOrReplace {

    private static final String TAG = TransactionInsertOrReplace.class.getCanonicalName();
    private SQLiteDatabase sqLiteDatabase;
    private SQLiteStatement statement;

    public TransactionInsertOrReplace(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
    }

    public abstract String createInsertOrReplaceStatement();

    public void begin() {
        Log.d(TAG, "begin() called with: " + createInsertOrReplaceStatement());
        statement = sqLiteDatabase.compileStatement(createInsertOrReplaceStatement());
        sqLiteDatabase.beginTransaction();
    }

    public void insertOrReplace(String id, long accessed, long created, String... values) {
        Log.d(TAG, "insertOrReplace() called");
        statement.clearBindings();

        statement.bindString(1, id);
        Log.d(TAG, "insertOrReplace() called 0");

        statement.bindLong(2, accessed);
        Log.d(TAG, "insertOrReplace() called 1");

        statement.bindLong(3, created);
        Log.d(TAG, "insertOrReplace() called 2");

        for (int index = 0; index < values.length; index++) {
            statement.bindString(index + 4, values[index]);
            Log.d(TAG, "insertOrReplace() called " + (index + 3));
        }

        statement.execute();
        Log.d(TAG, "insertOrReplace() called with: " + "id = [" + id + "], accessed = [" + accessed + "], created = [" + created + "], values = [" + values + "]");
    }

    public void end() {
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
        Log.d(TAG, "end() called with: " + "");
    }

}
