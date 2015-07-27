/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.FragmentNewPost;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.views.CustomItemTouchHelper;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.search.FragmentSearch;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Subreddit;

public class FragmentThreadList extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentThreadList.class.getCanonicalName();
    private static final long DURATION_TRANSITION = 150;
    private static final long DURATION_ACTIONS_FADE = 150;
    private static final float OFFSET_MODIFIER = 0.25f;
    private Activity activity;
    private FragmentListenerBase mListener;

    private SharedPreferences preferences;
    private RecyclerView recyclerThreadList;
    private AdapterLink adapterLink;
    private SwipeRefreshLayout swipeRefreshThreadList;
    private RecyclerView.LayoutManager layoutManager;
    private MenuItem itemInterface;

    private MenuItem itemSearch;
    private TextView textSidebar;
    private DrawerLayout drawerLayout;
    private TextView textEmpty;
    private Menu menu;
    private MenuItem itemSortTime;
    private Toolbar toolbar;
    private AdapterLinkList adapterLinkList;
    private AdapterLinkGrid adapterLinkGrid;
    private Button buttonSubscribe;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private AdapterLink.ViewHolderHeader.EventListener eventListenerHeader;
    private DisallowListener disallowListener;
    private RecyclerCallback recyclerCallback;
    private ControllerLinks.Listener listener;
    private Snackbar snackbar;
    private CustomItemTouchHelper itemTouchHelper;
    private FloatingActionButton buttonExpandActions;
    private FastOutSlowInInterpolator fastOutSlowInInterpolator;
    private LinearLayout layoutActions;
    private FloatingActionButton buttonClearViewed;
    private FloatingActionButton buttonJumpTop;
    private PorterDuffColorFilter colorFilterIcon;

    public static FragmentThreadList newInstance() {
        FragmentThreadList fragment = new FragmentThreadList();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentThreadList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        fastOutSlowInInterpolator = new FastOutSlowInInterpolator();

        eventListenerHeader = new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(String postType) {

                if (TextUtils.isEmpty(mListener.getControllerUser().getUser().getName())) {
                    Toast.makeText(activity, getString(R.string.must_be_logged_in),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                FragmentNewPost fragmentNewPost = FragmentNewPost.newInstance(
                        mListener.getControllerUser().getUser().getName(),
                        mListener.getControllerLinks().getSubreddit().getUrl(),
                        postType,
                        mListener.getControllerLinks().getSubreddit().getSubmitTextHtml());

                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, fragmentNewPost, FragmentNewPost.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void showSidebar() {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        };

        disallowListener = new DisallowListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerThreadList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshThreadList.requestDisallowInterceptTouchEvent(disallow);
                itemTouchHelper.select(null, CustomItemTouchHelper.ACTION_STATE_IDLE);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                itemTouchHelper.setDisallow(disallow);
            }
        };

        recyclerCallback = new RecyclerCallback() {
            @Override
            public void scrollTo(final int position) {
                recyclerThreadList.requestLayout();
                recyclerThreadList.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollToPositionWithOffset(position, 0);
                    }
                });
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerThreadList.getHeight();
            }

            @Override
            public RecyclerView.LayoutManager getLayoutManager() {
                return layoutManager;
            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

        };

        listener = new ControllerLinks.Listener() {

            @Override
            public void setSortAndTime(Sort sort, Time time) {
                menu.findItem(sort.getMenuId()).setChecked(true);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + menu
                                .findItem(mListener.getControllerLinks()
                                        .getTime().getMenuId()).toString());
            }

            @Override
            public void showEmptyView(boolean isEmpty) {
                textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void loadSideBar(Subreddit subreddit) {
                if (subreddit.getUrl().equals("/") || "/r/all/"
                        .equalsIgnoreCase(subreddit.getUrl())) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                            GravityCompat.END);
                    return;
                }

                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
                textSidebar.setText(subreddit.getDescriptionHtml());
                drawerLayout.setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_UNLOCKED);
                if (subreddit.isUserIsSubscriber()) {
                    buttonSubscribe.setText(R.string.unsubscribe);
                }
                else {
                    buttonSubscribe.setText(R.string.subscribe);
                }
                buttonSubscribe.setVisibility(TextUtils.isEmpty(
                        preferences.getString(AppSettings.ACCOUNT_JSON,
                                "")) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void scrollTo(int position) {
                scrollToPositionWithOffset(position, 0);
            }

            @Override
            public void post(Runnable runnable) {
                recyclerThreadList.post(runnable);
            }

            @Override
            public RecyclerView.Adapter getAdapter() {
                return adapterLink;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshThreadList.setRefreshing(refreshing);
            }
        };
    }

    private void setUpOptionsMenu() {

        toolbar.inflateMenu(R.menu.menu_thread_list);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        itemInterface = menu.findItem(R.id.item_interface);
        switch (preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)) {
            case AppSettings.MODE_LIST:
                itemInterface.setIcon(R.drawable.ic_view_module_white_24dp);
                break;
            case AppSettings.MODE_GRID:
                itemInterface.setIcon(R.drawable.ic_view_list_white_24dp);
                break;
        }

        itemSortTime = menu.findItem(R.id.item_sort_time);
        itemSearch = menu.findItem(R.id.item_search);

        menu.findItem(mListener.getControllerLinks().getSort().getMenuId()).setChecked(true);
        itemSortTime.setTitle(
                getString(R.string.time) + Reddit.TIME_SEPARATOR + menu
                        .findItem(mListener.getControllerLinks()
                                .getTime().getMenuId()).toString());

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().setColorFilter(colorFilterIcon);
        }

    }

    private void resetAdapter(AdapterLink newAdapter) {
        int[] currentPosition = new int[3];
        if (layoutManager instanceof LinearLayoutManager) {
            currentPosition[0] = ((LinearLayoutManager) layoutManager)
                    .findFirstVisibleItemPosition();
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPositions(
                    currentPosition);
        }

        adapterLink = newAdapter;
        layoutManager = adapterLink.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerThreadList.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerThreadList.setPadding(padding, 0, padding, 0);
        }

        /*
            Note that we must call setAdapter before setLayoutManager or the ViewHolders
            will not be properly recycled, leading to memory leaks.
         */
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.scrollToPosition(currentPosition[0]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_thread_list, container, false);

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
        typedArray.recycle();

        colorFilterIcon = new PorterDuffColorFilter(colorIconFilter,
                PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, FragmentSearch.newInstance(true),
                                FragmentSearch.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        });
        if (getFragmentManager().getBackStackEntryCount() <= 1) {
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
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterIcon);
        setUpOptionsMenu();

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        layoutActions = (LinearLayout) view.findViewById(R.id.layout_actions);
        buttonExpandActions = (FloatingActionButton) view.findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLayoutActions();
            }
        });

        FloatingActionButton.Behavior behaviorFloatingActionButton = new ScrollAwareFloatingActionButtonBehavior(
                activity, null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                        hideLayoutActions(0);
                    }

                    @Override
                    public void onEndHideFromScroll() {
                        buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                        buttonExpandActions.setColorFilter(colorFilterIcon);
                    }

                });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams())
                .setBehavior(behaviorFloatingActionButton);


        buttonJumpTop = (FloatingActionButton) view.findViewById(R.id.button_jump_top);
        buttonJumpTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollToPositionWithOffset(0, 0);
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

        buttonClearViewed = (FloatingActionButton) view.findViewById(R.id.button_clear_viewed);
        buttonClearViewed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.getControllerLinks().clearViewed(Historian.getInstance(activity));
            }
        });
        buttonClearViewed.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity,
                        getString(R.string.content_description_button_clear_viewed),
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

            LinearLayout.LayoutParams layoutParamsClearViewed = (LinearLayout.LayoutParams) buttonClearViewed
                    .getLayoutParams();
            layoutParamsClearViewed.setMargins(0, 0, 0, 0);
            buttonClearViewed.setLayoutParams(layoutParamsClearViewed);

            RelativeLayout.LayoutParams layoutParamsActions = (RelativeLayout.LayoutParams) layoutActions
                    .getLayoutParams();
            layoutParamsActions.setMarginStart(margin);
            layoutParamsActions.setMarginEnd(margin);
            layoutActions.setLayoutParams(layoutParamsActions);
        }

        buttonExpandActions.setColorFilter(colorFilterIcon);
        buttonJumpTop.setColorFilter(colorFilterIcon);
        buttonClearViewed.setColorFilter(colorFilterIcon);

        swipeRefreshThreadList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_thread_list);
        swipeRefreshThreadList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerLinks()
                        .reloadAllLinks();
            }
        });
        if (adapterLinkList == null) {
            adapterLinkList = new AdapterLinkList(activity, mListener.getControllerLinks(),
                    mListener.getControllerUser(),
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }
        if (adapterLinkGrid == null) {
            adapterLinkGrid = new AdapterLinkGrid(activity, mListener.getControllerLinks(),
                    mListener.getControllerUser(),
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }

        if (AppSettings.MODE_LIST.equals(preferences.getString(AppSettings.INTERFACE_MODE,
                AppSettings.MODE_GRID))) {
            adapterLink = adapterLinkList;
        }
        else {
            adapterLink = adapterLinkGrid;
        }

        adapterLinkList.setActivity(activity);
        adapterLinkGrid.setActivity(activity);

        layoutManager = adapterLink.getLayoutManager();

        recyclerThreadList = (RecyclerView) view.findViewById(R.id.recycler_thread_list);
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setItemAnimator(null);

        recyclerThreadList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        Picasso.with(activity).resumeTag(AdapterLink.TAG_PICASSO);
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        Picasso.with(activity).pauseTag(AdapterLink.TAG_PICASSO);
                        break;
                }
            }
        });

        itemTouchHelper = new CustomItemTouchHelper(
                new CustomItemTouchHelper.SimpleCallback(activity,
                        R.drawable.ic_visibility_off_white_24dp,
                        ItemTouchHelper.START | ItemTouchHelper.END,
                        ItemTouchHelper.START | ItemTouchHelper.END) {

                    @Override
                    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {

                        if (layoutManager instanceof StaggeredGridLayoutManager) {
                            return 1f / ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
                        }

                        return 0.5f;
                    }

                    @Override
                    public int getSwipeDirs(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder) {

                        if (viewHolder.getAdapterPosition() == 0) {
                            return 0;
                        }

                        ViewGroup.LayoutParams layoutParams = viewHolder.itemView.getLayoutParams();

                        if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams &&
                                !((StaggeredGridLayoutManager.LayoutParams) layoutParams)
                                        .isFullSpan()) {

                            int spanCount = layoutManager instanceof StaggeredGridLayoutManager ?
                                    ((StaggeredGridLayoutManager) layoutManager).getSpanCount() : 2;
                            int spanIndex = ((StaggeredGridLayoutManager.LayoutParams) layoutParams)
                                    .getSpanIndex() % spanCount;
                            if (spanIndex == 0) {
                                return ItemTouchHelper.END;
                            }
                            else if (spanIndex == spanCount - 1) {
                                return ItemTouchHelper.START;
                            }

                        }

                        return super.getSwipeDirs(recyclerView, viewHolder);
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return false;
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                        // Offset by 1 due to subreddit header
                        final int position = viewHolder.getAdapterPosition() - 1;
                        final Link link = mListener.getControllerLinks().remove(position);
                        mListener.getEventListenerBase().hide(link);

                        if (snackbar != null) {
                            snackbar.dismiss();
                        }
                        snackbar = Snackbar.make(recyclerThreadList,
                                link.isHidden() ? R.string.link_hidden : R.string.link_shown,
                                Snackbar.LENGTH_LONG)
                                .setActionTextColor(getResources().getColor(R.color.colorAccent))
                                .setAction(
                                        R.string.undo, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mListener.getEventListenerBase().hide(link);
                                                mListener.getControllerLinks().add(position, link);
                                                recyclerThreadList.invalidate();
                                            }
                                        });
                        snackbar.getView()
                                .setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        snackbar.show();
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerThreadList);

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerThreadList.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerThreadList.setPadding(padding, 0, padding, 0);
        }

        drawerLayout = (DrawerLayout) view.findViewById(R.id.drawer_layout);

        textSidebar = (TextView) view.findViewById(R.id.text_sidebar);
        textSidebar.setMovementMethod(LinkMovementMethod.getInstance());

        buttonSubscribe = (Button) view.findViewById(R.id.button_subscribe);
        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSubscribe.setText(
                        mListener.getControllerLinks().getSubreddit().isUserIsSubscriber() ?
                                R.string.subscribe : R.string.unsubscribe);
                mListener.getControllerLinks().subscribe();
                if (mListener.getControllerLinks().getSubreddit().isUserIsSubscriber()) {
                    mListener.getControllerSearch()
                            .addSubreddit(mListener.getControllerLinks().getSubreddit());
                }
            }
        });

        textEmpty = (TextView) view.findViewById(R.id.text_empty);

        return view;
    }

    private void toggleLayoutActions() {
        if (buttonJumpTop.isShown()) {
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
                    buttonExpandActions.setColorFilter(colorFilterIcon);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
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
        super.onDetach();
        mListener = null;
        activity = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.getControllerLinks()
                .addListener(listener);
    }

    @Override
    public void onPause() {
        mListener.getControllerLinks()
                .removeListener(listener);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        item.setChecked(true);
        switch (item.getItemId()) {
            case R.id.item_search:
                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, FragmentSearch.newInstance(false),
                                FragmentSearch.TAG)
                        .addToBackStack(null)
                        .commit();
                return true;
            case R.id.item_interface:
                if (AppSettings.MODE_LIST.equals(
                        preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID))) {
                    resetAdapter(adapterLinkGrid);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_list_white_24dp));
                    item.getIcon().setColorFilter(colorFilterIcon);
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .commit();
                }
                else {
                    resetAdapter(adapterLinkList);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    item.getIcon().setColorFilter(colorFilterIcon);
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)
                            .commit();
                }
                return true;

        }


        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            mListener.getControllerLinks()
                    .setSort(sort);
            scrollToPositionWithOffset(0, 0);
            return true;
        }

        Time time = Time.fromMenuId(item.getItemId());
        if (time != null) {
            mListener.getControllerLinks()
                    .setTime(time);
            itemSortTime.setTitle(
                    getString(R.string.time) + Reddit.TIME_SEPARATOR + item.toString());
            scrollToPositionWithOffset(0, 0);
            return true;
        }

        return false;
    }

    /**
     * Helper method to scroll without if statement sprinkled everywhere, as
     * scrollToPositionWithOffset is not abstracted into the upper LayoutManager
     * for some reason
     *
     * @param position to scroll to
     * @param offset   from top of view
     */
    private void scrollToPositionWithOffset(int position, int offset) {
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager)
                    .scrollToPositionWithOffset(position, offset);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterLink.pauseViewHolders();
        }
    }

    @Override
    public boolean navigateBack() {
        return adapterLink.navigateBack();
    }

    @Override
    public void onShown() {
        adapterLink.setVisibility(View.VISIBLE);
        ViewCompat.animate(buttonExpandActions)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(DURATION_TRANSITION)
                .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                .setListener(null)
                .start();
    }

    @Override
    public void onWindowTransitionStart() {
        super.onWindowTransitionStart();
        ViewCompat.animate(buttonExpandActions)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(DURATION_TRANSITION)
                .setInterpolator(ScrollAwareFloatingActionButtonBehavior.INTERPOLATOR)
                .setListener(null)
                .start();
    }
}