package com.winsonchiu.reader.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
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

    // Constant values to represent Thing states
    public enum Vote {
        NOT_VOTED, UPVOTED, DOWNVOTED
    }
    public enum Distinguished {
        NOT_DISTINGUISHED, MODERATOR, ADMIN, SPECIAL
    }

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String USER_AGENT = "User-Agent";
    public static final String CUSTOM_USER_AGENT = "android:com.winsonchiu.reader:v0.1 (by " +
            "/u/TheKeeperOfPie)";

    public static final String GIF = ".gif";
    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
    public static final String WEBP = ".webp";

    public static final String SELF = "self";
    public static final String DEFAULT = "default";
    public static final String NSFW = "nsfw";

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

    public static boolean resolveError(int code, Activity activity, ErrorListener listener) {
        if (code == 401) {
            try {
                fetchApplicationAccessToken(activity, listener);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public static void fetchApplicationAccessToken(Context appContext, final ErrorListener listener) throws JSONException {

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences
                (appContext);

        Log.d(TAG, "Expire time: " + preferences.getLong(AppSettings.EXPIRE_TIME, Long.MAX_VALUE));
        Log.d(TAG, "Current time: " + System.currentTimeMillis());

        if (preferences.getLong(AppSettings.EXPIRE_TIME, Long.MAX_VALUE) < System.currentTimeMillis() || preferences.getString(AppSettings.APP_ACCESS_TOKEN, "").equals("")) {

            if (preferences.getString(AppSettings.DEVICE_ID, "").equals("")) {
                preferences.edit().putString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()).commit();
            }

            final HashMap<String, String> params = new HashMap<>();
            params.put(REDIRECT_URI, "https://com.winsonchiu.reader");
            params.put(GRANT_TYPE, INSTALLED_CLIENT_GRANT);
            params.put(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID()
                    .toString()));
            params.put(DURATION, "permanent");

            RequestQueue requestQueue = Volley.newRequestQueue(appContext);

            requestQueue.add(new StringRequest(Request.Method.POST, APP_ONLY_URL, new
                    Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Log.d(TAG, "Response: " + response);
                        preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN,
                                jsonObject.getString(ACCESS_TOKEN)).commit();
                        preferences.edit().putLong(AppSettings.EXPIRE_TIME, System.currentTimeMillis() + jsonObject.getLong("expires_in") * SEC_TO_MS).commit();
                        if (listener != null) {
                            listener.onErrorHandled();
                        }
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

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    return params;
                }
            });


//            String client = String.format("%s:%s", "zo7k-Nsh7vgn-Q", "");
//            String auth = "Basic " + Base64.encodeToString(client.getBytes(), Base64.DEFAULT);
//
//            Ion.with(appContext)
//                    .load("POST", APP_ONLY_URL)
//                    .setLogging("ION", Log.VERBOSE)
//                    .addHeader(USER_AGENT, CUSTOM_USER_AGENT)
//                    .addHeader(Reddit.AUTHORIZATION, auth)
//                    .addHeader("Content-Type","application/json; charset=utf-8")
//                    .setBodyParameter(REDIRECT_URI, "https://com.winsonchiu.reader")
//                    .setBodyParameter(GRANT_TYPE, INSTALLED_CLIENT_GRANT)
//                    .setBodyParameter(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID
//                            .randomUUID()
//                            .toString()))
//                    .asString()
//                    .setCallback(new FutureReddit() {
//                        @Override
//                        public void onCompleted(Exception exception, JSONObject result) {
//                            try {
//                                Log.d(TAG, "Result: " + result);
//                                preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN, result.getString(ACCESS_TOKEN)).commit();
//                                preferences.edit().putLong(AppSettings.EXPIRE_TIME, System.currentTimeMillis() + result.getLong("expires_in") * SEC_TO_MS).commit();
//                            }
//                            catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });
        }
    }

    public interface ErrorListener {

        void onErrorHandled();

    }

}
