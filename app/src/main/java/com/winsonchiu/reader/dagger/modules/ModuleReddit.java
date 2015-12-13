/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import com.winsonchiu.reader.dagger.ActivityScope;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.links.ControllerLinks;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/13/2015.
 */
@Module
public class ModuleReddit {

    @ActivityScope
    @Provides
    public ControllerLinks provideControllerLinks() {
        return new ControllerLinks("", Sort.HOT);
    }

}
