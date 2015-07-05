/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Fragment;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 */
public abstract class FragmentBase extends Fragment {

    /**
     * Calculate whether or not to handle back action from Activity
     * @return true if back action should continue, false otherwise
     */
    abstract boolean navigateBack();
    public void onShown() {

    }
    void onWindowTransitionStart() {

    }
}
