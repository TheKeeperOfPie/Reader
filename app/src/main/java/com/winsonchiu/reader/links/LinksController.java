/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.rx.RxPaginator;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 8/19/2016.
 */

public abstract class LinksController<Holder extends LinksController.EventHolder> {

    private final Holder eventHolder;

    private RxPaginator<Listing, LinksModel, LinksError> paginator;

    public LinksModel linksModel;

    public LinksController(Holder holder) {
        this.eventHolder = holder;
    }

    public void initialize(Func1<List<Listing>, Observable<Listing>> generator, Func1<List<Listing>, Observable<LinksModel>> converter) {
        paginator = new RxPaginator<>(eventHolder, generator, converter, t -> LinksError.LOAD);

        eventHolder.getData()
                .subscribe(linksModel -> this.linksModel = linksModel);
    }

    public Holder getEventHolder() {
        if (!eventHolder.getData().hasValue() || eventHolder.getData().getValue().getLinks().isEmpty()) {
            loadMore();
        }

        return eventHolder;
    }

    public void clear() {
        paginator.clear();
    }

    public void reload() {
        paginator.reload();
    }

    public void loadMore() {
        paginator.loadMore();
    }

    public int indexOfLink(Link link) {
        String name = link.getName();
        return 0;
//        return UtilsList.indexOf(listing.getChildren(), thing -> thing.getName().equals(name));
    }

    public void update(Link link) {
        int index = indexOfLink(link);

        if (index > -1) {
//            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
        }

        publishUpdate();
    }

    public void remove(Link link) {
        int index = indexOfLink(link);

        if (index > -1) {
//            listing.getChildren().remove(index);
        }

        publishUpdate();
    }

    public void clearViewed(Historian historian) {
        List<Integer> indexesToRemove = new ArrayList<>();

//        for (int index = 0; index < listing.getChildren().size(); index++) {
//            Link link = (Link) listing.getChildren().get(index);
//            if (historian.contains(link.getName())) {
//                indexesToRemove.add(0, index);
//            }
//        }
//
//        for (int index : indexesToRemove) {
//            listing.getChildren().remove(index);
//        }

        publishUpdate();
    }

    public void publishUpdate() {
        // TODO: Make removals/updates actually work by storing a secondary mutable state object
//        eventHolder.getData().call(paginatedEventHolder.getData().getValue());
    }

    public static class EventHolder extends RxPaginator.EventHolder<LinksModel, LinksError> {

    }
}
