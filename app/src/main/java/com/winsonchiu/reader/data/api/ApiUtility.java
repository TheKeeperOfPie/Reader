package com.winsonchiu.reader.data.api;

import com.winsonchiu.reader.data.reddit.Reddit;

import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

/**
 * Created by TheKeeperOfPie on 12/6/2015.
 */
public interface ApiUtility {

    @GET(Reddit.GFYCAT_URL + "{id}")
    Observable<String> gfycat(@Path("id") String id);
}
