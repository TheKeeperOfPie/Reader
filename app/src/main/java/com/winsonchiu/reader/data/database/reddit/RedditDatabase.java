/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.winsonchiu.reader.data.database.Table;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public class RedditDatabase {

    private static final Table TABLE_SUBREDDIT = new TableSubreddit();
    private static final Table TABLE_LINK = new TableLink();

    private static final Table[] TABLES = new Table[]{
            TABLE_SUBREDDIT,
            TABLE_LINK,
    };

    private RedditOpenHelper redditOpenHelper;

    private BehaviorSubject<SQLiteDatabase> subjectDatabase = BehaviorSubject.create();

    public RedditDatabase(Application application) {
        redditOpenHelper = new RedditOpenHelper(application.getApplicationContext(), TABLES);

        final Scheduler.Worker worker = Schedulers.io().createWorker();
        worker.schedule(new Action0() {
            @Override
            public void call() {
                subjectDatabase.onNext(redditOpenHelper.getWritableDatabase());
                worker.unsubscribe();
            }
        });
    }

    public Observable<SQLiteDatabase> openDatabase() {
        return subjectDatabase.first();
    }

}
