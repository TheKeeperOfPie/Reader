/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.util.List;

import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 4/17/2016.
 */
public class UtilsList {

    public static <Type> int indexOf(List<Type> list, Func1<Type, Boolean> condition) {
        int index = 0;

        for (Type type : list) {
            if (condition.call(type)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    @Nullable
    public static <Type> Pair<Integer, Type> findWithIndex(List<Type> list, Func1<Type, Boolean> condition) {
        int index = 0;

        for (Type type : list) {
            if (condition.call(type)) {
                return Pair.create(index, type);
            }

            index++;
        }

        return null;
    }

    public static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }
}
