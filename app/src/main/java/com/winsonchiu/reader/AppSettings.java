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
    public static final String APP_ACCESS_TOKEN = "applicationAccessToken";
    public static final String DEVICE_ID = "deviceId";
    public static final String EXPIRE_TIME = "expireTime";

    public static final String INTERFACE_MODE = "interfaceMode";
    public static final String MODE_LIST = "List";
    public static final String MODE_GRID = "Grid";

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