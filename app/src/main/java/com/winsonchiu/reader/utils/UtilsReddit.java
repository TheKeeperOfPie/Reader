/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by TheKeeperOfPie on 2/6/2016.
 */
public class UtilsReddit {

    @Nullable
    public static String parseRawSubredditString(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        input = input.replaceAll("\\s", "");

        if (input.startsWith("/r/")) {
            input = input.substring(3);
        }
        else if (input.startsWith("r/")) {
            input = input.substring(2);
        }
        else if (input.startsWith("/")) {
            input = input.substring(1);
        }

        if (!input.matches("^[A-z]+$")) {
            return null;
        }

        return input;
    }

    @Nullable
    public static String parseSubredditUrlPart(String input) {
        String subreddit = parseRawSubredditString(input);
        if (TextUtils.isEmpty(subreddit)) {
            return null;
        }
        else {
            return "/r/" + subreddit + "/";
        }
    }
}
