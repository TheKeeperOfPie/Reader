package com.winsonchiu.reader;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Stack;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends AppCompatActivity
        implements FragmentThreadList.OnFragmentInteractionListener,
        FragmentWeb.OnFragmentInteractionListener,
        FragmentComments.OnFragmentInteractionListener,
        FragmentAuth.OnFragmentInteractionListener,
        FragmentProfile.OnFragmentInteractionListener,
        FragmentInbox.OnFragmentInteractionListener,
        FragmentSearch.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private CharSequence mTitle;
    private int oldId = -1;
    private Toolbar toolbar;
    private ControllerLinks controllerLinks;
    private ControllerComments controllerComments;
    private ControllerProfile controllerProfile;
    private ControllerInbox controllerInbox;
    private ControllerSearch controllerSearch;
    private SharedPreferences sharedPreferences;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private NavigationView viewNavigation;

    private ImageView imageNavHeader;
    private TextView textAccountName;
    private TextView textAccountInfo;

    private Stack<Object> stackHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        stackHistory = new Stack<>();

        if (controllerLinks == null) {
            controllerLinks = new ControllerLinks(this, "", Sort.HOT);
        }
        if (controllerComments == null) {
            controllerComments = new ControllerComments(this, "", "");
        }
        if (controllerProfile == null) {
            controllerProfile = new ControllerProfile(this);
        }
        if (controllerInbox == null) {
            controllerInbox = new ControllerInbox(this);
        }
        if (controllerSearch == null) {
            controllerSearch = new ControllerSearch(this, controllerLinks);
        }
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItem itemSearch = toolbar.getMenu()
                        .findItem(R.id.item_search);
                if (itemSearch != null) {
                    itemSearch.expandActionView();
                    SearchView searchView = ((SearchView) itemSearch.getActionView());
                    if (Reddit.FRONT_PAGE.equals(toolbar.getTitle())) {
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

        inflateNavigationDrawer();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mTitle = getTitle();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Log.d(TAG, "load intent: " + getIntent().toString());
            String urlString = getIntent().getDataString();
            if (URLUtil.isValidUrl(urlString)) {
                parseUrl(urlString);
            }
            else {
                Log.d(TAG, "Not valid URL: " + urlString);
            }
        }

        selectNavigationItem(R.id.item_home);

        /*
            Must be placed after ActionBarDrawerToggle instantiation,
            as the toggle will set its own OnClickListener. This is also
            why we must manually toggle the drawer after checking its
            visibility in onNavigationClick()
         */
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavigationClick();
            }
        });
    }

    private void inflateNavigationDrawer() {
        viewNavigation = (NavigationView) findViewById(R.id.navigation);

        View viewHeader = LayoutInflater.from(this).inflate(R.layout.header_navigation,
                viewNavigation, false);


        imageNavHeader = (ImageView) viewHeader.findViewById(R.id.image_nav_header);
        textAccountName = (TextView) viewHeader.findViewById(R.id.text_account_name);
        textAccountInfo = (TextView) viewHeader.findViewById(R.id.text_account_info);

        View.OnClickListener clickListenerAccount = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAccount();
            }
        };
        textAccountName.setOnClickListener(clickListenerAccount);
        textAccountInfo.setOnClickListener(clickListenerAccount);

        if (TextUtils.isEmpty(sharedPreferences.getString(AppSettings.REFRESH_TOKEN, ""))) {
            textAccountName.setText(R.string.add_account);
        }
        else {
            loadAccountInfo();
        }

        viewNavigation.addHeaderView(viewHeader);
        viewNavigation.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectNavigationItem(menuItem.getItemId());
                        return true;
                    }
                });

    }

    private void selectNavigationItem(int id) {
        getFragmentManager().popBackStackImmediate();
        switch (id) {
            case R.id.item_home:
                if (getFragmentManager().findFragmentByTag(FragmentThreadList.TAG) != null) {
                    controllerLinks.loadFrontPage(Sort.HOT);
                }
                else {
                    getFragmentManager().beginTransaction()
                            .replace(R.id.frame_fragment,
                                    FragmentThreadList.newInstance("", ""),
                                    FragmentThreadList.TAG)
                            .commit();
                }
                break;
            case R.id.item_profile:
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment,
                                FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .commit();
                break;
            case R.id.item_inbox:
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment,
                                FragmentInbox.newInstance("", ""),
                                FragmentInbox.TAG)
                        .commit();
                break;
            case R.id.item_settings:
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
                preferences.edit()
                        .putString(AppSettings.SUBSCRIBED_SUBREDDITS, "")
                        .apply();
                Toast.makeText(MainActivity.this, "Cleared refresh token", Toast.LENGTH_SHORT)
                        .show();
                break;
        }

        oldId = id;
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public void loadAccountInfo() {
        Reddit.getInstance(this).loadGet(Reddit.OAUTH_URL + "/api/v1/me",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            textAccountName.setText(jsonObject.getString("name"));
                            textAccountInfo.setText(jsonObject.getString(
                                    "link_karma") + " Link " + jsonObject.getString(
                                    "comment_karma") + " Comment");
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    private void onClickAccount() {
        getFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment, FragmentAuth.newInstance("", ""), FragmentAuth.TAG)
                .commit();
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void parseUrl(String urlString) {
        try {
            URL url = new URL(urlString);

            // TODO: Implement a history stack inside the Controllers

            if (!url.getHost().contains("reddit")) {
                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
                return;
            }

            String path = url.getPath();
            Log.d(TAG, "Path: " + path);
            int indexFirstSlash = path.indexOf("/", 1);
            int indexSecondSlash = path.indexOf("/", indexFirstSlash + 1);
            if (indexFirstSlash < 0) {
                controllerLinks.setParameters("", Sort.HOT);
                return;
            }
            String subreddit = path.substring(indexFirstSlash + 1,
                    indexSecondSlash > 0 ? indexSecondSlash : path.length());

            Log.d(TAG, "Subreddit: " + subreddit);

            if (path.contains("comments")) {
                int indexComments = path.indexOf("comments") + 9;
                int indexFourthSlash = path.indexOf("/", indexComments + 1);
                String id = path.substring(indexComments, indexFourthSlash > -1 ? indexFourthSlash : path.length());
                Log.d(TAG, "Comments ID: " + id);

                if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) == null) {
                    FragmentComments fragmentComments = FragmentComments.newInstance(subreddit,
                            id, false);

                    getFragmentManager().beginTransaction()
                            .add(R.id.frame_fragment, fragmentComments, FragmentComments.TAG)
                            .addToBackStack(null)
                            .commit();
                }
                else {
                    controllerComments.setLinkId(subreddit, id);
                }
            }
            else if (path.contains("/u/")) {
                int indexUser = path.indexOf("/u/") + 3;
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .addToBackStack(null)
                        .commit();
                controllerProfile.loadUser(urlString.substring(indexUser, path.indexOf("/", indexUser)));
            }
            else if (path.contains("/user/")) {
                int indexUser = path.indexOf("/user/") + 6;
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .addToBackStack(null)
                        .commit();
                controllerProfile.loadUser(urlString.substring(indexUser, path.indexOf("/", indexUser)));
            }
            else {
                int indexSort = path.indexOf("/", subreddit.length() + 1);
                String sort =
                        indexSort > -1 ? path.substring(subreddit.length() + 1, indexSort) :
                                "hot";
                controllerLinks.setParameters(subreddit, Sort.HOT);
                Log.d(TAG, "Sort: " + sort);
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void onNavigationClick() {
        Log.d(TAG, "Back stack count: " + getFragmentManager().getBackStackEntryCount());
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            mDrawerToggle.onDrawerSlide(mDrawerLayout, 0.0f);
        }
        else {
            if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
            else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }

    public void restoreActionBar() {
        setToolbarTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
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

        Log.d(TAG, "Menu item clicked: " + item.toString());

        switch (item.getItemId()) {
            case android.R.id.home:
                onNavigationClick();
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
            if (urlString.startsWith("/r/") || urlString.startsWith("/u/")) {
                urlString = "https://reddit.com" + urlString;
            }
            if (URLUtil.isValidUrl(urlString)) {
                parseUrl(urlString);
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
    public ControllerInbox getControllerInbox() {
        return controllerInbox;
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
        mDrawerToggle.onDrawerSlide(mDrawerLayout, value);
    }

    @Override
    public void onAuthFinished(boolean success) {
        selectNavigationItem(R.id.item_home);
        if (success) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT)
                    .show();
            loadAccountInfo();
        }
        else {
            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public ControllerSearch getControllerSearch() {
        return controllerSearch;
    }
}