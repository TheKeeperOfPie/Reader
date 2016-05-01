/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.theme.Themer;

/**
 * Created by TheKeeperOfPie on 4/23/2016.
 */
public class UtilsTheme {

    public static Toolbar generateToolbar(Context context, AppBarLayout layoutAppBar, Themer themer, FragmentListenerBase fragmentListenerBase) {
        int styleColorBackground = AppSettings.THEME_DARK.equals(fragmentListenerBase.getThemeBackground()) ? R.style.MenuDark : R.style.MenuLight;

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ContextThemeWrapper(context, UtilsColor.getThemeForColor(context.getResources(), themer.getColorPrimary(), fragmentListenerBase)), styleColorBackground);

        Toolbar toolbar = (Toolbar) LayoutInflater.from(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);

        layoutAppBar.addView(toolbar);

        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        toolbar.setTitleTextColor(themer.getColorFilterPrimary().getColor());

        return toolbar;
    }

}
