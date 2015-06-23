package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public interface DisallowListener {
    void requestDisallowInterceptTouchEventVertical(boolean disallow);
    void requestDisallowInterceptTouchEventHorizontal(boolean disallow);
}
