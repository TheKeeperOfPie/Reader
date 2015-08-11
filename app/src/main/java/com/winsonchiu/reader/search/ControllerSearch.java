/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Set;

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
    private Listing subredditsSubscribed;
    private Listing subreddits;
    private Listing subredditsRecommended;
    private Listing links;
    private Listing linksSubreddit;
    private String query;
    private Sort sort;
    private Time time;
    private volatile int currentPage;
    private Request<String> requestSubreddits;
    private Request<String> requestLinks;
    private Request<String> requestLinksSubreddit;
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
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapterSearchSubreddits().notifyDataSetChanged();
        listener.setSort(sort);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setQuery(String query) {
        this.query = query;
        setTitle();
        if ((TextUtils.isEmpty(query) || query.length() < 2) && currentPage == PAGE_SUBREDDITS) {
            if (requestSubreddits != null) {
                requestSubreddits.cancel();
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
        String url;

        if (controllerUser.hasUser()) {
            url = Reddit.OAUTH_URL + "/subreddits/mine/subscriber?show=all&limit=100";
        }
        else {
            url = Reddit.OAUTH_URL + "/subreddits/default?show=all&limit=100";
        }

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "reloadSubscriptionList response: " + response);
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                            subredditsSubscribed = listing;
                            Collections.sort(subredditsSubscribed.getChildren(), new Comparator<Thing>() {
                                @Override
                                public int compare(Thing lhs, Thing rhs) {
                                    return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                                }
                            });
                            if (TextUtils.isEmpty(query)) {
                                subreddits = subredditsSubscribed;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits()
                                            .notifyDataSetChanged();
                                }
                            }
                            if (listing.getChildren().size() == 100) {
                                loadMoreSubscriptions();
                            }
                            else {
                                loadContributorSubreddits();
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
                }, 0);
    }

    private void loadMoreSubscriptions() {
        String url;

        if (TextUtils.isEmpty(controllerUser.getUser().getName())) {
            url = Reddit.OAUTH_URL + "/subreddits/default?show=all&limit=100&after=" + subredditsSubscribed.getAfter();
        }
        else {
            url = Reddit.OAUTH_URL + "/subreddits/mine/subscriber?show=all&limit=100&after=" + subredditsSubscribed.getAfter();
        }

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                            subredditsSubscribed.addChildren(listing.getChildren());
                            Collections.sort(subredditsSubscribed.getChildren(), new Comparator<Thing>() {
                                @Override
                                public int compare(Thing lhs, Thing rhs) {
                                    return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                                }
                            });
                            if (TextUtils.isEmpty(query)) {
                                subreddits = subredditsSubscribed;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits()
                                            .notifyDataSetChanged();
                                }
                            }
                            if (listing.getChildren().size() == 100) {
                                loadMoreSubscriptions();
                            }
                            else {
                                loadContributorSubreddits();
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
                }, 0);
    }

    private void loadContributorSubreddits() {

        if (TextUtils.isEmpty(controllerUser.getUser().getName())) {
            return;
        }

        reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/mine/contributor?show=all&limit=100&after=" + subredditsSubscribed.getAfter(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                            subredditsSubscribed.addChildren(listing.getChildren());
                            Collections.sort(subredditsSubscribed.getChildren(), new Comparator<Thing>() {
                                @Override
                                public int compare(Thing lhs, Thing rhs) {
                                    return ((Subreddit) lhs).getDisplayName().compareToIgnoreCase(
                                            ((Subreddit) rhs).getDisplayName());
                                }
                            });
                            if (TextUtils.isEmpty(query)) {
                                subreddits = subredditsSubscribed;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits()
                                            .notifyDataSetChanged();
                                }
                            }
                            if (listing.getChildren().size() == 100) {
                                loadMoreContributorSubreddits();
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
                }, 0);
    }

    private void loadMoreContributorSubreddits() {

        reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/mine/contributor?show=all&limit=100",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                            subredditsSubscribed.addChildren(listing.getChildren());
                            Collections.sort(subredditsSubscribed.getChildren(),
                                    new Comparator<Thing>() {
                                        @Override
                                        public int compare(Thing lhs, Thing rhs) {
                                            return ((Subreddit) lhs).getDisplayName()
                                                    .compareToIgnoreCase(
                                                            ((Subreddit) rhs).getDisplayName());
                                        }
                                    });
                            if (TextUtils.isEmpty(query)) {
                                subreddits = subredditsSubscribed;
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits()
                                            .notifyDataSetChanged();
                                }
                            }
                            if (listing.getChildren().size() == 100) {
                                loadMoreSubscriptions();
                            }
                            else {
                                loadContributorSubreddits();
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
                }, 0);
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

        if (requestSubreddits != null) {
            requestSubreddits.cancel();
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
            requestSubreddits = reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/search?show=all&q=" + URLEncoder.encode(query, Reddit.UTF_8).replaceAll("\\s", "") + "&sort=" + sort.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(final String response) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    try {
                                        Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
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
                                            listener.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    listener.getAdapterSearchSubreddits().notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }).start();

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void reloadSubredditsRecommended() {
        Log.d(TAG, "reloadSubredditsRecommended");

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
        setLoadingLinks(true);

        if (requestLinks != null) {
            requestLinks.cancel();
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }
            requestLinks = reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + URLEncoder.encode(query,
                            Reddit.UTF_8) + "&sort=" + sortString + "&t=" + time.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Response: " + response);
                            try {
                                links = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinks().notifyDataSetChanged();
                                    listener.scrollToLinks(0);
                                }
                                setLoadingLinks(false);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                                setLoadingLinks(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinks(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            setLoadingLinks(false);
        }
    }

    public void reloadLinksSubreddit() {
        setLoadingLinksSubreddit(true);

        Subreddit subreddit = controllerLinks.getSubreddit();
        String url = Reddit.OAUTH_URL;
        if (TextUtils.isEmpty(subreddit.getUrl())) {
            url += "/";
        }
        else {
            url += subreddit.getUrl();
        }

        if (requestLinksSubreddit != null) {
            requestLinksSubreddit.cancel();
        }

        try {
            requestLinksSubreddit = reddit.loadGet(url + "search?restrict_sr=on&q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sort.toString() + "&t=" + time.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                linksSubreddit = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinksSubreddit().notifyDataSetChanged();
                                    listener.scrollToLinksSubreddit(0);
                                }
                                setLoadingLinksSubreddit(false);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                                setLoadingLinksSubreddit(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinksSubreddit(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
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
        setLoadingLinks(true);

        if (requestLinks != null) {
            requestLinks.cancel();
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }
            requestLinks = reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sortString + "&t=" + time.toString() + "&after=" + links.getAfter(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {

                                int positionStart = links.getChildren()
                                        .size() + 1;
                                int startSize = links.getChildren().size();
                                Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                                links.addChildren(listing.getChildren());
                                links.setAfter(listing.getAfter());
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinks().notifyItemRangeInserted(positionStart, links.getChildren().size() - startSize);
                                }
                                setLoadingLinks(false);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                                setLoadingLinks(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinks(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
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
        String url = Reddit.OAUTH_URL;
        if (TextUtils.isEmpty(subreddit.getUrl())) {
            url += "/";
        }
        else {
            url += subreddit.getUrl();
        }

        if (requestLinksSubreddit != null) {
            requestLinksSubreddit.cancel();
        }

        try {
            requestLinksSubreddit = reddit.loadGet(url + "search?restrict_sr=on&q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sort.toString() + "&t=" + time.toString() + "&after=" + linksSubreddit.getAfter(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                                if (listing.getChildren().isEmpty() || listing.getChildren().get(0) instanceof Subreddit) {
                                    setLoadingLinksSubreddit(false);
                                    return;
                                }
                                int startSize = linksSubreddit.getChildren().size();
                                int positionStart = startSize + 1;

                                linksSubreddit.addChildren(listing.getChildren());
                                linksSubreddit.setAfter(listing.getAfter());
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinksSubreddit().notifyItemRangeInserted(positionStart, linksSubreddit.getChildren().size() - startSize);
                                }
                                setLoadingLinksSubreddit(false);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                                setLoadingLinksSubreddit(false);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            setLoadingLinksSubreddit(false);
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, 0);
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
        if (this.sort != sort) {
            this.sort = sort;
            for (Listener listener : listeners) {
                listener.setSort(sort);
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

    public void clearResults() {
        if (subreddits != subredditsSubscribed) {
            subreddits.getChildren()
                    .clear();
            subreddits = subredditsSubscribed;
        }
        links.getChildren().clear();
        linksSubreddit.getChildren().clear();
        query = "";
        sort = Sort.RELEVANCE;
        for (Listener listener : listeners) {
            listener.setSort(sort);
            listener.getAdapterSearchSubreddits().notifyDataSetChanged();
            listener.getAdapterLinks().notifyDataSetChanged();
            listener.getAdapterLinksSubreddit().notifyDataSetChanged();
        }
    }

    public void setControllers(ControllerLinks controllerLinks, ControllerUser controllerUser) {
        this.controllerLinks = controllerLinks;
        this.controllerUser = controllerUser;
    }

    public void addSubreddit(Subreddit subreddit) {
        // TODO: Implement global data store for subscriptions, to be updated dynamically
        subredditsSubscribed.getChildren().add(subreddit);
        Collections.sort(subredditsSubscribed.getChildren(),
                new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        return ((Subreddit) lhs).getDisplayName()
                                .compareToIgnoreCase(
                                        ((Subreddit) rhs).getDisplayName());
                    }
                });
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

    public interface Listener extends ControllerListener {
        AdapterSearchSubreddits getAdapterSearchSubreddits();
        AdapterSearchSubreddits getAdapterSearchSubredditsRecommended();
        AdapterLink getAdapterLinks();
        AdapterLink getAdapterLinksSubreddit();
        void setToolbarTitle(CharSequence title);
        void setSort(Sort sort);
        void scrollToLinks(int position);
        void scrollToLinksSubreddit(int position);
    }

}
