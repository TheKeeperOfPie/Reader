/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.utils.ControllerListener;
import com.winsonchiu.reader.utils.FinalizingSubscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks implements ControllerLinksBase {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    public static final int LIMIT = 25;

    private Listing listingLinks;
    private Subreddit subreddit;
    private boolean isLoading;
    private Sort sort;
    private Time time;
    private Set<Listener> listeners = new HashSet<>();

    @Inject Reddit reddit;

    public ControllerLinks(String subredditName, Sort sort) {
        CustomApplication.getComponentMain().inject(this);
        listingLinks = new Listing();
        this.sort = sort;
        this.time = Time.ALL;
        subreddit = new Subreddit();
        subreddit.setDisplayName(subredditName);
        if (TextUtils.isEmpty(subredditName)) {
            subreddit.setUrl("/");
        }
        else {
            subreddit.setUrl("/r/" + subredditName);
        }

    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.setSortAndTime(sort, time);
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
        listener.loadSideBar(subreddit);
        if (!isLoading() && listingLinks.getChildren().isEmpty()) {
            reloadSubreddit();
        }
        Log.d(TAG, "addListener: " + listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Observable<Subreddit> setParameters(String subredditName, Sort sort, Time time) {
        Log.d(TAG, "parseUrl setParameters() called with: " + "subredditName = [" + subredditName + "], sort = [" + sort + "], time = [" + time + "]");

        if (!TextUtils.equals(subredditName, subreddit.getDisplayName())) {
            this.sort = sort;
            this.time = time;
            subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddit.setUrl("/r/" + subredditName + "/");
            int size = sizeLinks();
            listingLinks = new Listing();
            for (Listener listener : listeners) {
                listener.setSortAndTime(sort, time);
                listener.getAdapter()
                        .notifyItemRangeRemoved(0, size + 1);
            }
            return reloadSubreddit();
        }

        return Observable.empty();
    }

    public Observable<Subreddit> reloadSubreddit() {
        Log.d(TAG, "reloadSubreddit() called with: ", new Exception());
        Observable<Subreddit> observable = reddit.about(subreddit.getUrl())
                .flatMap(new Func1<String, Observable<Subreddit>>() {
                    @Override
                    public Observable<Subreddit> call(String response) {
                        try {
                            return Observable.just(Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(
                                    response, JsonNode.class)));
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                });
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
                        for (Listener listener : listeners) {
                            listener.showEmptyView(TextUtils.isEmpty(subreddit.getUrl()));
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                        reloadAllLinks(true);
                    }
                });
        return observable;
    }

    public Observable<Subreddit> reloadSubredditOnly() {
        Log.d(TAG, "reloadSubredditOnly() called with: ", new Exception());
        Observable<Subreddit> observable = reddit.about(subreddit.getUrl())
                .flatMap(new Func1<String, Observable<Subreddit>>() {
                    @Override
                    public Observable<Subreddit> call(String response) {
                        try {
                            Subreddit subreddit = Subreddit.fromJson(ComponentStatic.getObjectMapper().readValue(
                                    response, JsonNode.class));

                            if (!TextUtils.isEmpty(subreddit.getUrl())) {
                                return Observable.just(subreddit);
                            }
                            else {
                                return Observable.error(new Exception());
                            }
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                });
        observable.subscribe(new FinalizingSubscriber<Subreddit>() {
            @Override
            public void start() {
                setLoading(true);
            }

            @Override
            public void error(Throwable e) {
                e.printStackTrace();
                for (Listener listener : listeners) {
                    listener.showEmptyView(true);
                }
            }

            @Override
            public void next(Subreddit next) {
                ControllerLinks.this.subreddit = next;
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
            for (Listener listener : listeners) {
                listener.setRefreshing(true);
            }
            this.sort = sort;
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            reloadAllLinks(true);
        }
        Log.d(TAG, "loadFrontPage");
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            for (Listener listener : listeners) {
                listener.setSortAndTime(sort, time);
            }
            reloadAllLinks(true);
        }
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            for (Listener listener : listeners) {
                listener.setSortAndTime(sort, time);
            }
            reloadAllLinks(true);
        }
    }

    public void setTitle() {
        final String subredditName;
        if (!TextUtils.isEmpty(subreddit.getDisplayName())) {
            subredditName = "/r/" + subreddit.getDisplayName();
        }
        else {
            subredditName = Reddit.FRONT_PAGE;
        }

        for (final Listener listener : listeners) {
            listener.post(new Runnable() {
                @Override
                public void run() {
                    listener.setToolbarTitle(subredditName);
                }
            });
        }
    }

    public Link getLink(int position) {
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
                .flatMap(Listing.FLAT_MAP)
                .flatMap(new Func1<Listing, Observable<Listing>>() {
                    @Override
                    public Observable<Listing> call(Listing listing) {
                        if (!listing.getChildren().isEmpty() && !(listing.getChildren().get(0) instanceof Link)) {
                            return Observable.error(new Exception());
                        }

                        return Observable.just(listing);
                    }
                });

        observable.subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing next) {
                        listingLinks = next;
                    }

                    @Override
                    public void finish() {
                        setLoading(false);

                        for (final Listener listener : listeners) {
                            listener.getAdapter().notifyDataSetChanged();
                            listener.loadSideBar(subreddit);
                            listener.showEmptyView(listingLinks.getChildren()
                                    .isEmpty() && TextUtils.isEmpty(subreddit.getDisplayName()));
                            if (scroll) {
                                listener.scrollTo(0);
                            }
                        }
                        setTitle();
                    }
                });

        Log.d(TAG, "reloadAllLinks");

        return observable;
    }

    public Observable<Listing> loadMoreLinks() {
        if (isLoading) {
            return Observable.empty();
        }

        if (TextUtils.isEmpty(listingLinks.getAfter())) {
            return Observable.empty();
        }

        setLoading(true);

        Observable<Listing> observable = reddit.links(subreddit.getUrl(), sort.toString(), time.toString(), LIMIT, listingLinks.getAfter())
                .flatMap(Listing.FLAT_MAP);

        observable.subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void completed() {
                        setLoading(false);
                    }

                    @Override
                    public void next(Listing listing) {
                        final int positionStart = listingLinks.getChildren()
                                .size();
                        listingLinks.addChildren(listing.getChildren());
                        listingLinks.setAfter(listing.getAfter());
                        for (final Listener listener : listeners) {
                            listener.getAdapter()
                                    .notifyItemRangeInserted(positionStart + 1,
                                            listingLinks.getChildren()
                                                    .size() - positionStart);
                        }
                    }
                });

        return observable;
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
        for (Listener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRemoved(index + 1);
        }


        return reddit.delete(link);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        for (final Listener listener : listeners) {
            listener.post(new Runnable() {
                @Override
                public void run() {
                    listener.setRefreshing(isLoading);
                }
            });
        }
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
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 1);
                }
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
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 1);
                }
                return;
            }
        }
    }

    public Link remove(int position) {
        Link link = (Link) listingLinks.getChildren().remove(position);
        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(position + 1);
        }
        return link;
    }

    public void subscribe() {
        final boolean subscribed = subreddit.isUserIsSubscriber();
        subreddit.setUserIsSubscriber(!subreddit.isUserIsSubscriber());
        reddit.subscribe(subreddit.isUserIsSubscriber(), subreddit.getName())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        for (Listener listener : listeners) {
                            listener.setSubscribed(subscribed);
                        }
                    }

                    @Override
                    public void onNext(String response) {
                        Log.d(TAG, "subscribe onNext: " + response);
                    }
                });
    }

    public void add(int position, Link link) {
        listingLinks.getChildren().add(position, link);
        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemInserted(position + 1);
        }
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
            for (Listener listener : listeners) {
                // Offset 1 for subreddit header
                listener.getAdapter().notifyItemRemoved(index + 1);
            }
        }

    }

    public boolean isOnSpecificSubreddit() {
        return !TextUtils.isEmpty(subreddit.getDisplayName()) && !"/r/all/".equalsIgnoreCase(
                subreddit.getUrl()) && !subreddit.getUrl().contains("+");
    }

    public interface Listener extends ControllerListener {
        void setSortAndTime(Sort sort, Time time);
        void showEmptyView(boolean isEmpty);
        void loadSideBar(Subreddit subreddit);
        void scrollTo(int position);
        void setSubscribed(boolean subscribed);
    }

}
