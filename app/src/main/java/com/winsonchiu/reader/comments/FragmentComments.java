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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.YouTubePlayerStateListener;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.utils.AnimationUtils;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.views.CustomRelativeLayout;

import java.util.Arrays;

public class FragmentComments extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentComments.class.getCanonicalName();

    private static final String ARG_IS_GRID = "isGrid";
    private static final String ARG_COLOR_LINK = "colorLink";
    private static final String ARG_LOCATION = "location";
    private static final String ARG_ITEM_HEIGHT = "itemHeight";
    private static final String ARG_ITEM_WIDTH = "itemWidth";
    private static final String ARG_INITIALIZED = "initialized";

    private static final long DURATION_ENTER = 250;
    private static final long DURATION_EXIT = 150;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.25f;

    private FragmentListenerBase mListener;
    private RecyclerView recyclerCommentList;
    private LinearLayoutManager linearLayoutManager;
    private AdapterCommentList adapterCommentList;
    private SwipeRefreshLayout swipeRefreshCommentList;
    private ControllerComments.Listener listener;
    private Toolbar toolbar;
    private LinearLayout layoutActions;
    private FloatingActionButton buttonExpandActions;
    private FloatingActionButton buttonJumpTop;
    private FloatingActionButton buttonCommentPrevious;
    private FloatingActionButton buttonCommentNext;
    private ScrollAwareFloatingActionButtonBehavior behaviorFloatingActionButton;
    private YouTubePlayerView viewYouTube;
    private YouTubePlayer youTubePlayer;
    private RecyclerView.AdapterDataObserver observer;
    private FastOutSlowInInterpolator fastOutSlowInInterpolator;
    private FragmentBase fragmentToHide;
    private String fragmentParentTag;
    private boolean isFullscreen;
    private int viewHolderWidth;
    private float startX;
    private float startY;
    private int startMarginEnd;
    private View viewBackground;
    private MenuItem itemLoadFullComments;
    private View viewHolderView;
    private float toolbarHeight;
    private GestureDetectorCompat gestureDetector;
    private SharedPreferences preferences;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private boolean hasSwipedRight;
    private RelativeLayout layoutRelative;
    private CustomRelativeLayout layoutRoot;
    private boolean isFinished;
    private float swipeDifferenceX;
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterAccent;

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

    public static FragmentComments newInstance(RecyclerView.ViewHolder viewHolder,
            int colorLink) {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        if (viewHolder.itemView
                .getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            args.putBoolean(ARG_IS_GRID, true);
        }
        int[] location = new int[2];
        viewHolder.itemView.getLocationOnScreen(location);

        args.putIntArray(ARG_LOCATION, location);
        args.putInt(ARG_COLOR_LINK, colorLink);
        args.putInt(ARG_ITEM_HEIGHT, viewHolder.itemView.getHeight());
        args.putInt(ARG_ITEM_WIDTH, viewHolder.itemView.getWidth());
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentComments() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fastOutSlowInInterpolator = new FastOutSlowInInterpolator();
        setHasOptionsMenu(true);
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
        itemLoadFullComments = toolbar.getMenu().findItem(R.id.item_load_full_comments);
        toolbar.setOnMenuItemClickListener(this);

        toolbar.getMenu().findItem(mListener.getControllerComments().getSort().getMenuId())
                .setChecked(true);

        Menu menu = toolbar.getMenu();

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().setColorFilter(colorFilterPrimary);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        layoutRoot = (CustomRelativeLayout) inflater.inflate(R.layout.fragment_comments, container, false);

        listener = new ControllerComments.Listener() {
            @Override
            public void setSort(Sort sort) {
                toolbar.getMenu().findItem(sort.getMenuId()).setChecked(true);
            }

            @Override
            public void setIsCommentThread(boolean isCommentThread) {
                itemLoadFullComments.setEnabled(isCommentThread);
                itemLoadFullComments.setVisible(isCommentThread);
            }

            @Override
            public void scrollTo(final int position) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0);
//                linearLayoutManager.scrollToPositionWithOffset(position,
//                        recyclerCommentList.getHeight() / 2 - toolbar.getHeight());
//                recyclerCommentList.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        final RecyclerView.ViewHolder viewHolder = recyclerCommentList
//                                .findViewHolderForAdapterPosition(position);
//                        if (viewHolder != null) {
//                            viewHolder.itemView.getBackground().setState(
//                                    new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});
//                            recyclerCommentList.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    viewHolder.itemView.getBackground().setState(new int[0]);
//                                }
//                            }, 150);
//                        }
//                    }
//                }, 200);
            }

            @Override
            public RecyclerView.Adapter getAdapter() {
                return adapterCommentList;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshCommentList.setRefreshing(refreshing);
            }

            @Override
            public void post(Runnable runnable) {
                recyclerCommentList.post(runnable);
            }
        };

        DisallowListener disallowListener = new DisallowListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        RecyclerCallback recyclerCallback = new RecyclerCallback() {
            @Override
            public void scrollTo(int position) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0);
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerCommentList.getHeight();
            }

            @Override
            public RecyclerView.LayoutManager getLayoutManager() {
                return linearLayoutManager;
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
        };

        YouTubeListener youTubeListener = new YouTubeListener() {
            @Override
            public void loadYouTube(Link link, final String id, final int timeInMillis) {

                if (youTubePlayer != null) {
                    viewYouTube.setVisibility(View.VISIBLE);
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
                                                        if (youTubePlayer != null) {
                                                            youTubePlayer.pause();
                                                            youTubePlayer.release();
                                                            youTubePlayer = null;
                                                        }
                                                        viewYouTube.setVisibility(View.GONE);
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
                                viewYouTube.setVisibility(View.VISIBLE);
                                youTubePlayer.loadVideo(id);
                            }

                            @Override
                            public void onInitializationFailure(YouTubePlayer.Provider provider,
                                    YouTubeInitializationResult youTubeInitializationResult) {
                            }
                        });
            }

            @Override
            public boolean hideYouTube() {
                if (viewYouTube.isShown()) {
                    if (youTubePlayer != null) {
                        youTubePlayer.pause();
                    }
                    viewYouTube.setVisibility(View.GONE);
                    return false;
                }

                return true;
            }
        };

        TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(
                new int[] {R.attr.colorPrimary, R.attr.colorAccent});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorAccent = typedArray.getColor(1, getResources().getColor(R.color.colorAccent));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceAccent = UtilsColor.computeContrast(colorAccent, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new PorterDuffColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterAccent = new PorterDuffColorFilter(getResources().getColor(colorResourceAccent), PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) layoutRoot.findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
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

        buttonExpandActions = (FloatingActionButton) layoutRoot.findViewById(R.id.button_expand_actions);
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
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
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
                int position = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                if (position == 1) {
                    linearLayoutManager.scrollToPositionWithOffset(0, 0);
                    return;
                }
                int newPosition = mListener.getControllerComments().getPreviousCommentPosition(
                        position - 1) + 1;
                listener.scrollTo(newPosition);
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

        buttonCommentNext = (FloatingActionButton) layoutRoot.findViewById(R.id.button_comment_next);
        buttonCommentNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = linearLayoutManager.findFirstVisibleItemPosition();
                if (position == 0) {
                    if (adapterCommentList.getItemCount() > 0) {
                        listener.scrollTo(1);
                    }
                    return;
                }
                final int newPosition = mListener.getControllerComments()
                        .getNextCommentPosition(position - 1) + 1;
                listener.scrollTo(newPosition);

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

        swipeRefreshCommentList = (SwipeRefreshLayout) layoutRoot.findViewById(
                R.id.swipe_refresh_comment_list);
        swipeRefreshCommentList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerComments().reloadAllComments();
            }
        });

        layoutCoordinator = (CoordinatorLayout) layoutRoot.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) layoutRoot.findViewById(R.id.layout_app_bar);
        layoutRelative = (RelativeLayout) layoutRoot.findViewById(R.id.layout_relative);

        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerCommentList = (RecyclerView) layoutRoot.findViewById(R.id.recycler_comment_list);
        recyclerCommentList.setLayoutManager(linearLayoutManager);
        recyclerCommentList.setItemAnimator(null);

        final float screenWidth = getResources().getDisplayMetrics().widthPixels;
        final float swipeRightDistance = screenWidth * 0.4f;

        gestureDetector = new GestureDetectorCompat(getActivity(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onScroll(MotionEvent e1,
                            MotionEvent e2,
                            float distanceX,
                            float distanceY) {

                        // TODO: Implement a fling gesture based on distance-based velocity
                        // TODO: Support RTL (make sure to change preference label)

                        if (isFinished) {
                            return true;
                        }

                        swipeDifferenceX = e2.getX() - e1.getX();

                        if (swipeDifferenceX > 0 && e1.getX() < screenWidth * 0.2f) {
                            if (!hasSwipedRight) {
                                FragmentBase fragment = (FragmentBase) getFragmentManager()
                                        .findFragmentByTag(fragmentParentTag);
                                if (fragment != null) {
                                    fragment.setVisibilityOfThing(View.VISIBLE, mListener.getControllerComments().getLink());
                                    fragment.onHiddenChanged(false);
                                }
                                viewBackground.setVisibility(View.VISIBLE);
                                hasSwipedRight = true;
                            }
                            layoutRelative.setTranslationX(swipeDifferenceX);
                        }

                        return super.onScroll(e1, e2, distanceX, distanceY);
                    }
                });

        if (preferences.getBoolean(AppSettings.SWIPE_EXIT_COMMENTS, true)) {
            layoutRoot.setOnInterceptTouchEventListener(new CustomRelativeLayout.OnInterceptTouchEventListener() {
                @Override
                public boolean onInterceptTouchEvent(MotionEvent event) {

                    boolean value = gestureDetector.onTouchEvent(event);

                    if (isFinished || event.getAction() != MotionEvent.ACTION_UP || !hasSwipedRight) {
                        return false;
                    }

                    if (swipeDifferenceX > swipeRightDistance) {
                        slideExit();
                        return true;
                    }
                    else {
                        hasSwipedRight = false;
                        if (!value) {
                            layoutRelative.animate().translationX(0).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (!isFinished) {
                                        FragmentBase fragment = (FragmentBase) getFragmentManager()
                                                .findFragmentByTag(fragmentParentTag);
                                        if (fragment != null) {
                                            fragment.setVisibilityOfThing(View.INVISIBLE, mListener.getControllerComments().getLink());
                                            fragment.onHiddenChanged(true);
                                        }
                                        viewBackground.setVisibility(View.GONE);
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
            });
//            recyclerCommentList.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//
//                    boolean value = gestureDetector.onTouchEvent(event);
//
//                    if (event.getAction() == MotionEvent.ACTION_UP && hasSwipedRight) {
//                        hasSwipedRight = false;
//                        view.animate().translationX(0).setListener(new Animator.AnimatorListener() {
//                            @Override
//                            public void onAnimationStart(Animator animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                FragmentBase fragment = (FragmentBase) getFragmentManager()
//                                        .findFragmentByTag(fragmentParentTag);
//                                if (fragment != null) {
//                                    fragment.onHiddenChanged(true);
//                                }
//                                viewBackground.setVisibility(View.GONE);
//                            }
//
//                            @Override
//                            public void onAnimationCancel(Animator animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationRepeat(Animator animation) {
//
//                            }
//                        });
//                    }
//
//                    return value;
//                }
//            });

//            recyclerCommentList.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
//                @Override
//                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
//                    return false;
//                }
//
//                @Override
//                public void onTouchEvent(RecyclerView rv, MotionEvent e) {
//                    gestureDetector.onTouchEvent(e);
//
//                    if (e.getAction() == MotionEvent.ACTION_UP && hasSwipedRight) {
//                        hasSwipedRight = false;
//                        layoutRelative.animate().translationX(0).setListener(new Animator.AnimatorListener() {
//                            @Override
//                            public void onAnimationStart(Animator animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                FragmentBase fragment = (FragmentBase) getFragmentManager()
//                                        .findFragmentByTag(fragmentParentTag);
//                                if (fragment != null) {
//                                    fragment.onHiddenChanged(true);
//                                }
//                                viewBackground.setVisibility(View.GONE);
//                            }
//
//                            @Override
//                            public void onAnimationCancel(Animator animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationRepeat(Animator animation) {
//
//                            }
//                        });
//                    }
//                }
//
//                @Override
//                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
//
//                }
//            });
        }

        adapterCommentList = new AdapterCommentList(getActivity(),
                mListener.getControllerComments(),
                mListener.getEventListenerBase(),
                mListener.getEventListenerComment(),
                disallowListener,
                recyclerCallback,
                youTubeListener,
                getArguments().getBoolean(ARG_IS_GRID, false),
                getArguments().getInt(ARG_COLOR_LINK));

        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                if (positionStart == 0 && youTubePlayer != null) {
                    youTubePlayer.release();
                    youTubePlayer = null;
                }
            }
        };

        adapterCommentList.registerAdapterDataObserver(observer);
        recyclerCommentList.setAdapter(adapterCommentList);

        viewBackground = layoutRoot.findViewById(R.id.view_background);

        if (!getArguments().getBoolean(ARG_INITIALIZED, false)) {
            viewBackground.setVisibility(View.GONE);
            recyclerCommentList.setVisibility(View.GONE);
            swipeRefreshCommentList.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            layoutAppBar.setTranslationY(-100);

            layoutRoot.post(new Runnable() {
                @Override
                public void run() {
                    animateEnter(layoutRoot);
                }
            });
        }
        else {
            adapterCommentList.setAnimationFinished(true);
        }

        return layoutRoot;
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
                    new int[] {android.R.attr.actionBarSize});
            toolbarHeight = styledAttributes.getDimension(0, 0);
            styledAttributes.recycle();

            startX = location[0];
            startY = location[1];

            Log.d(TAG, "location: " + Arrays.toString(location));
            Log.d(TAG, "toolbarHeight: " + toolbarHeight);
            Log.d(TAG, "startX: " + startX);
            Log.d(TAG, "startY: " + startY);

            if (getArguments().getBoolean(ARG_IS_GRID)) {
                float margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                        getResources().getDisplayMetrics());
                startX -= margin;
                startY -= margin;
                viewHolderWidth += 2 * margin;
            }
            startMarginEnd = (int) (screenWidth - startX - viewHolderWidth);
        }

        int[] locationRootView = new int[2];
        view.getLocationOnScreen(locationRootView);
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
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) swipeRefreshCommentList
                        .getLayoutParams();
                float reverseInterpolation = 1.0f - interpolatedTime;
                layoutParams.topMargin = (int) (targetY * reverseInterpolation);
                layoutParams.setMarginStart((int) (startX * reverseInterpolation));
                layoutParams.setMarginEnd((int) (startMarginEnd * reverseInterpolation));
                swipeRefreshCommentList.setLayoutParams(layoutParams);
                layoutAppBar.setTranslationY(-toolbarHeight * reverseInterpolation);

                RelativeLayout.LayoutParams layoutParamsBackground = (RelativeLayout.LayoutParams) viewBackground
                        .getLayoutParams();
                layoutParamsBackground.width = (int) (screenWidth - (startX + startMarginEnd) * reverseInterpolation);
                layoutParamsBackground.height = (int) (interpolatedTime * screenHeight);
                viewBackground.setLayoutParams(layoutParamsBackground);
                viewBackground.setTranslationX(startX * reverseInterpolation);
                viewBackground.setTranslationY(targetY * reverseInterpolation);
            }
        };
        animation.setDuration(DURATION_ENTER);
        animation.setInterpolator(fastOutSlowInInterpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

                viewBackground.setVisibility(View.VISIBLE);
                swipeRefreshCommentList.setVisibility(View.VISIBLE);
                recyclerCommentList.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
                if (viewHolderView != null) {
                    viewHolderView.setVisibility(View.INVISIBLE);
                    viewHolderView = null;
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!isAdded()) {
                    return;
                }
                if (fragmentToHide != null) {
                    fragmentToHide.onHiddenChanged(true);
//                    getFragmentManager().beginTransaction().hide(fragmentToHide).commit();
                    fragmentToHide = null;
                }
                swipeRefreshCommentList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        viewBackground.setVisibility(View.GONE);
                        adapterCommentList.setAnimationFinished(true);
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
                        .setDuration(DURATION_ACTIONS_FADE)
                        .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                        .setListener(null)
                        .start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) swipeRefreshCommentList
                .getLayoutParams();
        layoutParams.topMargin = (int) targetY;
        layoutParams.setMarginStart((int) startX);
        layoutParams.setMarginEnd(startMarginEnd);
        swipeRefreshCommentList.setLayoutParams(layoutParams);

        view.startAnimation(animation);
    }

    private int getIndexAtCenter() {

        if (adapterCommentList.getItemCount() < 2) {
            return 0;
        }

        int start = linearLayoutManager.findFirstVisibleItemPosition();
        int end = linearLayoutManager.findLastVisibleItemPosition();

        int[] locationRecycler = new int[2];
        recyclerCommentList.getLocationOnScreen(locationRecycler);

        int centerY = locationRecycler[1] + 10 + recyclerCommentList.getHeight() / 2 - toolbar
                .getHeight();
        int[] location = new int[2];

        for (int index = start; index <= end; index++) {

            RecyclerView.ViewHolder viewHolder = recyclerCommentList
                    .findViewHolderForAdapterPosition(index);
            if (viewHolder != null) {
                viewHolder.itemView.getLocationOnScreen(location);
                Rect bounds = new Rect(location[0], location[1],
                        location[0] + viewHolder.itemView.getWidth(),
                        location[1] + viewHolder.itemView.getHeight());
                if (bounds.contains(bounds.centerX(), centerY)) {
                    return index;
                }

            }

        }

        return linearLayoutManager.findFirstCompletelyVisibleItemPosition();

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
            AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    buttonExpandActions.setImageResource(android.R.color.transparent);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setInterpolator(fastOutSlowInInterpolator);
            alphaAnimation.setDuration(DURATION_ACTIONS_FADE);
            alphaAnimation.setStartOffset(
                    (long) ((layoutActions
                            .getChildCount() - 1 - index) * DURATION_ACTIONS_FADE * OFFSET_MODIFIER));
            view.startAnimation(alphaAnimation);
        }

    }

    private void hideLayoutActions(long offset) {
        for (int index = 0; index < layoutActions.getChildCount(); index++) {
            final View view = layoutActions.getChildAt(index);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                    buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                    buttonExpandActions.setColorFilter(colorFilterAccent);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setInterpolator(fastOutSlowInInterpolator);
            alphaAnimation.setDuration(DURATION_ACTIONS_FADE);
            alphaAnimation.setStartOffset((long) (index * offset * OFFSET_MODIFIER));
            view.startAnimation(alphaAnimation);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerComments().addListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.getControllerComments().addListener(listener);
    }

    @Override
    public void onPause() {
        mListener.getControllerComments().removeListener(listener);
        super.onPause();
    }

    @Override
    public void onStop() {
        adapterCommentList.destroyViewHolderLink();
        super.onStop();
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
    public void onDestroyView() {
        adapterCommentList.unregisterAdapterDataObserver(observer);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (youTubePlayer != null) {
            youTubePlayer.release();
            youTubePlayer = null;
        }
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    public void setFragmentToHide(FragmentBase fragmentToHide, View viewHolderView) {
        this.fragmentToHide = fragmentToHide;
        this.viewHolderView = viewHolderView;
        fragmentParentTag = fragmentToHide.getTag();
    }

    @Override
    public boolean navigateBack() {
        Log.d(TAG, "navigateBack");

        if (youTubePlayer != null && isFullscreen) {
            youTubePlayer.setFullscreen(false);
        }
        else if (getFragmentManager().getBackStackEntryCount() == 0) {
            calculateExit();
        }
        else if (!recyclerCommentList.isShown()) {
            calculateExit();
        }
        else {
            if (!isAdded()) {
                return true;
            }

            calculateExit();

        }
        return false;

    }

    private void calculateExit() {
        isFinished = true;
        if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
            animateExit();
        }
        else if (linearLayoutManager.findFirstVisibleItemPosition() < 20) {
            recyclerCommentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        animateExit();
                    }
                }
            });
            recyclerCommentList.smoothScrollToPosition(0);
        }
        else {
            slideExit();
        }
    }

    private void animateExit() {

        // TODO: Make startY solely dependent on literal screen location

        viewBackground.setVisibility(View.VISIBLE);
        adapterCommentList.collapseViewHolderLink();
        adapterCommentList.fadeComments(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                adapterCommentList.setAnimationFinished(false);
                recyclerCommentList.post(new Runnable() {
                    @Override
                    public void run() {

                        if (!isAdded()) {
                            return;
                        }

                        int[] locationSwipeRefresh = new int[2];
                        swipeRefreshCommentList.getLocationOnScreen(locationSwipeRefresh);
                        final float screenWidth = getResources().getDisplayMetrics().widthPixels;
                        final float screenHeight = getResources().getDisplayMetrics().heightPixels;
                        final float targetY = startY - locationSwipeRefresh[1];

                        long duration = ScrollAwareFloatingActionButtonBehavior.DURATION;

                        AnimationUtils.shrinkAndFadeOut(buttonExpandActions, duration).start();

                        if (buttonJumpTop.isShown()) {
                            AnimationUtils.shrinkAndFadeOut(buttonJumpTop, duration).start();
                            AnimationUtils.shrinkAndFadeOut(buttonCommentNext, duration).start();
                            AnimationUtils.shrinkAndFadeOut(buttonCommentPrevious, duration).start();
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
                                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) swipeRefreshCommentList
                                        .getLayoutParams();
                                layoutParams.topMargin = (int) (targetY * interpolatedTime);
                                layoutParams.setMarginStart((int) (startX * interpolatedTime));
                                layoutParams.setMarginEnd(
                                        (int) (startMarginEnd * interpolatedTime));
                                swipeRefreshCommentList.setLayoutParams(layoutParams);
                                layoutAppBar.setTranslationY(-toolbarHeight * interpolatedTime);

                                RelativeLayout.LayoutParams layoutParamsBackground = (RelativeLayout.LayoutParams) viewBackground
                                        .getLayoutParams();
                                layoutParamsBackground.width = (int) (screenWidth - (startX + startMarginEnd) * interpolatedTime);
                                layoutParamsBackground.height = (int) ((1f - interpolatedTime) * screenHeight);
                                viewBackground.setLayoutParams(layoutParamsBackground);
                                viewBackground.setTranslationX(startX * interpolatedTime);
                                viewBackground.setTranslationY(targetY * interpolatedTime);
                            }
                        };
                        animation.setDuration(DURATION_EXIT);
                        animation.setStartOffset(ScrollAwareFloatingActionButtonBehavior.DURATION);
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
                                    fragment.onShown();
                                }
                                try {
                                    getFragmentManager().popBackStackImmediate();
                                }
                                catch (IllegalStateException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        recyclerCommentList.startAnimation(animation);
                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void slideExit() {
        viewBackground.setVisibility(View.VISIBLE);
        FragmentBase fragment = (FragmentBase) getFragmentManager()
                .findFragmentByTag(fragmentParentTag);
        if (fragment != null) {
            fragment.setVisibilityOfThing(View.VISIBLE, mListener.getControllerComments().getLink());
            fragment.onHiddenChanged(false);
        }
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        layoutRelative.animate().translationX(screenWidth).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                FragmentBase fragment = (FragmentBase) getFragmentManager()
                        .findFragmentById(R.id.frame_fragment);
                if (fragment != null) {
                    fragment.onHiddenChanged(false);
                    getFragmentManager().beginTransaction()
                            .show(fragment)
                            .commit();
                }
                getFragmentManager().popBackStackImmediate();

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
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_load_full_comments:
                mListener.getControllerComments().loadLinkComments();
                break;
        }

        item.setChecked(true);

        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            mListener.getControllerComments()
                    .setSort(sort);
            linearLayoutManager.scrollToPositionWithOffset(1, 0);
            return true;
        }

        return true;
    }

    public interface YouTubeListener {
        void loadYouTube(Link link, String id, int timeInMillis);
        boolean hideYouTube();
    }

}