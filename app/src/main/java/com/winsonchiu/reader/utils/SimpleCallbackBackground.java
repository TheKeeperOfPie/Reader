package com.winsonchiu.reader.utils;

import android.graphics.Paint;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Created by TheKeeperOfPie on 8/20/2015.
 */
public abstract class SimpleCallbackBackground extends ItemTouchHelper.SimpleCallback {

    protected Paint paint;

    public SimpleCallbackBackground(int dragDirs, int swipeDirs, int backgroundColor) {
        super(dragDirs, swipeDirs);
        paint = new Paint();
        paint.setColor(backgroundColor);
    }

}
