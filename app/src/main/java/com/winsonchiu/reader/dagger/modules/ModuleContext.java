/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.data.database.reddit.RedditDatabase;
import com.winsonchiu.reader.data.reddit.Reddit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Module
public class ModuleContext {

    public static final int IMAGE_CACHE_SIZE = 1024 * 1024 * 1024;

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
    public Picasso providePicasso(Context context) {
        Picasso.Builder builder = new Picasso.Builder(context.getApplicationContext())
                .downloader(new OkHttp3Downloader(context, IMAGE_CACHE_SIZE));
        if (BuildConfig.DEBUG) {
//            builder = builder.loggingEnabled(true);
        }
        return builder.build();
    }

    @Singleton
    @Provides
    public RequestManager provideRequestManager(Context context) {
        return Glide.with(context.getApplicationContext());
    }

    @Singleton
    @Provides
    public SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Singleton
    @Provides
    public RedditDatabase provideRedditDatabase(Application application, Reddit reddit, Picasso picasso) {
        return new RedditDatabase(application, reddit, picasso);
    }

}
