/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import com.android.volley.toolbox.HurlStack;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class OkHttpStack extends HurlStack {

    private final OkUrlFactory okUrlFactory;

    public OkHttpStack() {
        this.okUrlFactory = new OkUrlFactory(new OkHttpClient());
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return okUrlFactory.open(url);
    }

}
