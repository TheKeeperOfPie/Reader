/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.rx;

import rx.Observer;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public class ObserverEmpty<Type> implements Observer<Type> {

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(Type type) {

    }
}
