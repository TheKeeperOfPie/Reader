package com.winsonchiu.reader;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by TheKeeperOfPie on 3/24/2015.
 */

public class TextViewShadow extends TextView {

    private static final int NUM_TO_DRAW = 5;

    public TextViewShadow(Context context) {
        super(context);
    }

    public TextViewShadow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewShadow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextViewShadow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        for (int num = 0; num < NUM_TO_DRAW; num++) {
            super.onDraw(canvas);
//        }
    }
}
