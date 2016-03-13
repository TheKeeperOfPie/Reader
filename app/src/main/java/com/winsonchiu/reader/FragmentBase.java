/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.winsonchiu.reader.dagger.components.ComponentMain;
import com.winsonchiu.reader.data.reddit.Thing;

import javax.inject.Inject;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 * All Fragments for ActivityMain should extend this class.
 */
public abstract class FragmentBase extends Fragment {

    @Inject RequestManager requestManagerGlobal;
    RequestManager requestManagerLocal;


    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        inject();
        return onCreateViewInternal(inflater, container, savedInstanceState);
    }

    protected abstract void inject();

    protected abstract View onCreateViewInternal(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

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
