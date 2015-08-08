/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.ControllerComments;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.search.ControllerSearch;

/**
 * Created by TheKeeperOfPie on 6/10/2015.
 */
public interface FragmentListenerBase {
    void onNavigationBackClick();
    void openDrawer();
    ControllerLinks getControllerLinks();
    ControllerInbox getControllerInbox();
    ControllerComments getControllerComments();
    ControllerProfile getControllerProfile();
    ControllerSearch getControllerSearch();
    ControllerHistory getControllerHistory();
    ControllerUser getControllerUser();
    Reddit getReddit();
    AdapterLink.ViewHolderBase.EventListener getEventListenerBase();
    AdapterCommentList.ViewHolderComment.EventListener getEventListenerComment();
}
