/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.auth;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by TheKeeperOfPie on 4/16/2016.
 */
public class AuthReceiver extends WakefulBroadcastReceiver {

    public static final String INTENT_ACTION_AUTH_REFRESH = "com.winsonchiu.reader.INTENT_ACTION_AUTH_REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
//        Uri uri = intent.getData();
//
//        String error = uri.getQueryParameter("error");
//        String returnedState = uri.getQueryParameter("state");
////        if (!TextUtils.isEmpty(error) || !state.equals(returnedState)) {
////            Toast.makeText(ActivityLogin.this, error, Toast.LENGTH_LONG).show();
////            webAuth.loadUrl(Reddit.getUserAuthUrl(state));
////            return;
////        }
//        // TODO: Failsafe with error and state
//
//        String code = uri.getQueryParameter("code");
//
//        fetchTokens(code);
//        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(INTENT_ACTION_AUTH_REFRESH));
    }

//    private void fetchTokens(String code) {
//        RequestBody requestBody = new FormBody.Builder()
//                .add(Reddit.QUERY_GRANT_TYPE, Reddit.CODE_GRANT)
//                .add(Reddit.QUERY_CODE, code)
//                .add(Reddit.QUERY_REDIRECT_URI, Reddit.REDIRECT_URI)
//                .build();
//
//        Request request = Reddit.withRequestBasicAuth()
//                .url(Reddit.ACCESS_URL)
//                .post(requestBody)
//                .build();
//
//        reddit.load(request)
//                .subscribe(new FinalizingSubscriber<String>() {
//                    @Override
//                    public void next(String response) {
//                        try {
//                            JSONObject jsonObject = new JSONObject(response);
//
//                            String tokenAccess = jsonObject.getString(Reddit.QUERY_ACCESS_TOKEN);
//                            String tokenRefresh = jsonObject.getString(Reddit.QUERY_REFRESH_TOKEN);
//                            long timeExpire = System.currentTimeMillis() + jsonObject.getLong(
//                                    Reddit.QUERY_EXPIRES_IN) * Reddit.SEC_TO_MS;
//
//                            Log.d(TAG, "timeExpire in: " + jsonObject.getLong(Reddit.QUERY_EXPIRES_IN));
//
//                            loadAccountInfo(tokenAccess, tokenRefresh, timeExpire);
//                        }
//                        catch (JSONException e) {
//                            onError(e);
//                        }
//                    }
//
//                    @Override
//                    public void error(Throwable e) {
//                        Toast.makeText(ActivityLogin.this, R.string.error_logging_in, Toast.LENGTH_LONG).show();
//                    }
//                });
//    }
//
//    private void loadAccountInfo(final String tokenAuth, final String tokenRefresh, final long timeExpire) {
//        Request request = new Request.Builder()
//                .url(Reddit.OAUTH_URL + "/api/v1/me")
//                .header(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
//                .header(Reddit.AUTHORIZATION, Reddit.BEARER + tokenAuth)
//                .header(Reddit.CONTENT_TYPE, Reddit.CONTENT_TYPE_APP_JSON)
//                .get()
//                .build();
//
//        reddit.load(request)
//                .flatMap(UtilsRx.flatMapWrapError(response -> User.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class))))
//                .subscribe(new FinalizingSubscriber<User>() {
//                    @Override
//                    public void next(User user) {
//                        Account account = new Account(user.getName(), Reddit.ACCOUNT_TYPE);
//                        Intent result = new Intent();
//
//                        result.putExtra(AccountManager.KEY_ACCOUNT_NAME, user.getName());
//                        result.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Reddit.ACCOUNT_TYPE);
//                        result.putExtra(AccountManager.KEY_AUTHTOKEN, tokenAuth);
//
//                        if (getIntent().getBooleanExtra(KEY_IS_NEW_ACCOUNT, false)) {
//                            Bundle extras = new Bundle();
//                            extras.putString(KEY_TIME_EXPIRATION, String.valueOf(timeExpire));
//                            accountManager.addAccountExplicitly(account, tokenRefresh, extras);
//                            accountManager.setAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS, tokenAuth);
//                        }
//
//                        destroyWebView();
//
//                        setAccountAuthenticatorResult(result.getExtras());
//                        setResult(RESULT_OK, result);
//                        ActivityLogin.this.finish();
//                    }
//                });
//    }
}
