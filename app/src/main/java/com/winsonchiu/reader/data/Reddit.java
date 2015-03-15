package com.winsonchiu.reader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.winsonchiu.reader.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
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
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String USER_AGENT = "User-Agent";
    public static final String CUSTOM_USER_AGENT = "android:com.winsonchiu.reader:v0.1 (by " +
            "/u/TheKeeperOfPie)";

    public static final String GIFV = ".gifv";
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

    public static boolean needsToken(Context context, int iteration) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        return preferences.getLong(AppSettings.EXPIRE_TIME,
                Long.MAX_VALUE) < System.currentTimeMillis() || preferences.getString(
                AppSettings.APP_ACCESS_TOKEN, "")
                .equals("");

    }

    public static void fetchToken(Context context, final ErrorListener listener) {

        Ion.getDefault(context).configure().setLogging("ION", Log.VERBOSE);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        if (preferences.getString(AppSettings.DEVICE_ID, "").equals("")) {
            preferences.edit().putString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()).commit();
        }

        String creds = String.format("%s:%s", "zo7k-Nsh7vgn-Q", "");
        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);

        HashMap<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI, "https://com.winsonchiu.reader");
        params.put(GRANT_TYPE, INSTALLED_CLIENT_GRANT);
        params.put(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID()
                .toString()));

        Ion.with(context)
                .load("POST", APP_ONLY_URL)
                .userAgent(CUSTOM_USER_AGENT)
                .basicAuthentication("zo7k-Nsh7vgn-Q", "")
                .addHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8")
                .setBodyParameter(REDIRECT_URI, "https://com.winsonchiu.reader")
                .setBodyParameter(GRANT_TYPE, INSTALLED_CLIENT_GRANT)
                .setBodyParameter(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID,
                        UUID.randomUUID()
                                .toString()))
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        Log.d(TAG, "fetchToken response: " + result.getResult());
                        try {
                            JSONObject jsonObject = new JSONObject(result.getResult());
                            preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN, jsonObject.getString(ACCESS_TOKEN)).commit();
                            preferences.edit().putLong(AppSettings.EXPIRE_TIME, System.currentTimeMillis() + jsonObject.getLong("expires_in") * SEC_TO_MS).commit();
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                        if (listener != null) {
                            listener.onErrorHandled();
                        }
                    }
                });
    }

    public static void loadGet(final Context context, final String url, final FutureCallback<Response<String>> callback, final int iteration) {

        if (needsToken(context, iteration)) {
            fetchToken(context, new ErrorListener() {
                @Override
                public void onErrorHandled() {
                    loadGet(context, url, callback, iteration + 1);
                }
            });
            return;
        }

        Ion.with(context)
                .load("GET", url)
                .userAgent(CUSTOM_USER_AGENT)
                .addHeader(AUTHORIZATION, getAuthorizationHeader(context))
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .asString()
                .withResponse()
                .setCallback(callback);
    }

    private static String getAuthorizationHeader(Context context) {
        // TODO: Add check for user login token
        return Reddit.BEARER + PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getString(AppSettings
                .APP_ACCESS_TOKEN, "");
    }

    public static boolean checkIsImage(String url) {
        return url.endsWith(GIF) || url.endsWith(PNG) || url.endsWith(JPG)
                || url.endsWith(JPEG) || url.endsWith(WEBP);
    }

    public static Spannable formatHtml(String html, final UrlClickListener listener) {
        CharSequence sequence = Html.fromHtml(html);
        // Trims leading and trailing whitespace
        int start = 0;
        int end = sequence.length();
        while (start < end && Character.isWhitespace(sequence.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
            end--;
        }
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (final URLSpan span : urls) {
            int spanStart = strBuilder.getSpanStart(span);
            int spanEnd = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    listener.onUrlClick(span.getURL());
                }
            };
            strBuilder.setSpan(clickable, spanStart, spanEnd, flags);
            strBuilder.removeSpan(span);
        }
        return strBuilder;
    }

    public interface UrlClickListener {
        void onUrlClick(String url);
    }

    public interface ErrorListener {
        void onErrorHandled();
    }

}
