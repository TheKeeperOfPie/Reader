package com.winsonchiu.reader;

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
}
