/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.winsonchiu.reader.data.reddit.Thing;

import javax.inject.Inject;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 * All Fragments for ActivityMain should extend this class.
 */
public abstract class FragmentBase extends Fragment {

    @Inject RequestManager requestManagerGlobal;
    RequestManager requestManagerLocal;

    private boolean injected;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!injected) {
            inject();
            injected = true;
        }
    }

    protected abstract void inject();

    protected RequestManager getGlideRequestManager() {
        if (requestManagerLocal == null && getActivity() != null) {
            requestManagerLocal = Glide.with(this);
        }

        if (requestManagerLocal != null) {
            return requestManagerLocal;
        }

        if (requestManagerGlobal != null) {
            return requestManagerGlobal;
        }

        return Glide.with(CustomApplication.getApplication());
    }

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
