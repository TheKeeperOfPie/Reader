/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.SimpleCallbackBackground;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsInput;
import com.winsonchiu.reader.utils.UtilsRx;
import com.winsonchiu.reader.utils.UtilsTheme;

import javax.inject.Inject;

import butterknife.BindView;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class FragmentSearch extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentSearch.class.getCanonicalName();
    private static final String ARG_HIDE_KEYBOARD = "hideKeyboard";

    private FragmentListenerBase mListener;

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
    private View view;
    private ItemTouchHelper itemTouchHelperSubreddits;

    private Subscription subscriptionLinks;
    private Subscription subscriptionLinksSubreddit;
    private Subscription subscriptionSort;
    private Subscription subscriptionTime;
    private Subscription subscriptionCurrentPage;

    @BindView(R.id.layout_coordinator)
    CoordinatorLayout layoutCoordinator;

    @BindView(R.id.layout_app_bar)
    AppBarLayout layoutAppBar;

    @BindView(R.id.recycler_search_subreddits)
    RecyclerView recyclerSearchSubreddits;

    @BindView(R.id.recycler_search_links)
    RecyclerView recyclerSearchLinks;

    @BindView(R.id.recycler_search_links_subreddit)
    RecyclerView recyclerSearchLinksSubreddit;

    @BindView(R.id.recycler_search_subreddits_recommended)
    RecyclerView recyclerSearchSubredditsRecommended;

    @BindView(R.id.pager_search)
    ViewPager pagerSearch;

    @BindView(R.id.tab_search)
    TabLayout layoutTabs;

    @Inject ControllerLinks controllerLinks;
    @Inject ControllerSearch controllerSearch;

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
        toolbar = UtilsTheme.generateToolbar(getContext(), layoutAppBar, themer, mListener);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(v -> mListener.onNavigationBackClick());
        toolbar.getNavigationIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
        toolbar.inflateMenu(R.menu.menu_search);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        itemSortTime = menu.findItem(R.id.item_sort_time);

        itemSearch = menu.findItem(R.id.item_search);
        itemSearch.expandActionView();

        SearchView viewSearch = (SearchView) itemSearch.getActionView();
        viewSearch.setIconified(false);
        viewSearch.requestFocus();

        View view = viewSearch.findViewById(android.support.v7.appcompat.R.id.search_go_btn);
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(themer.getColorFilterPrimary());
        }
        view = viewSearch.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        if (view instanceof EditText) {
            ((EditText) view).setTextColor(themer.getColorFilterPrimary().getColor());
            ((EditText) view).setHintTextColor(themer.getColorFilterPrimary().getColor());
        }

        MenuItemCompat.setOnActionExpandListener(itemSearch, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                toolbar.getNavigationIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });
        viewSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (pagerSearch.getCurrentItem() == ControllerSearch.PAGE_SUBREDDITS) {
                    controllerLinks.setParameters(query.replaceAll("\\s", ""), Sort.HOT, Time.ALL);
                    closeKeyboard();
                    mListener.onNavigationBackClick();
                }
                else {
                    controllerSearch.setQuery(query)
                            .subscribe(getReloadObserver());
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!isAdded() || mListener == null) {
                    return false;
                }

                controllerSearch.setQuery(newText)
                        .subscribe(getReloadObserver());
                return false;
            }
        });
        viewSearch.setSubmitButtonEnabled(true);
        viewSearch.setQuery(controllerSearch.getQuery(), false);

        menu.findItem(R.id.item_sort_relevance)
                .setChecked(true);
        menu.findItem(R.id.item_sort_all)
                .setChecked(true);
        itemSortTime.setTitle(getString(R.string.time_description, getString(R.string.item_sort_all)));

        if (getArguments().getBoolean(ARG_HIDE_KEYBOARD)) {
            viewSearch.clearFocus();
        }

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
        }

    }

    private Observer<Listing> getReloadObserver() {
        return new FinalizingSubscriber<Listing>() {
            @Override
            public void error(Throwable e) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.error_loading), Toast.LENGTH_SHORT)
                        .show();
            }
        };
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
    protected void inject() {
        ((ActivityMain) getActivity()).getComponentActivity().inject(this);
    }

    // TODO: Remove/fix ResourceType warning
    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = bind(inflater.inflate(R.layout.fragment_search, container, false));

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
            public void scrollToLinks(int position) {
                layoutManagerLinks.scrollToPositionWithOffset(0, 0);
            }

            @Override
            public void scrollToLinksSubreddit(int position) {
                layoutManagerLinksSubreddit.scrollToPositionWithOffset(0, 0);
            }
        };

        int windowBackground = UtilsTheme.getAttributeColor(getContext(), android.R.attr.windowBackground, 0);

        setUpToolbar();

        adapterSearchSubreddits = new AdapterSearchSubreddits(getActivity(),
                new ControllerSearchBase() {
                    @Override
                    public Subreddit getSubreddit(int position) {
                        return controllerSearch.getSubreddit(position);
                    }

                    @Override
                    public int getSubredditCount() {
                        return controllerSearch.getCountSubreddit();
                    }
                },
                new AdapterSearchSubreddits.ViewHolder.EventListener() {
                    @Override
                    public void onClickSubreddit(Subreddit subreddit) {
                        controllerSearch.addViewedSubreddit(subreddit);
                        controllerLinks.setParameters(subreddit.getDisplayName(), Sort.HOT, Time.ALL);
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
                        controllerSearch.moveSubreddit(viewHolder.getAdapterPosition(), 0);
                        recyclerSearchSubreddits.postOnAnimationDelayed(() -> {
                            if (layoutManagerSubreddits.findFirstVisibleItemPosition() <= 1) {
                                layoutManagerSubreddits.smoothScrollToPosition(recyclerSearchSubreddits, null, 0);
                            }
                        }, 150);
                    }

                    @Override
                    public boolean isSubscriptionListShown() {
                        return controllerSearch.isSubscriptionListShown();
                    }
                });

        layoutManagerSubreddits = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerSearchSubreddits.setLayoutManager(layoutManagerSubreddits);
        recyclerSearchSubreddits.setAdapter(adapterSearchSubreddits);
        recyclerSearchSubreddits.addItemDecoration(
                new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST));

        itemTouchHelperSubreddits = new ItemTouchHelper(new SimpleCallbackBackground(0, 0, windowBackground) {

            @Override
            public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (controllerSearch.isSubscriptionListShown()) {
                    return ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                }

                return super.getDragDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                controllerSearch.moveSubreddit(viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        });
        itemTouchHelperSubreddits.attachToRecyclerView(recyclerSearchSubreddits);

        AdapterLink.ViewHolderLink.Listener listenerLink = new AdapterLink.ViewHolderLink.Listener() {
            @Override
            public void onSubmitComment(Link link, String text) {

            }

            @Override
            public void onDownloadImage(Link link) {

            }

            @Override
            public void onDownloadImage(Link link, String title, String fileName, String url) {

            }

            @Override
            public void onLoadUrl(Link link, boolean forceExternal) {

            }

            @Override
            public void onShowFullEditor(Link link) {

            }

            @Override
            public void onVote(Link link, AdapterLink.ViewHolderLink viewHolderLink, Likes vote) {

            }

            @Override
            public void onCopyText(Link link) {

            }

            @Override
            public void onEdit(Link link) {

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
            public void onShowComments(Link link, AdapterLink.ViewHolderLink viewHolderLink, Source source) {

            }

            @Override
            public void onShowError(String error) {

            }

            @Override
            public void onMarkNsfw(Link link) {

            }
        };

        adapterLinks = new AdapterSearchLinkList(getActivity(), new AdapterListener() {
            @Override
            public void requestMore() {
                controllerSearch.loadMoreLinks()
                        .subscribe(getReloadObserver());
            }

            @Override
            public void scrollAndCenter(int position, int height) {
                UtilsAnimation.scrollToPositionWithCentering(position, recyclerSearchLinks, false);
            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000,
                        true);
            }

            @Override
            public void clearDecoration() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerSearchLinks.requestDisallowInterceptTouchEvent(disallow);
                recyclerSearchLinksSubreddit.requestDisallowInterceptTouchEvent(disallow);
                pagerSearch.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        }, new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(Reddit.PostType postType) {

            }

            @Override
            public void showSidebar() {

            }
        }, listenerLink,
                Source.SEARCH_LINKS);

        adapterLinksSubreddit = new AdapterSearchLinkList(getActivity(), new AdapterListener() {
            @Override
            public void requestMore() {
                controllerSearch.loadMoreLinksSubreddit()
                        .subscribe(getReloadObserver());
            }

            @Override
            public void scrollAndCenter(int position, int height) {
                UtilsAnimation.scrollToPositionWithCentering(position, recyclerSearchLinksSubreddit, false);
            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void clearDecoration() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerSearchLinks.requestDisallowInterceptTouchEvent(disallow);
                recyclerSearchLinksSubreddit.requestDisallowInterceptTouchEvent(disallow);
                pagerSearch.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        }, new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(Reddit.PostType postType) {

            }

            @Override
            public void showSidebar() {

            }
        }, listenerLink,
                Source.SEARCH_LINKS_SUBREDDIT);

        layoutManagerLinks = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerSearchLinks.setLayoutManager(layoutManagerLinks);
        recyclerSearchLinks.setAdapter(adapterLinks);
        recyclerSearchLinks.addItemDecoration(
                new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST));

        layoutManagerLinksSubreddit = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerSearchLinksSubreddit.setLayoutManager(layoutManagerLinksSubreddit);
        recyclerSearchLinksSubreddit.setAdapter(adapterLinksSubreddit);
        recyclerSearchLinksSubreddit.addItemDecoration(
                new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST));

        adapterSearchSubredditsRecommended = new AdapterSearchSubreddits(getActivity(),
                new ControllerSearchBase() {
                    @Override
                    public Subreddit getSubreddit(int position) {
                        return controllerSearch.getSubredditRecommended(position);
                    }

                    @Override
                    public int getSubredditCount() {
                        return controllerSearch.getCountSubredditRecommended();
                    }
                },
                new AdapterSearchSubreddits.ViewHolder.EventListener() {
                    @Override
                    public void onClickSubreddit(Subreddit subreddit) {
                        controllerLinks.setParameters(subreddit.getDisplayName(), Sort.HOT, Time.ALL);
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
                        return controllerSearch.isSubscriptionListShown();
                    }
                });

        layoutManagerSubredditsRecommended = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerSearchSubredditsRecommended.setLayoutManager(layoutManagerSubredditsRecommended);
        recyclerSearchSubredditsRecommended.setAdapter(adapterSearchSubredditsRecommended);
        recyclerSearchSubredditsRecommended.addItemDecoration(
                new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST));

        pagerSearch.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position,
                    float positionOffset,
                    int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                controllerSearch.setCurrentPage(position)
                        .subscribe(getReloadObserver());
                boolean sortSubredditsShown = controllerSearch
                        .getCurrentPage() == ControllerSearch.PAGE_SUBREDDITS ||
                        controllerSearch
                                .getCurrentPage() == ControllerSearch.PAGE_SUBREDDITS_RECOMMENDED;

                menu.findItem(R.id.item_sort_subreddits).setEnabled(sortSubredditsShown);
                menu.findItem(R.id.item_sort_subreddits).setVisible(sortSubredditsShown);
                menu.findItem(R.id.item_sort).setEnabled(!sortSubredditsShown);
                menu.findItem(R.id.item_sort).setVisible(!sortSubredditsShown);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        final int count = controllerLinks.isOnSpecificSubreddit() ? pagerSearch.getChildCount() : pagerSearch.getChildCount() - 1;

        pagerAdapter = new PagerAdapter() {
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                return pagerSearch.getChildAt(position);
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
                        return controllerLinks.getSubredditName();
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

        pagerSearch.setAdapter(pagerAdapter);

        layoutTabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        layoutTabs.setTabTextColors(themer.getColorFilterTextMuted().getColor(),
                themer.getColorFilterPrimary().getColor());
        layoutTabs.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        layoutTabs.setupWithViewPager(pagerSearch);
        pagerSearch.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(layoutTabs));

        return view;
    }

    private void closeKeyboard() {
        UtilsInput.hideKeyboard(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        controllerSearch.addListener(listenerSearch);

        ControllerSearch.EventHolder eventHolder = controllerSearch.getEventHolder();

        subscriptionLinks = eventHolder.getLinks()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(linksModelRxAdapterEvent -> adapterLinks.setData(linksModelRxAdapterEvent.getData()))
                .subscribe();

        subscriptionLinksSubreddit = eventHolder.getLinksSubreddit()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(linksModelRxAdapterEvent -> adapterLinksSubreddit.setData(linksModelRxAdapterEvent.getData()))
                .subscribe();

        subscriptionSort = eventHolder.getSort()
                .subscribe(sort -> menu.findItem(sort.getMenuId()).setChecked(true));

        subscriptionTime = eventHolder.getTime()
                .subscribe(time -> {
                    menu.findItem(time.getMenuId()).setChecked(true);
                    itemSortTime.setTitle(getString(R.string.time_description, menu.findItem(time.getMenuId()).toString()));
                });

        subscriptionCurrentPage = eventHolder.getCurrentPage()
                .subscribe(page -> pagerSearch.setCurrentItem(page));
    }

    @Override
    public void onPause() {
        UtilsRx.unsubscribe(subscriptionLinks);
        UtilsRx.unsubscribe(subscriptionLinksSubreddit);
        UtilsRx.unsubscribe(subscriptionSort);
        UtilsRx.unsubscribe(subscriptionTime);
        UtilsRx.unsubscribe(subscriptionCurrentPage);
        controllerSearch.removeListener(listenerSearch);
        controllerSearch.saveSubscriptions();
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        super.onDetach();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        item.setChecked(true);

        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            controllerSearch.setSort(sort)
                    .subscribe(getReloadObserver());
            flashSearchView();
            return true;
        }

        Time time = Time.fromMenuId(item.getItemId());
        if (time != null) {
            controllerSearch.setTime(time)
                    .subscribe(getReloadObserver());
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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            switch (pagerSearch.getCurrentItem()) {
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
        switch (pagerSearch.getCurrentItem()) {
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
        switch (pagerSearch.getCurrentItem()) {
            case ControllerSearch.PAGE_LINKS:
                adapterLinks.setVisibility(View.VISIBLE);
                break;
            case ControllerSearch.PAGE_LINKS_SUBREDDIT:
                adapterLinksSubreddit.setVisibility(View.VISIBLE);
                break;
        }
    }

}
