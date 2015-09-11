/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Fragment;

import com.winsonchiu.reader.data.reddit.Thing;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 */
public abstract class FragmentBase extends Fragment {

    /**
     * Calculate whether or not to handle back action from Activity
     * @return true if back action should continue, false otherwise
     */
    public abstract boolean navigateBack();

    public void onShown() {

    }
    public void onWindowTransitionStart() {

    }
    public void setVisibilityOfThing(int visibility, Thing thing) {

    }

    public boolean shouldOverrideUrl(String urlString) {
        return false;
    }
}
