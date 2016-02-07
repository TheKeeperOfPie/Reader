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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.utils.ObserverEmpty;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by TheKeeperOfPie on 6/30/2015.
 */
public class Receiver extends BroadcastReceiver {

    public static final String INTENT_INBOX = "com.winsonchiu.reader.inbox.Receiver.INTENT_INBOX";
    public static final String READ_NAMES = "readNames";

    private static final int NOTIFICATION_INBOX = 0;
    private static final String TAG = Receiver.class.getCanonicalName();
    private static final int LED_MS_ON = 250;
    private static final int LED_MS_OFF = 250;

    @Inject OkHttpClient okHttpClient;
    @Inject Reddit reddit;
    @Inject AccountManager accountManager;

    public Receiver() {
        CustomApplication.getComponentMain().inject(this);
    }

    public static void setAlarm(Context context) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Intent intentInbox = new Intent(INTENT_INBOX);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentInbox, PendingIntent.FLAG_UPDATE_CURRENT);

        long interval = Long.parseLong(
                preferences.getString(AppSettings.PREF_INBOX_CHECK_INTERVAL, "1800000"));

        if (interval > 0) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
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

    public void checkInbox(final Context context, @Nullable final ArrayList<String> names) {

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
                final Listing messages = new Listing();

                Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);

                for (Account account : accounts) {

                    final AccountManagerFuture<Bundle> futureAuth = accountManager.getAuthToken(account, Reddit.AUTH_TOKEN_FULL_ACCESS, null, true, null, null);

                    try {
                        Bundle bundle = futureAuth.getResult();
                        final String tokenAuth = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                        Request request = new Request.Builder()
                                .url(Reddit.OAUTH_URL + "/message/unread")
                                .header(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
                                .header(Reddit.AUTHORIZATION, Reddit.BEARER + tokenAuth)
                                .header(Reddit.CONTENT_TYPE, Reddit.CONTENT_TYPE_APP_JSON)
                                .get()
                                .build();

                        String response = okHttpClient.newCall(request).execute().body().string();
                        Listing listing = Listing.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class));

                        messages.addChildren(listing.getChildren());

                        Log.d(TAG, account.name + " checkInbox response: " + response);

                    }
                    catch (OperationCanceledException | AuthenticatorException | IOException e) {
                        e.printStackTrace();
                    }

                }

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                Thing thing = null;

                for (int index = 0; index < messages.getChildren().size(); index++) {
                    thing = messages.getChildren().get(index);
                    if (readNames.contains(thing.getName())) {
                        reddit.markRead(thing.getName())
                                .subscribe(new ObserverEmpty<>());
                        thing = null;
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

                Intent intentActivity = new Intent(context, ActivityMain.class);
                intentActivity.putExtra(ActivityMain.ACCOUNT, dest);
                intentActivity.putExtra(ActivityMain.NAV_ID, R.id.item_inbox);
                intentActivity.putExtra(ActivityMain.NAV_PAGE, ControllerInbox.UNREAD);
                PendingIntent pendingIntentActivity = PendingIntent
                        .getActivity(context, 0, intentActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent intentRecheckInbox = new Intent(INTENT_INBOX);
                intentRecheckInbox.putExtra(READ_NAMES, readNames);
                PendingIntent pendingIntentRecheckInbox = PendingIntent
                        .getBroadcast(context, 0, intentRecheckInbox, PendingIntent.FLAG_CANCEL_CURRENT);

                TypedArray typedArray = context.obtainStyledAttributes(new int[]{R.attr.colorPrimary});
                int colorPrimary = typedArray.getColor(0, context.getResources().getColor(R.color.colorPrimary));
                typedArray.recycle();

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.app_icon_white_outline)
                        .setContentTitle(messages.getChildren().size() + " " + context
                                .getResources()
                                .getString(titleSuffixResource))
                        .setContentText(context.getString(R.string.expand_to_read_first_message))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setSummaryText(context.getString(R.string.from) + " /u/" + author)
                                .bigText(content))
                        .setContentIntent(pendingIntentActivity)
                        .addAction(new NotificationCompat.Action(
                                R.drawable.ic_check_white_24dp,
                                context.getString(R.string.mark_read),
                                pendingIntentRecheckInbox))
                        .setDeleteIntent(pendingIntentRecheckInbox)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_EMAIL)
                        .setColor(colorPrimary)
                        .setLights(colorPrimary, LED_MS_ON, LED_MS_OFF);

                notificationManager.notify(NOTIFICATION_INBOX, builder.build());

            }
        }).start();
    }

}
