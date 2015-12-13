/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.components;

import com.winsonchiu.reader.dagger.modules.ModuleData;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Singleton
@Component(
        modules = {
                ModuleData.class
        }
)
public interface ComponentData {
    void inject(ComponentStatic component);
}