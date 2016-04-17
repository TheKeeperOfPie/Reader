/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.adapter.RxAdapterEvent;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.utils.UtilsRx;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class ControllerSearch {

    public static final int PAGE_SUBREDDITS = 0;
    public static final int PAGE_LINKS_SUBREDDIT = 1;
    public static final int PAGE_LINKS = 2;
    public static final int PAGE_SUBREDDITS_RECOMMENDED = 3;

    private static final String TAG = ControllerSearch.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private ControllerUser controllerUser;
    private Set<Listener> listeners = new HashSet<>();
    private SharedPreferences preferences;
    private Listing subredditsLoaded = new Listing();
    private Listing subredditsSubscribed = new Listing();
    private Listing subreddits = new Listing();
    private Listing subredditsRecommended = new Listing();
    private Listing links = new Listing();
    private Listing linksSubreddit = new Listing();
    private String query = "";
    private Sort sort = Sort.RELEVANCE;
    private Sort sortSubreddits = Sort.RELEVANCE;
    private Time time = Time.ALL;
    private volatile int currentPage;
    private Subscription subscriptionSubreddits;
    private Subscription subscriptionLinks;
    private Subscription subscriptionLinksSubreddit;
    private boolean isLoadingLinks;
    private boolean isLoadingLinksSubreddit;

    private List<Subscription> subscriptionsSubredditsRecommended = new ArrayList<>();
    private String currentSubreddit;

    private EventHolder eventHolder = new EventHolder();

    @Inject AccountManager accountManager;
    @Inject Reddit reddit;

    public ControllerSearch(Context context, ControllerLinks controllerLinks, ControllerUser controllerUser) {
        CustomApplication.getComponentMain().inject(this);
        this.controllerLinks = controllerLinks;
        this.controllerUser = controllerUser;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
    }

    public EventHolder getEventHolder() {
        return eventHolder;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.getAdapterSearchSubreddits().notifyDataSetChanged();
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Observable<Listing> setData(int page, String query, Sort sort, Time time){
        setCurrentPage(page);
        setSort(sort);
        setTime(time);
        return setQuery(query);
    }

    public Observable<Listing> setQuery(String query) {
        this.query = query;
        if ((TextUtils.isEmpty(query) || query.length() < 2) && currentPage == PAGE_SUBREDDITS) {
            if (subscriptionSubreddits != null && !subscriptionSubreddits.isUnsubscribed()) {
                subscriptionSubreddits.unsubscribe();
                subscriptionSubreddits = null;
            }
            subreddits = subredditsSubscribed;
            for (Listener listener : listeners) {
                listener.getAdapterSearchSubreddits()
                        .notifyDataSetChanged();
            }
        }
        else {
            return reloadCurrentPage();
        }
        return Observable.empty();
    }

    public void reloadSubscriptionList() {

        Listing listing = new Listing();
        String subscriptionsJson = preferences.getString(AppSettings.SUBSCRIPTIONS + controllerUser.getUser().getName(), "");

        Log.d(TAG, "subscriptionsJson: " + subscriptionsJson);

        if (!TextUtils.isEmpty(subscriptionsJson)) {

            try {
                JsonNode jsonNode = ComponentStatic.getObjectMapper().readValue(subscriptionsJson, JsonNode.class);

                for (JsonNode node : jsonNode) {
                    listing.getChildren().add(Subreddit.fromJson(node));
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        subredditsSubscribed = listing;

        if (TextUtils.isEmpty(query)) {
            subreddits = subredditsSubscribed;
            for (Listener listener : listeners) {
                listener.getAdapterSearchSubreddits()
                        .notifyDataSetChanged();
            }
        }

        String url;

        if (controllerUser.hasUser()) {
            url = Reddit.OAUTH_URL + "/subreddits/mine/subscriber";
        }
        else {
            url = Reddit.OAUTH_URL + "/subreddits/default";
        }

        reddit.subreddits(url, null, 100)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Listing listing) {
                        subredditsLoaded.addChildren(listing.getChildren());
                        subredditsLoaded.setAfter(listing.getAfter());
                        if (listing.getChildren().size() == 100) {
                            loadMoreSubscriptions();
                        }
                        else {
                            loadContributorSubreddits();
                        }
                    }
                });
    }

    private void loadMoreSubscriptions() {
        String url;

        if (controllerUser.hasUser()) {
            url = Reddit.OAUTH_URL + "/subreddits/mine/subscriber";
        }
        else {
            url = Reddit.OAUTH_URL + "/subreddits/default";
        }

        reddit.subreddits(url, subredditsLoaded.getAfter(), 100)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Listing listing) {
                        subredditsLoaded.addChildren(listing.getChildren());
                        subredditsLoaded.setAfter(listing.getAfter());
                        if (listing.getChildren().size() == 100) {
                            loadMoreSubscriptions();
                        }
                        else {
                            loadContributorSubreddits();
                        }
                    }
                });
    }

    private void saveSubscriptions(Listing listing) {

        Log.d(TAG, "saveSubscriptions with listing: " + listing.getChildren());

        boolean sort = subredditsSubscribed.getChildren().isEmpty();

        if (controllerUser.hasUser()) {
            ListIterator<Thing> iterator = subredditsSubscribed.getChildren().listIterator();
            while (iterator.hasNext()) {
                Thing next = iterator.next();
                int index = listing.getChildren().indexOf(next);
                if (index > -1) {
                    iterator.set(listing.getChildren().get(index));
                }
                else {
                    iterator.remove();
                }
            }
        }

        subredditsSubscribed.addChildren(listing.getChildren());

        subredditsLoaded = new Listing();

        if (sort) {
            Collections.sort(subredditsSubscribed.getChildren(), new Comparator<Thing>() {
                @Override
                public int compare(Thing lhs, Thing rhs) {
                    return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                }
            });
        }

        saveSubscriptions();
    }

    public void saveSubscriptions() {

        String data;

        try {
            data = ComponentStatic.getObjectMapper().writeValueAsString(subredditsSubscribed.getChildren());

        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        preferences.edit().putString(AppSettings.SUBSCRIPTIONS + controllerUser.getUser().getName(), data).apply();

        if (TextUtils.isEmpty(query)) {
            subreddits = subredditsSubscribed;
            for (Listener listener : listeners) {
                listener.getAdapterSearchSubreddits()
                        .notifyDataSetChanged();
            }
        }
    }

    private void loadContributorSubreddits() {
        if (TextUtils.isEmpty(controllerUser.getUser().getName())) {
            saveSubscriptions(subredditsLoaded);
            return;
        }

        String url = Reddit.OAUTH_URL + "/subreddits/mine/contributor";

        reddit.subreddits(url, null, 100)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Listing listing) {
                        subredditsLoaded.addChildren(listing.getChildren());
                        subredditsLoaded.setAfter(listing.getAfter());
                        if (listing.getChildren().size() == 100) {
                            loadMoreContributorSubreddits();
                        }
                        else {
                            saveSubscriptions(subredditsLoaded);
                        }
                    }
                });
    }

    private void loadMoreContributorSubreddits() {
        String url = Reddit.OAUTH_URL + "/subreddits/mine/contributor";

        reddit.subreddits(url, subredditsLoaded.getAfter(), 100)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Listing listing) {
                        subredditsLoaded.addChildren(listing.getChildren());
                        subredditsLoaded.setAfter(listing.getAfter());
                        if (listing.getChildren().size() == 100) {
                            loadMoreContributorSubreddits();
                        }
                        else {
                            saveSubscriptions(subredditsLoaded);
                        }
                    }
                });
    }

    public Observable<Listing> reloadCurrentPage() {
        Log.d(TAG, "reloadCurrentPage");
        switch (currentPage) {
            case PAGE_SUBREDDITS:
                if (TextUtils.isEmpty(query) || query.length() < 2) {
                    for (Listener listener : listeners) {
                        listener.getAdapterSearchSubreddits()
                                .notifyDataSetChanged();
                    }
                }
                else {
                    reloadSubreddits();
                }
                break;
            case PAGE_LINKS:
                if (!TextUtils.isEmpty(query)) {
                    return reloadLinks();
                }
                break;
            case PAGE_LINKS_SUBREDDIT:
                if (!TextUtils.isEmpty(query)) {
                    return reloadLinksSubreddit();
                }
                break;
            case PAGE_SUBREDDITS_RECOMMENDED:
                if (!controllerLinks.getSubreddit().getDisplayName().equals(currentSubreddit)) {
                    reloadSubredditsRecommended();
                }
                break;
        }

        return Observable.empty();
    }

    public void reloadSubreddits() {
        if (subscriptionSubreddits != null && !subscriptionSubreddits.isUnsubscribed()) {
            subscriptionSubreddits.unsubscribe();
            subscriptionSubreddits = null;
        }

        // TODO: Move this to asynchronous with Thread cancelling

        String queryTrimmed = query.toLowerCase().replaceAll("\\s", "");

        Listing subscribedResults = new Listing();
        List<Thing> resultsThatContainQueryInTitle = new ArrayList<>();
        List<Thing> resultsThatContainQueryInDescription = new ArrayList<>();

        for (Thing thing : subredditsSubscribed.getChildren()) {
            Subreddit subreddit = (Subreddit) thing;
            if (subreddit.getDisplayName().toLowerCase().startsWith(queryTrimmed)) {
                subscribedResults.getChildren().add(subreddit);
            }
            else if (subreddit.getDisplayName().toLowerCase().contains(queryTrimmed)) {
                resultsThatContainQueryInTitle.add(subreddit);
            }
            else if (subreddit.getPublicDescription().toLowerCase().contains(queryTrimmed)) {
                resultsThatContainQueryInDescription.add(subreddit);
            }
        }

        subscribedResults.addChildren(resultsThatContainQueryInTitle);
        subscribedResults.addChildren(resultsThatContainQueryInDescription);

        subreddits = subscribedResults;
        for (Listener listener : listeners) {
            listener.getAdapterSearchSubreddits().notifyDataSetChanged();
        }

        try {
            subscriptionSubreddits = reddit.subredditsSearch(URLEncoder.encode(query, Reddit.UTF_8).replaceAll("\\s", ""), sort.toString())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                    .subscribe(new Observer<Listing>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(Listing listing) {
                            Iterator<Thing> iterator = listing.getChildren().iterator();
                            while (iterator.hasNext()) {
                                Subreddit subreddit = (Subreddit) iterator.next();
                                if (subreddit.getSubredditType()
                                        .equalsIgnoreCase(Subreddit.PRIVATE) && !subreddit.isUserIsContributor()) {
                                    iterator.remove();
                                }
                            }

                            subreddits.addChildren(listing.getChildren());
                            for (final Listener listener : listeners) {
                                listener.getAdapterSearchSubreddits().notifyDataSetChanged();
                            }
                        }
                    });
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void reloadSubredditsRecommended() {

        for (Subscription subscription : subscriptionsSubredditsRecommended) {
            subscription.unsubscribe();
        }

        subscriptionsSubredditsRecommended.clear();

        subredditsRecommended.getChildren().clear();
        for (Listener listener : listeners) {
            listener.getAdapterSearchSubredditsRecommended().notifyDataSetChanged();
        }

        StringBuilder builderOmit = new StringBuilder();
        for (Thing thing : subredditsSubscribed.getChildren()) {
            Subreddit subreddit = (Subreddit) thing;
            builderOmit.append(subreddit.getDisplayName());
            builderOmit.append(",");
        }

        currentSubreddit = controllerLinks.getSubreddit().getDisplayName();

        reddit.recommend(currentSubreddit, builderOmit.toString())
                .flatMap(UtilsRx.flatMapWrapError( response -> {
                    final JSONArray jsonArray = new JSONArray(response);
                    List<String> names = new ArrayList<>(jsonArray.length());
                    for (int index = 0; index < jsonArray.length(); index++) {
                            /*
                                No idea why the API returns a {"sr_name": "subreddit"} rather than an
                                array of Strings, but we'll convert it.
                             */
                        JSONObject dataSubreddit = jsonArray.getJSONObject(index);
                        names.add(dataSubreddit.optString("sr_name"));
                    }

                    return names;
                }))
                .flatMap(Observable::from)
                .flatMap(next -> reddit.about("/r/" + next + "/"))
                .flatMap(UtilsRx.flatMapWrapError(response -> Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new FinalizingSubscriber<Subreddit>() {
                    @Override
                    public void start() {
                        subredditsRecommended.getChildren().clear();
                    }

                    @Override
                    public void next(Subreddit next) {
                        subredditsRecommended.getChildren().add(next);
                    }

                    @Override
                    public void finish() {
                        Collections.sort(subredditsRecommended.getChildren(),
                                new Comparator<Thing>() {
                                    @Override
                                    public int compare(Thing lhs,
                                                       Thing rhs) {
                                        return ((Subreddit) lhs)
                                                .getDisplayName()
                                                .compareToIgnoreCase(
                                                        ((Subreddit) rhs)
                                                                .getDisplayName());
                                    }
                                });
                        for (Listener listener : listeners) {
                            listener.getAdapterSearchSubredditsRecommended()
                                    .notifyDataSetChanged();
                        }
                    }
                });
    }

    public Observable<Listing> reloadLinks() {
        if (subscriptionLinks != null && !subscriptionLinks.isUnsubscribed()) {
            subscriptionLinks.unsubscribe();
            subscriptionLinks = null;
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }

            Observable<Listing> observable = reddit.search("", URLEncoder.encode(query, Reddit.UTF_8), sortString, time.toString(), null, false)
                    .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))));
            subscriptionLinks = observable
                    .subscribe(new FinalizingSubscriber<Listing>() {
                        @Override
                        public void start() {
                            setLoadingLinks(true);
                        }

                        @Override
                        public void error(Throwable e) {
                        }

                        @Override
                        public void next(Listing listing) {
                            links = listing;
                            eventHolder.callLinks(new RxAdapterEvent<>(links.getChildren()));

                            for (Listener listener : listeners) {
                                listener.scrollToLinks(0);
                            }
                            setLoadingLinks(false);
                        }

                        @Override
                        public void finish() {
                            setLoadingLinks(false);
                        }
                    });

            return observable;
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }

    public Observable<Listing> reloadLinksSubreddit() {
        if (subscriptionLinksSubreddit != null && !subscriptionLinksSubreddit.isUnsubscribed()) {
            subscriptionLinksSubreddit.unsubscribe();
            subscriptionLinksSubreddit = null;
        }

        Subreddit subreddit = controllerLinks.getSubreddit();

        String pathSubreddit = subreddit.getUrl();
        if (pathSubreddit.length() < 2) {
            pathSubreddit = "";
        }

        Observable<Listing> observable = reddit.search(pathSubreddit, query, sort.toString(), time.toString(), null, true)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))));
        subscriptionSubreddits = observable
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoadingLinksSubreddit(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        linksSubreddit = listing;
                        eventHolder.callLinksSubreddit(new RxAdapterEvent<>(linksSubreddit.getChildren()));
                        for (Listener listener : listeners) {
                            listener.scrollToLinksSubreddit(0);
                        }
                    }

                    @Override
                    public void finish() {
                        setLoadingLinksSubreddit(false);
                    }
                });
        return observable;
    }

    public Subreddit getSubreddit(int position) {
        return (Subreddit) subreddits.getChildren()
                .get(position);
    }

    public int getCountSubreddit() {
        return subreddits.getChildren()
                .size();
    }

    public Subreddit getSubredditRecommended(int position) {
        return (Subreddit) subredditsRecommended.getChildren()
                .get(position);
    }

    public int getCountSubredditRecommended() {
        return subredditsRecommended.getChildren()
                .size();
    }

    public Link getLink(int position) {
        return (Link) links.getChildren().get(position - 1);
    }

    public int sizeLinks() {
        return links.getChildren().size();
    }

    public boolean isLoadingLinks() {
        return isLoadingLinks;
    }

    public Observable<Listing> loadMoreLinks() {
        if (isLoadingLinks()) {
            return Observable.empty();
        }

        if (subscriptionLinks != null && !subscriptionLinks.isUnsubscribed()) {
            subscriptionLinks.unsubscribe();
            subscriptionLinks = null;
        }

        String sortString = sort.toString();
        if (sort == Sort.ACTIVITY) {
            sortString = Sort.HOT.name();
        }

        Observable<Listing> observable = reddit.search("", query, sortString, time.toString(), links.getAfter(), false)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))));

        subscriptionLinks = observable.subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoadingLinks(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        int positionStart = links.getChildren()
                                .size() + 1;
                        int startSize = links.getChildren().size();
                        links.addChildren(listing.getChildren());
                        links.setAfter(listing.getAfter());
                        eventHolder.callLinks(new RxAdapterEvent<>(links.getChildren(), RxAdapterEvent.Type.INSERT, positionStart, links.getChildren().size() - startSize));
                    }

                    @Override
                    public void finish() {
                        setLoadingLinks(false);
                    }
                });

        return observable;
    }

    private void setLoadingLinks(boolean loading) {
        isLoadingLinks = loading;
    }

    public Link getLinkSubreddit(int position) {
        return (Link) linksSubreddit.getChildren().get(position - 1);
    }

    public int sizeLinksSubreddit() {
        return linksSubreddit.getChildren().size();
    }

    public boolean isLoadingLinksSubreddit() {
        return isLoadingLinksSubreddit;
    }

    public Observable<Listing> loadMoreLinksSubreddit() {
        if (isLoadingLinksSubreddit()) {
            return Observable.empty();
        }

        Subreddit subreddit = controllerLinks.getSubreddit();

        if (subscriptionLinksSubreddit != null && !subscriptionLinksSubreddit.isUnsubscribed()) {
            subscriptionLinksSubreddit.unsubscribe();
            subscriptionLinksSubreddit = null;
        }

        String pathSubreddit = subreddit.getUrl();
        if (pathSubreddit.length() < 2) {
            pathSubreddit = "";
        }

        Observable<Listing> observable = reddit.search(pathSubreddit, query, sort.toString(), time.toString(), linksSubreddit.getAfter(), true)
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))));

        subscriptionLinksSubreddit = observable.subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoadingLinksSubreddit(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        if (listing.getChildren().isEmpty() || listing.getChildren().get(0) instanceof Subreddit) {
                            return;
                        }
                        int startSize = linksSubreddit.getChildren().size();
                        int positionStart = startSize + 1;

                        linksSubreddit.addChildren(listing.getChildren());
                        linksSubreddit.setAfter(listing.getAfter());

                        eventHolder.callLinksSubreddit(new RxAdapterEvent<>(linksSubreddit.getChildren(), RxAdapterEvent.Type.INSERT, positionStart, linksSubreddit.getChildren().size() - startSize));
                    }

                    @Override
                    public void finish() {
                        setLoadingLinksSubreddit(false);
                    }
                });

        return observable;
    }

    private void setLoadingLinksSubreddit(boolean loading) {
        isLoadingLinksSubreddit = loading;
    }

    public Observable<Listing> setSort(Sort sort) {

        Listing listingSort = getCurrentPage() == PAGE_SUBREDDITS_RECOMMENDED ? subredditsRecommended : subreddits;

        if (Sort.ALPHABETICAL.equals(sort)) {

            if (Sort.ALPHABETICAL.equals(sortSubreddits)) {
                Collections.sort(listingSort.getChildren(), new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        return ((Subreddit) rhs).getDisplayName().compareToIgnoreCase(((Subreddit) lhs).getDisplayName());
                    }
                });
                sortSubreddits = null;
            }
            else {
                Collections.sort(listingSort.getChildren(), new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                    }
                });
                sortSubreddits = Sort.ALPHABETICAL;
            }
            if (listingSort == subredditsSubscribed) {
                saveSubscriptions();
            }
            else if (listingSort == subredditsRecommended) {
                for (Listener listener : listeners) {
                    listener.getAdapterSearchSubredditsRecommended()
                            .notifyDataSetChanged();
                }
            }
            else {
                for (Listener listener : listeners) {
                    listener.getAdapterSearchSubreddits()
                            .notifyDataSetChanged();
                }
            }
        }
        else if (Sort.SUBSCRIBERS.equals(sort)) {

            if (Sort.SUBSCRIBERS.equals(sortSubreddits)) {
                Collections.sort(listingSort.getChildren(), new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        long subscribersFirst = ((Subreddit) lhs).getSubscribers();
                        long subscribersSecond = ((Subreddit) rhs).getSubscribers();

                        return subscribersFirst < subscribersSecond ? -1 : (subscribersFirst == subscribersSecond ? 0 : 1);
                    }
                });
                sortSubreddits = null;
            }
            else {
                Collections.sort(listingSort.getChildren(), new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        long subscribersFirst = ((Subreddit) lhs).getSubscribers();
                        long subscribersSecond = ((Subreddit) rhs).getSubscribers();

                        return subscribersSecond < subscribersFirst ? -1 : (subscribersFirst == subscribersSecond ? 0 : 1);
                    }
                });
                sortSubreddits = Sort.SUBSCRIBERS;
            }
            if (listingSort == subredditsSubscribed) {
                saveSubscriptions();
            }
            else if (listingSort == subredditsRecommended) {
                for (Listener listener : listeners) {
                    listener.getAdapterSearchSubredditsRecommended()
                            .notifyDataSetChanged();
                }
            }
            else {
                for (Listener listener : listeners) {
                    listener.getAdapterSearchSubreddits()
                            .notifyDataSetChanged();
                }
            }
        }
        else if (this.sort != sort) {
            this.sort = sort;
            eventHolder.getSort().call(sort);
            return reloadCurrentPage();
        }
        return Observable.empty();
    }

    public Observable<Listing> setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            eventHolder.getTime().call(time);
            return reloadCurrentPage();
        }
        return Observable.empty();
    }

    public String getQuery() {
        return query;
    }

    public Observable<Listing> setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        eventHolder.getCurrentPage().call(currentPage);
        return reloadCurrentPage();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void addSubreddit(Subreddit subreddit) {
        // TODO: Implement global data store for subscriptions, to be updated dynamically
        List<Thing> additions = new ArrayList<>();
        additions.add(subreddit);
        subredditsSubscribed.addChildren(additions);
        saveSubscriptions();
    }

    public boolean setReplyTextLinks(String name, String text, boolean collapsed) {

        for (int index = 0; index < links.getChildren().size(); index++) {
            Thing thing = links.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                eventHolder.callLinks(new RxAdapterEvent<>(links.getChildren(), RxAdapterEvent.Type.CHANGE, index + 1));
                return true;
            }
        }

        return false;
    }

    public boolean setReplyTextLinksSubreddit(String name, String text, boolean collapsed) {

        for (int index = 0; index < linksSubreddit.getChildren().size(); index++) {
            Thing thing = linksSubreddit.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                eventHolder.callLinksSubreddit(new RxAdapterEvent<>(linksSubreddit.getChildren(), RxAdapterEvent.Type.CHANGE, index + 1));
                return true;
            }
        }

        return false;
    }

    public void setNsfwLinks(String name, boolean over18) {

        for (int index = 0; index < links.getChildren().size(); index++) {
            Thing thing = links.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);
                eventHolder.callLinks(new RxAdapterEvent<>(links.getChildren(), RxAdapterEvent.Type.CHANGE, index + 1));
                return;
            }
        }
    }


    public void setNsfwLinksSubreddit(String name, boolean over18) {

        for (int index = 0; index < linksSubreddit.getChildren().size(); index++) {
            Thing thing = linksSubreddit.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);
                eventHolder.callLinksSubreddit(new RxAdapterEvent<>(linksSubreddit.getChildren(), RxAdapterEvent.Type.CHANGE, index + 1));
                return;
            }
        }
    }

    public void moveSubreddit(int startAdapterPosition, int targetAdapterPosition) {
        if (startAdapterPosition < targetAdapterPosition) {
            for (int index = startAdapterPosition; index < targetAdapterPosition; index++) {
                Collections.swap(subredditsSubscribed.getChildren(), index, index + 1);
            }
        } else {
            for (int index = startAdapterPosition; index > targetAdapterPosition; index--) {
                Collections.swap(subredditsSubscribed.getChildren(), index, index - 1);
            }
        }

        for (Listener listener : listeners) {
            listener.getAdapterSearchSubreddits().notifyItemMoved(startAdapterPosition, targetAdapterPosition);
        }
    }

    public boolean isSubscriptionListShown() {
        return getCurrentPage() == PAGE_SUBREDDITS && subreddits == subredditsSubscribed;
    }

    public void addViewedSubreddit(Subreddit subreddit) {
        if (!controllerUser.hasUser()) {
            subredditsSubscribed.getChildren().add(subreddit);
            if (getCurrentPage() == PAGE_SUBREDDITS || getCurrentPage() == PAGE_SUBREDDITS_RECOMMENDED) {
                for (Listener listener : listeners) {
                    listener.getAdapterSearchSubreddits()
                            .notifyDataSetChanged();
                }
            }
        }
    }

    public int indexOfLink(Link link) {
        return links.getChildren().indexOf(link);
    }

    public int indexOfLinkSubreddit(Link link) {
        return linksSubreddit.getChildren().indexOf(link);
    }

    @Nullable
    public Link getPreviousLink(Link linkCurrent, int offset) {
        int index = indexOfLink(linkCurrent) - offset;
        if (index >= 0 && !links.getChildren().isEmpty()) {
            return (Link) links.getChildren().get(index);
        }

        return null;
    }

    @Nullable
    public Link getNextLink(Link linkCurrent, int offset) {
        int index = indexOfLink(linkCurrent) + offset;
        if (index < links.getChildren().size() && !links.getChildren().isEmpty()) {
            return (Link) links.getChildren().get(index);
        }

        return null;
    }

    @Nullable
    public Link getPreviousLinkSubreddit(Link linkCurrent, int offset) {
        int index = indexOfLinkSubreddit(linkCurrent) - offset;
        if (index >= 0 && !linksSubreddit.getChildren().isEmpty()) {
            return (Link) linksSubreddit.getChildren().get(index);
        }

        return null;
    }

    @Nullable
    public Link getNextLinkSubreddit(Link linkCurrent, int offset) {
        int index = indexOfLinkSubreddit(linkCurrent) + offset;
        if (index < linksSubreddit.getChildren().size() && !linksSubreddit.getChildren().isEmpty()) {
            return (Link) linksSubreddit.getChildren().get(index);
        }

        return null;
    }

    public interface Listener {
        AdapterSearchSubreddits getAdapterSearchSubreddits();
        AdapterSearchSubreddits getAdapterSearchSubredditsRecommended();
        void scrollToLinks(int position);
        void scrollToLinksSubreddit(int position);
    }

    public static class EventHolder {

        private BehaviorRelay<RxAdapterEvent<List<Thing>>> relayLinks = BehaviorRelay.create(new RxAdapterEvent<>(new ArrayList<>()));
        private BehaviorRelay<RxAdapterEvent<List<Thing>>> relayLinksSubreddit = BehaviorRelay.create(new RxAdapterEvent<>(new ArrayList<>()));
        private BehaviorRelay<Sort> relaySort = BehaviorRelay.create(Sort.RELEVANCE);
        private BehaviorRelay<Time> relayTime = BehaviorRelay.create(Time.ALL);
        private BehaviorRelay<Integer> relayCurrentPage = BehaviorRelay.create(0);

        public void callLinks(RxAdapterEvent<List<Thing>> event) {
            relayLinks.call(event);
        }

        public void callLinksSubreddit(RxAdapterEvent<List<Thing>> event) {
            relayLinksSubreddit.call(event);
        }

        public Observable<RxAdapterEvent<List<Thing>>> getLinks() {
            List<Thing> data = relayLinks.hasValue() ? relayLinks.getValue().getData() : new ArrayList<>();

            return Observable.just(new RxAdapterEvent<>(data))
                    .mergeWith(relayLinks.skip(1));
        }

        public Observable<RxAdapterEvent<List<Thing>>> getLinksSubreddit() {
            List<Thing> data = relayLinksSubreddit.hasValue() ? relayLinksSubreddit.getValue().getData() : new ArrayList<>();

            return Observable.just(new RxAdapterEvent<>(data))
                    .mergeWith(relayLinksSubreddit.skip(1));
        }

        public BehaviorRelay<Sort> getSort() {
            return relaySort;
        }

        public BehaviorRelay<Time> getTime() {
            return relayTime;
        }

        public BehaviorRelay<Integer> getCurrentPage() {
            return relayCurrentPage;
        }
    }

}
