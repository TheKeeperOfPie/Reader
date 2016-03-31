/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

/**
 * Created by TheKeeperOfPie on 3/30/2016.
 */
public class Utils {

    public static boolean inRangeInclusive(int first, int input, int second) {
        if (first < second) {
            return input >= first && input <= second;
        }

        return input >= second && input <= first;
    }

    public static boolean inRangeExclusive(int first, int input, int second) {
        if (first < second) {
            return input >= first && input <= second;
        }

        return input >= second && input <= first;
    }

    public static boolean inRangeInclusive(float first, float input, float second) {
        if (first < second) {
            return input >= first && input <= second;
        }

        return input >= second && input <= first;
    }

    public static boolean inRangeExclusive(float first, float input, float second) {
        if (first < second) {
            return input >= first && input <= second;
        }

        return input >= second && input <= first;
    }
}
