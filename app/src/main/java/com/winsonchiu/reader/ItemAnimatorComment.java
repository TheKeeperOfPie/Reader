package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public class ItemAnimatorComment extends RecyclerView.ItemAnimator {

    @Override
    public void runPendingAnimations() {

    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        return false;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder,
                               int fromX,
                               int fromY,
                               int toX,
                               int toY) {
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
                                 RecyclerView.ViewHolder newHolder,
                                 int fromLeft,
                                 int fromTop,
                                 int toLeft,
                                 int toTop) {
        return false;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {

    }

    @Override
    public void endAnimations() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
