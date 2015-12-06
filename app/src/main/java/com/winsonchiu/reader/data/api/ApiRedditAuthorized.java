/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.api;

import com.squareup.okhttp.ResponseBody;
import com.winsonchiu.reader.data.reddit.Reddit;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by TheKeeperOfPie on 11/27/2015.
 */
public interface ApiRedditAuthorized {

    @GET("{subreddit}about")
    Observable<String> about(@Path(value = "subreddit", encoded = true) String pathSubreddit);

    @GET("/api/info")
    Observable<String> info(@Query("id") String id);

    @GET("{subreddit}{sort}?showAll=true")
    Observable<String> links(@Path(value = "subreddit", encoded = true) String pathSubreddit,
                               @Path(value = "sort", encoded = true) String sort,
                               @Query("t") String time,
                               @Query("limit") Integer limit,
                               @Query("after") String after);

    @GET("/r/{subreddit}/comments/{id}")
    Observable<String> comments(@Path("subreddit") String subreddit,
                                @Path("id") String id,
                                @Query("comment") String comment,
                                @Query("sort") String sort,
                                @Query("showmore") Boolean showMore,
                                @Query("showedits") Boolean showEdits,
                                @Query("context") Integer context,
                                @Query("depth") Integer depth,
                                @Query("limit") Integer limit);

    @POST("/api/morechildren?api_type=json")
    Observable<String> moreChildren(@Query("link_id") String idLink,
                                    @Query("children") String children);

    @GET("/message/{page}")
    Observable<String> message(@Path("page") String page,
                               @Query("after") String after);

    @GET("/user/{user}/{page}")
    Observable<String> user(@Path("user") String user,
                            @Path("page") String page,
                            @Query("sort") String sort,
                            @Query("t") String time,
                            @Query("after") String after,
                            @Query("limit") Integer limit);

    @GET("/subreddits/{page}?show=all")
    Observable<String> subreddits(@Path("page") String page,
                                  @Query("after") String after,
                                  @Query("limit") Integer limit);

    @GET("/subreddits/search?show=all")
    Observable<String> subredditsSearch(@Query("q") String query,
                                        @Query("sort") String sort);

    @GET("{subreddit}/search")
    Observable<String> search(@Path("subreddit") String pathSubreddit,
                              @Query("q") String query,
                              @Query("sort") String sort,
                              @Query("t") String time,
                              @Query("after") String after,
                              @Query("restrict_sr") Boolean restrictSubreddit);

    @POST("/api/comment?api_type=json")
    Call<ResponseBody> comment(@Query("thing_id") String id,
                               @Query("text") String text);

    @POST("/api/vote")
    Call vote(@Query(Reddit.QUERY_ID) String id,
              @Query(Reddit.QUERY_VOTE) Integer vote);

    @POST("/api/del")
    Call delete(@Query(Reddit.QUERY_ID) String id);

    @POST("/api/save")
    Call save(@Query(Reddit.QUERY_ID) String id,
              @Query(Reddit.QUERY_CATEGORY) String category);

    @POST("/api/unsave")
    Call unsave(@Query(Reddit.QUERY_ID) String id);

    @POST("/api/hide")
    Call hide(@Query(Reddit.QUERY_ID) String id);

    @POST("/api/unhide")
    Call unhide(@Query(Reddit.QUERY_ID) String id);

    @POST("/api/marknsfw")
    Call markNsfw(@Query(Reddit.QUERY_ID) String id);

    @POST("/api/unmarknsfw")
    Call unmarkNsfw(@Query(Reddit.QUERY_ID) String id);

    @POST("/api/read_message")
    Call markRead(@Query(Reddit.QUERY_ID) String id);

    @GET("/api/v1/me")
    Observable<String> me();

    @GET("/api/needs_captcha")
    Observable<String> needsCaptcha();

    @POST("/api/new_captcha?api_type=json")
    Observable<String> newCaptcha();

}
