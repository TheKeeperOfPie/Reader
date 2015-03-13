package com.winsonchiu.reader.data;

import android.text.TextUtils;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Response;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/10/2015.
 */
public abstract class FutureReddit implements FutureCallback<Response<String>> {

    private static final String TAG = FutureReddit.class.getCanonicalName();

    @Override
    public void onCompleted(Exception e, Response<String> result) {
        try {
            if (TextUtils.isEmpty(result.getResult())) {
                onCompleted(e, new JSONObject());
            }
            else {
                onCompleted(e, new JSONObject(result.getResult()));
            }
        }
        catch (JSONException e1) {
            e1.printStackTrace();
            onCompleted(e, new JSONObject());
        }
    }

    abstract public void onCompleted(Exception exception, JSONObject result);

}
