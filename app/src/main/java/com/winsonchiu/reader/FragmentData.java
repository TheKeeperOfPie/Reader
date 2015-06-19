package com.winsonchiu.reader;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentData extends Fragment {

    public static final String TAG = FragmentData.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private ControllerComments controllerComments;
    private ControllerProfile controllerProfile;
    private ControllerInbox controllerInbox;
    private ControllerSearch controllerSearch;

    public FragmentData() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void initializeControllers(Activity activity) {
        controllerLinks = new ControllerLinks(activity, "", Sort.HOT);
        controllerComments = new ControllerComments(activity, "", "");
        controllerProfile = new ControllerProfile(activity);
        controllerInbox = new ControllerInbox(activity);
        controllerSearch = new ControllerSearch(activity);
        controllerComments.setControllerLinks(controllerLinks);
        controllerSearch.setControllerLinks(controllerLinks);
    }

    public void resetActivity(Activity activity) {
        if (controllerSearch == null) {
            initializeControllers(activity);
        }

        controllerLinks.setActivity(activity);
        controllerComments.setActivity(activity);
        controllerProfile.setActivity(activity);
        controllerInbox.setActivity(activity);
        controllerSearch.setActivity(activity);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity())
                .watch(this);
    }
}
