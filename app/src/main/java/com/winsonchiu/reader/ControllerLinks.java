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

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
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
import java.util.Map;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks implements ControllerLinksBase {

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
    private Set<Listener> listeners;
    private SharedPreferences preferences;

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

    public ControllerLinks(Activity activity, JSONObject data) {
        setActivity(activity);
        this.listeners = new HashSet<>();
        listingLinks = new Listing();
        try {
            sort = Sort.valueOf(data.getString("sort"));
            time = Time.valueOf(data.getString("time"));
            subreddit = Subreddit.fromJson(new JSONObject(data.getString("subreddit")));
        }
        catch (JSONException e) {
            this.sort = Sort.HOT;
            this.time = Time.ALL;
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            e.printStackTrace();
        }
        reloadAllLinks(true);
    }

    public String saveData() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sort", sort.name());
        jsonObject.put("time", time.name());
        jsonObject.put("subreddit", subreddit.toJsonString());

        return jsonObject.toString();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.setSort(sort);
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
        Log.d(TAG, "addListener: " + listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setParameters(String subredditName, Sort sort) {
        if (!TextUtils.equals(subredditName, subreddit.getDisplayName())) {
            this.sort = sort;
            subreddit = new Subreddit();
            subreddit.setDisplayName(subredditName);
            subreddit.setUrl("/r/" + subredditName + "/");
            int size = sizeLinks();
            listingLinks = new Listing();
            for (Listener listener : listeners) {
                listener.setSort(sort);
                listener.getAdapter()
                        .notifyItemRangeRemoved(0, size + 1);
            }
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
                            Log.d(TAG, "subreddit: " + response);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        reloadAllLinks(true);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        reloadAllLinks(true);
                    }
                }, 0);
    }

    public void loadFrontPage(Sort sort, boolean force) {
        if (force || !TextUtils.isEmpty(subreddit.getDisplayName())) {
            for (Listener listener : listeners) {
                listener.setRefreshing(true);
            }
            this.sort = sort;
            subreddit = new Subreddit();
            subreddit.setUrl("/");
            reloadAllLinks(false);
        }
        Log.d(TAG, "loadFrontPage");
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            reloadAllLinks(false);
        }
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            reloadAllLinks(false);
        }
    }

    public void setTitle() {
        String subredditName = Reddit.FRONT_PAGE;
        if (!TextUtils.isEmpty(subreddit.getDisplayName())) {
            subredditName = subreddit.getUrl();
        }

        for (Listener listener : listeners) {
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

        if (Reddit.DEFAULT.equals(thumbnail) || Reddit.NSFW.equals(thumbnail)) {
            return drawableDefault;
        }

        return null;
    }

    @Override
    public int sizeLinks() {
        return listingLinks.getChildren() == null ? 0 : listingLinks.getChildren()
                .size();
    }

    public void reloadAllLinks(final boolean isListCleared) {
        setLoading(true);

        String url = Reddit.OAUTH_URL + subreddit.getUrl() + sort.toString() + "?t=" + time.toString() + "&limit=25&showAll=true";

        reddit.loadGet(url, new Response.Listener<String>() {
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

                for (Listener listener : listeners) {
                    listener.getAdapter().notifyDataSetChanged();
                    listener.loadSideBar(subreddit);
                    listener.showEmptyView(listingLinks.getChildren()
                            .isEmpty());
                }
                setTitle();
                setLoading(false);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                setLoading(false);
                Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                        .show();
            }
        }, 0);
        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        String url = Reddit.OAUTH_URL + subreddit.getUrl() + sort.toString() + "?t=" + time.toString() + "&limit=15&showAll=true&after=" + listingLinks.getAfter();

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            int positionStart = listingLinks.getChildren()
                                    .size();
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            listingLinks.addChildren(listing.getChildren());
                            listingLinks.setAfter(listing.getAfter());
                            for (Listener listener : listeners) {
                                listener.getAdapter()
                                        .notifyItemRangeInserted(positionStart + 1,
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
                        setLoading(false);
                        Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
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
    public void deletePost(Link link) {
        int index = listingLinks.getChildren()
                .indexOf(link);

        if (index >= 0) {
            listingLinks.getChildren()
                    .remove(index);
            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRemoved(index + 1);
            }

            Map<String, String> params = new HashMap<>();
            params.put("id", link.getName());

            reddit.loadPost(Reddit.OAUTH_URL + "/api/del",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(
                                String response) {
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(
                                VolleyError error) {

                        }
                    }, params, 0);
        }
    }

    @Override
    public void voteLink(final RecyclerView.ViewHolder viewHolder, final Link link, int vote) {
        reddit.voteLink(viewHolder, link, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    public Reddit getReddit() {
        return reddit;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        for (Listener listener : listeners) {
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
        this.reddit = Reddit.getInstance(activity);
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
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

    public interface Listener extends ControllerListener {
        void setSort(Sort sort);
        void showEmptyView(boolean isEmpty);
        void loadSideBar(Subreddit subreddit);
    }

    public interface ListenerCallback {
        ControllerLinksBase getControllerLinks();
        float getItemWidth();
        int getTitleMargin();
        RecyclerView.LayoutManager getLayoutManager();
        SharedPreferences getPreferences();
        ControllerCommentsBase getControllerComments();
        User getUser();
        void pauseViewHolders();
    }

}
