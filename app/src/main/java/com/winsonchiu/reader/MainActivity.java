/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.ControllerComments;
import com.winsonchiu.reader.comments.FragmentComments;
import com.winsonchiu.reader.comments.FragmentReply;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
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
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.TouchEventListener;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.views.CustomRelativeLayout;
import com.winsonchiu.reader.views.ScrollViewHeader;
import com.winsonchiu.reader.views.WebViewFixed;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends YouTubeBaseActivity
        implements FragmentListenerBase, AppCompatCallback {

    public static final String REDDIT_PAGE = "redditPage";
    public static final String NAV_ID = "navId";
    public static final String NAV_PAGE = "navPage";
    public static final String ACCOUNT = "account";

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int REQUEST_SETTINGS = 0;
    private static final long DURATION_CHECK_INBOX_ACTIVE = 120000;

    private FragmentData fragmentData;

    private int loadId = -1;

    private SharedPreferences sharedPreferences;
    private DrawerLayout drawerLayout;
    private NavigationView viewNavigation;

    private ScrollViewHeader scrollHeaderVertical;
    private HorizontalScrollView scrollHeaderHorizontal;
    private ImageView imageHeader;
    private TextView textAccountName;
    private TextView textAccountInfo;
    private ImageButton buttonAccounts;
    private LinearLayout layoutAccounts;

    private Reddit reddit;
    private Handler handler;
    private AccountManager accountManager;
    private Account accountUser;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;
    private final Runnable runnableInbox = new Runnable() {
        @Override
        public void run() {
            Receiver.checkInbox(MainActivity.this, null);
            handler.postDelayed(this, DURATION_CHECK_INBOX_ACTIVE);
        }
    };
    private CustomColorFilter colorFilterPrimary;
    private AppCompatDelegate appCompatDelegate;
    private boolean isDownloadingHeaderImage;
    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            Log.d(TAG, "downloadHeaderImage onBitmapLoaded");
            sharedPreferences.edit().putString(AppSettings.HEADER_NAME, linkHeader.getName()).apply();
            sharedPreferences.edit().putString(AppSettings.HEADER_PERMALINK,
                    linkHeader.getPermalink()).apply();
            sharedPreferences.edit().putLong(AppSettings.HEADER_EXPIRATION,
                    System.currentTimeMillis() + sharedPreferences
                            .getLong(AppSettings.HEADER_INTERVAL, AlarmManager.INTERVAL_HALF_DAY)).apply();
            isDownloadingHeaderImage = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileOutputStream fileOutputStream = openFileOutput(AppSettings.HEADER_FILE_NAME, Context.MODE_PRIVATE);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                        fileOutputStream.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadHeaderFromFile();
                        }
                    });
                }
            }).start();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d(TAG, "downloadHeaderImage onBitmapFailed");
            isDownloadingHeaderImage = false;
            imageHeader.setAlpha(1f);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.d(TAG, "downloadHeaderImage onPrepareLoad");
        }
    };
    private Link linkHeader;
    private Theme theme;
    private int style;
    private String themeBackground;
    private String themePrimary;
    private String themeAccent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        style = R.style.AppDarkTheme;

        if (sharedPreferences.getBoolean(AppSettings.SECRET, false)) {
            theme = Theme.random();
            themePrimary = theme.getName();
            themeAccent = AppSettings.randomThemeString();
        }
        else {
            themePrimary = sharedPreferences.getString(AppSettings.PREF_THEME_PRIMARY, AppSettings.THEME_DEEP_PURPLE);
            theme = Theme.fromString(themePrimary);
            themeAccent = sharedPreferences.getString(AppSettings.PREF_THEME_ACCENT, AppSettings.THEME_YELLOW);
        }
        themeBackground = sharedPreferences.getString(AppSettings.PREF_THEME_BACKGROUND, AppSettings.THEME_DARK);
        style = theme.getStyle(themeBackground, themeAccent);
        setTheme(style);

        Fabric.with(this, new Crashlytics());

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.colorPrimary});
        int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        int resourceIcon = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.mipmap.app_icon_white_outline : R.mipmap.app_icon_dark_outline;

        colorFilterPrimary = new CustomColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Reader", BitmapFactory.decodeResource(getResources(), resourceIcon), colorPrimary);
            setTaskDescription(taskDescription);
        }

        super.onCreate(savedInstanceState);

        appCompatDelegate = AppCompatDelegate.create(this, this);
        appCompatDelegate.onCreate(savedInstanceState);

        reddit = Reddit.getInstance(this);
        handler = new Handler();
        accountManager = AccountManager.get(getApplicationContext());

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

        appCompatDelegate.setContentView(R.layout.activity_main);

        inflateNavigationDrawer();

        Receiver.setAlarm(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if (loadId != 0) {
                    selectNavigationItem(loadId, null, true);
                }
                setAccountsVisible(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        eventListenerBase = new AdapterLink.ViewHolderBase.EventListener() {

            @Override
            public void sendComment(String name, String text) {
                reddit.sendComment(name, text, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Comment newComment = Comment.fromJson(Reddit.getObjectMapper().readValue(response, JsonNode.class).get("json")
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
                            Message newMessage = Message.fromJson(Reddit.getObjectMapper().readValue(response, JsonNode.class).get("json")
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
                return WebViewFixed.newInstance(getApplicationContext(), true, onFinishedListener);
            }

            @Override
            public void toast(String text) {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean isUserLoggedIn() {
                return getControllerUser().hasUser();
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
                getControllerProfile().deletePost(link);
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

            @Override
            public void loadWebFragment(String url) {
                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public User getUser() {
                return getControllerUser().getUser();
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

        Reddit.ErrorListener redditErrorListener = new Reddit.ErrorListener() {
            @Override
            public void onErrorHandled() {

            }
        };

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

        String name;
        if (getIntent().hasExtra(ACCOUNT)) {
            name = getIntent().getStringExtra(ACCOUNT);
        }
        else {
            name = sharedPreferences.getString(AppSettings.ACCOUNT_NAME, "");
        }

        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);

        Account accountUser = null;

        for (Account account : accounts) {
            if (account.name.equals(name)) {
                accountUser = account;
                break;
            }
        }

        if (accountUser == null) {
            clearAccount();
        }
        else {
            setAccount(accountUser);
        }

    }

    private void inflateNavigationDrawer() {
        CustomRelativeLayout viewHeader = (CustomRelativeLayout) findViewById(R.id.layout_header_navigation);
        viewNavigation = (NavigationView) findViewById(R.id.navigation);

        // TODO: Adhere to guidelines by making the increment 56dp on mobile and 64dp on tablet
        float standardIncrement = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        float screenWidth = getResources().getDisplayMetrics().widthPixels;

        TypedArray typedArray = getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize, R.attr.colorPrimary});
        float marginEnd = typedArray.getDimension(0, standardIncrement);
        typedArray.recycle();

        float navigationWidth = screenWidth - marginEnd;
        if (navigationWidth > standardIncrement * 6) {
            navigationWidth = standardIncrement * 6;
        }

        findViewById(R.id.drawer_navigation).getLayoutParams().width = (int) navigationWidth;

        scrollHeaderVertical = (ScrollViewHeader) viewHeader.findViewById(R.id.scroll_header_vertical);
        scrollHeaderHorizontal = (HorizontalScrollView) viewHeader.findViewById(R.id.scroll_header_horizontal);
        imageHeader = (ImageView) viewHeader.findViewById(R.id.image_nav_header);
        textAccountName = (TextView) viewHeader.findViewById(R.id.text_account_name);
        textAccountInfo = (TextView) viewHeader.findViewById(R.id.text_account_info);
        buttonAccounts = (ImageButton) viewHeader.findViewById(R.id.button_accounts);
        layoutAccounts = (LinearLayout) viewHeader.findViewById(R.id.layout_accounts);

        textAccountName.setTextColor(colorFilterPrimary.getColor());
        textAccountInfo.setTextColor(colorFilterPrimary.getColor());

        buttonAccounts.setColorFilter(colorFilterPrimary);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAccountsVisible(!layoutAccounts.isShown());
            }
        };

        textAccountName.setOnClickListener(onClickListener);
        textAccountInfo.setOnClickListener(onClickListener);
        buttonAccounts.setOnClickListener(onClickListener);

        resetAccountList();

        final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                loadHeaderImage(true);
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                String permalink = sharedPreferences.getString(AppSettings.HEADER_PERMALINK, "");

                if (!TextUtils.isEmpty(permalink)) {
                    Intent intentActivity = new Intent(MainActivity.this, MainActivity.class);
                    intentActivity.setAction(Intent.ACTION_VIEW);
                    intentActivity.putExtra(REDDIT_PAGE, Reddit.BASE_URL + permalink);
                    MainActivity.super.startActivity(intentActivity);
                    return true;
                }
                return super.onSingleTapConfirmed(e);
            }
        });

        // Add an empty view to remove extra margin on top
        viewNavigation.addHeaderView(new View(this));
        viewNavigation.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        loadId = menuItem.getItemId();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                });

        scrollHeaderVertical.setTouchEventListener(new TouchEventListener() {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                gestureDetector.onTouchEvent(event);

                switch (MotionEventCompat.getActionMasked(event)) {
                    case MotionEvent.ACTION_DOWN:
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                        break;
                }

                return false;
            }
        });

        loadHeaderImage(false);
    }

    private void loadHeaderImage(boolean force) {

        // TODO: Implement expiration enabled history
        imageHeader.setAlpha(0.5f);

        if (!force && getFileStreamPath(AppSettings.HEADER_FILE_NAME).exists() && System.currentTimeMillis() < sharedPreferences.getLong(AppSettings.HEADER_EXPIRATION, 0)) {
            loadHeaderFromFile();
            return;
        }

        String subreddit = loadAndParseHeaderSubreddit();

        if (TextUtils.isEmpty(subreddit)) {
            Toast.makeText(this, R.string.header_subreddit_error, Toast.LENGTH_LONG).show();
            Reddit.loadPicasso(this).load(android.R.color.transparent).into(imageHeader);
            return;
        }

        String url = Reddit.OAUTH_URL + subreddit + Sort.HOT.toString() + "?t=" + Time.ALL.toString() + "&limit=100&showAll=true";

        reddit.loadGet(url, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                // TODO: Catch null errors in parent method call
                if (response == null) {
                    return;
                }

                try {
                    Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                            response, JsonNode.class));
                    Collections.shuffle(listing.getChildren());

                    Link linkChosen = null;
                    String nameCurrent = sharedPreferences.getString(AppSettings.HEADER_NAME, "");

                    for (Thing thing : listing.getChildren()) {
                        Link link = (Link) thing;
                        if (Reddit.checkIsImage(link.getUrl()) && !thing.getName()
                                .equals(nameCurrent)) {
                            linkChosen = link;
                            break;
                        }
                    }

                    if (linkChosen != null) {
                        downloadHeaderImage(linkChosen);
                    }
                    else {
                        imageHeader.setAlpha(1f);
                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                imageHeader.setAlpha(1f);
            }
        }, 0);

    }

    private void loadHeaderFromFile() {
        Reddit.loadPicasso(MainActivity.this).invalidate(
                getFileStreamPath(AppSettings.HEADER_FILE_NAME));

        scrollHeaderVertical.post(new Runnable() {
            @Override
            public void run() {

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(
                        getFileStreamPath(AppSettings.HEADER_FILE_NAME).getAbsolutePath(), options);

                /*
                    Determine which length is longer and scale image appropriately
                 */
                float ratioHeader = (float) scrollHeaderVertical.getWidth() / scrollHeaderVertical
                        .getHeight();
                float ratioImage = (float) options.outWidth / options.outHeight;
                int targetWidth = 0;
                int targetHeight = 0;

                if (ratioImage > ratioHeader) {
                    targetHeight = scrollHeaderVertical.getHeight();
                }
                else {
                    targetWidth = scrollHeaderVertical.getWidth();
                }

                Reddit.loadPicasso(MainActivity.this)
                        .load(getFileStreamPath(AppSettings.HEADER_FILE_NAME)).noPlaceholder()
                        .resize(targetWidth, targetHeight).into(imageHeader, new Callback() {
                    @Override
                    public void onSuccess() {

                        Log.d(TAG, "loadHeaderFromFile onSuccess");

                        imageHeader.setAlpha(1f);
                        textAccountName.setTextColor(Color.WHITE);
                        textAccountName.setShadowLayer(3, 0, 0, Color.BLACK);
                        textAccountInfo.setTextColor(Color.WHITE);
                        textAccountInfo.setShadowLayer(3, 0, 0, Color.BLACK);
                        buttonAccounts.clearColorFilter();
                        imageHeader.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollHeaderVertical.scrollTo(0,
                                        imageHeader.getHeight() / 2 - scrollHeaderVertical
                                                .getHeight() / 2);
                                scrollHeaderHorizontal.scrollTo(0,
                                        imageHeader.getWidth() / 2 - scrollHeaderHorizontal
                                                .getWidth() / 2);
                            }
                        });
                    }

                    @Override
                    public void onError() {
                        imageHeader.setAlpha(1f);
                    }
                });
            }
        });

    }

    private void downloadHeaderImage(final Link link) {

        Log.d(TAG, "downloadHeaderImage: " + link);

        if (isDownloadingHeaderImage) {
            return;
        }

        isDownloadingHeaderImage = true;
        linkHeader = link;
        Reddit.loadPicasso(MainActivity.this).load(link.getUrl()).into(target);
    }

    private String loadAndParseHeaderSubreddit() {
        String inputRaw = sharedPreferences.getString(AppSettings.PREF_HEADER_SUBREDDIT, "earthporn");
        inputRaw = inputRaw.replaceAll("\\s", "");

        if (inputRaw.startsWith("/r/")) {
            inputRaw = inputRaw.substring(3);
        }
        else if (inputRaw.startsWith("r/")) {
            inputRaw = inputRaw.substring(2);
        }

        if (!inputRaw.matches("^[A-z]+$")) {
            return null;
        }

        return "/r/" + inputRaw + "/";
    }

    private void resetAccountList() {
        layoutAccounts.removeAllViews();

        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);

        for (final Account account : accounts) {
            View viewAccount = getLayoutInflater().inflate(R.layout.row_account, layoutAccounts, false);
            TextView textUsername = (TextView) viewAccount.findViewById(R.id.text_username);
            ImageButton buttonDeleteAccount = (ImageButton) viewAccount.findViewById(R.id.button_delete_account);
            buttonDeleteAccount.setColorFilter(colorFilterPrimary);

            textUsername.setText(account.name);
            textUsername.setTextColor(colorFilterPrimary.getColor());

            viewAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAccount(account);
                }
            });
            buttonDeleteAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAccount(account);
                }
            });

            layoutAccounts.addView(viewAccount);
        }


        View viewAddAccount = getLayoutInflater().inflate(R.layout.row_account, layoutAccounts, false);
        viewAddAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewAccount();
            }
        });
        TextView textAddAccount = (TextView) viewAddAccount.findViewById(R.id.text_username);
        textAddAccount.setText(R.string.add_account);
        textAddAccount.setTextColor(colorFilterPrimary.getColor());
        ImageButton buttonAddAccount = (ImageButton) viewAddAccount.findViewById(R.id.button_delete_account);
        buttonAddAccount.setImageResource(R.drawable.ic_add_white_24dp);
        buttonAddAccount.setColorFilter(colorFilterPrimary);
        buttonAddAccount.setClickable(false);
        layoutAccounts.addView(viewAddAccount);

        View viewLogout = getLayoutInflater().inflate(R.layout.row_account, layoutAccounts, false);
        viewLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAccount();
            }
        });
        TextView textLogout = (TextView) viewLogout.findViewById(R.id.text_username);
        textLogout.setText(R.string.logout);
        textLogout.setTextColor(colorFilterPrimary.getColor());
        ImageButton buttonLogout = (ImageButton) viewLogout.findViewById(R.id.button_delete_account);
        buttonLogout.setImageResource(R.drawable.ic_exit_to_app_white_24dp);
        buttonLogout.setColorFilter(colorFilterPrimary);
        buttonLogout.setClickable(false);
        layoutAccounts.addView(viewLogout);
    }

    private void clearAccount() {
        accountUser = null;
        sharedPreferences.edit().putString(AppSettings.ACCOUNT_NAME, "").apply();
        reddit.clearAccount(new Reddit.ErrorListener() {
            @Override
            public void onErrorHandled() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getControllerUser().clearAccount();
                        getControllerSearch().reloadSubscriptionList();
                        loadAccountInfo();
                    }
                });
            }
        });
    }

    private void setAccount(final Account account) {
        accountUser = account;
        sharedPreferences.edit().putString(AppSettings.ACCOUNT_NAME, account.name).apply();
        reddit.setAccount(account, new Reddit.ErrorListener() {
            @Override
            public void onErrorHandled() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getControllerUser().setAccount(account);
                        getControllerSearch().reloadSubscriptionList();
                        loadAccountInfo();
                    }
                });
            }
        });

    }

    private void deleteAccount(final Account account) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(account.name)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        String tokenRefresh = accountManager.getPassword(account);

                        Map<String, String> params = new ArrayMap<>(2);
                        params.put("token_type_hint", "refresh_token");
                        params.put("token", tokenRefresh);

                        reddit.loadPostDefault(Reddit.REVOKE_URL, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d(TAG, "revoke_token response: " + response);

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final AccountManagerFuture future;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                            future = accountManager.removeAccount(account, null, null, null);
                                        }
                                        else {
                                            future = accountManager.removeAccount(account, null, null);
                                        }

                                        sharedPreferences.edit().putString(AppSettings.SUBSCRIPTIONS + account.name, "").apply();

                                        try {
                                            // Force changes in AccountManager
                                            future.getResult();
                                        }
                                        catch (OperationCanceledException | IOException | AuthenticatorException e) {
                                            e.printStackTrace();
                                        }

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                resetAccountList();
                                                if (account.equals(accountUser)) {
                                                    clearAccount();
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, R.string.error_delete_account_securely, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }, params, 0);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setAccountsVisible(boolean visible) {
        if (visible) {
            buttonAccounts.setImageResource(R.drawable.ic_arrow_drop_down_white_24dp);
        }
        else {
            buttonAccounts.setImageResource(R.drawable.ic_arrow_drop_up_white_24dp);
        }

        // Checks if layoutAccounts needs animating or to just immediately hide it
        if ((layoutAccounts.getVisibility() == View.VISIBLE) != visible) {
            if (layoutAccounts.isShown() || layoutAccounts.getVisibility() != View.VISIBLE) {
                UtilsAnimation.animateExpand(layoutAccounts, 1f, null, 250);
            }
            else {
                layoutAccounts.setVisibility(View.GONE);
            }
        }
    }

    private void selectNavigationItem(final int id, String page, boolean animate) {

        loadId = 0;

        if (id == R.id.item_settings) {
            Intent intentSettings = new Intent(this, ActivitySettings.class);
            startActivityForResult(intentSettings, REQUEST_SETTINGS);
            return;
        }

        MenuItem item = viewNavigation.getMenu().findItem(id);
        if (item != null) {
            item.setChecked(true);
        }

        getFragmentManager().popBackStackImmediate();

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        if (animate) {
            fragmentTransaction.setCustomAnimations(R.animator.slide_from_left, R.animator.slide_to_right);
        }

        while (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStackImmediate();
        }

        switch (id) {
            case R.id.item_home:
                FragmentBase fragmentThreadList = (FragmentBase) getFragmentManager().findFragmentByTag(FragmentThreadList.TAG);
                if (fragmentThreadList != null) {
                    getControllerLinks().loadFrontPage(Sort.HOT, false);
                    fragmentThreadList.onHiddenChanged(false);
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

                if (getControllerUser().hasUser()) {
                    getControllerProfile().setUser(getControllerUser().getUser());
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
        boolean visible = getControllerUser().hasUser();

        if (visible) {
            Reddit.getInstance(this)
                    .loadGet(Reddit.OAUTH_URL + "/api/v1/me",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        textAccountName.setText(jsonObject.getString("name"));
                                        textAccountInfo.setText(getString(R.string.account_info, jsonObject.getString(
                                                "link_karma"), jsonObject.getString("comment_karma")));
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d(TAG, "loadAccountInfo error: " + error);
                                }
                            }, 0);
        }
        else {
            textAccountName.setText(R.string.login);
            textAccountInfo.setText("");
        }

        viewNavigation.getMenu().findItem(R.id.item_inbox).setVisible(visible);
        viewNavigation.getMenu().findItem(R.id.item_inbox).setEnabled(visible);
    }

    private void addNewAccount() {
        accountManager.addAccount(Reddit.ACCOUNT_TYPE, Reddit.AUTH_TOKEN_FULL_ACCESS, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(final AccountManagerFuture<Bundle> future) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Bundle bundle = future.getResult();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String name = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
                                    sharedPreferences.edit().putString(AppSettings.ACCOUNT_NAME, name).apply();
                                    Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);

                                    for (Account account : accounts) {
                                        if (account.name.equals(name)) {
                                            setAccount(account);
                                            break;
                                        }
                                    }

                                    resetAccountList();
                                }
                            });
                        }
                        catch (OperationCanceledException | AuthenticatorException | IOException e) {
                            e.printStackTrace();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, R.string.error_account, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        }, null);
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
            else {
                if (path.contains("/u/")) {
                    int indexUser = path.indexOf("u/") + 2;
                    getControllerProfile().loadUser(
                            path.substring(indexUser, path.indexOf("/", indexUser)));
                    getFragmentManager().beginTransaction()
                            .replace(R.id.frame_fragment, FragmentProfile.newInstance(),
                                    FragmentProfile.TAG)
                            .commit();
                }
                else {
                    if (path.contains("/user/")) {
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
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            else {
                drawerLayout.openDrawer(GravityCompat.START);
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
            else {
                if (urlString.indexOf("reddit.com") > 0 && urlString.indexOf("reddit.com") < 20) {
                    intentActivity.putExtra(REDDIT_PAGE, urlString);
                    Log.d(TAG, "startActivity with REDDIT_PAGE");
                    super.startActivity(intentActivity);
                }
                else {
                    if (URLUtil.isValidUrl(urlString)) {

                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                        FragmentBase fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
                        if (fragment != null) {
                            fragmentTransaction.hide(fragment);
                            if (fragment.shouldOverrideUrl(urlString)) {
                                return;
                            }
                        }
                        Log.d(TAG, "FragmentWeb added");

                        fragmentTransaction.add(R.id.frame_fragment, FragmentWeb
                                .newInstance(urlString), FragmentWeb.TAG)
                                .addToBackStack(null)
                                .commit();
                    }
                }
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
    public Theme getAppColorTheme() {
        return theme;
    }

    @Override
    public String getThemeBackgroundPrefString() {
        return themeBackground;
    }

    @Override
    public String getThemePrimaryPrefString() {
        return themePrimary;
    }

    @Override
    public String getThemeAccentPrefString() {
        return themeAccent;
    }

    @Override
    public void onNavigationBackClick() {
        FragmentBase fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);

        // TODO: Remove this and use a better system
        if (fragment != null) {
            if (!fragment.isFinished()) {
                fragment.navigateBack();
                return;
            } else {
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    if (isTaskRoot()) {
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.exit_reader)
                                .setPositiveButton(R.string.yes,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                MainActivity.super.onBackPressed();
                                            }
                                        })
                                .setNegativeButton(R.string.no, null)
                                .show();

                    } else {
                        super.onBackPressed();
                    }
                }
            }

            /*
                If this is the only fragment in the stack, close out the Activity,
                otherwise show the fragment
             */
            getFragmentManager().popBackStackImmediate();

            fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragment != null) {
                fragment.onHiddenChanged(false);
                getFragmentManager().beginTransaction().show(fragment).commit();
                fragment.onShown();
                Log.d(TAG, "Fragment shown");
            }
            else {
                finish();
            }
        }
    }

    @Override
    public void openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START);
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        appCompatDelegate.onPostCreate(savedInstanceState);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        appCompatDelegate.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        appCompatDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        if (isTaskRoot()) {
            Historian.saveToFile(this);
        }
        appCompatDelegate.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        appCompatDelegate.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {

    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {

    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }
}