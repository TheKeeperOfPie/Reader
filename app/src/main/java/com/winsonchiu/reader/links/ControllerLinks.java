/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.adapter.RxAdapterEvent;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.database.reddit.RedditDatabase;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.utils.UtilsRx;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks implements ControllerLinksBase {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    public static final int LIMIT = 25;

    private boolean isLoading;
    private Listing listingLinks = new Listing();
    private Subreddit subreddit = new Subreddit();
    private Sort sort = Sort.HOT;
    private Time time = Time.ALL;

    private EventHolder eventHolder = new EventHolder();

    @Inject Reddit reddit;
    @Inject RedditDatabase redditDatabase;

    public ControllerLinks() {
        CustomApplication.getComponentMain().inject(this);
        subreddit.setUrl("/");
    }

    public EventHolder getEventHolder() {
        if (!isLoading() && listingLinks.getChildren().isEmpty()) {
            reloadSubreddit();
        }

        return eventHolder;
    }

    public Observable<Subreddit> setParameters(String subredditName, Sort sort, Time time) {
        if (!TextUtils.equals(subredditName, subreddit.getDisplayName())) {
            this.sort = sort;
            this.time = time;
            subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddit.setUrl("/r/" + subredditName + "/");
            int size = sizeLinks();
            listingLinks = new Listing();
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, 0, size + 1));

            eventHolder.getSort().call(sort);
            eventHolder.getTime().call(time);
            return reloadSubreddit();
        }

        return Observable.empty();
    }

    public Observable<Subreddit> reloadSubreddit() {
        Observable<Subreddit> observable = reddit.about(subreddit.getUrl())
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response -> Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new FinalizingSubscriber<Subreddit>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void error(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void next(Subreddit next) {
                        ControllerLinks.this.subreddit = next;
                        listingLinks = new Listing();
                        eventHolder.call(new RxAdapterEvent<>(getData()));
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                        reloadAllLinks(true)
                                .subscribe(new ObserverEmpty<>());
                    }
                });
        return observable;
    }

    public Observable<Subreddit> reloadSubredditOnly() {
        Observable<Subreddit> observable = reddit.about(subreddit.getUrl())
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError((UtilsRx.Call<String, Subreddit>) response -> Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .flatMap(subreddit1 -> {
                    if (!TextUtils.isEmpty(subreddit1.getUrl())) {
                        return Observable.just(subreddit1);
                    }
                    else {
                        return Observable.error(new Exception("Subreddit URL empty"));
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new FinalizingSubscriber<Subreddit>() {
            @Override
            public void start() {
                setLoading(true);
            }

            @Override
            public void error(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void next(Subreddit next) {
                ControllerLinks.this.subreddit = next;
                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0));
            }

            @Override
            public void finish() {
                setLoading(false);
            }
        });

        return observable;
    }

    public void loadFrontPage(Sort sort, boolean force) {
        if (force || !TextUtils.isEmpty(subreddit.getDisplayName())) {
            this.sort = sort;
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0));
            reloadAllLinks(true)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            eventHolder.getSort().call(sort);
            reloadAllLinks(true)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            eventHolder.getTime().call(time);
            reloadAllLinks(true)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    public Link getLink(int position) {
        // TODO: Really gotta figure out the position shifts
        return (Link) listingLinks.getChildren()
                .get(position - 1);
    }

    @Override
    public int sizeLinks() {
        return listingLinks.getChildren() == null ? 0 : listingLinks.getChildren()
                .size();
    }

    public Observable<Listing> reloadAllLinks(final boolean scroll) {
        Observable<Listing> observable = reddit.links(subreddit.getUrl(), sort.toString(), time.toString(), LIMIT, null)
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .flatMap(listing -> {
                    if (!listing.getChildren().isEmpty() && !(listing.getChildren().get(0) instanceof Link)) {
                        return Observable.error(new Exception());
                    }

                    return Observable.just(listing);
                })
                .doOnNext(redditDatabase.storeListing(subreddit, sort, time))
                .doOnNext(redditDatabase.cacheListing())
                .onErrorResumeNext(Observable.<Listing>empty())
                .switchIfEmpty(redditDatabase.getLinksForSubreddit(subreddit))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(() -> setLoading(true))
                .doOnNext(listing -> {
                    listingLinks = listing;

                    eventHolder.call(new RxAdapterEvent<>(getData()));
                })
                .doOnTerminate(() -> setLoading(false));

        Log.d(TAG, "reloadAllLinks");

        return observable;
    }

    public Observable<Listing> loadMoreLinks() {
        if (isLoading || TextUtils.isEmpty(listingLinks.getAfter())) {
            return Observable.empty();
        }

        setLoading(true);

        return reddit.links(subreddit.getUrl(), sort.toString(), time.toString(), LIMIT, listingLinks.getAfter())
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .doOnNext(redditDatabase.appendListing(subreddit, sort, time))
                .doOnNext(redditDatabase.cacheListing())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(listing -> {
                    final int positionStart = listingLinks.getChildren()
                            .size();
                    listingLinks.addChildren(listing.getChildren());
                    listingLinks.setAfter(listing.getAfter());
                    eventHolder.call(new RxAdapterEvent<>(getData(),
                            RxAdapterEvent.Type.INSERT,
                            positionStart + 1,
                            listingLinks.getChildren().size() - positionStart));
                })
                .doOnSubscribe(() -> setLoading(true))
                .doOnTerminate(() -> setLoading(false));
    }

    @Override
    public Subreddit getSubreddit() {
        return subreddit;
    }

    public Observable<String> deletePost(Link link) {
        int index = listingLinks.getChildren()
                .indexOf(link);

        if (index < 0) {
            return Observable.empty();
        }

        listingLinks.getChildren()
                .remove(index);

        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, index + 1));


        return reddit.delete(link);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        eventHolder.getLoading().call(isLoading);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getSubredditName() {

        if (TextUtils.isEmpty(subreddit.getDisplayName())) {
            return Reddit.FRONT_PAGE;
        }

        return subreddit.getDisplayName();
    }

    public Sort getSort() {
        return sort;
    }

    public Time getTime() {
        return time;
    }

    @Override
    public boolean showSubreddit() {
        return "/".equals(subreddit.getUrl()) || "/r/all/".equals(subreddit.getUrl());
    }

    @Override
    public boolean setReplyText(String name, String text, boolean collapsed) {

        for (int index = 0; index < listingLinks.getChildren().size(); index++) {
            Thing thing = listingLinks.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);


                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
                return true;
            }
        }
        return false;
    }

    @Override
    public void setNsfw(String name, boolean over18) {

        for (int index = 0; index < listingLinks.getChildren().size(); index++) {
            Thing thing = listingLinks.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);

                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
                return;
            }
        }
    }

    public Link remove(int position) {
        Link link = (Link) listingLinks.getChildren().remove(position);
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, position + 1));
        return link;
    }

    public void subscribe() {
        final boolean subscribed = subreddit.isUserIsSubscriber();
        subreddit.setUserIsSubscriber(!subreddit.isUserIsSubscriber());
        reddit.subscribe(subreddit.isUserIsSubscriber(), subreddit.getName())
                .doOnError(throwable -> subreddit.setUserIsSubscriber(subscribed))
                .subscribe(subreddit -> eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0)));
    }

    public void add(int position, Link link) {
        listingLinks.getChildren().add(position, link);
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, position + 1));
    }

    public void clearViewed(Historian historian) {

        List<Integer> indexesToRemove = new ArrayList<>();

        for (int index = 0; index < listingLinks.getChildren().size(); index++) {
            Link link = (Link) listingLinks.getChildren().get(index);
            if (historian.contains(link.getName())) {
                indexesToRemove.add(0, index);
            }
        }

        for (int index : indexesToRemove) {
            listingLinks.getChildren().remove(index);
            // Offset 1 for subreddit header
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, index + 1));
        }

    }

    public boolean isOnSpecificSubreddit() {
        return !TextUtils.isEmpty(subreddit.getDisplayName()) && !"/r/all/".equalsIgnoreCase(
                subreddit.getUrl()) && !subreddit.getUrl().contains("+");
    }

    public int indexOf(Link link) {
        return listingLinks.getChildren().indexOf(link);
    }

    @Nullable
    public Link getPreviousLink(Link linkCurrent, int offset) {
        int index = indexOf(linkCurrent) - offset;
        if (index >= 0 && !listingLinks.getChildren().isEmpty()) {
            return (Link) listingLinks.getChildren().get(index);
        }

        return null;
    }

    @Nullable
    public Link getNextLink(Link linkCurrent, int offset) {
        int index = indexOf(linkCurrent) + offset;
        if (index < listingLinks.getChildren().size() && !listingLinks.getChildren().isEmpty()) {
            return (Link) listingLinks.getChildren().get(index);
        }

        return null;
    }

    public Pair<Subreddit, List<Thing>> getData() {
        return Pair.create(subreddit, listingLinks.getChildren());
    }

    public static class EventHolder implements Action1<RxAdapterEvent<Pair<Subreddit, List<Thing>>>> {

        private BehaviorRelay<RxAdapterEvent<Pair<Subreddit, List<Thing>>>> relayData = BehaviorRelay.create(new RxAdapterEvent<>(Pair.create(new Subreddit(), new ArrayList<>())));
        private BehaviorRelay<Boolean> relayLoading = BehaviorRelay.create(false);
        private BehaviorRelay<Sort> relaySort = BehaviorRelay.create(Sort.HOT);
        private BehaviorRelay<Time> relayTime = BehaviorRelay.create(Time.ALL);

        @Override
        public void call(RxAdapterEvent<Pair<Subreddit, List<Thing>>> event) {
            relayData.call(event);
        }

        public Observable<RxAdapterEvent<Pair<Subreddit, List<Thing>>>> getData() {
            Pair<Subreddit, List<Thing>> data = relayData.hasValue() ? relayData.getValue().getData() : Pair.create(new Subreddit(), new ArrayList<>());

            return Observable.just(new RxAdapterEvent<>(data))
                    .mergeWith(relayData.skip(1));
        }

        public BehaviorRelay<Boolean> getLoading() {
            return relayLoading;
        }

        public BehaviorRelay<Sort> getSort() {
            return relaySort;
        }

        public BehaviorRelay<Time> getTime() {
            return relayTime;
        }
    }
}
