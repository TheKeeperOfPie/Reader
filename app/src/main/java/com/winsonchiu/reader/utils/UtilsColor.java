package com.winsonchiu.reader.utils;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Target;
import android.view.Menu;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.theme.ThemeBackground;
import com.winsonchiu.reader.theme.ThemeColor;

/**
 * Created by TheKeeperOfPie on 8/10/2015.
 */
public class UtilsColor {

    public static final float CONTRAST_THRESHOLD = 3F;

    public static LruCache<Pair<Integer, Integer>, Float> contrastCache;

    /**
     * Code taken from com.android.systemui.recents.misc.Utilities
     * @param background
     * @param foreground
     * @return
     */
    public static float computeContrast(int background, int foreground) {
        Pair<Integer, Integer> key = new Pair<>(background, foreground);

        if (contrastCache == null) {
            contrastCache = new LruCache<>(5);
        } else {
            Float contrast = contrastCache.get(key);
            if (contrast != null) {
                return contrast;
            }
        }

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

        float value = Math.abs((fgL + 0.05f) / (bgL + 0.05f));

        contrastCache.put(key, value);

        return value;
    }

    public static boolean showOnWhite(int color) {
        return UtilsColor.computeContrast(color, Color.WHITE) > CONTRAST_THRESHOLD;
    }

    public static void tintMenu(Menu menu, ColorFilter colorFilter) {
        for (int index = 0; index < menu.size(); index++) {
            Drawable drawable = menu.getItem(index).getIcon();
            if (drawable != null) {
                drawable.mutate().setColorFilter(colorFilter);
            }
        }
    }

    public static void applyTheme(Resources.Theme theme,
            @AppSettings.ThemeBackground String background,
            @AppSettings.ThemeColor String colorPrimary,
            @AppSettings.ThemeColor String colorPrimaryDark,
            @AppSettings.ThemeColor String colorAccent) {

        theme.applyStyle(ThemeBackground.getTheme(background).getStyleBackground(), true);
        theme.applyStyle(ThemeColor.getTheme(colorPrimary).getStyleColorPrimary(), true);
        theme.applyStyle(ThemeColor.getTheme(colorPrimaryDark).getStyleColorPrimaryDark(), true);
        theme.applyStyle(ThemeColor.getTheme(colorAccent).getStyleColorAccent(), true);
    }

    public static Resources.Theme getThemeForColor(Resources resources, int color, FragmentListenerBase fragmentListenerBase) {
        return getThemeForColor(resources,
                color,
                fragmentListenerBase.getThemePrimary(),
                fragmentListenerBase.getThemePrimaryDark(),
                fragmentListenerBase.getThemeAccent());
    }

    public static Resources.Theme getThemeForColor(Resources resources,
            @ColorInt int color,
            @AppSettings.ThemeColor String colorPrimary,
            @AppSettings.ThemeColor String colorPrimaryDark,
            @AppSettings.ThemeColor String colorAccent) {
        Resources.Theme theme = resources.newTheme();

        if (UtilsColor.showOnWhite(color)) {
            applyTheme(theme,
                    AppSettings.THEME_DARK,
                    colorPrimary,
                    colorPrimaryDark,
                    colorAccent);
        }
        else {
            applyTheme(theme,
                    AppSettings.THEME_LIGHT,
                    colorPrimary,
                    colorPrimaryDark,
                    colorAccent);
        }

        return theme;
    }

    @Nullable
    public static Palette.Swatch getSwatch(Palette palette, Target... targets) {
        for (Target target : targets) {
            Palette.Swatch swatch = palette.getSwatchForTarget(target);
            if (swatch != null) {
                return swatch;
            }
        }

        return null;
    }

}
