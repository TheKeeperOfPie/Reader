package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Subreddit;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentSearch.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentSearch#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentSearch extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int PAGE_COUNT = 3;
    private static final String TAG = FragmentSearch.class.getCanonicalName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private Activity activity;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ControllerSearch controllerSearch;
    private RecyclerView recyclerSearchSubreddits;
    private RecyclerView recyclerSearchLinks;
    private RecyclerView recyclerSearchUsers;
    private AdapterSearchSubreddits adapterSearchSubreddits;
    private AdapterLinkList adapterLinks;
    private AdapterSearchUsers adapterSearchUsers;
    private ControllerSearch.Listener listenerSearch;
    private PagerAdapter pagerAdapter;
    private Menu menu;
    private MenuItem itemSearch;

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

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();
        inflater.inflate(R.menu.menu_search, menu);
        this.menu = menu;

        itemSearch = menu.findItem(R.id.item_search);
        itemSearch.expandActionView();

        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        getFragmentManager().popBackStack();
                        return true;
                    }
                });

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
                Log.d(TAG, "Query entered");
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Remove spaces from query text
                if (!isAdded() || mListener == null) {
                    return false;
                }
                if (newText.contains(" ")) {
                    searchView.setQuery(newText.replaceAll(" ", ""), false);
                }
                String query = newText.toLowerCase()
                        .replaceAll(" ", "");
                mListener.getControllerSearch()
                        .setQuery(query);
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);
    }

    @Override
    public void onDestroyOptionsMenu() {
        SearchView searchView = (SearchView) itemSearch.getActionView();
        searchView.setOnQueryTextListener(null);
        itemSearch = null;
        super.onDestroyOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        listenerSearch = new ControllerSearch.Listener() {
            @Override
            public void onClickSubreddit(Subreddit subreddit) {
                mListener.getControllerLinks().setParameters(subreddit.getDisplayName(), "hot");
                getFragmentManager().popBackStack();
            }

            @Override
            public void notifyChangedSubreddits() {
                adapterSearchSubreddits.notifyDataSetChanged();
            }

            @Override
            public void notifyChangedLinks() {
                adapterLinks.notifyDataSetChanged();
            }

            @Override
            public AdapterLink getAdapterLinks() {
                return adapterLinks;
            }
        };

        adapterSearchSubreddits = new AdapterSearchSubreddits(activity,
                mListener.getControllerSearch(), listenerSearch);
        recyclerSearchSubreddits = (RecyclerView) view.findViewById(R.id.recycler_search_subreddits);
        recyclerSearchSubreddits.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerSearchSubreddits.setAdapter(adapterSearchSubreddits);

        adapterLinks = new AdapterLinkList(activity, mListener.getControllerSearch(),
                new ControllerLinks.LinkClickListener() {
                    @Override
                    public void onClickComments(Link link, RecyclerView.ViewHolder viewHolder) {

                    }

                    @Override
                    public void loadUrl(String url) {

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
                        return 0;
                    }

                    @Override
                    public void loadSideBar(Listing listingSubreddits) {

                    }

                    @Override
                    public void setEmptyView(boolean visible) {

                    }

                    @Override
                    public int getRecyclerWidth() {
                        return 0;
                    }

                    @Override
                    public ControllerCommentsBase getControllerComments() {
                        return null;
                    }

                    @Override
                    public void requestDisallowInterceptTouchEvent(boolean disallow) {

                    }
                });
        recyclerSearchLinks = (RecyclerView) view.findViewById(R.id.recycler_search_links);
        recyclerSearchLinks.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerSearchLinks.setAdapter(adapterLinks);

        adapterSearchUsers = new AdapterSearchUsers();
        recyclerSearchUsers = (RecyclerView) view.findViewById(R.id.recycler_search_users);
        recyclerSearchUsers.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));

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
                    case 0:
                        return getString(R.string.subreddit);
                    case 1:
                        return getString(R.string.link);
                    case 2:
                        return getString(R.string.user);
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

        viewPager = (ViewPager) view.findViewById(R.id.view_pager_search);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

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
            mListener = (OnFragmentInteractionListener) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        ControllerSearch getControllerSearch();
        ControllerLinks getControllerLinks();
    }

}
