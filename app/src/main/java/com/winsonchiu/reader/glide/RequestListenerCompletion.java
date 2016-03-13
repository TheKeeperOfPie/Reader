/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.glide;

import android.support.annotation.CallSuper;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/**
 * Created by TheKeeperOfPie on 3/13/2016.
 */
public abstract class RequestListenerCompletion<Model, Resource> implements RequestListener<Model, Resource> {

    @Override
    @CallSuper
    public boolean onException(Exception e, Model model, Target<Resource> target, boolean isFirstResource) {
        onCompleted();
        return false;
    }

    @Override
    @CallSuper
    public boolean onResourceReady(Resource resource, Model model, Target<Resource> target, boolean isFromMemoryCache, boolean isFirstResource) {
        onCompleted();
        return false;
    }

    protected abstract void onCompleted();
}
