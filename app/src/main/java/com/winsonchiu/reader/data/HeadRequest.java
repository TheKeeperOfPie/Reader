package com.winsonchiu.reader.data;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by TheKeeperOfPie on 3/9/2015.
 */
public class HeadRequest extends Request<JSONObject> {

    private static final String TAG = HeadRequest.class.getCanonicalName();
    private final Response.Listener<JSONObject> listener;

    public HeadRequest(int method, String url, Response.Listener<JSONObject>listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String responseString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("response", responseString);

            JSONObject headers = new JSONObject();
            for (String key : response.headers.keySet()) {
                headers.put(key, response.headers.get(key));
            }
            jsonObject.put("headers", headers);
            Log.d(TAG, "Headers: " + headers);
            return Response.success(jsonObject,
                    HttpHeaderParser.parseCacheHeaders(response));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }


}
