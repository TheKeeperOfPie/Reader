/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 6/5/2015.
 */
public enum Time {

    HOUR(R.id.item_sort_hour),
    DAY(R.id.item_sort_day),
    WEEK(R.id.item_sort_week),
    MONTH(R.id.item_sort_month),
    YEAR(R.id.item_sort_year),
    ALL(R.id.item_sort_all);

    private final int menuId;

    Time(int menuId) {
        this.menuId = menuId;
    }

    public int getMenuId() {
        return menuId;
    }

    public static Time fromMenuId(int id) {
        for (Time time : values()) {
            if (time.getMenuId() == id) {
                return time;
            }
        }

        return null;
    }

    // String returned is lowercase for use in URL and formatting
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
