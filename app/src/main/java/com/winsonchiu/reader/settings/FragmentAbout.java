/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class FragmentAbout extends FragmentPreferences {

    private Toast toast;
    private int countClicked = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_about);

        findPreference(AppSettings.PREF_VERSION).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (toast != null) {
                    toast.cancel();
                }

                if (countClicked > 5) {
                    countClicked--;
                }
                else if (countClicked > 0) {
                    toast = Toast.makeText(activity, getString(R.string.secret_prefix) + " " + countClicked-- + " " + getString(R.string.secret_suffix), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else if (countClicked-- == 0) {
                    preferences.edit().putBoolean(AppSettings.SECRET, !preferences.getBoolean(AppSettings.SECRET, false)).apply();
                    Toast.makeText(activity, getString(R.string.secret_toggled), Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });
    }

}
