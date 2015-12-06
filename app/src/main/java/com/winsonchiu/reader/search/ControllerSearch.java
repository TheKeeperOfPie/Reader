/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.utils.ControllerListener;

import org.json.JSONArray;
import org.json.JSONException;
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

import rx.Observer;
import rx.Subscriber;
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
    private Set<Listener> listeners;
    private Activity activity;
    private SharedPreferences preferences;
    private Reddit reddit;
    private AccountManager accountManager;
    private Listing subredditsLoaded;
    private Listing subredditsSubscribed;
    private Listing subreddits;
    private Listing subredditsRecommended;
    private Listing links;
    private Listing linksSubreddit;
    private String query;
    private Sort sort;
    private Sort sortSubreddits;
    private Time time;
    private volatile int currentPage;
    private Subscription subscriptionSubreddits;
    private Subscription subscriptionLinks;
    private Subscription subscriptionLinksSubreddit;
    private boolean isLoadingLinks;
    private boolean isLoadingLinksSubreddit;

    private List<Request> requestsSubredditsRecommended;
    private String currentSubreddit;

    public ControllerSearch(Activity activity) {
        setActivity(activity);
        sort = Sort.RELEVANCE;
        time = Time.ALL;
        listeners = new HashSet<>();
        query = "";
        subredditsLoaded = new Listing();
        subredditsSubscribed = new Listing();
        subreddits = new Listing();
        subredditsRecommended = new Listing();
        links = new Listing();
        linksSubreddit = new Listing();
        requestsSubredditsRecommended = new ArrayList<>();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        this.accountManager = AccountManager.get(activity.getApplicationContext());
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapterSearchSubreddits().notifyDataSetChanged();
        listener.setSortAndTime(sort, time);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setQuery(String query) {
        this.query = query;
        setTitle();
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
            reloadCurrentPage();
        }
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle(query);
        }
    }

    public void reloadSubscriptionList() {

        Listing listing = new Listing();
        String subscriptionsJson = preferences.getString(AppSettings.SUBSCRIPTIONS + controllerUser.getUser().getName(), "");

        Log.d(TAG, "subscriptionsJson: " + subscriptionsJson);

        if (!TextUtils.isEmpty(subscriptionsJson)) {

            try {
                JsonNode jsonNode = Reddit.getObjectMapper().readValue(subscriptionsJson, JsonNode.class);

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

        String page;

        if (controllerUser.hasUser()) {
            page = "mine/subscriber";
        }
        else {
            page = "default";
        }

        reddit.subreddits(page, null, 100)
                .flatMap(Listing.FLAT_MAP)
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
        String page;

        if (TextUtils.isEmpty(controllerUser.getUser().getName())) {
            page = "default";
        }
        else {
            page = "mine/subscriber";
        }

        reddit.subreddits(page, subredditsLoaded.getAfter(), 100)
                .flatMap(Listing.FLAT_MAP)
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
            data = Reddit.getObjectMapper().writeValueAsString(subredditsSubscribed.getChildren());

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

        reddit.subreddits("mine/contributor", null, 100)
                .flatMap(Listing.FLAT_MAP)
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
        reddit.subreddits("mine/contributor", subredditsLoaded.getAfter(), 100)
                .flatMap(Listing.FLAT_MAP)
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

    public void reloadCurrentPage() {
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
                    reloadLinks();
                }
                break;
            case PAGE_LINKS_SUBREDDIT:
                if (!TextUtils.isEmpty(query)) {
                    reloadLinksSubreddit();
                }
                break;
            case PAGE_SUBREDDITS_RECOMMENDED:
                if (!controllerLinks.getSubreddit().getDisplayName().equals(currentSubreddit)) {
                    reloadSubredditsRecommended();
                }
                break;
        }
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
                    .flatMap(Listing.FLAT_MAP)
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

        for (Request request : requestsSubredditsRecommended) {
            request.cancel();
        }
        requestsSubredditsRecommended.clear();

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

        reddit.loadGet(Reddit.OAUTH_URL + "/api/recommend/sr/" + currentSubreddit + "?omit=" + builderOmit, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    final JSONArray jsonArray = new JSONArray(response);

                    for (int index = 0; index < jsonArray.length(); index++) {

                        /*
                            No idea why the API returns a {"sr_name": "subreddit"} rather than an
                            array of Strings, but we'll convert it.
                         */
                        JSONObject dataSubreddit = jsonArray.getJSONObject(index);
                        String name = dataSubreddit.optString("sr_name");

                        requestsSubredditsRecommended.add(reddit.loadGet(
                                Reddit.OAUTH_URL + "/r/" + name + "/about",
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Subreddit subreddit = Subreddit
                                                    .fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                                            subredditsRecommended.getChildren().add(subreddit);

                                            if (subredditsRecommended.getChildren()
                                                    .size() == jsonArray.length()) {
                                                Collections
                                                        .sort(subredditsRecommended.getChildren(),
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
                                        }
                                        catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                }, 0));
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, 0);
    }

    public void reloadLinks() {
        if (subscriptionLinks != null && !subscriptionLinks.isUnsubscribed()) {
            subscriptionLinks.unsubscribe();
            subscriptionLinks = null;
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }

            subscriptionLinks = reddit.search("", URLEncoder.encode(query, Reddit.UTF_8), sortString, time.toString(), null, false)
                    .flatMap(Listing.FLAT_MAP)
                    .subscribe(new Subscriber<Listing>() {
                        @Override
                        public void onStart() {
                            setLoadingLinks(true);
                        }

                        @Override
                        public void onCompleted() {
                            setLoadingLinks(false);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onNext(Listing listing) {
                            links = listing;
                            for (Listener listener : listeners) {
                                listener.getAdapterLinks().notifyDataSetChanged();
                                listener.scrollToLinks(0);
                            }
                            setLoadingLinks(false);
                        }
                    });
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void reloadLinksSubreddit() {
        if (subscriptionLinksSubreddit != null && !subscriptionSubreddits.isUnsubscribed()) {
            subscriptionLinksSubreddit.unsubscribe();
            subscriptionLinksSubreddit = null;
        }

        Subreddit subreddit = controllerLinks.getSubreddit();

        String pathSubreddit = subreddit.getUrl();
        if (pathSubreddit.length() < 2) {
            pathSubreddit = "";
        }

        try {
            subscriptionSubreddits = reddit.search(pathSubreddit, URLEncoder.encode(query, Reddit.UTF_8), sort.toString(), time.toString(), null, true)
                    .flatMap(Listing.FLAT_MAP)
                    .subscribe(new Subscriber<Listing>() {
                        @Override
                        public void onStart() {
                            setLoadingLinksSubreddit(true);
                        }

                        @Override
                        public void onCompleted() {
                            setLoadingLinksSubreddit(false);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onNext(Listing listing) {
                            linksSubreddit = listing;
                            for (Listener listener : listeners) {
                                listener.getAdapterLinksSubreddit().notifyDataSetChanged();
                                listener.scrollToLinksSubreddit(0);
                            }
                            setLoadingLinksSubreddit(false);
                        }
                    });
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinksSubreddit(false);
        }
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

    public void loadMoreLinks() {
        if (isLoadingLinks()) {
            return;
        }

        if (subscriptionLinks != null && !subscriptionLinks.isUnsubscribed()) {
            subscriptionLinks.unsubscribe();
            subscriptionLinks = null;
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }

            subscriptionLinks = reddit.search("", URLEncoder.encode(query, Reddit.UTF_8), sortString, time.toString(), links.getAfter(), false)
                    .flatMap(Listing.FLAT_MAP)
                    .subscribe(new Subscriber<Listing>() {
                        @Override
                        public void onStart() {
                            setLoadingLinks(true);
                        }

                        @Override
                        public void onCompleted() {
                            setLoadingLinks(false);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onNext(Listing listing) {
                            int positionStart = links.getChildren()
                                    .size() + 1;
                            int startSize = links.getChildren().size();
                            links.addChildren(listing.getChildren());
                            links.setAfter(listing.getAfter());
                            for (Listener listener : listeners) {
                                listener.getAdapterLinks().notifyItemRangeInserted(positionStart, links.getChildren().size() - startSize);
                            }
                        }
                    });
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinks(false);
        }
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

    public void loadMoreLinksSubreddit() {
        if (isLoadingLinksSubreddit()) {
            return;
        }
        setLoadingLinksSubreddit(true);

        Subreddit subreddit = controllerLinks.getSubreddit();

        if (subscriptionLinksSubreddit != null && !subscriptionSubreddits.isUnsubscribed()) {
            subscriptionLinksSubreddit.unsubscribe();
            subscriptionLinksSubreddit = null;
        }

        String pathSubreddit = subreddit.getUrl();
        if (pathSubreddit.length() < 2) {
            pathSubreddit = "";
        }

        try {
            subscriptionLinksSubreddit = reddit.search(pathSubreddit, URLEncoder.encode(query, Reddit.UTF_8), sort.toString(), time.toString(), linksSubreddit.getAfter(), true)
                    .flatMap(Listing.FLAT_MAP)
                    .subscribe(new Subscriber<Listing>() {
                        @Override
                        public void onStart() {
                            setLoadingLinksSubreddit(true);
                        }

                        @Override
                        public void onCompleted() {
                            setLoadingLinksSubreddit(false);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onNext(Listing listing) {
                            if (listing.getChildren().isEmpty() || listing.getChildren().get(0) instanceof Subreddit) {
                                return;
                            }
                            int startSize = linksSubreddit.getChildren().size();
                            int positionStart = startSize + 1;

                            linksSubreddit.addChildren(listing.getChildren());
                            linksSubreddit.setAfter(listing.getAfter());
                            for (Listener listener : listeners) {
                                listener.getAdapterLinksSubreddit().notifyItemRangeInserted(positionStart, linksSubreddit.getChildren().size() - startSize);
                            }
                        }
                    });
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinksSubreddit(false);
        }
    }

    private void setLoadingLinksSubreddit(boolean loading) {
        isLoadingLinksSubreddit = loading;
    }

    public void setSort(Sort sort) {

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
            for (Listener listener : listeners) {
                listener.setSortAndTime(sort, time);
            }
            reloadCurrentPage();
        }
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            reloadCurrentPage();
        }
    }

    public String getQuery() {
        return query;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        reloadCurrentPage();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setControllers(ControllerLinks controllerLinks, ControllerUser controllerUser) {
        this.controllerLinks = controllerLinks;
        this.controllerUser = controllerUser;
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
                for (Listener listener : listeners) {
                    listener.getAdapterLinks().notifyItemChanged(index + 1);
                }
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
                for (Listener listener : listeners) {
                    listener.getAdapterLinks().notifyItemChanged(index + 1);
                }
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
                for (Listener listener : listeners) {
                    listener.getAdapterLinks().notifyItemChanged(index + 1);
                }
                return;
            }
        }
    }


    public void setNsfwLinksSubreddit(String name, boolean over18) {

        for (int index = 0; index < linksSubreddit.getChildren().size(); index++) {
            Thing thing = linksSubreddit.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);
                for (Listener listener : listeners) {
                    listener.getAdapterLinks().notifyItemChanged(index + 1);
                }
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

    public interface Listener extends ControllerListener {
        AdapterSearchSubreddits getAdapterSearchSubreddits();
        AdapterSearchSubreddits getAdapterSearchSubredditsRecommended();
        AdapterLink getAdapterLinks();
        AdapterLink getAdapterLinksSubreddit();
        void setToolbarTitle(CharSequence title);
        void setSortAndTime(Sort sort, Time time);
        void scrollToLinks(int position);
        void scrollToLinksSubreddit(int position);
    }

}
