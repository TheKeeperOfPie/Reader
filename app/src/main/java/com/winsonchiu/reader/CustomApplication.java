package com.winsonchiu.reader;

import android.app.Application;

import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        AppSettings.initPrefs(getApplicationContext());
        try {
            Reddit.fetchApplicationAccessToken(getApplicationContext(), null);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
