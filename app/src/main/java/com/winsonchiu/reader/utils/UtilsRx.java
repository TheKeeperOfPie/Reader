/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import rx.Subscription;

/**
 * Created by TheKeeperOfPie on 1/31/2016.
 */
public class UtilsRx {

    public static void unsubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

}
