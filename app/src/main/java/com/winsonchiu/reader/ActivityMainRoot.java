/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * Created by TheKeeperOfPie on 12/25/2015.
 */
public class ActivityMainRoot extends ActivityMain {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!isTaskRoot()) {
            Intent intent = getIntent();
            intent.setComponent(new ComponentName(getPackageName(), ActivityMain.class.getCanonicalName()));
            startActivity(intent);
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
    }
}
