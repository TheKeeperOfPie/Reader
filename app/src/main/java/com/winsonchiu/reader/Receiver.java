/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

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
import android.text.format.DateUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 6/30/2015.
 */
public class Receiver extends BroadcastReceiver {

    public static final String INTENT_INBOX = "com.winsonchiu.reader.Receiver.INTENT_INBOX";

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

        if (interval == 0) {
            alarmManager.cancel(pendingIntent);
        }
        else {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, interval, pendingIntent);
        }

        Log.d(TAG, "setAlarm: " + interval);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.d(TAG, "onReceive");

        Reddit reddit = Reddit.getInstance(context);

        reddit.loadGet(Reddit.OAUTH_URL + "/message/unread", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Listing listing = Listing.fromJson(new JSONObject(response));

                    if (!listing.getChildren().isEmpty()) {

                        User user;

                        try {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            user = User.fromJson(new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
                        }
                        catch (JSONException e) {
                            user = new User();
                        }

                        Intent intentActivity = new Intent(context, MainActivity.class);
                        intentActivity.putExtra(MainActivity.NAV_PAGE, R.id.item_inbox);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentActivity, 0);


                        Notification.Builder builder = new Notification.Builder(context)
                                        .setSmallIcon(R.mipmap.app_icon_notification)
                                        .setContentTitle(
                                                listing.getChildren().size() + " " + context
                                                        .getResources()
                                                        .getString(R.string.new_messages))
                                        .setContentText("/u/" + user.getName() + "\n" + DateUtils
                                                .formatDateTime(context, System.currentTimeMillis(),
                                                        DateUtils.FORMAT_SHOW_TIME))
                                        .setContentIntent(pendingIntent);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder.setColor(context.getResources().getColor(R.color.colorPrimary));
                        }

                        NotificationManager notificationManager =
                                (NotificationManager) context.getSystemService(
                                        Context.NOTIFICATION_SERVICE);

                        notificationManager.notify(NOTIFICATION_INBOX, builder.build());

                    }
                }
                catch (JSONException e) {
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
