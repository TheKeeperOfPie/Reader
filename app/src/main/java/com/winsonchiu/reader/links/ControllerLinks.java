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
import com.winsonchiu.reader.ControllerUser;
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
import java.util.Stack;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    public static final int LIMIT = 25;

    private boolean isLoading;
    private Listing listingLinks = new Listing();
    private Subreddit subreddit = new Subreddit();
    private Sort sort = Sort.HOT;
    private Time time = Time.ALL;

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

            setListing(new Listing());

            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, 0, listingLinks.getChildren().size() + 1));
            eventHolder.getSort().call(sort);
            eventHolder.getTime().call(time);

            return reloadSubreddit();
        }

        return Observable.empty();
    }

    private void setListing(Listing listing) {
        this.listingLinks = listing;
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
        return (Link) listingLinks.getChildren().get(position - 1);
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
                    setListing(listing);

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

    public void subscribe() {
        final boolean subscribed = subreddit.isUserIsSubscriber();
        subreddit.setUserIsSubscriber(!subreddit.isUserIsSubscriber());
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0));
        reddit.subscribe(subreddit.isUserIsSubscriber(), subreddit.getName())
                .doOnError(throwable -> subreddit.setUserIsSubscriber(subscribed))
                .subscribe(subreddit -> eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0)));
    }

    public Link hideLink(int position) {
        position--;
        Link link = (Link) listingLinks.getChildren().get(position);
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, position + 1));
        linksHidden.add(Pair.create(position, link));
        return link;
    }

    public void reshowLastHiddenLink() {
        if (linksHidden.isEmpty()) {
            return;
        }

        Pair<Integer, Link> pair = linksHidden.pop();
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, pair.first + 1));
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

    public LinksModel getData() {
        List<Link> links = new ArrayList<>(listingLinks.getChildren().size());

        for (Thing thing : listingLinks.getChildren()) {
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

    public int sizeLinks() {
        return listingLinks.getChildren() == null ? 0 : listingLinks.getChildren().size();
    }

//    public void report() {
//
//        View viewDialog = LayoutInflater.from(itemView.getContext())
//                .inflate(R.layout.dialog_text_input, null, false);
//        final EditText editText = (EditText) viewDialog.findViewById(R.id.edit_text);
//        editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(100)});
//        new AlertDialog.Builder(itemView.getContext())
//                .setView(viewDialog)
//                .setTitle(R.string.item_report)
//                .setPositiveButton(R.string.ok, (dialog, which) -> {
//                    eventListener.report(link, "other", editText.getText().toString());
//                })
//                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
//                .show();
//        break;
//    }
//
//    private void requestReport(final String reason) {
//        String author = link.getAuthor();
//        String title = link.getTitle();
//
//        new AlertDialog.Builder(itemView.getContext())
//                .setMessage(resources.getString(R.string.report, title, author, reason))
//                .setPositiveButton(R.string.ok, (dialog, which) -> {
//                    eventListener.report(link, reason, null);
//                })
//                .setNegativeButton(R.string.cancel, null)
//                .show();
//    }

    public static class EventHolder implements Action1<RxAdapterEvent<LinksModel>> {

        private BehaviorRelay<RxAdapterEvent<LinksModel>> relayData = BehaviorRelay.create(new RxAdapterEvent<>(new LinksModel()));
        private BehaviorRelay<Boolean> relayLoading = BehaviorRelay.create(false);
        private BehaviorRelay<Sort> relaySort = BehaviorRelay.create(Sort.HOT);
        private BehaviorRelay<Time> relayTime = BehaviorRelay.create(Time.ALL);

        @Override
        public void call(RxAdapterEvent<LinksModel> event) {
            relayData.call(event);
        }

        public Observable<RxAdapterEvent<LinksModel>> getData() {
            return relayData;
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
