/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.rx;

import rx.Observer;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public abstract class ObserverComplete<Type> implements Observer<Type> {

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(Type type) {

    }
}
