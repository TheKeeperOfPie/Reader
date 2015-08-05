/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.User;

import java.io.IOException;

/**
 * Created by TheKeeperOfPie on 6/24/2015.
 */
public class ControllerUser {

    private static final String TAG = ControllerUser.class.getCanonicalName();
    private SharedPreferences preferences;
    private User user;
    private Reddit reddit;

    public ControllerUser(Activity activity) {
        super();
        setActivity(activity);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        user = new User();
        reloadUser();
        loadSubredditList();
    }

    public User getUser() {
        return user;
    }

    public void setActivity(Activity activity) {
        reddit = Reddit.getInstance(activity);
    }

    public void addUser(String accountToken, String refreshToken) {

    }

    private void loadSubredditList() {
        // TODO: Support loading moderated, contributor, and multiple pages using after
        reddit.loadGet(Reddit.OAUTH_URL + "/api/v1/me",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "User onResponse: " + response);
                        preferences.edit()
                                .putString(AppSettings.ACCOUNT_JSON,
                                        response)
                                .apply();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Check user info error
                    }
                }, 0);
    }

    public void reloadUser() {
        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(Reddit.getObjectMapper().readValue(
                        preferences.getString(AppSettings.ACCOUNT_JSON, ""), JsonNode.class));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasUser() {
        return user != null && !TextUtils.isEmpty(user.getName());
    }
}
