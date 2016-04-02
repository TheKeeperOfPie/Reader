/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.links.AdapterLink;

/**
 * Created by TheKeeperOfPie on 6/10/2015.
 */
public interface FragmentListenerBase {
    void onNavigationBackClick();
    void openDrawer();
    Reddit getReddit();
    AdapterLink.ViewHolderLink.EventListener getEventListenerBase();
    AdapterCommentList.ViewHolderComment.EventListener getEventListener();
    @AppSettings.ThemeBackground String getThemeBackground();
    @AppSettings.ThemeColor String getThemePrimary();
    @AppSettings.ThemeColor String getThemePrimaryDark();
    @AppSettings.ThemeColor String getThemeAccent();
}