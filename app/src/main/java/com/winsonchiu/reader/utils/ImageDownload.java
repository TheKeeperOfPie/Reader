/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

/**
 * Created by TheKeeperOfPie on 1/10/2016.
 */
public class ImageDownload {

    private String title;
    private String fileName;
    private String url;

    public ImageDownload(String title, String fileName, String url) {
        this.title = title;
        this.fileName = fileName;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }
}
