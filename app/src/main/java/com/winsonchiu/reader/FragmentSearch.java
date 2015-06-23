package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

public class FragmentSearch extends Fragment implements Toolbar.OnMenuItemClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int PAGE_COUNT = 3;
    public static final String TAG = FragmentSearch.class.getCanonicalName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FragmentListenerBase mListener;

    private Activity activity;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private RecyclerView recyclerSearchSubreddits;
    private RecyclerView recyclerSearchLinks;
    private RecyclerView recyclerSearchLinksSubreddit;
    private AdapterSearchSubreddits adapterSearchSubreddits;
    private AdapterLink adapterLinks;
    private AdapterLink adapterLinksSubreddit;
    private ControllerSearch.Listener listenerSearch;
    private PagerAdapter pagerAdapter;
    private Menu menu;
    private MenuItem itemSearch;
    private MenuItem itemSortTime;
    private Toolbar toolbar;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentSearch.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentSearch newInstance(String param1, String param2) {
        FragmentSearch fragment = new FragmentSearch();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentSearch() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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

        searchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey: " + keyCode);
                return keyCode == 44;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (mListener.getControllerSearch().getCurrentPage() == ControllerSearch.PAGE_SUBREDDITS) {
                    mListener.getControllerLinks()
                            .setParameters(query.replaceAll("\\s", ""), Sort.HOT);
                    mListener.getControllerSearch()
                            .clearResults();
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
                // TODO: Remove spaces from query text
                if (!isAdded() || mListener == null) {
                    return false;
                }
                mListener.getControllerSearch()
                        .setQuery(newText);
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        menu.findItem(R.id.item_sort_relevance)
                .setChecked(true);
        menu.findItem(R.id.item_sort_all)
                .setChecked(true);
        itemSortTime.setTitle(
                getString(R.string.time) + Reddit.TIME_SEPARATOR + getString(
                        R.string.item_sort_all));
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
            public void onClickSubreddit(Subreddit subreddit) {
                mListener.getControllerLinks()
                        .setParameters(subreddit.getDisplayName(), Sort.HOT);
                mListener.getControllerSearch()
                        .clearResults();
                InputMethodManager inputManager = (InputMethodManager) activity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                getFragmentManager().popBackStack();
            }

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
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
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
                mListener.getControllerSearch(), listenerSearch);
        recyclerSearchSubreddits = (RecyclerView) view.findViewById(
                R.id.recycler_search_subreddits);
        recyclerSearchSubreddits.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerSearchSubreddits.setAdapter(adapterSearchSubreddits);

        ControllerLinks.LinkClickListener linkClickListener = new ControllerLinks.LinkClickListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerSearchLinks.requestDisallowInterceptTouchEvent(disallow);
                recyclerSearchLinksSubreddit.requestDisallowInterceptTouchEvent(disallow);
                viewPager.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }

            @Override
            public void onClickComments(final Link link, final RecyclerView.ViewHolder viewHolder) {


                mListener.getControllerComments()
                        .setLink(link);

                AnimationUtils.loadCommentFragmentAnimation(activity, mListener.getControllerSearch().getCurrentPage() == ControllerSearch.PAGE_LINKS ? recyclerSearchLinks.getLayoutManager() : recyclerSearchLinksSubreddit.getLayoutManager(),
                        viewHolder,
                        link, new AnimationUtils.OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                int color = viewHolder instanceof AdapterLinkGrid.ViewHolder ?
                                        ((ColorDrawable) viewHolder.itemView.getBackground()).getColor() :
                                        activity.getResources()
                                                .getColor(R.color.darkThemeBackground);

                                FragmentComments fragmentComments = FragmentComments.newInstance(
                                        link.getSubreddit(), link.getId(),
                                        viewHolder instanceof AdapterLinkGrid.ViewHolder, color);

                                getFragmentManager().beginTransaction()
                                        .hide(FragmentSearch.this)
                                        .add(R.id.frame_fragment, fragmentComments,
                                                FragmentComments.TAG)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        });
            }

            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction()
                        .hide(FragmentSearch.this)
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFullLoaded(int position) {

            }

            @Override
            public void setRefreshing(boolean refreshing) {

            }

            @Override
            public void setToolbarTitle(String title) {

            }

            @Override
            public AdapterLink getAdapter() {
                return null;
            }

            @Override
            public int getRecyclerHeight() {
                return viewPager.getHeight();
            }

            @Override
            public void loadSideBar(Subreddit listingSubreddits) {

            }

            @Override
            public void setEmptyView(boolean visible) {

            }

            @Override
            public int getRecyclerWidth() {
                return viewPager.getWidth();
            }

            @Override
            public void onClickSubmit(String postType) {

            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return mListener.getControllerComments();
            }

            @Override
            public void setSort(Sort sort) {

            }

            @Override
            public void loadVideoLandscape(int position) {

            }

            @Override
            public int getRequestedOrientation() {
                return mListener.getRequestedOrientation();
            }

        };

        adapterLinks = new AdapterSearchLinkList(activity, new ControllerLinksBase() {
            @Override
            public Link getLink(int position) {
                return mListener.getControllerSearch()
                        .getLink(position);
            }

            @Override
            public Reddit getReddit() {
                return mListener.getControllerLinks()
                        .getReddit();
            }

            @Override
            public void voteLink(RecyclerView.ViewHolder viewHolder, final Link link, int vote) {
                mListener.getControllerSearch()
                        .voteLink(viewHolder, link, vote);
            }

            @Override
            public Drawable getDrawableForLink(Link link) {
                return mListener.getControllerSearch()
                        .getDrawableForLink(link);
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
            public Activity getActivity() {
                return mListener.getControllerSearch()
                        .getActivity();
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
            public void deletePost(Link link) {
                // Not implemented
            }
        }, linkClickListener);

        adapterLinksSubreddit = new AdapterSearchLinkList(activity, new ControllerLinksBase() {
            @Override
            public Link getLink(int position) {
                return mListener.getControllerSearch()
                        .getLinkSubreddit(position);
            }

            @Override
            public Reddit getReddit() {
                return mListener.getControllerLinks()
                        .getReddit();
            }

            @Override
            public void voteLink(RecyclerView.ViewHolder viewHolder, final Link link, int vote) {
                mListener.getControllerSearch()
                        .voteLinkSubreddit(viewHolder, link, vote);
            }

            @Override
            public Drawable getDrawableForLink(Link link) {
                return mListener.getControllerSearch()
                        .getDrawableForLink(link);
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
            public Activity getActivity() {
                return mListener.getControllerSearch()
                        .getActivity();
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
            public void deletePost(Link link) {
                // Not implemented
            }
        }, linkClickListener);

        recyclerSearchLinks = (RecyclerView) view.findViewById(R.id.recycler_search_links);
        recyclerSearchLinks.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerSearchLinks.setAdapter(adapterLinks);

        recyclerSearchLinksSubreddit = (RecyclerView) view.findViewById(
                R.id.recycler_search_links_subreddit);
        recyclerSearchLinksSubreddit.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
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

}
