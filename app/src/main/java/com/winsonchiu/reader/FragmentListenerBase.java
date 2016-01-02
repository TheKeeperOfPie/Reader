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
    AdapterLink.ViewHolderBase.EventListener getEventListenerBase();
    AdapterCommentList.ViewHolderComment.EventListener getEventListener();
    Theme getAppColorTheme();
    String getThemeBackgroundPrefString();
    String getThemePrimaryPrefString();
    String getThemeAccentPrefString();
}