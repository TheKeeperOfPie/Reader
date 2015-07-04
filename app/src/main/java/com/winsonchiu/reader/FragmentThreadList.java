/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

import org.json.JSONException;
import org.json.JSONObject;

public class FragmentThreadList extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentThreadList.class.getCanonicalName();

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
    private AdapterLink.ViewHolderHeader.EventListener eventListenerHeader;
    private DisallowListener disallowListener;
    private RecyclerCallback recyclerCallback;
    private ControllerLinks.Listener listener;

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

        eventListenerHeader = new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(String postType) {

                if (TextUtils.isEmpty(mListener.getControllerUser().getUser().getName())) {
                    Toast.makeText(activity, getString(R.string.must_be_logged_in),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(activity, ActivityNewPost.class);
                intent.putExtra(ActivityNewPost.USER,
                        mListener.getControllerUser().getUser().getName());
                intent.putExtra(ActivityNewPost.SUBREDDIT,
                        mListener.getControllerLinks().getSubreddit().getUrl());
                intent.putExtra(ActivityNewPost.POST_TYPE, postType);
                intent.putExtra(ActivityNewPost.SUBMIT_TEXT_HTML,
                        mListener.getControllerLinks().getSubreddit().getSubmitTextHtml());
                startActivityForResult(intent, ActivityNewPost.REQUEST_CODE);
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
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        recyclerCallback = new RecyclerCallback() {
            @Override
            public void scrollTo(final int position) {
                recyclerThreadList.requestLayout();
                recyclerThreadList.post(new Runnable() {
                    @Override
                    public void run() {
                        if (layoutManager instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(
                                    position, 0);
                        }
                        else if (layoutManager instanceof StaggeredGridLayoutManager) {
                            ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(
                                    position, 0);
                        }
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

        };

        listener = new ControllerLinks.Listener() {
            @Override
            public void setSort(Sort sort) {
                menu.findItem(sort.getMenuId()).setChecked(true);
            }

            @Override
            public void showEmptyView(boolean isEmpty) {
                textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void loadSideBar(Subreddit subreddit) {
                if (subreddit.getUrl().equals("/") || "/r/all/"
                        .equalsIgnoreCase(subreddit.getUrl())) {
                    return;
                }

                mListener.getControllerLinks()
                        .getReddit()
                        .loadGet(
                                Reddit.OAUTH_URL + subreddit.getUrl() + "about",
                                new Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Subreddit loadedSubreddit = Subreddit.fromJson(
                                                    new JSONObject(response));
                                            textSidebar.setText(Reddit.getTrimmedHtml(
                                                    loadedSubreddit.getDescriptionHtml()));
                                            drawerLayout.setDrawerLockMode(
                                                    DrawerLayout.LOCK_MODE_UNLOCKED);
                                            if (mListener.getControllerLinks().getSubreddit()
                                                    .isUserIsSubscriber()) {
                                                buttonSubscribe.setText(R.string.unsubscribe);
                                            }
                                            else {
                                                buttonSubscribe.setText(R.string.subscribe);
                                            }
                                            buttonSubscribe.setVisibility(TextUtils.isEmpty(
                                                    preferences.getString(AppSettings.ACCOUNT_JSON,
                                                            "")) ? View.GONE : View.VISIBLE);
                                        }
                                        catch (JSONException e) {
                                            textSidebar.setText(null);
                                            drawerLayout.setDrawerLockMode(
                                                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        textSidebar.setText(null);
                                        drawerLayout.setDrawerLockMode(
                                                DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                                    }
                                }, 0);
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

        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        itemSearch.collapseActionView();
                        return false;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        return true;
                    }
                });

        menu.findItem(mListener.getControllerLinks().getSort().getMenuId()).setChecked(true);
        itemSortTime.setTitle(
                getString(R.string.time) + Reddit.TIME_SEPARATOR + mListener.getControllerLinks().getTime().toString());

    }

    private void resetSubmenuSelected() {
        onMenuItemClick(menu.findItem(mListener.getControllerLinks()
                .getSort()
                .getMenuId()));
        onMenuItemClick(menu.findItem(mListener.getControllerLinks()
                .getTime()
                .getMenuId()));

    }

    @Override
    public void onDestroyOptionsMenu() {
        MenuItemCompat.setOnActionExpandListener(itemSearch, null);
        itemSearch = null;
        super.onDestroyOptionsMenu();
    }

    /*
        Workaround for Android's drag-to-select menu bug, where the
        menu becomes unusable after a drag gesture
     */
    private void flashSearchView() {
        if (itemSearch != null) {
            itemSearch.expandActionView();
            itemSearch.collapseActionView();
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

        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.scrollToPosition(currentPosition[0]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_thread_list, container, false);

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
        setUpOptionsMenu();

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
        recyclerThreadList.setHasFixedSize(true);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setItemAnimator(null);

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
            }
        });

        textEmpty = (TextView) view.findViewById(R.id.text_empty);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            mListener.getControllerLinks().reloadAllLinks();
        }

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
    public void onStart() {
        super.onStart();
        mListener.getControllerLinks()
                .addListener(listener);
    }

    @Override
    public void onStop() {
        mListener.getControllerLinks()
                .removeListener(listener);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeRefreshThreadList.setRefreshing(mListener.getControllerLinks()
                .isLoading());
        mListener.getControllerLinks()
                .setTitle();
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
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .commit();
                }
                else {
                    resetAdapter(adapterLinkList);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)
                            .commit();
                }
                return true;

        }

        for (Sort sort : Sort.values()) {
            if (sort.getMenuId() == item.getItemId()) {
                mListener.getControllerLinks()
                        .setSort(sort);
                flashSearchView();

                if (layoutManager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
                }
                else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
                }
                return true;
            }
        }

        for (Time time : Time.values()) {
            if (time.getMenuId() == item.getItemId()) {
                mListener.getControllerLinks()
                        .setTime(time);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + item.toString());
                flashSearchView();

                if (layoutManager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
                }
                else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterLink.pauseViewHolders();
        }
    }

    @Override
    boolean navigateBack() {
        return true;
    }

    @Override
    public void onShown() {
        adapterLink.setVisibility(View.VISIBLE);
    }
}