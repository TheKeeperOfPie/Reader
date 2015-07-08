/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by TheKeeperOfPie on 6/22/2015.
 */
public class OnTouchListenerDisallow implements View.OnTouchListener {

    private final DisallowListener disallowListener;
    private float startY;

    public OnTouchListenerDisallow(DisallowListener disallowListener) {
        this.disallowListener = disallowListener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        if (event.getPointerCount() > 1) {
            disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();

                if ((view.canScrollVertically(1) && view.canScrollVertically(
                        -1))) {
                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                }
                else {
                    disallowListener.requestDisallowInterceptTouchEventVertical(false);
                }
                disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                disallowListener.requestDisallowInterceptTouchEventHorizontal(false);
                break;
            case MotionEvent.ACTION_UP:
                disallowListener.requestDisallowInterceptTouchEventVertical(false);
                disallowListener.requestDisallowInterceptTouchEventHorizontal(false);
                break;
            case MotionEvent.ACTION_MOVE:
                disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                if (event.getY() - startY < 0 && view.canScrollVertically(1)) {
                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                }
                else if (event.getY() - startY > 0 && view.canScrollVertically(-1)) {
                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                }
                break;
        }
        return false;
    }

}
