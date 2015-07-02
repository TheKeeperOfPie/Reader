package com.winsonchiu.reader;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class FragmentHelp extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_help);
    }
}
