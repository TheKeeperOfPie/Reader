/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 4/3/2016.
 */
public class UtilsView {

    public static int getContentWidth(RecyclerView.LayoutManager layoutManager) {
        return layoutManager.getWidth() - layoutManager.getPaddingStart() - layoutManager.getPaddingEnd();
    }

    public static int getSpanCount(Context context) {
        Resources resources = context.getResources();

        int spanCount = 0;

        try {
            spanCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(AppSettings.PREF_GRID_COLUMNS, String.valueOf(0)));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (spanCount <= 0) {
            int columnThreshold = resources.getDimensionPixelSize(R.dimen.grid_column_width_threshold);
            int width = resources.getDisplayMetrics().widthPixels;
            int columns = width / columnThreshold;
            spanCount = Math.max(1, columns);
        }

        return spanCount;
    }
}
