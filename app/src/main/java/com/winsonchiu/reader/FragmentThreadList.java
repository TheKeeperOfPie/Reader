package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

import com.winsonchiu.reader.data.Link;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentThreadList.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentThreadList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentThreadList extends Fragment {

    public static final String TAG = FragmentThreadList.class.getCanonicalName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Activity activity;
    private OnFragmentInteractionListener mListener;

    private SharedPreferences preferences;
    private RecyclerView recyclerThreadList;
    private AdapterLink adapterLink;
    private SwipeRefreshLayout swipeRefreshThreadList;
    private RecyclerView.LayoutManager layoutManager;
    private ControllerLinks controllerLinks;
    private MenuItem itemInterface;

    private ControllerLinks.LinkClickListener linkClickListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentThreadList.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentThreadList newInstance(String param1, String param2) {
        FragmentThreadList fragment = new FragmentThreadList();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentThreadList() {
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
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_thread_list, menu);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());

        itemInterface = menu.findItem(R.id.item_interface);
        switch (preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)) {
            case AppSettings.MODE_LIST:
                itemInterface.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                break;
            case AppSettings.MODE_GRID:
                itemInterface.setIcon(getResources().getDrawable(R.drawable.ic_view_list_white_24dp));
                break;
        }

        final MenuItem itemSearch = menu.findItem(R.id.item_search);

        final SearchView searchView = (SearchView) itemSearch.getActionView();

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return false;
            }
        });
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
                controllerLinks.setParameters(query, "hot");
                itemSearch.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Remove spaces from query text

//                if (newText.contains(" ")) {
//                    searchView.setQuery(newText.replaceAll(" ", ""), false);
//                }
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());


        switch (item.getItemId()) {
            case R.id.item_sort_hot:
                controllerLinks.setSort("hot");
                break;
            case R.id.item_sort_new:
                controllerLinks.setSort("new");
                break;
            case R.id.item_sort_top_hour:
                controllerLinks.setSort("hourtop");
                break;
            case R.id.item_sort_top_day:
                controllerLinks.setSort("daytop");
                break;
            case R.id.item_sort_top_week:
                controllerLinks.setSort("weektop");
                break;
            case R.id.item_sort_top_month:
                controllerLinks.setSort("monthtop");
                break;
            case R.id.item_sort_top_year:
                controllerLinks.setSort("yeartop");
                break;
            case R.id.item_sort_top_all:
                controllerLinks.setSort("alltop");
                break;
            case R.id.item_sort_controversial:
                controllerLinks.setSort("controversial");
                break;
            case R.id.item_interface:
                if (AppSettings.MODE_LIST.equals(
                        preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST))) {
                    resetAdapter(new AdapterLinkGrid(activity, controllerLinks, linkClickListener));
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_list_white_24dp));
                    preferences.edit().putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .commit();
                }
                else {
                    resetAdapter(new AdapterLinkList(activity, controllerLinks, linkClickListener));
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    preferences.edit().putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST).commit();
                }
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void resetAdapter(AdapterLink newAdapter) {
        int[] currentPosition = new int[3];
        if (layoutManager instanceof LinearLayoutManager) {
            currentPosition[0] = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPositions(currentPosition);
        }

        recyclerThreadList.removeItemDecoration(adapterLink.getItemDecoration());
        adapterLink = newAdapter;
        layoutManager = adapterLink.getLayoutManager();
        if (adapterLink.getItemDecoration() != null) {
            recyclerThreadList.addItemDecoration(adapterLink.getItemDecoration());
        }
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        layoutManager.scrollToPosition(currentPosition[0]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_thread_list, container, false);

        linkClickListener = new ControllerLinks.LinkClickListener() {
            @Override
            public void onClickComments(Link link) {
                getFragmentManager().beginTransaction().add(R.id.frame_fragment, FragmentComments
                        .newInstance(link.getSubreddit(), link.getId()), "fragmentComments").addToBackStack(null)
                        .commit();
            }

            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction().add(R.id.frame_fragment, FragmentWeb
                        .newInstance(url, ""), FragmentWeb.TAG).addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFullLoaded(final int position) {
                recyclerThreadList.post(new Runnable() {
                    @Override
                    public void run() {
                        if (layoutManager instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, 0);
                        }
                        else if (layoutManager instanceof StaggeredGridLayoutManager) {
                            ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(position, 0);
                        }
                    }
                });
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshThreadList.setRefreshing(refreshing);
            }

            @Override
            public void setToolbarTitle(String title) {
                mListener.setToolbarTitle(title);
            }

            @Override
            public AdapterLink getAdapter() {
                return adapterLink;
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                recyclerThreadList.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerThreadList.getHeight();
            }
        };

        swipeRefreshThreadList = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_thread_list);
        swipeRefreshThreadList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerLinks.reloadAllLinks();
            }
        });

        if (controllerLinks == null) {
            controllerLinks = mListener.getControllerLinks();
            controllerLinks.reloadAllLinks();
        }
        controllerLinks.setActivity(activity);

        if (adapterLink == null) {
            if (AppSettings.MODE_LIST.equals(
                    PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())
                            .getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST))) {
                adapterLink = new AdapterLinkList(activity, controllerLinks, linkClickListener);
            }
            else {
                adapterLink = new AdapterLinkGrid(activity, controllerLinks, linkClickListener);
            }
        }
        adapterLink.setActivity(activity);
        layoutManager = adapterLink.getLayoutManager();

        recyclerThreadList = (RecyclerView) view.findViewById(R.id.recycler_thread_list);
        recyclerThreadList.setScrollBarDefaultDelayBeforeFade(0);
        recyclerThreadList.setScrollBarFadeDuration(100);
        if (adapterLink.getItemDecoration() != null) {
            recyclerThreadList.addItemDecoration(adapterLink.getItemDecoration());
        }
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setHasFixedSize(true);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
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
        mListener = null;
        activity = null;
        super.onDetach();
    }


    @Override
    public void onStart() {
        super.onStart();
        controllerLinks.addListener(linkClickListener);
    }

    @Override
    public void onStop() {
        controllerLinks.removeListener(linkClickListener);
        super.onStop();
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
        void setToolbarTitle(CharSequence title);
        ControllerLinks getControllerLinks();
    }

}
