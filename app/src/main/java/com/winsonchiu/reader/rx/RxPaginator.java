/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.rx;

import com.jakewharton.rxrelay.BehaviorRelay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * TODO: Handle terminal event for no more content
 * Created by TheKeeperOfPie on 8/27/2016.
 */

public class RxPaginator<Response, Result, Error> {

    public static final String TAG = RxPaginator.class.getCanonicalName();

    private BehaviorRelay<Integer> relayLoadMore = BehaviorRelay.create();

    private State<Response> state = new State<>();

    public RxPaginator(EventHolder<Result, Error> eventHolder,
            Func1<List<Response>, Observable<Response>> generator,
            Func1<List<Response>, Observable<Result>> converter,
            Func1<Throwable, Error> error) {
        relayLoadMore
                .distinctUntilChanged()
                .flatMap(integer -> {
                    if (integer == 0) {
                        state.getPage().set(1);
                        state.getResponses().clear();

                        if (eventHolder.getData().hasValue()) {
                            return converter.call(state.getResponses())
                                    .doOnNext(eventHolder.relayData)
                                    .map(result -> integer);
                        }
                    }

                    return Observable.just(integer);
                })
                .filter(page -> page > 0)
                .doOnNext(page -> eventHolder.getLoading().call(true))
                .flatMap(page1 -> generator.call(state.getResponses())
                        .flatMap(response -> {
                            state.getPage().incrementAndGet();
                            state.getResponses().add(response);

                            return converter.call(state.getResponses());
                        })
                        .doOnNext(eventHolder.relayData)
                        .doOnUnsubscribe(() -> eventHolder.getLoading().call(false))
                        .doOnError(throwable -> relayLoadMore.call(-2))
                        .doOnError(t -> eventHolder.getErrors().call(error.call(t)))
                        .onErrorResumeNext(Observable.empty())
                        .subscribeOn(Schedulers.io()))
                .subscribe();
    }

    public void clear() {
        relayLoadMore.call(0);
    }

    public void reload() {
        clear();
        loadMore();
    }

    public void loadMore() {
        relayLoadMore.call(state.getPage().get());
    }

    public State<Response> getState() {
        return state;
    }

    public static class EventHolder<Data, Error> {

        private BehaviorRelay<Data> relayData = BehaviorRelay.create();
        private BehaviorRelay<Boolean> relayLoading = BehaviorRelay.create(false);
        private BehaviorRelay<Error> relayErrors = BehaviorRelay.create();

        public BehaviorRelay<Data> getData() {
            return relayData;
        }

        public BehaviorRelay<Boolean> getLoading() {
            return relayLoading;
        }

        public BehaviorRelay<Error> getErrors() {
            return relayErrors;
        }
    }

    public static class State<Response> {

        private final AtomicInteger page = new AtomicInteger(1);
        private final List<Response> responses = Collections.synchronizedList(new ArrayList<>());

        public AtomicInteger getPage() {
            return page;
        }

        public List<Response> getResponses() {
            return responses;
        }
    }
}