/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/17/2015.
 */
public class WebViewFixed extends WebView {

    private static final String TAG = WebViewFixed.class.getCanonicalName();
    ;
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

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (getHeight() > 0 && getHeight() < getMinimumHeight()) {
            setVisibility(GONE);

            ViewGroup.LayoutParams layoutParams = null;

            if (getLayoutParams() instanceof FrameLayout.LayoutParams) {
                layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            else if (getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                layoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
            }

            if (layoutParams != null) {
                setLayoutParams(layoutParams);
                reload();
            }
            Rect rect = new Rect();
            getParent().getChildVisibleRect(this, rect, null);
            getParent().invalidateChild(this, rect);
            setVisibility(VISIBLE);
        }
    }

    public void lockHeight() {
        if (maxHeight == Integer.MAX_VALUE) {
            maxHeight = AnimationUtils.getMeasuredHeight(this, 1.0f);
        }
        getLayoutParams().height = maxHeight;
        requestLayout();
    }

    public static WebViewFixed newInstance(Context context) {
        final WebViewFixed webViewFixed = new WebViewFixed(context.getApplicationContext());
        Reddit.incrementCreate();
        webViewFixed.setMinimumHeight(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                        context.getResources().getDisplayMetrics()));
        webViewFixed.getSettings()
                .setUseWideViewPort(true);
        webViewFixed.getSettings()
                .setLoadWithOverviewMode(true);
        webViewFixed.getSettings()
                .setBuiltInZoomControls(true);
        webViewFixed.getSettings()
                .setDisplayZoomControls(false);
        webViewFixed.setBackgroundColor(0x000000);
        webViewFixed.setInitialScale(1);
        return webViewFixed;
    }

}
