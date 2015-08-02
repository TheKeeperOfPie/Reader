/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.Theme;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class ActivitySettings extends AppCompatActivity {

    private static final String TAG = ActivitySettings.class.getCanonicalName();
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Theme theme = Theme.fromString(sharedPreferences.getString(AppSettings.PREF_THEME_ACCENT, AppSettings.THEME_DEEP_PURPLE));
        if (theme != null) {
            setTheme(theme.getStyle((sharedPreferences
                    .getString(AppSettings.PREF_THEME_PRIMARY, AppSettings.THEME_DARK))));
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        TypedArray typedArray = getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
        typedArray.recycle();

        PorterDuffColorFilter colorFilterIcon = new PorterDuffColorFilter(colorIconFilter,
                PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.settings);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterIcon);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.frame_fragment, FragmentHeaders.newInstance(), FragmentHeaders.TAG)
                    .commit();
        }
        else if (getFragmentManager().getBackStackEntryCount() > 0) {
            Fragment fragment = getFragmentManager().findFragmentByTag(FragmentHeaders.TAG);
            if (fragment != null) {
                getFragmentManager().beginTransaction().hide(fragment).commit();
            }
            setResult(Activity.RESULT_OK);
        }

    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();

            Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragment != null) {
                getFragmentManager().beginTransaction()
                        .show(fragment)
                        .commit();
            }

        }
        else {
            super.onBackPressed();
        }
    }
}