package com.winsonchiu.reader;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by TheKeeperOfPie on 6/30/2015.
 */
public class FragmentPreferences extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = FragmentPreferences.class.getCanonicalName();

    private Activity activity;
    private SharedPreferences preferences;

    public static Fragment newInstance() {
        return new FragmentPreferences();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        preferences = getPreferenceManager().getSharedPreferences();

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_INBOX_CHECK_INTERVAL));
        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_GRID_THUMBNAIL_SIZE));

        findPreference("pref_logout").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.confirm_logout)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // TODO: Manually invalidate access token
                                                preferences.edit()
                                                        .putString(AppSettings.ACCESS_TOKEN, "")
                                                        .apply();
                                                preferences.edit()
                                                        .putString(AppSettings.REFRESH_TOKEN, "")
                                                        .apply();
                                                preferences.edit()
                                                        .putString(AppSettings.ACCOUNT_JSON, "")
                                                        .apply();
                                                preferences.edit()
                                                        .putString(AppSettings.SUBSCRIBED_SUBREDDITS, "")
                                                        .apply();
                                                Toast.makeText(activity, "Logged out", Toast.LENGTH_SHORT).show();
                                                activity.recreate();
                                            }
                                        })
                                .setNegativeButton(R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                .show();
                        return true;
                    }
                });

        findPreference(AppSettings.PREF_CLEAR_HISTORY).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AppSettings.clearHistory(preferences);
                        Toast.makeText(activity, "History cleared", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
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

    private void bindPreferenceListenerSummary(Preference preference) {

        preference.setOnPreferenceChangeListener(preferenceListenerSummary);
        preferenceListenerSummary.onPreferenceChange(preference,
                getPreferenceScreen().getSharedPreferences().getString(preference.getKey(), ""));

    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value. Android Studio generated code.
     */
    private static Preference.OnPreferenceChangeListener preferenceListenerSummary = new Preference.OnPreferenceChangeListener() {
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

            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppSettings.PREF_INBOX_CHECK_INTERVAL)) {
            Receiver.setAlarm(activity);
        }
    }
}
