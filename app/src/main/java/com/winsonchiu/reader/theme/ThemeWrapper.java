/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.theme;

/**
 * Created by TheKeeperOfPie on 4/2/2016.
 */

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.view.LayoutInflater;

/**
 * A ContextWrapper that allows you to modify the theme from what is in the
 * wrapped context. Needed because Theme constructor for {@link android.view.ContextThemeWrapper}
 * is API 23+.
 */
public class ThemeWrapper extends ContextWrapper {
    private Resources.Theme theme;
    private LayoutInflater mInflater;
    private Resources resources;

    public ThemeWrapper(Context base, Resources.Theme theme) {
        super(base);
        this.theme = theme;
    }

    @Override
    public Resources getResources() {
        if (resources != null) {
            return resources;
        }

        resources = super.getResources();
        return resources;
    }

    @Override public Resources.Theme getTheme() {
        return theme;
    }

    @Override public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
            }
            return mInflater;
        }
        return getBaseContext().getSystemService(name);
    }
}
