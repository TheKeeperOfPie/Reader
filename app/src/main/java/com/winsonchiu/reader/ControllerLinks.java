package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks implements Controller {

    // TODO: Check if need setActivity

    private static final String TAG = ControllerLinks.class.getCanonicalName();

    private Activity activity;
    private Listing listingLinks;
    private Listing listingSubreddits;
    private boolean isLoading;
    private String sort = "";
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Reddit reddit;
    private List<LinkClickListener> listeners;
    private SharedPreferences preferences;

    public ControllerLinks(Activity activity, String subredditName, String sort) {
        setActivity(activity);
        this.reddit = Reddit.getInstance(activity);
        this.listeners = new ArrayList<>();
        listingLinks = new Listing();
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        this.sort = sort;
        listingSubreddits = new Listing();
        if (!TextUtils.isEmpty(subredditName)) {
            List<Thing> subreddits = new ArrayList<>();
            Subreddit subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddits.add(subreddit);
            listingSubreddits.setChildren(subreddits);
        }
        // TODO: Check whether using name vs displayName matters when loading subreddits
    }

    public void addListener(LinkClickListener linkClickListener) {
        listeners.add(linkClickListener);
    }

    public void removeListener(LinkClickListener linkClickListener) {
        listeners.remove(linkClickListener);
    }

    public void setParameters(String subredditName, String sort) {
        this.sort = sort;
        List<Thing> subreddits = new ArrayList<>();
        Subreddit subreddit = new Subreddit();
        subreddit.setDisplayName(subredditName);
        subreddits.add(subreddit);
        listingSubreddits = new Listing();
        listingSubreddits.setChildren(subreddits);
        reloadAllLinks();
    }

    public void setSort(String sort) {
        if (!this.sort.equalsIgnoreCase(sort)) {
            this.sort = sort;
            reloadAllLinks();
        }
    }

    private void setTitle() {
        String subredditName = "Front Page";
        if (listingSubreddits.getChildren().size() > 0) {
            subredditName = "/r/" + ((Subreddit) listingSubreddits.getChildren().get(0)).getDisplayName();
        }

        for (LinkClickListener listener : listeners) {
            listener.setToolbarTitle(subredditName);
        }
    }

    public Link getLink(int position) {
        return (Link) listingLinks.getChildren().get(position);
    }

    public Drawable getDrawableForLink(Link link) {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return drawableSelf;
        }

        if (TextUtils.isEmpty(thumbnail) || thumbnail.equals(Reddit.DEFAULT)) {
            return drawableDefault;
        }

        return null;
    }

    public void reloadAllLinks() {
        setLoading(true);

        String url = Reddit.OAUTH_URL + "/";

        if (listingSubreddits.getChildren().size() > 0) {

            StringBuilder builder = new StringBuilder();
            for (Thing thing : listingSubreddits.getChildren()) {
                builder.append(((Subreddit) thing).getDisplayName());
                builder.append("+");
            }

            url += "r/" + builder.toString()
                    .substring(0, builder.length() - 1) + "/";
        }

        if (sort.contains("top")) {
            url += "top?t=" + sort.substring(0, sort.indexOf("top")) + "&";
        }
        else {
            url += sort + "?";
        }

        url += "limit=50&showAll=true";

        reddit.loadGet(url, new Listener<String>() {
            @Override
            public void onResponse(String response) {
                // TODO: Catch null errors in parent method call
                if (response == null) {
                    return;
                }
                Log.d(TAG, "Result: " + response);

                try {
                    listingLinks = Listing.fromJson(new JSONObject(response));
                    for (LinkClickListener listener : listeners) {
                        listener.getAdapter()
                                .notifyDataSetChanged();
                        listener.onFullLoaded(0);
                    }
                    setTitle();
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

            }
        }, 0);
        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        String url = Reddit.OAUTH_URL + "/";

        if (listingSubreddits.getChildren().size() > 0) {

            StringBuilder builder = new StringBuilder();
            for (Thing thing : listingSubreddits.getChildren()) {
                builder.append(((Subreddit) thing).getDisplayName());
                builder.append("+");
            }

            url += "r/" + builder.toString()
                    .substring(0, builder.length() - 1) + "/";
        }

        if (sort.contains("top")) {
            url += "top?t=" + sort.substring(0, sort.indexOf("top")) + "&";
        }
        else {
            url += sort + "?";
        }

        url += "limit=50&after=" + listingLinks.getAfter() + "&showAll=true";

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
                        } catch (JSONException exception) {
                            exception.printStackTrace();
                        } finally {
                            setLoading(false);
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    public void voteLink(final RecyclerView.ViewHolder viewHolder, final int vote) {

        if (TextUtils.isEmpty(preferences.getString(AppSettings.REFRESH_TOKEN, null))) {
            Toast.makeText(activity, "Must be logged in to vote", Toast.LENGTH_SHORT).show();
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
                ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo();
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
                        ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo();
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

    public ImageLoader.ImageContainer loadImage(String url, ImageLoader.ImageListener imageListener) {
        return reddit.getImageLoader().get(url, imageListener);
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

    public int size() {
        return listingLinks.getChildren() == null ? 0 : listingLinks.getChildren().size();
    }

    public interface LinkClickListener extends DisallowListener {

        void onClickComments(Link link);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        void setToolbarTitle(String title);
        AdapterLink getAdapter();
        int getRecyclerHeight();
    }

    public interface ListenerCallback {
        LinkClickListener getListener();
        Controller getController();
        int getColorPositive();
        int getColorNegative();
        Activity getActivity();
        float getItemWidth();
        RecyclerView.LayoutManager getLayoutManager();
    }

}
