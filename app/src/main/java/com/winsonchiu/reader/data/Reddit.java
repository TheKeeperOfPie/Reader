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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.LruCacheBitmap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Reddit {

;

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
    public static final String CUSTOM_USER_AGENT = "android:com.winsonchiu.reader:" + BuildConfig.VERSION_NAME + " (by " +
            "/u/TheKeeperOfPie)";

    public static final String GFYCAT_URL = "http://gfycat.com/cajax/get/";
    public static final String GFYCAT_WEBM = "webmUrl";
    public static final String GFYCAT_ITEM = "gfyItem";

    private static final String IMGUR_ALBUM_URL = "https://api.imgur.com/3/album/";
    private static final String IMGUR_IMAGE_URL = "https://api.imgur.com/3/image/";

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
    private static final String EXPIRES_IN = "expires_in";

    private static Reddit reddit;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private SharedPreferences preferences;

    private Reddit(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        imageLoader = new ImageLoader(requestQueue, new LruCacheBitmap(context.getApplicationContext()));
        preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static Reddit getInstance(Context context) {
        if (reddit == null) {
            reddit = new Reddit(context);
        }
        return reddit;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public boolean needsToken() {
        return preferences.getLong(AppSettings.EXPIRE_TIME,
                Long.MAX_VALUE) < System.currentTimeMillis() || "".equals(preferences.getString(
                AppSettings.APP_ACCESS_TOKEN, ""));

    }

    public void fetchToken(final RedditErrorListener listener) {

        if ("".equals(preferences.getString(AppSettings.DEVICE_ID, ""))) {
            preferences.edit().putString(AppSettings.DEVICE_ID, UUID.randomUUID().toString()).commit();
        }

        final HashMap<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI, "https://com.winsonchiu.reader");
        params.put(GRANT_TYPE, INSTALLED_CLIENT_GRANT);
        params.put(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID()
                .toString()));

        requestQueue.add(new StringRequest(Request.Method.POST, APP_ONLY_URL,
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d(TAG, "fetchToken response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN, jsonObject.getString(ACCESS_TOKEN)).commit();
                            preferences.edit().putLong(AppSettings.EXPIRE_TIME,
                                    System.currentTimeMillis() + jsonObject.getLong(
                                            EXPIRES_IN) * SEC_TO_MS).commit();
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                        if (listener != null) {
                            listener.onErrorHandled();
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {

                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        return params;
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        HashMap<String, String> headers = new HashMap<>(3);
                        String creds = String.format("%s:%s", "zo7k-Nsh7vgn-Q", "");
                        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                        headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                        headers.put(AUTHORIZATION, auth);
                        headers.put("Content-Type", "application/json; charset=utf-8");
                        return headers;
                    }
                });

//        Ion.with(context)
//                .load("POST", APP_ONLY_URL)
//                .setLogging("ION", Log.VERBOSE)
//                .userAgent(CUSTOM_USER_AGENT)
//                .basicAuthentication("zo7k-Nsh7vgn-Q", "")
//                .addHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8")
//                .setBodyParameter(REDIRECT_URI, "https://com.winsonchiu.reader")
//                .setBodyParameter(GRANT_TYPE, INSTALLED_CLIENT_GRANT)
//                .setBodyParameter(DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID,
//                        UUID.randomUUID()
//                                .toString()))
//                .asString()
//                .withResponse()
//                .setCallback(new FutureCallback<Response<String>>() {
//                    @Override
//                    public void onCompleted(Exception e, Response<String> result) {
//                        if (result == null) {
//                            return;
//                        }
//
//                        Log.d(TAG, "fetchToken response: " + result.getResult());
//                        try {
//                            JSONObject jsonObject = new JSONObject(result.getResult());
//                            preferences.edit().putString(AppSettings.APP_ACCESS_TOKEN, jsonObject.getString(ACCESS_TOKEN)).commit();
//                            preferences.edit().putLong(AppSettings.EXPIRE_TIME,
//                                    System.currentTimeMillis() + jsonObject.getLong(
//                                            EXPIRES_IN) * SEC_TO_MS).commit();
//                        }
//                        catch (JSONException e1) {
//                            e1.printStackTrace();
//                        }
//
//                        if (listener != null) {
//                            listener.onErrorHandled();
//                        }
//                    }
//                });
    }

    /**
     * HTTP GET call to Reddit OAuth API, query parameters must exist inside url param
     *  @param url
     * @param listener
     * @param errorListener
     * @param iteration
     */
    public Request<String> loadGet(final String url, final Listener<String> listener, final ErrorListener errorListener, final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        if (needsToken()) {
            fetchToken(new RedditErrorListener() {
                @Override
                public void onErrorHandled() {
                    loadGet(url, listener, errorListener, iteration + 1);
                }
            });
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                listener, errorListener) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        HashMap<String, String> headers = new HashMap<>(3);
                        headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                        headers.put(AUTHORIZATION, getAuthorizationHeader());
                        headers.put("Content-Type", "application/json; charset=utf-8");
                        return headers;
                    }
                };

        return requestQueue.add(getRequest);
    }

    public Request<String> loadImgurImage(String id, Listener<String> listener, final ErrorListener errorListener, final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_IMAGE_URL + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    public Request<String> loadImgurAlbum(String id, Listener<String> listener, final ErrorListener errorListener, final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_ALBUM_URL + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    private String getAuthorizationHeader() {
        // TODO: Add check for user login token
        return Reddit.BEARER + preferences.getString(AppSettings.APP_ACCESS_TOKEN, "");
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

    /**
     * Sets link's URL to proper image format if applicable
     *
     * @param link to set URL
     * @return try if link is image file, false otherwise
     */
    public static boolean placeImageUrl(Link link) {

        String url = link.getUrl();
        if (!url.contains("http")) {
            url += "http://";
        }
        // TODO: Add support for popular image domains
        String domain = link.getDomain();
        if (domain.contains("imgur")) {
            if (url.endsWith(Reddit.GIFV)) {
                return false;
            }
            else if (url.contains(".com/a/") || url.contains(".com/gallery/")) {
                return false;
            }
            else if (!Reddit.checkIsImage(url)) {
                if (url.charAt(url.length() - 1) == '/') {
                    url = url.substring(0, url.length() - 2);
                }
                url += ".jpg";
            }
        }

        boolean isImage = Reddit.checkIsImage(url);
        if (!isImage) {
            return false;
        }

        link.setUrl(url);
        return true;
    }

    public static String getImageHtml(String src) {
        return "<html><head><meta name=\"viewport\" content=\"width=device-width, minimum-scale=0.1\"><style>img {width:100%;}</style></head><body style=\"margin: 0px;\"><img style=\"-webkit-user-select: none; cursor: zoom-in;\" src=\"" + src + "\"></body></html>";
    }

    public interface UrlClickListener {
        void onUrlClick(String url);
    }

    public interface RedditErrorListener {
        void onErrorHandled();
    }

}
