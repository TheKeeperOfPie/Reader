package com.winsonchiu.reader.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by TheKeeperOfPie on 8/6/2015.
 */
public class Authenticator extends AbstractAccountAuthenticator {

    private static final String TAG = Authenticator.class.getCanonicalName();
    private Context context;

    public Authenticator(Context context) {
        super(context);
        this.context = context;
        Log.d(TAG, "Authenticator created");
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {

        Intent intent = new Intent(context, ActivityLogin.class);
        intent.putExtra(ActivityLogin.KEY_IS_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        AccountManager accountManager = AccountManager.get(context);
        String tokenAuth = accountManager.peekAuthToken(account, authTokenType);
        long timeExpire;
        try {
            timeExpire = Long.parseLong(accountManager.getUserData(account, ActivityLogin.KEY_TIME_EXPIRATION));
        }
        catch (NumberFormatException e) {
            timeExpire = 0;
        }
        Log.d(TAG, "time: " + System.currentTimeMillis());
        Log.d(TAG, "timeExpire: " + timeExpire);

        if (TextUtils.isEmpty(tokenAuth)|| System.currentTimeMillis() > timeExpire) {
            try {
                RequestBody requestBody = new FormEncodingBuilder()
                        .add(Reddit.QUERY_GRANT_TYPE, Reddit.QUERY_REFRESH_TOKEN)
                        .add(Reddit.QUERY_REFRESH_TOKEN, accountManager.getPassword(account))
                        .build();

                Request request = Reddit.withRequestBasicAuth()
                        .url(Reddit.ACCESS_URL)
                        .post(requestBody)
                        .build();

                String responseNetwork = new OkHttpClient().newCall(request).execute().body().string();

                JSONObject jsonObject = new JSONObject(responseNetwork);
                tokenAuth = jsonObject.getString(Reddit.QUERY_ACCESS_TOKEN);
                timeExpire = System.currentTimeMillis() + jsonObject.getLong(
                        Reddit.QUERY_EXPIRES_IN) * Reddit.SEC_TO_MS;

                if (jsonObject.has(Reddit.QUERY_REFRESH_TOKEN)) {
                    accountManager.setPassword(account, jsonObject.getString(Reddit.QUERY_REFRESH_TOKEN));
                }
                accountManager.setUserData(account, ActivityLogin.KEY_TIME_EXPIRATION, String.valueOf(timeExpire));
            }
            catch (JSONException | IOException e) {
                e.printStackTrace();
            }

        }

        if (!TextUtils.isEmpty(tokenAuth)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, tokenAuth);
            result.putLong(ActivityLogin.KEY_TIME_EXPIRATION, timeExpire);
            Log.d(TAG, "getAuthToken() called with: " + "response = [" + response + "], account = [" + account + "], authTokenType = [" + authTokenType + "], options = [" + options + "]");
            return result;
        }

        Intent intent = new Intent(context, ActivityLogin.class);
        intent.putExtra(ActivityLogin.KEY_IS_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        Log.d(TAG, "getAuthToken() called with: " + "response = [" + response + "], account = [" + account + "], authTokenType = [" + authTokenType + "], options = [" + options + "]");
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return context.getString(R.string.auth_full_access);
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }

}
