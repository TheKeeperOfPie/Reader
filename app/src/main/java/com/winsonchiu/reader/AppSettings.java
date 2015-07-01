package com.winsonchiu.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AppSettings {

    // Massive list of all possible tokens
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

    public static final String HISTORY_SET = "historySet";

    public static final String PREF_FULL_TIMESTAMPS = "pref_full_timestamps";
    public static final String PREF_SHOW_THUMBNAILS = "pref_show_thumbnails";
    public static final String PREF_NSFW_THUMBNAILS = "pref_nsfw_thumbnails";
    public static final String PREF_GRID_THUMBNAIL_SIZE = "pref_grid_thumbnail_size";
    public static final String PREF_INBOX_CHECK_INTERVAL = "pref_inbox_check_interval";

    public static boolean initPrefs(Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean initialized = preferences.getBoolean(INITIALIZED, false);

        if (!initialized) {
            // TODO: Set initial values

            SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean(INITIALIZED, true);
            editor.commit();

        }

        return initialized;
    }
}