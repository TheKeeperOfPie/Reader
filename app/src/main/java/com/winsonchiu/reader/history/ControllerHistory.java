/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.adapter.RxAdapterEvent;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.links.LinksModel;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.utils.UtilsRx;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 7/8/2015.
 */
public class ControllerHistory implements ControllerLinksBase {

    private static final String SEPARATOR = ",";
    private static final String TAG = ControllerHistory.class.getCanonicalName();

    @Inject Reddit reddit;
    @Inject Historian historian;
    private ControllerUser controllerUser;

    private boolean isLoading;
    private int lastIndex;
    private Thread threadSearch;
    private long timeStart;
    private long timeEnd = Long.MAX_VALUE;

    private String query = "";
    private Listing history = new Listing();
    private Subreddit subreddit = new Subreddit();
    private List<String> namesToFetch = new ArrayList<>();

    private EventHolder eventHolder = new EventHolder();

    public ControllerHistory(ControllerUser controllerUser) {
        this.controllerUser = controllerUser;
        CustomApplication.getComponentMain().inject(this);
    }

    public EventHolder getEventHolder() {
        if (!isLoading() && history.getChildren().isEmpty()) {
            reload();
        }

        return eventHolder;
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
            eventHolder.call(new RxAdapterEvent<>(getData()));
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
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setIsLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        history = listing;
                        eventHolder.call(new RxAdapterEvent<>(getData()));
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
        eventHolder.getLoading().call(isLoading);
    }

    @Override
    public Observable<Listing> loadMoreLinks() {
        int finalIndex = lastIndex + 25 < namesToFetch.size() ? lastIndex + 25 : namesToFetch.size();

        if (isLoading || namesToFetch.isEmpty() || finalIndex == lastIndex) {
            return Observable.empty();
        }

        StringBuilder builder = new StringBuilder();

        while (lastIndex < finalIndex) {
            builder.append(namesToFetch.get(lastIndex));
            builder.append(SEPARATOR);
            lastIndex++;
        }

        return reddit.info(builder.toString())
                .flatMap(UtilsRx.flatMapWrapError(response -> Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
                .doOnNext(listing -> {
                    int startPosition = history.getChildren().size();
                    history.addChildren(listing.getChildren());
                    eventHolder.call(new RxAdapterEvent<>(getData(),
                            RxAdapterEvent.Type.INSERT,
                            startPosition + 1,
                            history.getChildren().size() - startPosition));
                })
                .doOnSubscribe(() -> setIsLoading(true))
                .doOnTerminate(() -> setIsLoading(false));
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
                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
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
                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
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
                        names -> {
                            namesToFetch = names;
                            reload();
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
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, position));
        return link;
    }

    public String getQuery() {
        return query;
    }

    public void add(int position, Link link) {
        historian.getEntry(link).setRemoved(false);
        history.getChildren().add(position - 1, link);
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, position));
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

    private LinksModel getData() {
        List<Link> links = new ArrayList<>(history.getChildren().size());

        for (Thing thing : history.getChildren()) {
            if (thing instanceof Link) {
                links.add((Link) thing);
            }
        }

        return new LinksModel(new Subreddit(), links, true, controllerUser.getUser());
    }

    public static class EventHolder implements Action1<RxAdapterEvent<LinksModel>> {

        private BehaviorRelay<RxAdapterEvent<LinksModel>> relayData = BehaviorRelay.create(new RxAdapterEvent<>(new LinksModel()));
        private BehaviorRelay<Boolean> relayLoading = BehaviorRelay.create(false);

        @Override
        public void call(RxAdapterEvent<LinksModel> event) {
            relayData.call(event);
        }

        public Observable<RxAdapterEvent<LinksModel>> getData() {
            return relayData;
        }

        public BehaviorRelay<Boolean> getLoading() {
            return relayLoading;
        }
    }

}
