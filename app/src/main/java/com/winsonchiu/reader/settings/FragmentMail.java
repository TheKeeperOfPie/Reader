/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.inbox.Receiver;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class FragmentMail extends FragmentPreferences {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_mail);

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_INBOX_CHECK_INTERVAL));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppSettings.PREF_INBOX_CHECK_INTERVAL)) {
            Receiver.setAlarm(activity);
        }
    }
}
