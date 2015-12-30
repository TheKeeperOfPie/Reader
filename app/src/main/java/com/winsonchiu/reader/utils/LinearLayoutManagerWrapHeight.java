package com.winsonchiu.reader.utils;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.views.CustomRelativeLayout;

/**
 *
 */
public class LinearLayoutManagerWrapHeight extends LinearLayoutManager {

    private static final String TAG = LinearLayoutManagerWrapHeight.class.getCanonicalName();

    private RecyclerView recyclerView;
    private int heightFirstChild = ViewGroup.LayoutParams.WRAP_CONTENT;
    private OnSizeChangedListener onSizeChangedListener;

    public LinearLayoutManagerWrapHeight(Context context) {
        super(context);
    }

    public LinearLayoutManagerWrapHeight(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public LinearLayoutManagerWrapHeight(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public static int makeUnspecifiedSpec() {
        return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        recyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        recyclerView = null;
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        registerView(child);
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        registerView(child);
    }

    @Override
    public void removeView(View child) {
        super.removeView(child);
        unregisterView(child);
    }

    private void registerView(View view) {
        if (view instanceof CustomRelativeLayout) {
            ((CustomRelativeLayout) view).setOnSizeChangedListener(new OnSizeChangedListener() {
                @Override
                public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                    if (recyclerView != null && height > 0) {
                        heightFirstChild = height;

                        recyclerView.getLayoutParams().height = height;
                        recyclerView.requestLayout();
                    }

                    if (onSizeChangedListener != null) {
                         onSizeChangedListener.onSizeChanged(width, height, oldWidth, oldHeight);
                    }
                }
            });
        }

        view.requestLayout();
    }

    private void unregisterView(View view) {
        if (view instanceof CustomRelativeLayout) {
            ((CustomRelativeLayout) view).setOnSizeChangedListener(null);
        }
    }

    public int getFirstChildHeight() {
        return heightFirstChild;
    }

    public void setOnSizeChangedListener(OnSizeChangedListener onSizeChangedListener) {
        this.onSizeChangedListener = onSizeChangedListener;
    }
}