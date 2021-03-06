/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.winsonchiu.reader.dagger.components.ComponentMain;
import com.winsonchiu.reader.dagger.components.DaggerComponentMain;

import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class CustomApplication extends Application {

    private static final String TAG = CustomApplication.class.getCanonicalName();

    private static ComponentMain componentMain = DaggerComponentMain.builder().build();
    private static CustomApplication application;

    public static RefWatcher getRefWatcher(Context context) {
        CustomApplication application = (CustomApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    private RefWatcher refWatcher;

    public static ComponentMain getComponentMain() {
        return componentMain;
    }

    public static CustomApplication getApplication() {
        return application;
    }

    @Override
    public void onCreate() {
        application = this;
        super.onCreate();
        refWatcher = LeakCanary.install(this);
        Fabric.with(this, new Crashlytics());
        AppSettings.initPrefs(getApplicationContext());

        if (BuildConfig.DEBUG) {
            ButterKnife.setDebug(true);
            Stetho.initializeWithDefaults(this);
        }
    }
}
