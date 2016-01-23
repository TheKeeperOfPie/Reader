/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.LinearLayoutManagerWrapHeight;
import com.winsonchiu.reader.utils.OnSizeChangedListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.YouTubeListener;
import com.winsonchiu.reader.views.CustomFrameLayout;

public class FragmentCommentsInner extends FragmentBase {

    public static final String TAG = FragmentCommentsInner.class.getCanonicalName();

    private static final String ARG_IS_GRID = "isGrid";
    private static final String ARG_FIRST_LINK_NAME = "firstLinkName";
    private static final String ARG_COLOR_LINK = "colorLink";
    private static final String ARG_LOCATION = "location";
    private static final String ARG_ITEM_HEIGHT = "itemHeight";
    private static final String ARG_ITEM_WIDTH = "itemWidth";
    private static final String ARG_ACTIONS_EXPANDED = "actionsExpanded";
    private static final String ARG_YOUTUBE_ID = "youTubeId";
    private static final String ARG_YOUTUBE_TIME = "youTubeTime";
    private static final int EXPAND_FLING_THRESHOLD = 1000;

    private FragmentListenerBase mListener;
    private RecyclerView recyclerLink;
    private RecyclerView recyclerCommentList;
    private ViewGroup layoutExpandPostInner;
    private ImageView imageExpandIndicator;
    private LinearLayoutManagerWrapHeight layoutManagerLink;
    private LinearLayoutManager linearLayoutManager;
    private AdapterLinkHeader adapterLink;
    private AdapterCommentList adapterCommentList;
    private SwipeRefreshLayout swipeRefreshCommentList;
    private RecyclerView.AdapterDataObserver observer;
    private CustomFrameLayout layoutRoot;
    private ColorFilter colorFilterIcon;
    private int scrollToPaddingTop;
    private int scrollToPaddingBottom;

    private boolean postExpanded;
    private int heightExpandHandle;
    private int targetExpandPostHeight;
    private float expandFlingThreshold;
    private ValueAnimator valueAnimatorPostExpand = ValueAnimator.ofFloat(0, 1);
    private boolean animationFinished;

    private ControllerComments controllerComments = new ControllerComments();
    private Link link;

    private ControllerComments.Listener listener = new ControllerComments.Listener() {
        @Override
        public void setSort(Sort sort) {
            if (callback.isCurrentFragment(FragmentCommentsInner.this)) {
                callback.setSort(sort);
            }
        }

        @Override
        public void setIsCommentThread(boolean isCommentThread) {
            if (callback.isCurrentFragment(FragmentCommentsInner.this)) {
                callback.setIsCommentThread(isCommentThread);
            }
        }

        @Override
        public void scrollTo(final int position) {
            UtilsAnimation.scrollToPositionWithCentering(position,
                    recyclerCommentList, linearLayoutManager, scrollToPaddingTop, scrollToPaddingBottom, true);
        }

        @Override
        public void insertComment(Comment comment) {
            // Child ControllerComments so do nothing
        }

        @Override
        public void setNsfw(String name, boolean over18) {
            // Child ControllerComments so do nothing
        }

        @Override
        public RecyclerView.Adapter getAdapter() {
            return adapterCommentList;
        }

        @Override
        public void setToolbarTitle(CharSequence title) {
            if (callback.isCurrentFragment(FragmentCommentsInner.this)) {
                callback.setTitle(title);
            }
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
    private DisallowListener disallowListener = new DisallowListener() {
        @Override
        public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
            recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
            swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
        }

        @Override
        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

        }
    };
    private YouTubeListener youTubeListener = new YouTubeListener() {
        @Override
        public void loadYouTubeVideo(Link link, final String id, final int timeInMillis) {
            callback.loadYouTubeVideo(link, id, timeInMillis);
        }

        @Override
        public boolean hideYouTube() {
            return callback.hideYouTube();
        }
    };
    private Callback callback = new Callback() {
        @Override
        public void loadYouTubeVideo(String id, int timeInMillis) {

        }

        @Override
        public void releaseYouTube() {

        }

        @Override
        public void setPostExpanded(boolean expanded) {

        }

        @Override
        public void setIsCommentThread(boolean isCommentThread) {

        }

        @Override
        public void setSort(Sort sort) {

        }

        @Override
        public void setTitle(CharSequence title) {

        }

        @Override
        public void onReplyShown() {

        }

        @Override
        public void hideToolbar() {

        }

        @Override
        public int getAppBarHeight() {
            return 0;
        }

        @Override
        public boolean isCurrentFragment(FragmentCommentsInner fragmentCommentsInner) {
            return false;
        }

        @Override
        public void loadYouTubeVideo(Link link, String id, int timeInMillis) {

        }

        @Override
        public boolean hideYouTube() {
            return false;
        }
    };

    public static FragmentCommentsInner newInstance() {
        FragmentCommentsInner fragment = new FragmentCommentsInner();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_GRID, false);
        args.putInt(ARG_COLOR_LINK, 0);
        args.putIntArray(ARG_LOCATION, new int[2]);
        args.putInt(ARG_ITEM_HEIGHT, 0);
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentCommentsInner newInstance(boolean isGrid, String firstLinkId, int colorLink) {
        FragmentCommentsInner fragment = new FragmentCommentsInner();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_GRID, isGrid);
        args.putString(ARG_FIRST_LINK_NAME, firstLinkId);
        args.putInt(ARG_COLOR_LINK, colorLink);
        args.putIntArray(ARG_LOCATION, new int[2]);
        args.putInt(ARG_ITEM_HEIGHT, 0);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentCommentsInner() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("Link", controllerComments.getLink());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        scrollToPaddingTop = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        scrollToPaddingBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56,
                getResources().getDisplayMetrics());

        layoutRoot = (CustomFrameLayout) inflater
                .inflate(R.layout.fragment_comments_inner, container, false);

        final RecyclerCallback recyclerCallback = new RecyclerCallback() {
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
                callback.hideToolbar();
            }

            @Override
            public void onReplyShown() {
                callback.onReplyShown();
            }
        };

        CallbackYouTubeDestruction callbackYouTubeDestruction = new CallbackYouTubeDestruction() {
            @Override
            public void destroyYouTubePlayerFragments() {
                adapterCommentList.destroyYouTubePlayerFragments();
                adapterLink.destroyYouTubePlayerFragments();
            }
        };

        TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIcon = typedArray.getColor(0, getResources().getColor(R.color.darkThemeIconFilter));
        typedArray.recycle();

        colorFilterIcon = new PorterDuffColorFilter(colorIcon,
                PorterDuff.Mode.MULTIPLY);

        swipeRefreshCommentList = (SwipeRefreshLayout) layoutRoot.findViewById(
                R.id.swipe_refresh_comment_list);
        swipeRefreshCommentList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerComments.reloadAllComments();
            }
        });

        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerCommentList = (RecyclerView) layoutRoot.findViewById(R.id.recycler_comment_list);
        recyclerCommentList.setLayoutManager(linearLayoutManager);
        recyclerCommentList.setItemAnimator(null);
        recyclerCommentList.addItemDecoration(
                new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST));

        adapterCommentList = new AdapterCommentList(getActivity(),
                controllerComments,
                mListener.getEventListenerBase(),
                mListener.getEventListener(),
                disallowListener,
                recyclerCallback,
                youTubeListener,
                callbackYouTubeDestruction,
                getArguments().getBoolean(ARG_IS_GRID, false),
                getArguments().getString(ARG_FIRST_LINK_NAME),
                getArguments().getInt(ARG_COLOR_LINK, 0),
                getArguments().getBoolean(ARG_ACTIONS_EXPANDED, false));

        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                if (positionStart == 0) {
                    callback.releaseYouTube();
                }
            }
        };

        adapterCommentList.registerAdapterDataObserver(observer);
        recyclerCommentList.setAdapter(adapterCommentList);

        layoutManagerLink = new LinearLayoutManagerWrapHeight(getActivity(), LinearLayoutManager.VERTICAL, false);
        layoutManagerLink.setOnSizeChangedListener(new OnSizeChangedListener() {
            @Override
            public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                if (postExpanded && targetExpandPostHeight == 0) {
                    targetExpandPostHeight = height + heightExpandHandle;
                    postExpanded = false;
                    expandPost(true);
                }
            }
        });

        final DisallowListener disallowListenerLink = new DisallowListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerLink.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        final RecyclerCallback recyclerCallbackLink = new RecyclerCallback() {
            @Override
            public void scrollTo(int position) {
                layoutManagerLink.scrollToPositionWithOffset(position, 0);
            }

            @Override
            public int getRecyclerHeight() {
                // Since we animate the height of recyclerLink, we need to return a stable height
                return recyclerCommentList.getHeight();
            }

            @Override
            public RecyclerView.LayoutManager getLayoutManager() {
                return layoutManagerLink;
            }

            @Override
            public void hideToolbar() {
                callback.hideToolbar();
            }

            @Override
            public void onReplyShown() {
                callback.onReplyShown();
            }
        };

        adapterLink = new AdapterLinkHeader(getActivity(),
                controllerComments,
                mListener.getEventListenerBase(),
                disallowListenerLink,
                recyclerCallbackLink,
                youTubeListener,
                callbackYouTubeDestruction,
                getArguments().getBoolean(ARG_IS_GRID, false),
                getArguments().getString(ARG_FIRST_LINK_NAME),
                getArguments().getInt(ARG_COLOR_LINK, 0),
                getArguments().getBoolean(ARG_ACTIONS_EXPANDED, false));

        final GestureDetectorCompat gestureDetectorExpand = new GestureDetectorCompat(getActivity(), new GestureDetector.SimpleOnGestureListener() {

            private float startY;
            private int startHeight;

            @Override
            public boolean onDown(MotionEvent e) {
                if (targetExpandPostHeight == 0) {
                    targetExpandPostHeight = layoutManagerLink.getFirstChildHeight() + heightExpandHandle;
                }

                if (e.getY() > imageExpandIndicator.getY() && e.getY() < imageExpandIndicator.getY() + heightExpandHandle) {
                    startY = e.getY();
                    startHeight = targetExpandPostHeight;
                }
                else {
                    startY = 0;
                }

                return super.onDown(e);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

                if (startY == 0) {
                    return super.onScroll(e1, e2, distanceX, distanceY);
                }

                float distance = e2.getY() - startY;

                targetExpandPostHeight = (int) Math.min(startHeight + distance, layoutRoot.getHeight());

                layoutExpandPostInner.getLayoutParams().height = targetExpandPostHeight;
                layoutExpandPostInner.requestLayout();

                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (startY == 0) {
                    return super.onFling(e1, e2, velocityX, velocityY);
                }

                if (velocityY < -expandFlingThreshold) {
                    startY = 0;
                    expandPost(false);
                    return true;
                }
                else if (velocityY > expandFlingThreshold) {
                    startY = 0;
                    targetExpandPostHeight = layoutRoot.getHeight() - heightExpandHandle;
                    postExpanded = false;
                    expandPost(true);
                    return true;
                }

                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        heightExpandHandle = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 33, getResources().getDisplayMetrics());
        expandFlingThreshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EXPAND_FLING_THRESHOLD, getResources().getDisplayMetrics());

        layoutExpandPostInner = (ViewGroup) layoutRoot.findViewById(R.id.layout_expand_post_inner);
        imageExpandIndicator = (ImageView) layoutRoot.findViewById(R.id.image_expand_indicator);
        imageExpandIndicator.setColorFilter(colorFilterIcon);

        layoutRoot.setDispatchTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetectorExpand.onTouchEvent(event);
            }
        });

        recyclerLink = (RecyclerView) layoutRoot.findViewById(R.id.recycler_link);
        recyclerLink.setLayoutManager(layoutManagerLink);
        recyclerLink.setItemAnimator(null);

        if (savedInstanceState != null) {
            String youtubeId = savedInstanceState.getString(ARG_YOUTUBE_ID, null);

            if (!TextUtils.isEmpty(youtubeId)) {
                callback.loadYouTubeVideo(youtubeId, savedInstanceState.getInt(ARG_YOUTUBE_TIME, 0));
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("Link")) {
            adapterLink.setAnimationFinished(true);
            adapterCommentList.setAnimationFinished(true);
            controllerComments.setLinkFromCache((Link) savedInstanceState.get("Link"));
            Log.d(TAG, "onCreateView() called with: " + "inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");
        } else if (link != null) {
            controllerComments.setLink(link);
            link = null;
        }

        if (animationFinished) {
            setAnimationFinished(true);
        }

        getArguments().putBoolean("Created", true);

        return layoutRoot;
    }

    private int getIndexAtCenter() {

        if (adapterCommentList.getItemCount() < 2) {
            return 0;
        }

        int start = linearLayoutManager.findFirstVisibleItemPosition();
        int end = linearLayoutManager.findLastVisibleItemPosition();

        int centerY = 10 + (recyclerCommentList
                .getHeight() - scrollToPaddingBottom + scrollToPaddingTop) / 2;

        for (int index = start; index <= end; index++) {

            RecyclerView.ViewHolder viewHolder = recyclerCommentList
                    .findViewHolderForAdapterPosition(index);
            if (viewHolder != null) {
                RectF bounds = new RectF(viewHolder.itemView.getX(), viewHolder.itemView.getY(),
                        viewHolder.itemView.getX() + viewHolder.itemView.getWidth(),
                        viewHolder.itemView.getY() + viewHolder.itemView.getHeight());
                if (bounds.contains(bounds.centerX(), centerY)) {
                    return index;
                }

            }

        }

        return linearLayoutManager.findFirstCompletelyVisibleItemPosition();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (animationFinished) {
            controllerComments.addListener(listener);
        }
    }

    @Override
    public void onPause() {
        controllerComments.removeListener(listener);
        super.onPause();
    }

    @Override
    public void onStop() {
        adapterCommentList.destroyViewHolderLink();
        adapterLink.destroyViewHolderLink();
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

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
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    public void expandPost(boolean expanded) {
        postExpanded = expanded;

        final float expandedHeight = targetExpandPostHeight > 0 ? targetExpandPostHeight : layoutManagerLink.getFirstChildHeight() + heightExpandHandle;
        final float targetHeight = postExpanded ? expandedHeight : 0;
        final float startHeight = targetExpandPostHeight == 0 ? 0 : layoutExpandPostInner.getHeight();
        valueAnimatorPostExpand.cancel();
        valueAnimatorPostExpand.removeAllUpdateListeners();
        valueAnimatorPostExpand.removeAllListeners();
        valueAnimatorPostExpand.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float interpolatedValue = animation.getAnimatedFraction();
                layoutExpandPostInner.getLayoutParams().height = (int) (startHeight + (targetHeight - startHeight) * interpolatedValue);
                layoutExpandPostInner.requestLayout();
            }
        });
        valueAnimatorPostExpand.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (callback.isCurrentFragment(FragmentCommentsInner.this)) {
                    callback.setPostExpanded(postExpanded);
                }
                layoutExpandPostInner.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!postExpanded) {
                    layoutExpandPostInner.setVisibility(View.INVISIBLE);
                    layoutManagerLink.requestLayout();
                }
            }
        });
        valueAnimatorPostExpand.start();
    }

    public void setAnimationFinished(boolean animationFinished) {
        if (isAdded() && adapterLink != null && adapterCommentList != null) {
            adapterLink.setAnimationFinished(animationFinished);
            adapterCommentList.setAnimationFinished(animationFinished);

            if (animationFinished) {
                if (recyclerLink.getAdapter() != adapterLink) {
                    recyclerLink.setAdapter(adapterLink);
                }
                controllerComments.addListener(listener);
            }
        }


        this.animationFinished = animationFinished;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setLink(Link link) {
        if (TextUtils.isEmpty(controllerComments.getLink().getId())) {
            if (getView() == null) {
                this.link = link;
            }
            else {
                controllerComments.setLink(link);
            }
        }
    }

    public boolean getPostExpanded() {
        return postExpanded;
    }

    public boolean getIsCommentThread() {
        return controllerComments.getIsCommentThread();
    }

    public Sort getSort() {
        return controllerComments.getSort();
    }

    public CharSequence getTitle() {
        if (link != null) {
            return link.getTitle();
        }

        return controllerComments.getLink().getTitle();
    }

    public void clear() {
        setCallback(new Callback() {
            @Override
            public void loadYouTubeVideo(String id, int timeInMillis) {

            }

            @Override
            public void releaseYouTube() {

            }

            @Override
            public void setPostExpanded(boolean expanded) {

            }

            @Override
            public void setIsCommentThread(boolean isCommentThread) {

            }

            @Override
            public void setSort(Sort sort) {

            }

            @Override
            public void setTitle(CharSequence title) {

            }

            @Override
            public void hideToolbar() {

            }

            @Override
            public void onReplyShown() {

            }

            @Override
            public int getAppBarHeight() {
                return 0;
            }

            @Override
            public boolean isCurrentFragment(FragmentCommentsInner fragmentCommentsInner) {
                return false;
            }

            @Override
            public void loadYouTubeVideo(Link link, String id, int timeInMillis) {

            }

            @Override
            public boolean hideYouTube() {
                return false;
            }
        });
        controllerComments.removeListener(listener);
    }

    public Link getLink() {
        return controllerComments.getLink();
    }

    public void setPosition(int position) {
        getArguments().putInt("position", position);
    }

    public int getPosition() {
        return getArguments().getInt("position");
    }

    public void setSort(Sort sort) {
        controllerComments.setSort(sort);
    }

    public void collapseViewHolderLink(boolean actionsExpanded) {
        adapterCommentList.collapseViewHolderLink(actionsExpanded);
    }

    public void fadeComments(Runnable runnable) {
        adapterCommentList.fadeComments(getResources(), runnable);
    }

    public void scrollToPositionWithOffset(int position, int offset) {
        linearLayoutManager.scrollToPositionWithOffset(position, offset);
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return linearLayoutManager.findFirstCompletelyVisibleItemPosition();
    }

    public int findFirstVisibleItemPosition() {
        return linearLayoutManager.findFirstVisibleItemPosition();
    }

    public void smoothScrollToPosition(int position) {
        recyclerCommentList.smoothScrollToPosition(position);
    }

    public void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
        recyclerCommentList.addOnScrollListener(onScrollListener);
    }

    public boolean isRecyclerCommentsShown() {
        return recyclerCommentList.isShown();
    }

    public void nextComment() {
        if (adapterCommentList.getItemCount() == 0) {
            return;
        }

        callback.hideToolbar();

        int position = getIndexAtCenter();

        switch (position) {
            case RecyclerView.NO_POSITION:
                position = 0;
                break;
            case 0:
                position = 1;
                break;
            default:
                position = controllerComments
                        .getNextCommentPosition(position - 1) + 1;
                break;
        }

        UtilsAnimation.scrollToPositionWithCentering(position,
                recyclerCommentList,
                linearLayoutManager,
                scrollToPaddingTop,
                scrollToPaddingBottom,
                true);

    }

    public void previousComment() {
        callback.hideToolbar();
        int position = getIndexAtCenter();
        if (position == -1) {
            position = linearLayoutManager.findFirstVisibleItemPosition();
        }
        if (position == 1) {
            position = 0;
        }

        final int newPosition = controllerComments
                .getPreviousCommentPosition(
                        position - 1) + 1;

        UtilsAnimation.scrollToPositionWithCentering(newPosition,
                recyclerCommentList,
                linearLayoutManager,
                scrollToPaddingTop,
                scrollToPaddingBottom,
                true);

    }

    public ControllerComments getControllerComments() {
        return controllerComments;
    }

    public void loadLinkComments() {
        controllerComments.loadLinkComments();
    }

    public boolean isPostExpanded() {
        return postExpanded;
    }

    public interface Callback extends YouTubeListener {
        void loadYouTubeVideo(String id, int timeInMillis);
        void releaseYouTube();
        void setPostExpanded(boolean expanded);
        void setIsCommentThread(boolean isCommentThread);
        void setSort(Sort sort);
        void setTitle(CharSequence title);
        void hideToolbar();
        void onReplyShown();
        int getAppBarHeight();
        boolean isCurrentFragment(FragmentCommentsInner fragmentCommentsInner);
    }
}