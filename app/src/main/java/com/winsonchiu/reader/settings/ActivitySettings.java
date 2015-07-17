/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class ActivitySettings extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (sharedPreferences.getString(AppSettings.PREF_THEME, "Dark")) {
            case AppSettings.THEME_DARK:
                setTheme(R.style.AppDarkTheme);
                break;
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.AppLightTheme);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.AppBlackTheme);
                break;
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

        getFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment, FragmentHeaders.newInstance())
                .commit();

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