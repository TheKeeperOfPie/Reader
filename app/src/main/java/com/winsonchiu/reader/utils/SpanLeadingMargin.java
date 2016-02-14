package com.winsonchiu.reader.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan.LeadingMarginSpan2;

/**
 * Created by TheKeeperOfPie on 2/14/2016.
 */
public class SpanLeadingMargin implements LeadingMarginSpan2 {

    private boolean drawCalled;
    private int lineCount;
    private int lineTarget;
    private float margin;

    public SpanLeadingMargin(int lineTarget, float margin) {
        this.lineTarget = lineTarget;
        this.margin = margin;
    }

    @Override
    public int getLeadingMarginLineCount() {
        return lineTarget;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return first ? (int) margin : 0;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        drawCalled = true;
    }
}
