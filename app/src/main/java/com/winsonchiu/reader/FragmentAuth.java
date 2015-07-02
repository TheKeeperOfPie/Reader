package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

public class FragmentAuth extends FragmentBase {

    public static final String TAG = FragmentAuth.class.getCanonicalName();

    private Activity activity;
    private SharedPreferences preferences;
    private FragmentListenerBase mListener;
    private WebView webAuth;
    private Reddit reddit;
    private String state;
    private Toolbar toolbar;
    private CustomSwipeRefreshLayout swipeRefreshAuth;

    public static FragmentAuth newInstance() {
        FragmentAuth fragment = new FragmentAuth();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentAuth() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = UUID.randomUUID()
                .toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_auth, container, false);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle("Login");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });


        swipeRefreshAuth = (CustomSwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_auth);
        swipeRefreshAuth.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webAuth.reload();
            }
        });
        swipeRefreshAuth.setMinScrollY(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150,
                getResources().getDisplayMetrics()));

        webAuth = (WebView) view.findViewById(R.id.web_auth);
        webAuth.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                swipeRefreshAuth.setRefreshing(true);
                Uri uri = Uri.parse(url);
                if (uri.getHost()
                        .equals(Reddit.REDIRECT_URI.replaceFirst("https://", ""))) {
                    String error = uri.getQueryParameter("error");
                    String returnedState = uri.getQueryParameter("state");
                    if (!TextUtils.isEmpty(error) || !state.equals(returnedState)) {
                        destroy(false);
                        return;
                    }
                    // TODO: Failsafe with error and state
                    String code = uri.getQueryParameter("code");

                    HashMap<String, String> params = new HashMap<>(3);
                    params.put(Reddit.QUERY_GRANT_TYPE, Reddit.CODE_GRANT);
                    params.put(Reddit.QUERY_CODE, code);
                    params.put(Reddit.QUERY_REDIRECT_URI, Reddit.REDIRECT_URI);

                    reddit.loadPostDefault(Reddit.ACCESS_URL, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                preferences.edit()
                                        .putString(AppSettings.ACCESS_TOKEN,
                                                jsonObject.getString(Reddit.QUERY_ACCESS_TOKEN))
                                        .commit();
                                preferences.edit()
                                        .putString(AppSettings.REFRESH_TOKEN,
                                                jsonObject.getString(Reddit.QUERY_REFRESH_TOKEN))
                                        .commit();
                                preferences.edit()
                                        .putLong(AppSettings.EXPIRE_TIME,
                                                System.currentTimeMillis() + jsonObject.getLong(
                                                        Reddit.QUERY_EXPIRES_IN) * Reddit.SEC_TO_MS)
                                        .commit();
                                loadSubredditList();
                            }
                            catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }, params);

                }

                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshAuth.setRefreshing(false);
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
        webAuth.loadUrl(reddit.getUserAuthUrl(state));

        return view;
    }

    private void loadSubredditList() {
        // TODO: Support loading moderated, contributor, and multiple pages using after
        reddit.loadGet(Reddit.OAUTH_URL + "/api/v1/me",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "User onResponse: " + response);
                        preferences.edit()
                                .putString(AppSettings.ACCOUNT_JSON,
                                        response)
                                .commit();
                        destroy(true);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Check user info error
                        destroy(false);
                    }
                }, 0);
    }

    private void destroy(boolean success) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
        }
        else {
            CookieManager.getInstance().removeAllCookie();
        }
        webAuth.destroy();
        mListener.onAuthFinished(success);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        reddit = Reddit.getInstance(activity);
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        activity = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        webAuth.onResume();
    }

    @Override
    public void onPause() {
        webAuth.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    public boolean navigateBack() {
        if (webAuth.canGoBack()) {
            webAuth.goBack();
            return false;
        }
        destroy(false);
        return true;
    }

}
