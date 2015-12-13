/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.winsonchiu.reader.dagger.components.ComponentMain;
import com.winsonchiu.reader.dagger.components.DaggerComponentMain;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class CustomApplication extends Application {

    private ComponentMain componentMain;
    private static CustomApplication application;

    public static RefWatcher getRefWatcher(Context context) {
        CustomApplication application = (CustomApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    private RefWatcher refWatcher;

    public static ComponentMain getComponentMain() {
        return application.componentMain;
    }

    public static CustomApplication getApplication() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        refWatcher = LeakCanary.install(this);
        componentMain = DaggerComponentMain.builder()
                .build();
        AppSettings.initPrefs(getApplicationContext());
    }
}
