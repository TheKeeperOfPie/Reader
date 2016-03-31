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
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.EditText;

import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.auth.ActivityLogin;
import com.winsonchiu.reader.data.api.ApiRedditAuthorized;
import com.winsonchiu.reader.data.api.ApiRedditDefault;
import com.winsonchiu.reader.data.retrofit.ConverterFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
    public static final String INSTALLED_CLIENT_GRANT = Html.escapeHtml(Reddit.OAUTH_URL + "/grants/installed_client");
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

    public static final Transformer TRANSFORMER = new Transformer();

    private final OkHttpClient okHttpClientAuthorized;
    private final OkHttpClient okHttpClientDefault;
    private ApiRedditAuthorized apiRedditAuthorized;
    private ApiRedditDefault apiRedditDefault;
    private SharedPreferences preferences;
    private String tokenAuth;
    private Account account;
    private long timeExpire;

    @Inject OkHttpClient okHttpClient;
    @Inject AccountManager accountManager;

    public Reddit(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
        CustomApplication.getComponentMain().inject(this);

        OkHttpClient.Builder okHttpClientAuthorizedBuilder = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        getTokenBlocking();

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "tokenAuth: " + tokenAuth);
                        }

                        return response.request().newBuilder()
                                .header(AUTHORIZATION, getAuthorizationHeader())
                                .build();
                    }
                })
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request requestOriginal = chain.request();

                        Request requestModified = requestOriginal.newBuilder()
                                .header(USER_AGENT, CUSTOM_USER_AGENT)
                                .header(AUTHORIZATION, getAuthorizationHeader())
                                .header(CONTENT_TYPE, CONTENT_TYPE_APP_JSON)
                                .build();

                        return chain.proceed(requestModified);
                    }
                });

        OkHttpClient.Builder okHttpClientDefaultBuilder = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request requestOriginal = chain.request();

                        String credentials = ApiKeys.REDDIT_CLIENT_ID + ":";
                        String authorization = "Basic " + Base64.encodeToString(credentials.getBytes(),
                                Base64.NO_WRAP);

                        Request requestModified = requestOriginal.newBuilder()
                                .header(USER_AGENT, CUSTOM_USER_AGENT)
                                .header(AUTHORIZATION, authorization)
                                .header(CONTENT_TYPE, CONTENT_TYPE_APP_JSON)
                                .build();

                        return chain.proceed(requestModified);
                    }
                });

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientAuthorizedBuilder.addInterceptor(httpLoggingInterceptor);
            okHttpClientDefaultBuilder.addInterceptor(httpLoggingInterceptor);
        }

        okHttpClientAuthorized = okHttpClientAuthorizedBuilder.build();
        okHttpClientDefault = okHttpClientDefaultBuilder.build();

        apiRedditAuthorized = new Retrofit.Builder()
                .baseUrl(OAUTH_URL)
                .client(okHttpClientAuthorized)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(new ConverterFactory())
                .build()
                .create(ApiRedditAuthorized.class);

        apiRedditDefault = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClientDefault)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(new ConverterFactory())
                .build()
                .create(ApiRedditDefault.class);
    }

    public void clearAccount() {
        account = null;
        tokenAuth = null;
    }

    public void setAccount(Account accountUser) {
        clearAccount();
        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(accountUser.name)) {
                this.account = account;
                break;
            }
        }
    }

    public static Request.Builder withRequestBasicAuth() {
        String credentials = ApiKeys.REDDIT_CLIENT_ID + ":";
        String authorization = "Basic " + Base64.encodeToString(credentials.getBytes(),
                Base64.NO_WRAP);

        return new Request.Builder()
                .header(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
                .header(Reddit.AUTHORIZATION, authorization)
                .header(Reddit.CONTENT_TYPE, Reddit.CONTENT_TYPE_APP_JSON);
    }

    public boolean needsToken() {
        return System.currentTimeMillis() > timeExpire || TextUtils.isEmpty(tokenAuth);
    }

    public static String getUserAuthUrl(String state) {

        return USER_AUTHENTICATION_URL + QUERY_CLIENT_ID + "=" + ApiKeys.REDDIT_CLIENT_ID + "&response_type=code&state=" + state + "&" + QUERY_REDIRECT_URI + "=" + REDIRECT_URI + "&" + QUERY_DURATION + "=permanent&scope=" + AUTH_SCOPES;
    }

    public void getTokenBlocking() {
        if (account == null) {
            getApplicationWideTokenBlocking();
            return;
        }

        String token = accountManager.peekAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS);
        accountManager.invalidateAuthToken(Reddit.ACCOUNT_TYPE, token);
        accountManager.invalidateAuthToken(Reddit.ACCOUNT_TYPE, tokenAuth);
        final AccountManagerFuture<Bundle> future = accountManager.getAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS, null, true, null, null);

        try {
            Bundle bundle = future.getResult();
            tokenAuth = bundle.getString(AccountManager.KEY_AUTHTOKEN);

            try {
                timeExpire = Long.parseLong(accountManager.getUserData(account, ActivityLogin.KEY_TIME_EXPIRATION));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            e.printStackTrace();
        }

    }

    private void getApplicationWideTokenBlocking() {
        if ("".equals(preferences.getString(AppSettings.DEVICE_ID, ""))) {
            preferences.edit()
                    .putString(AppSettings.DEVICE_ID, UUID.randomUUID()
                            .toString())
                    .apply();
        }

        RequestBody requestBody = new FormBody.Builder()
                .add(QUERY_GRANT_TYPE, INSTALLED_CLIENT_GRANT)
                .add(QUERY_DEVICE_ID, preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID()
                        .toString()))
                .build();

        Request request = new Request.Builder()
                .url(ACCESS_URL)
                .post(requestBody)
                .build();

        try {
            String response = okHttpClientDefault.newCall(request).execute().body().string();

            // Check if an account was set while fetching a token
            if (account == null) {
                JSONObject jsonObject = new JSONObject(response);
                tokenAuth = jsonObject.getString(QUERY_ACCESS_TOKEN);
                timeExpire = System.currentTimeMillis() + jsonObject.getLong(
                        QUERY_EXPIRES_IN) * SEC_TO_MS;
            }

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public Observable<String> about(String pathSubreddit) {
        return apiRedditAuthorized.about(pathSubreddit + "about")
                .compose(TRANSFORMER);
    }

    public Observable<String> info(String id) {
        return apiRedditAuthorized.info(id)
                .compose(TRANSFORMER);
    }

    public Observable<String> links(final String pathSubreddit,
                                          String sort,
                                          final String time,
                                          final Integer limit,
                                          final String after) {
        String url;

        if (TextUtils.isEmpty(pathSubreddit)) {
            url = "/" + sort;
        }
        else {
            url = pathSubreddit + sort;
        }

        return apiRedditAuthorized.links(url, time, limit, after, "all")
                .compose(TRANSFORMER);
    }

    public Observable<Link> comments(String subreddit,
            String id,
            String comment,
            String sort,
            Boolean showMore,
            Boolean showEdits,
            Integer context,
            Integer depth,
            Integer limit) {
        if (TextUtils.isEmpty(subreddit)) {
            return apiRedditAuthorized.comments(id,
                    comment,
                    sort,
                    showMore,
                    showEdits,
                    context,
                    depth,
                    limit)
                    .compose(TRANSFORMER)
                    .flatMap(Link.COMMENTS);
        }

        return apiRedditAuthorized.comments(
                subreddit,
                id,
                comment,
                sort,
                showMore,
                showEdits,
                context,
                depth,
                limit)
                .compose(TRANSFORMER)
                .flatMap(Link.COMMENTS);
    }

    public Observable<String> moreChildren(String idLink, String children) {
        return apiRedditAuthorized.moreChildren(idLink, children)
                .compose(TRANSFORMER);
    }

    public Observable<String> message(String page, String after) {
        return apiRedditAuthorized.message(page, after)
                .compose(TRANSFORMER);
    }

    public Observable<String> user(String user, String page, String sort, String time, String after, Integer limit) {
        return apiRedditAuthorized.user(user, page, sort, time, after, limit)
                .compose(TRANSFORMER);
    }

    public Observable<String> subreddits(final String url, final String after, final Integer limit) {
        return apiRedditAuthorized.subreddits(url, after, limit, "all")
                .compose(TRANSFORMER);
    }

    public Observable<String> subredditsSearch(String query, String sort) {
        return apiRedditAuthorized.subredditsSearch(query, sort)
                .compose(TRANSFORMER);
    }

    public Observable<String> search(String pathSubreddit, String query, String sort, String time, String after, Boolean restrictSubreddit) {
        String url;

        if (TextUtils.isEmpty(pathSubreddit)) {
            url = "/search";
        }
        else {
            url = pathSubreddit + "search";
        }

        return apiRedditAuthorized.search(url, query, sort, time, after, restrictSubreddit)
                .compose(TRANSFORMER);
    }

    public Observable<String> voteLink(final Link link,
                                       int vote) {
        return apiRedditAuthorized.vote(link.getName(), vote)
                .compose(TRANSFORMER);
    }

    public Observable<String> sendComment(String name,
                                          String text) {

        return apiRedditAuthorized.comment(name, text)
                .compose(TRANSFORMER);
    }

    public Observable<String> voteComment(final Comment comment,
            int vote) {
        return apiRedditAuthorized.vote(comment.getName(), vote)
                .compose(TRANSFORMER);
    }

    public Observable<String> delete(Thing thing) {
        return apiRedditAuthorized.delete(thing.getName())
                .compose(TRANSFORMER);
    }

    public Observable<String> save(Thing thing, String category) {
        return apiRedditAuthorized.save(thing.getName(), category)
                .compose(TRANSFORMER);
    }

    public Observable<String> unsave(Thing thing) {
        return apiRedditAuthorized.unsave(thing.getName())
                .compose(TRANSFORMER);
    }

    public Observable<String> hide(Link link) {
        return apiRedditAuthorized.hide(link.getName())
                .compose(TRANSFORMER);
    }

    public Observable<String> unhide(Link link) {
        return apiRedditAuthorized.unhide(link.getName())
                .compose(TRANSFORMER);
    }

    public Observable<String> markNsfw(Link link) {
        return apiRedditAuthorized.markNsfw(link.getName())
                .compose(TRANSFORMER);
    }

    public Observable<String> unmarkNsfw(Link link) {
        return apiRedditAuthorized.unmarkNsfw(link.getName())
                .compose(TRANSFORMER);
    }

    public Observable<String> markRead(String name) {
        return apiRedditAuthorized.markRead(name)
                .compose(TRANSFORMER);
    }

    public static String parseUrlId(String url, String prefix, String suffix) {
        int startIndex = url.indexOf(prefix) + prefix.length();
        int slashIndex = url.substring(startIndex)
                .indexOf(suffix) + startIndex;
        int lastIndex = slashIndex > startIndex ? slashIndex : url.length();
        return url.substring(startIndex, lastIndex);
    }

    public Observable<String> load(final okhttp3.Request request) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.body() == null) {
                            subscriber.onError(new IOException("Response body null"));
                        }
                        else {
                            subscriber.onNext(response.body().string());
                            subscriber.onCompleted();
                        }
                    }
                });

            }
        })
                .compose(TRANSFORMER);
    }

    public Observable<String> loadImgurImage(String id) {
        return load(new okhttp3.Request.Builder()
                .url(IMGUR_URL_IMAGE + id)
                .header(USER_AGENT, CUSTOM_USER_AGENT)
                .header(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION)
                .header(CONTENT_TYPE, CONTENT_TYPE_APP_JSON)
                .get()
                .build());
    }

    public Observable<String> loadImgurAlbum(String id) {
        return load(new Request.Builder()
                .url(IMGUR_URL_ALBUM + id)
                .header(USER_AGENT, CUSTOM_USER_AGENT)
                .header(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION)
                .header(CONTENT_TYPE, CONTENT_TYPE_APP_JSON)
                .get()
                .build());
    }

    public Observable<String> loadImgurGallery(String id) {
        return load(new Request.Builder()
                .url(IMGUR_URL_GALLERY + id)
                .header(USER_AGENT, CUSTOM_USER_AGENT)
                .header(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION)
                .header(CONTENT_TYPE, CONTENT_TYPE_APP_JSON)
                .get()
                .build());
    }

    public Observable<String> loadGfycat(String id) {
        return load(new Request.Builder()
                .url(Reddit.GFYCAT_URL + id)
                .get()
                .build());
    }

    private String getAuthorizationHeader() {
        return Reddit.BEARER + tokenAuth;//preferences.getString(AppSettings.ACCESS_TOKEN, "");
    }

    public String tokenAuthBlocking() throws ExecutionException, InterruptedException {
        RequestBody requestBody = new FormBody.Builder()
                .add(QUERY_GRANT_TYPE, QUERY_REFRESH_TOKEN)
                .add(QUERY_REFRESH_TOKEN, accountManager.getPassword(account))
                .build();

        Request request = Reddit.withRequestBasicAuth()
                .url(Reddit.ACCESS_URL)
                .post(requestBody)
                .build();

        try {
            return okHttpClientDefault.newCall(request).execute().body().string();
        } catch (IOException e) {
            return null;
        }
    }

    public Observable<String> tokenAuth() {
        RequestBody requestBody = new FormBody.Builder()
                .add(QUERY_GRANT_TYPE, QUERY_REFRESH_TOKEN)
                .add(QUERY_REFRESH_TOKEN, accountManager.getPassword(account))
                .build();

        Request request = Reddit.withRequestBasicAuth()
                .url(Reddit.ACCESS_URL)
                .post(requestBody)
                .build();

        return load(request);
    }

    public Observable<String> tokenRevoke(String tokenType, String token) {
        RequestBody requestBody = new FormBody.Builder()
                .add("token_type_hint", tokenType)
                .add("token", token)
                .build();

        Request request = Reddit.withRequestBasicAuth()
                .url(Reddit.ACCESS_URL)
                .post(requestBody)
                .build();

        return load(request);
    }

    public Observable<String> me() {
        return apiRedditAuthorized.me()
                .compose(TRANSFORMER);
    }

    public Observable<String> needsCaptcha() {
        return apiRedditAuthorized.needsCaptcha()
                .compose(TRANSFORMER);
    }

    public Observable<String> newCaptcha() {
        return apiRedditAuthorized.newCaptcha()
                .compose(TRANSFORMER);
    }

    public Observable<String> subscribe(boolean subscribe, String subreddit) {
        return apiRedditAuthorized.subscribe(subscribe ? "sub" : "unsub", subreddit)
                .compose(TRANSFORMER);
    }

    public Observable<String> editUserText(String id, String text) {
        return apiRedditAuthorized.editUserText(id, text)
                .compose(TRANSFORMER);
    }

    public Observable<String> readAllMessages() {
        return apiRedditAuthorized.readAllMessages()
                .compose(TRANSFORMER);
    }

    public Observable<String> compose(String subject,
                                      String text,
                                      String recipient,
                                      String captchaId,
                                      String captchaText) {
        return apiRedditAuthorized.compose(subject,
                text,
                recipient,
                captchaId,
                captchaText)
                .compose(TRANSFORMER);
    }

    public Observable<String> submit(PostType postType,
                                     String subreddit,
                                     String title,
                                     String body,
                                     String captchaId,
                                     String captchaText) {
        String kind = null;
        String url = null;
        String text = null;

        switch (postType) {
            case LINK:
                url = body;
                kind = Reddit.POST_TYPE_LINK;
                break;
            case SELF:
                text = body;
                kind = Reddit.POST_TYPE_SELF;
                break;
        }

        return apiRedditAuthorized.submit(kind,
                subreddit,
                title,
                url,
                text,
                captchaId,
                captchaText)
                .compose(TRANSFORMER);

    }

    public Observable<String> report(String id, String reason, String otherReason) {
        return apiRedditAuthorized.report(id, reason, otherReason)
                .compose(TRANSFORMER);
    }

    public Observable<String> recommend(String subreddit, String omit) {
        return apiRedditAuthorized.recommend(subreddit, omit)
                .compose(TRANSFORMER);
    }

    public static void onMenuItemClickEditor(EditText editText, MenuItem menuItem, Resources resources) {

        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        boolean isMultipleSelected = selectionEnd != selectionStart;
        boolean isNewLine = editText.getText().length() == 0 || editText.getText().charAt(editText.length() - 1) == '\n';

        switch (menuItem.getItemId()) {
            case R.id.item_editor_italicize:
                if (isMultipleSelected) {
                    editText.getText().insert(selectionEnd, "*");
                    editText.getText().insert(selectionStart, "*");
                    setSelectionHelper(editText, selectionStart + 1, selectionEnd + 1);
                }
                else {
                    editText.getText().insert(selectionStart, "**");
                    setSelectionHelper(editText, selectionStart + 1);
                }
                break;
            case R.id.item_editor_bold:
                if (isMultipleSelected) {
                    editText.getText().insert(selectionEnd, "**");
                    editText.getText().insert(selectionStart, "**");
                    setSelectionHelper(editText, selectionStart + 2, selectionEnd + 2);
                }
                else {
                    editText.getText().insert(selectionStart, "****");
                    setSelectionHelper(editText, selectionStart + 2);
                }
                break;
            case R.id.item_editor_strikethrough:
                if (isMultipleSelected) {
                    editText.getText().insert(selectionEnd, "~~");
                    editText.getText().insert(selectionStart, "~~");
                    setSelectionHelper(editText, selectionStart + 2, selectionEnd + 2);
                }
                else {
                    editText.getText().insert(selectionStart, "~~~~");
                    setSelectionHelper(editText, selectionStart + 2);
                }
                break;
            case R.id.item_editor_quote:
                if (isMultipleSelected) {
                    editText.getText().insert(selectionStart, "> ");
                    setSelectionHelper(editText, selectionStart + 2, selectionEnd + 2);
                }
                else if (isNewLine) {
                    editText.getText().insert(selectionStart, "> ");
                    setSelectionHelper(editText, selectionStart + 2);
                }
                else {
                    editText.getText().insert(selectionStart, "\n> ");
                    setSelectionHelper(editText, selectionStart + 3);
                }
                break;
            case R.id.item_editor_link:
                String labelText = resources.getString(R.string.editor_label_text);
                String labelLink = resources.getString(R.string.editor_label_link);
                if (isMultipleSelected) {
                    if (URLUtil.isValidUrl(editText.getText().subSequence(selectionStart, selectionEnd).toString())) {
                        editText.getText().insert(selectionEnd, ")");
                        editText.getText().insert(selectionStart, "[" + labelText + "](");
                        setSelectionHelper(editText, selectionStart + 1, selectionStart + 1 + labelText.length());
                    }
                    else {
                        editText.getText().insert(selectionEnd, "](" + labelLink + ")");
                        editText.getText().insert(selectionStart, "[");
                        setSelectionHelper(editText, selectionEnd + 3, selectionEnd + 3 + labelLink.length());
                    }
                }
                else {
                    editText.getText().insert(selectionStart, "[" + labelText + "](" + labelLink + ")");
                    setSelectionHelper(editText, selectionStart + 1, selectionStart + 1 + labelText.length());
                }
                break;
            case R.id.item_editor_list_bulleted:
                if (isNewLine) {
                    editText.getText().insert(selectionStart, "* \n* \n* ");
                    setSelectionHelper(editText, selectionStart + 2);
                }
                else {
                    editText.getText().insert(selectionStart, "\n\n* \n* \n* ");
                    setSelectionHelper(editText, selectionStart + 4);
                }
                break;
            case R.id.item_editor_list_numbered:
                if (isNewLine) {
                    editText.getText().insert(selectionStart, "1. \n2. \n3. ");
                    setSelectionHelper(editText, selectionStart + 3);
                }
                else {
                    editText.getText().insert(selectionStart, "\n\n1. \n2. \n3. ");
                    setSelectionHelper(editText, selectionStart + 5);
                }
                break;
        }
    }


    /**
     * Helper method to ensure selection is valid (due to max length requirements)
     * @param editText
     * @param selectionStart
     */
    private static void setSelectionHelper(EditText editText, int selectionStart) {
        setSelectionHelper(editText, selectionStart, selectionStart);
    }

    /**
     * Helper method to ensure selection is valid (due to max length requirements)
     * @param editText
     * @param selectionStart
     * @param selectionEnd
     */
    private static void setSelectionHelper(EditText editText, int selectionStart, int selectionEnd) {

        if (selectionStart > editText.getText().length()) {
            selectionStart = editText.getText().length();
        }

        if (selectionEnd > editText.getText().length()) {
            selectionEnd = editText.getText().length();
        }

        editText.setSelection(selectionStart, selectionEnd);
    }

    public enum PostType {
        LINK,
        SELF;

        public static PostType fromString(String name) {
            switch (name) {
                case Reddit.POST_TYPE_LINK:
                    return LINK;
                case Reddit.POST_TYPE_SELF:
                    return SELF;
            }

            return null;
        }
    }

    public static class Transformer implements Observable.Transformer<String, String> {

        @Override
        public Observable<String> call(final Observable<String> stringObservable) {
            return stringObservable.subscribeOn(Schedulers.computation())
                    .unsubscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

}
