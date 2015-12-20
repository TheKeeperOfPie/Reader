/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.UriMatcher;
import android.net.Uri;

import java.util.Arrays;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 12/19/2015.
 */
public enum Match {

    // TODO: Handle wiki links
    NONE(UriMatcher.NO_MATCH, ""),
    SUBREDDIT(0, "/r/*/"),
    SUBREDDIT_HOT(1, "/r/*/hot/"),
    SUBREDDIT_NEW(2, "/r/*/new/"),
    SUBREDDIT_RISING(3, "/r/*/rising/"),
    SUBREDDIT_CONTROVERSIAL(4, "/r/*/controversial/"),
    SUBREDDIT_TOP(5, "/r/*/top/"),
    SUBREDDIT_GILDED(6, "/r/*/gilded/"),
    COMMENTS(10, "/comments/*/", "/r/*/comments/*/"),
    COMMENTS_TITLED(12, "/comments/*/*/", "/r/*/comments/*/*/"),
    COMMENTS_TITLED_ID(13, "/comments/*/*/*/", "/r/*/comments/*/*/*/"),
    USER(20, "/u/*/", "/user/*/"),
    SEARCH_SUBREDDIT(30, "/r/*/search/")
    ;

    private static final String TAG = Match.class.getCanonicalName();

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    public static final String AUTHORITY_BASE = "reddit.com";

    public static final List<String> HOST_PREFIXES = Arrays.asList(
            "",
            "m.",
            "i.",
            "np.",
            "www."
    );

    // Generate all catching Reddit URIs
    static {
        for (String prefix : HOST_PREFIXES) {
            for (Match match : values()) {
                if (match != NONE) {
                    for (String suffix : match.getSuffixes()) {
                        URI_MATCHER.addURI(prefix + AUTHORITY_BASE, suffix, match.getId());
                    }
                }
            }
        }
    }

    private final int id;
    private final String[] suffixes;

    Match(int id, String... suffixes) {
        this.id = id;
        this.suffixes = suffixes;
    }

    public static Match fromId(int id) {
        for (Match match : values()) {
            if (match.getId() == id) {
                return match;
            }
        }

        throw new IllegalArgumentException(TAG + " ID is invalid");
    }

    public static Match matchUri(Uri uri) {
        return fromId(URI_MATCHER.match(uri));
    }

    public int getId() {
        return id;
    }

    public String[] getSuffixes() {
        return suffixes;
    }
}
