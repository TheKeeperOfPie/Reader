/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.support.v4.app.Fragment;

import com.winsonchiu.reader.data.reddit.Thing;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 * All Fragments for ActivityMain should extend this class.
 */
public abstract class FragmentBase extends Fragment {

    public void navigateBack() {

    }

    public boolean isFinished() {
        return true;
    }

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
