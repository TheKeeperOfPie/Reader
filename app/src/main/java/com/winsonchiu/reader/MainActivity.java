/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.ControllerComments;
import com.winsonchiu.reader.comments.FragmentComments;
import com.winsonchiu.reader.comments.FragmentReply;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.history.FragmentHistory;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.inbox.FragmentInbox;
import com.winsonchiu.reader.inbox.Receiver;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.links.FragmentThreadList;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.profile.FragmentProfile;
import com.winsonchiu.reader.search.ControllerSearch;
import com.winsonchiu.reader.search.FragmentSearch;
import com.winsonchiu.reader.settings.ActivitySettings;
import com.winsonchiu.reader.views.WebViewFixed;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends YouTubeBaseActivity
        implements FragmentListenerBase {

    public static final String REDDIT_PAGE = "redditPage";
    public static final String NAV_ID = "navId";
    public static final String NAV_PAGE = "navPage";

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int REQUEST_SETTINGS = 0;

    private FragmentData fragmentData;

    private int loadId = -1;

    private SharedPreferences sharedPreferences;
    private DrawerLayout mDrawerLayout;
    private NavigationView viewNavigation;

    private ImageView imageNavHeader;
    private TextView textAccountName;
    private TextView textAccountInfo;

    private Reddit reddit;
    private Handler handler;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;
    private final Runnable runnableInbox = new Runnable() {
        @Override
        public void run() {
            Receiver.checkInbox(MainActivity.this, null);
            handler.postDelayed(this, 60000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Theme theme = Theme.fromString(sharedPreferences.getString(AppSettings.PREF_THEME_PRIMARY, AppSettings.THEME_DEEP_PURPLE));
        if (theme != null) {
            String themeBackground = sharedPreferences.getString(AppSettings.PREF_THEME_BACKGROUND, AppSettings.THEME_DARK);
            String themeAccent = sharedPreferences.getString(AppSettings.PREF_THEME_ACCENT, AppSettings.THEME_YELLOW);

            setTheme(theme.getStyle(themeBackground, themeAccent));
        }
        Fabric.with(this, new Crashlytics());

        super.onCreate(savedInstanceState);

        reddit = Reddit.getInstance(this);
        handler = new Handler();

        fragmentData = (FragmentData) getFragmentManager().findFragmentByTag(FragmentData.TAG);
        if (fragmentData == null) {
            fragmentData = new FragmentData();
            getFragmentManager().beginTransaction().add(fragmentData, FragmentData.TAG).commit();
            fragmentData.initializeControllers(this);
            Log.d(TAG, "FragmentData not found, initialized");
        }
        else {
            Log.d(TAG, "FragmentData found, resetting Activity");
            fragmentData.resetActivity(this);
        }

        setContentView(R.layout.activity_main);

        inflateNavigationDrawer();

        Receiver.setAlarm(this);

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
                if (loadId != 0) {
                    selectNavigationItem(loadId, null, true);
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
                    selectNavigationItem(getIntent().getIntExtra(NAV_ID, R.id.item_home), getIntent().getStringExtra(
                            NAV_PAGE), false);
                }
            }
            else {
                getControllerLinks().loadFrontPage(Sort.HOT, true);
                selectNavigationItem(getIntent().getIntExtra(NAV_ID, R.id.item_home), getIntent().getStringExtra(
                        NAV_PAGE), false);
            }
        }

        eventListenerBase = new AdapterLink.ViewHolderBase.EventListener() {

            @Override
            public void sendComment(String name, String text) {
                reddit.sendComment(name, text, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Comment newComment = Comment.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class).get("json")
                                            .get("data")
                                            .get("things")
                                            .get(0), 0);
                            if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) != null) {
                                getControllerComments().insertComment(newComment);
                            }
                            if (getFragmentManager().findFragmentByTag(FragmentProfile.TAG) != null) {
                                getControllerProfile().insertComment(newComment);
                            }
                            if (getFragmentManager().findFragmentByTag(FragmentInbox.TAG) != null) {
                                getControllerInbox().insertComment(newComment);
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, R.string.failed_reply, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void sendMessage(String name, String text) {

                getReddit().sendComment(name, text, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                        Message newMessage = Message.fromJson(Reddit.getObjectMapper().readValue(
                                        response, JsonNode.class).get("json")
                                        .get("data")
                                        .get("things")
                                        .get(0));
                        getControllerInbox()
                                .insertMessage(newMessage);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, R.string.failed_message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onClickComments(Link link, final AdapterLink.ViewHolderBase viewHolderBase) {

                Log.d(TAG, "onClickComments: " + link);

                getControllerComments()
                        .setLink(link);

                int color = viewHolderBase.getBackgroundColor();

                FragmentBase fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
                fragment.onWindowTransitionStart();

                FragmentComments fragmentComments = FragmentComments
                        .newInstance(viewHolderBase, color);
                fragmentComments.setFragmentToHide(fragment, viewHolderBase.itemView);

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
            public void loadUrl(String urlString) {
                if (sharedPreferences.getBoolean(AppSettings.PREF_EXTERNAL_BROWSER, false)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(urlString));
                    startActivity(intent);
                    return;
                }
                Log.d(TAG, "loadUrl: " + loadId);

                URL url = null;
                try {
                    url = new URL(urlString);
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (url != null && url.getHost().contains("reddit.com")) {
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(REDDIT_PAGE, urlString);
                    startActivity(intent);
                    return;
                }

                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void downloadImage(final String fileName, final String url) {

                Toast.makeText(MainActivity.this, getString(R.string.image_downloading), Toast.LENGTH_SHORT).show();

                ImageRequest imageRequest = new ImageRequest(url,
                        new Response.Listener<Bitmap>() {
                            @Override
                            public void onResponse(Bitmap response) {

                                boolean created = false;
                                String path = Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES)
                                        .getAbsolutePath() + "/ReaderForReddit/" + fileName;

                                int index = url.lastIndexOf(".");
                                if (index > -1) {
                                    String extension = url.substring(index, index + 4);
                                    path += extension;
                                }
                                else {
                                    path += ".png";
                                }

                                File file = new File(path);

                                file.getParentFile()
                                        .mkdirs();

                                FileOutputStream out = null;
                                try {
                                    out = new FileOutputStream(file);
                                    response.compress(Bitmap.CompressFormat.PNG, 90, out);
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

                                Toast.makeText(MainActivity.this, getString(R.string.image_downloaded),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.ARGB_8888,
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(MainActivity.this, getString(R.string.error_downloading), Toast.LENGTH_SHORT).show();
                            }
                        });

                reddit.getRequestQueue().add(imageRequest);

            }

            @Override
            public Reddit getReddit() {
                return reddit;
            }

            @Override
            public WebViewFixed getNewWebView(WebViewFixed.OnFinishedListener onFinishedListener) {
                return WebViewFixed.newInstance(getApplicationContext(), onFinishedListener);
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
                                Toast.makeText(MainActivity.this, getString(R.string.error_voting), Toast.LENGTH_SHORT)
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

            @Override
            public void report(Thing thing, String reason, String otherReason) {
                Map<String, String> params = new HashMap<>();
                params.put("api_type", "json");
                params.put("thing_id", thing.getName());
                params.put("reason", reason);
                if (!TextUtils.isEmpty(otherReason)) {
                    params.put("other_reason", otherReason);
                }

                reddit.loadPost(Reddit.OAUTH_URL + "/api/report", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, params, 0);
            }

            @Override
            public void hide(Link link) {
                link.setHidden(!link.isHidden());
                if (link.isHidden()) {
                    reddit.hide(link);
                }
                else {
                    reddit.unhide(link);
                }

            }

            @Override
            public void editLink(Link link) {
                FragmentNewPost fragmentNewPost = FragmentNewPost.newInstanceEdit(getControllerUser().getUser().getName(), link);

                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, fragmentNewPost, FragmentNewPost.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void showReplyEditor(Replyable replyable) {

                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, FragmentReply.newInstance(replyable),
                                FragmentReply.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void markRead(Thing thing) {
                reddit.markRead(thing.getName());
            }

            @Override
            public void markNsfw(final Link link) {
                link.setOver18(!link.isOver18());
                if (link.isOver18()) {
                    getReddit().markNsfw(link, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            link.setOver18(false);
                            Toast.makeText(MainActivity.this, R.string.error_marking_nsfw,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else {
                    getReddit().unmarkNsfw(link, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            link.setOver18(true);
                            Toast.makeText(MainActivity.this, R.string.error_unmarking_nsfw, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                if (getFragmentManager().findFragmentByTag(FragmentThreadList.TAG) != null) {
                    getControllerLinks().setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) != null) {
                    getControllerComments().setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentProfile.TAG) != null) {
                    getControllerProfile().setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentHistory.TAG) != null) {
                    getControllerHistory().setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentSearch.TAG) != null) {
                    getControllerSearch().setNsfwLinks(link.getName(), link.isOver18());
                    getControllerSearch().setNsfwLinksSubreddit(link.getName(), link.isOver18());
                }

            }

        };

        eventListenerComment = new AdapterCommentList.ViewHolderComment.EventListener() {
            @Override
            public void loadNestedComments(Comment comment) {

                if (comment.getCount() == 0) {
                    Intent intentCommentThread = new Intent(MainActivity.this, MainActivity.class);
                    intentCommentThread.setAction(Intent.ACTION_VIEW);
                    // Double slashes used to trigger parseUrl correctly
                    intentCommentThread.putExtra(REDDIT_PAGE, Reddit.BASE_URL + "/r/" + getControllerComments().getSubredditName() + "/comments/" + getControllerComments().getLink().getId() + "//" + comment.getParentId() + "/");
                    startActivity(intentCommentThread);
                }
                else {
                    getControllerComments().loadNestedComments(comment);
                }
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
            public void editComment(String name, int level, String text) {
                getControllerComments().editComment(name, level, text);
            }

            @Override
            public void jumpToParent(Comment comment) {
                getControllerComments().jumpToParent(comment);
            }

        };

        if (sharedPreferences.getBoolean(AppSettings.BETA_NOTICE_0, true)) {
            try {

                View view = LayoutInflater.from(this).inflate(R.layout.dialog_text_alert, null, false);
                TextView textTitle = (TextView) view.findViewById(R.id.text_title);
                TextView textMessage = (TextView) view.findViewById(R.id.text_message);

                textTitle.setTextColor(getResources().getColor(R.color.colorPrimary));
                textTitle.setText("Reader (BETA)");

                textMessage.setText(R.string.beta_notice_0);

                new AlertDialog.Builder(this)
                        .setView(view)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
            finally {
                sharedPreferences.edit().putBoolean(AppSettings.BETA_NOTICE_0, false).apply();
            }
        }

    }

    private void inflateNavigationDrawer() {
        viewNavigation = (NavigationView) findViewById(R.id.navigation);

        // TODO: Adhere to guidelines by making the increment 56dp on mobile and 64dp on tablet
        float standardIncrement = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        float screenWidth = getResources().getDisplayMetrics().widthPixels;

        TypedArray typedArray = getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        float marginEnd = typedArray.getDimension(0, standardIncrement);
        typedArray.recycle();

        float navigationWidth = screenWidth - marginEnd;
        if (navigationWidth > standardIncrement * 6) {
            navigationWidth = standardIncrement * 6;
        }

        viewNavigation.getLayoutParams().width = (int) navigationWidth;

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
        viewHeader.setOnClickListener(clickListenerAccount);
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

    private void selectNavigationItem(final int id, String page, boolean animate) {

        loadId = 0;

        if (id == R.id.item_settings) {
            Intent intentSettings = new Intent(this, ActivitySettings.class);
            startActivityForResult(intentSettings, REQUEST_SETTINGS);
            return;
        }

        getFragmentManager().popBackStackImmediate();

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        if (animate) {
            fragmentTransaction.setCustomAnimations(R.animator.slide_from_left, R.animator.slide_to_right);
        }

        switch (id) {
            case R.id.item_home:
                if (getFragmentManager().findFragmentByTag(FragmentThreadList.TAG) != null) {
                    getControllerLinks().loadFrontPage(Sort.HOT, false);
                }
                else {
                    fragmentTransaction.replace(R.id.frame_fragment,
                            FragmentThreadList.newInstance(),
                            FragmentThreadList.TAG);
                }
                break;
            case R.id.item_history:
                getControllerHistory().reload();
                if (getFragmentManager().findFragmentByTag(FragmentHistory.TAG) == null) {
                    fragmentTransaction.replace(R.id.frame_fragment,
                            FragmentHistory.newInstance(),
                            FragmentHistory.TAG);
                }
                break;
            case R.id.item_profile:

                if (!TextUtils.isEmpty(
                        sharedPreferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
                    try {
                        getControllerProfile().setUser(User.fromJson(
                                Reddit.getObjectMapper().readValue(sharedPreferences.getString(
                                        AppSettings.ACCOUNT_JSON, ""),
                                        JsonNode.class)));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                fragmentTransaction.replace(R.id.frame_fragment,
                        FragmentProfile.newInstance(),
                        FragmentProfile.TAG);

                break;
            case R.id.item_inbox:
                Log.d(TAG, "Page: " + page);

                if (!TextUtils.isEmpty(page)) {
                    // TODO: Add other cases
                    switch (page) {
                        case ControllerInbox.INBOX:
                            getControllerInbox()
                                    .setPage(new Page(page, getString(R.string.inbox_page_inbox)));
                            break;
                        case ControllerInbox.UNREAD:
                            getControllerInbox()
                                    .setPage(new Page(page, getString(R.string.inbox_page_unread)));
                            break;
                        case ControllerInbox.SENT:
                            getControllerInbox()
                                    .setPage(new Page(page, getString(R.string.inbox_page_sent)));
                            break;
                    }
                }

                getControllerInbox().reload();
                fragmentTransaction.replace(R.id.frame_fragment,
                        FragmentInbox.newInstance(),
                        FragmentInbox.TAG);
                break;
        }

        fragmentTransaction.commit();
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
            textAccountName.setText(R.string.login);
        }

        viewNavigation.getMenu().findItem(R.id.item_inbox).setVisible(visible);
        viewNavigation.getMenu().findItem(R.id.item_inbox).setEnabled(visible);
    }

    private void onClickAccount() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction()
                .add(R.id.frame_fragment, FragmentAuth.newInstance(), FragmentAuth.TAG)
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

            if (!url.getHost().contains("reddit.com")) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
                return;
            }

            // TODO: Handle special cases like redd.it and / for Front Page
            // TODO: Change it using regex

            String path = url.getPath() + "/";
            Log.d(TAG, "Path: " + path);
            int indexFirstSlash = path.indexOf("/", 1);
            int indexSecondSlash = path.indexOf("/", indexFirstSlash + 1);
            if (indexFirstSlash < 0) {
                getControllerLinks().setParameters("", Sort.HOT, Time.ALL);
                return;
            }
            String subreddit = path.substring(indexFirstSlash + 1,
                    indexSecondSlash > 0 ? indexSecondSlash : path.length());

            Log.d(TAG, "Subreddit: " + subreddit);

            if (path.contains("comments")) {
                int indexComments = path.indexOf("comments") + 9;
                int indexFifthSlash = path.indexOf("/", indexComments + 1);
                String id = path.substring(indexComments,
                        indexFifthSlash > -1 ? indexFifthSlash : path.length());
                Log.d(TAG, "Comments ID: " + id);

                int indexSixthSlash = path.indexOf("/", indexFifthSlash + 1);
                Log.d(TAG, "indexSixthSlash: " + indexSixthSlash);

                if (indexSixthSlash > -1) {
                    int indexSeventhSlash = path.indexOf("/", indexSixthSlash + 1);
                    Log.d(TAG, "indexSeventhSlash: " + indexSeventhSlash);
                    String commentId = path.substring(indexSixthSlash + 1, indexSeventhSlash > -1 ? indexSeventhSlash : path.length());
                    Log.d(TAG, "commentId: " + commentId);
                    if (!TextUtils.isEmpty(commentId)) {
                        getFragmentManager().beginTransaction()
                                .replace(R.id.frame_fragment,
                                        FragmentComments.newInstance(),
                                        FragmentComments.TAG)
                                .commit();
                        getControllerComments().setLinkId(subreddit, id, commentId, 1);
                        return;
                    }
                }

                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment,
                                FragmentComments.newInstance(),
                                FragmentComments.TAG)
                        .commit();
                getControllerComments().setLinkId(subreddit, id);
            }
            else if (path.contains("/u/")) {
                int indexUser = path.indexOf("u/") + 2;
                getControllerProfile().loadUser(
                        path.substring(indexUser, path.indexOf("/", indexUser)));
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance(),
                                FragmentProfile.TAG)
                        .commit();
            }
            else if (path.contains("/user/")) {
                int indexUser = path.indexOf("user/") + 5;
                int endIndex = path.indexOf("/", indexUser);
                if (endIndex > -1) {
                    getControllerProfile()
                            .loadUser(
                                    path.substring(indexUser, endIndex));
                }
                else {
                    getControllerProfile()
                            .loadUser(
                                    path.substring(indexUser));
                }
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentProfile.newInstance(),
                                FragmentProfile.TAG)
                        .commit();

            }
            else {
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentThreadList.newInstance(),
                                FragmentThreadList.TAG)
                        .commit();
                int indexSort = path.indexOf("/", subreddit.length() + 1);
                String sort =
                        indexSort > -1 ? path.substring(subreddit.length() + 1, indexSort) :
                                "hot";
                Log.d(TAG, "Sort: " + sort);
                // TODO: Parse time
                getControllerLinks().setParameters(subreddit, Sort.HOT, Time.ALL);
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
        onNavigationBackClick();
    }

    @Override
    public void startActivity(Intent intent) {
        Log.d(TAG, "startActivity: " + intent.toString());

        if (!intent.hasExtra(REDDIT_PAGE) && !sharedPreferences.getBoolean(AppSettings.PREF_EXTERNAL_BROWSER, false) && Intent.ACTION_VIEW.equals(intent.getAction())) {
            String urlString = intent.getDataString();

            Log.d(TAG, "index: " + urlString.indexOf("reddit.com"));

            Intent intentActivity = new Intent(this, MainActivity.class);
            intentActivity.setAction(Intent.ACTION_VIEW);
            if (urlString.startsWith("/r/") || urlString.startsWith("/u/")) {
                intentActivity.putExtra(REDDIT_PAGE, Reddit.BASE_URL + urlString);
                Log.d(TAG, "startActivity with REDDIT_PAGE");
                super.startActivity(intentActivity);

            }
            else if (urlString.indexOf("reddit.com") < 20) {
                intentActivity.putExtra(REDDIT_PAGE, urlString);
                Log.d(TAG, "startActivity with REDDIT_PAGE");
                super.startActivity(intentActivity);
            }
            else if (URLUtil.isValidUrl(urlString)) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                Fragment fragment = getFragmentManager().findFragmentById(R.id.frame_fragment);
                if (fragment != null) {
                    fragmentTransaction.hide(fragment);
                }

                fragmentTransaction.add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        }
        else {
            super.startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SETTINGS && resultCode == Activity.RESULT_OK) {
            TaskStackBuilder.create(this)
                    .addNextIntent(new Intent(this, MainActivity.class))
                    .addNextIntent(this.getIntent())
                    .startActivities();
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
        if (success) {
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT)
                    .show();
            loadAccountInfo();
            getControllerUser().reloadUser();
            getControllerSearch().reloadSubscriptionList();
            onNavigationBackClick();
        }
        else {
            Toast.makeText(this, getString(R.string.login_failure), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public ControllerSearch getControllerSearch() {
        return fragmentData.getControllerSearch();
    }

    @Override
    public ControllerHistory getControllerHistory() {
        return fragmentData.getControllerHistory();
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
            FragmentBase fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);

            if (fragment != null && !fragment.navigateBack()) {
                return;
            }

            /*
                If this is the only fragment in the stack, close out the Activity,
                otherwise show the fragment
             */
//            if (getFragmentManager().getBackStackEntryCount() == 1) {
//                finish();
//                return;
//            }
            getFragmentManager().popBackStackImmediate();

            fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragment != null) {
                getFragmentManager().beginTransaction().show(fragment).commit();
                fragment.onShown();
                Log.d(TAG, "Fragment shown");
            }
            else {
                finish();
            }
        }
        else {
            if (isTaskRoot()) {
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
        handler.postDelayed(runnableInbox, 1000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnableInbox);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (isTaskRoot()) {
            Historian.saveToFile(this);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}