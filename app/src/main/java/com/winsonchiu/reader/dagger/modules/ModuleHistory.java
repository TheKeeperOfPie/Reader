/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.content.Context;

import com.winsonchiu.reader.history.Historian;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Module
public class ModuleHistory {

    @Singleton
    @Provides
    public Historian provideHistorian(Context context) {
        return new Historian(context);
    }

}
