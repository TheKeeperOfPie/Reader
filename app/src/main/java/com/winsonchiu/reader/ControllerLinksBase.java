package com.winsonchiu.reader;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerLinksBase {

    // TODO: Include default implementations

    Link getLink(int position);
    Reddit getReddit();
    void voteLink(final RecyclerView.ViewHolder viewHolder, final int vote);
    Drawable getDrawableForLink(Link link);
    int sizeLinks();
    boolean isLoading();
    void loadMoreLinks();
}