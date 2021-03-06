/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.UtilsTheme;

/**
 * Created by TheKeeperOfPie on 6/30/2015.
 */
public abstract class FragmentPreferences extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = FragmentPreferences.class.getCanonicalName();

    protected Activity activity;
    protected SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getPreferenceManager().getSharedPreferences();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        view.setBackgroundColor(UtilsTheme.getAttributeColor(getContext(), R.attr.colorScreenBackground, 0));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onPause();
    }

    protected void bindPreferenceListenerSummary(Preference preference) {
        preference.setOnPreferenceChangeListener(preferenceListenerSummary);

        String summary = getPreferenceScreen().getSharedPreferences().getString(preference.getKey(), null);

        if (!TextUtils.isEmpty(summary)) {
            preferenceListenerSummary.onPreferenceChange(preference, summary);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value. Android Studio generated code.
     */
    protected static Preference.OnPreferenceChangeListener preferenceListenerSummary = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

                Log.d(TAG, "onPreferenceChange() called with: " + "preference = [" + preference.getKey() + "], index = [" + index + "]");

            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

}
