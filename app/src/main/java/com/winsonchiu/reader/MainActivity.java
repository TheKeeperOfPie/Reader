package com.winsonchiu.reader;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.net.MalformedURLException;
import java.net.URL;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends AppCompatActivity
        implements FragmentNavDrawer.NavigationDrawerCallbacks,
        FragmentThreadList.OnFragmentInteractionListener,
        FragmentWeb.OnFragmentInteractionListener,
        FragmentComments.OnFragmentInteractionListener,
        FragmentAuth.OnFragmentInteractionListener,
        FragmentProfile.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getCanonicalName();
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private FragmentNavDrawer mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private int oldPosition = -1;
    private Toolbar toolbar;
    private ControllerLinks controllerLinks;
    private ControllerComments controllerComments;
    private ControllerProfile controllerProfile;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (controllerLinks == null) {
            controllerLinks = new ControllerLinks(this, "", "hot");
        }
        if (controllerComments == null) {
            controllerComments = new ControllerComments(this, "", "");
        }
        if (controllerProfile == null) {
            controllerProfile = new ControllerProfile(this);
        }
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (FragmentNavDrawer)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItem itemSearch = toolbar.getMenu()
                        .findItem(R.id.item_search);
                if (itemSearch != null) {
                    itemSearch.expandActionView();
                    SearchView searchView = ((SearchView) itemSearch.getActionView());
                    if ("Front Page".equals(getTitle())) {
                        searchView.setQuery("", false);
                    }
                    else {
                        searchView.setQuery(toolbar.getTitle()
                                .toString()
                                .replaceAll("/r/", ""), false);
                    }
                }
            }
        });
        setSupportActionBar(toolbar);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                toolbar);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Log.d(TAG, "load intent: " + getIntent().toString());
            String urlString = getIntent().getDataString();
            if (URLUtil.isValidUrl(urlString)) {
                try {
                    URL url = new URL(urlString);
                    String path = url.getPath();
                    Log.d(TAG, "Path: " + path);
                    int indexFirstSlash = path.indexOf("/", 1);
                    int indexSecondSlash = path.indexOf("/", indexFirstSlash + 1);
                    if (indexFirstSlash < 0) {
                        controllerLinks.setParameters("", "hot");
                        return;
                    }
                    String subreddit = path.substring(indexFirstSlash + 1,
                            indexSecondSlash > 0 ? indexSecondSlash : path.length());

                    Log.d(TAG, "Subreddit: " + subreddit);

                    if (path.contains("comments")) {
                        int indexComments = path.indexOf("comments") + 8;
                        String id = path.substring(indexComments,
                                path.indexOf("/", indexComments) > -1 ?
                                        path.indexOf("/", indexComments) : path.length());
                        FragmentComments fragmentComments = FragmentComments.newInstance(subreddit,
                                id, false);

                        getFragmentManager().beginTransaction()
                                .add(R.id.frame_fragment, fragmentComments, FragmentComments.TAG)
                                .addToBackStack(null)
                                .commit();
                    }
                    else {
                        int indexSort = path.indexOf("/", subreddit.length() + 1);
                        String sort =
                                indexSort > -1 ? path.substring(subreddit.length() + 1, indexSort) :
                                        "hot";
                        controllerLinks.setParameters(subreddit, "hot");
                        Log.d(TAG, "Sort: " + sort);
                    }
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position, boolean force) {
        // TODO: update the main content by replacing fragments
        // TODO: Reimplement oldPosition check
        getFragmentManager().popBackStackImmediate();
//        if (oldPosition != position | force) {
        switch (position) {
            case 0:
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment,
                                FragmentThreadList.newInstance("", ""),
                                FragmentThreadList.TAG)
                        .commit();
                controllerLinks.loadFrontPage("hot");
                break;
            case 1:
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .commit();
                break;
            case 3:
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext());
                // TODO: Manually invalidate access token
                preferences.edit()
                        .putString(AppSettings.ACCESS_TOKEN, "")
                        .apply();
                preferences.edit()
                        .putString(AppSettings.REFRESH_TOKEN, "")
                        .apply();
                preferences.edit()
                        .putString(AppSettings.ACCOUNT_JSON, "")
                        .apply();
                Toast.makeText(this, "Cleared refresh token", Toast.LENGTH_SHORT)
                        .show();
                break;
        }
//        }
        oldPosition = position;
    }

    @Override
    public void onNavigationClick() {
        Log.d(TAG, "Back stack count: " + getFragmentManager().getBackStackEntryCount());
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            mNavigationDrawerFragment.setNavigationAnimation(0.0f);
        }
        else {
            if (mNavigationDrawerFragment.isDrawerOpen()) {
                mNavigationDrawerFragment.setDrawer(false);
            }
            else {
                mNavigationDrawerFragment.setDrawer(true);
            }
        }
    }

    public void restoreActionBar() {
        setToolbarTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            FragmentWeb fragmentWeb = (FragmentWeb) getFragmentManager().findFragmentByTag(
                    FragmentWeb.TAG);

            if (fragmentWeb != null && fragmentWeb.navigateBack()) {
                return;
            }

            getFragmentManager().popBackStack();
            Log.d(TAG, "popBackStack");
        }
        else {
            if (getFragmentManager().getBackStackEntryCount() <= 1) {
                new AlertDialog.Builder(this)
                        .setMessage("Exit Reader?")
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        MainActivity.super.onBackPressed();
                                    }
                                })
                        .setNegativeButton("No", null)
                        .show();

            }
            else {
                super.onBackPressed();
            }
        }

    }

    @Override
    public void startActivity(Intent intent) {
        Log.d(TAG, "startActivity: " + intent.toString());

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && TextUtils.equals(
                getApplicationContext().getPackageName(), intent.getStringExtra(
                        Browser.EXTRA_APPLICATION_ID))) {
            String urlString = intent.getDataString();
            if (URLUtil.isValidUrl(urlString)) {
                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }
            else if (urlString.startsWith("/r/")) {
                Log.d(TAG, "Path: " + urlString);
                int indexFirstSlash = urlString.indexOf("/", 1);
                int indexSecondSlash = urlString.indexOf("/", indexFirstSlash + 1);
                if (indexFirstSlash < 0) {
                    controllerLinks.setParameters("", "hot");
                    return;
                }
                String subreddit = urlString.substring(indexFirstSlash + 1,
                        indexSecondSlash > 0 ? indexSecondSlash : urlString.length());

                Log.d(TAG, "Subreddit: " + subreddit);

                if (urlString.contains("wiki")) {
                    urlString = "https://reddit.com" + urlString;
                    if (URLUtil.isValidUrl(urlString)) {
                        getFragmentManager().beginTransaction()
                                .add(R.id.frame_fragment, FragmentWeb.newInstance(urlString, ""),
                                        FragmentWeb.TAG)
                                .addToBackStack(null)
                                .commit();
                    }
                }
                else if (urlString
                        .contains("comments")) {
                    int indexComments = urlString.indexOf("comments") + 8;
                    String id = urlString.substring(indexComments,
                            urlString.indexOf("/", indexComments) > -1 ?
                                    urlString.indexOf("/", indexComments) : urlString.length());
                    FragmentComments fragmentComments = FragmentComments.newInstance(subreddit,
                            id, false);

                    getFragmentManager().beginTransaction()
                            .add(R.id.frame_fragment, fragmentComments, FragmentComments.TAG)
                            .addToBackStack(null)
                            .commit();
                }
                else {
                    int indexSort = urlString.indexOf("/", subreddit.length() + 1);
                    String sort =
                            indexSort > -1 ?
                                    urlString.substring(subreddit.length() + 1, indexSort) :
                                    "hot";
                    controllerLinks.setParameters(subreddit, "hot");
                    Log.d(TAG, "Sort: " + sort);
                }
            }
            else if (urlString.startsWith("/u/")) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .addToBackStack(null)
                        .commit();
                controllerProfile.loadUser(urlString.substring(3, urlString.length()));
            }
        }
        else {
            super.startActivity(intent);
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        mTitle = title;
        toolbar.setTitle(mTitle);
    }

    @Override
    public ControllerLinks getControllerLinks() {
        return controllerLinks;
    }

    @Override
    public ControllerComments getControllerComments() {
        return controllerComments;
    }

    @Override
    public ControllerProfile getControllerProfile() {
        return controllerProfile;
    }

    @Override
    public void setNavigationAnimation(float value) {
        mNavigationDrawerFragment.setNavigationAnimation(value);
    }

    @Override
    public void onAuthFinished(boolean success) {
        onNavigationDrawerItemSelected(0, true);
        if (success) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT)
                    .show();
            mNavigationDrawerFragment.loadAccountInfo();
        }
        else {
            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}