/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.ControllerListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 7/8/2015.
 */
public class ControllerHistory implements ControllerLinksBase {

    private static final String SEPARATOR = ",";
    private static final String TAG = ControllerHistory.class.getCanonicalName();

    private Reddit reddit;
    private ArrayList<Listener> listeners;
    private Historian historian;
    private boolean isLoading;
    private Subreddit subreddit;
    private Listing history;
    private Activity activity;
    private String query;
    private List<String> namesToFetch;
    private int lastIndex;
    private Thread threadSearch;
    private long timeStart;
    private long timeEnd;

    public ControllerHistory(Activity activity) {
        setActivity(activity);
        subreddit = new Subreddit();
        listeners = new ArrayList<>();
        history = new Listing();
        namesToFetch = new ArrayList<>();
        query = "";

        timeEnd = Long.MAX_VALUE;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.historian = Historian.getInstance(activity);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.setToolbarTitle(activity.getString(R.string.history));
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
        if (!isLoading() && history.getChildren().isEmpty()) {
            reload();
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
        Historian.saveToFile(activity);
    }

    public int getSize() {
        return historian.getSize();
    }

    public void reload() {

        lastIndex = 0;

        if (TextUtils.isEmpty(query)) {
            namesToFetch.clear();
            HistoryEntry currentEntry = historian.getFirst();

            while (currentEntry != null) {
                if (!currentEntry.isRemoved() && currentEntry.getTimestamp() > timeStart && currentEntry.getTimestamp() < timeEnd) {
                    namesToFetch.add(currentEntry.getName());
                }
                currentEntry = currentEntry.getNext();
            }
        }

        if (namesToFetch.isEmpty()) {
            history = new Listing();
            for (Listener listener : listeners) {
                listener.getAdapter().notifyDataSetChanged();
            }
            setIsLoading(false);
            return;
        }

        setIsLoading(true);

        StringBuilder builder = new StringBuilder();

        int finalIndex = lastIndex + 25 > namesToFetch.size() ? namesToFetch.size() : lastIndex + 25;

        while (lastIndex < finalIndex) {
            builder.append(namesToFetch.get(lastIndex));
            builder.append(SEPARATOR);
            lastIndex++;
        }

        Log.d(TAG, "reload");

        reddit.loadGet(Reddit.OAUTH_URL + "/api/info?id=" + builder.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            history = Listing.fromJson(new JSONObject(response));
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyDataSetChanged();
                            }
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        setIsLoading(false);

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setIsLoading(false);
                    }
                }, 0);

    }

    @Override
    public Link getLink(int position) {
        return (Link) history.getChildren().get(position - 1);
    }

    @Override
    public int sizeLinks() {
        return history.getChildren().size();
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
        for (Listener listener : listeners) {
            listener.setRefreshing(isLoading);
        }
    }

    @Override
    public void loadMoreLinks() {
        if (isLoading || namesToFetch.isEmpty()) {
            return;
        }

        int finalIndex = lastIndex + 25 < namesToFetch.size() ? lastIndex + 25 : namesToFetch.size();

        if (finalIndex == lastIndex) {
            return;
        }

        setIsLoading(true);

        StringBuilder builder = new StringBuilder();

        while (lastIndex < finalIndex) {
            builder.append(namesToFetch.get(lastIndex));
            builder.append(SEPARATOR);
            lastIndex++;
        }

        reddit.loadGet(Reddit.OAUTH_URL + "/api/info?id=" + builder.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            int startPosition = history.getChildren().size();

                            Listing listing = Listing.fromJson(new JSONObject(response));
                            history.addChildren(listing.getChildren());
                            for (Listener listener : listeners) {
                                listener.getAdapter()
                                        .notifyItemRangeInserted(startPosition + 1, history
                                                .getChildren().size() - startPosition);
                            }
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        setIsLoading(false);

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setIsLoading(false);
                    }
                }, 0);

    }

    @Override
    public Subreddit getSubreddit() {
        return subreddit;
    }

    @Override
    public boolean showSubreddit() {
        return true;
    }

    public void setQuery(String query) {
        query = query.toLowerCase();
        if (!this.query.equals(query)) {
            this.query = query;

            if (threadSearch != null) {
                threadSearch.interrupt();
                threadSearch = null;
            }

            if (!TextUtils.isEmpty(query)) {

                // TODO: Figure out if this is a good way of doing active search
                threadSearch = new ThreadHistorySearch(historian.getFirst(), query,
                        new ThreadHistorySearch.Callback() {
                            @Override
                            public void onFinished(List<String> names) {
                                namesToFetch = names;
                                for (Listener listener : listeners) {
                                    listener.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            reload();
                                        }
                                    });
                                }
                            }
                        });

                threadSearch.start();

            }
            else {
                reload();
            }
        }
    }

    public Link remove(int position) {
        Link link = getLink(position);
        history.getChildren().remove(link);
        historian.getEntry(link).setRemoved(true);
        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(position);
        }
        return link;
    }

    public String getQuery() {
        return query;
    }

    public void add(int position, Link link) {
        historian.getEntry(link).setRemoved(false);
        history.getChildren().add(position - 1, link);
        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemInserted(position);
        }
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public interface Listener extends ControllerListener {

    }

}
