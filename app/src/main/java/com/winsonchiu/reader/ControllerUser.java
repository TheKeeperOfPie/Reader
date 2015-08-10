/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
    private AccountManager accountManager;
    private Account account;

    public ControllerUser(Activity activity) {
        super();
        setActivity(activity);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        user = new User();
    }

    public User getUser() {
        return user;
    }

    public void setActivity(Activity activity) {
        reddit = Reddit.getInstance(activity);
        accountManager = AccountManager.get(activity.getApplicationContext());
    }

    public void addUser(String tokenAccess, String tokenRefresh, long timeExpire) {
    }

    public void reloadUser() {
        reddit.loadGet(Reddit.OAUTH_URL + "/api/v1/me",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "User onResponse: " + response);
                        try {
                            user = User.fromJson(Reddit.getObjectMapper().readValue(response, JsonNode.class));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Check user info error
                    }
                }, 0);
    }

    public boolean hasUser() {
        return account != null;//user != null && !TextUtils.isEmpty(user.getName());
    }

    public void clearAccount() {
        account = null;
        user = new User();
    }

    public void setAccount(Account accountUser) {
        boolean accountFound = false;
        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(accountUser.name)) {
                this.account = account;
                accountFound = true;
                reloadUser();
                break;
            }
        }

        if (!accountFound) {
            account = null;
            user = new User();
        }
    }
}
