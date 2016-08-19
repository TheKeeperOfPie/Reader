/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
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
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.utils.UtilsRx;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks extends LinksController {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    public static final int LIMIT = 25;

    private Subreddit subreddit = new Subreddit();

    private EventHolder eventHolder = new EventHolder();
    private Stack<Pair<Integer, Link>> linksHidden = new Stack<>();

    @Inject Reddit reddit;
    @Inject RedditDatabase redditDatabase;
    ControllerUser controllerUser;

    public ControllerLinks(ControllerUser controllerUser) {
        CustomApplication.getComponentMain().inject(this);
        this.controllerUser = controllerUser;
        subreddit.setUrl("/");
    }

    public EventHolder getEventHolder() {
        if (!eventHolder.getLoading().getValue() && listing.getChildren().isEmpty()) {
            reloadSubreddit();
        }

        return eventHolder;
    }

    public Observable<Subreddit> setParameters(String subredditName, Sort sort, Time time) {
        if (!TextUtils.equals(subredditName, subreddit.getDisplayName())) {
            subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddit.setUrl("/r/" + subredditName + "/");

            setListing(new Listing());

            publishUpdate();
            eventHolder.getSort().call(sort);
            eventHolder.getTime().call(time);

            return reloadSubreddit();
        }

        return Observable.empty();
    }

    private void setListing(Listing listing) {
        this.listing = listing;
        this.linksHidden.clear();
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
                        setListing(new Listing());
                        publishUpdate();
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
                .flatMap(UtilsRx.flatMapWrapError(response -> Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
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
                publishUpdate();
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
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            publishUpdate();
            reloadAllLinks(true)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    public void setSort(Sort sort) {
        if (eventHolder.getSort().getValue() != sort) {
            eventHolder.getSort().call(sort);
            reloadAllLinks(true)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    public void setTime(Time time) {
        if (eventHolder.getTime().getValue() != time) {
            eventHolder.getTime().call(time);
            reloadAllLinks(true)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    public Link getLink(int position) {
        // TODO: Really gotta figure out the position shifts
        return (Link) listing.getChildren().get(position - 1);
    }

    public Observable<Listing> reloadAllLinks(final boolean scroll) {
        Observable<Listing> observable = reddit.links(subreddit.getUrl(), eventHolder.getSort().getValue().toString(), eventHolder.getTime().getValue().toString(), LIMIT, null)
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .flatMap(listing -> {
                    if (!listing.getChildren().isEmpty() && !(listing.getChildren().get(0) instanceof Link)) {
                        return Observable.error(new Exception());
                    }

                    return Observable.just(listing);
                })
                .doOnNext(redditDatabase.storeListing(subreddit, eventHolder.getSort().getValue(), eventHolder.getTime().getValue()))
                .doOnNext(redditDatabase.cacheListing())
                .onErrorReturn(throwable -> {
                    Log.d(TAG, "reloadAllLinks() called with: throwable = [" + throwable + "]", throwable);
                    return new Listing();
                })
                .onErrorResumeNext(Observable.empty())
                .switchIfEmpty(redditDatabase.getLinksForSubreddit(subreddit))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(() -> setLoading(true))
                .doOnNext(listing -> {
                    setListing(listing);

                    publishUpdate();
                })
                .doOnTerminate(() -> setLoading(false));

        return observable;
    }

    public Observable<Listing> loadMoreLinks() {
        if (eventHolder.getLoading().getValue() || TextUtils.isEmpty(listing.getAfter())) {
            return Observable.empty();
        }

        setLoading(true);

        return reddit.links(subreddit.getUrl(), eventHolder.getSort().getValue().toString(), eventHolder.getTime().getValue().toString(), LIMIT, listing.getAfter())
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .doOnNext(redditDatabase.appendListing(subreddit, eventHolder.getSort().getValue(), eventHolder.getTime().getValue()))
                .doOnNext(redditDatabase.cacheListing())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(listing -> {
                    this.listing.addChildren(listing.getChildren());
                    this.listing.setAfter(listing.getAfter());
                    publishUpdate();
                })
                .doOnSubscribe(() -> setLoading(true))
                .doOnTerminate(() -> setLoading(false));
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setLoading(boolean loading) {
        eventHolder.getLoading().call(loading);
    }

    public String getSubredditName() {

        if (TextUtils.isEmpty(subreddit.getDisplayName())) {
            return Reddit.FRONT_PAGE;
        }

        return subreddit.getDisplayName();
    }

    public boolean setReplyText(String name, String text, boolean collapsed) {
        for (int index = 0; index < listing.getChildren().size(); index++) {
            Thing thing = listing.getChildren().get(index);
            if (thing.getName().equals(name)) {
                Replyable replyable = (Replyable) thing;
                replyable.setReplyText(text);
                replyable.setReplyExpanded(!collapsed);
                publishUpdate();
                return true;
            }
        }

        return false;
    }

    public void setNsfw(String name, boolean over18) {
        for (int index = 0; index < listing.getChildren().size(); index++) {
            Thing thing = listing.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);

                    publishUpdate();
                return;
            }
        }
    }

    public void subscribe() {
        final boolean subscribed = subreddit.isUserIsSubscriber();
        subreddit.setUserIsSubscriber(!subreddit.isUserIsSubscriber());
        publishUpdate();
        reddit.subscribe(subreddit.isUserIsSubscriber(), subreddit.getName())
                .doOnError(throwable -> subreddit.setUserIsSubscriber(subscribed))
                .subscribe(subreddit -> publishUpdate());
    }

    public Link hideLink(int position) {
        position--;
        Link link = (Link) listing.getChildren().get(position);
        publishUpdate();
        linksHidden.add(Pair.create(position, link));
        return link;
    }

    public void reshowLastHiddenLink() {
        if (linksHidden.isEmpty()) {
            return;
        }

        Pair<Integer, Link> pair = linksHidden.pop();
        publishUpdate();
    }

    public boolean isOnSpecificSubreddit() {
        return !TextUtils.isEmpty(subreddit.getDisplayName()) && !"/r/all/".equalsIgnoreCase(
                subreddit.getUrl()) && !subreddit.getUrl().contains("+");
    }

    public LinksModel getData() {
        List<Link> links = new ArrayList<>(listing.getChildren().size());

        for (Thing thing : listing.getChildren()) {
            if (thing instanceof Link) {
                links.add((Link) thing);
            }
        }

        boolean showSubreddit = false;

        if (!TextUtils.isEmpty(subreddit.getUrl())) {
            switch (subreddit.getUrl()) {
                case "/":
                case "/r/all/":
                    showSubreddit = true;
            }
        }

        return new LinksModel(subreddit, links, showSubreddit, controllerUser.getUser());
    }

    public Subreddit getSubreddit() {
        return subreddit;
    }

    @Override
    public void publishUpdate() {
        eventHolder.call(getData());
    }

    public static class EventHolder implements Action1<LinksModel> {

        private BehaviorRelay<LinksModel> relayData = BehaviorRelay.create(new LinksModel());
        private BehaviorRelay<Boolean> relayLoading = BehaviorRelay.create(false);
        private BehaviorRelay<Sort> relaySort = BehaviorRelay.create(Sort.HOT);
        private BehaviorRelay<Time> relayTime = BehaviorRelay.create(Time.ALL);
        private BehaviorRelay<LinksError> relayErrors = BehaviorRelay.create();

        @Override
        public void call(LinksModel linksModel) {
            relayData.call(linksModel);
        }

        public Observable<LinksModel> getData() {
            return relayData.skip(1)
                    .startWith(relayData.getValue());
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

        public BehaviorRelay<LinksError> getErrors() {
            return relayErrors;
        }
    }
}
