/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.theme;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;

import java.security.SecureRandom;

/**
 * Created by TheKeeperOfPie on 8/2/2015.
 */
public enum ThemeBackground {

    EMPTY("", R.style.Empty),

    DARK(AppSettings.THEME_DARK, R.style.AppDarkTheme),

    LIGHT(AppSettings.THEME_LIGHT, R.style.AppLightTheme),

    BLACK(AppSettings.THEME_BLACK, R.style.AppBlackTheme);

    private final String name;

    private final int styleBackground;

    ThemeBackground(String name,
            int styleBackground) {
        this.name = name;
        this.styleBackground = styleBackground;
    }

    public static ThemeBackground random() {
        return values()[new SecureRandom().nextInt(values().length)];
    }

    public static ThemeBackground getTheme(@AppSettings.ThemeBackground String themeBackground) {
        switch (themeBackground) {
            case AppSettings.THEME_DARK:
                return DARK;
            case AppSettings.THEME_LIGHT:
                return LIGHT;
            case AppSettings.THEME_BLACK:
                return BLACK;
        }

        return EMPTY;
    }

    public String getName() {
        return name;
    }

    public int getStyleBackground() {
        return styleBackground;
    }
}
