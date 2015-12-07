/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public abstract class TargetImageDownload implements Target {

    private String fileName;
    private String url;

    public TargetImageDownload(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }
}
