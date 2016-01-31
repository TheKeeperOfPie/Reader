/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.modules;

import android.app.Application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winsonchiu.reader.data.database.reddit.RedditDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Module
public class ModuleData {

    @Singleton
    @Provides
    public ObjectMapper provideObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Singleton
    @Provides
    public RedditDatabase provideRedditDatabase(Application application) {
        return new RedditDatabase(application);
    }

}
