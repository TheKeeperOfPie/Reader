package com.winsonchiu.reader;

import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 6/10/2015.
 */
public interface FragmentListenerBase {
    void onNavigationBackClick();
    void openDrawer();
    void onAuthFinished(boolean success);
    ControllerLinks getControllerLinks();
    ControllerInbox getControllerInbox();
    ControllerComments getControllerComments();
    ControllerProfile getControllerProfile();
    ControllerSearch getControllerSearch();
    ControllerUser getControllerUser();
    Reddit getReddit();
    AdapterLink.ViewHolderBase.EventListener getEventListenerBase();
    AdapterCommentList.ViewHolderComment.EventListener getEventListenerComment();
}
