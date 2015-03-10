package com.winsonchiu.reader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.winsonchiu.reader.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Reddit {

    private static final String TAG = Reddit.class.getCanonicalName();

    private static final long SEC_TO_MS = 1000;

    private static final String APP_ONLY_URL = "https://www.reddit.com/api/v1/access_token";
    private static final String OAUTH_BASE_URL = "https://oauth.reddit.com";
    private static final String REQUEST_LINK = "Link";

    // JSON fields
    private static final String INSTALLED_CLIENT_GRANT = "https://oauth.reddit.com/grants/installed_client";
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String GRANT_TYPE = "grant_type";
    private static final String DEVICE_ID = "device_id";
    private static final String DURATION = "duration";

    private static final String AFTER = "after";
    private static final String BEFORE = "before";
    private static final String COUNT = "count";
    private static final String LIMIT = "limit";
    private static final String SHOW = "show";

    private static Reddit reddit;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private SharedPreferences preferences;

    private Reddit(Context appContext) {
        requestQueue = Volley.newRequestQueue(appContext);
        imageLoader = new ImageLoader(requestQueue, new LruCacheBitmap(appContext));
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        try {
            fetchApplicationAccessToken();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        requestQueue.start();
    }

    public static Reddit getReddit(Context context) {
        if (reddit == null) {
            reddit = new Reddit(context.getApplicationContext());
        }
        return reddit;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    private void fetchApplicationAccessToken() throws JSONException {

        Log.d(TAG, "Expire time: " + preferences.getLong(AppSettings.EXPIRE_TIME, Long.MAX_VALUE));
        Log.d(TAG, "Current time: " + System.currentTimeMillis());

        if (preferences.getLong(AppSettings.EXPIRE_TIME, Long.MAX_VALUE) < System.currentTimeMillis() || preferences.getString(AppSettings.APP_ACCESS_TOKEN, "").equals("")) {

            if (preferences.getString(AppSettings.DEVICE_ID, "").equals("")) {
                preferences.edit().putString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()).commit();
            }

            HashMap<String, String> params = new HashMap<>();
            params.put(REDIRECT_URI, "https://com.winsonchiu.reader");
            params.put(GRANT_TYPE, INSTALLED_CLIENT_GRANT);
            params.put(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()));
            params.put(DURATION, "permanent");

            requestQueue.add(new RedditJsonRequest(preferences, params, Request.Method.POST, APP_ONLY_URL, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        Log.d(TAG, "Response: " + response);
                        preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN, response.getString(ACCESS_TOKEN)).commit();
                        preferences.edit().putLong(AppSettings.EXPIRE_TIME, System.currentTimeMillis() + response.getLong("expires_in") * SEC_TO_MS).commit();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    Log.d(TAG, new String(error.networkResponse.data));
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<>(3);
                    headers.put("User-agent", "android:com.winsonchiu.reader:v0.1 (by /u/TheKeeperOfPie)");
                    String creds = String.format("%s:%s", "zo7k-Nsh7vgn-Q", "");
                    String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                    headers.put("Authorization", auth);
                    headers.put("Content-Type","application/x-www-form-urlencoded; charset=utf-8");
                    return headers;
                }
            });
        }
    }

    public HeadRequest fetchHeaders(String url, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        HeadRequest headRequest = new HeadRequest(Request.Method.HEAD, url, listener, errorListener);
        requestQueue.add(headRequest);
        return headRequest;
    }


    public RedditJsonRequest getMoreLinks(String subreddit, String sort, String after, String before, int limit, boolean show, Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) throws JSONException {
        HashMap<String, String> params = new HashMap<>();

        StringBuilder builder = new StringBuilder(OAUTH_BASE_URL).append("/r/").append(subreddit).append("/").append(sort).append("?");
        if (!TextUtils.isEmpty(after)) {
            builder.append("after=").append(after).append("&");
        }
        else if (!TextUtils.isEmpty(before)) {
            builder.append("before=").append(before).append("&");
        }
        builder.append(CLIENT_ID).append("=").append("zo7k-Nsh7vgn-Q").append("&");
        builder.append(REDIRECT_URI).append("=").append("https://com.winsonchiu.reader").append("&");
        if (show) {
            builder.append(SHOW).append("all&");
        }

        String url = builder.toString();

        RedditJsonRequest redditJsonRequest = new RedditJsonRequest(preferences, params, Request.Method.GET, url, listener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    try {
                        fetchApplicationAccessToken();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                errorListener.onErrorResponse(error);
            }
        });

        requestQueue.add(redditJsonRequest);

        Log.d(TAG, "requestQueue added: " + requestQueue.toString());

        return redditJsonRequest;
    }

}
