package com.winsonchiu.reader.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageButton;

import com.winsonchiu.reader.utils.TouchEventListener;

/**
 * Created by TheKeeperOfPie on 8/21/2015.
 */
public class CustomImageButton extends ImageButton {

    private TouchEventListener touchEventListener;

    public CustomImageButton(Context context) {
        super(context);
    }

    public CustomImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TouchEventListener getTouchEventListener() {
        return touchEventListener;
    }

    public void setTouchEventListener(TouchEventListener touchEventListener) {
        this.touchEventListener = touchEventListener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (touchEventListener != null) {
            touchEventListener.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

}
