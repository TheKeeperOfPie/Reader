package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected Activity activity;
    protected LayoutManager layoutManager;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public abstract RecyclerView.ItemDecoration getItemDecoration();
}
