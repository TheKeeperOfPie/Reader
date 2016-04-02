/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.UtilsReddit;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class FragmentDisplay extends FragmentPreferences {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_display);

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_GRID_THUMBNAIL_SIZE));
        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_THEME_BACKGROUND));
        findPreference(AppSettings.PREF_THEME_BACKGROUND).setOnPreferenceChangeListener(
                (preference, newValue) -> recreate());

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_THEME_PRIMARY));
        findPreference(AppSettings.PREF_THEME_PRIMARY).setOnPreferenceChangeListener(
                (preference, newValue) -> recreate());

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_THEME_PRIMARY_DARK));
        findPreference(AppSettings.PREF_THEME_PRIMARY_DARK).setOnPreferenceChangeListener(
                (preference, newValue) -> recreate());

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_THEME_ACCENT));
        findPreference(AppSettings.PREF_THEME_ACCENT).setOnPreferenceChangeListener(
                (preference, newValue) -> recreate());

        Preference preferenceHomeSubreddit = findPreference(AppSettings.PREF_HEADER_SUBREDDIT);

        preferenceHomeSubreddit.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    String summary = String.valueOf(newValue);

                    if (TextUtils.isEmpty(summary)) {
                        preference.setSummary(R.string.pref_header_subreddit_summary);
                    }
                    else {
                        preference.setSummary(getString(R.string.subreddit_formatted, UtilsReddit.parseRawSubredditString(summary)));
                    }

                    preferences.edit().putLong(AppSettings.HEADER_EXPIRATION, 0).apply();
                    return true;
                });
        preferenceHomeSubreddit.getOnPreferenceChangeListener().onPreferenceChange(preferenceHomeSubreddit, preferences.getString(AppSettings.PREF_HEADER_SUBREDDIT, ""));
    }

    private boolean recreate() {
        if (getActivity() != null) {
            getActivity().recreate();
        }

        return true;
    }

}
