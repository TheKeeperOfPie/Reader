/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.Theme;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.SimpleCallbackBackground;
import com.winsonchiu.reader.utils.UtilsColor;

public class FragmentSearch extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentSearch.class.getCanonicalName();
    private static final String ARG_HIDE_KEYBOARD = "hideKeyboard";

    private FragmentListenerBase mListener;

    private Activity activity;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private RecyclerView recyclerSearchSubreddits;
    private RecyclerView recyclerSearchLinks;
    private RecyclerView recyclerSearchLinksSubreddit;
    private RecyclerView recyclerSearchSubredditsRecommended;
    private LinearLayoutManager layoutManagerSubreddits;
    private LinearLayoutManager layoutManagerLinks;
    private LinearLayoutManager layoutManagerLinksSubreddit;
    private LinearLayoutManager layoutManagerSubredditsRecommended;
    private AdapterSearchSubreddits adapterSearchSubreddits;
    private AdapterLink adapterLinks;
    private AdapterLink adapterLinksSubreddit;
    private AdapterSearchSubreddits adapterSearchSubredditsRecommended;
    private ControllerSearch.Listener listenerSearch;
    private PagerAdapter pagerAdapter;
    private Menu menu;
    private MenuItem itemSearch;
    private MenuItem itemSortTime;
    private Toolbar toolbar;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private View view;
    private CustomColorFilter colorFilterPrimary;
    private ItemTouchHelper itemTouchHelperSubreddits;

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

    private void setUpToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });
        toolbar.inflateMenu(R.menu.menu_search);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        itemSortTime = menu.findItem(R.id.item_sort_time);

        itemSearch = menu.findItem(R.id.item_search);
        itemSearch.expandActionView();

        final SearchView searchView = (SearchView) itemSearch.getActionView();

        View view = searchView.findViewById(android.support.v7.appcompat.R.id.search_go_btn);
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(colorFilterPrimary);
        }
        view = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        if (view instanceof EditText) {
            ((EditText) view).setTextColor(colorFilterPrimary.getColor());
            ((EditText) view).setHintTextColor(colorFilterPrimary.getColor());
        }

        MenuItemCompat.setOnActionExpandListener(itemSearch, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (viewPager.getCurrentItem() == ControllerSearch.PAGE_SUBREDDITS) {
                    mListener.getControllerLinks()
                            .setParameters(query.replaceAll("\\s", ""), Sort.HOT, Time.ALL);
                    closeKeyboard();
                    mListener.onNavigationBackClick();
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

                mListener.getControllerSearch()
                        .setQuery(newText);
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);
        searchView.setQuery(mListener.getControllerSearch().getQuery(), false);

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

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }

    }

    @Override
    public void onDestroyOptionsMenu() {
        if (itemSearch != null) {
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            MenuItemCompat.setOnActionExpandListener(itemSearch, null);
            itemSearch = null;
        }
        super.onDestroyOptionsMenu();
    }

    /*
        Workaround for Android's drag-to-select menu bug, where the
        menu becomes unusable after a drag gesture
     */
    private void flashSearchView() {
        // Removed as this clears the search query
        // TODO: Find fix
//        if (itemSearch != null) {
//            itemSearch.expandActionView();
//            itemSearch.collapseActionView();
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search, container, false);

        listenerSearch = new ControllerSearch.Listener() {

            @Override
            public AdapterSearchSubreddits getAdapterSearchSubreddits() {
                return adapterSearchSubreddits;
            }

            @Override
            public AdapterSearchSubreddits getAdapterSearchSubredditsRecommended() {
                return adapterSearchSubredditsRecommended;
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
            public void setSortAndTime(Sort sort, Time time) {
                menu.findItem(sort.getMenuId()).setChecked(true);
                menu.findItem(time.getMenuId()).setChecked(true);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + menu
                                .findItem(time.getMenuId()).toString());
            }

            @Override
            public void setRefreshing(boolean refreshing) {

            }

            @Override
            public void post(Runnable runnable) {
                view.post(runnable);
            }

            @Override
            public void scrollToLinks(int position) {
                layoutManagerLinks.scrollToPositionWithOffset(0, 0);
            }

            @Override
            public void scrollToLinksSubreddit(int position) {
                layoutManagerLinksSubreddit.scrollToPositionWithOffset(0, 0);
            }
        };

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

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, android.R.attr.windowBackground});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        final int windowBackground = typedArray.getColor(1, getResources().getColor(R.color.darkThemeBackground));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new CustomColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        int styleToolbar = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? mListener.getAppColorTheme().getStyle(AppSettings.THEME_DARK, mListener.getThemeAccentPrefString()) : mListener.getAppColorTheme().getStyle(AppSettings.THEME_LIGHT, mListener.getThemeAccentPrefString());

        int styleColorBackground = AppSettings.THEME_DARK.equals(mListener.getThemeBackgroundPrefString()) ? R.style.MenuDark : R.style.MenuLight;

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ContextThemeWrapper(activity, styleToolbar), styleColorBackground);

        toolbar = (Toolbar) activity.getLayoutInflater().cloneInContext(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);
        layoutAppBar.addView(toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        setUpToolbar();

        adapterSearchSubreddits = new AdapterSearchSubreddits(activity,
                new ControllerSearchBase() {
                    @Override
                    public Subreddit getSubreddit(int position) {
                        return mListener.getControllerSearch().getSubreddit(position);
                    }

                    @Override
                    public int getSubredditCount() {
                        return mListener.getControllerSearch().getCountSubreddit();
                    }
                },
                new AdapterSearchSubreddits.ViewHolder.EventListener() {
                    @Override
                    public void onClickSubreddit(Subreddit subreddit) {
                        mListener.getControllerSearch().addViewedSubreddit(subreddit);
                        mListener.getControllerLinks()
                                .setParameters(subreddit.getDisplayName(), Sort.HOT, Time.ALL);
                        closeKeyboard();
                        mListener.onNavigationBackClick();
                    }

                    @Override
                    public boolean supportsDrag() {
                        return true;
                    }

                    @Override
                    public void onStartDrag(AdapterSearchSubreddits.ViewHolder viewHolder) {
                        itemTouchHelperSubreddits.startDrag(viewHolder);
                    }

                    @Override
                    public void sendToTop(AdapterSearchSubreddits.ViewHolder viewHolder) {
                        mListener.getControllerSearch().moveSubreddit(viewHolder.getAdapterPosition(), 0);
                        recyclerSearchSubreddits.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (layoutManagerSubreddits.findFirstVisibleItemPosition() <= 1) {
                                    layoutManagerSubreddits.smoothScrollToPosition(recyclerSearchSubreddits, null, 0);
                                }
                            }
                        }, 150);
                    }

                    @Override
                    public boolean isSubscriptionListShown() {
                        return mListener.getControllerSearch().isSubscriptionListShown();
                    }
                });

        layoutManagerSubreddits = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchSubreddits = (RecyclerView) view.findViewById(
                R.id.recycler_search_subreddits);
        recyclerSearchSubreddits.setLayoutManager(layoutManagerSubreddits);
        recyclerSearchSubreddits.setAdapter(adapterSearchSubreddits);
        recyclerSearchSubreddits.addItemDecoration(new ItemDecorationDivider(activity, ItemDecorationDivider.VERTICAL_LIST));

        itemTouchHelperSubreddits = new ItemTouchHelper(new SimpleCallbackBackground(0, 0, windowBackground) {

            @Override
            public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (mListener.getControllerSearch().isSubscriptionListShown()) {
                    return ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                }

                return super.getDragDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                mListener.getControllerSearch().moveSubreddit(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }
        });
        itemTouchHelperSubreddits.attachToRecyclerView(recyclerSearchSubreddits);

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
            public boolean setReplyText(String name, String text, boolean collapsed) {
                return mListener.getControllerSearch().setReplyTextLinks(name, text, collapsed);
            }

            @Override
            public void setNsfw(String name, boolean over18) {
                mListener.getControllerSearch().setNsfwLinks(name, over18);
            }

        }, new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(String postType) {

            }

            @Override
            public void showSidebar() {

            }
        }, mListener.getEventListenerBase(), disallowListener,
                new RecyclerCallback() {
                    @Override
                    public void scrollTo(final int position) {
                        recyclerSearchLinks.requestLayout();
                        recyclerSearchLinks.post(new Runnable() {
                            @Override
                            public void run() {
                                RecyclerView.ViewHolder viewHolder = recyclerSearchLinks.findViewHolderForAdapterPosition(position);
                                int offset = 0;
                                if (viewHolder != null) {
                                    int difference = recyclerSearchLinks.getHeight() - viewHolder.itemView.getHeight();
                                    if (difference > 0) {
                                        offset = difference / 2;
                                    }
                                }
                                layoutManagerLinks.scrollToPositionWithOffset(position, offset);
                            }
                        });
                    }

                    @Override
                    public int getRecyclerHeight() {
                        return recyclerSearchLinks.getHeight();
                    }

                    @Override
                    public RecyclerView.LayoutManager getLayoutManager() {
                        return layoutManagerLinks;
                    }

                    @Override
                    public void hideToolbar() {
                        AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                        behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000,
                                true);
                    }

                    @Override
                    public void onReplyShown() {

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
            public boolean setReplyText(String name, String text, boolean collapsed) {
                return mListener.getControllerSearch().setReplyTextLinksSubreddit(name, text,
                        collapsed);
            }

            @Override
            public void setNsfw(String name, boolean over18) {
                mListener.getControllerSearch().setNsfwLinksSubreddit(name, over18);
            }

        }, new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(String postType) {

            }

            @Override
            public void showSidebar() {

            }
        }, mListener.getEventListenerBase(), disallowListener,
                new RecyclerCallback() {
                    @Override
                    public void scrollTo(final int position) {
                        recyclerSearchLinksSubreddit.requestLayout();
                        recyclerSearchLinksSubreddit.post(new Runnable() {
                            @Override
                            public void run() {
                                RecyclerView.ViewHolder viewHolder = recyclerSearchLinksSubreddit.findViewHolderForAdapterPosition(position);
                                int offset = 0;
                                if (viewHolder != null) {
                                    int difference = recyclerSearchLinksSubreddit.getHeight() - viewHolder.itemView.getHeight();
                                    if (difference > 0) {
                                        offset = difference / 2;
                                    }
                                }
                                layoutManagerLinksSubreddit.scrollToPositionWithOffset(position, offset);
                            }
                        });
                    }

                    @Override
                    public int getRecyclerHeight() {
                        return recyclerSearchLinksSubreddit.getHeight();
                    }

                    @Override
                    public RecyclerView.LayoutManager getLayoutManager() {
                        return layoutManagerLinksSubreddit;
                    }

                    @Override
                    public void hideToolbar() {
                        AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                        behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
                    }

                    @Override
                    public void onReplyShown() {

                    }

                });

        layoutManagerLinks = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchLinks = (RecyclerView) view.findViewById(R.id.recycler_search_links);
        recyclerSearchLinks.setLayoutManager(layoutManagerLinks);
        recyclerSearchLinks.setAdapter(adapterLinks);
        recyclerSearchLinks.addItemDecoration(new ItemDecorationDivider(activity, ItemDecorationDivider.VERTICAL_LIST));

        layoutManagerLinksSubreddit = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchLinksSubreddit = (RecyclerView) view.findViewById(
                R.id.recycler_search_links_subreddit);
        recyclerSearchLinksSubreddit.setLayoutManager(layoutManagerLinksSubreddit);
        recyclerSearchLinksSubreddit.setAdapter(adapterLinksSubreddit);
        recyclerSearchLinksSubreddit.addItemDecoration(new ItemDecorationDivider(activity, ItemDecorationDivider.VERTICAL_LIST));

        adapterSearchSubredditsRecommended = new AdapterSearchSubreddits(activity,
                new ControllerSearchBase() {
                    @Override
                    public Subreddit getSubreddit(int position) {
                        return mListener.getControllerSearch().getSubredditRecommended(position);
                    }

                    @Override
                    public int getSubredditCount() {
                        return mListener.getControllerSearch().getCountSubredditRecommended();
                    }
                },
                new AdapterSearchSubreddits.ViewHolder.EventListener() {
                    @Override
                    public void onClickSubreddit(Subreddit subreddit) {
                        mListener.getControllerLinks()
                                .setParameters(subreddit.getDisplayName(), Sort.HOT, Time.ALL);
                        closeKeyboard();
                        mListener.onNavigationBackClick();
                    }

                    @Override
                    public boolean supportsDrag() {
                        return false;
                    }

                    @Override
                    public void onStartDrag(AdapterSearchSubreddits.ViewHolder viewHolder) {

                    }

                    @Override
                    public void sendToTop(AdapterSearchSubreddits.ViewHolder viewHolder) {

                    }

                    @Override
                    public boolean isSubscriptionListShown() {
                        return mListener.getControllerSearch().isSubscriptionListShown();
                    }
                });

        layoutManagerSubredditsRecommended = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerSearchSubredditsRecommended = (RecyclerView) view.findViewById(
                R.id.recycler_search_subreddits_recommended);
        recyclerSearchSubredditsRecommended.setLayoutManager(layoutManagerSubredditsRecommended);
        recyclerSearchSubredditsRecommended.setAdapter(adapterSearchSubredditsRecommended);
        recyclerSearchSubredditsRecommended.addItemDecoration(new ItemDecorationDivider(activity, ItemDecorationDivider.VERTICAL_LIST));

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_search);
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
                boolean sortSubredditsShown = mListener.getControllerSearch().getCurrentPage() == ControllerSearch.PAGE_SUBREDDITS || mListener.getControllerSearch().getCurrentPage() == ControllerSearch.PAGE_SUBREDDITS_RECOMMENDED;

                menu.findItem(R.id.item_sort_subreddits).setEnabled(sortSubredditsShown);
                menu.findItem(R.id.item_sort_subreddits).setVisible(sortSubredditsShown);
                menu.findItem(R.id.item_sort).setEnabled(!sortSubredditsShown);
                menu.findItem(R.id.item_sort).setVisible(!sortSubredditsShown);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        final int count = mListener.getControllerLinks().isOnSpecificSubreddit() ? viewPager.getChildCount() : viewPager.getChildCount() - 1;

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
                    case ControllerSearch.PAGE_SUBREDDITS_RECOMMENDED:
                        return getString(R.string.recommended);
                }

                return super.getPageTitle(position);
            }

            @Override
            public int getCount() {
                return count;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        };

        viewPager.setAdapter(pagerAdapter);

        tabLayout = (TabLayout) view.findViewById(R.id.tab_search);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        return view;
    }

    private void closeKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.getControllerSearch()
                .addListener(listenerSearch);
    }

    @Override
    public void onPause() {
        mListener.getControllerSearch()
                .removeListener(listenerSearch);
        mListener.getControllerSearch().saveSubscriptions();
        super.onPause();
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

        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            mListener.getControllerSearch().setSort(sort);
            flashSearchView();
            return true;
        }

        Time time = Time.fromMenuId(item.getItemId());
        if (time != null) {
            mListener.getControllerSearch()
                    .setTime(time);
            flashSearchView();
            return true;
        }

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public boolean navigateBack() {
        return true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            switch (viewPager.getCurrentItem()) {
                case ControllerSearch.PAGE_LINKS:
                    adapterLinks.pauseViewHolders();
                    break;
                case ControllerSearch.PAGE_LINKS_SUBREDDIT:
                    adapterLinksSubreddit.pauseViewHolders();
                    break;
            }
            view.setVisibility(View.INVISIBLE);
        }
        else {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setVisibilityOfThing(int visibility, Thing thing) {
        super.setVisibilityOfThing(visibility, thing);
        switch (viewPager.getCurrentItem()) {
            case ControllerSearch.PAGE_LINKS:
                adapterLinks.setVisibility(visibility, thing);
                break;
            case ControllerSearch.PAGE_LINKS_SUBREDDIT:
                adapterLinksSubreddit.setVisibility(visibility, thing);
                break;
        }
    }

    @Override
    public void onShown() {
        switch (viewPager.getCurrentItem()) {
            case ControllerSearch.PAGE_LINKS:
                adapterLinks.setVisibility(View.VISIBLE);
                break;
            case ControllerSearch.PAGE_LINKS_SUBREDDIT:
                adapterLinksSubreddit.setVisibility(View.VISIBLE);
                break;
        }
    }

}
