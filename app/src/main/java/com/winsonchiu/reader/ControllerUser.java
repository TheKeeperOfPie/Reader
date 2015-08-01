/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by TheKeeperOfPie on 6/24/2015.
 */
public class ControllerUser {

    private SharedPreferences preferences;
    private User user;

    public ControllerUser(Activity activity) {
        super();
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        user = new User();
        reloadUser();
    }

    public User getUser() {
        return user;
    }

    public void setActivity(Activity activity) {

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
}
