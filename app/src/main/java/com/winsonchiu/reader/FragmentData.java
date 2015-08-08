/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.winsonchiu.reader.comments.ControllerComments;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.search.ControllerSearch;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentData extends FragmentBase {

    public static final String TAG = FragmentData.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private ControllerComments controllerComments;
    private ControllerProfile controllerProfile;
    private ControllerInbox controllerInbox;
    private ControllerSearch controllerSearch;
    private ControllerHistory controllerHistory;
    private ControllerUser controllerUser;

    public FragmentData() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void initializeControllers(Activity activity) {
        Log.d(TAG, "initializeControllers");

        controllerLinks = new ControllerLinks(activity, "", Sort.HOT);
        controllerComments = new ControllerComments(activity);
        controllerProfile = new ControllerProfile(activity);
        controllerInbox = new ControllerInbox(activity);
        controllerSearch = new ControllerSearch(activity);
        controllerHistory = new ControllerHistory(activity);
        controllerUser = new ControllerUser(activity);
        controllerProfile.setControllerUser(controllerUser);
        controllerSearch.setControllers(controllerLinks, controllerUser);
    }

    public void resetActivity(Activity activity) {
        if (controllerSearch == null) {
            Toast.makeText(activity, R.string.error_memory_recreation, Toast.LENGTH_SHORT).show();
            initializeControllers(activity);
        }
        else {
            controllerLinks.setActivity(activity);
            controllerComments.setActivity(activity);
            controllerProfile.setActivity(activity);
            controllerInbox.setActivity(activity);
            controllerSearch.setActivity(activity);
            controllerHistory.setActivity(activity);
            controllerUser.setActivity(activity);
        }
    }

    public ControllerLinks getControllerLinks() {
        return controllerLinks;
    }

    public void setControllerLinks(ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
    }

    public ControllerComments getControllerComments() {
        return controllerComments;
    }

    public void setControllerComments(ControllerComments controllerComments) {
        this.controllerComments = controllerComments;
    }

    public ControllerProfile getControllerProfile() {
        return controllerProfile;
    }

    public void setControllerProfile(ControllerProfile controllerProfile) {
        this.controllerProfile = controllerProfile;
    }

    public ControllerInbox getControllerInbox() {
        return controllerInbox;
    }

    public void setControllerInbox(ControllerInbox controllerInbox) {
        this.controllerInbox = controllerInbox;
    }

    public ControllerSearch getControllerSearch() {
        return controllerSearch;
    }

    public void setControllerSearch(ControllerSearch controllerSearch) {
        this.controllerSearch = controllerSearch;
    }

    public ControllerUser getControllerUser() {
        return controllerUser;
    }

    public void setControllerUser(ControllerUser controllerUser) {
        this.controllerUser = controllerUser;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public boolean navigateBack() {
        throw new IllegalStateException("FragmentData should never be in the back stack");
    }

    public ControllerHistory getControllerHistory() {
        return controllerHistory;
    }

    public void setControllerHistory(ControllerHistory controllerHistory) {
        this.controllerHistory = controllerHistory;
    }
}
