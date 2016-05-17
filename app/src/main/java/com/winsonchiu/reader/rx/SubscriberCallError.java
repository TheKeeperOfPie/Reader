/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.rx;

import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 5/1/2016.
 */
public class SubscriberCallError<T, Event> extends Subscriber<T> {

    private final Action1<Event> action;
    private final Event event;

    public SubscriberCallError(Action1<Event> action, Event event) {
        this.action = action;
        this.event = event;
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        action.call(event);
    }

    @Override
    public void onNext(Object o) {

    }
}
