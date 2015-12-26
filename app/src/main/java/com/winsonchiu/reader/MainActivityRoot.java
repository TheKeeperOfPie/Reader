/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by TheKeeperOfPie on 12/25/2015.
 */
public class MainActivityRoot extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            intent.setComponent(new ComponentName(getPackageName(), MainActivity.class.getCanonicalName()));
            startActivity(intent);
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
    }
}
