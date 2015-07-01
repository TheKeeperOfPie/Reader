package com.winsonchiu.reader;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
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
    
    private Toolbar toolbar;
    private FragmentListenerBase mListener;
    private Activity activity;

    public static Fragment newInstance() {
        return new FragmentPreferences();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_INBOX_CHECK_INTERVAL));
        bindPreferenceListenerSummary(findPreference(AppSettings.PREF_GRID_THUMBNAIL_SIZE));

        findPreference("pref_logout").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

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
                        return true;
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preferences, container, false);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.settings);
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.openDrawer();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
        mListener = null;
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
