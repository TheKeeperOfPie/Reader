/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger;

import android.app.Fragment;
import android.os.Bundle;

import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.dagger.modules.ModuleReddit;

/**
 * Created by TheKeeperOfPie on 12/13/2015.
 */
public class FragmentPersist extends Fragment {

    public static final String TAG = FragmentPersist.class.getCanonicalName();

    private ComponentActivity componentActivity;

    public FragmentPersist() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void initialize() {
        componentActivity = CustomApplication.getComponentMain()
                .plus(new ModuleReddit());
    }

    public ComponentActivity getComponentActivity() {
        return componentActivity;
    }
}
