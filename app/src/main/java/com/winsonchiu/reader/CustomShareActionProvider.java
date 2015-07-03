/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.Context;
import android.support.v7.widget.ShareActionProvider;
import android.view.View;

/**
 * Created by TheKeeperOfPie on 4/1/2015.
 */
public class CustomShareActionProvider extends ShareActionProvider {

    public CustomShareActionProvider(Context context) {
        super(context);
    }

    /*
        Return null to force menu item to use assigned icon.
        Looks prettier.
     */
    @Override
    public View onCreateActionView() {
        return null;
    }
}
