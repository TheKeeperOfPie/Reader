/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.theme;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.UtilsColor;

/**
 * Created by TheKeeperOfPie on 4/23/2016.
 */
public class Themer {

    private final int colorPrimary;
    private final int colorPrimaryDark;
    private final int colorAccent;
    private final int colorIconFilter;

    private final CustomColorFilter colorFilterPrimary;
    private final CustomColorFilter colorFilterAccent;

    public Themer(Context context) {
        Resources resources = context.getResources();
        Resources.Theme theme = context.getTheme();

        TypedArray typedArray = theme.obtainStyledAttributes(R.styleable.ThemeColors);
        colorPrimary = typedArray.getColor(R.styleable.ThemeColors_colorPrimary, 0);
        colorPrimaryDark = typedArray.getColor(R.styleable.ThemeColors_colorPrimaryDark, 0);
        colorAccent = typedArray.getColor(R.styleable.ThemeColors_colorAccent, 0);
        colorIconFilter = typedArray.getColor(R.styleable.ThemeColors_colorIconFilter, 0);
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceAccent = UtilsColor.showOnWhite(colorAccent) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new CustomColorFilter(resources.getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterAccent = new CustomColorFilter(resources.getColor(colorResourceAccent), PorterDuff.Mode.MULTIPLY);
    }

    public int getColorPrimary() {
        return colorPrimary;
    }

    public int getColorPrimaryDark() {
        return colorPrimaryDark;
    }

    public int getColorAccent() {
        return colorAccent;
    }

    public int getColorIconFilter() {
        return colorIconFilter;
    }

    public CustomColorFilter getColorFilterPrimary() {
        return colorFilterPrimary;
    }

    public CustomColorFilter getColorFilterAccent() {
        return colorFilterAccent;
    }
}
