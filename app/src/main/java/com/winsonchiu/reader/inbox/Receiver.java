/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static void checkInbox(final Context context, @NonNull final ArrayList<String> readNames) {

        Reddit.getInstance(context).loadGet(Reddit.OAUTH_URL + "/message/unread",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));

                            NotificationManager notificationManager =
                                    (NotificationManager) context.getSystemService(
                                            Context.NOTIFICATION_SERVICE);


                            Thing thing = null;

                            for (int index = 0; index < listing.getChildren().size(); index++) {
                                thing = listing.getChildren().get(index);
                                if (readNames.contains(thing.getName())) {
                                    Reddit.getInstance(context).markRead(thing.getName());
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

                            Intent intentActivity = new Intent(context, MainActivity.class);
                            intentActivity.putExtra(MainActivity.NAV_ID, R.id.item_inbox);
                            intentActivity.putExtra(MainActivity.NAV_PAGE, ControllerInbox.UNREAD);
                            PendingIntent pendingIntentActivity = PendingIntent
                                    .getActivity(context, 0, intentActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                            Intent intentMarkRead = new Intent(INTENT_INBOX);
                            intentMarkRead.putExtra(READ_NAMES, readNames);
                            PendingIntent pendingIntentMarkRead = PendingIntent
                                    .getBroadcast(context, 0, intentMarkRead, PendingIntent.FLAG_CANCEL_CURRENT);

                            int titleSuffixResource =
                                    listing.getChildren().size() == 1 ? R.string.new_message :
                                            R.string.new_messages;
                            CharSequence content = "";
                            CharSequence author = "";

                            if (thing instanceof Message) {
                                content = ((Message) thing).getBodyHtml();
                                author = ((Message) thing).getAuthor();

                            }
                            else if (thing instanceof Comment) {
                                content = ((Comment) thing).getBodyHtml();
                                author = ((Comment) thing).getAuthor();
                            }

                            Notification.Builder builder = new Notification.Builder(context)
                                    .setSmallIcon(R.mipmap.app_icon_notification)
                                    .setContentTitle(listing.getChildren().size() + " " + context
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
                                            pendingIntentMarkRead)
                                    .setAutoCancel(true);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                builder.setColor(
                                        context.getResources().getColor(R.color.colorPrimary));
                            }

                            notificationManager.notify(NOTIFICATION_INBOX, builder.build());

                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

}
