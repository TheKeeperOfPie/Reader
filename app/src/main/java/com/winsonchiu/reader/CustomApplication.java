/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Application;

import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.history.Historian;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class CustomApplication extends Application {

//    public static RefWatcher getRefWatcher(Context context) {
//        CustomApplication application = (CustomApplication) context.getApplicationContext();
//        return application.refWatcher;
//    }
//
//    private RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();
//        refWatcher = LeakCanary.install(this);
//        Picasso.with(this).setIndicatorsEnabled(true);
//        Picasso.with(this).setLoggingEnabled(true);
        AppSettings.initPrefs(getApplicationContext());
        Reddit.getInstance(getApplicationContext()).fetchToken(null);
        Historian.getInstance(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
