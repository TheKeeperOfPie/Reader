/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.content.Context;

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
}
