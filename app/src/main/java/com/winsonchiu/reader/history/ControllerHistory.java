/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.ControllerListener;
import com.winsonchiu.reader.rx.FinalizingSubscriber;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by TheKeeperOfPie on 7/8/2015.
 */
public class ControllerHistory implements ControllerLinksBase {

    private static final String SEPARATOR = ",";
    private static final String TAG = ControllerHistory.class.getCanonicalName();

    @Inject Reddit reddit;
    @Inject Historian historian;

    private Context context;
    private ArrayList<Listener> listeners;
    private String title;
    private boolean isLoading;
    private Subreddit subreddit;
    private Listing history;
    private String query;
    private List<String> namesToFetch;
    private int lastIndex;
    private Thread threadSearch;
    private long timeStart;
    private long timeEnd;

    public ControllerHistory(Context context) {
        CustomApplication.getComponentMain().inject(this);
        this.context = context.getApplicationContext();
        subreddit = new Subreddit();
        listeners = new ArrayList<>();
        history = new Listing();
        namesToFetch = new ArrayList<>();
        query = "";
        timeEnd = Long.MAX_VALUE;
        title = context.getString(R.string.history);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.setToolbarTitle(title);
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
        if (!isLoading() && history.getChildren().isEmpty()) {
            reload();
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
        historian.saveToFile(context);
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

        reddit.info(builder.toString())
                .flatMap(Listing.FLAT_MAP)
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setIsLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        history = listing;
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void finish() {
                        setIsLoading(false);
                    }
                });
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
    public Observable<Listing> loadMoreLinks() {
        if (isLoading || namesToFetch.isEmpty()) {
            return Observable.empty();
        }

        int finalIndex = lastIndex + 25 < namesToFetch.size() ? lastIndex + 25 : namesToFetch.size();

        if (finalIndex == lastIndex) {
            return Observable.empty();
        }

        StringBuilder builder = new StringBuilder();

        while (lastIndex < finalIndex) {
            builder.append(namesToFetch.get(lastIndex));
            builder.append(SEPARATOR);
            lastIndex++;
        }

        Observable<Listing> observable = reddit.info(builder.toString())
                .flatMap(Listing.FLAT_MAP);

        observable.subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setIsLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        int startPosition = history.getChildren().size();
                        history.addChildren(listing.getChildren());
                        for (Listener listener : listeners) {
                            listener.getAdapter()
                                    .notifyItemRangeInserted(startPosition + 1, history
                                            .getChildren().size() - startPosition);
                        }
                    }

                    @Override
                    public void finish() {
                        setIsLoading(false);
                    }
                });

        return observable;
    }

    @Override
    public Subreddit getSubreddit() {
        return subreddit;
    }

    @Override
    public boolean showSubreddit() {
        return true;
    }

    @Override
    public boolean setReplyText(String name, String text, boolean collapsed) {

        for (int index = 0; index < history.getChildren().size(); index++) {
            Thing thing = history.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 1);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void setNsfw(String name, boolean over18) {

        for (int index = 0; index < history.getChildren().size(); index++) {
            Thing thing = history.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 1);
                }
                return;
            }
        }
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

    public int indexOf(Link link) {
        return history.getChildren().indexOf(link);
    }

    @Nullable
    public Link getPreviousLink(Link linkCurrent, int offset) {
        int index = indexOf(linkCurrent) - offset;
        if (index >= 0 && !history.getChildren().isEmpty()) {
            return (Link) history.getChildren().get(index);
        }

        return null;
    }

    @Nullable
    public Link getNextLink(Link linkCurrent, int offset) {
        int index = indexOf(linkCurrent) + offset;
        if (index < history.getChildren().size() && !history.getChildren().isEmpty()) {
            return (Link) history.getChildren().get(index);
        }

        return null;
    }

    public interface Listener extends ControllerListener {

    }

}
