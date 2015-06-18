package com.winsonchiu.reader;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends YouTubeBaseActivity
        implements FragmentThreadList.OnFragmentInteractionListener,
        FragmentWeb.OnFragmentInteractionListener,
        FragmentComments.OnFragmentInteractionListener,
        FragmentAuth.OnFragmentInteractionListener,
        FragmentProfile.OnFragmentInteractionListener,
        FragmentInbox.OnFragmentInteractionListener,
        FragmentSearch.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final String REDDIT_PAGE = "redditPage";

    private FragmentData fragmentData;

    private CharSequence mTitle;
    private int oldId = -1;

    private SharedPreferences sharedPreferences;
    private DrawerLayout mDrawerLayout;
    private NavigationView viewNavigation;

    private ImageView imageNavHeader;
    private TextView textAccountName;
    private TextView textAccountInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        fragmentData = (FragmentData) getFragmentManager().findFragmentByTag(FragmentData.TAG);
        if (fragmentData == null) {
            fragmentData = new FragmentData();
            getFragmentManager().beginTransaction().add(fragmentData, FragmentData.TAG).commit();
            fragmentData.initializeControllers(this);
        }
        else {
            fragmentData.resetActivity(this);
        }

        setContentView(R.layout.activity_main);

        inflateNavigationDrawer();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        mTitle = getTitle();

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
                Log.d(TAG, "load intent: " + getIntent().toString());
                String urlString = getIntent().getDataString();
                if (getIntent().hasExtra(REDDIT_PAGE)) {
                    urlString = getIntent().getExtras()
                            .getString(REDDIT_PAGE);
                }
                if (URLUtil.isValidUrl(urlString)) {
                    parseUrl(urlString);
                }
                else {
                    Log.d(TAG, "Not valid URL: " + urlString);
                    selectNavigationItem(R.id.item_home);
                }
            }
            else {
                selectNavigationItem(R.id.item_home);
            }
        }

    }

    private void inflateNavigationDrawer() {
        viewNavigation = (NavigationView) findViewById(R.id.navigation);

        View viewHeader = LayoutInflater.from(this)
                .inflate(R.layout.header_navigation,
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
                    fragmentData.getControllerLinks().loadFrontPage(Sort.HOT);
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
        Reddit.getInstance(this)
                .loadGet(Reddit.OAUTH_URL + "/api/v1/me",
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

            if (!url.getHost()
                    .contains("reddit")) {
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
                fragmentData.getControllerLinks().setParameters("", Sort.HOT);
                return;
            }
            String subreddit = path.substring(indexFirstSlash + 1,
                    indexSecondSlash > 0 ? indexSecondSlash : path.length());

            Log.d(TAG, "Subreddit: " + subreddit);

            if (path.contains("comments")) {
                int indexComments = path.indexOf("comments") + 9;
                int indexFourthSlash = path.indexOf("/", indexComments + 1);
                String id = path.substring(indexComments,
                        indexFourthSlash > -1 ? indexFourthSlash : path.length());
                Log.d(TAG, "Comments ID: " + id);

                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment,
                                FragmentComments.newInstance(subreddit, id, false),
                                FragmentComments.TAG)
                        .commit();
                fragmentData.getControllerComments().setLinkId(subreddit, id);
            }
            else if (path.contains("/u/")) {
                int indexUser = path.indexOf("/u/") + 3;
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .commit();
                fragmentData.getControllerProfile().loadUser(
                        urlString.substring(indexUser, path.indexOf("/", indexUser)));
            }
            else if (path.contains("/user/")) {
                int indexUser = path.indexOf("/user/") + 6;
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .commit();
                fragmentData.getControllerProfile().loadUser(
                        urlString.substring(indexUser, path.indexOf("/", indexUser)));
            }
            else {
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentThreadList.newInstance("", ""),
                                FragmentThreadList.TAG)
                        .commit();
                int indexSort = path.indexOf("/", subreddit.length() + 1);
                String sort =
                        indexSort > -1 ? path.substring(subreddit.length() + 1, indexSort) :
                                "hot";
                fragmentData.getControllerLinks().setParameters(subreddit, Sort.HOT);
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
//            mDrawerLayout.onDrawerSlide(mDrawerLayout, 0.0f);
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

            onNavigationBackClick();
        }
        else {
            onNavigationBackClick();
        }

    }

    @Override
    public void startActivity(Intent intent) {
        Log.d(TAG, "startActivity: " + intent.toString());

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && TextUtils.equals(
                getApplicationContext().getPackageName(), intent.getStringExtra(
                        Browser.EXTRA_APPLICATION_ID))) {
            String urlString = intent.getDataString();
            Intent intentActivity = new Intent(this, MainActivity.class);
            intentActivity.setAction(Intent.ACTION_VIEW);
            if (urlString.startsWith("/r/") || urlString.startsWith("/u/")) {
                intentActivity.putExtra(REDDIT_PAGE, Reddit.BASE_URL + urlString);
                startActivity(intentActivity);

            }
            else if (urlString.toLowerCase()
                    .contains("reddit")) {
                intentActivity.putExtra(REDDIT_PAGE, urlString);
                startActivity(intentActivity);
            }
            else if (URLUtil.isValidUrl(urlString)) {
                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        }
        else {
            super.startActivity(intent);
        }
    }

    @Override
    public ControllerLinks getControllerLinks() {
        return fragmentData.getControllerLinks();
    }

    @Override
    public ControllerInbox getControllerInbox() {
        return fragmentData.getControllerInbox();
    }

    @Override
    public ControllerComments getControllerComments() {
        return fragmentData.getControllerComments();
    }

    @Override
    public ControllerProfile getControllerProfile() {
        return fragmentData.getControllerProfile();
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
        return fragmentData.getControllerSearch();
    }

    @Override
    public void onNavigationBackClick() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        }
        else {
            if (isTaskRoot() && getFragmentManager().getBackStackEntryCount() <= 1) {
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
    public void openDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }
}