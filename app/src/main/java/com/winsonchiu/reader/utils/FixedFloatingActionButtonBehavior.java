/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by TheKeeperOfPie on 8/19/2016.
 */

public class FixedFloatingActionButtonBehavior extends FloatingActionButton.Behavior {

    public FixedFloatingActionButtonBehavior() {
        super();
    }

    public FixedFloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            // If we're depending on an AppBarLayout we will show/hide it automatically
            // if the FAB is anchored to the AppBarLayout
            super.onDependentViewChanged(parent, child, dependency);
        }

        return false;
    }
}
