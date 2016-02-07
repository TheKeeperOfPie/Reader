/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.api;

import com.winsonchiu.reader.data.reddit.Reddit;

import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by TheKeeperOfPie on 11/27/2015.
 */
public interface ApiRedditAuthorized {

    @GET
    Observable<String> about(@Url String url);

    @GET("/api/info")
    Observable<String> info(@Query("id") String id);

    @GET
    Observable<String> links(@Url String url,
                             @Query("t") String time,
                             @Query("limit") Integer limit,
                             @Query("after") String after,
                             @Query("show") String show);

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

    @POST("/api/morechildren" +
            "?api_type=json")
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

    @GET
    Observable<String> subreddits(@Url String url,
                                  @Query("after") String after,
                                  @Query("limit") Integer limit,
                                  @Query("show") String show);

    @GET("/subreddits/search" +
            "?show=all")
    Observable<String> subredditsSearch(@Query("q") String query,
                                        @Query("sort") String sort);

    @GET
    Observable<String> search(@Url String url,
                              @Query("q") String query,
                              @Query("sort") String sort,
                              @Query("t") String time,
                              @Query("after") String after,
                              @Query("restrict_sr") Boolean restrictSubreddit);

    @POST("/api/comment" +
            "?api_type=json")
    Observable<String> comment(@Query("thing_id") String id,
                               @Query("text") String text);

    @POST("/api/vote")
    Observable<String> vote(@Query("id") String id,
              @Query(Reddit.QUERY_VOTE) Integer vote);

    @POST("/api/del")
    Observable<String> delete(@Query("id") String id);

    @POST("/api/save")
    Observable<String> save(@Query("id") String id,
              @Query(Reddit.QUERY_CATEGORY) String category);

    @POST("/api/unsave")
    Observable<String> unsave(@Query("id") String id);

    @POST("/api/hide")
    Observable<String> hide(@Query("id") String id);

    @POST("/api/unhide")
    Observable<String> unhide(@Query("id") String id);

    @POST("/api/marknsfw")
    Observable<String> markNsfw(@Query("id") String id);

    @POST("/api/unmarknsfw")
    Observable<String> unmarkNsfw(@Query("id") String id);

    @POST("/api/read_message")
    Observable<String> markRead(@Query("id") String id);

    @GET("/api/v1/me")
    Observable<String> me();

    @GET("/api/needs_captcha")
    Observable<String> needsCaptcha();

    @POST("/api/new_captcha" +
            "?api_type=json")
    Observable<String> newCaptcha();

    @POST("/api/subscribe")
    Observable<String> subscribe(@Query("action") String action,
                                 @Query("sr") String subreddit);

    @POST("/api/editusertext" +
            "?api_type=json")
    Observable<String> editUserText(@Query("thing_id") String id,
                                   @Query("text") String text);

    @POST("/api/read_all_messages")
    Observable<String> readAllMessages();

    @POST("/api/compose" +
            "?api_type=json")
    Observable<String> compose(@Query("subject") String subject,
                               @Query("text") String text,
                               @Query("to") String recipient,
                               @Query("iden") String captchaId,
                               @Query("captcha") String captchaText);

    @POST("/api/submit" +
            "?api_type=json&" +
            "resubmit=true&" +
            "sendreplies=true&" +
            "then=comments&" +
            "extension=json")
    Observable<String> submit(@Query("kind") String kind,
                              @Query("sr") String subreddit,
                              @Query("title") String title,
                              @Query("url") String url,
                              @Query("text") String text,
                              @Query("iden") String captchaId,
                              @Query("captcha") String captchaText);

    @POST("/api/report?api_type=json")
    Observable<String> report(@Query("thing_id") String id,
                              @Query("reason") String reason,
                              @Query("other_reason") String otherReason);

    @GET("/api/recommend/sr/{subreddit}")
    Observable<String> recommend(@Path("subreddit") String subreddit,
                                 @Query("omit") String omit);

}
