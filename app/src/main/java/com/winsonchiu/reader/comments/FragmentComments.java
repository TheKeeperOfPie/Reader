/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.links.LinksController;
import com.winsonchiu.reader.links.LinksModel;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.search.ControllerSearch;
import com.winsonchiu.reader.utils.RecyclerFragmentPagerAdapter;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.UtilsList;
import com.winsonchiu.reader.utils.UtilsRx;
import com.winsonchiu.reader.utils.UtilsTheme;
import com.winsonchiu.reader.utils.YouTubeListener;
import com.winsonchiu.reader.utils.YouTubePlayerStateListener;
import com.winsonchiu.reader.views.CustomFrameLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscription;

public class FragmentComments extends FragmentBase
        implements Toolbar.OnMenuItemClickListener, View.OnTouchListener {

    public static final String TAG = FragmentComments.class.getCanonicalName();

    private static final String ARG_IS_GRID = "isGrid";
    private static final String ARG_FIRST_LINK_NAME = "firstLinkName";
    private static final String ARG_COLOR_LINK = "colorLink";
    private static final String ARG_LOCATION = "location";
    private static final String ARG_ITEM_HEIGHT = "itemHeight";
    private static final String ARG_ITEM_WIDTH = "itemWidth";
    private static final String ARG_INITIALIZED = "initialized";
    private static final String ARG_ACTIONS_EXPANDED = "actionsExpanded";
    private static final String ARG_YOUTUBE_ID = "youTubeId";
    private static final String ARG_YOUTUBE_TIME = "youTubeTime";

    public static final long DELAY_ENTER = 200;
    public static final long DURATION_ENTER = 350;
    public static final long DURATION_EXIT = 350;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.5f;

    private FragmentListenerBase mListener;
    private ScrollAwareFloatingActionButtonBehavior behaviorFloatingActionButton;
    private FastOutSlowInInterpolator fastOutSlowInInterpolator;
    private FragmentBase fragmentToHide;
    private String fragmentParentTag;
    private boolean isFullscreen;
    private int viewHolderWidth;
    private float startX;
    private float startY;
    private int startMarginRight;
    private MenuItem itemLoadFullComments;
    private MenuItem itemHideYouTube;
    private MenuItem itemExpandPost;
    private View viewHolderView;
    private float toolbarHeight;
    private GestureDetectorCompat gestureDetector;
    private SharedPreferences preferences;
    private boolean hasSwipedEnd;
    private CustomFrameLayout layoutRoot;
    private boolean isFinished;
    private float swipeDifferenceX;
    private float swipeEndDistance;

    private YouTubePlayer youTubePlayer;
    private YouTubeListener youTubeListener;
    private String currentYouTubeId;
    private int youTubeViewId = View.generateViewId();
    private YouTubePlayerSupportFragment youTubeFragment;

    private boolean isStartOnLeft;
    private boolean animationFinished;

    private FragmentCommentsInner.Callback fragmentCallback;
    private Link linkTop;
    private int positionCurrent;
    private int indexStart;
    private FragmentCommentsInner fragmentCurrent;
    private RecyclerFragmentPagerAdapter<FragmentCommentsInner> adapterComments;
    private Source source = Source.NONE;

    private Subscription subscriptionLinks;
    private Subscription subscriptionInsertions;
    private Subscription subscriptionUpdatesNsfw;
    private Subscription subscriptionUpdatesComment;
    private Subscription subscriptionUpdatesReplyText;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.layout_actions) ViewGroup layoutActions;
    @BindView(R.id.button_expand_actions) FloatingActionButton buttonExpandActions;
    @BindView(R.id.button_jump_top) FloatingActionButton buttonJumpTop;
    @BindView(R.id.button_comment_previous) FloatingActionButton buttonCommentPrevious;
    @BindView(R.id.button_comment_next) FloatingActionButton buttonCommentNext;
    @BindView(R.id.layout_youtube) ViewGroup layoutYouTube;
    @BindView(R.id.layout_coordinator) CoordinatorLayout layoutCoordinator;
    @BindView(R.id.layout_app_bar) AppBarLayout layoutAppBar;
    @BindView(R.id.layout_comments) CustomFrameLayout layoutComments;
    @BindView(R.id.view_background) View viewBackground;
    @BindView(R.id.pager_comments) ViewPager pagerComments;

    @Inject ControllerCommentsTop controllerCommentsTop;
    @Inject ControllerLinks controllerLinks;
    @Inject ControllerSearch controllerSearch;
    @Inject ControllerHistory controllerHistory;
    @Inject ControllerProfile controllerProfile;
    @Inject Historian historian;

    private LinksModel linksModel = new LinksModel();

    public static FragmentComments newInstance() {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_GRID, false);
        args.putInt(ARG_COLOR_LINK, 0);
        args.putIntArray(ARG_LOCATION, new int[2]);
        args.putInt(ARG_ITEM_HEIGHT, 0);
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentComments newInstance(AdapterLink.ViewHolderLink viewHolder,
            int colorLink) {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        if (viewHolder.itemView
                .getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            args.putBoolean(ARG_IS_GRID, true);
        }
        int[] location = viewHolder.getScreenAnchor();

        Link link = viewHolder.getLink();

        args.putIntArray(ARG_LOCATION, location);
        args.putString(ARG_FIRST_LINK_NAME, link.getName());
        args.putInt(ARG_COLOR_LINK, colorLink);
        args.putInt(ARG_ITEM_HEIGHT, viewHolder.itemView.getHeight());
        args.putInt(ARG_ITEM_WIDTH, viewHolder.itemView.getWidth());
        args.putBoolean(ARG_ACTIONS_EXPANDED, viewHolder.layoutContainerExpand.isShown());

        if (!TextUtils.isEmpty(link.getYouTubeId()) && link.getYouTubeTime() >= 0) {
            args.putString(ARG_YOUTUBE_ID, link.getYouTubeId());
            args.putInt(ARG_YOUTUBE_TIME, link.getYouTubeTime());
        }

        fragment.setArguments(args);
        return fragment;
    }

    public FragmentComments() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        fastOutSlowInInterpolator = new FastOutSlowInInterpolator();
    }

    private void setUpToolbar() {
        if (getFragmentManager().getBackStackEntryCount() == 0 && getActivity()
                .isTaskRoot() && getFragmentManager()
                .findFragmentByTag(fragmentParentTag) == null) {
            toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            toolbar.setNavigationOnClickListener(v -> mListener.openDrawer());
        }
        else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setNavigationOnClickListener(v -> mListener.onNavigationBackClick());
        }

        toolbar.getNavigationIcon().mutate().setColorFilter(themer.getColorFilterPrimary());

        toolbar.inflateMenu(R.menu.menu_comments);

        Menu menu = toolbar.getMenu();

        itemLoadFullComments = menu.findItem(R.id.item_load_full_comments);
        itemHideYouTube = menu.findItem(R.id.item_hide_youtube);
        itemExpandPost = menu.findItem(R.id.item_expand_post);
        toolbar.setOnMenuItemClickListener(this);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
        }
    }

    @Override
    protected void inject() {
        ((ActivityMain) getActivity()).getComponentActivity().inject(this);
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layoutRoot = (CustomFrameLayout) inflater
                .inflate(R.layout.fragment_comments, container, false);
        ButterKnife.bind(this, layoutRoot);

        youTubeListener = new YouTubeListener() {
            @Override
            public void loadYouTubeVideo(Link link, final String id, final int timeInMillis) {
                loadYoutubeVideo(id, timeInMillis);
            }

            @Override
            public boolean hideYouTube() {
                if (layoutYouTube.isShown()) {
                    if (youTubePlayer != null) {
                        youTubePlayer.pause();
                    }
                    toggleYouTubeVisibility(View.GONE);
                    return false;
                }

                return true;
            }
        };

        toolbar.setTitleTextColor(themer.getColorFilterPrimary().getColor());
        toolbar.setBackgroundColor(themer.getColorPrimary());
        setUpToolbar();

        behaviorFloatingActionButton = new ScrollAwareFloatingActionButtonBehavior(getActivity(),
                null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                        hideLayoutActions(0);
                    }

                    @Override
                    public void onEndHideFromScroll() {
                        buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                        buttonExpandActions.setColorFilter(themer.getColorFilterAccent());
                    }

                });

        buttonExpandActions.setOnClickListener(v -> toggleLayoutActions());
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                .setBehavior(behaviorFloatingActionButton);

        buttonJumpTop.setOnClickListener(v -> fragmentCurrent.scrollToPositionWithOffset(0, 0));
        buttonJumpTop.setOnLongClickListener(v -> {
            Toast.makeText(getActivity(),
                    getString(R.string.content_description_button_jump_top),
                    Toast.LENGTH_SHORT).show();
            return false;
        });

        buttonCommentPrevious.setOnClickListener(v -> fragmentCurrent.previousComment());
        buttonCommentPrevious.setOnLongClickListener(v -> {
            Toast.makeText(getActivity(),
                    getString(R.string.content_description_button_comment_previous),
                    Toast.LENGTH_SHORT).show();
            return false;
        });

        buttonCommentNext.setOnClickListener(v -> fragmentCurrent.nextComment());
        buttonCommentNext.setOnLongClickListener(v -> {
            Toast.makeText(getActivity(),
                    getString(R.string.content_description_button_comment_next),
                    Toast.LENGTH_SHORT).show();
            return false;
        });

        // Margin is included within shadow margin on pre-Lollipop, so remove all regular margin
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                    .setMargins(0, 0, 0, 0);

            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                    getResources().getDisplayMetrics());

            LinearLayout.LayoutParams layoutParamsJumpTop = (LinearLayout.LayoutParams) buttonJumpTop
                    .getLayoutParams();
            layoutParamsJumpTop.setMargins(0, 0, 0, 0);
            buttonJumpTop.setLayoutParams(layoutParamsJumpTop);

            LinearLayout.LayoutParams layoutParamsPrevious = (LinearLayout.LayoutParams) buttonCommentPrevious
                    .getLayoutParams();
            layoutParamsPrevious.setMargins(0, 0, 0, 0);
            buttonCommentPrevious.setLayoutParams(layoutParamsPrevious);

            LinearLayout.LayoutParams layoutParamsNext = (LinearLayout.LayoutParams) buttonCommentNext
                    .getLayoutParams();
            layoutParamsNext.setMargins(0, 0, 0, 0);
            buttonCommentNext.setLayoutParams(layoutParamsNext);

            ViewGroup.MarginLayoutParams layoutParamsActions = (ViewGroup.MarginLayoutParams) layoutActions
                    .getLayoutParams();
            layoutParamsActions.setMarginStart(margin);
            layoutParamsActions.setMarginEnd(margin);
            layoutActions.setLayoutParams(layoutParamsActions);
        }

        buttonExpandActions.setColorFilter(themer.getColorFilterAccent());
        buttonJumpTop.setColorFilter(themer.getColorFilterAccent());
        buttonCommentPrevious.setColorFilter(themer.getColorFilterAccent());
        buttonCommentNext.setColorFilter(themer.getColorFilterAccent());

        isStartOnLeft = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        final float screenWidth = getResources().getDisplayMetrics().widthPixels;
        swipeEndDistance = screenWidth * 0.4f;

        gestureDetector = new GestureDetectorCompat(getActivity(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onScroll(MotionEvent e1,
                            MotionEvent e2,
                            float distanceX,
                            float distanceY) {

                        // TODO: Implement a fling gesture based on distance-based velocity

                        if (isFinished) {
                            return true;
                        }

                        swipeDifferenceX = e2.getX() - e1.getX();

                        if (isStartOnLeft) {
                            if (e1.getX() > screenWidth * 0.2f || swipeDifferenceX < 0) {
                                return super.onScroll(e1, e2, distanceX, distanceY);
                            }
                        }
                        else {
                            if (e1.getX() < screenWidth * 0.8f || swipeDifferenceX > 0) {
                                return super.onScroll(e1, e2, distanceX, distanceY);
                            }
                        }

                        if (!hasSwipedEnd) {
                            FragmentBase fragment = (FragmentBase) getFragmentManager()
                                    .findFragmentByTag(fragmentParentTag);
                            if (fragment != null) {
                                fragment.setVisibilityOfThing(View.VISIBLE, linkTop);
                                fragment.onHiddenChanged(false);
                            }
                            hasSwipedEnd = true;
                        }
                        float ratio = 1f - swipeDifferenceX / screenWidth;
                        buttonExpandActions.setAlpha(ratio);
                        buttonExpandActions.setScaleX(ratio);
                        buttonExpandActions.setScaleY(ratio);
                        layoutAppBar.setTranslationX(swipeDifferenceX);
                        layoutComments.setTranslationX(swipeDifferenceX);
                        viewBackground.setTranslationX(swipeDifferenceX);

                        return super.onScroll(e1, e2, distanceX, distanceY);
                    }
                });

        if (preferences.getBoolean(AppSettings.SWIPE_EXIT_COMMENTS, true)) {
            layoutRoot.setDispatchTouchListener(this);
        }

        if (savedInstanceState == null) {
            if (getArguments().getInt(ARG_YOUTUBE_TIME, -1) >= 0) {
                loadYoutubeVideo(getArguments().getString(ARG_YOUTUBE_ID), getArguments().getInt(ARG_YOUTUBE_TIME));
            }
        }
        else {
            String youtubeId = savedInstanceState.getString(ARG_YOUTUBE_ID, null);

            if (!TextUtils.isEmpty(youtubeId)) {
                loadYoutubeVideo(youtubeId, savedInstanceState.getInt(ARG_YOUTUBE_TIME, 0));
            }
        }

        fragmentCallback = new FragmentCommentsInner.Callback() {
            @Override
            public void loadYouTubeVideo(String id, int timeInMillis) {
                FragmentComments.this.loadYoutubeVideo(id, timeInMillis);
            }

            @Override
            public void releaseYouTube() {
                FragmentComments.this.releaseYouTube();
            }

            @Override
            public void setPostExpanded(boolean expanded) {
                FragmentComments.this.setPostExpanded(expanded);
            }

            @Override
            public void setIsCommentThread(boolean isCommentThread) {
                FragmentComments.this.setIsCommentThread(isCommentThread);
            }

            @Override
            public void setSort(Sort sort) {
                FragmentComments.this.setSort(sort);
            }

            @Override
            public void setTitle(CharSequence title) {
                FragmentComments.this.setTitle(title);
            }

            @Override
            public void clearDecoration() {
                behaviorFloatingActionButton.animateOut(buttonExpandActions);
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar
                        .getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar
                        .getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public int getAppBarHeight() {
                return layoutAppBar.getHeight();
            }

            @Override
            public boolean isCurrentFragment(FragmentCommentsInner fragmentCommentsInner) {
                return positionCurrent == fragmentCommentsInner.getPosition();
            }

            @Override
            public void loadYouTubeVideo(Link link, String id, int timeInMillis) {
                youTubeListener.loadYouTubeVideo(link, id, timeInMillis);
            }

            @Override
            public boolean hideYouTube() {
                return youTubeListener.hideYouTube();
            }
        };

        setUpPager();

        if (!getArguments().getBoolean(ARG_INITIALIZED, false)) {
            viewBackground.setVisibility(View.INVISIBLE);
            pagerComments.setVisibility(View.INVISIBLE);
            layoutAppBar.setVisibility(View.GONE);

            layoutComments.postOnAnimation(() -> animateEnter(layoutRoot));
        }
        else {
            setAnimationFinished(true);
        }

        return layoutRoot;
    }

    @Override
    public void onResume() {
        super.onResume();

        subscribe();
    }

    private void subscribe() {
        CommentsTopModel data = controllerCommentsTop.getEventHolder().getData().getValue();
        Link linkStart = data.getLinkModel().getLink();
        Source source = data.getSource();

        LinksController linksController = getLinksControllerFromSource(source);

        if (linksController == null) {
            this.linksModel.getLinks().clear();
            this.linksModel.getLinks().add(linkStart);
        } else {
            subscriptionLinks = linksController.getEventHolder()
                    .getData()
                    .subscribe(linksModel -> {
                        this.linksModel = linksModel;

                        if (indexStart == 0) {
                            indexStart = UtilsList.indexOf(linksModel.getLinks(), link -> linkStart.getId().equals(link.getId()));
                            positionCurrent = indexStart;
                        }

                        adapterComments.notifyDataSetChanged();
                        pagerComments.setCurrentItem(positionCurrent, false);
                    });
        }
    }

    @Nullable
    private LinksController getLinksControllerFromSource(Source source) {
        switch (source) {
            case LINKS:
                return controllerLinks;
//            case SEARCH_LINKS:
//                return controllerSearch;
//            case SEARCH_LINKS_SUBREDDIT:
//                return controllerSearch;
//            case HISTORY:
//                return controllerHistory;
            default:
            case PROFILE:
            case NONE:
                return null;
        }
    }

    private void setUpPager() {
        CommentsTopModel data = controllerCommentsTop.getEventHolder().getData().getValue();
        linkTop = data.getLinkModel().getLink();
        source = data.getSource();

        adapterComments = new RecyclerFragmentPagerAdapter<FragmentCommentsInner>(getFragmentManager()) {

            @Override
            public FragmentCommentsInner createFragment() {
                return FragmentCommentsInner.newInstance(getArguments().getBoolean(ARG_IS_GRID, false), getArguments().getString(ARG_FIRST_LINK_NAME), getArguments().getInt(ARG_COLOR_LINK, 0));
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                Link link = linksModel.getLinks().get(position);

                FragmentCommentsInner fragment = (FragmentCommentsInner) super.instantiateItem(container, position);
                fragment.createControllerComments(((ActivityMain) getActivity()).getComponentActivity());
                fragment.setLink(link);
                fragment.setCallback(fragmentCallback);
                fragment.setAnimationFinished(animationFinished);
                fragment.setPosition(position);

                if (fragmentCurrent == null && indexStart == position) {
                    setCurrentFragment(fragment);
                }

                return fragment;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                super.destroyItem(container, position, object);

                FragmentCommentsInner fragmentCommentsInner = (FragmentCommentsInner) object;
                fragmentCommentsInner.clear();
            }

            @Override
            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }

            @Override
            public int getCount() {
                return linksModel.getLinks().size();
            }
        };

        pagerComments.setAdapter(adapterComments);
    }

    private void setCurrentFragment(FragmentCommentsInner fragment) {
        fragmentCurrent = fragment;

        setPostExpanded(fragmentCurrent.getPostExpanded());
        setTitle(fragmentCurrent.getTitle());
        fragmentCurrent.setAnimationFinished(animationFinished);

        // Don't add link to history if it's viewed from history
        switch (source) {
            case HISTORY:
                break;
            case NONE:
            case LINKS:
            case SEARCH_LINKS:
            case SEARCH_LINKS_SUBREDDIT:
            case PROFILE:
                historian.add(fragmentCurrent.getLink());
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        ControllerCommentsTop.EventHolder eventHolder = controllerCommentsTop.getEventHolder();

        subscriptionInsertions = eventHolder.getInsertions()
                .subscribe(comment -> fragmentCurrent.getControllerComments().insertComment(comment));

        subscriptionUpdatesNsfw = eventHolder.getUpdatesNsfw()
                .subscribe(pair -> fragmentCurrent.getControllerComments().setNsfw(pair.first, pair.second));

        subscriptionUpdatesComment = eventHolder.getUpdatesComment()
                .subscribe(comment -> {
                    // TODO: Implement this
                });

        subscriptionUpdatesReplyText = eventHolder.getUpdatesReplyText()
                .subscribe(comment -> fragmentCurrent.getControllerComments().setReplyText(comment));
    }

    @Override
    public void onStop() {
        UtilsRx.unsubscribe(subscriptionInsertions);
        UtilsRx.unsubscribe(subscriptionUpdatesNsfw);
        UtilsRx.unsubscribe(subscriptionUpdatesComment);
        UtilsRx.unsubscribe(subscriptionUpdatesReplyText);
        super.onStop();
    }

    private void loadYoutubeVideo(final String id, final int timeInMillis) {

        if (youTubePlayer != null) {
            toggleYouTubeVisibility(View.VISIBLE);
            if (!id.equals(currentYouTubeId)) {
                youTubePlayer.loadVideo(id);
                currentYouTubeId = id;
            }
            return;
        }

        youTubeFragment = new YouTubePlayerSupportFragment();
        layoutYouTube.setId(youTubeViewId);
        getFragmentManager().beginTransaction()
                .add(youTubeViewId, youTubeFragment, String.valueOf(youTubeViewId))
                .commit();
        youTubeFragment.initialize(ApiKeys.YOUTUBE_API_KEY,
                new YouTubePlayer.OnInitializedListener() {
                    @Override
                    public void onInitializationSuccess(YouTubePlayer.Provider provider,
                            YouTubePlayer player,
                            boolean b) {
                        FragmentComments.this.youTubePlayer = player;

                        youTubePlayer.setManageAudioFocus(false);
                        youTubePlayer.setFullscreenControlFlags(
                                YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);

                        DisplayMetrics displayMetrics = getActivity().getResources()
                                .getDisplayMetrics();

                        boolean isLandscape = displayMetrics.widthPixels > displayMetrics.heightPixels;

                        if (isLandscape) {
                            youTubePlayer.setOnFullscreenListener(
                                    new YouTubePlayer.OnFullscreenListener() {
                                        @Override
                                        public void onFullscreen(boolean fullscreen) {
                                            isFullscreen = fullscreen;
                                            if (!fullscreen) {
                                                releaseYouTube();
                                                toggleYouTubeVisibility(View.GONE);
                                            }
                                        }
                                    });
                        }
                        else {
                            youTubePlayer.setOnFullscreenListener(
                                    new YouTubePlayer.OnFullscreenListener() {
                                        @Override
                                        public void onFullscreen(boolean fullscreen) {
                                            isFullscreen = fullscreen;
                                        }
                                    });
                        }
                        youTubePlayer.setPlayerStateChangeListener(
                                new YouTubePlayerStateListener(youTubePlayer, timeInMillis,
                                        isLandscape));
                        toggleYouTubeVisibility(View.VISIBLE);
                        youTubePlayer.loadVideo(id);
                        currentYouTubeId = id;
                    }

                    @Override
                    public void onInitializationFailure(YouTubePlayer.Provider provider,
                            YouTubeInitializationResult youTubeInitializationResult) {
                    }
                });
    }

    private void toggleYouTubeVisibility(int visibility) {
        layoutYouTube.setVisibility(visibility);
        boolean visible = visibility == View.VISIBLE;
//        recyclerCommentList.scrollBy(0, visible ? viewYouTube.getHeight() : -viewYouTube.getHeight());
        itemHideYouTube.setVisible(visible);
        itemHideYouTube.setEnabled(visible);
    }

    private void animateEnter(final View view) {

        if (getArguments() != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            viewHolderWidth = getArguments().getInt(ARG_ITEM_WIDTH, screenWidth);

            int[] location = getArguments().getIntArray(ARG_LOCATION);
            if (location == null) {
                location = new int[2];
            }

            toolbarHeight = UtilsTheme.getAttributeDimension(getContext(), R.attr.actionBarSize, 0);

            startX = location[0];
            startY = location[1];

            if (getArguments().getBoolean(ARG_IS_GRID)) {
                float margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                        getResources().getDisplayMetrics());
                startX -= margin;
                startY -= margin;
                viewHolderWidth += 2 * margin;
            }
            startMarginRight = (int) (screenWidth - startX - viewHolderWidth);
        }

        int[] locationRootView = new int[2];
        view.getLocationOnScreen(locationRootView);
        final float startHeight = getArguments().getInt(ARG_ITEM_HEIGHT, 0);
        final float targetY = startY - locationRootView[1] - toolbarHeight;
        final int layoutHeight = layoutComments.getHeight();

        layoutComments.setPadding((int) startX, 0, startMarginRight, 0);
        layoutComments.setTranslationY(targetY);
        layoutAppBar.setTranslationY(-toolbarHeight);

        Rect clipBackground = new Rect();
        viewBackground.setClipBounds(clipBackground);

        ValueAnimator animatorExpand = ValueAnimator.ofFloat(1f, 0);
        animatorExpand.setStartDelay(DELAY_ENTER);
        animatorExpand.setDuration(DURATION_ENTER);
        animatorExpand.setInterpolator(fastOutSlowInInterpolator);
        animatorExpand.addUpdateListener(animation -> {
            float interpolatedValue = (float) animation.getAnimatedValue();
            layoutComments.setPadding((int) (startX * interpolatedValue),
                    layoutComments.getPaddingTop(),
                    (int) (startMarginRight * interpolatedValue),
                    layoutComments.getPaddingBottom());
            layoutComments.setTranslationY(targetY * interpolatedValue);
            layoutAppBar.setTranslationY(-toolbarHeight * interpolatedValue);

            clipBackground.left = (int) (startX * interpolatedValue);
            clipBackground.top = (int) ((targetY + toolbarHeight) * interpolatedValue);
            clipBackground.right = (int) (clipBackground.left + layoutComments.getWidth() - startMarginRight * interpolatedValue);
            clipBackground.bottom = (int) (layoutHeight - startY * interpolatedValue + startHeight);
            Log.d(TAG, "animateEnter() called with: clipBackground = [" + clipBackground + "]");
            viewBackground.setClipBounds(clipBackground);
            viewBackground.setVisibility(View.VISIBLE);
        });
        animatorExpand.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isAdded()) {
                    return;
                }

                if (fragmentToHide != null) {
                    fragmentToHide.onHiddenChanged(true);
                    fragmentToHide = null;
                }

                layoutComments.postOnAnimationDelayed(() -> setAnimationFinished(true), 150);

                getArguments().putBoolean(ARG_INITIALIZED, true);
                buttonExpandActions.setVisibility(View.VISIBLE);
                buttonExpandActions.setScaleX(0f);
                buttonExpandActions.setScaleY(0f);
                buttonExpandActions.setAlpha(0f);
                ViewCompat.animate(buttonExpandActions)
                        .scaleX(1.0F)
                        .scaleY(1.0F)
                        .alpha(1.0F)
                        .withLayer()
                        .setDuration(DURATION_ACTIONS_FADE)
                        .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                        .setListener(null);
            }
        });

        pagerComments.setVisibility(View.VISIBLE);
        layoutAppBar.setVisibility(View.VISIBLE);

        if (viewHolderView != null) {
            viewHolderView.setVisibility(View.INVISIBLE);
            viewHolderView = null;
        }

        animatorExpand.start();
    }

    private void toggleLayoutActions() {
        if (buttonCommentPrevious.isShown()) {
            hideLayoutActions(DURATION_ACTIONS_FADE);
        }
        else {
            showLayoutActions();
        }
    }

    private void showLayoutActions() {

        for (int index = layoutActions.getChildCount() - 1; index >= 0; index--) {
            final View view = layoutActions.getChildAt(index);
            view.setVisibility(View.VISIBLE);
            final int finalIndex = index;
            ViewCompat.animate(view)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(fastOutSlowInInterpolator)
                    .setDuration(DURATION_ACTIONS_FADE)
                    .setStartDelay((long) ((layoutActions
                            .getChildCount() - 1 - index) * DURATION_ACTIONS_FADE * OFFSET_MODIFIER))
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {
                            if (finalIndex == 0) {
                                buttonExpandActions.setImageResource(android.R.color.transparent);
                            }
                        }

                        @Override
                        public void onAnimationEnd(View view) {

                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .start();
        }

    }

    private void hideLayoutActions(long offset) {
        for (int index = 0; index < layoutActions.getChildCount(); index++) {
            final View view = layoutActions.getChildAt(index);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setAlpha(1f);
            final int finalIndex = index;
            ViewCompat.animate(view)
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setInterpolator(fastOutSlowInInterpolator)
                    .setDuration(DURATION_ACTIONS_FADE)
                    .setStartDelay((long) (index * offset * OFFSET_MODIFIER))
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {

                        }

                        @Override
                        public void onAnimationEnd(View view) {
                            view.setVisibility(View.GONE);
                            if (finalIndex == layoutActions.getChildCount() - 1) {
                                buttonExpandActions
                                        .setImageResource(R.drawable.ic_unfold_more_white_24dp);
                                buttonExpandActions.setColorFilter(themer.getColorFilterAccent());
                            }
                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .start();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.preferences = PreferenceManager
                .getDefaultSharedPreferences(activity.getApplicationContext());
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        releaseYouTube();
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    public void releaseYouTube() {
        if (youTubePlayer != null) {
            youTubePlayer.release();
            youTubePlayer = null;
        }

        if (youTubeFragment != null) {
            try {
                getFragmentManager().beginTransaction()
                        .remove(youTubeFragment)
                        .commitAllowingStateLoss();
            } catch (Exception e) {

            }
        }

        layoutYouTube.setVisibility(View.GONE);
    }

    public void setPostExpanded(boolean expanded) {
        itemExpandPost.setIcon(expanded ? R.drawable.ic_expand_less_white_24dp : R.drawable.ic_expand_more_white_24dp);
        itemExpandPost.getIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
    }

    private void setIsCommentThread(boolean isCommentThread) {
        itemLoadFullComments.setEnabled(isCommentThread);
        itemLoadFullComments.setVisible(isCommentThread);
    }

    private void setSort(Sort sort) {
        toolbar.getMenu().findItem(sort.getMenuId()).setChecked(true);
    }

    private void setTitle(CharSequence title) {
        toolbar.setTitle(title);
    }

    public void setFragmentToHide(FragmentBase fragmentToHide, View viewHolderView) {
        this.fragmentToHide = fragmentToHide;
        this.viewHolderView = viewHolderView;
        fragmentParentTag = fragmentToHide.getTag();
    }

    @Override
    public void navigateBack() {
        if (youTubePlayer != null && isFullscreen) {
            youTubePlayer.setFullscreen(false);
        }
        else if (getFragmentManager().getBackStackEntryCount() == 0) {
            calculateExit();
        }
        else if (fragmentCurrent != null && !fragmentCurrent.isRecyclerCommentsShown()) {
            calculateExit();
        }
        else {
            if (!isAdded()) {
                isFinished = true;
                getActivity().onBackPressed();
                return;
            }

            calculateExit();

        }

    }

    private void calculateExit() {
        if (fragmentCurrent.getPosition() != indexStart) {
            slideExit();
        }
        else if (fragmentCurrent.findFirstCompletelyVisibleItemPosition() <= 0) {
            animateExit();
        }
        else if (fragmentCurrent.findFirstVisibleItemPosition() < 10) {
            fragmentCurrent.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        animateExit();
                    }
                }
            });
            fragmentCurrent.smoothScrollToPosition(0);
        }
        else {
            slideExit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!TextUtils.isEmpty(currentYouTubeId) && youTubePlayer != null) {
            outState.putString(ARG_YOUTUBE_ID, currentYouTubeId);
            outState.putInt(ARG_YOUTUBE_TIME, youTubePlayer.getCurrentTimeMillis());
        }

        super.onSaveInstanceState(outState);
    }

    private void animateExit() {
        isFinished = true;

        FragmentBase fragment = (FragmentBase) getFragmentManager()
                .findFragmentByTag(fragmentParentTag);
        if (fragment != null) {
            fragment.onHiddenChanged(false);
            getFragmentManager().beginTransaction().show(fragment).commit();
            fragment.onShown();
        }

        viewBackground.setVisibility(View.VISIBLE);
        fragmentCurrent.collapseViewHolderLink(getArguments().getBoolean(ARG_ACTIONS_EXPANDED));
        fragmentCurrent.fadeComments(() -> {
            setAnimationFinished(false);

            if (fragmentCurrent.isPostExpanded()) {
                fragmentCurrent.expandPost(false);
            }

            pagerComments.postOnAnimation(() -> {
                if (!isAdded()) {
                    return;
                }

                int[] locationSwipeRefresh = new int[2];
                layoutComments.getLocationOnScreen(locationSwipeRefresh);
                final float targetY = startY - locationSwipeRefresh[1];

                buttonExpandActions.hide();
                buttonJumpTop.hide();
                buttonCommentNext.hide();
                buttonCommentPrevious.hide();

                ValueAnimator animatorExit = ValueAnimator.ofFloat(0, 1);
                animatorExit.setDuration(DURATION_EXIT);
                animatorExit.setInterpolator(fastOutSlowInInterpolator);
                animatorExit.addUpdateListener(animation -> {
                    float interpolatedValue = animation.getAnimatedFraction();
                    layoutComments.setPadding((int) (startX * interpolatedValue), 0, (int) (startMarginRight * interpolatedValue), 0);
                    layoutComments.setTranslationY(targetY * interpolatedValue);
                    layoutAppBar.setTranslationY(-toolbarHeight * interpolatedValue);
                });
                animatorExit.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        FragmentBase fragment = (FragmentBase) getFragmentManager()
                                .findFragmentByTag(fragmentParentTag);
                        if (fragment != null) {
                            fragment.onHiddenChanged(false);
                            fragment.setVisibilityOfThing(View.INVISIBLE, linkTop);
                            getFragmentManager().beginTransaction().show(fragment)
                                    .commit();
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        FragmentBase fragment = (FragmentBase) getFragmentManager()
                                .findFragmentByTag(fragmentParentTag);
                        if (fragment != null) {
                            fragment.onHiddenChanged(false);
                            getFragmentManager().beginTransaction().show(fragment).commit();
                            fragment.onShown();
                        }

                        getActivity().onBackPressed();
                    }
                });

                animatorExit.start();

                if (layoutYouTube.isShown()) {
                    layoutYouTube.animate().translationYBy(
                            -(layoutYouTube.getHeight() + toolbar.getHeight()));
                }
            });
        });
    }

    private void slideExit() {
        isFinished = true;
        viewBackground.setVisibility(View.VISIBLE);
        FragmentBase fragment = (FragmentBase) getFragmentManager()
                .findFragmentByTag(fragmentParentTag);
        if (fragment != null) {
            fragment.setVisibilityOfThing(View.VISIBLE, linkTop);
            fragment.onHiddenChanged(false);
        }
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float translationX = isStartOnLeft ? screenWidth : -screenWidth;
        behaviorFloatingActionButton.animateOut(buttonExpandActions);
        layoutAppBar.animate().translationX(translationX);
        viewBackground.animate().translationX(translationX);
        layoutComments.animate().translationX(translationX)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        FragmentBase fragment = (FragmentBase) getFragmentManager()
                                .findFragmentByTag(fragmentParentTag);
                        if (fragment != null) {
                            fragment.onShown();
                            fragment.onHiddenChanged(false);
                            getFragmentManager().beginTransaction()
                                    .show(fragment)
                                    .commit();
                        }

                        getActivity().onBackPressed();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_hide_youtube:
                toggleYouTubeVisibility(View.GONE);
                break;
            case R.id.item_load_full_comments:
                fragmentCurrent.loadLinkComments();
                break;
            case R.id.item_expand_post:
                if (fragmentCurrent != null) {
                    fragmentCurrent.expandPost(!fragmentCurrent.getPostExpanded());
                }
                break;
        }

        item.setChecked(true);

        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            fragmentCurrent.setSort(sort);
            fragmentCurrent.scrollToPositionWithOffset(1, 0);
            return true;
        }

        return true;
    }

    @Override
    public boolean shouldOverrideUrl(String urlString) {

        Pattern pattern = Pattern.compile(
                ".*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=)([^#&\\?]*).*");
        final Matcher matcher = pattern.matcher(urlString);
        if (matcher.matches()) {
            int time = 0;
            Uri uri = Uri.parse(urlString);
            String timeQuery = uri.getQueryParameter("t");
            if (!TextUtils.isEmpty(timeQuery)) {
                try {
                    // YouTube query provides time in seconds, but we need milliseconds
                    time = Integer.parseInt(timeQuery) * 1000;
                }
                catch (NumberFormatException e) {
                    time = 0;
                }
            }
            String id = matcher.group(1);

            if (id.equals(currentYouTubeId)) {
                return false;
            }

            loadYoutubeVideo(id, time);
            return true;
        }

        return super.shouldOverrideUrl(urlString);
    }

    public void setAnimationFinished(boolean animationFinished) {
        if (this.animationFinished != animationFinished) {
            this.animationFinished = animationFinished;

            if (animationFinished) {
                pagerComments.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        positionCurrent = position;

                        for (FragmentCommentsInner fragment : adapterComments.getFragments()) {
                            if (fragment != null && fragment.getPosition() == position) {
                                setCurrentFragment(fragment);
                                break;
                            }
                        }

                        if (position >= adapterComments.getCount() - 3) {
                            controllerLinks.loadMore();
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });
            }
        }

        for (FragmentCommentsInner fragmentCommentsInner : adapterComments.getFragments()) {
            if (fragmentCommentsInner != null) {
                fragmentCommentsInner.setAnimationFinished(animationFinished);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean value = gestureDetector.onTouchEvent(event);

        if (isFinished || MotionEventCompat
                .getActionMasked(event) != MotionEvent.ACTION_UP || !hasSwipedEnd) {
            return false;
        }

        if (isStartOnLeft && swipeDifferenceX > swipeEndDistance) {
            slideExit();
            return false;
        }
        else if (!isStartOnLeft && swipeDifferenceX < -swipeEndDistance) {
            slideExit();
            return false;
        }
        else {
            hasSwipedEnd = false;
            if (!value) {

                behaviorFloatingActionButton.animateIn(buttonExpandActions);
                layoutAppBar.animate().translationX(0);
                viewBackground.animate().translationX(0);
                layoutComments.animate().translationX(0)
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (!isFinished) {
                                    FragmentBase fragment = (FragmentBase) getFragmentManager()
                                            .findFragmentByTag(fragmentParentTag);
                                    if (fragment != null) {
                                        fragment.setVisibilityOfThing(View.INVISIBLE, linkTop);
                                        fragment.onHiddenChanged(true);
                                    }
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
            }
        }

        return false;
    }
}