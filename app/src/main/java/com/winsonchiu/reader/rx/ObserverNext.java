/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.rx;

import rx.Observer;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public abstract class ObserverNext<Type> implements Observer<Type> {

    @Override
    public final void onCompleted() {

    }

    @Override
    public final void onError(Throwable e) {

    }
}
