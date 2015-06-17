package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class ControllerSearch {

    public static final int PAGE_SUBREDDITS = 0;
    public static final int PAGE_LINKS_SUBREDDIT = 1;
    public static final int PAGE_LINKS = 2;

    private static final String TAG = ControllerSearch.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private Set<Listener> listeners;
    private Activity activity;
    private SharedPreferences preferences;
    private Reddit reddit;
    private Listing subreddits;
    private Listing links;
    private Listing linksSubreddit;
    private String query;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Sort sort;
    private Time time;
    private int currentPage;
    private Request<String> requestSubreddits;
    private Request<String> requestLinks;
    private Request<String> requestLinksSubreddit;

    public ControllerSearch(Activity activity) {
        setActivity(activity);
        sort = Sort.RELEVANCE;
        time = Time.ALL;
        listeners = new HashSet<>();
        query = "";
        subreddits = new Listing();
        links = new Listing();
        linksSubreddit = new Listing();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapterSearchSubreddits().notifyDataSetChanged();
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            return;
        }
        this.query = query;
        setTitle();
        reloadCurrentPage();
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle(query);
        }
    }

    public void reloadCurrentPage() {
        Log.d(TAG, "reloadCurrentPage");
        switch (currentPage) {
            case PAGE_SUBREDDITS:
                reloadSubreddits();
                break;
            case PAGE_LINKS:
                reloadLinks();
                break;
            case PAGE_LINKS_SUBREDDIT:
                reloadLinksSubreddit();
                break;
        }
    }

    public void reloadSubreddits() {

        if (requestSubreddits != null) {
            requestSubreddits.cancel();
        }

        try {
            requestSubreddits = reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/search?show=all&q=" + URLEncoder.encode(query, Reddit.UTF_8).replaceAll("\\s", "") + "&sort=" + sort.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            subreddits = new Listing();
                            try {
                                subreddits = Listing.fromJson(new JSONObject(response));
                                for (Listener listener : listeners) {
                                    listener.getAdapterSearchSubreddits().notifyDataSetChanged();
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
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void reloadLinks() {

        if (requestLinks != null) {
            requestLinks.cancel();
        }

        try {
            String sortString = sort.toString();
            if (sort == Sort.ACTIVITY) {
                sortString = Sort.HOT.name();
            }
            requestLinks = reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + URLEncoder.encode(query, Reddit.UTF_8) + "&sort=" + sortString + "&t=" + time.toString(),
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            links = new Listing();

                            Log.d(TAG, "Response: " + response);
                            try {
                                links = Listing.fromJson(new JSONObject(response));
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinks().notifyDataSetChanged();
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
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void reloadLinksSubreddit() {

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
                            linksSubreddit = new Listing();

                            Log.d(TAG, "Response: " + response);
                            try {
                                linksSubreddit = Listing.fromJson(new JSONObject(response));
                                for (Listener listener : listeners) {
                                    listener.getAdapterLinksSubreddit().notifyDataSetChanged();
                                }
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "Error: " + error);
                        }
                    }, 0);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public Subreddit getSubreddit(int position) {
        return (Subreddit) subreddits.getChildren()
                .get(position);
    }

    public int getSubredditCount() {
        return subreddits.getChildren()
                .size();
    }

    public Drawable getDrawableForLink(Link link) {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return drawableSelf;
        }

        if (TextUtils.isEmpty(thumbnail) || thumbnail.equals(Reddit.DEFAULT) || thumbnail.equals(
                Reddit.NSFW)) {
            return drawableDefault;
        }

        return null;
    }

    public Link getLink(int position) {
        return (Link) links.getChildren().get(position - 1);
    }

    public void voteLink(RecyclerView.ViewHolder viewHolder, int vote) {

    }

    public int sizeLinks() {
        return links.getChildren().size();
    }

    public boolean isLoading() {
        return false;
    }

    public void loadMoreLinks() {
        // TODO: Load more links when end of list reached
        reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + query + "&after=" + links.getAfter(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            int positionStart = links.getChildren()
                                    .size();
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            links.addChildren(listing.getChildren());
                            links.setAfter(listing.getAfter());
                            for (Listener listener : listeners) {
                                listener.getAdapterLinks().notifyItemRangeInserted(positionStart,
                                        listing.getChildren().size());
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

    public Link getLinkSubreddit(int position) {
        return (Link) linksSubreddit.getChildren().get(position - 1);
    }

    public void voteLinkSubreddit(RecyclerView.ViewHolder viewHolder, int vote) {

    }

    public int sizeLinksSubreddit() {
        return linksSubreddit.getChildren().size();
    }

    public boolean isLoadingSubreddit() {
        return false;
    }

    public void loadMoreLinksSubreddit() {
        // TODO: Load more links when end of list reached
        reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + query + "&after=" + links.getAfter(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            int positionStart = linksSubreddit.getChildren()
                                    .size();
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            linksSubreddit.addChildren(listing.getChildren());
                            linksSubreddit.setAfter(listing.getAfter());
                            for (Listener listener : listeners) {
                                listener.getAdapterLinksSubreddit().notifyItemRangeInserted(positionStart,
                                        listing.getChildren().size());
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

    public Activity getActivity() {
        return activity;
    }

    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            reloadCurrentPage();
        }
    }

    public Time getTime() {
        return time;
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
        subreddits.getChildren().clear();
        links.getChildren().clear();
        linksSubreddit.getChildren().clear();
        query = "";
        for (Listener listener : listeners) {
            listener.getAdapterSearchSubreddits().notifyDataSetChanged();
            listener.getAdapterLinks().notifyDataSetChanged();
            listener.getAdapterLinksSubreddit().notifyDataSetChanged();
        }
    }

    public void setControllerLinks(ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
    }

    public interface Listener {
        void onClickSubreddit(Subreddit subreddit);
        AdapterSearchSubreddits getAdapterSearchSubreddits();
        AdapterLink getAdapterLinks();
        AdapterLink getAdapterLinksSubreddit();
        void setToolbarTitle(CharSequence title);
    }

    public interface ListenerCallback {
        ControllerSearch.Listener getListener();
        ControllerSearch getController();
        Activity getActivity();
    }
}
