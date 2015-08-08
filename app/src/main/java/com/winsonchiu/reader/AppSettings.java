/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AppSettings {

    public static final String THEME_DARK = "Dark";
    public static final String THEME_LIGHT = "Light";
    public static final String THEME_BLACK = "Black";

    public static final String THEME_RED = "Red";
    public static final String THEME_PINK = "Pink";
    public static final String THEME_PURPLE = "Purple";
    public static final String THEME_DEEP_PURPLE = "Deep Purple";
    public static final String THEME_INDIGO = "Indigo";
    public static final String THEME_BLUE = "Blue";
    public static final String THEME_LIGHT_BLUE = "Light Blue";
    public static final String THEME_CYAN = "Cyan";
    public static final String THEME_TEAL = "Teal";
    public static final String THEME_GREEN = "Green";
    public static final String THEME_LIGHT_GREEN = "Light Green";
    public static final String THEME_LIME = "Lime";
    public static final String THEME_YELLOW = "Yellow";
    public static final String THEME_AMBER = "Amber";
    public static final String THEME_ORANGE = "Orange";
    public static final String THEME_DEEP_ORANGE = "Deep Orange";
    public static final String THEME_BROWN = "Brown";
    public static final String THEME_GREY = "Grey";
    public static final String THEME_BLUE_GREY = "Blue Grey";

    // List of all possible tokens
    public static final String INITIALIZED = "initialized";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String DEVICE_ID = "deviceId";
    public static final String EXPIRE_TIME = "expireTime";

    public static final String INTERFACE_MODE = "interfaceMode";
    public static final String MODE_LIST = "List";
    public static final String MODE_GRID = "Grid";

    public static final String ACCOUNT_JSON = "accountJson";
    public static final String SUBSCRIBED_SUBREDDITS = "subscribedSubreddits";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String ACCOUNT_NAME = "accountName";

    public static final String HISTORY_SET = "historySet";

    // Preferences

    public static final String PREF_FULL_TIMESTAMPS = "pref_full_timestamps";
    public static final String PREF_SHOW_THUMBNAILS = "pref_show_thumbnails";
    public static final String PREF_NSFW_THUMBNAILS = "pref_nsfw_thumbnails";
    public static final String PREF_GRID_THUMBNAIL_SIZE = "pref_grid_thumbnail_size";
    public static final String PREF_INBOX_CHECK_INTERVAL = "pref_inbox_check_interval";
    public static final String PREF_SAVE_HISTORY = "pref_save_history";
    public static final String PREF_CLEAR_HISTORY = "pref_clear_history";
    public static final String PREF_DIM_POSTS = "pref_dim_posts";
    public static final String PREF_EXTERNAL_BROWSER = "pref_external_browser";
    public static final String PREF_HISTORY_SIZE = "pref_history_size";
    public static final String BETA_NOTICE_0 = "betaNotice0";
    public static final String SWIPE_EXIT_COMMENTS = "swipe_exit_comments";
    public static final String PREF_THEME_BACKGROUND = "pref_theme_background";
    public static final String PREF_THEME_PRIMARY = "pref_theme_primary";
    public static final String PREF_THEME_ACCENT = "pref_theme_accent";

    private static final String TAG = AppSettings.class.getCanonicalName();

    public static boolean initPrefs(Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean initialized = preferences.getBoolean(INITIALIZED, false);

        if (!initialized) {
            // TODO: Set initial values

            SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean(INITIALIZED, true);
            editor.apply();

        }

        return initialized;
    }

}