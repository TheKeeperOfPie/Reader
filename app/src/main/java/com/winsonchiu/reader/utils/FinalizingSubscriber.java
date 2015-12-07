package com.winsonchiu.reader.utils;

import rx.Subscriber;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public abstract class FinalizingSubscriber<Type> extends Subscriber<Type> {

    @Override
    public final void onStart() {
        start();
    }

    @Override
    public final void onCompleted() {
        completed();
        finish();
    }

    @Override
    public final void onError(Throwable e) {
        e.printStackTrace();
        error(e);
        finish();
    }

    @Override
    public final void onNext(Type next) {
        next(next);
    }

    public void start() {

    }

    public void completed() {

    }

    public void error(Throwable e) {

    }

    public void next(Type next) {

    }

    public void finish() {

    }

}
