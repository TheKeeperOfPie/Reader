/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Thing;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by TheKeeperOfPie on 6/30/2015.
 */
public class Receiver extends BroadcastReceiver {

    public static final String INTENT_INBOX = "com.winsonchiu.reader.inbox.Receiver.INTENT_INBOX";
    public static final String READ_NAMES = "readNames";

    private static final int NOTIFICATION_INBOX = 0;
    private static final String TAG = Receiver.class.getCanonicalName();

    public static void setAlarm(Context context) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Intent intentInbox = new Intent(INTENT_INBOX);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentInbox, 0);

        long interval = Long.parseLong(
                preferences.getString(AppSettings.PREF_INBOX_CHECK_INTERVAL, "1800000"));

        alarmManager.cancel(pendingIntent);
        if (interval > 0) {
            alarmManager
                    .setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
                            interval, pendingIntent);
        }

        Log.d(TAG, "setAlarm: " + interval);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.d(TAG, "onReceive: " + intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
                setAlarm(context);
                break;
            case INTENT_INBOX:
                ArrayList<String> readNames = intent.getStringArrayListExtra(READ_NAMES);
                if (readNames == null) {
                    readNames = new ArrayList<>();
                }
                checkInbox(context, readNames);
                break;
        }


    }

    public static void checkInbox(final Context context, @Nullable final ArrayList<String> names) {

        final ArrayList<String> readNames;

        if (names == null) {
            readNames = new ArrayList<>();
        }
        else {
            readNames = names;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Reddit reddit = Reddit.getInstance(context);
                Listing messages = new Listing();
                AccountManager accountManager = AccountManager.get(context);

                Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);

                for (Account account : accounts) {

                    final AccountManagerFuture<Bundle> futureAuth = accountManager.getAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS, null, true, null, null);

                    try {
                        Bundle bundle = futureAuth.getResult();
                        final String tokenAuth = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                        RequestFuture<String> requestFuture = RequestFuture.newFuture();

                        StringRequest getRequest = new StringRequest(Request.Method.GET, Reddit.OAUTH_URL + "/message/unread", requestFuture, requestFuture) {
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

                        String response = requestFuture.get();

                        Log.d(TAG, account.name + " checkInbox response: " + response);

                        Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(response, JsonNode.class));

                        messages.addChildren(listing.getChildren());

                    }
                    catch (OperationCanceledException | AuthenticatorException | IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                }

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE);

                Thing thing = null;

                for (int index = 0; index < messages.getChildren().size(); index++) {
                    thing = messages.getChildren().get(index);
                    if (readNames.contains(thing.getName())) {
                        reddit.markRead(thing.getName());
                    }
                    else {
                        readNames.add(thing.getName());
                        break;
                    }
                }

                if (thing == null) {
                    notificationManager.cancel(NOTIFICATION_INBOX);
                    return;
                }

                int titleSuffixResource =
                        messages.getChildren().size() == 1 ? R.string.new_message :
                                R.string.new_messages;
                CharSequence content = "";
                CharSequence author = "";
                CharSequence dest = "";

                if (thing instanceof Message) {
                    content = ((Message) thing).getBodyHtml();
                    author = ((Message) thing).getAuthor();
                    dest = ((Message) thing).getDest();

                }
                else if (thing instanceof Comment) {
                    content = ((Comment) thing).getBodyHtml();
                    author = ((Comment) thing).getAuthor();
                    dest = ((Comment) thing).getDest();
                }

                Intent intentActivity = new Intent(context, MainActivity.class);
                intentActivity.putExtra(MainActivity.ACCOUNT, dest);
                intentActivity.putExtra(MainActivity.NAV_ID, R.id.item_inbox);
                intentActivity.putExtra(MainActivity.NAV_PAGE, ControllerInbox.UNREAD);
                PendingIntent pendingIntentActivity = PendingIntent
                        .getActivity(context, 0, intentActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent intentRecheckInbox = new Intent(INTENT_INBOX);
                intentRecheckInbox.putExtra(READ_NAMES, readNames);
                PendingIntent pendingIntentRecheckInbox = PendingIntent
                        .getBroadcast(context, 0, intentRecheckInbox, PendingIntent.FLAG_CANCEL_CURRENT);

                Notification.Builder builder = new Notification.Builder(context)
                        .setSmallIcon(R.mipmap.app_icon_white_outline)
                        .setContentTitle(messages.getChildren().size() + " " + context
                                .getResources()
                                .getString(titleSuffixResource))
                        .setContentText(
                                context.getString(
                                        R.string.expand_to_read_first_message))
                        .setStyle(new Notification.BigTextStyle().setSummaryText(
                                context.getString(R.string.from) + " /u/" + author)
                                .bigText(content))
                        .setContentIntent(pendingIntentActivity)
                        .addAction(R.drawable.ic_check_white_24dp,
                                context.getString(R.string.mark_read),
                                pendingIntentRecheckInbox)
                        .setDeleteIntent(pendingIntentRecheckInbox)
                        .setAutoCancel(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    TypedArray typedArray = context.obtainStyledAttributes(new int[] {R.attr.colorPrimary});
                    int colorPrimary = typedArray.getColor(0, context.getResources().getColor(R.color.colorPrimary));
                    typedArray.recycle();

                    builder.setColor(colorPrimary);
                }

                notificationManager.notify(NOTIFICATION_INBOX, builder.build());

            }
        }).start();
    }

}
