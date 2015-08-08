/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.auth.ActivityLogin;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.utils.OkHttpStack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Reddit {

    private static AtomicInteger created = new AtomicInteger();
    private static AtomicInteger destroyed = new AtomicInteger();
    private static AtomicInteger bound = new AtomicInteger();
    private static AtomicInteger recycled = new AtomicInteger();

    /**
     * Used to count and log the creation of WebViews to track memory leaks
     */
    public static void incrementCreate() {
        Log.d(TAG, "Created: " + created.incrementAndGet());
        Log.d(TAG, "Destroyed: " + destroyed.get());
    }

    /**
     * Used to count and log the destruction of WebViews to track memory leaks
     */
    public static void incrementDestroy() {
        Log.d(TAG, "Created: " + created.get());
        Log.d(TAG, "Destroyed: " + destroyed.incrementAndGet());
    }

    /**
     * Used to count and log the binding of ViewHolders to track memory leaks
     */
    public static void incrementBind() {
        Log.d(TAG, "onBind: " + bound.incrementAndGet());
        Log.d(TAG, "onRecycled: " + recycled.get());
    }

    /**
     * Used to count and log the recycling of ViewHolders to track memory leaks
     */
    public static void incrementRecycled() {
        Log.d(TAG, "onBind: " + bound.get());
        Log.d(TAG, "onRecycled: " + recycled.incrementAndGet());
    }

    // Constant values to represent Thing states
    public enum Distinguished {
        NOT_DISTINGUISHED, MODERATOR, ADMIN, SPECIAL
    }

    public static final CharSequence NULL = "null";
    public static final String BASE_URL = "https://reddit.com";
    public static final String OAUTH_URL = "https://oauth.reddit.com";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_APP_JSON = "application/json; charset=utf-8";
    public static final String USER_AGENT = "User-Agent";
    public static final String CUSTOM_USER_AGENT = "android:com.winsonchiu.reader:" + BuildConfig.VERSION_NAME + " (by /u/TheKeeperOfPie)";
    public static final String REDIRECT_URI = "https://com.winsonchiu.reader";
    public static final String ACCOUNT_TYPE = "com.winsonchiu.reader.ACCOUNT_REDDIT";
    public static final String AUTH_TOKEN_FULL_ACCESS = "com.winsonchiu.reader.AUTH_FULL_ACCESS";

    public static final String GFYCAT_PREFIX = "gfycat.com/";
    public static final String GFYCAT_URL = "http://gfycat.com/cajax/get/";
    public static final String GFYCAT_WEBM = "webmUrl";
    public static final String GFYCAT_MP4 = "mp4Url";
    public static final String GFYCAT_ITEM = "gfyItem";

    public static final String IMGUR_PREFIX = "imgur.com/";
    public static final String IMGUR_PREFIX_ALBUM = "imgur.com/a/";
    public static final String IMGUR_PREFIX_GALLERY = "imgur.com/gallery/";

    private static final String IMGUR_URL_ALBUM = "https://api.imgur.com/3/album/";
    private static final String IMGUR_URL_GALLERY = "https://api.imgur.com/3/gallery/";
    private static final String IMGUR_URL_IMAGE = "https://api.imgur.com/3/image/";

    public static final String GIFV = ".gifv";
    public static final String GIF = ".gif";
    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
    public static final String WEBP = ".webp";

    public static final String DEFAULT = "default";
    public static final String NSFW = "nsfw";

    public static final String POST_TYPE_LINK = "Link";
    public static final String POST_TYPE_SELF = "Self";

    public static final String UTF_8 = "UTF-8";
    public static final String FRONT_PAGE = "Front Page";
    public static final String TIME_SEPARATOR = " - ";

    private static final String TAG = Reddit.class.getCanonicalName();

    public static final long SEC_TO_MS = 1000;

    // As a note, both of these URLs require the www. part to function
    public static final String ACCESS_URL = "https://www.reddit.com/api/v1/access_token";
    public static final String REVOKE_URL = "https://www.reddit.com/api/v1/revoke_token";

    private static final String USER_AUTHENTICATION_URL = "https://www.reddit.com/api/v1/authorize.compact?";
    private static final String AUTH_SCOPES = "account,creddits,edit,flair,history,identity,livemanage,modconfig,modflair,modlog,modothers,modposts,modself,modwiki,mysubreddits,privatemessages,read,report,save,submit,subscribe,vote,wikiedit,wikiread";

    // Query fields
    public static final String INSTALLED_CLIENT_GRANT = Reddit.OAUTH_URL + "/grants/installed_client";
    public static final String CODE_GRANT = "authorization_code";
    public static final String QUERY_CODE = "code";
    public static final String QUERY_CLIENT_ID = "client_id";
    public static final String QUERY_REDIRECT_URI = "redirect_uri";
    public static final String QUERY_ACCESS_TOKEN = "access_token";
    public static final String QUERY_REFRESH_TOKEN = "refresh_token";
    public static final String QUERY_GRANT_TYPE = "grant_type";
    public static final String QUERY_DEVICE_ID = "device_id";
    public static final String QUERY_DURATION = "duration";
    public static final String QUERY_EXPIRES_IN = "expires_in";
    public static final String QUERY_ID = "id";
    public static final String QUERY_VOTE = "dir";
    public static final String QUERY_CATEGORY = "category";

    private static Reddit reddit;
    private RequestQueue requestQueue;
    private SharedPreferences preferences;
    private AccountManager accountManager;
    private String tokenAuth;
    private Account account;
    private long timeExpire;

    private static Picasso picasso;
    private static ObjectMapper objectMapper;

    public static Picasso loadPicasso(Context context) {
        if (picasso == null) {
            picasso = new Picasso.Builder(context)
                    .downloader(new OkHttpDownloader(new OkHttpClient()))
                    .build();
        }
        return picasso;
    }

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return objectMapper;
    }

    public static Reddit getInstance(Context context) {
        if (reddit == null) {
            reddit = new Reddit(context);
        }
        return reddit;
    }

    private Reddit(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext(), new OkHttpStack());
        preferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
        accountManager = AccountManager.get(context.getApplicationContext());
    }

    private Request<String> fetchApplicationWideToken(final ErrorListener listener) {

        final HashMap<String, String> params = new HashMap<>(2);

        if ("".equals(preferences.getString(AppSettings.DEVICE_ID, ""))) {
            preferences.edit()
                    .putString(AppSettings.DEVICE_ID, UUID.randomUUID()
                            .toString())
                    .apply();
        }

        params.put(QUERY_GRANT_TYPE, INSTALLED_CLIENT_GRANT);
        params.put(QUERY_DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID()
                .toString()));

        return loadPostDefault(ACCESS_URL, new Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    // Check if an account was set while fetching a token
                    if (account == null) {
                        JSONObject jsonObject = new JSONObject(response);
                        tokenAuth = jsonObject.getString(QUERY_ACCESS_TOKEN);
                        timeExpire = System.currentTimeMillis() + jsonObject.getLong(
                                QUERY_EXPIRES_IN) * SEC_TO_MS;
                    }

                    if (listener != null) {
                        listener.onErrorHandled();
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, params);
    }

    public void clearAccount(ErrorListener errorListener) {
        account = null;
        fetchToken(errorListener);
    }

    public void setAccount(Account accountUser, ErrorListener errorListener) {
        boolean accountFound = false;
        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(accountUser.name)) {
                this.account = account;
                accountFound = true;
                fetchToken(errorListener);
                break;
            }
        }

        if (!accountFound) {
            account = null;
            fetchToken(errorListener);
        }
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public boolean needsToken() {

        Log.d(TAG, "needsToken: " + (System.currentTimeMillis() > timeExpire || TextUtils.isEmpty(tokenAuth)));

        return System.currentTimeMillis() > timeExpire || TextUtils.isEmpty(tokenAuth);
    }

    public static String getUserAuthUrl(String state) {

        return USER_AUTHENTICATION_URL + QUERY_CLIENT_ID + "=" + ApiKeys.REDDIT_CLIENT_ID + "&response_type=code&state=" + state + "&" + QUERY_REDIRECT_URI + "=" + REDIRECT_URI + "&" + QUERY_DURATION + "=permanent&scope=" + AUTH_SCOPES;
    }

    public void fetchToken(final ErrorListener listener) {

        if (account == null) {
            fetchApplicationWideToken(listener);
            return;
        }

        accountManager.invalidateAuthToken(Reddit.ACCOUNT_TYPE, tokenAuth);
        final AccountManagerFuture<Bundle> future = accountManager.getAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS, null, true, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bundle = future.getResult();
                    tokenAuth = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.d(TAG, "getAuthToken: " + tokenAuth);

                    try {
                        timeExpire = Long.parseLong(accountManager.getUserData(account, ActivityLogin.KEY_TIME_EXPIRATION));
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "time: " + System.currentTimeMillis());
                    Log.d(TAG, "timeExpire: " + timeExpire);

                    if (listener != null) {
                        listener.onErrorHandled();
                    }
                }
                catch (OperationCanceledException | IOException | AuthenticatorException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Request<String> loadPostDefault(final String url,
            @Nullable Listener<String> listener,
            @Nullable final com.android.volley.Response.ErrorListener errorListener,
            final Map<String, String> params) {

        if (listener == null) {
            // Volley can't handle a null Response.Listener, so for convenience, we check if it's null
            listener = new Listener<String>() {
                @Override
                public void onResponse(String response) {

                }
            };
        }

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                listener, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                String credentials = ApiKeys.REDDIT_CLIENT_ID + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(),
                        Base64.DEFAULT);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, auth);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(postRequest);
    }


    /**
     * HTTP POST call to Reddit OAuth API
     *
     * @param url
     * @param listener
     * @param errorListener
     * @param iteration
     */
    public Request<String> loadPost(final String url,
            @Nullable Listener<String> listener,
            @Nullable final com.android.volley.Response.ErrorListener errorListener,
            final Map<String, String> params,
            final int iteration) {

        if (listener == null) {
            // Volley can't handle a null Response.Listener, so for convenience, we check if it's null
            listener = new Listener<String>() {
                @Override
                public void onResponse(String response) {

                }
            };
        }
        final Listener<String> listenerFinal = listener;

        if (iteration > 2 && errorListener != null) {
            errorListener.onErrorResponse(null);
            return null;
        }

        if (needsToken()) {
            fetchToken(new ErrorListener() {
                @Override
                public void onErrorHandled() {
                    loadPost(url, listenerFinal, errorListener, params, iteration + 1);
                }
            });
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.POST, url,
                listener, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "loadPost error: " + error);
                if (errorListener != null) {
                    errorListener.onErrorResponse(error);
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, getAuthorizationHeader());
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);
    }

    /**
     * HTTP GET call to Reddit OAuth API, query parameters must exist inside url param
     *
     * @param url
     * @param listener
     * @param errorListener
     * @param iteration
     */
    public Request<String> loadGet(final String url,
            @Nullable Listener<String> listener,
            @Nullable final com.android.volley.Response.ErrorListener errorListener,
            final int iteration) {

        Log.d(TAG, "loadGet, tokenAuth: " + tokenAuth);

        if (listener == null) {
            // Volley can't handle a null Response.Listener, so for convenience, we check if it's null
            listener = new Listener<String>() {
                @Override
                public void onResponse(String response) {

                }
            };
        }
        final Listener<String> listenerFinal = listener;

        if (iteration > 2 && errorListener != null) {
            errorListener.onErrorResponse(null);
            return null;
        }

        if (needsToken()) {
            fetchToken(new ErrorListener() {
                @Override
                public void onErrorHandled() {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            loadGet(url, listenerFinal, errorListener, iteration + 1);
                        }
                    });
                }
            });
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                listener, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "loadGet error: " + error);
                if (errorListener != null) {
                    errorListener.onErrorResponse(error);
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, getAuthorizationHeader());
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);
    }

    public void voteLink(final RecyclerView.ViewHolder viewHolder,
            final Link link,
            int vote,
            final VoteResponseListener voteResponseListener) {
        final int position = viewHolder.getAdapterPosition();

        final int oldVote = link.getLikes();
        int newVote = 0;

        if (link.getLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, link.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        link.setScore(link.getScore() + newVote - link.getLikes());
        link.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            if (viewHolder instanceof AdapterLink.ViewHolderBase) {
                ((AdapterLink.ViewHolderBase) viewHolder).setVoteColors();
            }
        }
        final int finalNewVote = newVote;
        loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                voteResponseListener.onVoteFailed();
                link.setScore(link.getScore() - finalNewVote);
                link.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    if (viewHolder instanceof AdapterLink.ViewHolderBase) {
                        ((AdapterLink.ViewHolderBase) viewHolder).setVoteColors();
                    }
                }
            }
        }, params, 0);
    }

    public Request<String> sendComment(String name,
            String text,
            Listener<String> listener,
            com.android.volley.Response.ErrorListener errorListener) {
        Log.d(TAG, "sendComment");
        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("thing_id", name);
        params.put("text", text);

        return loadPost(Reddit.OAUTH_URL + "/api/comment", listener,
                errorListener, params, 0);
    }

    public boolean voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment,
            int vote,
            final VoteResponseListener voteResponseListener) {

        final int position = viewHolder.getAdapterPosition();
        final int oldVote = comment.getLikes();
        int newVote = 0;

        if (comment.getLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, comment.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        comment.setScore(comment.getScore() + newVote - comment.getLikes());
        comment.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            viewHolder.setVoteColors();
        }
        final int finalNewVote = newVote;
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                voteResponseListener.onVoteFailed();

                comment.setScore(comment.getScore() - finalNewVote);
                comment.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    viewHolder.setVoteColors();
                }
            }
        }, params, 0);
        return true;
    }

    public void save(Thing thing, String category, com.android.volley.Response.ErrorListener errorListener) {

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, thing.getName());
        if (!TextUtils.isEmpty(category)) {
            params.put(Reddit.QUERY_CATEGORY, category);
        }

        reddit.loadPost(Reddit.OAUTH_URL + "/api/save", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, errorListener, params, 0);
    }

    public void unsave(Thing thing) {
        HashMap<String, String> params = new HashMap<>(1);
        params.put(Reddit.QUERY_ID, thing.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/unsave", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }, params, 0);
    }

    public void hide(Link link) {

        HashMap<String, String> params = new HashMap<>(1);
        params.put(Reddit.QUERY_ID, link.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/hide", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }, params, 0);
    }

    public void unhide(Link link) {

        HashMap<String, String> params = new HashMap<>(1);
        params.put(Reddit.QUERY_ID, link.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/unhide", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }, params, 0);
    }


    public void markNsfw(Link link, com.android.volley.Response.ErrorListener errorListener) {

        HashMap<String, String> params = new HashMap<>(1);
        params.put(Reddit.QUERY_ID, link.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/marknsfw", null, errorListener, params, 0);
    }

    public void unmarkNsfw(Link link, com.android.volley.Response.ErrorListener errorListener) {

        HashMap<String, String> params = new HashMap<>(1);
        params.put(Reddit.QUERY_ID, link.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/unmarknsfw", null, errorListener, params, 0);
    }

    public void markRead(String name) {

        Map<String, String> params = new HashMap<>();
        params.put("id", name);

        loadPost(Reddit.OAUTH_URL + "/api/read_message", null, null, params, 0);
    }

    public static String parseUrlId(String url, String prefix, String suffix) {
        int startIndex = url.indexOf(prefix) + prefix.length();
        int slashIndex = url.substring(startIndex)
                .indexOf(suffix) + startIndex;
        int lastIndex = slashIndex > startIndex ? slashIndex : url.length();
        return url.substring(startIndex, lastIndex);
    }


    public static Drawable getDrawableForLink(Context context, Link link) {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return context.getResources().getDrawable(R.drawable.ic_chat_white_48dp);
        }

        if (Reddit.DEFAULT.equals(thumbnail)) {
            return context.getResources().getDrawable(R.drawable.ic_web_white_48dp);
        }

        return null;
    }

    public Request<String> loadImgurImage(String id,
            @NonNull Listener<String> listener,
            @Nullable final com.android.volley.Response.ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2 && errorListener != null) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_URL_IMAGE + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    public Request<String> loadImgurAlbum(String id,
            @NonNull Listener<String> listener,
            @Nullable final com.android.volley.Response.ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2 && errorListener != null) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_URL_ALBUM + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    public Request<String> loadImgurGallery(String id,
            @NonNull Listener<String> listener,
            @Nullable final com.android.volley.Response.ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2 && errorListener != null) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_URL_GALLERY + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    private String getAuthorizationHeader() {
        return Reddit.BEARER + tokenAuth;//preferences.getString(AppSettings.ACCESS_TOKEN, "");
    }

    public static boolean checkIsImage(String url) {
        return url.endsWith(GIF) || url.endsWith(PNG) || url.endsWith(JPG)
                || url.endsWith(JPEG) || url.endsWith(WEBP);
    }

    public static boolean showThumbnail(Link link) {
        if (TextUtils.isEmpty(link.getUrl())) {
            return false;
        }
        String domain = link.getDomain();
        return domain.contains("gfycat") || domain.contains("imgur") || Reddit.placeImageUrl(link);
    }

    /**
     * Sets link's URL to proper image format if applicable
     *
     * @param link to set URL
     * @return try if link is single image file, false otherwise
     */
    public static boolean placeImageUrl(Link link) {

        String url = link.getUrl();
        if (!url.contains("http")) {
            url += "http://";
        }
        // TODO: Add support for popular image domains
        String domain = link.getDomain();
        if (domain.contains("imgur")) {
            if (url.contains(",")) {
                return false;
            }
            else if (url.endsWith(Reddit.GIFV)) {
                return false;
            }
            else if (url.contains(".com/gallery")) {
                return false;
            }
            else if (url.contains(".com/a/")) {
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
        return "<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=0.1\">" +
                "<style>" +
                "    img {" +
                "        width:100%;" +
                "    }" +
                "    body {" +
                "        margin:0px;" +
                "    }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<img src=\"" + src + "\"/>" +
                "</body>" +
                "</html>";
    }

    public static CharSequence getFormattedHtml(String html) {

        if (TextUtils.isEmpty(html)) {
            return new SpannedString("");
        }

        html = html.replaceAll("\n", "<br>");

        CharSequence sequence = Html.fromHtml(Html.fromHtml(html).toString(), null, new TagHandlerReddit());

        // Trims leading and trailing whitespace
        int start = 0;
        int end = sequence.length();
        while (start < end && Character.isWhitespace(sequence.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
            end--;
        }


        return sequence.subSequence(start, end);
    }

    public interface ErrorListener {
        void onErrorHandled();
    }

    public interface VoteResponseListener {
        void onVoteFailed();
    }

}
