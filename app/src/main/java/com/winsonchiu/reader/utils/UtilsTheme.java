/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
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

        layoutAppBar.addView(toolbar, 0);

        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        toolbar.setTitleTextColor(themer.getColorFilterPrimary().getColor());

        return toolbar;
    }

    public static int getAttributeColor(Context context, @AttrRes int resourceTarget, @ColorInt int colorDefault) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{resourceTarget});
        int color = typedArray.getColor(0, colorDefault);
        typedArray.recycle();

        return color;
    }

    public static float getAttributeDimension(Context context, @AttrRes int resourceTarget, float valueDefault) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{resourceTarget});
        float dimension = typedArray.getDimension(0, valueDefault);
        typedArray.recycle();

        return dimension;
    }
}
