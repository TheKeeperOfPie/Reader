package com.winsonchiu.reader;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by TheKeeperOfPie on 3/17/2015.
 */
public class WebViewFixed extends WebView {

    private static final String TAG = WebViewFixed.class.getCanonicalName();

    private int maxHeight = Integer.MAX_VALUE;
    private RenderListener renderListener;

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

    public int getMaxScrollY() {
        return computeVerticalScrollRange() - getHeight();
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

    public void lockHeight() {
        if (maxHeight == Integer.MAX_VALUE) {
            maxHeight = getMeasuredHeight();
            setMeasuredDimension(getMeasuredWidth(), maxHeight);
        }
    }

    @Override
    public void invalidate(Rect dirty) {
        super.invalidate(dirty);
        checkRender();
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        super.invalidate(l, t, r, b);
        checkRender();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        checkRender();
    }

    private void checkRender() {
        if (getContentHeight() > 0 && getProgress() == 100 && renderListener != null) {
            renderListener.onRenderFinished();
        }
    }

    public void setRenderListener(RenderListener renderListener) {
        this.renderListener = renderListener;
    }

    public interface RenderListener {
        void onRenderFinished();
    }
}
