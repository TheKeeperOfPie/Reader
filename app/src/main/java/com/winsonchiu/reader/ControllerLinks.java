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
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks implements ControllerLinksBase {

    // TODO: Check if need setActivity

    private static final String TAG = ControllerLinks.class.getCanonicalName();

    private Activity activity;
    private Listing listingLinks;
    private Subreddit subreddit;
    private boolean isLoading;
    private Sort sort;
    private Time time;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Reddit reddit;
    private Set<LinkClickListener> listeners;
    private SharedPreferences preferences;

    public ControllerLinks(Activity activity, String subredditName, Sort sort) {
        setActivity(activity);
        this.reddit = Reddit.getInstance(activity);
        this.listeners = new HashSet<>();
        listingLinks = new Listing();
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
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
        // TODO: Check whether using name vs displayName matters when loading subreddits
    }

    public void addListener(LinkClickListener linkClickListener) {
        listeners.add(linkClickListener);
    }

    public void removeListener(LinkClickListener linkClickListener) {
        listeners.remove(linkClickListener);
    }

    public void setParameters(String subredditName, Sort sort) {
        if (!TextUtils.equals(subredditName, subreddit.getDisplayName())) {
            this.sort = sort;
            subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddit.setUrl("/r/" + subredditName + "/");
            reloadSubreddit();
        }
    }

    public void reloadSubreddit() {
        reddit.loadGet(
                Reddit.OAUTH_URL + subreddit.getUrl() + "about",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            subreddit = Subreddit.fromJson(new JSONObject(response));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        reloadAllLinks();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        reloadAllLinks();
                    }
                }, 0);
    }

    public void loadFrontPage(Sort sort) {
        if (!TextUtils.isEmpty(subreddit.getDisplayName())) {
            for (LinkClickListener listener : listeners) {
                listener.setRefreshing(true);
            }
            this.sort = sort;
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            reloadAllLinks();
        }
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            reloadAllLinks();
        }
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            reloadAllLinks();
        }
    }

    public void setTitle() {
        String subredditName = Reddit.FRONT_PAGE;
        if (!TextUtils.isEmpty(subreddit.getDisplayName())) {
            subredditName = subreddit.getUrl();
        }

        for (LinkClickListener listener : listeners) {
            listener.setToolbarTitle(subredditName);
        }
    }

    public Link getLink(int position) {
        return (Link) listingLinks.getChildren()
                .get(position - 1);
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

    @Override
    public int sizeLinks() {
        return listingLinks.getChildren() == null ? 0 : listingLinks.getChildren()
                .size();
    }

    public void reloadAllLinks() {
        setLoading(true);

        String url = Reddit.OAUTH_URL + subreddit.getUrl() + sort.toString() + "?t=" + time.toString() + "&limit=50&showAll=true";

        reddit.loadGet(url, new Listener<String>() {
            @Override
            public void onResponse(final String response) {
                // TODO: Catch null errors in parent method call
                if (response == null) {
                    return;
                }

                Log.d(TAG, "Response: " + response);

                try {
                    Listing listing = Listing.fromJson(new JSONObject(response));
                    if (listing.getChildren()
                            .isEmpty() || !(listing.getChildren()
                            .get(0) instanceof Link)) {
                        listingLinks = new Listing();
                    }
                    else {
                        listingLinks = listing;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                for (LinkClickListener listener : listeners) {
                    listener.getAdapter()
                            .notifyDataSetChanged();
                    listener.onFullLoaded(0);
                    listener.loadSideBar(subreddit);
                    listener.setEmptyView(listingLinks.getChildren()
                            .isEmpty());
                }
                setTitle();
                setLoading(false);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, 0);
        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        String url = Reddit.OAUTH_URL + subreddit.getUrl() + sort.toString() + "?t=" + time.toString() + "&limit=50&showAll=true&after=" + listingLinks.getAfter();

        reddit.loadGet(url,
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            int positionStart = listingLinks.getChildren()
                                    .size();
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            listingLinks.addChildren(listing.getChildren());
                            listingLinks.setAfter(listing.getAfter());
                            for (LinkClickListener listener : listeners) {
                                listener.getAdapter()
                                        .notifyItemRangeInserted(positionStart,
                                                listingLinks.getChildren()
                                                        .size() - positionStart);
                            }
                        }
                        catch (JSONException exception) {
                            exception.printStackTrace();
                        }
                        finally {
                            setLoading(false);
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(activity, "Error loading links", Toast.LENGTH_SHORT)
                                .show();
                    }
                }, 0);
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public Subreddit getSubreddit() {
        return subreddit;
    }

    @Override
    public void voteLink(final RecyclerView.ViewHolder viewHolder, final int vote) {

        if (TextUtils.isEmpty(preferences.getString(AppSettings.REFRESH_TOKEN, null))) {
            Toast.makeText(activity, "Must be logged in to vote", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        final int position = viewHolder.getAdapterPosition();
        final Link link = getLink(position);

        final int oldVote = link.isLikes();
        int newVote = 0;

        if (link.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, link.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        link.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo(link);
            }
            else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
            }
        }
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();

                link.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                        ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                        ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo(link);
                    }
                    else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                        ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
                    }
                }
            }
        }, params, 0);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        for (LinkClickListener listener : listeners) {
            listener.setRefreshing(loading);
        }
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
    }

    public String getSubredditName() {

        if (TextUtils.isEmpty(subreddit.getDisplayName())) {
            return Reddit.FRONT_PAGE;
        }

        return subreddit.getDisplayName();
    }

    public String getSubredditUrl() {
        if (TextUtils.isEmpty(subreddit.getUrl())) {
            return Reddit.FRONT_PAGE;
        }

        return subreddit.getUrl();
    }

    public Sort getSort() {
        return sort;
    }

    public Time getTime() {
        return time;
    }

    public interface LinkClickListener extends DisallowListener {

        void onClickComments(Link link, RecyclerView.ViewHolder viewHolder);

        void loadUrl(String url);

        void onFullLoaded(int position);

        void setRefreshing(boolean refreshing);

        void setToolbarTitle(String title);

        AdapterLink getAdapter();

        int getRecyclerHeight();

        void loadSideBar(Subreddit listingSubreddits);

        void setEmptyView(boolean visible);

        int getRecyclerWidth();

        ControllerCommentsBase getControllerComments();
    }

    public interface ListenerCallback {
        LinkClickListener getListener();

        ControllerLinksBase getController();

        float getItemWidth();

        RecyclerView.LayoutManager getLayoutManager();

        SharedPreferences getPreferences();

        ControllerCommentsBase getControllerComments();

        void pauseViewHolders();
    }

}
