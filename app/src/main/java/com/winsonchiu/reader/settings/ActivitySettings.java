/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.theme.ThemeColor;
import com.winsonchiu.reader.theme.Themer;
import com.winsonchiu.reader.utils.UtilsColor;

/**
 * Created by TheKeeperOfPie on 7/1/2015.
 */
public class ActivitySettings extends AppCompatActivity {

    private static final String TAG = ActivitySettings.class.getCanonicalName();
    private Toolbar toolbar;

    @Override
    public Resources.Theme getTheme() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean secret = sharedPreferences.getBoolean(AppSettings.SECRET, false);
        @AppSettings.ThemeBackground String themeBackground = sharedPreferences.getString(AppSettings.PREF_THEME_BACKGROUND, AppSettings.THEME_DARK);
        @AppSettings.ThemeColor String themePrimary = secret ? ThemeColor.random().getName() : sharedPreferences.getString(AppSettings.PREF_THEME_PRIMARY, AppSettings.THEME_DEEP_PURPLE);
        @AppSettings.ThemeColor String themePrimaryDark = secret ? ThemeColor.random().getName() : sharedPreferences.getString(AppSettings.PREF_THEME_PRIMARY_DARK, AppSettings.THEME_DEEP_PURPLE);
        @AppSettings.ThemeColor String themeAccent = secret ? ThemeColor.random().getName() : sharedPreferences.getString(AppSettings.PREF_THEME_ACCENT, AppSettings.THEME_YELLOW);

        Resources.Theme theme = getResources().newTheme();

        UtilsColor.applyTheme(theme,
                themeBackground,
                themePrimary,
                themePrimaryDark,
                themeAccent);

        return theme;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        Themer themer = new Themer(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.settings);
        toolbar.setTitleTextColor(themer.getColorFilterPrimary().getColor());
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.getNavigationIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
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