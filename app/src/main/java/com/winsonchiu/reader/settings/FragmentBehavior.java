/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class FragmentBehavior extends FragmentPreferences
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_behavior);

        findPreference(AppSettings.PREF_CLEAR_HISTORY).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Historian.clear(activity);
                        Toast.makeText(activity, activity.getString(R.string.history_cleared),
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

        Preference preferenceHistorySize = findPreference(AppSettings.PREF_HISTORY_SIZE);

        preferenceHistorySize.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        preference.setSummary(String.valueOf(newValue) + " entries");
                        return true;
                    }
                });
        preferenceHistorySize.getOnPreferenceChangeListener().onPreferenceChange(preferenceHistorySize, preferences.getString(AppSettings.PREF_HISTORY_SIZE, "5000"));
    }

}
