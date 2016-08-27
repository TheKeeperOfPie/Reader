/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.database.reddit.RedditDatabase;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.utils.UtilsRx;

import java.util.List;
import java.util.Stack;

import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks extends LinksController<ControllerLinks.EventHolder> {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    public static final int LIMIT = 25;

    private Subreddit subreddit = new Subreddit();

    private Stack<Pair<Integer, Link>> linksHidden = new Stack<>();

    @Inject Reddit reddit;
    @Inject RedditDatabase redditDatabase;
    ControllerUser controllerUser;

    public ControllerLinks(ControllerUser controllerUser) {
        super(new EventHolder());

        CustomApplication.getComponentMain().inject(this);
        this.controllerUser = controllerUser;
        subreddit.setUrl("/");

        initialize();
    }

    private void initialize() {
        initialize(listings -> {
            if (listings.isEmpty()) {
                return reddit.about(subreddit.getUrl())
                        .observeOn(Schedulers.io())
                        .flatMap(UtilsRx.flatMapWrapError(response -> Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                        .onErrorReturn(t -> this.subreddit)
                        .doOnNext(subreddit -> this.subreddit = subreddit)
                        .flatMap(subreddit -> reddit.links(subreddit.getUrl(), getSort().toString(), getTime().toString(), LIMIT, null))
                        .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                        .flatMap(listing -> {
                            if (!(listing.getChildren().get(0) instanceof Link)) {
                                return Observable.error(new Exception());
                            }

                            return Observable.just(listing);
                        })
                        .doOnNext(redditDatabase.storeListing(subreddit, getSort(), getTime()))
                        .doOnNext(redditDatabase.cacheListing())
                        .onErrorResumeNext(Observable.empty())
                        .switchIfEmpty(redditDatabase.getLinksForSubreddit(subreddit));
            }

            Listing listingLast = listings.get(listings.size() - 1);

            return reddit.links(subreddit.getUrl(), getSort().toString(), getTime().toString(), LIMIT, listingLast.getAfter())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                    .doOnNext(redditDatabase.appendListing(subreddit, getSort(), getTime()))
                    .doOnNext(redditDatabase.cacheListing());
        }, this::convert);
    }

    private Sort getSort() {
        return getEventHolder().getSort().getValue();
    }

    private Time getTime() {
        return getEventHolder().getTime().getValue();
    }

    private Observable<LinksModel> convert(List<Listing> listings) {
        return Observable.from(listings)
                .flatMap(listing -> Observable.from(listing.getChildren()))
                .ofType(Link.class)
                .toList()
                .map(links -> {
                    boolean showSubreddit = false;

                    if (!TextUtils.isEmpty(subreddit.getUrl())) {
                        switch (subreddit.getUrl()) {
                            case "/":
                            case "/r/all/":
                                showSubreddit = true;
                        }
                    }

                    return new LinksModel(subreddit, links, showSubreddit, controllerUser.getUser());
                });
    }

    public Observable<Subreddit> setParameters(String subredditName, Sort sort, Time time) {
        if (!TextUtils.equals(subredditName, subreddit.getDisplayName())) {
            subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddit.setUrl("/r/" + subredditName + "/");

            publishUpdate();
            getEventHolder().getSort().call(sort);
            getEventHolder().getTime().call(time);

            reload();
        }

        return Observable.empty();
    }

    public void loadFrontPage(Sort sort, boolean force) {
        if (force || !TextUtils.isEmpty(subreddit.getDisplayName())) {
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            publishUpdate();
            reload();
        }
    }

    public void setSort(Sort sort) {
        if (getEventHolder().getSort().getValue() != sort) {
            getEventHolder().getSort().call(sort);
            reload();
        }
    }

    public void setTime(Time time) {
        if (getEventHolder().getTime().getValue() != time) {
            getEventHolder().getTime().call(time);
            reload();
        }
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setLoading(boolean loading) {
        getEventHolder().getLoading().call(loading);
    }

    public String getSubredditName() {

        if (TextUtils.isEmpty(subreddit.getDisplayName())) {
            return Reddit.FRONT_PAGE;
        }

        return subreddit.getDisplayName();
    }

    public boolean setReplyText(String name, String text, boolean collapsed) {
//        for (int index = 0; index < listing.getChildren().size(); index++) {
//            Thing thing = listing.getChildren().get(index);
//            if (thing.getName().equals(name)) {
//                Replyable replyable = (Replyable) thing;
//                replyable.setReplyText(text);
//                replyable.setReplyExpanded(!collapsed);
//                publishUpdate();
//                return true;
//            }
//        }

        return false;
    }

    public void setNsfw(String name, boolean over18) {
//        for (int index = 0; index < listing.getChildren().size(); index++) {
//            Thing thing = listing.getChildren().get(index);
//            if (thing.getName().equals(name)) {
//                ((Link) thing).setOver18(over18);
//
//                    publishUpdate();
//                return;
//            }
//        }
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
        return new Link();
//        position--;
//        Link link = (Link) listing.getChildren().get(position);
//        publishUpdate();
//        linksHidden.add(Pair.create(position, link));
//        return link;
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

    public Subreddit getSubreddit() {
        return subreddit;
    }

    public static class EventHolder extends LinksController.EventHolder {

        private BehaviorRelay<Sort> relaySort = BehaviorRelay.create(Sort.HOT);
        private BehaviorRelay<Time> relayTime = BehaviorRelay.create(Time.ALL);

        public BehaviorRelay<Sort> getSort() {
            return relaySort;
        }

        public BehaviorRelay<Time> getTime() {
            return relayTime;
        }
    }
}
