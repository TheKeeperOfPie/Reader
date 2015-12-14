/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.accounts.AccountManager;
import android.content.Context;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.CustomApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Module
public class ModuleContext {

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
                .downloader(new OkHttpDownloader(okHttpClient));
        if (BuildConfig.DEBUG) {
            builder = builder.loggingEnabled(true);
        }
        return builder.build();
    }

}
