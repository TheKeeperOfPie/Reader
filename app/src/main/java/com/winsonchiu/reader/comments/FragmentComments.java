/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.animation.Animator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.utils.FinalizingSubscriber;
import com.winsonchiu.reader.utils.RecyclerFragmentPagerAdapter;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.TouchEventListener;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.YouTubeListener;
import com.winsonchiu.reader.utils.YouTubePlayerStateListener;
import com.winsonchiu.reader.views.CustomRelativeLayout;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class FragmentComments extends FragmentBase
        implements Toolbar.OnMenuItemClickListener, TouchEventListener {

    public static final String TAG = FragmentComments.class.getCanonicalName();

    private static final String ARG_IS_GRID = "isGrid";
    private static final String ARG_COLOR_LINK = "colorLink";
    private static final String ARG_LOCATION = "location";
    private static final String ARG_ITEM_HEIGHT = "itemHeight";
    private static final String ARG_ITEM_WIDTH = "itemWidth";
    private static final String ARG_INITIALIZED = "initialized";
    private static final String ARG_ACTIONS_EXPANDED = "actionsExpanded";
    private static final String ARG_YOUTUBE_ID = "youTubeId";
    private static final String ARG_YOUTUBE_TIME = "youTubeTime";

    public static final long DURATION_ENTER = 250;
    public static final long DURATION_EXIT = 250;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.5f;

    private FragmentListenerBase mListener;
    private Toolbar toolbar;
    private LinearLayout layoutActions;
    private FloatingActionButton buttonExpandActions;
    private FloatingActionButton buttonJumpTop;
    private FloatingActionButton buttonCommentPrevious;
    private FloatingActionButton buttonCommentNext;
    private ScrollAwareFloatingActionButtonBehavior behaviorFloatingActionButton;
    private YouTubePlayerView viewYouTube;
    private YouTubePlayer youTubePlayer;
    private YouTubeListener youTubeListener;
    private ControllerCommentsTop.Listener listenerTop;
    private FastOutSlowInInterpolator fastOutSlowInInterpolator;
    private FragmentBase fragmentToHide;
    private String fragmentParentTag;
    private boolean isFullscreen;
    private int viewHolderWidth;
    private float startX;
    private float startY;
    private int startMarginRight;
    private View viewBackground;
    private MenuItem itemLoadFullComments;
    private MenuItem itemHideYouTube;
    private MenuItem itemExpandPost;
    private View viewHolderView;
    private float toolbarHeight;
    private GestureDetectorCompat gestureDetector;
    private SharedPreferences preferences;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private boolean hasSwipedEnd;
    private CustomRelativeLayout layoutRelative;
    private CustomRelativeLayout layoutRoot;
    private boolean isFinished;
    private float swipeDifferenceX;
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterAccent;
    private float swipeEndDistance;
    private String currentYouTubeId;

    private boolean isStartOnLeft;
    private boolean animationFinished;

    private ViewPager pagerComments;
    private FragmentCommentsInner.Callback fragmentCallback;
    private Link linkTop;
    private int positionCurrent;
    private int indexStart;
    private int itemCount;
    private FragmentCommentsInner fragmentCurrent;
    private RecyclerFragmentPagerAdapter<FragmentCommentsInner> adapterComments;

    @Inject ControllerCommentsTop controllerCommentsTop;
    @Inject ControllerLinks controllerLinks;

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

    public static FragmentComments newInstance(AdapterLink.ViewHolderBase viewHolder,
            int colorLink) {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        if (viewHolder.itemView
                .getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            args.putBoolean(ARG_IS_GRID, true);
        }
        int[] location = viewHolder.getScreenAnchor();

        Log.d(TAG, "getScreenAnchor: " + Arrays.toString(location));

        args.putIntArray(ARG_LOCATION, location);
        args.putInt(ARG_COLOR_LINK, colorLink);
        args.putInt(ARG_ITEM_HEIGHT, viewHolder.itemView.getHeight());
        args.putInt(ARG_ITEM_WIDTH, viewHolder.itemView.getWidth());
        args.putBoolean(ARG_ACTIONS_EXPANDED, viewHolder.layoutContainerExpand.isShown());
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
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.openDrawer();
                }
            });
        }
        else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onNavigationBackClick();
                }
            });
        }

        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);

        toolbar.inflateMenu(R.menu.menu_comments);

        Menu menu = toolbar.getMenu();

        itemLoadFullComments = menu.findItem(R.id.item_load_full_comments);
        itemHideYouTube = menu.findItem(R.id.item_hide_youtube);
        itemExpandPost = menu.findItem(R.id.item_expand_post);
        toolbar.setOnMenuItemClickListener(this);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ((ActivityMain) getActivity()).getComponentActivity().inject(this);

        layoutRoot = (CustomRelativeLayout) inflater
                .inflate(R.layout.fragment_comments, container, false);

        listenerTop = new ControllerCommentsTop.Listener() {
            @Override
            public void insertComment(Comment comment) {
                fragmentCurrent.getControllerComments().insertComment(comment);
            }

            @Override
            public void setNsfw(String name, boolean over18) {
                fragmentCurrent.getControllerComments().setNsfw(name, over18);
            }

            @Override
            public void updateComment(Comment newComment) {

            }

            @Override
            public void setReplyText(String nameParent, String text, boolean collapsed) {

            }
        };

        youTubeListener = new YouTubeListener() {
            @Override
            public void loadYouTubeVideo(Link link, final String id, final int timeInMillis) {
                loadYoutubeVideo(id, timeInMillis);
            }

            @Override
            public boolean hideYouTube() {
                if (viewYouTube.isShown()) {
                    if (youTubePlayer != null) {
                        youTubePlayer.pause();
                    }
                    toggleYouTubeVisibility(View.GONE);
                    return false;
                }

                return true;
            }
        };

        TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorAccent});
        final int colorPrimary = typedArray
                .getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorAccent = typedArray.getColor(1, getResources().getColor(R.color.colorAccent));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ?
                R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceAccent = UtilsColor.computeContrast(colorAccent, Color.WHITE) > 3f ?
                R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new PorterDuffColorFilter(
                getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterAccent = new PorterDuffColorFilter(getResources().getColor(colorResourceAccent),
                PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) layoutRoot.findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        toolbar.setBackgroundColor(colorPrimary);
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
                        buttonExpandActions.setColorFilter(colorFilterAccent);
                    }

                });

        layoutActions = (LinearLayout) layoutRoot.findViewById(R.id.layout_actions);

        buttonExpandActions = (FloatingActionButton) layoutRoot
                .findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLayoutActions();
            }
        });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                .setBehavior(behaviorFloatingActionButton);

        buttonJumpTop = (FloatingActionButton) layoutRoot.findViewById(R.id.button_jump_top);
        buttonJumpTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentCurrent.scrollToPositionWithOffset(0, 0);
            }
        });
        buttonJumpTop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(),
                        getString(R.string.content_description_button_jump_top),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonCommentPrevious = (FloatingActionButton) layoutRoot.findViewById(
                R.id.button_comment_previous);
        buttonCommentPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentCurrent.previousComment();
            }
        });
        buttonCommentPrevious.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(),
                        getString(R.string.content_description_button_comment_previous),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonCommentNext = (FloatingActionButton) layoutRoot
                .findViewById(R.id.button_comment_next);
        buttonCommentNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentCurrent.nextComment();
            }
        });
        buttonCommentNext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(),
                        getString(R.string.content_description_button_comment_next),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
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

            RelativeLayout.LayoutParams layoutParamsActions = (RelativeLayout.LayoutParams) layoutActions
                    .getLayoutParams();
            layoutParamsActions.setMarginStart(margin);
            layoutParamsActions.setMarginEnd(margin);
            layoutActions.setLayoutParams(layoutParamsActions);
        }

        buttonExpandActions.setColorFilter(colorFilterAccent);
        buttonJumpTop.setColorFilter(colorFilterAccent);
        buttonCommentPrevious.setColorFilter(colorFilterAccent);
        buttonCommentNext.setColorFilter(colorFilterAccent);

        viewYouTube = (YouTubePlayerView) layoutRoot.findViewById(R.id.youtube);

        layoutCoordinator = (CoordinatorLayout) layoutRoot.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) layoutRoot.findViewById(R.id.layout_app_bar);
        layoutRelative = (CustomRelativeLayout) layoutRoot.findViewById(R.id.layout_relative);

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
                                fragment.setVisibilityOfThing(View.VISIBLE,
                                        controllerCommentsTop.getLink());
                                fragment.onHiddenChanged(false);
                            }
                            hasSwipedEnd = true;
                        }
                        float ratio = 1f - swipeDifferenceX / screenWidth;
                        buttonExpandActions.setAlpha(ratio);
                        buttonExpandActions.setScaleX(ratio);
                        buttonExpandActions.setScaleY(ratio);
                        layoutAppBar.setTranslationX(swipeDifferenceX);
                        layoutRelative.setTranslationX(swipeDifferenceX);

                        return super.onScroll(e1, e2, distanceX, distanceY);
                    }
                });

        if (preferences.getBoolean(AppSettings.SWIPE_EXIT_COMMENTS, true)) {
            layoutRoot.setTouchEventListener(this);
        }

        viewBackground = layoutRoot.findViewById(R.id.view_background);

        if (savedInstanceState != null) {
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
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar
                        .getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void onReplyShown() {
                behaviorFloatingActionButton.animateOut(buttonExpandActions);
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

        linkTop = controllerCommentsTop.getLink();
        indexStart = controllerLinks.indexOf(linkTop);
        itemCount = controllerLinks.sizeLinks();
        positionCurrent = indexStart;

        adapterComments = new RecyclerFragmentPagerAdapter<FragmentCommentsInner>(getFragmentManager()) {
            @Override
            public FragmentCommentsInner createFragment() {
                return FragmentCommentsInner.newInstance(getArguments().getBoolean(ARG_IS_GRID, false));
            }

            @Override
            protected void bindFragment(FragmentCommentsInner fragment, int position) {
                Link link = null;

                if (position < indexStart) {
                    link = controllerLinks.getPreviousLink(linkTop, indexStart - position);
                }
                else if (position > indexStart) {
                    link = controllerLinks.getNextLink(linkTop, position - indexStart);
                }

                if (link == null) {
                    link = linkTop;
                }

                fragment.setCallback(fragmentCallback);
                fragment.setAnimationFinished(animationFinished);
                fragment.setLink(link);
                fragment.setPosition(position);

                if (fragmentCurrent == null && indexStart == position) {
                    setCurrentFragment(fragment);
                }
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                super.destroyItem(container, position, object);

                FragmentCommentsInner fragmentCommentsInner = (FragmentCommentsInner) object;
                fragmentCommentsInner.clear();
            }

            @Override
            public int getCount() {
                return itemCount;
            }
        };

        pagerComments = (ViewPager) layoutRoot.findViewById(R.id.pager_comments);
        pagerComments.setOffscreenPageLimit(0);
        pagerComments.setAdapter(adapterComments);
        pagerComments.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public int stateScroll;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                positionCurrent = position;

                for (FragmentCommentsInner fragment : adapterComments.getFragments()) {
                    if (fragment.getPosition() == position) {
                        setCurrentFragment(fragment);
                        break;
                    }
                }

                if (position >= adapterComments.getCount() - 3) {
                    controllerLinks.loadMoreLinks()
                            .subscribe(new FinalizingSubscriber<Listing>() {
                                @Override
                                public void completed() {
                                    itemCount = controllerLinks.sizeLinks();
                                    adapterComments.notifyDataSetChanged();
                                }
                            });
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (stateScroll == ViewPager.SCROLL_STATE_SETTLING && state == ViewPager.SCROLL_STATE_IDLE) {
                    pagerComments.setOffscreenPageLimit(2);
                }
                else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    pagerComments.setOffscreenPageLimit(1);
                }

                stateScroll = state;
            }
        });

        if (indexStart > 0) {
            pagerComments.setCurrentItem(indexStart, false);
        }

        if (!getArguments().getBoolean(ARG_INITIALIZED, false)) {
            viewBackground.setVisibility(View.INVISIBLE);
            pagerComments.setVisibility(View.INVISIBLE);
            toolbar.setVisibility(View.GONE);
            layoutAppBar.setTranslationY(-100);

            layoutRelative.post(new Runnable() {
                @Override
                public void run() {
                    animateEnter(layoutRoot);
                }
            });
        }
        else {
            setAnimationFinished(true);
        }

        return layoutRoot;
    }

    private void setCurrentFragment(FragmentCommentsInner fragment) {
        fragmentCurrent = fragment;

        setPostExpanded(fragmentCurrent.getPostExpanded());
        setIsCommentThread(fragmentCurrent.getIsCommentThread());
        setSort(fragmentCurrent.getSort());
        setTitle(fragmentCurrent.getTitle());
        fragmentCurrent.setAnimationFinished(animationFinished);
    }

    @Override
    public void onStart() {
        super.onStart();
        controllerCommentsTop.addListener(listenerTop);
    }

    @Override
    public void onResume() {
        super.onResume();
        controllerCommentsTop.addListener(listenerTop);
    }

    @Override
    public void onPause() {
        controllerCommentsTop.removeListener(listenerTop);
        super.onPause();
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

        viewYouTube.initialize(ApiKeys.YOUTUBE_API_KEY,
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
        viewYouTube.setVisibility(visibility);
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

            final TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(
                    new int[]{android.R.attr.actionBarSize});
            toolbarHeight = styledAttributes.getDimension(0, 0);
            styledAttributes.recycle();

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
        final float screenWidth = getResources().getDisplayMetrics().widthPixels;
        final float screenHeight = getResources().getDisplayMetrics().heightPixels;
        final float targetY = startY - locationRootView[1] - toolbarHeight;

        final Animation animation = new Animation() {

            @Override
            public boolean willChangeBounds() {
                return true;
            }

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) layoutRelative
                        .getLayoutParams();
                float reverseInterpolation = 1.0f - interpolatedTime;
                layoutParams.topMargin = (int) (targetY * reverseInterpolation);
                layoutParams.leftMargin = (int) (startX * reverseInterpolation);
                layoutParams.rightMargin = (int) (startMarginRight * reverseInterpolation);
                layoutRelative.setLayoutParams(layoutParams);
                layoutAppBar.setTranslationY(-toolbarHeight * reverseInterpolation);
                ViewGroup.LayoutParams layoutParamsBackground = viewBackground
                        .getLayoutParams();
                layoutParamsBackground.height = (int) (startHeight + interpolatedTime * screenHeight);
                viewBackground.setLayoutParams(layoutParamsBackground);

            }
        };
        animation.setDuration(DURATION_ENTER);
        animation.setInterpolator(fastOutSlowInInterpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                viewBackground.setVisibility(View.VISIBLE);
                pagerComments.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
                if (viewHolderView != null) {
                    viewHolderView.setVisibility(View.INVISIBLE);
                    viewHolderView = null;
                }
//                if (adapterCommentList.getViewHolderLink() != null) {
//                    adapterCommentList.getViewHolderLink()
//                            .calculateVisibleToolbarItems(layoutRoot.getWidth());
//                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!isAdded()) {
                    return;
                }

                if (fragmentToHide != null) {
                    fragmentToHide.onHiddenChanged(true);
                    fragmentToHide = null;
                }

                layoutRelative.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setAnimationFinished(true);
                    }
                }, 150);

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

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) layoutRelative
                .getLayoutParams();
        layoutParams.topMargin = (int) targetY;
        layoutParams.leftMargin = (int) startX;
        layoutParams.rightMargin = startMarginRight;
        layoutRelative.setLayoutParams(layoutParams);

        layoutRelative.startAnimation(animation);
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
            view.setScaleX(0f);
            view.setScaleY(0f);
            view.setAlpha(0f);
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
                                buttonExpandActions.setColorFilter(colorFilterAccent);
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
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseYouTube();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    public void releaseYouTube() {
        if (youTubePlayer != null) {
            youTubePlayer.release();
            youTubePlayer = null;
        }
    }

    public void setPostExpanded(boolean expanded) {
        itemExpandPost.setIcon(expanded ? R.drawable.ic_expand_less_white_24dp : R.drawable.ic_expand_more_white_24dp);
        itemExpandPost.getIcon().mutate().setColorFilter(colorFilterPrimary);
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
        Log.d(TAG, "navigateBack");

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
        isFinished = true;
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

        viewBackground.setVisibility(View.VISIBLE);
        fragmentCurrent.collapseViewHolderLink(getArguments().getBoolean(ARG_ACTIONS_EXPANDED));
        fragmentCurrent.fadeComments(new Runnable() {
            @Override
            public void run() {
                setAnimationFinished(false);
                fragmentCurrent.expandPost(false);

                pagerComments.post(new Runnable() {
                    @Override
                    public void run() {

                        if (!isAdded()) {
                            return;
                        }

                        int[] locationSwipeRefresh = new int[2];
                        layoutRelative.getLocationOnScreen(locationSwipeRefresh);
                        final float viewHeight = layoutRelative.getHeight();
                        final float targetY = startY - locationSwipeRefresh[1];

                        long duration = ScrollAwareFloatingActionButtonBehavior.DURATION;

                        UtilsAnimation.shrinkAndFadeOut(buttonExpandActions, duration);

                        if (buttonJumpTop.isShown()) {
                            UtilsAnimation.shrinkAndFadeOut(buttonJumpTop, duration);
                            UtilsAnimation.shrinkAndFadeOut(buttonCommentNext, duration);
                            UtilsAnimation.shrinkAndFadeOut(buttonCommentPrevious, duration);
                        }

                        final Animation animation = new Animation() {
                            @Override
                            public boolean willChangeBounds() {
                                return true;
                            }

                            @Override
                            protected void applyTransformation(float interpolatedTime,
                                    Transformation t) {
                                super.applyTransformation(interpolatedTime, t);
                                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) layoutRelative
                                        .getLayoutParams();
                                layoutParams.topMargin = (int) (targetY * interpolatedTime);
                                layoutParams.leftMargin = (int) (startX * interpolatedTime);
                                layoutParams.rightMargin = (int) (startMarginRight * interpolatedTime);
                                layoutRelative.setLayoutParams(layoutParams);
                                layoutAppBar.setTranslationY(-toolbarHeight * interpolatedTime);

                                ViewGroup.LayoutParams layoutParamsBackground = viewBackground
                                        .getLayoutParams();
                                layoutParamsBackground.height = (int) ((1f - interpolatedTime) * viewHeight - targetY * interpolatedTime);
                                viewBackground.setLayoutParams(layoutParamsBackground);
                            }
                        };
                        animation.setDuration(DURATION_EXIT);
                        animation.setInterpolator(fastOutSlowInInterpolator);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                FragmentBase fragment = (FragmentBase) getFragmentManager()
                                        .findFragmentByTag(fragmentParentTag);
                                if (fragment != null) {
                                    fragment.onHiddenChanged(false);
                                    getFragmentManager().beginTransaction().show(fragment)
                                            .commit();
                                }
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                FragmentBase fragment = (FragmentBase) getFragmentManager()
                                        .findFragmentByTag(fragmentParentTag);
                                if (fragment != null) {
                                    fragment.onHiddenChanged(false);
                                    getFragmentManager().beginTransaction().show(fragment).commit();
                                    fragment.onShown();
                                }

                                getActivity().onBackPressed();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        if (viewYouTube.isShown()) {
                            viewYouTube.animate().translationYBy(
                                    -(viewYouTube.getHeight() + toolbar.getHeight()));
                        }

                        pagerComments.startAnimation(animation);
                    }
                });
            }
        });
    }

    private void slideExit() {
        viewBackground.setVisibility(View.VISIBLE);
        FragmentBase fragment = (FragmentBase) getFragmentManager()
                .findFragmentByTag(fragmentParentTag);
        if (fragment != null) {
            fragment.setVisibilityOfThing(View.VISIBLE,
                    controllerCommentsTop.getLink());
            fragment.onHiddenChanged(false);
        }
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float translationX = isStartOnLeft ? screenWidth : -screenWidth;
        behaviorFloatingActionButton.animateOut(buttonExpandActions);
        layoutAppBar.animate().translationX(translationX);
        layoutRelative.animate().translationX(translationX)
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
    public boolean onTouchEvent(MotionEvent event) {

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
                layoutRelative.animate().translationX(0)
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
                                        fragment.setVisibilityOfThing(View.INVISIBLE,
                                                controllerCommentsTop.getLink());
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
        this.animationFinished = animationFinished;

        for (FragmentCommentsInner fragmentCommentsInner : adapterComments.getFragments()) {
            fragmentCommentsInner.setAnimationFinished(animationFinished);
        }

        pagerComments.setOffscreenPageLimit(2);
    }
}