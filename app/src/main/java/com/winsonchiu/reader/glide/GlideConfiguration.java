/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.module.GlideModule;

/**
 * Created by TheKeeperOfPie on 3/13/2016.
 */
public class GlideConfiguration implements GlideModule {

    public static final int IMAGE_CACHE_SIZE = 1024 * 1024 * 1024;

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, IMAGE_CACHE_SIZE));
    }

    @Override
    public void registerComponents(Context context, Glide glide) {

    }
}
