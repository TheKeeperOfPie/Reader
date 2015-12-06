/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.R;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks implements ControllerLinksBase {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    public static final int LIMIT = 25;

    private Activity activity;
    private Listing listingLinks;
    private Subreddit subreddit;
    private boolean isLoading;
    private Sort sort;
    private Time time;
    private Reddit reddit;
    private Set<Listener> listeners;

    public ControllerLinks(Activity activity, String subredditName, Sort sort) {
        setActivity(activity);
        this.listeners = new HashSet<>();
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
            Log.d(TAG, "addListener reloaded");
        }
        Log.d(TAG, "addListener: " + listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setParameters(String subredditName, Sort sort, Time time) {
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
            reloadSubreddit();
        }
    }

    public void reloadSubreddit() {
        setLoading(true);

        reddit.about(subreddit.getUrl())
                .flatMap(new Func1<String, Observable<Subreddit>>() {
                    @Override
                    public Observable<Subreddit> call(String response) {
                        try {
                            return Observable.just(Subreddit.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class)));
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribe(new Subscriber<Subreddit>() {
                    @Override
                    public void onStart() {
                        setLoading(true);
                    }

                    @Override
                    public void onCompleted() {
                        reloadAllLinks(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Subreddit subreddit) {
                        ControllerLinks.this.subreddit = subreddit;
                    }
                });
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

    public void reloadAllLinks(final boolean scroll) {
        setLoading(true);

        reddit.links(subreddit.getUrl(), sort.toString(), time.toString(), LIMIT, null)
                .flatMap(new Func1<String, Observable<Listing>>() {
                    @Override
                    public Observable<Listing> call(String s) {
                        try {
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    s, JsonNode.class));
                            if (!listing.getChildren()
                                    .isEmpty() && listing.getChildren()
                                    .get(0) instanceof Link) {
                                return Observable.just(listing);
                            }

                            return Observable.empty();
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {
                        setLoading(false);

                        for (final Listener listener : listeners) {
                            listener.getAdapter().notifyDataSetChanged();
                            listener.loadSideBar(subreddit);
                            listener.showEmptyView(listingLinks.getChildren()
                                    .isEmpty());
                            if (scroll) {
                                listener.scrollTo(0);
                            }
                        }
                        setTitle();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                .show();
                        for (final Listener listener : listeners) {
                            listener.showEmptyView(listingLinks.getChildren()
                                    .isEmpty());
                        }
                    }

                    @Override
                    public void onNext(final Listing next) {
                        listingLinks = next;
                    }
                });

        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }

        if (TextUtils.isEmpty(listingLinks.getAfter())) {
            return;
        }

        setLoading(true);
        String url = Reddit.OAUTH_URL + subreddit.getUrl() + sort.toString() + "?t=" + time.toString() + "&limit=25&showAll=true&after=" + listingLinks.getAfter();

        reddit.links(subreddit.getUrl(), sort.toString(), time.toString(), LIMIT, listingLinks.getAfter())
                .flatMap(Listing.FLAT_MAP)
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {
                        setLoading(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(activity, activity.getString(R.string.error_loading_links),
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onNext(Listing listing) {
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
    }

    @Override
    public Subreddit getSubreddit() {
        return subreddit;
    }

    public boolean deletePost(Link link) {
        int index = listingLinks.getChildren()
                .indexOf(link);

        if (index < 0) {
            return false;
        }

        listingLinks.getChildren()
                .remove(index);
        for (Listener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRemoved(index + 1);
        }

        reddit.delete(link, null, null);

        return true;
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

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
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
        String action = subreddit.isUserIsSubscriber() ? "unsub" : "sub";
        subreddit.setUserIsSubscriber(!subreddit.isUserIsSubscriber());

        Map<String, String> params = new HashMap<>();
        params.put("action", action);
        params.put("sr", subreddit.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/subscribe",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "subscribe response: " + response);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, params, 0);
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
    }

}
