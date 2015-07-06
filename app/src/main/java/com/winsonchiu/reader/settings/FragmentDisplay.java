/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.os.Bundle;

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
    }

}
