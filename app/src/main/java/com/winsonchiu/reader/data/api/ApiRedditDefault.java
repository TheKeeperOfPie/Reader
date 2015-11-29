/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.api;

import com.squareup.okhttp.ResponseBody;
import com.winsonchiu.reader.data.reddit.Reddit;

import retrofit.Call;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by TheKeeperOfPie on 11/28/2015.
 */
public interface ApiRedditDefault {

    @POST("/api/v1/access_token")
    Call<ResponseBody> token(@Query(Reddit.QUERY_GRANT_TYPE) String grant,
                             @Query(Reddit.QUERY_DEVICE_ID) String id);

    @POST("/api/v1/revoke_token")
    Observable<String> tokenRevoke(@Query("token_type_hint") String tokenType,
                             @Query("token") String token);

}
