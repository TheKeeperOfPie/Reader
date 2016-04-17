/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.annotation.CallSuper;

import com.squareup.picasso.Callback;

/**
 * Created by TheKeeperOfPie on 4/16/2016.
 */
public abstract class PicassoEndCallback implements Callback {

    @CallSuper
    @Override
    public void onSuccess() {
        onEnd();
    }

    @CallSuper
    @Override
    public void onError() {
        onEnd();
    }

    public abstract void onEnd();
}
