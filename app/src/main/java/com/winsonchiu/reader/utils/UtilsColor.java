package com.winsonchiu.reader.utils;

import android.graphics.Color;

/**
 * Created by TheKeeperOfPie on 8/10/2015.
 */
public class UtilsColor {

    public static final float CONTRAST_THRESHOLD = 3F;

    /**
     * Code taken from com.android.systemui.recents.misc.Utilities to properly
     * set recents card icon color
     * @param background
     * @param foreground
     * @return
     */
    public static float computeContrast(int background, int foreground) {
        float bgR = Color.red(background) / 255f;
        float bgG = Color.green(background) / 255f;
        float bgB = Color.blue(background) / 255f;
        bgR = (bgR < 0.03928f) ? bgR / 12.92f : (float) Math.pow((bgR + 0.055f) / 1.055f, 2.4f);
        bgG = (bgG < 0.03928f) ? bgG / 12.92f : (float) Math.pow((bgG + 0.055f) / 1.055f, 2.4f);
        bgB = (bgB < 0.03928f) ? bgB / 12.92f : (float) Math.pow((bgB + 0.055f) / 1.055f, 2.4f);
        float bgL = 0.2126f * bgR + 0.7152f * bgG + 0.0722f * bgB;

        float fgR = Color.red(foreground) / 255f;
        float fgG = Color.green(foreground) / 255f;
        float fgB = Color.blue(foreground) / 255f;
        fgR = (fgR < 0.03928f) ? fgR / 12.92f : (float) Math.pow((fgR + 0.055f) / 1.055f, 2.4f);
        fgG = (fgG < 0.03928f) ? fgG / 12.92f : (float) Math.pow((fgG + 0.055f) / 1.055f, 2.4f);
        fgB = (fgB < 0.03928f) ? fgB / 12.92f : (float) Math.pow((fgB + 0.055f) / 1.055f, 2.4f);
        float fgL = 0.2126f * fgR + 0.7152f * fgG + 0.0722f * fgB;

        return Math.abs((fgL + 0.05f) / (bgL + 0.05f));
    }

    public static boolean showOnWhite(int color) {
        return UtilsColor.computeContrast(color, Color.WHITE) > CONTRAST_THRESHOLD;
    }

}
