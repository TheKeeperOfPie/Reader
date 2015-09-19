/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.views;

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

import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.UtilsAnimation;

/**
 * Created by TheKeeperOfPie on 3/17/2015.
 */
public class WebViewFixed extends WebView {

    private static final String TAG = WebViewFixed.class.getCanonicalName();
    private OnFinishedListener onFinishedListener;
    private String data;
    private int maxHeight = Integer.MAX_VALUE;
    private boolean isSingular;
    private int lastInvalidateHeight = -1;

    private WebViewFixed(Context context, boolean isSingular, OnFinishedListener onFinishedListener) {
        super(context);
        this.isSingular = isSingular;
        this.onFinishedListener = onFinishedListener;
    }

    private WebViewFixed(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private WebViewFixed(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private WebViewFixed(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (getContentHeight() > 0) {
            if (lastInvalidateHeight < 0) {
                lastInvalidateHeight = getContentHeight();
                return;
            }

            /*if (getHeight() == 0) {
//                Toast.makeText(getContext(), "Error: height == 0", Toast.LENGTH_SHORT).show();
//                reload();
            }
            else*/ if (onFinishedListener != null) {
                onFinishedListener.onFinished();
                onFinishedListener = null;
            }
        }
//        else if (!TextUtils.isEmpty(data) && getProgress() == 100 && isShown() && isSingular) {
//            Toast.makeText(getContext(), "Data reload", Toast.LENGTH_SHORT).show();
//            data = null;
//            setVisibility(GONE);
//            loadData(data, "text/html", "UTF-8");
//            setVisibility(VISIBLE);
//        }
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

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        this.data = data;
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl,
            String data,
            String mimeType,
            String encoding,
            String historyUrl) {
        this.data = data;
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    public void lockHeight() {
        if (maxHeight == Integer.MAX_VALUE) {
            maxHeight = UtilsAnimation.getMeasuredHeight(this, 1.0f);
        }
        getLayoutParams().height = maxHeight;
        requestLayout();
    }

    public static WebViewFixed newInstance(Context context, boolean isSingular, OnFinishedListener onFinishedListener) {
        final WebViewFixed webViewFixed = new WebViewFixed(context, isSingular, onFinishedListener);
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


    public interface OnFinishedListener {
        void onFinished();
    }

}
