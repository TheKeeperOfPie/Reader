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

import java.util.HashSet;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class ControllerSearch implements ControllerLinksBase {

    public static final int PAGE_LINKS_SUBREDDIT = 0;
    public static final int PAGE_SUBREDDITS = 1;
    public static final int PAGE_LINKS = 2;

    private static final String TAG = ControllerSearch.class.getCanonicalName();

    private final ControllerLinks controllerLinks;
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

    public ControllerSearch(Activity activity, ControllerLinks controllerLinks) {
        setActivity(activity);
        this.controllerLinks = controllerLinks;
        sort = Sort.RELEVANCE;
        time = Time.ALL;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        listeners = new HashSet<>();
        query = "";
        subreddits = new Listing();
        links = new Listing();
        linksSubreddit = new Listing();
    }

    public void addListener(Listener linkClickListener) {
        listeners.add(linkClickListener);
    }

    public void removeListener(Listener linkClickListener) {
        listeners.remove(linkClickListener);
    }

    public void setQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            return;
        }
        this.query = query;
        for (Listener listener : listeners) {
            listener.setToolbarTitle(query);
        }
        reloadCurrentPage();
    }

    public void reloadCurrentPage() {
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

        requestSubreddits = reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/search?show=all&q=" + query + "&sort=" + sort.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        subreddits = new Listing();
                        try {
                            subreddits = Listing.fromJson(new JSONObject(response));
                            for (Listener listener : listeners) {
                                listener.notifyChangedSubreddits();
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

        if (requestLinks != null) {
            requestLinks.cancel();
        }

        requestLinks = reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + query + "&sort=" + sort.toString() + "&t=" + time.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        links = new Listing();

                        Log.d(TAG, "Response: " + response);
                        try {
                            links = Listing.fromJson(new JSONObject(response));
                            for (Listener listener : listeners) {
                                listener.notifyChangedLinks();
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

    public void reloadLinksSubreddit() {

        String subreddit = controllerLinks.getSubredditName();
        String url = Reddit.OAUTH_URL;

        if (!Reddit.FRONT_PAGE.equals(subreddit)) {
            url += "/r/" + subreddit;
        }

        if (requestLinksSubreddit != null) {
            requestLinksSubreddit.cancel();
        }

        requestLinksSubreddit = reddit.loadGet(url + "/search?restrict_sr=on&q=" + query + "&sort=" + sort.toString() + "&t=" + time.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        linksSubreddit = new Listing();

                        Log.d(TAG, "Response: " + response);
                        try {
                            linksSubreddit = Listing.fromJson(new JSONObject(response));
                            for (Listener listener : listeners) {
                                listener.notifyChangedLinks();
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

    public Subreddit getSubreddit(int position) {
        return (Subreddit) subreddits.getChildren()
                .get(position);
    }

    public int getSubredditCount() {
        return subreddits.getChildren()
                .size();
    }

    @Override
    public Link getLink(int position) {
        if (currentPage == PAGE_LINKS_SUBREDDIT) {
            return (Link) linksSubreddit.getChildren().get(position);
        }

        return (Link) links.getChildren()
                .get(position);
    }

    @Override
    public Reddit getReddit() {
        return reddit;
    }

    @Override
    public void voteLink(RecyclerView.ViewHolder viewHolder, int vote) {

    }

    @Override
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

    @Override
    public int sizeLinks() {
        if (currentPage == PAGE_LINKS_SUBREDDIT) {
            return linksSubreddit.getChildren().size();
        }

        return links.getChildren()
                .size();
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
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

    @Override
    public Activity getActivity() {
        return activity;
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
        if (currentPage == PAGE_LINKS || currentPage == PAGE_LINKS_SUBREDDIT) {
            for (Listener listener : listeners) {
                listener.notifyChangedLinks();
            }
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public interface Listener {
        void onClickSubreddit(Subreddit subreddit);
        void notifyChangedSubreddits();
        void notifyChangedLinks();
        AdapterLink getAdapterLinks();
        void setToolbarTitle(CharSequence title);
    }

    public interface ListenerCallback {
        ControllerSearch.Listener getListener();
        ControllerSearch getController();
        Activity getActivity();
    }
}
