/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.rx;

import android.util.Log;

import rx.functions.Action1;

/**
 * Created by TheKeeperOfPie on 2/6/2016.
 */
public class ActionLog<Type> implements Action1<Type> {

    private String tag;

    public ActionLog(String tag) {
        this.tag = tag;
    }

    @Override
    public void call(Type o) {
        Log.d(tag, "call() called with: " + o);
    }
}
