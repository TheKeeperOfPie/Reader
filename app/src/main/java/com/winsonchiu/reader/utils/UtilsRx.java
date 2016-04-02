/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 1/31/2016.
 */
public class UtilsRx {

    public static void unsubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    public static <Input, Result> Func1<Input, Observable<Result>> flatMapWrapError(Call<Input, Result> call) {
        return first -> {
            try {
                return Observable.just(call.call(first));
            } catch (Throwable t) {
                return Observable.error(t);
            }
        };
    }

    public interface Call<Input, Result> {
        Result call(Input input) throws Exception;
    }
}
