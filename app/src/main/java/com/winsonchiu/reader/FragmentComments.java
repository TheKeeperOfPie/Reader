/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.winsonchiu.reader.data.Link;

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
    private Activity activity;
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
    private Fragment fragmentToHide;
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

    private void setUpOptionsMenu() {
        // No menu items needed
        toolbar.inflateMenu(R.menu.menu_comments);
        itemLoadFullComments = toolbar.getMenu().findItem(R.id.item_load_full_comments);
        toolbar.setOnMenuItemClickListener(this);

        toolbar.getMenu().findItem(mListener.getControllerComments().getSort().getMenuId()).setChecked(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_comments, container, false);

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
                linearLayoutManager.scrollToPositionWithOffset(position, recyclerCommentList.getHeight() / 2 - toolbar.getHeight());
                recyclerCommentList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final RecyclerView.ViewHolder viewHolder = recyclerCommentList.findViewHolderForAdapterPosition(position);
                        if (viewHolder != null) {
                            viewHolder.itemView.getBackground().setState(new int[] {android.R.attr.state_pressed, android.R.attr.state_enabled});
                            recyclerCommentList.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    viewHolder.itemView.getBackground().setState(new int[0]);
                                }
                            }, 150);
                        }
                    }
                }, 200);
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

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });
        setUpOptionsMenu();

        layoutActions = (LinearLayout) view.findViewById(R.id.layout_actions);
        buttonExpandActions = (FloatingActionButton) view.findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLayoutActions();
            }
        });

        behaviorFloatingActionButton = new ScrollAwareFloatingActionButtonBehavior(activity, null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                        hideLayoutActions(0);
                    }

                    @Override
                    public void onEndHideFromScroll() {
                        buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                    }

                });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                .setBehavior(behaviorFloatingActionButton);

        buttonJumpTop = (FloatingActionButton) view.findViewById(R.id.button_jump_top);
        buttonJumpTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
            }
        });
        buttonJumpTop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity, getString(R.string.content_description_button_jump_top),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonCommentPrevious = (FloatingActionButton) view
                .findViewById(R.id.button_comment_previous);
        buttonCommentPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getIndexAtCenter();
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
                Toast.makeText(activity, getString(R.string.content_description_button_comment_previous), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        buttonCommentNext = (FloatingActionButton) view.findViewById(R.id.button_comment_next);
        buttonCommentNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getIndexAtCenter();
                if (position == 0) {
                    if (adapterCommentList.getItemCount() > 0) {
                        listener.scrollTo(1);
                    }
                    return;
                }
                final int newPosition = mListener.getControllerComments().getNextCommentPosition(position - 1) + 1;
                listener.scrollTo(newPosition);

            }
        });
        buttonCommentNext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity, getString(R.string.content_description_button_comment_next), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams()).setMargins(0, 0, 0, 0);

            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

            LinearLayout.LayoutParams layoutParamsJumpTop = (LinearLayout.LayoutParams) buttonJumpTop.getLayoutParams();
            layoutParamsJumpTop.setMargins(0, 0, 0, 0);
            buttonJumpTop.setLayoutParams(layoutParamsJumpTop);

            LinearLayout.LayoutParams layoutParamsPrevious = (LinearLayout.LayoutParams) buttonCommentPrevious.getLayoutParams();
            layoutParamsPrevious.setMargins(0, 0, 0, 0);
            buttonCommentPrevious.setLayoutParams(layoutParamsPrevious);

            LinearLayout.LayoutParams layoutParamsNext = (LinearLayout.LayoutParams) buttonCommentNext.getLayoutParams();
            layoutParamsNext.setMargins(0, 0, 0, 0);
            buttonCommentNext.setLayoutParams(layoutParamsNext);

            RelativeLayout.LayoutParams layoutParamsActions = (RelativeLayout.LayoutParams) layoutActions.getLayoutParams();
            layoutParamsActions.setMarginStart(margin);
            layoutParamsActions.setMarginEnd(margin);
            layoutActions.setLayoutParams(layoutParamsActions);
        }

        viewYouTube = (YouTubePlayerView) view.findViewById(R.id.youtube);

        swipeRefreshCommentList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_comment_list);
        swipeRefreshCommentList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerComments().reloadAllComments();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerCommentList = (RecyclerView) view.findViewById(R.id.recycler_comment_list);
        recyclerCommentList.setLayoutManager(linearLayoutManager);
        recyclerCommentList.setItemAnimator(null);

        if (adapterCommentList == null) {

            adapterCommentList = new AdapterCommentList(activity, mListener.getControllerComments(),
                    mListener.getControllerUser(),
                    mListener.getEventListenerBase(),
                    mListener.getEventListenerComment(),
                    new DisallowListener() {
                        @Override
                        public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                            recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
                            swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
                        }

                        @Override
                        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

                        }
                    }, new RecyclerCallback() {
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

            }, new YouTubeListener() {
                @Override
                public void loadYouTube(Link link,
                        final String id,
                        final int timeInMillis) {

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

                                    DisplayMetrics displayMetrics = activity.getResources()
                                            .getDisplayMetrics();

                                    if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
                                        youTubePlayer.setOnFullscreenListener(
                                                new YouTubePlayer.OnFullscreenListener() {
                                                    @Override
                                                    public void onFullscreen(boolean fullscreen) {
                                                        isFullscreen = fullscreen;
                                                        Log.d(TAG, "fullscreen: " + fullscreen);
                                                        if (!fullscreen) {
                                                            youTubePlayer.pause();
                                                            youTubePlayer.release();
                                                            youTubePlayer = null;
                                                            viewYouTube.setVisibility(View.GONE);
                                                        }
                                                    }
                                                });
                                        youTubePlayer.setPlayerStateChangeListener(
                                                new YouTubePlayer.PlayerStateChangeListener() {
                                                    @Override
                                                    public void onLoading() {

                                                    }

                                                    @Override
                                                    public void onLoaded(String s) {
                                                    }

                                                    @Override
                                                    public void onAdStarted() {

                                                    }

                                                    @Override
                                                    public void onVideoStarted() {
                                                        youTubePlayer.setFullscreen(true);
                                                        youTubePlayer.seekToMillis(timeInMillis);
                                                        youTubePlayer.setPlayerStateChangeListener(
                                                                new YouTubePlayer.PlayerStateChangeListener() {
                                                                    @Override
                                                                    public void onLoading() {

                                                                    }

                                                                    @Override
                                                                    public void onLoaded(String s) {

                                                                    }

                                                                    @Override
                                                                    public void onAdStarted() {

                                                                    }

                                                                    @Override
                                                                    public void onVideoStarted() {

                                                                    }

                                                                    @Override
                                                                    public void onVideoEnded() {

                                                                    }

                                                                    @Override
                                                                    public void onError(YouTubePlayer.ErrorReason errorReason) {

                                                                    }
                                                                });
                                                    }

                                                    @Override
                                                    public void onVideoEnded() {

                                                    }

                                                    @Override
                                                    public void onError(YouTubePlayer.ErrorReason errorReason) {

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
                                        youTubePlayer.setPlayerStateChangeListener(
                                                new YouTubePlayer.PlayerStateChangeListener() {
                                                    @Override
                                                    public void onLoading() {

                                                    }

                                                    @Override
                                                    public void onLoaded(String s) {

                                                    }

                                                    @Override
                                                    public void onAdStarted() {

                                                    }

                                                    @Override
                                                    public void onVideoStarted() {
                                                        youTubePlayer.seekToMillis(timeInMillis);
                                                        youTubePlayer.setPlayerStateChangeListener(
                                                                new YouTubePlayer.PlayerStateChangeListener() {
                                                                    @Override
                                                                    public void onLoading() {

                                                                    }

                                                                    @Override
                                                                    public void onLoaded(String s) {

                                                                    }

                                                                    @Override
                                                                    public void onAdStarted() {

                                                                    }

                                                                    @Override
                                                                    public void onVideoStarted() {

                                                                    }

                                                                    @Override
                                                                    public void onVideoEnded() {

                                                                    }

                                                                    @Override
                                                                    public void onError(YouTubePlayer.ErrorReason errorReason) {

                                                                    }
                                                                });
                                                    }

                                                    @Override
                                                    public void onVideoEnded() {

                                                    }

                                                    @Override
                                                    public void onError(YouTubePlayer.ErrorReason errorReason) {

                                                    }
                                                });
                                    }
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
            }, getArguments().getBoolean(ARG_IS_GRID, false),
                    getArguments().getInt(ARG_COLOR_LINK));

        }

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
        viewBackground = view.findViewById(R.id.view_background);

        if (!getArguments().getBoolean(ARG_INITIALIZED, false)) {
            viewBackground.setVisibility(View.GONE);
            recyclerCommentList.setVisibility(View.GONE);
            swipeRefreshCommentList.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            toolbar.setTranslationY(-100);

            view.post(new Runnable() {
                @Override
                public void run() {
                    animateEnter(view);
                }
            });
        }
        else {
            adapterCommentList.setAnimationFinished(true);
        }

        return view;
    }

    private void animateEnter(final View view) {

        if (getArguments() != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            viewHolderWidth = getArguments().getInt(ARG_ITEM_WIDTH, screenWidth);

            int[] location = getArguments().getIntArray(ARG_LOCATION);
            if (location == null) {
                location = new int[2];
            }

            final TypedArray styledAttributes = activity.getTheme().obtainStyledAttributes(
                    new int[] {android.R.attr.actionBarSize});
            toolbarHeight = styledAttributes.getDimension(0, 0);
            styledAttributes.recycle();

            int[] locationRootView = new int[2];
            view.getLocationOnScreen(locationRootView);

            startX = location[0];
            startY = location[1] - locationRootView[1] - toolbarHeight;

            Log.d(TAG, "location: " + Arrays.toString(location));
            Log.d(TAG, "toolbarHeight: " + toolbarHeight);
            Log.d(TAG, "startX: " + startX);
            Log.d(TAG, "startY: " + startY);

            if (getArguments().getBoolean(ARG_IS_GRID)) {
                float margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
                startX -= margin;
                startY -= margin;
                viewHolderWidth += 2 * margin;
            }
            startMarginEnd = (int) (screenWidth - startX - viewHolderWidth);
        }

        final float screenWidth = getResources().getDisplayMetrics().widthPixels;
        final float screenHeight = getResources().getDisplayMetrics().heightPixels;

        final Animation animation = new Animation() {
            @Override
            public boolean willChangeBounds() {
                return true;
            }

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshCommentList
                        .getLayoutParams();
                float reverseInterpolation = 1.0f - interpolatedTime;
                layoutParams.topMargin = (int) (startY * reverseInterpolation);
                layoutParams.setMarginStart((int) (startX * reverseInterpolation));
                layoutParams.setMarginEnd((int) (startMarginEnd * reverseInterpolation));
                swipeRefreshCommentList.setLayoutParams(layoutParams);
                toolbar.setTranslationY(-toolbarHeight * reverseInterpolation);

                RelativeLayout.LayoutParams layoutParamsBackground = (RelativeLayout.LayoutParams) viewBackground.getLayoutParams();
                layoutParamsBackground.width = (int) (screenWidth - (startX + startMarginEnd) * reverseInterpolation);
                layoutParamsBackground.height = (int) (interpolatedTime * screenHeight);
                viewBackground.setLayoutParams(layoutParamsBackground);
                viewBackground.setTranslationX(startX * reverseInterpolation);
                viewBackground.setTranslationY(startY * reverseInterpolation);
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
                    getFragmentManager().beginTransaction().hide(fragmentToHide).commit();
                    fragmentToHide = null;
                }
                swipeRefreshCommentList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
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

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshCommentList
                .getLayoutParams();
        layoutParams.topMargin = (int) startY;
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

        int centerY = locationRecycler[1] + 10 + recyclerCommentList.getHeight() / 2 - toolbar.getHeight();
        int[] location = new int[2];

        for (int index = start; index <= end; index++) {

            RecyclerView.ViewHolder viewHolder = recyclerCommentList.findViewHolderForAdapterPosition(index);
            if (viewHolder != null) {
                viewHolder.itemView.getLocationOnScreen(location);
                Rect bounds = new Rect(location[0], location[1], location[0] + viewHolder.itemView.getWidth(), location[1] + viewHolder.itemView.getHeight());
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
            AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    view.setVisibility(View.VISIBLE);
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
    public void onResume() {
        super.onResume();
        swipeRefreshCommentList.setRefreshing(mListener.getControllerComments()
                .isLoading());
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerComments().addListener(listener);
    }

    @Override
    public void onStop() {
        adapterCommentList.destroyViewHolderLink();
        mListener.getControllerComments().removeListener(listener);
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        this.activity = activity;
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        activity = null;
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
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    public void setFragmentToHide(Fragment fragmentToHide, View viewHolderView) {
        this.fragmentToHide = fragmentToHide;
        this.viewHolderView = viewHolderView;
        fragmentParentTag = fragmentToHide.getTag();
    }

    @Override
    boolean navigateBack() {
        if (youTubePlayer != null && isFullscreen) {
            youTubePlayer.setFullscreen(false);
            return false;
        }
        else if (getFragmentManager().getBackStackEntryCount() == 0) {
            return true;
        }
        else {
            final float screenWidth = getResources().getDisplayMetrics().widthPixels;
            final float screenHeight = getResources().getDisplayMetrics().heightPixels;

            linearLayoutManager.scrollToPositionWithOffset(0, 0);

            adapterCommentList.setAnimationFinished(false);
            adapterCommentList.collapseViewHolderLink();

            viewBackground.setVisibility(View.VISIBLE);

            ViewCompat.animate(buttonExpandActions)
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                    .setListener(new ViewPropertyAnimatorListener() {
                        @Override
                        public void onAnimationStart(View view) {

                        }

                        @Override
                        public void onAnimationEnd(View view) {

                            final Animation animation = new Animation() {
                                @Override
                                public boolean willChangeBounds() {
                                    return true;
                                }

                                @Override
                                protected void applyTransformation(float interpolatedTime,
                                        Transformation t) {
                                    super.applyTransformation(interpolatedTime, t);
                                    CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) swipeRefreshCommentList
                                            .getLayoutParams();
                                    layoutParams.topMargin = (int) (startY * interpolatedTime);
                                    layoutParams.setMarginStart((int) (startX * interpolatedTime));
                                    layoutParams.setMarginEnd(
                                            (int) (startMarginEnd * interpolatedTime));
                                    swipeRefreshCommentList.setLayoutParams(layoutParams);
                                    toolbar.setTranslationY(-toolbarHeight * interpolatedTime);

                                    RelativeLayout.LayoutParams layoutParamsBackground = (RelativeLayout.LayoutParams) viewBackground.getLayoutParams();
                                    layoutParamsBackground.width = (int) (screenWidth - (startX + startMarginEnd) * interpolatedTime);
                                    layoutParamsBackground.height = (int) ((1f - interpolatedTime) * screenHeight);
                                    viewBackground.setLayoutParams(layoutParamsBackground);
                                    viewBackground.setTranslationX(startX * interpolatedTime);
                                    viewBackground.setTranslationY(startY * interpolatedTime);
                                }
                            };
                            animation.setDuration(DURATION_EXIT);
                            animation.setInterpolator(fastOutSlowInInterpolator);
                            animation.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    Fragment fragment = getFragmentManager()
                                            .findFragmentByTag(fragmentParentTag);
                                    if (fragment != null) {
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
                                    getFragmentManager().popBackStackImmediate();
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });

                            if (getView() != null) {
                                getView().startAnimation(animation);
                            }
                            else {
                                getFragmentManager().popBackStackImmediate();
                            }

                        }

                        @Override
                        public void onAnimationCancel(View view) {

                        }
                    })
                    .start();

            return false;
        }

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_load_full_comments:
                mListener.getControllerComments().loadLinkComments();
                break;
        }

        for (Sort sort : Sort.values()) {
            if (sort.getMenuId() == item.getItemId()) {
                mListener.getControllerComments()
                        .setSort(sort);
                linearLayoutManager.scrollToPositionWithOffset(1, 0);
                return true;
            }
        }

        return true;
    }

    public interface YouTubeListener {
        void loadYouTube(Link link, String id, int timeInMillis);
        boolean hideYouTube();
    }

}