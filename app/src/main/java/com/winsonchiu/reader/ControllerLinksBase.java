package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerLinksBase {

    // TODO: Include default implementations

    Link getLink(int position);
    Reddit getReddit();
    int sizeLinks();
    boolean isLoading();
    void loadMoreLinks();
    Subreddit getSubreddit();
    boolean showSubreddit();
}