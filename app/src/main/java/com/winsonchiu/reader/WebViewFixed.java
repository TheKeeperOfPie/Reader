package com.winsonchiu.reader;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by TheKeeperOfPie on 3/17/2015.
 */
public class WebViewFixed extends WebView {

    private static final String TAG = WebViewFixed.class.getCanonicalName();

    private int maxHeight = Integer.MAX_VALUE;

    public WebViewFixed(Context context) {
        super(context);
    }

    public WebViewFixed(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebViewFixed(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebViewFixed(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void resetMaxHeight() {
        this.maxHeight = Integer.MAX_VALUE;
        setMeasuredDimension(getMeasuredWidth(), 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight == Integer.MAX_VALUE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        else {
            setMeasuredDimension(getMeasuredWidth(), maxHeight);
        }
    }

    public void setMaxHeight(int height) {
        maxHeight = height;
    }

    public void lockHeight() {
        if (maxHeight == Integer.MAX_VALUE) {
            maxHeight = AnimationUtils.getMeasuredHeight(this, 1.0f);
        }
        setMeasuredDimension(getMeasuredWidth(), maxHeight);
        getLayoutParams().height = maxHeight;
        requestLayout();
    }

}
