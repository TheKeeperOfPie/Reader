package com.winsonchiu.reader.data;

import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.winsonchiu.reader.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 3/8/2015.
 */
public class RedditJsonRequest extends Request<JSONObject> {

    private static final String TAG = RedditJsonRequest.class.getCanonicalName();
    private final String USER_AGENT = "android:com.winsonchiu.reader:v0.1 (by /u/TheKeeperOfPie)";
    private final SharedPreferences preferences;
    private final HashMap<String, String> params;
    private final Response.Listener<JSONObject> listener;
    private NetworkResponse networkResponse;

    public RedditJsonRequest(SharedPreferences preferences, HashMap<String, String> params, int method, String url, final Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
        this.preferences = preferences;
        this.params = params;
        try {
            getBody();
        }
        catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
        }
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return params;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>(3);
        headers.put("User-agent", USER_AGENT);
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Authorization", "bearer " + preferences.getString(AppSettings.APP_ACCESS_TOKEN, ""));
        headers.put("after", params.get("after"));
        return headers;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        }
        catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
        catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }


    @Override
    public byte[] getBody() throws AuthFailureError {
        byte[] body = super.getBody();
        if (body != null && body.length > 0) {
            Log.d(TAG, "RedditJsonRequest body: " + new String(body));
        }
        return body;
    }
}
