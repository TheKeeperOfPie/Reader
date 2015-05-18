package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class ControllerSubreddits {

    private static final String TAG = ControllerSubreddits.class.getCanonicalName();
    private Set<SubredditListener> listeners;
    private Activity activity;
    private Listing allSubreddits;
    private List<Thing> visibleSubreddits;
    private Reddit reddit;
    private String query;

    public ControllerSubreddits(Activity activity) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        query = "";
        reddit = Reddit.getInstance(activity);
        listeners = new HashSet<>();
        allSubreddits = new Listing();
        visibleSubreddits = new ArrayList<>();
        if (TextUtils.isEmpty(preferences.getString(AppSettings.SUBSCRIBED_SUBREDDITS, ""))) {
            loadFrontPageSubreddits();
        }
        else {
            try {
                // TODO: Refresh subscription list periodically
                allSubreddits = Listing.fromJson(
                        new JSONObject(
                                preferences.getString(AppSettings.SUBSCRIBED_SUBREDDITS, "")));
                Collections.sort(allSubreddits.getChildren(), new Comparator<Thing>() {
                    @Override
                    public int compare(Thing lhs, Thing rhs) {
                        return ((Subreddit) lhs).getDisplayName()
                                .compareToIgnoreCase(((Subreddit) rhs).getDisplayName());
                    }
                });
                visibleSubreddits = new ArrayList<>(allSubreddits.getChildren());
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFrontPageSubreddits() {
        reddit.loadGet(Reddit.OAUTH_URL + "/subreddits/default?limit=100&show=all",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            allSubreddits = Listing.fromJson(new JSONObject(response));
                            Collections.sort(allSubreddits.getChildren(), new Comparator<Thing>() {
                                @Override
                                public int compare(Thing lhs, Thing rhs) {
                                    return (int) (((Subreddit) rhs).getSubscribers() - ((Subreddit) lhs).getSubscribers());
                                }
                            });
                            setQuery(query);
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

    public void addListener(SubredditListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SubredditListener listener) {
        listeners.remove(listener);
    }

    public int getItemCount() {
        return visibleSubreddits.size();
    }

    public Subreddit getSubreddit(int position) {
        return (Subreddit) visibleSubreddits.get(position);
    }

    public void setQuery(String query) {

        this.query = query;

        if (TextUtils.isEmpty(query)) {
            visibleSubreddits = new ArrayList<>(allSubreddits.getChildren());
            return;
        }

        List<Thing> subreddits = new ArrayList<>();
        List<Thing> trailingResults = new ArrayList<>();

        for (Thing thing : allSubreddits.getChildren()) {
            Subreddit subreddit = (Subreddit) thing;

            int queryIndex = subreddit.getDisplayName().indexOf(query);

            if (queryIndex == 0) {
                subreddits.add(subreddit);
            }
            else if (queryIndex > 0) {
                trailingResults.add(subreddit);
            }

        }

        subreddits.addAll(trailingResults);

        visibleSubreddits = subreddits;

        Log.d(TAG, "visibleSubreddits:" + visibleSubreddits);

        for (SubredditListener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }

    }

    public interface SubredditListener extends DisallowListener {
        void onClickSubreddit(Subreddit subreddit);
        AdapterSubreddits getAdapter();
    }

    public interface ListenerCallback {
        SubredditListener getListener();
        ControllerSubreddits getController();
        Activity getActivity();
    }

}