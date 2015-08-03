/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.os.Bundle;
import android.preference.Preference;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;

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
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                        return true;
                    }
                });
        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_THEME_PRIMARY));
        findPreference(AppSettings.PREF_THEME_PRIMARY).setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                        return true;
                    }
                });
        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_THEME_ACCENT));
        findPreference(AppSettings.PREF_THEME_ACCENT).setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (getActivity() != null){
                            getActivity().recreate();
                        }
                        return true;
                    }
                });
    }

}
