package com.winsonchiu.reader.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivityLogin extends AccountAuthenticatorActivity {

    private static final String TAG = ActivityLogin.class.getCanonicalName();
    public static final String KEY_IS_NEW_ACCOUNT = "isNewAccount";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_TIME_EXPIRATION = "timeExpiration";
    private Reddit reddit;
    private SharedPreferences preferences;
    private Toolbar toolbar;
    private ProgressBar progressAuth;
    private WebView webAuth;
    private String state;
    private RelativeLayout layoutRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        reddit = Reddit.getInstance(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        state = UUID.randomUUID().toString();

        layoutRoot = (RelativeLayout) findViewById(R.id.layout_root);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
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
            public void onReceivedError(WebView view,
                                        int errorCode,
                                        String description,
                                        String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
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

        HashMap<String, String> params = new HashMap<>(3);
        params.put(Reddit.QUERY_GRANT_TYPE, Reddit.CODE_GRANT);
        params.put(Reddit.QUERY_CODE, code);
        params.put(Reddit.QUERY_REDIRECT_URI, Reddit.REDIRECT_URI);

        reddit.loadPostDefault(Reddit.ACCESS_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String tokenAccess = jsonObject.getString(Reddit.QUERY_ACCESS_TOKEN);
                    String tokenRefresh = jsonObject.getString(Reddit.QUERY_REFRESH_TOKEN);
                    long timeExpire = System.currentTimeMillis() + jsonObject.getLong(
                            Reddit.QUERY_EXPIRES_IN) * Reddit.SEC_TO_MS;

                    Log.d(TAG, "timeExpire in: " + jsonObject.getLong(Reddit.QUERY_EXPIRES_IN));

                    loadAccountInfo(tokenAccess, tokenRefresh, timeExpire);
                }
                catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: " + error);
            }
        }, params, 0);
    }

    private void loadAccountInfo(final String tokenAuth, final String tokenRefresh, final long timeExpire) {

        StringRequest getRequest = new StringRequest(Request.Method.GET, Reddit.OAUTH_URL + "/api/v1/me",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d(TAG, "User onResponse: " + response);
                        try {
                            User user = User.fromJson(Reddit.getObjectMapper().readValue(response, JsonNode.class));

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
                            finish();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "loadGet error: " + error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT);
                headers.put(Reddit.AUTHORIZATION, Reddit.BEARER + tokenAuth);
                headers.put(Reddit.CONTENT_TYPE, Reddit.CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        reddit.getRequestQueue().add(getRequest);
    }

    @Override
    public void onBackPressed() {
        destroyWebView();
        super.onBackPressed();
    }

    private void destroyWebView() {
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
