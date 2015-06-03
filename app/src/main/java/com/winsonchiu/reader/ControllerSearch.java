package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class ControllerSearch implements ControllerLinksBase {

    private static final String TAG = ControllerSearch.class.getCanonicalName();
    private final ControllerLinks controllerLinks;
    private Set<Listener> listeners;
    private Activity activity;
    private SharedPreferences preferences;
    private Reddit reddit;
    private Listing subreddits;
    private Listing links;
    private Listing ussrs;
    private String query;
    private Drawable drawableSelf;
    private Drawable drawableDefault;

    public ControllerSearch(Activity activity, ControllerLinks controllerLinks) {
        setActivity(activity);
        this.controllerLinks = controllerLinks;
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
        ussrs = new Listing();
    }

    public void addListener(Listener linkClickListener) {
        listeners.add(linkClickListener);
    }

    public void removeListener(Listener linkClickListener) {
        listeners.remove(linkClickListener);
    }

    public void setQuery(String query) {
        this.query = query;
        reloadSubreddits();
        reloadLinks();
    }

    public void reloadSubreddits() {

        reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/search?sort=relevance&show=all&q=" + query,
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

        reddit.loadGet(Reddit.OAUTH_URL + "/search?q=" + query,
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
        return (Link) links.getChildren()
                .get(position);
    }

    @Override
    public Reddit getReddit() {
        return null;
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

    public User getUser(int position) {
        return null;
    }

    public int getUserCount() {
        return 0;
    }

    public interface Listener {
        void onClickSubreddit(Subreddit subreddit);
        void notifyChangedSubreddits();
        void notifyChangedLinks();
        AdapterLink getAdapterLinks();
    }

    public interface ListenerCallback {
        ControllerSearch.Listener getListener();

        ControllerSearch getController();

        Activity getActivity();
    }
}
