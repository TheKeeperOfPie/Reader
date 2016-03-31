/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/**
 * ColorFilter used to access original color assigned at creation
 * 
 * Created by TheKeeperOfPie on 7/17/2015.
 */
public class CustomColorFilter extends PorterDuffColorFilter {

    private final int color;

    public CustomColorFilter(int color) {
        super(color, PorterDuff.Mode.SRC_IN);
        this.color = color;
    }

    public CustomColorFilter(int color, PorterDuff.Mode mode) {
        super(color, mode);
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
