package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

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
        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public User getUser() {
        return user;
    }

    public void setActivity(Activity activity) {

    }
}
