/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import java.util.Random;

/**
 * Created by TheKeeperOfPie on 8/2/2015.
 */
public enum Theme {
    // @formatter:off
    THEME_RED(AppSettings.THEME_RED,
            R.style.AppDarkRedRedTheme, R.style.AppLightRedRedTheme, R.style.AppBlackRedRedTheme,
            R.style.AppDarkRedPinkTheme, R.style.AppLightRedPinkTheme, R.style.AppBlackRedPinkTheme,
            R.style.AppDarkRedPurpleTheme, R.style.AppLightRedPurpleTheme, R.style.AppBlackRedPurpleTheme,
            R.style.AppDarkRedDeepPurpleTheme, R.style.AppLightRedDeepPurpleTheme, R.style.AppBlackRedDeepPurpleTheme,
            R.style.AppDarkRedIndigoTheme, R.style.AppLightRedIndigoTheme, R.style.AppBlackRedIndigoTheme,
            R.style.AppDarkRedBlueTheme, R.style.AppLightRedBlueTheme, R.style.AppBlackRedBlueTheme,
            R.style.AppDarkRedLightBlueTheme, R.style.AppLightRedLightBlueTheme, R.style.AppBlackRedLightBlueTheme,
            R.style.AppDarkRedCyanTheme, R.style.AppLightRedCyanTheme, R.style.AppBlackRedCyanTheme,
            R.style.AppDarkRedTealTheme, R.style.AppLightRedTealTheme, R.style.AppBlackRedTealTheme,
            R.style.AppDarkRedGreenTheme, R.style.AppLightRedGreenTheme, R.style.AppBlackRedGreenTheme,
            R.style.AppDarkRedLightGreenTheme, R.style.AppLightRedLightGreenTheme, R.style.AppBlackRedLightGreenTheme,
            R.style.AppDarkRedLimeTheme, R.style.AppLightRedLimeTheme, R.style.AppBlackRedLimeTheme,
            R.style.AppDarkRedYellowTheme, R.style.AppLightRedYellowTheme, R.style.AppBlackRedYellowTheme,
            R.style.AppDarkRedAmberTheme, R.style.AppLightRedAmberTheme, R.style.AppBlackRedAmberTheme,
            R.style.AppDarkRedOrangeTheme, R.style.AppLightRedOrangeTheme, R.style.AppBlackRedOrangeTheme,
            R.style.AppDarkRedDeepOrangeTheme, R.style.AppLightRedDeepOrangeTheme, R.style.AppBlackRedDeepOrangeTheme,
            R.style.AppDarkRedBrownTheme, R.style.AppLightRedBrownTheme, R.style.AppBlackRedBrownTheme,
            R.style.AppDarkRedGreyTheme, R.style.AppLightRedGreyTheme, R.style.AppBlackRedGreyTheme,
            R.style.AppDarkRedBlueGreyTheme, R.style.AppLightRedBlueGreyTheme, R.style.AppBlackRedBlueGreyTheme),


    THEME_PINK(AppSettings.THEME_PINK,
            R.style.AppDarkPinkRedTheme, R.style.AppLightPinkRedTheme, R.style.AppBlackPinkRedTheme,
            R.style.AppDarkPinkPinkTheme, R.style.AppLightPinkPinkTheme, R.style.AppBlackPinkPinkTheme,
            R.style.AppDarkPinkPurpleTheme, R.style.AppLightPinkPurpleTheme, R.style.AppBlackPinkPurpleTheme,
            R.style.AppDarkPinkDeepPurpleTheme, R.style.AppLightPinkDeepPurpleTheme, R.style.AppBlackPinkDeepPurpleTheme,
            R.style.AppDarkPinkIndigoTheme, R.style.AppLightPinkIndigoTheme, R.style.AppBlackPinkIndigoTheme,
            R.style.AppDarkPinkBlueTheme, R.style.AppLightPinkBlueTheme, R.style.AppBlackPinkBlueTheme,
            R.style.AppDarkPinkLightBlueTheme, R.style.AppLightPinkLightBlueTheme, R.style.AppBlackPinkLightBlueTheme,
            R.style.AppDarkPinkCyanTheme, R.style.AppLightPinkCyanTheme, R.style.AppBlackPinkCyanTheme,
            R.style.AppDarkPinkTealTheme, R.style.AppLightPinkTealTheme, R.style.AppBlackPinkTealTheme,
            R.style.AppDarkPinkGreenTheme, R.style.AppLightPinkGreenTheme, R.style.AppBlackPinkGreenTheme,
            R.style.AppDarkPinkLightGreenTheme, R.style.AppLightPinkLightGreenTheme, R.style.AppBlackPinkLightGreenTheme,
            R.style.AppDarkPinkLimeTheme, R.style.AppLightPinkLimeTheme, R.style.AppBlackPinkLimeTheme,
            R.style.AppDarkPinkYellowTheme, R.style.AppLightPinkYellowTheme, R.style.AppBlackPinkYellowTheme,
            R.style.AppDarkPinkAmberTheme, R.style.AppLightPinkAmberTheme, R.style.AppBlackPinkAmberTheme,
            R.style.AppDarkPinkOrangeTheme, R.style.AppLightPinkOrangeTheme, R.style.AppBlackPinkOrangeTheme,
            R.style.AppDarkPinkDeepOrangeTheme, R.style.AppLightPinkDeepOrangeTheme, R.style.AppBlackPinkDeepOrangeTheme,
            R.style.AppDarkPinkBrownTheme, R.style.AppLightPinkBrownTheme, R.style.AppBlackPinkBrownTheme,
            R.style.AppDarkPinkGreyTheme, R.style.AppLightPinkGreyTheme, R.style.AppBlackPinkGreyTheme,
            R.style.AppDarkPinkBlueGreyTheme, R.style.AppLightPinkBlueGreyTheme, R.style.AppBlackPinkBlueGreyTheme),

    THEME_PURPLE(AppSettings.THEME_PURPLE,
            R.style.AppDarkPurpleRedTheme, R.style.AppLightPurpleRedTheme, R.style.AppBlackPurpleRedTheme,
            R.style.AppDarkPurplePinkTheme, R.style.AppLightPurplePinkTheme, R.style.AppBlackPurplePinkTheme,
            R.style.AppDarkPurplePurpleTheme, R.style.AppLightPurplePurpleTheme, R.style.AppBlackPurplePurpleTheme,
            R.style.AppDarkPurpleDeepPurpleTheme, R.style.AppLightPurpleDeepPurpleTheme, R.style.AppBlackPurpleDeepPurpleTheme,
            R.style.AppDarkPurpleIndigoTheme, R.style.AppLightPurpleIndigoTheme, R.style.AppBlackPurpleIndigoTheme,
            R.style.AppDarkPurpleBlueTheme, R.style.AppLightPurpleBlueTheme, R.style.AppBlackPurpleBlueTheme,
            R.style.AppDarkPurpleLightBlueTheme, R.style.AppLightPurpleLightBlueTheme, R.style.AppBlackPurpleLightBlueTheme,
            R.style.AppDarkPurpleCyanTheme, R.style.AppLightPurpleCyanTheme, R.style.AppBlackPurpleCyanTheme,
            R.style.AppDarkPurpleTealTheme, R.style.AppLightPurpleTealTheme, R.style.AppBlackPurpleTealTheme,
            R.style.AppDarkPurpleGreenTheme, R.style.AppLightPurpleGreenTheme, R.style.AppBlackPurpleGreenTheme,
            R.style.AppDarkPurpleLightGreenTheme, R.style.AppLightPurpleLightGreenTheme, R.style.AppBlackPurpleLightGreenTheme,
            R.style.AppDarkPurpleLimeTheme, R.style.AppLightPurpleLimeTheme, R.style.AppBlackPurpleLimeTheme,
            R.style.AppDarkPurpleYellowTheme, R.style.AppLightPurpleYellowTheme, R.style.AppBlackPurpleYellowTheme,
            R.style.AppDarkPurpleAmberTheme, R.style.AppLightPurpleAmberTheme, R.style.AppBlackPurpleAmberTheme,
            R.style.AppDarkPurpleOrangeTheme, R.style.AppLightPurpleOrangeTheme, R.style.AppBlackPurpleOrangeTheme,
            R.style.AppDarkPurpleDeepOrangeTheme, R.style.AppLightPurpleDeepOrangeTheme, R.style.AppBlackPurpleDeepOrangeTheme,
            R.style.AppDarkPurpleBrownTheme, R.style.AppLightPurpleBrownTheme, R.style.AppBlackPurpleBrownTheme,
            R.style.AppDarkPurpleGreyTheme, R.style.AppLightPurpleGreyTheme, R.style.AppBlackPurpleGreyTheme,
            R.style.AppDarkPurpleBlueGreyTheme, R.style.AppLightPurpleBlueGreyTheme, R.style.AppBlackPurpleBlueGreyTheme),


    THEME_DEEP_PURPLE(AppSettings.THEME_DEEP_PURPLE,
            R.style.AppDarkDeepPurpleRedTheme, R.style.AppLightDeepPurpleRedTheme, R.style.AppBlackDeepPurpleRedTheme,
            R.style.AppDarkDeepPurplePinkTheme, R.style.AppLightDeepPurplePinkTheme, R.style.AppBlackDeepPurplePinkTheme,
            R.style.AppDarkDeepPurplePurpleTheme, R.style.AppLightDeepPurplePurpleTheme, R.style.AppBlackDeepPurplePurpleTheme,
            R.style.AppDarkDeepPurpleDeepPurpleTheme, R.style.AppLightDeepPurpleDeepPurpleTheme, R.style.AppBlackDeepPurpleDeepPurpleTheme,
            R.style.AppDarkDeepPurpleIndigoTheme, R.style.AppLightDeepPurpleIndigoTheme, R.style.AppBlackDeepPurpleIndigoTheme,
            R.style.AppDarkDeepPurpleBlueTheme, R.style.AppLightDeepPurpleBlueTheme, R.style.AppBlackDeepPurpleBlueTheme,
            R.style.AppDarkDeepPurpleLightBlueTheme, R.style.AppLightDeepPurpleLightBlueTheme, R.style.AppBlackDeepPurpleLightBlueTheme,
            R.style.AppDarkDeepPurpleCyanTheme, R.style.AppLightDeepPurpleCyanTheme, R.style.AppBlackDeepPurpleCyanTheme,
            R.style.AppDarkDeepPurpleTealTheme, R.style.AppLightDeepPurpleTealTheme, R.style.AppBlackDeepPurpleTealTheme,
            R.style.AppDarkDeepPurpleGreenTheme, R.style.AppLightDeepPurpleGreenTheme, R.style.AppBlackDeepPurpleGreenTheme,
            R.style.AppDarkDeepPurpleLightGreenTheme, R.style.AppLightDeepPurpleLightGreenTheme, R.style.AppBlackDeepPurpleLightGreenTheme,
            R.style.AppDarkDeepPurpleLimeTheme, R.style.AppLightDeepPurpleLimeTheme, R.style.AppBlackDeepPurpleLimeTheme,
            R.style.AppDarkDeepPurpleYellowTheme, R.style.AppLightDeepPurpleYellowTheme, R.style.AppBlackDeepPurpleYellowTheme,
            R.style.AppDarkDeepPurpleAmberTheme, R.style.AppLightDeepPurpleAmberTheme, R.style.AppBlackDeepPurpleAmberTheme,
            R.style.AppDarkDeepPurpleOrangeTheme, R.style.AppLightDeepPurpleOrangeTheme, R.style.AppBlackDeepPurpleOrangeTheme,
            R.style.AppDarkDeepPurpleDeepOrangeTheme, R.style.AppLightDeepPurpleDeepOrangeTheme, R.style.AppBlackDeepPurpleDeepOrangeTheme,
            R.style.AppDarkDeepPurpleBrownTheme, R.style.AppLightDeepPurpleBrownTheme, R.style.AppBlackDeepPurpleBrownTheme,
            R.style.AppDarkDeepPurpleGreyTheme, R.style.AppLightDeepPurpleGreyTheme, R.style.AppBlackDeepPurpleGreyTheme,
            R.style.AppDarkDeepPurpleBlueGreyTheme, R.style.AppLightDeepPurpleBlueGreyTheme, R.style.AppBlackDeepPurpleBlueGreyTheme),

    THEME_INDIGO(AppSettings.THEME_INDIGO,
            R.style.AppDarkIndigoRedTheme, R.style.AppLightIndigoRedTheme, R.style.AppBlackIndigoRedTheme,
            R.style.AppDarkIndigoPinkTheme, R.style.AppLightIndigoPinkTheme, R.style.AppBlackIndigoPinkTheme,
            R.style.AppDarkIndigoPurpleTheme, R.style.AppLightIndigoPurpleTheme, R.style.AppBlackIndigoPurpleTheme,
            R.style.AppDarkIndigoDeepPurpleTheme, R.style.AppLightIndigoDeepPurpleTheme, R.style.AppBlackIndigoDeepPurpleTheme,
            R.style.AppDarkIndigoIndigoTheme, R.style.AppLightIndigoIndigoTheme, R.style.AppBlackIndigoIndigoTheme,
            R.style.AppDarkIndigoBlueTheme, R.style.AppLightIndigoBlueTheme, R.style.AppBlackIndigoBlueTheme,
            R.style.AppDarkIndigoLightBlueTheme, R.style.AppLightIndigoLightBlueTheme, R.style.AppBlackIndigoLightBlueTheme,
            R.style.AppDarkIndigoCyanTheme, R.style.AppLightIndigoCyanTheme, R.style.AppBlackIndigoCyanTheme,
            R.style.AppDarkIndigoTealTheme, R.style.AppLightIndigoTealTheme, R.style.AppBlackIndigoTealTheme,
            R.style.AppDarkIndigoGreenTheme, R.style.AppLightIndigoGreenTheme, R.style.AppBlackIndigoGreenTheme,
            R.style.AppDarkIndigoLightGreenTheme, R.style.AppLightIndigoLightGreenTheme, R.style.AppBlackIndigoLightGreenTheme,
            R.style.AppDarkIndigoLimeTheme, R.style.AppLightIndigoLimeTheme, R.style.AppBlackIndigoLimeTheme,
            R.style.AppDarkIndigoYellowTheme, R.style.AppLightIndigoYellowTheme, R.style.AppBlackIndigoYellowTheme,
            R.style.AppDarkIndigoAmberTheme, R.style.AppLightIndigoAmberTheme, R.style.AppBlackIndigoAmberTheme,
            R.style.AppDarkIndigoOrangeTheme, R.style.AppLightIndigoOrangeTheme, R.style.AppBlackIndigoOrangeTheme,
            R.style.AppDarkIndigoDeepOrangeTheme, R.style.AppLightIndigoDeepOrangeTheme, R.style.AppBlackIndigoDeepOrangeTheme,
            R.style.AppDarkIndigoBrownTheme, R.style.AppLightIndigoBrownTheme, R.style.AppBlackIndigoBrownTheme,
            R.style.AppDarkIndigoGreyTheme, R.style.AppLightIndigoGreyTheme, R.style.AppBlackIndigoGreyTheme,
            R.style.AppDarkIndigoBlueGreyTheme, R.style.AppLightIndigoBlueGreyTheme, R.style.AppBlackIndigoBlueGreyTheme),

    THEME_BLUE(AppSettings.THEME_BLUE,
            R.style.AppDarkBlueRedTheme, R.style.AppLightBlueRedTheme, R.style.AppBlackBlueRedTheme,
            R.style.AppDarkBluePinkTheme, R.style.AppLightBluePinkTheme, R.style.AppBlackBluePinkTheme,
            R.style.AppDarkBluePurpleTheme, R.style.AppLightBluePurpleTheme, R.style.AppBlackBluePurpleTheme,
            R.style.AppDarkBlueDeepPurpleTheme, R.style.AppLightBlueDeepPurpleTheme, R.style.AppBlackBlueDeepPurpleTheme,
            R.style.AppDarkBlueIndigoTheme, R.style.AppLightBlueIndigoTheme, R.style.AppBlackBlueIndigoTheme,
            R.style.AppDarkBlueBlueTheme, R.style.AppLightBlueBlueTheme, R.style.AppBlackBlueBlueTheme,
            R.style.AppDarkBlueLightBlueTheme, R.style.AppLightBlueLightBlueTheme, R.style.AppBlackBlueLightBlueTheme,
            R.style.AppDarkBlueCyanTheme, R.style.AppLightBlueCyanTheme, R.style.AppBlackBlueCyanTheme,
            R.style.AppDarkBlueTealTheme, R.style.AppLightBlueTealTheme, R.style.AppBlackBlueTealTheme,
            R.style.AppDarkBlueGreenTheme, R.style.AppLightBlueGreenTheme, R.style.AppBlackBlueGreenTheme,
            R.style.AppDarkBlueLightGreenTheme, R.style.AppLightBlueLightGreenTheme, R.style.AppBlackBlueLightGreenTheme,
            R.style.AppDarkBlueLimeTheme, R.style.AppLightBlueLimeTheme, R.style.AppBlackBlueLimeTheme,
            R.style.AppDarkBlueYellowTheme, R.style.AppLightBlueYellowTheme, R.style.AppBlackBlueYellowTheme,
            R.style.AppDarkBlueAmberTheme, R.style.AppLightBlueAmberTheme, R.style.AppBlackBlueAmberTheme,
            R.style.AppDarkBlueOrangeTheme, R.style.AppLightBlueOrangeTheme, R.style.AppBlackBlueOrangeTheme,
            R.style.AppDarkBlueDeepOrangeTheme, R.style.AppLightBlueDeepOrangeTheme, R.style.AppBlackBlueDeepOrangeTheme,
            R.style.AppDarkBlueBrownTheme, R.style.AppLightBlueBrownTheme, R.style.AppBlackBlueBrownTheme,
            R.style.AppDarkBlueGreyTheme, R.style.AppLightBlueGreyTheme, R.style.AppBlackBlueGreyTheme,
            R.style.AppDarkBlueBlueGreyTheme, R.style.AppLightBlueBlueGreyTheme, R.style.AppBlackBlueBlueGreyTheme),

    THEME_LIGHT_BLUE(AppSettings.THEME_LIGHT_BLUE,
            R.style.AppDarkLightBlueRedTheme, R.style.AppLightLightBlueRedTheme, R.style.AppBlackLightBlueRedTheme,
            R.style.AppDarkLightBluePinkTheme, R.style.AppLightLightBluePinkTheme, R.style.AppBlackLightBluePinkTheme,
            R.style.AppDarkLightBluePurpleTheme, R.style.AppLightLightBluePurpleTheme, R.style.AppBlackLightBluePurpleTheme,
            R.style.AppDarkLightBlueDeepPurpleTheme, R.style.AppLightLightBlueDeepPurpleTheme, R.style.AppBlackLightBlueDeepPurpleTheme,
            R.style.AppDarkLightBlueIndigoTheme, R.style.AppLightLightBlueIndigoTheme, R.style.AppBlackLightBlueIndigoTheme,
            R.style.AppDarkLightBlueBlueTheme, R.style.AppLightLightBlueBlueTheme, R.style.AppBlackLightBlueBlueTheme,
            R.style.AppDarkLightBlueLightBlueTheme, R.style.AppLightLightBlueLightBlueTheme, R.style.AppBlackLightBlueLightBlueTheme,
            R.style.AppDarkLightBlueCyanTheme, R.style.AppLightLightBlueCyanTheme, R.style.AppBlackLightBlueCyanTheme,
            R.style.AppDarkLightBlueTealTheme, R.style.AppLightLightBlueTealTheme, R.style.AppBlackLightBlueTealTheme,
            R.style.AppDarkLightBlueGreenTheme, R.style.AppLightLightBlueGreenTheme, R.style.AppBlackLightBlueGreenTheme,
            R.style.AppDarkLightBlueLightGreenTheme, R.style.AppLightLightBlueLightGreenTheme, R.style.AppBlackLightBlueLightGreenTheme,
            R.style.AppDarkLightBlueLimeTheme, R.style.AppLightLightBlueLimeTheme, R.style.AppBlackLightBlueLimeTheme,
            R.style.AppDarkLightBlueYellowTheme, R.style.AppLightLightBlueYellowTheme, R.style.AppBlackLightBlueYellowTheme,
            R.style.AppDarkLightBlueAmberTheme, R.style.AppLightLightBlueAmberTheme, R.style.AppBlackLightBlueAmberTheme,
            R.style.AppDarkLightBlueOrangeTheme, R.style.AppLightLightBlueOrangeTheme, R.style.AppBlackLightBlueOrangeTheme,
            R.style.AppDarkLightBlueDeepOrangeTheme, R.style.AppLightLightBlueDeepOrangeTheme, R.style.AppBlackLightBlueDeepOrangeTheme,
            R.style.AppDarkLightBlueBrownTheme, R.style.AppLightLightBlueBrownTheme, R.style.AppBlackLightBlueBrownTheme,
            R.style.AppDarkLightBlueGreyTheme, R.style.AppLightLightBlueGreyTheme, R.style.AppBlackLightBlueGreyTheme,
            R.style.AppDarkLightBlueBlueGreyTheme, R.style.AppLightLightBlueBlueGreyTheme, R.style.AppBlackLightBlueBlueGreyTheme),

    THEME_CYAN(AppSettings.THEME_CYAN,
            R.style.AppDarkCyanRedTheme, R.style.AppLightCyanRedTheme, R.style.AppBlackCyanRedTheme,
            R.style.AppDarkCyanPinkTheme, R.style.AppLightCyanPinkTheme, R.style.AppBlackCyanPinkTheme,
            R.style.AppDarkCyanPurpleTheme, R.style.AppLightCyanPurpleTheme, R.style.AppBlackCyanPurpleTheme,
            R.style.AppDarkCyanDeepPurpleTheme, R.style.AppLightCyanDeepPurpleTheme, R.style.AppBlackCyanDeepPurpleTheme,
            R.style.AppDarkCyanIndigoTheme, R.style.AppLightCyanIndigoTheme, R.style.AppBlackCyanIndigoTheme,
            R.style.AppDarkCyanBlueTheme, R.style.AppLightCyanBlueTheme, R.style.AppBlackCyanBlueTheme,
            R.style.AppDarkCyanLightBlueTheme, R.style.AppLightCyanLightBlueTheme, R.style.AppBlackCyanLightBlueTheme,
            R.style.AppDarkCyanCyanTheme, R.style.AppLightCyanCyanTheme, R.style.AppBlackCyanCyanTheme,
            R.style.AppDarkCyanTealTheme, R.style.AppLightCyanTealTheme, R.style.AppBlackCyanTealTheme,
            R.style.AppDarkCyanGreenTheme, R.style.AppLightCyanGreenTheme, R.style.AppBlackCyanGreenTheme,
            R.style.AppDarkCyanLightGreenTheme, R.style.AppLightCyanLightGreenTheme, R.style.AppBlackCyanLightGreenTheme,
            R.style.AppDarkCyanLimeTheme, R.style.AppLightCyanLimeTheme, R.style.AppBlackCyanLimeTheme,
            R.style.AppDarkCyanYellowTheme, R.style.AppLightCyanYellowTheme, R.style.AppBlackCyanYellowTheme,
            R.style.AppDarkCyanAmberTheme, R.style.AppLightCyanAmberTheme, R.style.AppBlackCyanAmberTheme,
            R.style.AppDarkCyanOrangeTheme, R.style.AppLightCyanOrangeTheme, R.style.AppBlackCyanOrangeTheme,
            R.style.AppDarkCyanDeepOrangeTheme, R.style.AppLightCyanDeepOrangeTheme, R.style.AppBlackCyanDeepOrangeTheme,
            R.style.AppDarkCyanBrownTheme, R.style.AppLightCyanBrownTheme, R.style.AppBlackCyanBrownTheme,
            R.style.AppDarkCyanGreyTheme, R.style.AppLightCyanGreyTheme, R.style.AppBlackCyanGreyTheme,
            R.style.AppDarkCyanBlueGreyTheme, R.style.AppLightCyanBlueGreyTheme, R.style.AppBlackCyanBlueGreyTheme),

    THEME_TEAL(AppSettings.THEME_TEAL,
            R.style.AppDarkTealRedTheme, R.style.AppLightTealRedTheme, R.style.AppBlackTealRedTheme,
            R.style.AppDarkTealPinkTheme, R.style.AppLightTealPinkTheme, R.style.AppBlackTealPinkTheme,
            R.style.AppDarkTealPurpleTheme, R.style.AppLightTealPurpleTheme, R.style.AppBlackTealPurpleTheme,
            R.style.AppDarkTealDeepPurpleTheme, R.style.AppLightTealDeepPurpleTheme, R.style.AppBlackTealDeepPurpleTheme,
            R.style.AppDarkTealIndigoTheme, R.style.AppLightTealIndigoTheme, R.style.AppBlackTealIndigoTheme,
            R.style.AppDarkTealBlueTheme, R.style.AppLightTealBlueTheme, R.style.AppBlackTealBlueTheme,
            R.style.AppDarkTealLightBlueTheme, R.style.AppLightTealLightBlueTheme, R.style.AppBlackTealLightBlueTheme,
            R.style.AppDarkTealCyanTheme, R.style.AppLightTealCyanTheme, R.style.AppBlackTealCyanTheme,
            R.style.AppDarkTealTealTheme, R.style.AppLightTealTealTheme, R.style.AppBlackTealTealTheme,
            R.style.AppDarkTealGreenTheme, R.style.AppLightTealGreenTheme, R.style.AppBlackTealGreenTheme,
            R.style.AppDarkTealLightGreenTheme, R.style.AppLightTealLightGreenTheme, R.style.AppBlackTealLightGreenTheme,
            R.style.AppDarkTealLimeTheme, R.style.AppLightTealLimeTheme, R.style.AppBlackTealLimeTheme,
            R.style.AppDarkTealYellowTheme, R.style.AppLightTealYellowTheme, R.style.AppBlackTealYellowTheme,
            R.style.AppDarkTealAmberTheme, R.style.AppLightTealAmberTheme, R.style.AppBlackTealAmberTheme,
            R.style.AppDarkTealOrangeTheme, R.style.AppLightTealOrangeTheme, R.style.AppBlackTealOrangeTheme,
            R.style.AppDarkTealDeepOrangeTheme, R.style.AppLightTealDeepOrangeTheme, R.style.AppBlackTealDeepOrangeTheme,
            R.style.AppDarkTealBrownTheme, R.style.AppLightTealBrownTheme, R.style.AppBlackTealBrownTheme,
            R.style.AppDarkTealGreyTheme, R.style.AppLightTealGreyTheme, R.style.AppBlackTealGreyTheme,
            R.style.AppDarkTealBlueGreyTheme, R.style.AppLightTealBlueGreyTheme, R.style.AppBlackTealBlueGreyTheme),

    THEME_GREEN(AppSettings.THEME_GREEN,
            R.style.AppDarkGreenRedTheme, R.style.AppLightGreenRedTheme, R.style.AppBlackGreenRedTheme,
            R.style.AppDarkGreenPinkTheme, R.style.AppLightGreenPinkTheme, R.style.AppBlackGreenPinkTheme,
            R.style.AppDarkGreenPurpleTheme, R.style.AppLightGreenPurpleTheme, R.style.AppBlackGreenPurpleTheme,
            R.style.AppDarkGreenDeepPurpleTheme, R.style.AppLightGreenDeepPurpleTheme, R.style.AppBlackGreenDeepPurpleTheme,
            R.style.AppDarkGreenIndigoTheme, R.style.AppLightGreenIndigoTheme, R.style.AppBlackGreenIndigoTheme,
            R.style.AppDarkGreenBlueTheme, R.style.AppLightGreenBlueTheme, R.style.AppBlackGreenBlueTheme,
            R.style.AppDarkGreenLightBlueTheme, R.style.AppLightGreenLightBlueTheme, R.style.AppBlackGreenLightBlueTheme,
            R.style.AppDarkGreenCyanTheme, R.style.AppLightGreenCyanTheme, R.style.AppBlackGreenCyanTheme,
            R.style.AppDarkGreenTealTheme, R.style.AppLightGreenTealTheme, R.style.AppBlackGreenTealTheme,
            R.style.AppDarkGreenGreenTheme, R.style.AppLightGreenGreenTheme, R.style.AppBlackGreenGreenTheme,
            R.style.AppDarkGreenLightGreenTheme, R.style.AppLightGreenLightGreenTheme, R.style.AppBlackGreenLightGreenTheme,
            R.style.AppDarkGreenLimeTheme, R.style.AppLightGreenLimeTheme, R.style.AppBlackGreenLimeTheme,
            R.style.AppDarkGreenYellowTheme, R.style.AppLightGreenYellowTheme, R.style.AppBlackGreenYellowTheme,
            R.style.AppDarkGreenAmberTheme, R.style.AppLightGreenAmberTheme, R.style.AppBlackGreenAmberTheme,
            R.style.AppDarkGreenOrangeTheme, R.style.AppLightGreenOrangeTheme, R.style.AppBlackGreenOrangeTheme,
            R.style.AppDarkGreenDeepOrangeTheme, R.style.AppLightGreenDeepOrangeTheme, R.style.AppBlackGreenDeepOrangeTheme,
            R.style.AppDarkGreenBrownTheme, R.style.AppLightGreenBrownTheme, R.style.AppBlackGreenBrownTheme,
            R.style.AppDarkGreenGreyTheme, R.style.AppLightGreenGreyTheme, R.style.AppBlackGreenGreyTheme,
            R.style.AppDarkGreenBlueGreyTheme, R.style.AppLightGreenBlueGreyTheme, R.style.AppBlackGreenBlueGreyTheme),

    THEME_LIGHT_GREEN(AppSettings.THEME_LIGHT_GREEN,
            R.style.AppDarkLightGreenRedTheme, R.style.AppLightLightGreenRedTheme, R.style.AppBlackLightGreenRedTheme,
            R.style.AppDarkLightGreenPinkTheme, R.style.AppLightLightGreenPinkTheme, R.style.AppBlackLightGreenPinkTheme,
            R.style.AppDarkLightGreenPurpleTheme, R.style.AppLightLightGreenPurpleTheme, R.style.AppBlackLightGreenPurpleTheme,
            R.style.AppDarkLightGreenDeepPurpleTheme, R.style.AppLightLightGreenDeepPurpleTheme, R.style.AppBlackLightGreenDeepPurpleTheme,
            R.style.AppDarkLightGreenIndigoTheme, R.style.AppLightLightGreenIndigoTheme, R.style.AppBlackLightGreenIndigoTheme,
            R.style.AppDarkLightGreenBlueTheme, R.style.AppLightLightGreenBlueTheme, R.style.AppBlackLightGreenBlueTheme,
            R.style.AppDarkLightGreenLightBlueTheme, R.style.AppLightLightGreenLightBlueTheme, R.style.AppBlackLightGreenLightBlueTheme,
            R.style.AppDarkLightGreenCyanTheme, R.style.AppLightLightGreenCyanTheme, R.style.AppBlackLightGreenCyanTheme,
            R.style.AppDarkLightGreenTealTheme, R.style.AppLightLightGreenTealTheme, R.style.AppBlackLightGreenTealTheme,
            R.style.AppDarkLightGreenGreenTheme, R.style.AppLightLightGreenGreenTheme, R.style.AppBlackLightGreenGreenTheme,
            R.style.AppDarkLightGreenLightGreenTheme, R.style.AppLightLightGreenLightGreenTheme, R.style.AppBlackLightGreenLightGreenTheme,
            R.style.AppDarkLightGreenLimeTheme, R.style.AppLightLightGreenLimeTheme, R.style.AppBlackLightGreenLimeTheme,
            R.style.AppDarkLightGreenYellowTheme, R.style.AppLightLightGreenYellowTheme, R.style.AppBlackLightGreenYellowTheme,
            R.style.AppDarkLightGreenAmberTheme, R.style.AppLightLightGreenAmberTheme, R.style.AppBlackLightGreenAmberTheme,
            R.style.AppDarkLightGreenOrangeTheme, R.style.AppLightLightGreenOrangeTheme, R.style.AppBlackLightGreenOrangeTheme,
            R.style.AppDarkLightGreenDeepOrangeTheme, R.style.AppLightLightGreenDeepOrangeTheme, R.style.AppBlackLightGreenDeepOrangeTheme,
            R.style.AppDarkLightGreenBrownTheme, R.style.AppLightLightGreenBrownTheme, R.style.AppBlackLightGreenBrownTheme,
            R.style.AppDarkLightGreenGreyTheme, R.style.AppLightLightGreenGreyTheme, R.style.AppBlackLightGreenGreyTheme,
            R.style.AppDarkLightGreenBlueGreyTheme, R.style.AppLightLightGreenBlueGreyTheme, R.style.AppBlackLightGreenBlueGreyTheme),

    THEME_LIME(AppSettings.THEME_LIME,
            R.style.AppDarkLimeRedTheme, R.style.AppLightLimeRedTheme, R.style.AppBlackLimeRedTheme,
            R.style.AppDarkLimePinkTheme, R.style.AppLightLimePinkTheme, R.style.AppBlackLimePinkTheme,
            R.style.AppDarkLimePurpleTheme, R.style.AppLightLimePurpleTheme, R.style.AppBlackLimePurpleTheme,
            R.style.AppDarkLimeDeepPurpleTheme, R.style.AppLightLimeDeepPurpleTheme, R.style.AppBlackLimeDeepPurpleTheme,
            R.style.AppDarkLimeIndigoTheme, R.style.AppLightLimeIndigoTheme, R.style.AppBlackLimeIndigoTheme,
            R.style.AppDarkLimeBlueTheme, R.style.AppLightLimeBlueTheme, R.style.AppBlackLimeBlueTheme,
            R.style.AppDarkLimeLightBlueTheme, R.style.AppLightLimeLightBlueTheme, R.style.AppBlackLimeLightBlueTheme,
            R.style.AppDarkLimeCyanTheme, R.style.AppLightLimeCyanTheme, R.style.AppBlackLimeCyanTheme,
            R.style.AppDarkLimeTealTheme, R.style.AppLightLimeTealTheme, R.style.AppBlackLimeTealTheme,
            R.style.AppDarkLimeGreenTheme, R.style.AppLightLimeGreenTheme, R.style.AppBlackLimeGreenTheme,
            R.style.AppDarkLimeLightGreenTheme, R.style.AppLightLimeLightGreenTheme, R.style.AppBlackLimeLightGreenTheme,
            R.style.AppDarkLimeLimeTheme, R.style.AppLightLimeLimeTheme, R.style.AppBlackLimeLimeTheme,
            R.style.AppDarkLimeYellowTheme, R.style.AppLightLimeYellowTheme, R.style.AppBlackLimeYellowTheme,
            R.style.AppDarkLimeAmberTheme, R.style.AppLightLimeAmberTheme, R.style.AppBlackLimeAmberTheme,
            R.style.AppDarkLimeOrangeTheme, R.style.AppLightLimeOrangeTheme, R.style.AppBlackLimeOrangeTheme,
            R.style.AppDarkLimeDeepOrangeTheme, R.style.AppLightLimeDeepOrangeTheme, R.style.AppBlackLimeDeepOrangeTheme,
            R.style.AppDarkLimeBrownTheme, R.style.AppLightLimeBrownTheme, R.style.AppBlackLimeBrownTheme,
            R.style.AppDarkLimeGreyTheme, R.style.AppLightLimeGreyTheme, R.style.AppBlackLimeGreyTheme,
            R.style.AppDarkLimeBlueGreyTheme, R.style.AppLightLimeBlueGreyTheme, R.style.AppBlackLimeBlueGreyTheme),

    THEME_YELLOW(AppSettings.THEME_YELLOW,
            R.style.AppDarkYellowRedTheme, R.style.AppLightYellowRedTheme, R.style.AppBlackYellowRedTheme,
            R.style.AppDarkYellowPinkTheme, R.style.AppLightYellowPinkTheme, R.style.AppBlackYellowPinkTheme,
            R.style.AppDarkYellowPurpleTheme, R.style.AppLightYellowPurpleTheme, R.style.AppBlackYellowPurpleTheme,
            R.style.AppDarkYellowDeepPurpleTheme, R.style.AppLightYellowDeepPurpleTheme, R.style.AppBlackYellowDeepPurpleTheme,
            R.style.AppDarkYellowIndigoTheme, R.style.AppLightYellowIndigoTheme, R.style.AppBlackYellowIndigoTheme,
            R.style.AppDarkYellowBlueTheme, R.style.AppLightYellowBlueTheme, R.style.AppBlackYellowBlueTheme,
            R.style.AppDarkYellowLightBlueTheme, R.style.AppLightYellowLightBlueTheme, R.style.AppBlackYellowLightBlueTheme,
            R.style.AppDarkYellowCyanTheme, R.style.AppLightYellowCyanTheme, R.style.AppBlackYellowCyanTheme,
            R.style.AppDarkYellowTealTheme, R.style.AppLightYellowTealTheme, R.style.AppBlackYellowTealTheme,
            R.style.AppDarkYellowGreenTheme, R.style.AppLightYellowGreenTheme, R.style.AppBlackYellowGreenTheme,
            R.style.AppDarkYellowLightGreenTheme, R.style.AppLightYellowLightGreenTheme, R.style.AppBlackYellowLightGreenTheme,
            R.style.AppDarkYellowLimeTheme, R.style.AppLightYellowLimeTheme, R.style.AppBlackYellowLimeTheme,
            R.style.AppDarkYellowYellowTheme, R.style.AppLightYellowYellowTheme, R.style.AppBlackYellowYellowTheme,
            R.style.AppDarkYellowAmberTheme, R.style.AppLightYellowAmberTheme, R.style.AppBlackYellowAmberTheme,
            R.style.AppDarkYellowOrangeTheme, R.style.AppLightYellowOrangeTheme, R.style.AppBlackYellowOrangeTheme,
            R.style.AppDarkYellowDeepOrangeTheme, R.style.AppLightYellowDeepOrangeTheme, R.style.AppBlackYellowDeepOrangeTheme,
            R.style.AppDarkYellowBrownTheme, R.style.AppLightYellowBrownTheme, R.style.AppBlackYellowBrownTheme,
            R.style.AppDarkYellowGreyTheme, R.style.AppLightYellowGreyTheme, R.style.AppBlackYellowGreyTheme,
            R.style.AppDarkYellowBlueGreyTheme, R.style.AppLightYellowBlueGreyTheme, R.style.AppBlackYellowBlueGreyTheme),

    THEME_AMBER(AppSettings.THEME_AMBER,
            R.style.AppDarkAmberRedTheme, R.style.AppLightAmberRedTheme, R.style.AppBlackAmberRedTheme,
            R.style.AppDarkAmberPinkTheme, R.style.AppLightAmberPinkTheme, R.style.AppBlackAmberPinkTheme,
            R.style.AppDarkAmberPurpleTheme, R.style.AppLightAmberPurpleTheme, R.style.AppBlackAmberPurpleTheme,
            R.style.AppDarkAmberDeepPurpleTheme, R.style.AppLightAmberDeepPurpleTheme, R.style.AppBlackAmberDeepPurpleTheme,
            R.style.AppDarkAmberIndigoTheme, R.style.AppLightAmberIndigoTheme, R.style.AppBlackAmberIndigoTheme,
            R.style.AppDarkAmberBlueTheme, R.style.AppLightAmberBlueTheme, R.style.AppBlackAmberBlueTheme,
            R.style.AppDarkAmberLightBlueTheme, R.style.AppLightAmberLightBlueTheme, R.style.AppBlackAmberLightBlueTheme,
            R.style.AppDarkAmberCyanTheme, R.style.AppLightAmberCyanTheme, R.style.AppBlackAmberCyanTheme,
            R.style.AppDarkAmberTealTheme, R.style.AppLightAmberTealTheme, R.style.AppBlackAmberTealTheme,
            R.style.AppDarkAmberGreenTheme, R.style.AppLightAmberGreenTheme, R.style.AppBlackAmberGreenTheme,
            R.style.AppDarkAmberLightGreenTheme, R.style.AppLightAmberLightGreenTheme, R.style.AppBlackAmberLightGreenTheme,
            R.style.AppDarkAmberLimeTheme, R.style.AppLightAmberLimeTheme, R.style.AppBlackAmberLimeTheme,
            R.style.AppDarkAmberYellowTheme, R.style.AppLightAmberYellowTheme, R.style.AppBlackAmberYellowTheme,
            R.style.AppDarkAmberAmberTheme, R.style.AppLightAmberAmberTheme, R.style.AppBlackAmberAmberTheme,
            R.style.AppDarkAmberOrangeTheme, R.style.AppLightAmberOrangeTheme, R.style.AppBlackAmberOrangeTheme,
            R.style.AppDarkAmberDeepOrangeTheme, R.style.AppLightAmberDeepOrangeTheme, R.style.AppBlackAmberDeepOrangeTheme,
            R.style.AppDarkAmberBrownTheme, R.style.AppLightAmberBrownTheme, R.style.AppBlackAmberBrownTheme,
            R.style.AppDarkAmberGreyTheme, R.style.AppLightAmberGreyTheme, R.style.AppBlackAmberGreyTheme,
            R.style.AppDarkAmberBlueGreyTheme, R.style.AppLightAmberBlueGreyTheme, R.style.AppBlackAmberBlueGreyTheme),

    THEME_ORANGE(AppSettings.THEME_ORANGE,
            R.style.AppDarkOrangeRedTheme, R.style.AppLightOrangeRedTheme, R.style.AppBlackOrangeRedTheme,
            R.style.AppDarkOrangePinkTheme, R.style.AppLightOrangePinkTheme, R.style.AppBlackOrangePinkTheme,
            R.style.AppDarkOrangePurpleTheme, R.style.AppLightOrangePurpleTheme, R.style.AppBlackOrangePurpleTheme,
            R.style.AppDarkOrangeDeepPurpleTheme, R.style.AppLightOrangeDeepPurpleTheme, R.style.AppBlackOrangeDeepPurpleTheme,
            R.style.AppDarkOrangeIndigoTheme, R.style.AppLightOrangeIndigoTheme, R.style.AppBlackOrangeIndigoTheme,
            R.style.AppDarkOrangeBlueTheme, R.style.AppLightOrangeBlueTheme, R.style.AppBlackOrangeBlueTheme,
            R.style.AppDarkOrangeLightBlueTheme, R.style.AppLightOrangeLightBlueTheme, R.style.AppBlackOrangeLightBlueTheme,
            R.style.AppDarkOrangeCyanTheme, R.style.AppLightOrangeCyanTheme, R.style.AppBlackOrangeCyanTheme,
            R.style.AppDarkOrangeTealTheme, R.style.AppLightOrangeTealTheme, R.style.AppBlackOrangeTealTheme,
            R.style.AppDarkOrangeGreenTheme, R.style.AppLightOrangeGreenTheme, R.style.AppBlackOrangeGreenTheme,
            R.style.AppDarkOrangeLightGreenTheme, R.style.AppLightOrangeLightGreenTheme, R.style.AppBlackOrangeLightGreenTheme,
            R.style.AppDarkOrangeLimeTheme, R.style.AppLightOrangeLimeTheme, R.style.AppBlackOrangeLimeTheme,
            R.style.AppDarkOrangeYellowTheme, R.style.AppLightOrangeYellowTheme, R.style.AppBlackOrangeYellowTheme,
            R.style.AppDarkOrangeAmberTheme, R.style.AppLightOrangeAmberTheme, R.style.AppBlackOrangeAmberTheme,
            R.style.AppDarkOrangeOrangeTheme, R.style.AppLightOrangeOrangeTheme, R.style.AppBlackOrangeOrangeTheme,
            R.style.AppDarkOrangeDeepOrangeTheme, R.style.AppLightOrangeDeepOrangeTheme, R.style.AppBlackOrangeDeepOrangeTheme,
            R.style.AppDarkOrangeBrownTheme, R.style.AppLightOrangeBrownTheme, R.style.AppBlackOrangeBrownTheme,
            R.style.AppDarkOrangeGreyTheme, R.style.AppLightOrangeGreyTheme, R.style.AppBlackOrangeGreyTheme,
            R.style.AppDarkOrangeBlueGreyTheme, R.style.AppLightOrangeBlueGreyTheme, R.style.AppBlackOrangeBlueGreyTheme),

    THEME_DEEP_ORANGE(AppSettings.THEME_DEEP_ORANGE,
            R.style.AppDarkDeepOrangeRedTheme, R.style.AppLightDeepOrangeRedTheme, R.style.AppBlackDeepOrangeRedTheme,
            R.style.AppDarkDeepOrangePinkTheme, R.style.AppLightDeepOrangePinkTheme, R.style.AppBlackDeepOrangePinkTheme,
            R.style.AppDarkDeepOrangePurpleTheme, R.style.AppLightDeepOrangePurpleTheme, R.style.AppBlackDeepOrangePurpleTheme,
            R.style.AppDarkDeepOrangeDeepPurpleTheme, R.style.AppLightDeepOrangeDeepPurpleTheme, R.style.AppBlackDeepOrangeDeepPurpleTheme,
            R.style.AppDarkDeepOrangeIndigoTheme, R.style.AppLightDeepOrangeIndigoTheme, R.style.AppBlackDeepOrangeIndigoTheme,
            R.style.AppDarkDeepOrangeBlueTheme, R.style.AppLightDeepOrangeBlueTheme, R.style.AppBlackDeepOrangeBlueTheme,
            R.style.AppDarkDeepOrangeLightBlueTheme, R.style.AppLightDeepOrangeLightBlueTheme, R.style.AppBlackDeepOrangeLightBlueTheme,
            R.style.AppDarkDeepOrangeCyanTheme, R.style.AppLightDeepOrangeCyanTheme, R.style.AppBlackDeepOrangeCyanTheme,
            R.style.AppDarkDeepOrangeTealTheme, R.style.AppLightDeepOrangeTealTheme, R.style.AppBlackDeepOrangeTealTheme,
            R.style.AppDarkDeepOrangeGreenTheme, R.style.AppLightDeepOrangeGreenTheme, R.style.AppBlackDeepOrangeGreenTheme,
            R.style.AppDarkDeepOrangeLightGreenTheme, R.style.AppLightDeepOrangeLightGreenTheme, R.style.AppBlackDeepOrangeLightGreenTheme,
            R.style.AppDarkDeepOrangeLimeTheme, R.style.AppLightDeepOrangeLimeTheme, R.style.AppBlackDeepOrangeLimeTheme,
            R.style.AppDarkDeepOrangeYellowTheme, R.style.AppLightDeepOrangeYellowTheme, R.style.AppBlackDeepOrangeYellowTheme,
            R.style.AppDarkDeepOrangeAmberTheme, R.style.AppLightDeepOrangeAmberTheme, R.style.AppBlackDeepOrangeAmberTheme,
            R.style.AppDarkDeepOrangeOrangeTheme, R.style.AppLightDeepOrangeOrangeTheme, R.style.AppBlackDeepOrangeOrangeTheme,
            R.style.AppDarkDeepOrangeDeepOrangeTheme, R.style.AppLightDeepOrangeDeepOrangeTheme, R.style.AppBlackDeepOrangeDeepOrangeTheme,
            R.style.AppDarkDeepOrangeBrownTheme, R.style.AppLightDeepOrangeBrownTheme, R.style.AppBlackDeepOrangeBrownTheme,
            R.style.AppDarkDeepOrangeGreyTheme, R.style.AppLightDeepOrangeGreyTheme, R.style.AppBlackDeepOrangeGreyTheme,
            R.style.AppDarkDeepOrangeBlueGreyTheme, R.style.AppLightDeepOrangeBlueGreyTheme, R.style.AppBlackDeepOrangeBlueGreyTheme),

    THEME_BROWN(AppSettings.THEME_BROWN,
            R.style.AppDarkBrownRedTheme, R.style.AppLightBrownRedTheme, R.style.AppBlackBrownRedTheme,
            R.style.AppDarkBrownPinkTheme, R.style.AppLightBrownPinkTheme, R.style.AppBlackBrownPinkTheme,
            R.style.AppDarkBrownPurpleTheme, R.style.AppLightBrownPurpleTheme, R.style.AppBlackBrownPurpleTheme,
            R.style.AppDarkBrownDeepPurpleTheme, R.style.AppLightBrownDeepPurpleTheme, R.style.AppBlackBrownDeepPurpleTheme,
            R.style.AppDarkBrownIndigoTheme, R.style.AppLightBrownIndigoTheme, R.style.AppBlackBrownIndigoTheme,
            R.style.AppDarkBrownBlueTheme, R.style.AppLightBrownBlueTheme, R.style.AppBlackBrownBlueTheme,
            R.style.AppDarkBrownLightBlueTheme, R.style.AppLightBrownLightBlueTheme, R.style.AppBlackBrownLightBlueTheme,
            R.style.AppDarkBrownCyanTheme, R.style.AppLightBrownCyanTheme, R.style.AppBlackBrownCyanTheme,
            R.style.AppDarkBrownTealTheme, R.style.AppLightBrownTealTheme, R.style.AppBlackBrownTealTheme,
            R.style.AppDarkBrownGreenTheme, R.style.AppLightBrownGreenTheme, R.style.AppBlackBrownGreenTheme,
            R.style.AppDarkBrownLightGreenTheme, R.style.AppLightBrownLightGreenTheme, R.style.AppBlackBrownLightGreenTheme,
            R.style.AppDarkBrownLimeTheme, R.style.AppLightBrownLimeTheme, R.style.AppBlackBrownLimeTheme,
            R.style.AppDarkBrownYellowTheme, R.style.AppLightBrownYellowTheme, R.style.AppBlackBrownYellowTheme,
            R.style.AppDarkBrownAmberTheme, R.style.AppLightBrownAmberTheme, R.style.AppBlackBrownAmberTheme,
            R.style.AppDarkBrownOrangeTheme, R.style.AppLightBrownOrangeTheme, R.style.AppBlackBrownOrangeTheme,
            R.style.AppDarkBrownDeepOrangeTheme, R.style.AppLightBrownDeepOrangeTheme, R.style.AppBlackBrownDeepOrangeTheme,
            R.style.AppDarkBrownBrownTheme, R.style.AppLightBrownBrownTheme, R.style.AppBlackBrownBrownTheme,
            R.style.AppDarkBrownGreyTheme, R.style.AppLightBrownGreyTheme, R.style.AppBlackBrownGreyTheme,
            R.style.AppDarkBrownBlueGreyTheme, R.style.AppLightBrownBlueGreyTheme, R.style.AppBlackBrownBlueGreyTheme),

    THEME_GREY(AppSettings.THEME_GREY,
            R.style.AppDarkGreyRedTheme, R.style.AppLightGreyRedTheme, R.style.AppBlackGreyRedTheme,
            R.style.AppDarkGreyPinkTheme, R.style.AppLightGreyPinkTheme, R.style.AppBlackGreyPinkTheme,
            R.style.AppDarkGreyPurpleTheme, R.style.AppLightGreyPurpleTheme, R.style.AppBlackGreyPurpleTheme,
            R.style.AppDarkGreyDeepPurpleTheme, R.style.AppLightGreyDeepPurpleTheme, R.style.AppBlackGreyDeepPurpleTheme,
            R.style.AppDarkGreyIndigoTheme, R.style.AppLightGreyIndigoTheme, R.style.AppBlackGreyIndigoTheme,
            R.style.AppDarkGreyBlueTheme, R.style.AppLightGreyBlueTheme, R.style.AppBlackGreyBlueTheme,
            R.style.AppDarkGreyLightBlueTheme, R.style.AppLightGreyLightBlueTheme, R.style.AppBlackGreyLightBlueTheme,
            R.style.AppDarkGreyCyanTheme, R.style.AppLightGreyCyanTheme, R.style.AppBlackGreyCyanTheme,
            R.style.AppDarkGreyTealTheme, R.style.AppLightGreyTealTheme, R.style.AppBlackGreyTealTheme,
            R.style.AppDarkGreyGreenTheme, R.style.AppLightGreyGreenTheme, R.style.AppBlackGreyGreenTheme,
            R.style.AppDarkGreyLightGreenTheme, R.style.AppLightGreyLightGreenTheme, R.style.AppBlackGreyLightGreenTheme,
            R.style.AppDarkGreyLimeTheme, R.style.AppLightGreyLimeTheme, R.style.AppBlackGreyLimeTheme,
            R.style.AppDarkGreyYellowTheme, R.style.AppLightGreyYellowTheme, R.style.AppBlackGreyYellowTheme,
            R.style.AppDarkGreyAmberTheme, R.style.AppLightGreyAmberTheme, R.style.AppBlackGreyAmberTheme,
            R.style.AppDarkGreyOrangeTheme, R.style.AppLightGreyOrangeTheme, R.style.AppBlackGreyOrangeTheme,
            R.style.AppDarkGreyDeepOrangeTheme, R.style.AppLightGreyDeepOrangeTheme, R.style.AppBlackGreyDeepOrangeTheme,
            R.style.AppDarkGreyBrownTheme, R.style.AppLightGreyBrownTheme, R.style.AppBlackGreyBrownTheme,
            R.style.AppDarkGreyGreyTheme, R.style.AppLightGreyGreyTheme, R.style.AppBlackGreyGreyTheme,
            R.style.AppDarkGreyBlueGreyTheme, R.style.AppLightGreyBlueGreyTheme, R.style.AppBlackGreyBlueGreyTheme),

    THEME_BLUE_GREY(AppSettings.THEME_BLUE_GREY,
            R.style.AppDarkBlueGreyRedTheme, R.style.AppLightBlueGreyRedTheme, R.style.AppBlackBlueGreyRedTheme,
            R.style.AppDarkBlueGreyPinkTheme, R.style.AppLightBlueGreyPinkTheme, R.style.AppBlackBlueGreyPinkTheme,
            R.style.AppDarkBlueGreyPurpleTheme, R.style.AppLightBlueGreyPurpleTheme, R.style.AppBlackBlueGreyPurpleTheme,
            R.style.AppDarkBlueGreyDeepPurpleTheme, R.style.AppLightBlueGreyDeepPurpleTheme, R.style.AppBlackBlueGreyDeepPurpleTheme,
            R.style.AppDarkBlueGreyIndigoTheme, R.style.AppLightBlueGreyIndigoTheme, R.style.AppBlackBlueGreyIndigoTheme,
            R.style.AppDarkBlueGreyBlueTheme, R.style.AppLightBlueGreyBlueTheme, R.style.AppBlackBlueGreyBlueTheme,
            R.style.AppDarkBlueGreyLightBlueTheme, R.style.AppLightBlueGreyLightBlueTheme, R.style.AppBlackBlueGreyLightBlueTheme,
            R.style.AppDarkBlueGreyCyanTheme, R.style.AppLightBlueGreyCyanTheme, R.style.AppBlackBlueGreyCyanTheme,
            R.style.AppDarkBlueGreyTealTheme, R.style.AppLightBlueGreyTealTheme, R.style.AppBlackBlueGreyTealTheme,
            R.style.AppDarkBlueGreyGreenTheme, R.style.AppLightBlueGreyGreenTheme, R.style.AppBlackBlueGreyGreenTheme,
            R.style.AppDarkBlueGreyLightGreenTheme, R.style.AppLightBlueGreyLightGreenTheme, R.style.AppBlackBlueGreyLightGreenTheme,
            R.style.AppDarkBlueGreyLimeTheme, R.style.AppLightBlueGreyLimeTheme, R.style.AppBlackBlueGreyLimeTheme,
            R.style.AppDarkBlueGreyYellowTheme, R.style.AppLightBlueGreyYellowTheme, R.style.AppBlackBlueGreyYellowTheme,
            R.style.AppDarkBlueGreyAmberTheme, R.style.AppLightBlueGreyAmberTheme, R.style.AppBlackBlueGreyAmberTheme,
            R.style.AppDarkBlueGreyOrangeTheme, R.style.AppLightBlueGreyOrangeTheme, R.style.AppBlackBlueGreyOrangeTheme,
            R.style.AppDarkBlueGreyDeepOrangeTheme, R.style.AppLightBlueGreyDeepOrangeTheme, R.style.AppBlackBlueGreyDeepOrangeTheme,
            R.style.AppDarkBlueGreyBrownTheme, R.style.AppLightBlueGreyBrownTheme, R.style.AppBlackBlueGreyBrownTheme,
            R.style.AppDarkBlueGreyGreyTheme, R.style.AppLightBlueGreyGreyTheme, R.style.AppBlackBlueGreyGreyTheme,
            R.style.AppDarkBlueGreyBlueGreyTheme, R.style.AppLightBlueGreyBlueGreyTheme, R.style.AppBlackBlueGreyBlueGreyTheme);

    // @formatter:on

    private final String name;

    private final int styleDarkRed;
    private final int styleLightRed;
    private final int styleBlackRed;

    private final int styleDarkPink;
    private final int styleLightPink;
    private final int styleBlackPink;

    private final int styleDarkPurple;
    private final int styleLightPurple;
    private final int styleBlackPurple;

    private final int styleDarkDeepPurple;
    private final int styleLightDeepPurple;
    private final int styleBlackDeepPurple;

    private final int styleDarkIndigo;
    private final int styleLightIndigo;
    private final int styleBlackIndigo;

    private final int styleDarkBlue;
    private final int styleLightBlue;
    private final int styleBlackBlue;

    private final int styleDarkLightBlue;
    private final int styleLightLightBlue;
    private final int styleBlackLightBlue;

    private final int styleDarkCyan;
    private final int styleLightCyan;
    private final int styleBlackCyan;

    private final int styleDarkTeal;
    private final int styleLightTeal;
    private final int styleBlackTeal;

    private final int styleDarkGreen;
    private final int styleLightGreen;
    private final int styleBlackGreen;

    private final int styleDarkLightGreen;
    private final int styleLightLightGreen;
    private final int styleBlackLightGreen;

    private final int styleDarkLime;
    private final int styleLightLime;
    private final int styleBlackLime;

    private final int styleDarkYellow;
    private final int styleLightYellow;
    private final int styleBlackYellow;

    private final int styleDarkAmber;
    private final int styleLightAmber;
    private final int styleBlackAmber;

    private final int styleDarkOrange;
    private final int styleLightOrange;
    private final int styleBlackOrange;

    private final int styleDarkDeepOrange;
    private final int styleLightDeepOrange;
    private final int styleBlackDeepOrange;

    private final int styleDarkBrown;
    private final int styleLightBrown;
    private final int styleBlackBrown;

    private final int styleDarkGrey;
    private final int styleLightGrey;
    private final int styleBlackGrey;

    private final int styleDarkBlueGrey;
    private final int styleLightBlueGrey;
    private final int styleBlackBlueGrey;

    Theme(String name,
            int styleDarkRed,
            int styleLightRed,
            int styleBlackRed,
            int styleDarkPink,
            int styleLightPink,
            int styleBlackPink,
            int styleDarkPurple,
            int styleLightPurple,
            int styleBlackPurple,
            int styleDarkDeepPurple,
            int styleLightDeepPurple,
            int styleBlackDeepPurple,
            int styleDarkIndigo,
            int styleLightIndigo,
            int styleBlackIndigo,
            int styleDarkBlue,
            int styleLightBlue,
            int styleBlackBlue,
            int styleDarkLightBlue,
            int styleLightLightBlue,
            int styleBlackLightBlue,
            int styleDarkCyan,
            int styleLightCyan,
            int styleBlackCyan,
            int styleDarkTeal,
            int styleLightTeal,
            int styleBlackTeal,
            int styleDarkGreen,
            int styleLightGreen,
            int styleBlackGreen,
            int styleDarkLightGreen,
            int styleLightLightGreen,
            int styleBlackLightGreen,
            int styleDarkLime,
            int styleLightLime,
            int styleBlackLime,
            int styleDarkYellow,
            int styleLightYellow,
            int styleBlackYellow,
            int styleDarkAmber,
            int styleLightAmber,
            int styleBlackAmber,
            int styleDarkOrange,
            int styleLightOrange,
            int styleBlackOrange,
            int styleDarkDeepOrange,
            int styleLightDeepOrange,
            int styleBlackDeepOrange,
            int styleDarkBrown,
            int styleLightBrown,
            int styleBlackBrown,
            int styleDarkGrey,
            int styleLightGrey,
            int styleBlackGrey,
            int styleDarkBlueGrey,
            int styleLightBlueGrey,
            int styleBlackBlueGrey) {
        this.name = name;
            this.styleDarkRed = styleDarkRed;
            this.styleLightRed = styleLightRed;
            this.styleBlackRed = styleBlackRed;
            this.styleDarkPink = styleDarkPink;
            this.styleLightPink = styleLightPink;
            this.styleBlackPink = styleBlackPink;
            this.styleDarkPurple = styleDarkPurple;
            this.styleLightPurple = styleLightPurple;
            this.styleBlackPurple = styleBlackPurple;
            this.styleDarkDeepPurple = styleDarkDeepPurple;
            this.styleLightDeepPurple = styleLightDeepPurple;
            this.styleBlackDeepPurple = styleBlackDeepPurple;
            this.styleDarkIndigo = styleDarkIndigo;
            this.styleLightIndigo = styleLightIndigo;
            this.styleBlackIndigo = styleBlackIndigo;
            this.styleDarkBlue = styleDarkBlue;
            this.styleLightBlue = styleLightBlue;
            this.styleBlackBlue = styleBlackBlue;
            this.styleDarkLightBlue = styleDarkLightBlue;
            this.styleLightLightBlue = styleLightLightBlue;
            this.styleBlackLightBlue = styleBlackLightBlue;
            this.styleDarkCyan = styleDarkCyan;
            this.styleLightCyan = styleLightCyan;
            this.styleBlackCyan = styleBlackCyan;
            this.styleDarkTeal = styleDarkTeal;
            this.styleLightTeal = styleLightTeal;
            this.styleBlackTeal = styleBlackTeal;
            this.styleDarkGreen = styleDarkGreen;
            this.styleLightGreen = styleLightGreen;
            this.styleBlackGreen = styleBlackGreen;
            this.styleDarkLightGreen = styleDarkLightGreen;
            this.styleLightLightGreen = styleLightLightGreen;
            this.styleBlackLightGreen = styleBlackLightGreen;
            this.styleDarkLime = styleDarkLime;
            this.styleLightLime = styleLightLime;
            this.styleBlackLime = styleBlackLime;
            this.styleDarkYellow = styleDarkYellow;
            this.styleLightYellow = styleLightYellow;
            this.styleBlackYellow = styleBlackYellow;
            this.styleDarkAmber = styleDarkAmber;
            this.styleLightAmber = styleLightAmber;
            this.styleBlackAmber = styleBlackAmber;
            this.styleDarkOrange = styleDarkOrange;
            this.styleLightOrange = styleLightOrange;
            this.styleBlackOrange = styleBlackOrange;
            this.styleDarkDeepOrange = styleDarkDeepOrange;
            this.styleLightDeepOrange = styleLightDeepOrange;
            this.styleBlackDeepOrange = styleBlackDeepOrange;
            this.styleDarkBrown = styleDarkBrown;
            this.styleLightBrown = styleLightBrown;
            this.styleBlackBrown = styleBlackBrown;
            this.styleDarkGrey = styleDarkGrey;
            this.styleLightGrey = styleLightGrey;
            this.styleBlackGrey = styleBlackGrey;
            this.styleDarkBlueGrey = styleDarkBlueGrey;
            this.styleLightBlueGrey = styleLightBlueGrey;
            this.styleBlackBlueGrey = styleBlackBlueGrey;
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
        return THEME_YELLOW;
    }


    public static Theme random() {
        return values()[new Random().nextInt(values().length)];
    }

    public int getStyle(String themeBackground, String themeAccent) {
        switch (themeBackground) {
            case AppSettings.THEME_DARK:
                switch (themeAccent) {
                    case AppSettings.THEME_RED:
                        return styleDarkRed;
                    case AppSettings.THEME_PINK:
                        return styleDarkPink;
                    case AppSettings.THEME_PURPLE:
                        return styleDarkPurple;
                    case AppSettings.THEME_DEEP_PURPLE:
                        return styleDarkDeepPurple;
                    case AppSettings.THEME_INDIGO:
                        return styleDarkIndigo;
                    case AppSettings.THEME_BLUE:
                        return styleDarkBlue;
                    case AppSettings.THEME_LIGHT_BLUE:
                        return styleDarkLightBlue;
                    case AppSettings.THEME_CYAN:
                        return styleDarkCyan;
                    case AppSettings.THEME_TEAL:
                        return styleDarkTeal;
                    case AppSettings.THEME_GREEN:
                        return styleDarkGreen;
                    case AppSettings.THEME_LIGHT_GREEN:
                        return styleDarkLightGreen;
                    case AppSettings.THEME_LIME:
                        return styleDarkLime;
                    case AppSettings.THEME_YELLOW:
                        return styleDarkYellow;
                    case AppSettings.THEME_AMBER:
                        return styleDarkAmber;
                    case AppSettings.THEME_ORANGE:
                        return styleDarkOrange;
                    case AppSettings.THEME_DEEP_ORANGE:
                        return styleDarkDeepOrange;
                    case AppSettings.THEME_BROWN:
                        return styleDarkBrown;
                    case AppSettings.THEME_GREY:
                        return styleDarkGrey;
                    case AppSettings.THEME_BLUE_GREY:
                        return styleDarkBlueGrey;
                }
            case AppSettings.THEME_LIGHT:
                switch (themeAccent) {
                    case AppSettings.THEME_RED:
                        return styleLightRed;
                    case AppSettings.THEME_PINK:
                        return styleLightPink;
                    case AppSettings.THEME_PURPLE:
                        return styleLightPurple;
                    case AppSettings.THEME_DEEP_PURPLE:
                        return styleLightDeepPurple;
                    case AppSettings.THEME_INDIGO:
                        return styleLightIndigo;
                    case AppSettings.THEME_BLUE:
                        return styleLightBlue;
                    case AppSettings.THEME_LIGHT_BLUE:
                        return styleLightLightBlue;
                    case AppSettings.THEME_CYAN:
                        return styleLightCyan;
                    case AppSettings.THEME_TEAL:
                        return styleLightTeal;
                    case AppSettings.THEME_GREEN:
                        return styleLightGreen;
                    case AppSettings.THEME_LIGHT_GREEN:
                        return styleLightLightGreen;
                    case AppSettings.THEME_LIME:
                        return styleLightLime;
                    case AppSettings.THEME_YELLOW:
                        return styleLightYellow;
                    case AppSettings.THEME_AMBER:
                        return styleLightAmber;
                    case AppSettings.THEME_ORANGE:
                        return styleLightOrange;
                    case AppSettings.THEME_DEEP_ORANGE:
                        return styleLightDeepOrange;
                    case AppSettings.THEME_BROWN:
                        return styleLightBrown;
                    case AppSettings.THEME_GREY:
                        return styleLightGrey;
                    case AppSettings.THEME_BLUE_GREY:
                        return styleLightBlueGrey;
                }
            case AppSettings.THEME_BLACK:
                switch (themeAccent) {
                    case AppSettings.THEME_RED:
                        return styleBlackRed;
                    case AppSettings.THEME_PINK:
                        return styleBlackPink;
                    case AppSettings.THEME_PURPLE:
                        return styleBlackPurple;
                    case AppSettings.THEME_DEEP_PURPLE:
                        return styleBlackDeepPurple;
                    case AppSettings.THEME_INDIGO:
                        return styleBlackIndigo;
                    case AppSettings.THEME_BLUE:
                        return styleBlackBlue;
                    case AppSettings.THEME_LIGHT_BLUE:
                        return styleBlackLightBlue;
                    case AppSettings.THEME_CYAN:
                        return styleBlackCyan;
                    case AppSettings.THEME_TEAL:
                        return styleBlackTeal;
                    case AppSettings.THEME_GREEN:
                        return styleBlackGreen;
                    case AppSettings.THEME_LIGHT_GREEN:
                        return styleBlackLightGreen;
                    case AppSettings.THEME_LIME:
                        return styleBlackLime;
                    case AppSettings.THEME_YELLOW:
                        return styleBlackYellow;
                    case AppSettings.THEME_AMBER:
                        return styleBlackAmber;
                    case AppSettings.THEME_ORANGE:
                        return styleBlackOrange;
                    case AppSettings.THEME_DEEP_ORANGE:
                        return styleBlackDeepOrange;
                    case AppSettings.THEME_BROWN:
                        return styleBlackBrown;
                    case AppSettings.THEME_GREY:
                        return styleBlackGrey;
                    case AppSettings.THEME_BLUE_GREY:
                        return styleBlackBlueGrey;
                }
        }
        return R.style.AppDarkTheme;
    }

    public static int getMenuStyle(String themeAccent) {
        switch (themeAccent) {

            case AppSettings.THEME_RED:
                return R.style.MenuRed;
            case AppSettings.THEME_PINK:
                return R.style.MenuPink;
            case AppSettings.THEME_PURPLE:
                return R.style.MenuPurple;
            case AppSettings.THEME_DEEP_PURPLE:
                return R.style.MenuDeepPurple;
            case AppSettings.THEME_INDIGO:
                return R.style.MenuIndigo;
            case AppSettings.THEME_BLUE:
                return R.style.MenuBlue;
            case AppSettings.THEME_LIGHT_BLUE:
                return R.style.MenuLightBlue;
            case AppSettings.THEME_CYAN:
                return R.style.MenuCyan;
            case AppSettings.THEME_TEAL:
                return R.style.MenuTeal;
            case AppSettings.THEME_GREEN:
                return R.style.MenuGreen;
            case AppSettings.THEME_LIGHT_GREEN:
                return R.style.MenuLightGreen;
            case AppSettings.THEME_LIME:
                return R.style.MenuLime;
            case AppSettings.THEME_YELLOW:
                return R.style.MenuYellow;
            case AppSettings.THEME_AMBER:
                return R.style.MenuAmber;
            case AppSettings.THEME_ORANGE:
                return R.style.MenuOrange;
            case AppSettings.THEME_DEEP_ORANGE:
                return R.style.MenuDeepOrange;
            case AppSettings.THEME_BROWN:
                return R.style.MenuBrown;
            case AppSettings.THEME_GREY:
                return R.style.MenuGrey;
            case AppSettings.THEME_BLUE_GREY:
                return R.style.MenuBlueGrey;
        }

        return R.style.AppDarkTheme;
    }

}
