/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.URLUtil;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.data.database.Table;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.rx.ActionLog;
import com.winsonchiu.reader.utils.ObserverEmpty;
import com.winsonchiu.reader.utils.UtilsImage;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * Created by TheKeeperOfPie on 1/30/2016.
 */
public class RedditDatabase {

    public static final String TAG = RedditDatabase.class.getCanonicalName();

    private static final TableSubredditPage TABLE_SUBREDDIT_PAGE = new TableSubredditPage();
    private static final TableSubreddit TABLE_SUBREDDIT = new TableSubreddit();
    private static final TableLink TABLE_LINK = new TableLink();

    private static final Table[] TABLES = new Table[]{
            TABLE_SUBREDDIT_PAGE,
            TABLE_SUBREDDIT,
            TABLE_LINK,
    };

    private final Reddit reddit;
    private final Picasso picasso;
    private final SharedPreferences sharedPreferences;

    private RedditOpenHelper redditOpenHelper;

    private BehaviorSubject<SQLiteDatabase> subjectDatabase = BehaviorSubject.create();

    public RedditDatabase(Application application, Reddit reddit, Picasso picasso) {
        this.reddit = reddit;
        this.picasso = picasso;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        redditOpenHelper = new RedditOpenHelper(application.getApplicationContext(), TABLES);

        final Scheduler.Worker worker = Schedulers.io().createWorker();
        worker.schedule(new Action0() {
            @Override
            public void call() {
                SQLiteDatabase database = redditOpenHelper.getWritableDatabase();

                for (Table table : TABLES) {
                    table.setDatabase(database);
                }

                subjectDatabase.onNext(database);
                worker.unsubscribe();
            }
        });
    }

    public Observable<SQLiteDatabase> openDatabase() {
        return subjectDatabase.first();
    }

    public Action1<Listing> storeListing(final Subreddit subreddit, final Sort sort, final Time time) {
        return new Action1<Listing>() {
            @Override
            public void call(Listing listing) {
                List<Link> links = new ArrayList<>();
                List<String> linkIds = new ArrayList<>();
                for (Thing thing : listing.getChildren()) {
                    if (thing instanceof Link) {
                        links.add((Link) thing);
                        linkIds.add(thing.getId());
                    }
                }

                openDatabase()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnNext(TABLE_SUBREDDIT_PAGE.storePage(subreddit, linkIds, sort, time))
                        .doOnNext(TABLE_LINK.storeLinks(links))
                        .subscribe(new ObserverEmpty<SQLiteDatabase>());
            }
        };
    }

    public Action1<Listing> appendListing(final Subreddit subreddit, final Sort sort, final Time time) {
        return new Action1<Listing>() {
            @Override
            public void call(Listing listing) {
                List<Link> links = new ArrayList<>();
                List<String> linkIds = new ArrayList<>();
                for (Thing thing : listing.getChildren()) {
                    if (thing instanceof Link) {
                        links.add((Link) thing);
                        linkIds.add(thing.getId());
                    }
                }

                openDatabase()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnNext(TABLE_SUBREDDIT_PAGE.appendPage(subreddit, linkIds, sort, time))
                        .doOnNext(TABLE_LINK.storeLinks(links))
                        .subscribe(new ObserverEmpty<SQLiteDatabase>());
            }
        };
    }

    public Observable<Listing> getLinksForSubreddit(final Subreddit subreddit) {
        return openDatabase()
                .subscribeOn(Schedulers.io())
                .doOnNext(new ActionLog<SQLiteDatabase>(TAG))
                .map(new Func1<SQLiteDatabase, List<String>>() {
                    @Override
                    public List<String> call(SQLiteDatabase sqLiteDatabase) {
                        return TABLE_SUBREDDIT_PAGE.getLinksForSubreddit(subreddit);
                    }
                })
                .doOnNext(new ActionLog<List<String>>(TAG))
                .filter(new Func1<List<String>, Boolean>() {
                    @Override
                    public Boolean call(List<String> strings) {
                        return strings != null;
                    }
                })
                .doOnNext(new ActionLog<List<String>>(TAG))
                .map(new Func1<List<String>, List<Thing>>() {
                    @Override
                    public List<Thing> call(List<String> ids) {
                        return TABLE_LINK.queryListing(ids);
                    }
                })
                .doOnNext(new ActionLog<List<Thing>>(TAG))
                .map(new Func1<List<Thing>, Listing>() {
                    @Override
                    public Listing call(List<Thing> links) {
                        return new Listing(links);
                    }
                });
    }

    public Action1<Link> storeLink() {
        return new Action1<Link>() {
            @Override
            public void call(final Link link) {
                openDatabase()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnNext(TABLE_LINK.storeLink(link))
                        .doOnNext(cacheLinkImage(link))
                        .subscribe(new ObserverEmpty<SQLiteDatabase>());
            }
        };
    }

    public Action1<SQLiteDatabase> cacheLinkImage(final Link link) {
        return new Action1<SQLiteDatabase>() {
            @Override
            public void call(SQLiteDatabase sqLiteDatabase) {
                boolean preventCache = link.isOver18() && !sharedPreferences.getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true);

                if (!link.isSelf()
                        && !Reddit.DEFAULT.equals(link.getThumbnail())
                        && !preventCache) {
                    String thumbnail = UtilsImage.parseThumbnail(link);

                    if (URLUtil.isNetworkUrl(thumbnail)) {
                        picasso.load(thumbnail)
                                .priority(Picasso.Priority.LOW)
                                .fetch();
                        Log.d(TAG, "cacheLinkImage call() called with: " + thumbnail);
                    }

                    if (UtilsImage.placeImageUrl(link)) {
                        picasso.load(link.getUrl())
                                .priority(Picasso.Priority.LOW)
                                .fetch();
                        Log.d(TAG, "cacheLinkImage call() called with: " + link.getUrl());
                    }
                }
            }
        };
    }

    public Observable<Link> getLink(final String id) {
        return openDatabase()
                .subscribeOn(Schedulers.io())
                .map(new Func1<SQLiteDatabase, Link>() {
                    @Override
                    public Link call(SQLiteDatabase sqLiteDatabase) {
                        return (Link) TABLE_LINK.query(id);
                    }
                });
    }

    public Action1<? super Listing> cacheListing() {
        return new Action1<Listing>() {
            @Override
            public void call(Listing listing) {
                for (Thing thing : listing.getChildren()) {
                    if (thing instanceof Link) {
                        Link link = (Link) thing;
                        reddit.comments(link.getSubreddit(), link.getId(), link.getCommentId(), Sort.CONFIDENCE.toString(), true, true, link.getContextLevel(), 3, 25)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .doOnNext(storeLink())
                                .subscribe(new ObserverEmpty<Link>());
                    }
                }
            }
        };
    }
}
