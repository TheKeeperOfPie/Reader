package com.winsonchiu.reader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ImageViewSquare extends ImageView {
    public ImageViewSquare(Context context) {
        super(context);
    }

    public ImageViewSquare(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewSquare(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("NewApi")
    public ImageViewSquare(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
