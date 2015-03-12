package com.winsonchiu.reader.data;

import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/10/2015.
 */
public abstract class FutureReddit implements FutureCallback<String> {

    private static final String TAG = FutureReddit.class.getCanonicalName();

    @Override
    public void onCompleted(Exception e, String result) {
        try {
            Log.d(TAG, "FutureReddit result: " + result);
            onCompleted(e, new JSONObject(result));
        }
        catch (JSONException e1) {
            e1.printStackTrace();
            onCompleted(e, new JSONObject());
        }
    }

    abstract public void onCompleted(Exception exception, JSONObject result);

}
