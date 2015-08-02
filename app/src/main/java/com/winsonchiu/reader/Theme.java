/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 8/2/2015.
 */
public enum Theme {

    THEME_RED(AppSettings.THEME_RED, R.style.AppRedDarkTheme, R.style.AppRedLightTheme, R.style.AppRedBlackTheme),
    THEME_PINK(AppSettings.THEME_PINK, R.style.AppPinkDarkTheme, R.style.AppPinkLightTheme, R.style.AppPinkBlackTheme),
    THEME_PURPLE(AppSettings.THEME_PURPLE, R.style.AppPurpleDarkTheme, R.style.AppPurpleLightTheme, R.style.AppPurpleBlackTheme),
    THEME_DEEP_PURPLE(AppSettings.THEME_DEEP_PURPLE, R.style.AppDeepPurpleDarkTheme, R.style.AppDeepPurpleLightTheme, R.style.AppDeepPurpleBlackTheme),
    THEME_INDIGO(AppSettings.THEME_INDIGO, R.style.AppIndigoDarkTheme, R.style.AppIndigoLightTheme, R.style.AppIndigoBlackTheme),
    THEME_BLUE(AppSettings.THEME_BLUE, R.style.AppBlueDarkTheme, R.style.AppBlueLightTheme, R.style.AppBlueBlackTheme),
    THEME_LIGHT_BLUE(AppSettings.THEME_LIGHT_BLUE, R.style.AppLightBlueDarkTheme, R.style.AppLightBlueLightTheme, R.style.AppLightBlueBlackTheme),
    THEME_CYAN(AppSettings.THEME_CYAN, R.style.AppCyanDarkTheme, R.style.AppCyanLightTheme, R.style.AppCyanBlackTheme),
    THEME_TEAL(AppSettings.THEME_TEAL, R.style.AppTealDarkTheme, R.style.AppTealLightTheme, R.style.AppTealBlackTheme),
    THEME_GREEN(AppSettings.THEME_GREEN, R.style.AppGreenDarkTheme, R.style.AppGreenLightTheme, R.style.AppGreenBlackTheme),
    THEME_LIGHT_GREEN(AppSettings.THEME_LIGHT_GREEN, R.style.AppLightGreenDarkTheme, R.style.AppLightGreenLightTheme, R.style.AppLightGreenBlackTheme),
    THEME_LIME(AppSettings.THEME_LIME, R.style.AppLimeDarkTheme, R.style.AppLimeLightTheme, R.style.AppLimeBlackTheme),
    THEME_YELLOW(AppSettings.THEME_YELLOW, R.style.AppYellowDarkTheme, R.style.AppYellowLightTheme, R.style.AppYellowBlackTheme),
    THEME_AMBER(AppSettings.THEME_AMBER, R.style.AppAmberDarkTheme, R.style.AppAmberLightTheme, R.style.AppAmberBlackTheme),
    THEME_ORANGE(AppSettings.THEME_ORANGE, R.style.AppOrangeDarkTheme, R.style.AppOrangeLightTheme, R.style.AppOrangeBlackTheme),
    THEME_DEEP_ORANGE(AppSettings.THEME_DEEP_ORANGE, R.style.AppDeepOrangeDarkTheme, R.style.AppDeepOrangeLightTheme, R.style.AppDeepOrangeBlackTheme),
    THEME_BROWN(AppSettings.THEME_BROWN, R.style.AppBrownDarkTheme, R.style.AppBrownLightTheme, R.style.AppBrownBlackTheme),
    THEME_GREY(AppSettings.THEME_GREY, R.style.AppGreyDarkTheme, R.style.AppGreyLightTheme, R.style.AppGreyBlackTheme),
    THEME_BLUE_GREY(AppSettings.THEME_BLUE_GREY, R.style.AppBlueGreyDarkTheme, R.style.AppBlueGreyLightTheme, R.style.AppBlueGreyBlackTheme);

    private final String name;
    private final int styleDark;
    private final int styleLight;
    private final int styleBlack;

    Theme(String name, int styleDark, int styleLight, int styleBlack) {
        this.name = name;
        this.styleDark = styleDark;
        this.styleLight = styleLight;
        this.styleBlack = styleBlack;
    }

    public String getName() {
        return name;
    }

    public static Theme fromString(String themeString) {
        for (Theme theme : values()) {
            if (theme.getName().equals(themeString)) {
                return theme;
            }
        }
        return null;
    }

    public int getStyle(String themePrimary) {
        switch (themePrimary) {
            case AppSettings.THEME_DARK:
                return styleDark;
            case AppSettings.THEME_LIGHT:
                return styleLight;
            case AppSettings.THEME_BLACK:
                return styleBlack;
        }
        return R.style.AppDarkTheme;
    }
}
