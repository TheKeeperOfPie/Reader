/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.database.reddit;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
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
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.utils.UtilsImage;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
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
        return listing -> openDatabase()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(sqLiteDatabase -> Observable.from(listing.getChildren()))
                .ofType(Link.class)
                .toList()
                .doOnNext(TABLE_LINK::storeLinks)
                .flatMap(Observable::from)
                .map(Thing::getId)
                .toList()
                .doOnNext(ids -> TABLE_SUBREDDIT_PAGE.storePage(subreddit, ids, sort, time))
                .subscribe(new ObserverEmpty<>());
    }

    public Action1<Listing> appendListing(final Subreddit subreddit, final Sort sort, final Time time) {
        return listing -> openDatabase()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(sqLiteDatabase -> Observable.from(listing.getChildren()))
                .ofType(Link.class)
                .toList()
                .doOnNext(TABLE_LINK::storeLinks)
                .flatMap(Observable::from)
                .map(Thing::getId)
                .toList()
                .doOnNext(ids -> TABLE_SUBREDDIT_PAGE.appendPage(subreddit, ids, sort, time))
                .subscribe(new ObserverEmpty<>());
    }

    public Observable<Listing> getLinksForSubreddit(final Subreddit subreddit) {
        return openDatabase()
                .subscribeOn(Schedulers.io())
                .map(sqLiteDatabase -> TABLE_SUBREDDIT_PAGE.getLinksForSubreddit(subreddit))
                .filter(strings -> strings != null)
                .map(TABLE_LINK::queryListing)
                .map(Listing::new);
    }

    public Action1<Link> storeLink() {
        return link -> openDatabase()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext(TABLE_LINK.storeLink(link))
                .doOnNext(cacheLinkImage(link))
                .subscribe(new ObserverEmpty<>());
    }

    public Action1<SQLiteDatabase> cacheLinkImage(final Link link) {
        return sqLiteDatabase -> {
            boolean preventCache = link.isOver18() && !sharedPreferences.getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true);

            if (!link.isSelf()
                    && !Reddit.DEFAULT.equals(link.getThumbnail())
                    && !preventCache) {
                String thumbnail = UtilsImage.parseThumbnail(link);

                if (URLUtil.isNetworkUrl(thumbnail)) {
                    picasso.load(thumbnail)
                            .priority(Picasso.Priority.LOW)
                            .fetch();
                }

                if (UtilsImage.placeImageUrl(link)) {
                    picasso.load(link.getUrl())
                            .priority(Picasso.Priority.LOW)
                            .fetch();
                }
            }
        };
    }

    public Observable<Link> getLink(final String id) {
        return openDatabase()
                .subscribeOn(Schedulers.io())
                .map(sqLiteDatabase -> (Link) TABLE_LINK.query(id));
    }

    public Action1<? super Listing> cacheListing() {
        return listing -> Observable.from(listing.getChildren())
                .subscribeOn(Schedulers.computation())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .ofType(Link.class)
                .flatMap(link -> reddit.comments(link.getSubreddit(), link.getId(), link.getCommentId(), Sort.CONFIDENCE.toString(), true, true, link.getContextLevel(), 3, 25))
                .doOnNext(storeLink())
                .subscribe(new ObserverEmpty<>());
    }
}
