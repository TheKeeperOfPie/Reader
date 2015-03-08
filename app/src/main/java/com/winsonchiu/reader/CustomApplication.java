package com.winsonchiu.reader;

import android.app.Application;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        AppSettings.initPrefs(getApplicationContext());
    }
}
