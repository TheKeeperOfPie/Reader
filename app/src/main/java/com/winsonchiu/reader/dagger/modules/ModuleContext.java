/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.data.database.reddit.RedditDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Module
public class ModuleContext {

    @Singleton
    @Provides
    public Application provideApplication() {
        return CustomApplication.getApplication();
    }

    @Singleton
    @Provides
    public CustomApplication provideCustomApplication() {
        return CustomApplication.getApplication();
    }

    @Singleton
    @Provides
    public Context provideContext() {
        return CustomApplication.getApplication().getApplicationContext();
    }

    @Singleton
    @Provides
    public AccountManager provideAccountManager(Context context) {
        return AccountManager.get(context.getApplicationContext());
    }

    @Singleton
    @Provides
    public Picasso providePicasso(Context context, OkHttpClient okHttpClient) {
        Picasso.Builder builder = new Picasso.Builder(context.getApplicationContext())
                .downloader(new OkHttp3Downloader(okHttpClient));
        if (BuildConfig.DEBUG) {
//            builder = builder.loggingEnabled(true);
        }
        return builder.build();
    }

    @Singleton
    @Provides
    public SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Singleton
    @Provides
    public RedditDatabase provideRedditDatabase(Application application) {
        return new RedditDatabase(application);
    }

}
