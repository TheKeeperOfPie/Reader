/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.content.Context;

import com.winsonchiu.reader.data.reddit.Reddit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Module
public class ModuleApi {

    @Singleton
    @Provides
    public Reddit provideReddit(Context context) {
        return new Reddit(context);
    }
}
