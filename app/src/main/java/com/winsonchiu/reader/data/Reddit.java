package com.winsonchiu.reader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.winsonchiu.reader.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Reddit {

    private static final String APP_ONLY_URL = "https://www.reddit.com/api/v1/";
    private static final String REQUEST_LINK = "Link";

    // JSON fields
    private static final String INSTALLED_CLIENT_GRANT = "https://oauth.reddit.com/grants/installed_client";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String GRANT_TYPE = "grant_type";
    private static final String DEVICE_ID = "device_id";

    private static Reddit reddit;

    private RequestQueue requestQueue;
    private SharedPreferences preferences;

    private Reddit(Context appContext) {
        requestQueue = Volley.newRequestQueue(appContext);
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        fetchApplicationAccessToken();
    }

    public static Reddit getReddit(Context context) {
        if (reddit == null) {
            reddit = new Reddit(context.getApplicationContext());
        }
        return reddit;
    }

    private void fetchApplicationAccessToken() {

        if (preferences.getString(AppSettings.APP_ACCESS_TOKEN, "").equals("")) {

            if (preferences.getString(AppSettings.DEVICE_ID, "").equals("")) {
                preferences.edit().putString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()).commit();
            }

            try {
                JSONObject requestObject = new JSONObject();
                requestObject.put(GRANT_TYPE, INSTALLED_CLIENT_GRANT);
                requestObject.put(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()));

                requestQueue.add(new JsonObjectRequest(Request.Method.GET, APP_ONLY_URL, requestObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN, response.getString(ACCESS_TOKEN)).commit();
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    public void stopNetwork() {
        requestQueue.stop();
    }

}
