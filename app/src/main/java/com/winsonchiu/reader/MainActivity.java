package com.winsonchiu.reader;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends YouTubeBaseActivity
        implements FragmentListenerBase {

    public static final String REDDIT_PAGE = "redditPage";

    private static final String TAG = MainActivity.class.getCanonicalName();

    private FragmentData fragmentData;

    private int loadId = -1;

    private SharedPreferences sharedPreferences;
    private DrawerLayout mDrawerLayout;
    private NavigationView viewNavigation;

    private ImageView imageNavHeader;
    private TextView textAccountName;
    private TextView textAccountInfo;

    private WebView webView;
    private Reddit reddit;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        reddit = Reddit.getInstance(this);
        webView = new WebView(getApplicationContext());
        Reddit.incrementCreate();

        fragmentData = (FragmentData) getFragmentManager().findFragmentByTag(FragmentData.TAG);
        if (fragmentData == null) {
            fragmentData = new FragmentData();
            getFragmentManager().beginTransaction().add(fragmentData, FragmentData.TAG).commit();
            fragmentData.initializeControllers(this);
            Log.d(TAG, "FragmentData NOT FOUND");
        }
        else {
            Log.d(TAG, "FragmentData FOUND");
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
                if (loadId >= 0) {
                    selectNavigationItem(loadId);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

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
                    getControllerLinks().loadFrontPage(Sort.HOT, true);
                    selectNavigationItem(R.id.item_home);
                }
            }
            else {
                getControllerLinks().loadFrontPage(Sort.HOT, true);
                selectNavigationItem(R.id.item_home);
            }
        }

        if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) != null) {

            hideFragment(FragmentThreadList.TAG);
            hideFragment(FragmentProfile.TAG);
            hideFragment(FragmentInbox.TAG);
            hideFragment(FragmentSearch.TAG);

        }

        eventListenerBase = new AdapterLink.ViewHolderBase.EventListener() {

            @Override
            public void sendComment(String name, String text) {
                reddit.sendComment(name, text, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Comment newComment = Comment.fromJson(
                                    jsonObject.getJSONObject("json")
                                            .getJSONObject("data")
                                            .getJSONArray("things")
                                            .getJSONObject(0), 0);
                            getControllerComments().insertComment(newComment);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, null);
            }

            @Override
            public void onClickComments(Link link, AdapterLink.ViewHolderBase viewHolderBase) {

                Log.d(TAG, "onClickComments: " + link);

                if (link.getNumComments() == 0) {
                    if (!link.isCommentsClicked()) {
                        Toast.makeText(MainActivity.this, getString(R.string.no_comments),
                                Toast.LENGTH_SHORT)
                                .show();
                        link.setCommentsClicked(true);
                        return;
                    }
                }

                // TODO: Implemented pausing ViewHolders on Fragment hide
//                adapterLink.pauseViewHolders();
                getControllerComments()
                        .setLink(link);

                int color = viewHolderBase.getBackgroundColor();

                FragmentComments fragmentComments = FragmentComments
                        .newInstance(viewHolderBase, color);
                fragmentComments.setFragmentToHide(getFragmentManager().findFragmentById(R.id.frame_fragment));

                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, fragmentComments,
                                FragmentComments.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void save(Link link) {
                link.setSaved(!link.isSaved());
                if (link.isSaved()) {
                    reddit.save(link, "", new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });
                }
                else {
                    reddit.unsave(link);
                }
            }

            @Override
            public void save(Comment comment) {
                comment.setSaved(!comment.isSaved());
                if (comment.isSaved()) {
                    reddit.save(comment, "", new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });
                }
                else {
                    reddit.unsave(comment);
                }
            }

            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void downloadImage(final Link link) {

                Picasso.with(MainActivity.this)
                        .load(link.getUrl())
                        .into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                                boolean created = false;
                                File file = new File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES)
                                        .getAbsolutePath() + "/ReaderForReddit/" + link.getId() + ".png");

                                file.getParentFile()
                                        .mkdirs();

                                FileOutputStream out = null;
                                try {
                                    out = new FileOutputStream(file);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                                    created = true;
                                }
                                catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                finally {
                                    try {
                                        if (out != null) {
                                            out.close();
                                        }
                                    }
                                    catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (created) {
                                    MediaScannerConnection.scanFile(MainActivity.this,
                                            new String[]{file.toString()}, null, null);
                                }

                                Toast.makeText(MainActivity.this, "Image downloaded",
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onBitmapFailed(Drawable errorDrawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                            }
                        });
            }

            @Override
            public Reddit getReddit() {
                return reddit;
            }

            @Override
            public WebViewFixed getNewWebView(DisallowListener disallowListener) {
                return WebViewFixed.newInstance(getApplicationContext(), disallowListener);
            }

            @Override
            public void toast(String text) {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean isUserLoggedIn() {
                return !TextUtils.isEmpty(sharedPreferences.getString(AppSettings.ACCOUNT_JSON, ""));
            }

            @Override
            public void voteLink(AdapterLink.ViewHolderBase viewHolderBase, Link link, int vote) {
                reddit.voteLink(viewHolderBase, link, vote,
                        new Reddit.VoteResponseListener() {
                            @Override
                            public void onVoteFailed() {
                                Toast.makeText(MainActivity.this, "Error voting", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
            }

            @Override
            public void startActivity(Intent intent) {
                MainActivity.this.startActivity(intent);
            }

            @Override
            public void deletePost(Link link) {
                getControllerLinks().deletePost(link);
            }
        };

        eventListenerComment = new AdapterCommentList.ViewHolderComment.EventListener() {
            @Override
            public void loadNestedComments(Comment comment) {
                getControllerComments().loadNestedComments(comment);
            }

            @Override
            public boolean isCommentExpanded(int position) {
                return getControllerComments().isCommentExpanded(position);
            }

            @Override
            public boolean hasChildren(Comment comment) {
                return getControllerComments().hasChildren(comment);
            }

            @Override
            public void voteComment(AdapterCommentList.ViewHolderComment viewHolderComment,
                    Comment comment,
                    int vote) {
                getControllerComments().voteComment(viewHolderComment, comment, vote);
            }

            @Override
            public boolean toggleComment(int position) {
                return getControllerComments().toggleComment(position);
            }

            @Override
            public void deleteComment(Comment comment) {
                getControllerComments().deleteComment(comment);
            }

            @Override
            public void editComment(Comment comment, String text) {
                getControllerComments().editComment(comment, text);
            }

        };

    }

    private void hideFragment(String tag) {
        Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            getFragmentManager().beginTransaction().hide(fragment).commit();
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

        loadAccountInfo();

        viewNavigation.addHeaderView(viewHeader);
        viewNavigation.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        loadId = menuItem.getItemId();
                        return true;
                    }
                });

    }

    private void selectNavigationItem(final int id) {
        getFragmentManager().popBackStackImmediate();
        switch (id) {
            case R.id.item_home:
                if (getFragmentManager()
                        .findFragmentByTag(FragmentThreadList.TAG) != null) {
                    getControllerLinks().loadFrontPage(Sort.HOT, false);
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

                if (!TextUtils.isEmpty(
                        sharedPreferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
                    try {
                        getControllerProfile().setUser(User.fromJson(
                                new JSONObject(sharedPreferences
                                        .getString(AppSettings.ACCOUNT_JSON, ""))));
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                break;
            case R.id.item_inbox:
                getControllerInbox().reload();
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment,
                                FragmentInbox.newInstance("", ""),
                                FragmentInbox.TAG)
                        .commit();
                break;
            case R.id.item_settings:
                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(
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
                getControllerLinks().loadFrontPage(Sort.HOT, true);
                getControllerSearch().reloadSubscriptionList();
                Toast.makeText(MainActivity.this, "Cleared refresh token",
                        Toast.LENGTH_SHORT)
                        .show();
                break;
        }

        loadId = -1;
    }

    public void loadAccountInfo() {
        boolean visible = !TextUtils.isEmpty(sharedPreferences.getString(AppSettings.REFRESH_TOKEN, ""));
        if (visible) {
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
        else {
            textAccountName.setText(R.string.add_account);
        }

        viewNavigation.getMenu().findItem(R.id.item_profile).setVisible(visible);
        viewNavigation.getMenu().findItem(R.id.item_profile).setEnabled(visible);
        viewNavigation.getMenu().findItem(R.id.item_inbox).setVisible(visible);
        viewNavigation.getMenu().findItem(R.id.item_inbox).setEnabled(visible);
        viewNavigation.getMenu().findItem(R.id.item_settings).setVisible(visible);
        viewNavigation.getMenu().findItem(R.id.item_settings).setEnabled(visible);
    }

    private void onClickAccount() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction()
                .add(R.id.frame_fragment, FragmentAuth.newInstance("", ""), FragmentAuth.TAG)
                .addToBackStack(null);

        Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_fragment);
        if (fragment != null) {
            fragmentTransaction.hide(fragment);
        }

        fragmentTransaction.commit();
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void parseUrl(String urlString) {
        try {
            URL url = new URL(urlString);

            // TODO: Implement a history stack inside the Controllers

            if (!url.getHost()
                    .contains("redd")) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString, ""), FragmentWeb.TAG)
                        .addToBackStack(null);

                Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_fragment);
                if (fragment != null) {
                    fragmentTransaction.hide(fragment);
                }

                fragmentTransaction.commit();
                return;
            }

            // TODO: Handle special cases like redd.it and / for Front Page

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
                                FragmentComments.newInstance(subreddit, id, false, getResources().getColor(R.color.darkThemeBackground), 0, 0, 0),
                                FragmentComments.TAG)
                        .commit();
                fragmentData.getControllerComments().setLinkId(subreddit, id);
            }
            else if (path.contains("/u/")) {
                int indexUser = path.indexOf("u/") + 2;
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .commit();
                fragmentData.getControllerProfile().loadUser(
                        path.substring(indexUser, path.indexOf("/", indexUser)));
            }
            else if (path.contains("/user/")) {
                int indexUser = path.indexOf("user/") + 5;
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance("", ""),
                                FragmentProfile.TAG)
                        .commit();

                int endIndex = path.indexOf("/", indexUser);
                if (endIndex > -1) {
                    fragmentData.getControllerProfile()
                            .loadUser(
                                    path.substring(indexUser, endIndex));
                }
                else {
                    fragmentData.getControllerProfile()
                            .loadUser(
                                    path.substring(indexUser));
                }
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
                Log.d(TAG, "Sort: " + sort);
                fragmentData.getControllerLinks().setParameters(subreddit, Sort.HOT);
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
            // Fetch top Fragment to see if we should override the back action
            FragmentBase fragmentBase = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragmentBase != null && !fragmentBase.navigateBack()) {
                return;
            }
        }

        onNavigationBackClick();

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
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString, ""), FragmentWeb.TAG)
                        .addToBackStack(null);

                Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_fragment);
                if (fragment != null) {
                    fragmentTransaction.hide(fragment);
                }

                fragmentTransaction.commit();
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
            getControllerUser().reloadUser();
            getControllerSearch().reloadSubscriptionList();
            getControllerLinks().loadFrontPage(Sort.HOT, true);
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
    public ControllerUser getControllerUser() {
        return fragmentData.getControllerUser();
    }

    @Override
    public Reddit getReddit() {
        return reddit;
    }

    @Override
    public AdapterLink.ViewHolderBase.EventListener getEventListenerBase() {
        return eventListenerBase;
    }

    @Override
    public AdapterCommentList.ViewHolderComment.EventListener getEventListenerComment() {
        return eventListenerComment;
    }

    @Override
    public void onNavigationBackClick() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();
            Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragment != null) {
                getFragmentManager().beginTransaction().show(fragment).commit();
                Log.d(TAG, "Fragment shown");
            }
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

    @Override
    protected void onResume() {
        super.onResume();
        webView.resumeTimers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        Reddit.incrementDestroy();
        super.onDestroy();
    }
}