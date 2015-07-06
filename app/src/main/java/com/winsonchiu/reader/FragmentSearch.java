/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

public class FragmentSearch extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    private static final int PAGE_COUNT = 3;
    public static final String TAG = FragmentSearch.class.getCanonicalName();
    private static final String ARG_HIDE_KEYBOARD = "hideKeyboard";

    private FragmentListenerBase mListener;

    private Activity activity;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private RecyclerView recyclerSearchSubreddits;
    private RecyclerView recyclerSearchLinks;
    private RecyclerView recyclerSearchLinksSubreddit;
    private LinearLayoutManager layoutManagerSubreddits;
    private LinearLayoutManager layoutManagerLinks;
    private LinearLayoutManager layoutManagerLinksSubreddit;
    private AdapterSearchSubreddits adapterSearchSubreddits;
    private AdapterLink adapterLinks;
    private AdapterLink adapterLinksSubreddit;
    private ControllerSearch.Listener listenerSearch;
    private PagerAdapter pagerAdapter;
    private Menu menu;
    private MenuItem itemSearch;
    private MenuItem itemSortTime;
    private Toolbar toolbar;

    public static FragmentSearch newInstance(boolean hideKeyboard) {
        FragmentSearch fragment = new FragmentSearch();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HIDE_KEYBOARD, hideKeyboard);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentSearch() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_search);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        itemSortTime = menu.findItem(R.id.item_sort_time);

        itemSearch = menu.findItem(R.id.item_search);
        itemSearch.expandActionView();

        final SearchView searchView = (SearchView) itemSearch.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (mListener.getControllerSearch().getCurrentPage() == ControllerSearch.PAGE_SUBREDDITS) {
                    mListener.getControllerLinks()
                            .setParameters(query.replaceAll("\\s", ""), Sort.HOT, Time.ALL);
                    getFragmentManager().popBackStack();
                }
                else {
                    mListener.getControllerSearch()
                            .setQuery(query);
                    mListener.getControllerSearch().reloadCurrentPage();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!isAdded() || mListener == null) {
                    return false;
                }
                Log.d(TAG, "newText: " + newText);

                mListener.getControllerSearch()
                        .setQuery(newText);
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);
        if (mListener.getControllerLinks().sizeLinks() == 0) {
            searchView.setQuery(mListener.getControllerSearch().getQuery(), false);
        }

        menu.findItem(R.id.item_sort_relevance)
                .setChecked(true);
        menu.findItem(R.id.item_sort_all)
                .setChecked(true);
        itemSortTime.setTitle(
                getString(R.string.time) + Reddit.TIME_SEPARATOR + getString(
                        R.string.item_sort_all));

        if (getArguments().getBoolean(ARG_HIDE_KEYBOARD)) {
            searchView.clearFocus();
        }

    }

    @Override
    public void onDestroyOptionsMenu() {
        SearchView searchView = (SearchView) itemSearch.getActionView();
        searchView.setOnQueryTextListener(null);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_search, container, false);

        listenerSearch = new ControllerSearch.Listener() {

            @Override
            public AdapterSearchSubreddits getAdapterSearchSubreddits() {
                return adapterSearchSubreddits;
            }

            @Override
            public AdapterLink getAdapterLinks() {
                return adapterLinks;
            }

            @Override
            public AdapterLink getAdapterLinksSubreddit() {
                return adapterLinksSubreddit;
            }

            @Override
            public RecyclerView.Adapter getAdapter() {
                return null;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {

            }

            @Override
            public void post(Runnable runnable) {
                view.post(runnable);
            }

            @Override
            public void setSort(Sort sort) {
                menu.findItem(sort.getMenuId()).setChecked(true);
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

        adapterSearchSubreddits = new AdapterSearchSubreddits(activity,
                mListener.getControllerSearch(),
                new AdapterSearchSubreddits.ViewHolder.EventListener() {
                    @Override
                    public void onClickSubreddit(Subreddit subreddit) {
                        mListener.getControllerLinks()
                                .setParameters(subreddit.getDisplayName(), Sort.HOT, Time.ALL);
                        InputMethodManager inputManager = (InputMethodManager) activity
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);
                        getFragmentManager().popBackStack();
                    }
                });

        layoutManagerSubreddits = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchSubreddits = (RecyclerView) view.findViewById(
                R.id.recycler_search_subreddits);
        recyclerSearchSubreddits.setLayoutManager(layoutManagerSubreddits);
        recyclerSearchSubreddits.setAdapter(adapterSearchSubreddits);

        DisallowListener disallowListener = new DisallowListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerSearchLinks.requestDisallowInterceptTouchEvent(disallow);
                recyclerSearchLinksSubreddit.requestDisallowInterceptTouchEvent(disallow);
                viewPager.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        adapterLinks = new AdapterSearchLinkList(activity, new ControllerLinksBase() {
            @Override
            public Link getLink(int position) {
                return mListener.getControllerSearch()
                        .getLink(position);
            }

            @Override
            public int sizeLinks() {
                return mListener.getControllerSearch()
                        .sizeLinks();
            }

            @Override
            public boolean isLoading() {
                return mListener.getControllerSearch()
                        .isLoadingLinks();
            }

            @Override
            public void loadMoreLinks() {
                mListener.getControllerSearch()
                        .loadMoreLinks();
            }

            @Override
            public Subreddit getSubreddit() {
                return new Subreddit();
            }

            @Override
            public boolean showSubreddit() {
                return true;
            }

            @Override
            public Link remove(int position) {
                return null;
            }

        }, mListener.getControllerUser(),
                new AdapterLink.ViewHolderHeader.EventListener() {
                    @Override
                    public void onClickSubmit(String postType) {

                    }

                    @Override
                    public void showSidebar() {

                    }
                }, mListener.getEventListenerBase(), disallowListener,
                new RecyclerCallback() {
                    @Override
                    public void scrollTo(int position) {
                        layoutManagerLinks.scrollToPositionWithOffset(position, 0);
                    }

                    @Override
                    public int getRecyclerHeight() {
                        return recyclerSearchLinks.getHeight();
                    }

                    @Override
                    public RecyclerView.LayoutManager getLayoutManager() {
                        return layoutManagerLinks;
                    }

                });

        adapterLinksSubreddit = new AdapterSearchLinkList(activity, new ControllerLinksBase() {
            @Override
            public Link getLink(int position) {
                return mListener.getControllerSearch()
                        .getLinkSubreddit(position);
            }

            @Override
            public int sizeLinks() {
                return mListener.getControllerSearch()
                        .sizeLinksSubreddit();
            }

            @Override
            public boolean isLoading() {
                return mListener.getControllerSearch()
                        .isLoadingLinksSubreddit();
            }

            @Override
            public void loadMoreLinks() {
                mListener.getControllerSearch()
                        .loadMoreLinksSubreddit();
            }

            @Override
            public Subreddit getSubreddit() {
                return new Subreddit();
            }

            @Override
            public boolean showSubreddit() {
                return true;
            }

            @Override
            public Link remove(int position) {
                return null;
            }

        }, mListener.getControllerUser(),
                new AdapterLink.ViewHolderHeader.EventListener() {
                    @Override
                    public void onClickSubmit(String postType) {

                    }

                    @Override
                    public void showSidebar() {

                    }
                }, mListener.getEventListenerBase(), disallowListener,
                new RecyclerCallback() {
                    @Override
                    public void scrollTo(int position) {
                        layoutManagerLinksSubreddit.scrollToPositionWithOffset(position, 0);
                    }

                    @Override
                    public int getRecyclerHeight() {
                        return recyclerSearchLinksSubreddit.getHeight();
                    }

                    @Override
                    public RecyclerView.LayoutManager getLayoutManager() {
                        return layoutManagerLinksSubreddit;
                    }

                });

        layoutManagerLinks = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchLinks = (RecyclerView) view.findViewById(R.id.recycler_search_links);
        recyclerSearchLinks.setLayoutManager(layoutManagerLinks);
        recyclerSearchLinks.setAdapter(adapterLinks);

        layoutManagerLinksSubreddit = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchLinksSubreddit = (RecyclerView) view.findViewById(
                R.id.recycler_search_links_subreddit);
        recyclerSearchLinksSubreddit.setLayoutManager(layoutManagerLinksSubreddit);
        recyclerSearchLinksSubreddit.setAdapter(adapterLinksSubreddit);

        pagerAdapter = new PagerAdapter() {
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                return viewPager.getChildAt(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                // No need to destroy the RecyclerViews since they'll be reused for a new query
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case ControllerSearch.PAGE_SUBREDDITS:
                        return getString(R.string.subreddit);
                    case ControllerSearch.PAGE_LINKS:
                        return getString(R.string.all);
                    case ControllerSearch.PAGE_LINKS_SUBREDDIT:
                        return mListener.getControllerLinks()
                                .getSubredditName();
                }

                return super.getPageTitle(position);
            }

            @Override
            public int getCount() {
                return PAGE_COUNT;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        };

        tabLayout = (TabLayout) view.findViewById(R.id.tab_search);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_search);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position,
                    float positionOffset,
                    int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mListener.getControllerSearch()
                        .setCurrentPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.setupWithViewPager(viewPager);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerSearch()
                .addListener(listenerSearch);
    }

    @Override
    public void onStop() {
        mListener.getControllerSearch()
                .removeListener(listenerSearch);
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        mListener.getControllerLinks()
                .setTitle();
        activity = null;
        mListener = null;
        super.onDetach();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        item.setChecked(true);

        for (Sort sort : Sort.values()) {
            if (sort.getMenuId() == item.getItemId()) {
                mListener.getControllerSearch()
                        .setSort(sort);
                flashSearchView();
                return true;
            }
        }

        for (Time time : Time.values()) {
            if (time.getMenuId() == item.getItemId()) {
                mListener.getControllerSearch()
                        .setTime(time);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + item.toString());
                flashSearchView();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    @Override
    boolean navigateBack() {
        return true;
    }
}
