/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.utils.ObserverEmpty;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public class RedditDatabase {

    private static final TableSubreddit TABLE_SUBREDDIT = new TableSubreddit();
    private static final TableLink TABLE_LINK = new TableLink();

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

    public void storeListing(Listing listing) {
        List<Link> links = new ArrayList<>();
        for (Thing thing : listing.getChildren()) {
            if (thing instanceof Link) {
                links.add((Link) thing);
            }
        }

        openDatabase()
                .subscribeOn(Schedulers.io())
//                .doOnNext(TABLE_LINK.storeLinks(links))
                .subscribe(new ObserverEmpty<SQLiteDatabase>());
    }
}
