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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.ControllerCommentsTop;
import com.winsonchiu.reader.comments.FragmentComments;
import com.winsonchiu.reader.comments.FragmentReply;
import com.winsonchiu.reader.dagger.FragmentPersist;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.dagger.modules.ModuleReddit;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
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
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.FinalizingSubscriber;
import com.winsonchiu.reader.utils.ObserverEmpty;
import com.winsonchiu.reader.utils.TargetImageDownload;
import com.winsonchiu.reader.utils.TouchEventListener;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
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
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class ActivityMain extends YouTubeBaseActivity
        implements FragmentListenerBase, AppCompatCallback {

    private static final String TAG = ActivityMain.class.getCanonicalName();

    public static final String REDDIT_PAGE = "redditPage";
    public static final String NAV_ID = "navId";
    public static final String NAV_PAGE = "navPage";
    public static final String ACCOUNT = "account";

    private static final int REQUEST_SETTINGS = 0;
    private static final long DURATION_CHECK_INBOX_ACTIVE = 120000;

    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 61;

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

    private Handler handler;
    private Account accountUser;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;
    private final Runnable runnableInbox = new Runnable() {
        @Override
        public void run() {
            new Receiver().checkInbox(ActivityMain.this, null);
            handler.postDelayed(this, DURATION_CHECK_INBOX_ACTIVE);
        }
    };
    private CustomColorFilter colorFilterPrimary;
    private int colorPrimary;
    private AppCompatDelegate appCompatDelegate;
    private boolean isDownloadingHeaderImage;
    private TargetImageDownload targetDownload;
    private Target targetHeader = new Target() {
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
    private ComponentActivity componentActivity;

    private CustomTabsServiceConnection customTabsServiceConnection;
    private CustomTabsClient customTabsClient;
    private CustomTabsSession customTabsSession;

    @Inject AccountManager accountManager;
    @Inject Reddit reddit;
    @Inject Picasso picasso;
    @Inject Historian historian;
    @Inject ControllerLinks controllerLinks;
    @Inject ControllerUser controllerUser;
    @Inject ControllerCommentsTop controllerCommentsTop;
    @Inject ControllerProfile controllerProfile;
    @Inject ControllerInbox controllerInbox;
    @Inject ControllerSearch controllerSearch;
    @Inject ControllerHistory controllerHistory;

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

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.colorPrimary});
        colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
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

        handler = new Handler();

        FragmentPersist fragmentPersist = (FragmentPersist) getFragmentManager().findFragmentByTag(FragmentPersist.TAG);
        if (fragmentPersist == null) {
            fragmentPersist = new FragmentPersist();
            getFragmentManager().beginTransaction().add(fragmentPersist, FragmentPersist.TAG).commit();
            fragmentPersist.initialize();
        }

        componentActivity = fragmentPersist.getComponentActivity();

        if (componentActivity == null) {
            componentActivity = CustomApplication.getComponentMain()
                    .plus(new ModuleReddit());
        }

        componentActivity.inject(this);

        appCompatDelegate.setContentView(R.layout.activity_main);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

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
                reddit.sendComment(name, text)
                        .flatMap(new Func1<String, Observable<Comment>>() {
                            @Override
                            public Observable<Comment> call(String response) {
                                try {
                                    Comment comment = Comment.fromJson(ComponentStatic.getObjectMapper()
                                            .readValue(response, JsonNode.class).get("json")
                                            .get("data")
                                            .get("things")
                                            .get(0), 0);

                                    return Observable.just(comment);
                                }
                                catch (IOException e) {
                                    return Observable.error(e);
                                }
                            }
                        })
                        .subscribe(new FinalizingSubscriber<Comment>() {
                            @Override
                            public void error(Throwable e) {
                                Toast.makeText(ActivityMain.this, R.string.failed_reply, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void next(Comment comment) {
                                if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) != null) {
                                    controllerCommentsTop.insertComment(comment);
                                }
                                if (getFragmentManager().findFragmentByTag(FragmentProfile.TAG) != null) {
                                    controllerProfile.insertComment(comment);
                                }
                                if (getFragmentManager().findFragmentByTag(FragmentInbox.TAG) != null) {
                                    controllerInbox.insertComment(comment);
                                }
                            }
                        });
            }

            @Override
            public void sendMessage(String name, String text) {
                reddit.sendComment(name, text)
                        .flatMap(new Func1<String, Observable<Message>>() {
                            @Override
                            public Observable<Message> call(String response) {
                                try {
                                    Message message = Message.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class).get("json")
                                            .get("data")
                                            .get("things")
                                            .get(0));

                                    return Observable.just(message);
                                }
                                catch (IOException e) {
                                    return Observable.error(e);
                                }
                            }
                        })
                        .subscribe(new FinalizingSubscriber<Message>() {
                            @Override
                            public void error(Throwable e) {
                                Toast.makeText(ActivityMain.this, R.string.failed_message, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void next(Message next) {
                                controllerInbox.insertMessage(next);
                            }
                        });
            }

            @Override
            public void onClickComments(Link link, final AdapterLink.ViewHolderBase viewHolderBase) {

                Log.d(TAG, "onClickComments: " + link);

                controllerCommentsTop
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
                    reddit.save(link, null)
                            .subscribe(new FinalizingSubscriber<String>() {
                                @Override
                                public void error(Throwable e) {
                                    Toast.makeText(ActivityMain.this, R.string.error_saving_post, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                else {
                    reddit.unsave(link)
                            .subscribe(new ObserverEmpty<>());
                }
            }

            @Override
            public void save(Comment comment) {
                comment.setSaved(!comment.isSaved());
                if (comment.isSaved()) {
                    reddit.save(comment, null)
                            .subscribe(new FinalizingSubscriber<String>() {
                                @Override
                                public void error(Throwable e) {
                                    Toast.makeText(ActivityMain.this, R.string.error_saving_comment, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                else {
                    reddit.unsave(comment)
                            .subscribe(new ObserverEmpty<>());
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
                    Intent intent = new Intent(ActivityMain.this, ActivityMain.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(REDDIT_PAGE, urlString);
                    startActivity(intent);
                    return;
                }

                launchUrl(urlString, false);
            }

            @Override
            public void downloadImage(final String fileName, final String url) {
                targetDownload = new TargetImageDownload(fileName, url) {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
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
                            MediaScannerConnection.scanFile(ActivityMain.this,
                                    new String[]{file.toString()}, null, null);
                        }

                        Toast.makeText(ActivityMain.this, getString(R.string.image_downloaded),
                                Toast.LENGTH_SHORT)
                                .show();

                        targetDownload = null;
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        Toast.makeText(ActivityMain.this, getString(R.string.error_downloading), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        Toast.makeText(ActivityMain.this, getString(R.string.image_downloading), Toast.LENGTH_SHORT).show();
                    }
                };

                ActivityCompat.requestPermissions(ActivityMain.this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
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
                Toast.makeText(ActivityMain.this, text, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean isUserLoggedIn() {
                return controllerUser.hasUser();
            }

            @Override
            public void voteLink(final AdapterLink.ViewHolderBase viewHolderBase, final Link link, int vote) {
                final int position = viewHolderBase.getAdapterPosition();

                final int oldVote = link.getLikes();
                int newVote = 0;

                if (link.getLikes() != vote) {
                    newVote = vote;
                }

                HashMap<String, String> params = new HashMap<>(2);
                params.put(Reddit.QUERY_ID, link.getName());
                params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

                link.setScore(link.getScore() + newVote - link.getLikes());
                link.setLikes(newVote);
                if (position == viewHolderBase.getAdapterPosition()) {
                    viewHolderBase.setVoteColors();
                }
                final int finalNewVote = newVote;

                reddit.voteLink(link, newVote)
                        .subscribe(new FinalizingSubscriber<String>() {
                            @Override
                            public void error(Throwable e) {
                                link.setScore(link.getScore() - finalNewVote);
                                link.setLikes(oldVote);
                                if (position == viewHolderBase.getAdapterPosition()) {
                                    viewHolderBase.setVoteColors();
                                }
                                Toast.makeText(ActivityMain.this, getString(R.string.error_voting), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
            }

            @Override
            public void startActivity(Intent intent) {
                ActivityMain.this.startActivity(intent);
            }

            @Override
            public void deletePost(Link link) {
                Observable.merge(controllerLinks.deletePost(link), controllerProfile.deletePost(link))
                        .subscribe(new FinalizingSubscriber<String>() {
                            @Override
                            public void error(Throwable e) {
                                Toast.makeText(ActivityMain.this, R.string.error_deleting_post, Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void report(Thing thing, String reason, String otherReason) {
                reddit.report(thing.getName(),
                        reason, otherReason)
                        .subscribe(new Observer<String>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(String s) {

                            }
                        });
            }

            @Override
            public void hide(Link link) {
                link.setHidden(!link.isHidden());
                if (link.isHidden()) {
                    reddit.hide(link)
                            .subscribe(new ObserverEmpty<>());
                }
                else {
                    reddit.unhide(link)
                            .subscribe(new ObserverEmpty<>());
                }

            }

            @Override
            public void editLink(Link link) {
                FragmentNewPost fragmentNewPost = FragmentNewPost.newInstanceEdit(controllerUser.getUser().getName(), link);

                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, fragmentNewPost, FragmentNewPost.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void showReplyEditor(Replyable replyable) {
                FragmentReply fragmentReply = FragmentReply.newInstance(replyable);
                fragmentReply.setFragmentToHide(getFragmentManager().findFragmentById(R.id.frame_fragment));

                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, fragmentReply, FragmentReply.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void markRead(Thing thing) {
                reddit.markRead(thing.getName())
                        .subscribe(new ObserverEmpty<>());
            }

            @Override
            public void markNsfw(final Link link) {
                link.setOver18(!link.isOver18());
                if (link.isOver18()) {
                    reddit.markNsfw(link)
                            .subscribe(new FinalizingSubscriber<String>() {
                                @Override
                                public void error(Throwable e) {
                                    link.setOver18(false);
                                    Toast.makeText(ActivityMain.this, R.string.error_marking_nsfw,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
                else {
                    reddit.unmarkNsfw(link)
                            .subscribe(new FinalizingSubscriber<String>() {
                                @Override
                                public void error(Throwable e) {
                                    link.setOver18(true);
                                    Toast.makeText(ActivityMain.this, R.string.error_unmarking_nsfw, Toast.LENGTH_LONG).show();
                                }
                            });
                }

                if (getFragmentManager().findFragmentByTag(FragmentThreadList.TAG) != null) {
                    controllerLinks.setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) != null) {
                    controllerCommentsTop.setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentProfile.TAG) != null) {
                    controllerProfile.setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentHistory.TAG) != null) {
                    controllerHistory.setNsfw(link.getName(), link.isOver18());
                }
                if (getFragmentManager().findFragmentByTag(FragmentSearch.TAG) != null) {
                    controllerSearch.setNsfwLinks(link.getName(), link.isOver18());
                    controllerSearch.setNsfwLinksSubreddit(link.getName(), link.isOver18());
                }

            }

            @Override
            public void loadWebFragment(String url) {
                launchUrl(url, false);
            }

            @Override
            public User getUser() {
                return controllerUser.getUser();
            }

        };

        eventListenerComment = new AdapterCommentList.ViewHolderComment.EventListener() {
            @Override
            public boolean loadNestedComments(Comment comment, String subreddit, String linkId) {

                if (comment.getCount() == 0) {
                    Intent intentCommentThread = new Intent(ActivityMain.this, ActivityMain.class);
                    intentCommentThread.setAction(Intent.ACTION_VIEW);
                    // Double slashes used to trigger parseUrl correctly
                    intentCommentThread.putExtra(REDDIT_PAGE, Reddit.BASE_URL + "/r/" + subreddit + "/comments/" + linkId + "/title/" + comment.getParentId() + "/");
                    startActivity(intentCommentThread);
                    return true;
                }

                return false;
            }
        };

        if (sharedPreferences.getBoolean(AppSettings.BETA_NOTICE_0, true)) {
            try {

                View view = LayoutInflater.from(this).inflate(R.layout.dialog_text_alert, null, false);
                TextView textTitle = (TextView) view.findViewById(R.id.text_title);
                TextView textMessage = (TextView) view.findViewById(R.id.text_message);

                textTitle.setTextColor(getResources().getColor(R.color.colorPrimary));
                textTitle.setText(R.string.beta_title);
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
                    controllerLinks.loadFrontPage(Sort.HOT, true);
                    selectNavigationItem(getIntent().getIntExtra(NAV_ID, R.id.item_home), getIntent().getStringExtra(
                            NAV_PAGE), false);
                }
            }
            else {
                controllerLinks.loadFrontPage(Sort.HOT, true);
                selectNavigationItem(getIntent().getIntExtra(NAV_ID, R.id.item_home), getIntent().getStringExtra(
                        NAV_PAGE), false);
            }
        }

        customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                customTabsClient.warmup(0);
                customTabsSession = customTabsClient.newSession(new CustomTabsCallback() {
                    @Override
                    public void onNavigationEvent(int navigationEvent, Bundle extras) {
                        super.onNavigationEvent(navigationEvent, extras);
                        Log.d(TAG, "onNavigationEvent() called with: " + "navigationEvent = [" + navigationEvent + "], extras = [" + extras + "]");
                    }

                    @Override
                    public void extraCallback(String callbackName, Bundle args) {
                        super.extraCallback(callbackName, args);
                        Log.d(TAG, "extraCallback() called with: " + "callbackName = [" + callbackName + "], args = [" + args + "]");
                    }
                });

                CustomTabsClient.bindCustomTabsService(ActivityMain.this, "com.android.chrome", this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.need_permission_download_image, Toast.LENGTH_LONG).show();
                }
                else if (targetDownload != null){
                    picasso.load(targetDownload.getUrl())
                            .into(targetDownload);
                }
        }
    }

    private void inflateNavigationDrawer() {
        ViewGroup viewHeader = (ViewGroup) findViewById(R.id.layout_header_navigation);
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
                    Intent intentActivity = new Intent(ActivityMain.this, ActivityMain.class);
                    intentActivity.setAction(Intent.ACTION_VIEW);
                    intentActivity.setData(Uri.parse(Reddit.BASE_URL + permalink));
                    startActivity(intentActivity);
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
            picasso.load(android.R.color.transparent).into(imageHeader);
            return;
        }

        reddit.links(subreddit, Sort.HOT.toString(), Time.ALL.toString(), 100, null)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        imageHeader.setAlpha(1f);
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String response) {
                        try {
                            Listing listing = Listing.fromJson(ComponentStatic.getObjectMapper().readValue(
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
                            onError(e);
                        }
                    }
                });

    }

    private void loadHeaderFromFile() {
        picasso.invalidate(getFileStreamPath(AppSettings.HEADER_FILE_NAME));

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

                picasso.load(getFileStreamPath(AppSettings.HEADER_FILE_NAME)).noPlaceholder()
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
        picasso.load(link.getUrl()).into(targetHeader);
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
        reddit.clearAccount();
        controllerUser.clearAccount();
        controllerSearch.reloadSubscriptionList();
        loadAccountInfo();
    }

    private void setAccount(final Account account) {
        Log.d(TAG, "setAccount() called with: " + "account = [" + account + "]");
        accountUser = account;
        sharedPreferences.edit().putString(AppSettings.ACCOUNT_NAME, account.name).apply();
        reddit.setAccount(account);
        reddit.tokenAuth()
                .subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void completed() {
                        controllerUser.setAccount(account);
                        controllerSearch.reloadSubscriptionList();
                        loadAccountInfo();
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

                        reddit.tokenRevoke(Reddit.QUERY_REFRESH_TOKEN, tokenRefresh)
                                .observeOn(Schedulers.computation())
                                .flatMap(new Func1<String, Observable<?>>() {
                                    @Override
                                    public Observable<?> call(String s) {
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
                                            return Observable.just(future.getResult());
                                        }
                                        catch (OperationCanceledException | IOException | AuthenticatorException e) {
                                            return Observable.error(e);
                                        }
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<Object>() {
                                    @Override
                                    public void onCompleted() {
                                        resetAccountList();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Toast.makeText(ActivityMain.this, R.string.error_delete_account_securely, Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onNext(Object s) {
                                        if (account.equals(accountUser)) {
                                            clearAccount();
                                        }
                                    }
                                });
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

        Menu menu = viewNavigation.getMenu();

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).setChecked(false);
        }

        MenuItem item = menu.findItem(id);
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

        FragmentBase fragmentThreadList;

        switch (id) {
            case R.id.item_home:
                fragmentThreadList = (FragmentBase) getFragmentManager().findFragmentByTag(FragmentThreadList.TAG);
                if (fragmentThreadList != null) {
                    controllerLinks.loadFrontPage(Sort.HOT, false);
                    fragmentThreadList.onHiddenChanged(false);
                }
                else {
                    fragmentTransaction.replace(R.id.frame_fragment,
                            FragmentThreadList.newInstance(),
                            FragmentThreadList.TAG);
                }
                break;
            case R.id.item_history:
                controllerHistory.reload();
                if (getFragmentManager().findFragmentByTag(FragmentHistory.TAG) == null) {
                    fragmentTransaction.replace(R.id.frame_fragment,
                            FragmentHistory.newInstance(),
                            FragmentHistory.TAG);
                }
                break;
            case R.id.item_profile:

                if (controllerUser.hasUser()) {
                    controllerProfile.setUser(controllerUser.getUser());
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
                            controllerInbox.setPage(new Page(page, getString(R.string.inbox_page_inbox)));
                            break;
                        case ControllerInbox.UNREAD:
                            controllerInbox.setPage(new Page(page, getString(R.string.inbox_page_unread)));
                            break;
                        case ControllerInbox.SENT:
                            controllerInbox.setPage(new Page(page, getString(R.string.inbox_page_sent)));
                            break;
                    }
                }

                controllerInbox.reload();
                fragmentTransaction.replace(R.id.frame_fragment,
                        FragmentInbox.newInstance(),
                        FragmentInbox.TAG);
                break;
            case R.id.item_subreddit:
                fragmentThreadList = (FragmentBase) getFragmentManager().findFragmentByTag(FragmentThreadList.TAG);
                if (fragmentThreadList != null) {
                    controllerLinks.setParameters("ReaderForReddit", Sort.HOT, Time.ALL);
                    fragmentThreadList.onHiddenChanged(false);
                }
                else {
                    fragmentTransaction.replace(R.id.frame_fragment,
                            FragmentThreadList.newInstance(),
                            FragmentThreadList.TAG);
                }
                break;
        }

        fragmentTransaction.commit();
    }

    public void loadAccountInfo() {
        boolean visible = controllerUser.hasUser();

        if (visible) {
            reddit.me()
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(String response) {
                            try {
                                Log.d(TAG, "setAccount: onNext() called with: " + "response = [" + response + "]");
                                JSONObject jsonObject = new JSONObject(response);
                                textAccountName.setText(jsonObject.getString("name"));
                                textAccountInfo.setText(getString(R.string.account_info, jsonObject.getString(
                                        "link_karma"), jsonObject.getString("comment_karma")));
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
        else {
            textAccountName.setText(R.string.login);
            textAccountInfo.setText("");
        }

        MenuItem itemInbox = viewNavigation.getMenu().findItem(R.id.item_inbox);
        itemInbox.setVisible(visible);
        itemInbox.setEnabled(visible);
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
                                    Toast.makeText(ActivityMain.this, R.string.error_account, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        }, null);
    }

    private void loadSubreddit(String subreddit, Sort sort, Time time) {
        getFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment, FragmentThreadList.newInstance(),
                        FragmentThreadList.TAG)
                .commit();
        controllerLinks.setParameters(subreddit, sort, time);
    }

    private void loadComments(String idLink, String idComments, int context) {
        getFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment,
                        FragmentComments.newInstance(),
                        FragmentComments.TAG)
                .commit();

        if (TextUtils.isEmpty(idComments)) {
            controllerCommentsTop.setLinkId(idLink);
        } else {
            controllerCommentsTop.setLinkId(idLink, idComments, context);
        }
    }

    private void loadProfile(String user) {
        getFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment, FragmentProfile.newInstance(),
                        FragmentProfile.TAG)
                .commit();
        controllerProfile.loadUser(user);
    }

    private void parseUrl(String urlString) {
        Uri uri = Uri.parse(urlString);

        List<String> pathSegments = uri.getPathSegments();

        Sort sort = Sort.HOT;
        Time time = Time.ALL;
        String subreddit = "";
        String idLink;
        String idComments;
        int context = 1;

        switch (Match.matchUri(Uri.parse(urlString))) {
            case NONE:
                break;
            case SUBREDDIT:
                subreddit = uri.getLastPathSegment();
                time = Time.fromString(uri.getQueryParameter("time"));

                loadSubreddit(subreddit, sort, time);
                return;
            case SUBREDDIT_HOT:
            case SUBREDDIT_NEW:
            case SUBREDDIT_RISING:
            case SUBREDDIT_CONTROVERSIAL:
            case SUBREDDIT_TOP:
            case SUBREDDIT_GILDED:
                sort = Sort.fromString(uri.getLastPathSegment());
                time = Time.fromString(uri.getQueryParameter("time"));
                subreddit = pathSegments.get((pathSegments.size() - 2));

                loadSubreddit(subreddit, sort, time);
                return;
            case COMMENTS:
                idLink = uri.getLastPathSegment();

                loadComments(idLink, null, 0);
                return;
            case COMMENTS_TITLED:
                idLink = pathSegments.get(pathSegments.size() - 2);

                loadComments(idLink, null, 0);
                return;
            case COMMENTS_TITLED_ID:
                try {
                    context = Integer.parseInt(uri.getQueryParameter("context"));
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                idLink = pathSegments.get(pathSegments.size() - 3);
                idComments = uri.getLastPathSegment();

                loadComments(idLink, idComments, context);
                return;
            case USER:
                loadProfile(uri.getLastPathSegment());
                return;
            case SEARCH_SUBREDDIT:
                final String query = Html.fromHtml(uri.getQueryParameter("q")).toString();
                subreddit = urlString.substring(urlString.indexOf("/r/") + 3, urlString.indexOf("/search"));
                sort = Sort.fromString(uri.getQueryParameter("sort"));

                final Sort finalSort = sort;
                controllerLinks.setParameters(subreddit, Sort.HOT, Time.ALL)
                        .subscribe(new FinalizingSubscriber<Subreddit>() {
                            @Override
                            public void next(Subreddit subreddit) {
                                getFragmentManager().beginTransaction()
                                        .add(R.id.frame_fragment, FragmentSearch
                                                .newInstance(true), FragmentSearch.TAG)
                                        .addToBackStack(null)
                                        .commit();

                                controllerSearch.setData(ControllerSearch.PAGE_LINKS_SUBREDDIT, query, finalSort, Time.ALL);
                            }
                        });
                return;
        }

        launchUrl(urlString, true);
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
        if (!intent.hasExtra(REDDIT_PAGE) && !sharedPreferences.getBoolean(AppSettings.PREF_EXTERNAL_BROWSER, false) && Intent.ACTION_VIEW.equals(intent.getAction())) {
            String urlString = intent.getDataString();

            Uri uri = intent.getData();

            Match match = Match.matchUri(uri);
            if (match == Match.NONE && getApplicationContext().getPackageName().equals(intent.getStringExtra(Browser.EXTRA_APPLICATION_ID))) {
                uri = Uri.parse(Reddit.BASE_URL + urlString);
                match = Match.matchUri(uri);
                if (!URLUtil.isValidUrl(urlString)) {
                    urlString = Reddit.BASE_URL + urlString;
                }
            }

            if (match != Match.NONE) {
                intent.setData(uri);
                intent.setComponent(new ComponentName(getApplicationContext().getPackageName(), ActivityMain.class.getCanonicalName()));
                super.startActivity(intent);
            }
            else if (URLUtil.isValidUrl(urlString)) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

                FragmentBase fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
                if (fragment != null) {
                    fragmentTransaction.hide(fragment);
                    if (fragment.shouldOverrideUrl(urlString)) {
                        return;
                    }
                }

                launchUrl(urlString, false);
            }
            else {
                super.startActivity(intent);
            }
        }
        else {
            super.startActivity(intent);
        }
    }

    private void launchUrl(String url, boolean replace) {
        CustomTabsIntent intentCustomTabs = new CustomTabsIntent.Builder(customTabsSession)
                .setToolbarColor(colorPrimary)
                .build();

        intentCustomTabs.intent.setData(Uri.parse(url));

        Intent intentChrome = getIntentForChrome();

        if (intentChrome == null) {
            if (replace) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentWeb
                                .newInstance(url), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .hide(getFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        } else {
            intentCustomTabs.intent.setComponent(intentChrome.getComponent());
            super.startActivity(intentCustomTabs.intent, intentCustomTabs.startAnimationBundle);
        }
    }

    private Intent getIntentForChrome() {
        Intent intentChrome = getPackageManager().getLaunchIntentForPackage("com.chrome.dev");
        if (intentChrome == null) {
            intentChrome = getPackageManager().getLaunchIntentForPackage("com.chrome.beta");
        }
        if (intentChrome == null) {
            intentChrome = getPackageManager().getLaunchIntentForPackage("com.android.chrome");
        }

        return getPackageManager().getLaunchIntentForPackage("com.android.chrome");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SETTINGS && resultCode == Activity.RESULT_OK) {
            TaskStackBuilder.create(this)
                    .addNextIntent(new Intent(this, ActivityMain.class))
                    .addNextIntent(this.getIntent())
                    .startActivities();
        }
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
    public AdapterCommentList.ViewHolderComment.EventListener getEventListener() {
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
        int backStackCount = getFragmentManager().getBackStackEntryCount();

        if (backStackCount > 0) {
            FragmentBase fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragment != null && !fragment.isFinished()) {
                fragment.navigateBack();
                return;
            }
            if (backStackCount == 1) {
                super.onBackPressed();
            } else {
                getFragmentManager().popBackStackImmediate();

                fragment = (FragmentBase) getFragmentManager().findFragmentById(R.id.frame_fragment);

                if (fragment != null && fragment.isHidden()) {
                    getFragmentManager().beginTransaction()
                            .show(fragment)
                            .commit();
                    fragment.onHiddenChanged(false);
                    fragment.onShown();
                }
            }
        } else if (isTaskRoot()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.exit_reader)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialog,
                                        int which) {
                                    ActivityMain.super.onBackPressed();
                                }
                            })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    public void onNavigationBackClickOld() {
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
                                                ActivityMain.super.onBackPressed();
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
            historian.saveToFile(this);
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

    public ComponentActivity getComponentActivity() {
        return componentActivity;
    }
}