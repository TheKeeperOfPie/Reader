package com.winsonchiu.reader.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.Theme;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.utils.FinalizingSubscriber;
import com.winsonchiu.reader.utils.UtilsColor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class ActivityLogin extends AccountAuthenticatorActivity {

    private static final String TAG = ActivityLogin.class.getCanonicalName();
    public static final String KEY_IS_NEW_ACCOUNT = "isNewAccount";
    public static final String KEY_TIME_EXPIRATION = "timeExpiration";

    @Inject Reddit reddit;

    private SharedPreferences preferences;
    private Toolbar toolbar;
    private ProgressBar progressAuth;
    private WebView webAuth;
    private String state;
    private RelativeLayout layoutRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CustomApplication.getComponentMain().inject(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Theme theme;
        String themeAccent;

        if (sharedPreferences.getBoolean(AppSettings.SECRET, false)) {
            theme = Theme.random();
            themeAccent = AppSettings.randomThemeString();
        }
        else {
            theme = Theme.fromString(sharedPreferences.getString(AppSettings.PREF_THEME_PRIMARY, AppSettings.THEME_DEEP_PURPLE));
            themeAccent = sharedPreferences.getString(AppSettings.PREF_THEME_ACCENT, AppSettings.THEME_YELLOW);
        }
        if (theme != null) {
            String themeBackground = sharedPreferences.getString(AppSettings.PREF_THEME_BACKGROUND, AppSettings.THEME_DARK);

            setTheme(theme.getStyle(themeBackground, themeAccent));
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        state = UUID.randomUUID().toString();

        layoutRoot = (RelativeLayout) findViewById(R.id.layout_root);

        TypedArray typedArray = getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorAccent});
        int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));

        progressAuth = (ProgressBar) findViewById(R.id.progress_auth);
        webAuth = (WebView) findViewById(R.id.web_auth);
        webAuth.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressAuth.setIndeterminate(true);
                progressAuth.setVisibility(View.VISIBLE);
                Uri uri = Uri.parse(url);
                if (uri.getHost().equals(Reddit.REDIRECT_URI.replaceFirst("https://", ""))) {
                    String error = uri.getQueryParameter("error");
                    String returnedState = uri.getQueryParameter("state");
                    if (!TextUtils.isEmpty(error) || !state.equals(returnedState)) {
                        Toast.makeText(ActivityLogin.this, error, Toast.LENGTH_LONG).show();
                        webAuth.loadUrl(Reddit.getUserAuthUrl(state));
                        return;
                    }
                    // TODO: Failsafe with error and state

                    String code = uri.getQueryParameter("code");

                    fetchTokens(code);

                }

                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressAuth.setVisibility(View.GONE);
                toolbar.setTitle(view.getTitle());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView error: " + error);
            }

        });
        webAuth.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressAuth.setIndeterminate(false);
                progressAuth.setProgress(newProgress);
            }
        });
        webAuth.loadUrl(Reddit.getUserAuthUrl(state));

    }

    private void fetchTokens(String code) {
        RequestBody requestBody = new FormEncodingBuilder()
                .add(Reddit.QUERY_GRANT_TYPE, Reddit.CODE_GRANT)
                .add(Reddit.QUERY_CODE, code)
                .add(Reddit.QUERY_REDIRECT_URI, Reddit.REDIRECT_URI)
                .build();

        Request request = Reddit.withRequestBasicAuth()
                .url(Reddit.ACCESS_URL)
                .post(requestBody)
                .build();

        reddit.load(request)
                .subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void next(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            String tokenAccess = jsonObject.getString(Reddit.QUERY_ACCESS_TOKEN);
                            String tokenRefresh = jsonObject.getString(Reddit.QUERY_REFRESH_TOKEN);
                            long timeExpire = System.currentTimeMillis() + jsonObject.getLong(
                                    Reddit.QUERY_EXPIRES_IN) * Reddit.SEC_TO_MS;

                            Log.d(TAG, "timeExpire in: " + jsonObject.getLong(Reddit.QUERY_EXPIRES_IN));

                            loadAccountInfo(tokenAccess, tokenRefresh, timeExpire);
                        }
                        catch (JSONException e) {
                            onError(e);
                        }
                    }

                    @Override
                    public void error(Throwable e) {
                        Toast.makeText(ActivityLogin.this, R.string.error_logging_in, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadAccountInfo(final String tokenAuth, final String tokenRefresh, final long timeExpire) {
        Request request = new Request.Builder()
                .url(Reddit.OAUTH_URL + "/api/v1/me")
                .header(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
                .header(Reddit.AUTHORIZATION, Reddit.BEARER + tokenAuth)
                .header(Reddit.CONTENT_TYPE, Reddit.CONTENT_TYPE_APP_JSON)
                .get()
                .build();

        reddit.load(request)
                .flatMap(new Func1<String, Observable<User>>() {
                    @Override
                    public Observable<User> call(String response) {
                        try {
                            return Observable.just(User.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class)));
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribe(new FinalizingSubscriber<User>() {
                    @Override
                    public void next(User user) {
                        AccountManager accountManager = AccountManager.get(ActivityLogin.this);

                        Account account = new Account(user.getName(), Reddit.ACCOUNT_TYPE);
                        Intent result = new Intent();

                        result.putExtra(AccountManager.KEY_ACCOUNT_NAME, user.getName());
                        result.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Reddit.ACCOUNT_TYPE);
                        result.putExtra(AccountManager.KEY_AUTHTOKEN, tokenAuth);

                        if (getIntent().getBooleanExtra(KEY_IS_NEW_ACCOUNT, false)) {
                            Bundle extras = new Bundle();
                            extras.putString(KEY_TIME_EXPIRATION, String.valueOf(timeExpire));
                            accountManager.addAccountExplicitly(account, tokenRefresh, extras);
                            accountManager.setAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS, tokenAuth);
                        }

                        destroyWebView();

                        setAccountAuthenticatorResult(result.getExtras());
                        setResult(RESULT_OK, result);
                        ActivityLogin.this.finish();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        destroyWebView();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    private void destroyWebView() {
        if (webAuth != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().removeAllCookies(null);
            }
            else {
                CookieManager.getInstance().removeAllCookie();
            }
            webAuth.removeAllViews();
            webAuth.setWebChromeClient(null);
            webAuth.setWebViewClient(null);
            layoutRoot.removeView(webAuth);
            webAuth.destroy();
            webAuth = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
