/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.adapter.AdapterNotifySubscriber;
import com.winsonchiu.reader.adapter.RxAdapterEvent;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Report;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.LinksListenerBase;
import com.winsonchiu.reader.rx.ObserverError;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.LinearLayoutManagerWrapHeight;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsList;
import com.winsonchiu.reader.utils.UtilsRx;
import com.winsonchiu.reader.utils.YouTubeListener;
import com.winsonchiu.reader.views.CustomFrameLayout;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

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

    private ControllerComments controllerComments;
    private Link link;

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
        public void clearDecoration() {

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

    private Subscription subscriptionData;
    private Subscription subscriptionLoading;
    private Subscription subscriptionSort;
    private Subscription subscriptionScrollEvents;

    private Report reportSelected;

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
    public void onAttach(Context context) {
        super.onAttach(context);

        if (controllerComments == null) {
            controllerComments = new ControllerComments(((ActivityMain) context).getComponentActivity());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    protected void inject() {
        ((ActivityMain) getActivity()).getComponentActivity().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        scrollToPaddingTop = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        scrollToPaddingBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56,
                getResources().getDisplayMetrics());

        layoutRoot = (CustomFrameLayout) inflater
                .inflate(R.layout.fragment_comments_inner, container, false);

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
        recyclerCommentList.setHasFixedSize(true);
        recyclerCommentList.setItemAnimator(null);

        AdapterListener adapterListenerComment = new AdapterListener() {

            @Override
            public void scrollAndCenter(int position, int height) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0);
            }

            @Override
            public void hideToolbar() {
                callback.hideToolbar();
            }

            @Override
            public void clearDecoration() {
                callback.clearDecoration();
            }

            @Override
            public void requestMore() {
                controllerComments.loadMoreComments();
            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        AdapterLink.ViewHolderLink.Listener listener =  new LinksListenerBase(mListener.getEventListenerBase()) {
            @Override
            public void onVote(Link link, AdapterLink.ViewHolderLink viewHolderLink, int vote) {

            }

            @Override
            public void onDelete(Link link) {

            }

            @Override
            public void onReport(Link link) {

            }

            @Override
            public void onSave(Link link) {

            }

            @Override
            public void onMarkNsfw(Link link) {

            }
        };

        AdapterLink.ViewHolderLink.Listener listenerLink = new LinksListenerBase(mListener.getEventListenerBase()) {
            @Override
            public void onVote(Link link, AdapterLink.ViewHolderLink viewHolderLink, int vote) {

            }

            @Override
            public void onDelete(Link link) {

            }

            @Override
            public void onReport(Link link) {

            }

            @Override
            public void onSave(Link link) {

            }

            @Override
            public void onMarkNsfw(Link link) {

            }
        };

        AdapterCommentList.ViewHolderComment.Listener listenerComment = new AdapterCommentList.ViewHolderComment.Listener() {
            @Override
            public void onClickComments() {

            }

            @Override
            public void onToggleComment(Comment comment) {

            }

            @Override
            public void onShowReplyEditor(Comment comment) {

            }

            @Override
            public void onEditComment(Comment comment, String text) {

            }

            @Override
            public void onSendComment(Comment comment, String text) {

            }

            @Override
            public void onMarkRead(Comment comment) {

            }

            @Override
            public void onLoadNestedComments(Comment comment) {

            }

            @Override
            public void onJumpToParent(Comment comment) {

            }

            @Override
            public void onViewProfile(Comment comment) {

            }

            @Override
            public void onCopyText(Comment comment) {

            }

            @Override
            public void onDeleteComment(Comment comment) {

            }

            @Override
            public void onReport(Comment comment) {
                // TODO: Add comment text
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.report_title)
                        .setSingleChoiceItems(Report.getDisplayReasons(getResources()), -1, (dialog, which) -> {
                            reportSelected = Report.values()[which];
                        })
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            if (reportSelected == Report.OTHER) {
                                View viewDialog = LayoutInflater.from(getContext()).inflate(R.layout.dialog_text_input, null, false);
                                final EditText editText = (EditText) viewDialog.findViewById(R.id.edit_text);
                                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
                                new AlertDialog.Builder(getContext())
                                        .setView(viewDialog)
                                        .setTitle(R.string.item_report)
                                        .setPositiveButton(R.string.ok, (dialog1, which1) -> {
                                            mListener.getEventListenerBase()
                                                    .onReport(link, editText.getText().toString())
                                                    .subscribe(new ObserverError<String>() {
                                                        @Override
                                                        public void onError(Throwable e) {
                                                            controllerComments.getEventHolder().getErrors().call(CommentsError.REPORT);
                                                        }
                                                    });
                                        })
                                        .setNegativeButton(R.string.cancel, (dialog1, which1) -> {
                                            dialog1.dismiss();
                                        })
                                        .show();
                            }
                            else if (reportSelected != null) {
                                mListener.getEventListenerBase()
                                        .onReport(link, reportSelected.getReason())
                                        .subscribe(new ObserverError<String>() {
                                            @Override
                                            public void onError(Throwable e) {
                                                controllerComments.getEventHolder().getErrors().call(CommentsError.REPORT);
                                            }
                                        });
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onVoteComment(Comment comment, AdapterCommentList.ViewHolderComment viewHolderComment, int vote) {

            }

            @Override
            public void onSave(Comment comment) {

            }
        };

        adapterCommentList = new AdapterCommentList(getActivity(),
                adapterListenerComment,
                listenerComment,
                listenerLink,
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
        layoutManagerLink.setOnSizeChangedListener((width, height, oldWidth, oldHeight) -> {
            if (postExpanded && targetExpandPostHeight == 0) {
                targetExpandPostHeight = height + heightExpandHandle;
                postExpanded = false;
                expandPost(true);
            }
        });

        AdapterListener adapterListenerLink = new AdapterListener() {
            @Override
            public void scrollAndCenter(int position, int height) {
                layoutManagerLink.scrollToPositionWithOffset(position, 0);
            }

            @Override
            public void hideToolbar() {
                callback.hideToolbar();
            }

            @Override
            public void clearDecoration() {
                callback.clearDecoration();
            }

            @Override
            public void requestMore() {

            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerLink.requestDisallowInterceptTouchEvent(disallow);

            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        adapterLink = new AdapterLinkHeader(getActivity(),
                controllerComments,
                adapterListenerLink,
                listenerLink,
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

        adapterCommentList.setData(controllerComments.getData());

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

        ControllerComments.EventHolder eventHolder = controllerComments.getEventHolder();

        subscriptionData = eventHolder.getData()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new AdapterNotifySubscriber<>(adapterCommentList))
                .map(RxAdapterEvent::getData)
                .doOnNext(data -> {
                    if (callback.isCurrentFragment(this)) {
                        callback.setTitle(data.getLink().getTitle());
                    }
                })
                .map(data -> !UtilsList.isNullOrEmpty(data.getComments()) && !data.getLink().getId().equals(data.getComments().get(0).getParentId()))
                .subscribe(isCommentThread -> {
                    if (callback.isCurrentFragment(this)) {
                        callback.setIsCommentThread(isCommentThread);
                    }
                });

        subscriptionLoading = eventHolder.getLoading()
                .subscribe(swipeRefreshCommentList::setRefreshing);

        subscriptionSort = eventHolder.getSort()
                .subscribe(sort -> {
                    if (callback.isCurrentFragment(this)) {
                        callback.setSort(sort);
                    }
                });

        subscriptionScrollEvents = eventHolder.getScrollEvents()
                .subscribe(position -> {
                    UtilsAnimation.scrollToPositionWithCentering(position,
                            recyclerCommentList,
                            scrollToPaddingTop,
                            scrollToPaddingBottom,
                            true);
                });

        if (animationFinished) {

        }
    }

    @Override
    public void onPause() {
        unsubscribe();
        super.onPause();
    }

    public void unsubscribe() {
        UtilsRx.unsubscribe(subscriptionData);
        UtilsRx.unsubscribe(subscriptionLoading);
        UtilsRx.unsubscribe(subscriptionSort);
        UtilsRx.unsubscribe(subscriptionScrollEvents);
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
        valueAnimatorPostExpand.addUpdateListener(animation -> {
            float interpolatedValue = animation.getAnimatedFraction();
            layoutExpandPostInner.getLayoutParams().height = (int) (startHeight + (targetHeight - startHeight) * interpolatedValue);
            layoutExpandPostInner.requestLayout();
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
            }
        }


        this.animationFinished = animationFinished;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setLink(Link link) {
        /**
            This is a confusing but working way to ensure persistence of state
            between orientation changes. Ensures that a new API request
            is not made by {@link ControllerComments}.
         */
        if (TextUtils.isEmpty(controllerComments.getLink().getId())) {
            if (getView() == null) {
                this.link = link;
            }
            else {
                controllerComments.setLink(link);
            }
        } else if (!controllerComments.getLink().getId().equals(link.getId())) {
            controllerComments.setLink(link);
        }
    }

    public boolean getPostExpanded() {
        return postExpanded;
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
            public void clearDecoration() {

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
        });

        unsubscribe();
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

    public void createControllerComments(ComponentActivity componentActivity) {
        if (controllerComments == null) {
            controllerComments = new ControllerComments(componentActivity);
        }
    }

    public interface Callback extends YouTubeListener {
        void loadYouTubeVideo(String id, int timeInMillis);
        void releaseYouTube();
        void setPostExpanded(boolean expanded);
        void setIsCommentThread(boolean isCommentThread);
        void setSort(Sort sort);
        void setTitle(CharSequence title);
        void clearDecoration();
        void hideToolbar();
        int getAppBarHeight();
        boolean isCurrentFragment(FragmentCommentsInner fragmentCommentsInner);
    }
}