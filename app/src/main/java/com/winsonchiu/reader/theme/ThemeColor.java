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
public enum ThemeColor {

    EMPTY("", R.style.Empty, R.style.Empty, R.style.Empty),

    RED(AppSettings.THEME_RED, R.style.ColorPrimaryRed, R.style.ColorPrimaryDarkRed, R.style.ColorAccentRed),

    PINK(AppSettings.THEME_PINK, R.style.ColorPrimaryPink, R.style.ColorPrimaryDarkPink, R.style.ColorAccentPink),

    PURPLE(AppSettings.THEME_PURPLE, R.style.ColorPrimaryPurple, R.style.ColorPrimaryDarkPurple, R.style.ColorAccentPurple),

    DEEP_PURPLE(AppSettings.THEME_DEEP_PURPLE, R.style.ColorPrimaryDeepPurple, R.style.ColorPrimaryDarkDeepPurple, R.style.ColorAccentDeepPurple),

    INDIGO(AppSettings.THEME_INDIGO, R.style.ColorPrimaryIndigo, R.style.ColorPrimaryDarkIndigo, R.style.ColorAccentIndigo),

    BLUE(AppSettings.THEME_BLUE, R.style.ColorPrimaryBlue, R.style.ColorPrimaryDarkBlue, R.style.ColorAccentBlue),

    LIGHT_BLUE(AppSettings.THEME_LIGHT_BLUE, R.style.ColorPrimaryLightBlue, R.style.ColorPrimaryDarkLightBlue, R.style.ColorAccentLightBlue),

    CYAN(AppSettings.THEME_CYAN, R.style.ColorPrimaryCyan, R.style.ColorPrimaryDarkCyan, R.style.ColorAccentCyan),

    TEAL(AppSettings.THEME_TEAL, R.style.ColorPrimaryTeal, R.style.ColorPrimaryDarkTeal, R.style.ColorAccentTeal),

    GREEN(AppSettings.THEME_GREEN, R.style.ColorPrimaryGreen, R.style.ColorPrimaryDarkGreen, R.style.ColorAccentGreen),

    LIGHT_GREEN(AppSettings.THEME_LIGHT_GREEN, R.style.ColorPrimaryLightGreen, R.style.ColorPrimaryDarkLightGreen, R.style.ColorAccentLightGreen),

    LIME(AppSettings.THEME_LIME, R.style.ColorPrimaryLime, R.style.ColorPrimaryDarkLime, R.style.ColorAccentLime),

    YELLOW(AppSettings.THEME_YELLOW, R.style.ColorPrimaryYellow, R.style.ColorPrimaryDarkYellow, R.style.ColorAccentYellow),

    AMBER(AppSettings.THEME_AMBER, R.style.ColorPrimaryAmber, R.style.ColorPrimaryDarkAmber, R.style.ColorAccentAmber),

    ORANGE(AppSettings.THEME_ORANGE, R.style.ColorPrimaryOrange, R.style.ColorPrimaryDarkOrange, R.style.ColorAccentOrange),

    DEEP_ORANGE(AppSettings.THEME_DEEP_ORANGE, R.style.ColorPrimaryDeepOrange, R.style.ColorPrimaryDarkDeepOrange, R.style.ColorAccentDeepOrange),

    BROWN(AppSettings.THEME_BROWN, R.style.ColorPrimaryBrown, R.style.ColorPrimaryDarkBrown, R.style.ColorAccentBrown),

    GREY(AppSettings.THEME_GREY, R.style.ColorPrimaryGrey, R.style.ColorPrimaryDarkGrey, R.style.ColorAccentGrey),

    BLUE_GREY(AppSettings.THEME_BLUE_GREY, R.style.ColorPrimaryBlueGrey, R.style.ColorPrimaryDarkBlueGrey, R.style.ColorAccentBlueGrey);

    private final String name;

    private final int styleColorPrimary;
    private final int styleColorPrimaryDark;
    private final int styleColorAccent;

    ThemeColor(String name,
            int styleColorPrimary,
            int styleColorPrimaryDark,
            int styleColorAccent) {
        this.name = name;
        this.styleColorPrimary = styleColorPrimary;
        this.styleColorPrimaryDark = styleColorPrimaryDark;
        this.styleColorAccent = styleColorAccent;
    }

    public static ThemeColor random() {
        return values()[new SecureRandom().nextInt(values().length)];
    }

    public static ThemeColor getTheme(@AppSettings.ThemeColor String themeColor) {
        switch (themeColor) {
            case AppSettings.THEME_RED:
                return RED;
            case AppSettings.THEME_PINK:
                return PINK;
            case AppSettings.THEME_PURPLE:
                return PURPLE;
            case AppSettings.THEME_DEEP_PURPLE:
                return DEEP_PURPLE;
            case AppSettings.THEME_INDIGO:
                return INDIGO;
            case AppSettings.THEME_BLUE:
                return BLUE;
            case AppSettings.THEME_LIGHT_BLUE:
                return LIGHT_BLUE;
            case AppSettings.THEME_CYAN:
                return CYAN;
            case AppSettings.THEME_TEAL:
                return TEAL;
            case AppSettings.THEME_GREEN:
                return GREEN;
            case AppSettings.THEME_LIGHT_GREEN:
                return LIGHT_GREEN;
            case AppSettings.THEME_LIME:
                return LIME;
            case AppSettings.THEME_YELLOW:
                return YELLOW;
            case AppSettings.THEME_AMBER:
                return AMBER;
            case AppSettings.THEME_ORANGE:
                return ORANGE;
            case AppSettings.THEME_DEEP_ORANGE:
                return DEEP_ORANGE;
            case AppSettings.THEME_BROWN:
                return BROWN;
            case AppSettings.THEME_GREY:
                return GREY;
            case AppSettings.THEME_BLUE_GREY:
                return BLUE_GREY;
        }

        return EMPTY;
    }

    public String getName() {
        return name;
    }

    public int getStyleColorPrimary() {
        return styleColorPrimary;
    }

    public int getStyleColorPrimaryDark() {
        return styleColorPrimaryDark;
    }

    public int getStyleColorAccent() {
        return styleColorAccent;
    }
}
