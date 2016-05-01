/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.theme.Themer;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by TheKeeperOfPie on 6/25/2015.
 * All Fragments for ActivityMain should extend this class.
 */
public abstract class FragmentBase extends Fragment {

    @Inject RequestManager requestManagerGlobal;
    RequestManager requestManagerLocal;

    private boolean injected;
    private Unbinder unbinder;

    protected Themer themer;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        themer = new Themer(context);

        if (!injected) {
            inject();
            injected = true;
        }
    }

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    protected View bind(View view) {
        unbinder = ButterKnife.bind(this, view);
        return view;
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
