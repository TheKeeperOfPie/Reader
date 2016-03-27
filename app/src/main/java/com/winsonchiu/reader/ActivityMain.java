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
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Browser;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.accounts.AccountsAdapter;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.ControllerCommentsTop;
import com.winsonchiu.reader.comments.FragmentComments;
import com.winsonchiu.reader.comments.FragmentReply;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.database.reddit.RedditOpenHelper;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
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
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverNext;
import com.winsonchiu.reader.search.ControllerSearch;
import com.winsonchiu.reader.search.FragmentSearch;
import com.winsonchiu.reader.settings.ActivitySettings;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.EventListenerBase;
import com.winsonchiu.reader.utils.ImageDownload;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.UtilsJson;
import com.winsonchiu.reader.utils.UtilsReddit;
import com.winsonchiu.reader.views.ScrollViewHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class ActivityMain extends AppCompatActivity
        implements FragmentListenerBase, SharedPreferences.OnSharedPreferenceChangeListener, AccountsAdapter.Listener {

    private static final String TAG = ActivityMain.class.getCanonicalName();

    public static final String REDDIT_PAGE = "redditPage";
    public static final String NAV_ID = "navId";
    public static final String NAV_PAGE = "navPage";
    public static final String ACCOUNT = "account";

    private static final int REQUEST_SETTINGS = 0;
    private static final long DURATION_CHECK_INBOX_ACTIVE = 120000;

    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 61;

    private int loadId = -1;

    private Handler handler = new Handler();
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
    private int colorAccent;

    private Link linkHeader;
    private ImageDownload imageDownload;
    private boolean isDownloadingHeaderImage;
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
            new Thread(() -> {
                try {
                    FileOutputStream fileOutputStream = openFileOutput(AppSettings.HEADER_FILE_NAME, Context.MODE_PRIVATE);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    fileOutputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                handler.post(() -> loadHeaderFromFile());
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

    private String themeBackground;
    private String themePrimary;
    private String themeAccent;
    private Theme theme;
    private ComponentActivity componentActivity;

    private CustomTabsServiceConnection customTabsServiceConnection;
    private CustomTabsClient customTabsClient;
    private CustomTabsSession customTabsSession;

    private AccountsAdapter adapterAccounts;
    private boolean accountsVisible;

    @Bind(R.id.layout_drawer) DrawerLayout layoutDrawer;
    @Bind(R.id.layout_navigation) RelativeLayout layoutNavigation;
    @Bind(R.id.view_navigation) NavigationView viewNavigation;

    @Bind(R.id.scroll_header_vertical) ScrollViewHeader scrollHeaderVertical;
    @Bind(R.id.scroll_header_horizontal) HorizontalScrollView scrollHeaderHorizontal;
    @Bind(R.id.image_header) ImageView imageHeader;
    @Bind(R.id.text_account_name) TextView textAccountName;
    @Bind(R.id.text_account_info) TextView textAccountInfo;
    @Bind(R.id.button_accounts) ImageButton buttonAccounts;
    @Bind(R.id.recycler_accounts) RecyclerView recyclerAccounts;

    @Inject AccountManager accountManager;
    @Inject ControllerLinks controllerLinks;
    @Inject ControllerUser controllerUser;
    @Inject ControllerCommentsTop controllerCommentsTop;
    @Inject ControllerProfile controllerProfile;
    @Inject ControllerInbox controllerInbox;
    @Inject ControllerSearch controllerSearch;
    @Inject ControllerHistory controllerHistory;
    @Inject Historian historian;
    @Inject Picasso picasso;
    @Inject Reddit reddit;
    @Inject SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        componentActivity = (ComponentActivity) getLastCustomNonConfigurationInstance();

        if (componentActivity == null) {
            componentActivity = CustomApplication.getComponentMain()
                    .componentActivity();
        }

        componentActivity.inject(this);

        boolean secret = sharedPreferences.getBoolean(AppSettings.SECRET, false);
        themePrimary = secret
                ? AppSettings.randomThemeString()
                : sharedPreferences.getString(AppSettings.PREF_THEME_PRIMARY, AppSettings.THEME_DEEP_PURPLE);
        themeAccent = secret
                ? AppSettings.randomThemeString()
                : sharedPreferences.getString(AppSettings.PREF_THEME_ACCENT, AppSettings.THEME_YELLOW);
        themeBackground = sharedPreferences.getString(AppSettings.PREF_THEME_BACKGROUND, AppSettings.THEME_DARK);
        theme = Theme.fromString(themePrimary);

        setTheme(theme.getStyle(themeBackground, themeAccent));

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.colorPrimary, R.attr.colorAccent});
        colorPrimary = typedArray.getColor(0, ContextCompat.getColor(this, R.color.colorPrimary));
        colorAccent = typedArray.getColor(0, ContextCompat.getColor(this, R.color.colorAccent));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int resourceIcon = UtilsColor.showOnWhite(colorPrimary) ? R.mipmap.app_icon_white_outline : R.mipmap.app_icon_dark_outline;

        colorFilterPrimary = new CustomColorFilter(ContextCompat.getColor(this, colorResourcePrimary), PorterDuff.Mode.MULTIPLY);

        /**
         * Required for {@link YouTubePlayerSupportFragment} to inflate proper UI
         */
        getLayoutInflater().setFactory(this);
        super.onCreate(savedInstanceState);

        handler = new Handler();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        inflateNavigationDrawer();

        Receiver.setAlarm(this);

        layoutDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (loadId != 0) {
                    selectNavigationItem(loadId, null, true);
                }
                setAccountsVisible(false);
            }
        });

        eventListenerBase = new EventListenerBase(componentActivity) {
            @Override
            public void onClickComments(Link link, AdapterLink.ViewHolderBase viewHolderBase, Source source) {
                controllerCommentsTop
                        .setLink(link, source);

                int color = viewHolderBase.getBackgroundColor();

                FragmentBase fragment = (FragmentBase) getSupportFragmentManager().findFragmentById(R.id.frame_fragment);
                fragment.onWindowTransitionStart();

                FragmentComments fragmentComments = FragmentComments
                        .newInstance(viewHolderBase, color);
                fragmentComments.setFragmentToHide(fragment, viewHolderBase.itemView);

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, fragmentComments,
                                FragmentComments.TAG)
                        .addToBackStack(null)
                        .commit();
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
            public void downloadImage(String title, String fileName, String url) {
                imageDownload = new ImageDownload(title, fileName, url);
                ActivityCompat.requestPermissions(ActivityMain.this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
            }

            @Override
            public void editLink(Link link) {
                FragmentNewPost fragmentNewPost = FragmentNewPost.newInstanceEdit(controllerUser.getUser().getName(), link);

                getSupportFragmentManager().beginTransaction()
                        .hide(getSupportFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, fragmentNewPost, FragmentNewPost.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void showReplyEditor(Replyable replyable) {
                FragmentReply fragmentReply = FragmentReply.newInstance(replyable);
                fragmentReply.setFragmentToHide(getSupportFragmentManager().findFragmentById(R.id.frame_fragment));

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, fragmentReply, FragmentReply.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void loadWebFragment(String url) {
                launchUrl(url, false);
            }

            @Override
            public void launchScreen(Intent intent) {
                startActivity(intent);
            }
        };

        eventListenerComment = (comment, subreddit, linkId) -> {
            if (comment.getCount() == 0) {
                Intent intentCommentThread = new Intent(ActivityMain.this, ActivityMain.class);
                intentCommentThread.setAction(Intent.ACTION_VIEW);
                // Double slashes used to trigger parseUrl correctly
                intentCommentThread.putExtra(REDDIT_PAGE, Reddit.BASE_URL + "/r/" + subreddit + "/comments/" + linkId + "/title/" + comment.getParentId() + "/");
                startActivity(intentCommentThread);
                return true;
            }

            return false;
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Reader", BitmapFactory.decodeResource(getResources(), resourceIcon), colorPrimary);
            setTaskDescription(taskDescription);
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

        CustomTabsClient.bindCustomTabsService(this, getPackageName(), new CustomTabsServiceConnection() {
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
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        });

        handleFirstLaunch(savedInstanceState);

        showBetaNotice();

        loadAccount();

    }

    public void handleFirstLaunch(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
                Log.d(TAG, "load intent: " + getIntent().toString());
                String urlString = getIntent().getDataString();
                if (getIntent().hasExtra(REDDIT_PAGE)) {
                    urlString = getIntent().getExtras()
                            .getString(REDDIT_PAGE);
                }

                if (URLUtil.isNetworkUrl(urlString)) {
                    parseUrl(urlString);
                }
                else {
                    Log.d(TAG, "Not valid URL: " + urlString);
                    selectNavigationItem(getIntent().getIntExtra(NAV_ID, R.id.item_home), getIntent().getStringExtra(
                            NAV_PAGE), false);
                }
            }
            else {
                selectNavigationItem(getIntent().getIntExtra(NAV_ID, R.id.item_home), getIntent().getStringExtra(
                        NAV_PAGE), false);
            }
        }
    }

    public void showBetaNotice() {
        if (sharedPreferences.getBoolean(AppSettings.BETA_NOTICE_0, true)) {
            try {

                View view = LayoutInflater.from(this).inflate(R.layout.dialog_text_alert, null, false);
                TextView textTitle = ButterKnife.findById(view, R.id.text_title);
                TextView textMessage = ButterKnife.findById(view, R.id.text_message);

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
    }

    public void loadAccount() {
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
    public Object onRetainCustomNonConfigurationInstance() {
        return componentActivity;
    }

    protected void superOnCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.need_permission_download_image, Toast.LENGTH_LONG).show();
                }
                else if (imageDownload != null){
                    File destination = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ReaderForReddit" + File.separator + imageDownload.getFileName() + UtilsImage.getImageFileEnding(imageDownload.getUrl()));

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageDownload.getUrl()));
                    request.setDestinationUri(Uri.fromFile(destination));

                    DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    downloadManager.enqueue(request);
                }
        }
    }

    private void inflateNavigationDrawer() {

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

        layoutNavigation.getLayoutParams().width = (int) navigationWidth;

        textAccountName.setTextColor(colorFilterPrimary.getColor());
        textAccountInfo.setTextColor(colorFilterPrimary.getColor());

        buttonAccounts.setColorFilter(colorFilterPrimary);

        View.OnClickListener onClickListener = v -> setAccountsVisible(!accountsVisible);

        textAccountName.setOnClickListener(onClickListener);
        textAccountInfo.setOnClickListener(onClickListener);
        buttonAccounts.setOnClickListener(onClickListener);

        adapterAccounts = new AccountsAdapter(colorFilterPrimary.getColor(), this);
        recyclerAccounts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerAccounts.setAdapter(adapterAccounts);

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
                menuItem -> {
                    loadId = menuItem.getItemId();
                    layoutDrawer.closeDrawer(GravityCompat.START);
                    return true;
                });

        scrollHeaderVertical.setDispatchTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);

            switch (MotionEventCompat.getActionMasked(event)) {
                case MotionEvent.ACTION_DOWN:
                    layoutDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    layoutDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    break;
            }

            return false;
        });

        loadHeaderImage(false);
    }

    private void calculateShowFrontPage() {
        boolean showFrontPage = !TextUtils.isEmpty(sharedPreferences.getString(AppSettings.PREF_HOME_SUBREDDIT, null));

        MenuItem itemFrontPage = viewNavigation.getMenu().findItem(R.id.item_front_page);
        itemFrontPage.setVisible(showFrontPage);
        itemFrontPage.setEnabled(showFrontPage);
    }

    private void loadHeaderImage(boolean force) {

        // TODO: Implement expiration enabled history
        imageHeader.setAlpha(0.5f);

        if (!force && getFileStreamPath(AppSettings.HEADER_FILE_NAME).exists() && System.currentTimeMillis() < sharedPreferences.getLong(AppSettings.HEADER_EXPIRATION, 0)) {
            loadHeaderFromFile();
            return;
        }

        String subreddit = UtilsReddit.parseSubredditUrlPart(sharedPreferences.getString(AppSettings.PREF_HEADER_SUBREDDIT, "earthporn"));

        if (TextUtils.isEmpty(subreddit)) {
            Toast.makeText(this, R.string.header_subreddit_error, Toast.LENGTH_LONG).show();
            picasso.cancelRequest(imageHeader);
            imageHeader.setImageDrawable(null);
            return;
        }

        reddit.links(subreddit, Sort.HOT.toString(), Time.ALL.toString(), 25, null)
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
                                if (UtilsImage.checkIsImageUrl(link.getUrl()) && !thing.getName()
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

    private void resetAccountList() {
        int startSize = adapterAccounts.getItemCount();

        adapterAccounts.setAccounts(accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE));

        if (startSize != 0 && startSize != adapterAccounts.getItemCount()) {
            recyclerAccounts.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            recyclerAccounts.requestLayout();
        }
    }

    public void addNewAccount() {
        accountManager.addAccount(Reddit.ACCOUNT_TYPE, Reddit.AUTH_TOKEN_FULL_ACCESS, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(final AccountManagerFuture<Bundle> future) {
                Observable.just(future)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .flatMap(new Func1<AccountManagerFuture<Bundle>, Observable<Bundle>>() {
                            @Override
                            public Observable<Bundle> call(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                                try {
                                    return Observable.just(future.getResult());
                                } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                                    return Observable.error(e);
                                }
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new FinalizingSubscriber<Bundle>() {
                            @Override
                            public void next(Bundle next) {
                                super.next(next);
                                String name = next.getString(AccountManager.KEY_ACCOUNT_NAME);
                                Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);

                                for (Account account : accounts) {
                                    if (account.name.equals(name)) {
                                        setAccount(account);
                                        break;
                                    }
                                }
                            }

                            @Override
                            public void error(Throwable e) {
                                super.error(e);
                                Toast.makeText(ActivityMain.this, R.string.error_account, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void finish() {
                                super.finish();
                                resetAccountList();
                            }
                        });
            }
        }, null);
    }

    public void clearAccount() {
        accountUser = null;
        sharedPreferences.edit().putString(AppSettings.ACCOUNT_NAME, "").apply();
        reddit.clearAccount();
        controllerUser.clearAccount();
        controllerSearch.reloadSubscriptionList();
        loadAccountInfo();
    }

    public void setAccount(final Account account) {
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

    public void deleteAccount(final Account account) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(account.name)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String tokenRefresh = accountManager.getPassword(account);

                    reddit.tokenRevoke(Reddit.QUERY_REFRESH_TOKEN, tokenRefresh)
                            .observeOn(Schedulers.computation())
                            .flatMap(s -> {
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
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setAccountsVisible(boolean visible) {
        accountsVisible = visible;

        buttonAccounts.setImageResource(accountsVisible
                ? R.drawable.ic_arrow_drop_down_white_24dp
                : R.drawable.ic_arrow_drop_up_white_24dp);

        if (layoutDrawer.isDrawerOpen(GravityCompat.START)) {
            if (accountsVisible) {
                if (recyclerAccounts.getVisibility() == View.GONE) {
                    recyclerAccounts.getLayoutParams().height = 0;
                }
                UtilsAnimation.animateExpandHeight(recyclerAccounts, viewNavigation.getWidth(), 0, null);
            } else {
                UtilsAnimation.animateCollapseHeight(recyclerAccounts, 0, null);
            }
        } else {
            recyclerAccounts.setVisibility(accountsVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void selectNavigationItem(final int id, String page, boolean animate) {

        loadId = 0;

        switch (id) {
            case R.id.item_settings:
                Intent intentSettings = new Intent(this, ActivitySettings.class);
                startActivityForResult(intentSettings, REQUEST_SETTINGS);
                return;
            case R.id.item_delete:
                deleteDatabase(RedditOpenHelper.NAME);
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

        getSupportFragmentManager().popBackStackImmediate();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        if (animate) {
            fragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }

        while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        }

        FragmentBase fragmentThreadList;

        switch (id) {
            case R.id.item_home:
                fragmentThreadList = (FragmentBase) getSupportFragmentManager().findFragmentByTag(FragmentThreadList.TAG);
                if (fragmentThreadList != null) {
                    fragmentThreadList.onHiddenChanged(false);
                }
                else {
                    fragmentTransaction.replace(R.id.frame_fragment,
                            FragmentThreadList.newInstance(),
                            FragmentThreadList.TAG);
                }

                loadHomeSubreddit();
                break;
            case R.id.item_front_page:
                controllerLinks.loadFrontPage(Sort.HOT, false);
                break;
            case R.id.item_history:
                controllerHistory.reload();
                if (getSupportFragmentManager().findFragmentByTag(FragmentHistory.TAG) == null) {
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
                fragmentThreadList = (FragmentBase) getSupportFragmentManager().findFragmentByTag(FragmentThreadList.TAG);
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

    private void loadHomeSubreddit() {
        String subreddit = UtilsReddit.parseRawSubredditString(sharedPreferences.getString(AppSettings.PREF_HOME_SUBREDDIT, null));

        if (TextUtils.isEmpty(subreddit)) {
            controllerLinks.loadFrontPage(Sort.HOT, true);
        }
        else {
            controllerLinks.setParameters(subreddit, Sort.HOT, Time.ALL);
        }
    }

    private void loadAccountInfo() {
        boolean visible = controllerUser.hasUser();

        if (visible) {
            reddit.me()
                    .flatMap(response -> {
                        try {
                            return Observable.just(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class));
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    })
                    .subscribe(new ObserverNext<JsonNode>() {
                        @Override
                        public void onNext(JsonNode jsonNode) {
                            textAccountName.setText(UtilsJson.getString(jsonNode.get("name")));
                            textAccountInfo.setText(getString(R.string.account_info, UtilsJson.getString(
                                    jsonNode.get("link_karma")), UtilsJson.getString(jsonNode.get("comment_karma"))));
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

    private void loadSubreddit(String subreddit, Sort sort, Time time) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment, FragmentThreadList.newInstance(),
                        FragmentThreadList.TAG)
                .commit();
        controllerLinks.setParameters(subreddit, sort, time);
    }

    private void loadComments(String idLink, String idComments, int context) {
        if (TextUtils.isEmpty(idComments)) {
            controllerCommentsTop.setLinkId(idLink, Source.NONE);
        }
        else {
            controllerCommentsTop.setLinkId(idLink, idComments, context, Source.NONE);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_fragment,
                        FragmentComments.newInstance(),
                        FragmentComments.TAG)
                .commit();
    }

    private void loadProfile(String user) {
        getSupportFragmentManager().beginTransaction()
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
                                getSupportFragmentManager().beginTransaction()
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
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
        else {
            if (layoutDrawer.isDrawerVisible(GravityCompat.START)) {
                layoutDrawer.closeDrawer(GravityCompat.START);
            }
            else {
                layoutDrawer.openDrawer(GravityCompat.START);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                if (!URLUtil.isNetworkUrl(urlString)) {
                    urlString = Reddit.BASE_URL + urlString;
                }
            }

            if (match != Match.NONE) {
                intent.setData(uri);
                intent.setComponent(new ComponentName(getApplicationContext().getPackageName(), ActivityMain.class.getCanonicalName()));
                super.startActivity(intent);
            }
            else if (URLUtil.isNetworkUrl(urlString)) {
                FragmentBase fragment = (FragmentBase) getSupportFragmentManager().findFragmentById(R.id.frame_fragment);
                if (fragment != null) {
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
                .setStartAnimations(this, R.anim.slide_from_bottom, R.anim.nothing)
                .setExitAnimations(this, R.anim.nothing, R.anim.slide_to_bottom)
                .setToolbarColor(colorPrimary)
                .setSecondaryToolbarColor(colorAccent)
                .setShowTitle(true)
                .enableUrlBarHiding()
                .addDefaultShareMenuItem()
                .build();

        Intent intentChrome = getIntentForChrome();

        if (intentChrome == null) {
            if (replace) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_fragment, FragmentWeb.newInstance(url), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .hide(getSupportFragmentManager().findFragmentById(R.id.frame_fragment))
                        .add(R.id.frame_fragment, FragmentWeb.newInstance(url), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        } else {
            intentCustomTabs.intent.setComponent(intentChrome.getComponent());
            intentCustomTabs.launchUrl(this, Uri.parse(url));
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

        return intentChrome;
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
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();

        if (backStackCount > 0) {
            FragmentBase fragment = (FragmentBase) getSupportFragmentManager().findFragmentById(R.id.frame_fragment);
            if (fragment != null && !fragment.isFinished()) {
                fragment.navigateBack();
                return;
            }
            if (backStackCount == 1) {
                super.onBackPressed();
            }
            else {
                getSupportFragmentManager().popBackStackImmediate();

                fragment = (FragmentBase) getSupportFragmentManager().findFragmentById(R.id.frame_fragment);

                if (fragment != null && fragment.isHidden()) {
                    getSupportFragmentManager().beginTransaction()
                            .show(fragment)
                            .commit();
                    fragment.onHiddenChanged(false);
                    fragment.onShown();
                }
            }
        }
        else if (isTaskRoot()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.exit_reader)
                    .setPositiveButton(R.string.yes, (dialog, which) -> ActivityMain.super.onBackPressed())
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void openDrawer() {
        layoutDrawer.openDrawer(GravityCompat.START);
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
    protected void onStart() {
        super.onStart();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        calculateShowFrontPage();
    }

    @Override
    protected void onStop() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (isTaskRoot()) {
            historian.saveToFile(this);
        }
        super.onStop();
    }

    public ComponentActivity getComponentActivity() {
        return componentActivity;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case AppSettings.PREF_HOME_SUBREDDIT:
                calculateShowFrontPage();
                break;
        }
    }
}