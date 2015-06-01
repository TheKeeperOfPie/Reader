package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;


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
    private static final String ARG_PARAM2 = "param2";
    ;

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
    private MenuItem itemInterface;

    private ControllerLinks.LinkClickListener linkClickListener;
    private ControllerSubreddits.SubredditListener subredditListener;
    private Listing subreddits;
    private AdapterSubreddits adapterSubreddits;
    private MenuItem itemSearch;
    private TextView textSidebar;
    private DrawerLayout drawerLayout;
    private TextView textEmpty;
    private Menu menu;

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
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();
        inflater.inflate(R.menu.menu_thread_list, menu);
        this.menu = menu;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());

        menu.findItem(R.id.item_sort_hot)
                .setChecked(true);

        itemInterface = menu.findItem(R.id.item_interface);
        switch (preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)) {
            case AppSettings.MODE_LIST:
                itemInterface.setIcon(R.drawable.ic_view_module_white_24dp);
                break;
            case AppSettings.MODE_GRID:
                itemInterface.setIcon(R.drawable.ic_view_list_white_24dp);
                break;
        }

        itemSearch = menu.findItem(R.id.item_search);

        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        swipeRefreshThreadList.setEnabled(false);
                        recyclerThreadList.setLayoutManager(adapterSubreddits.getLayoutManager());
                        recyclerThreadList.setAdapter(adapterSubreddits);
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        recyclerThreadList.setLayoutManager(adapterLink.getLayoutManager());
                        recyclerThreadList.setAdapter(adapterLink);
                        swipeRefreshThreadList.setEnabled(true);
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
                // TODO: Save sort state for individual subreddits
                // TODO: Possibly add sort indicator on menu icon
                mListener.getControllerLinks()
                        .setParameters(query, "hot");
                recyclerThreadList.setLayoutManager(adapterLink.getLayoutManager());
                recyclerThreadList.setAdapter(adapterLink);
                menu.findItem(R.id.item_sort_hot)
                        .setChecked(true);
                itemSearch.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Remove spaces from query text
                if (newText.contains(" ")) {
                    searchView.setQuery(newText.replaceAll(" ", ""), false);
                }
                String query = newText.toLowerCase()
                        .replaceAll(" ", "");
                mListener.getControllerSubreddits()
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
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());

        item.setChecked(true);
        switch (item.getItemId()) {
            case R.id.item_sort_hot:
                mListener.getControllerLinks()
                        .setSort("hot");
                flashSearchView();
                return true;
            case R.id.item_sort_new:
                mListener.getControllerLinks()
                        .setSort("new");
                flashSearchView();
                return true;
            case R.id.item_sort_top_hour:
                mListener.getControllerLinks()
                        .setSort("hourtop");
                flashSearchView();
                return true;
            case R.id.item_sort_top_day:
                mListener.getControllerLinks()
                        .setSort("daytop");
                flashSearchView();
                return true;
            case R.id.item_sort_top_week:
                mListener.getControllerLinks()
                        .setSort("weektop");
                flashSearchView();
                return true;
            case R.id.item_sort_top_month:
                mListener.getControllerLinks()
                        .setSort("monthtop");
                flashSearchView();
                return true;
            case R.id.item_sort_top_year:
                mListener.getControllerLinks()
                        .setSort("yeartop");
                flashSearchView();
                return true;
            case R.id.item_sort_top_all:
                mListener.getControllerLinks()
                        .setSort("alltop");
                flashSearchView();
                return true;
            case R.id.item_sort_controversial:
                mListener.getControllerLinks()
                        .setSort("controversial");
                flashSearchView();
                return true;
            case R.id.item_interface:
                if (AppSettings.MODE_LIST.equals(
                        preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST))) {
                    resetAdapter(new AdapterLinkGrid(activity, mListener.getControllerLinks(),
                            linkClickListener));
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_list_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .commit();
                }
                else {
                    resetAdapter(new AdapterLinkList(activity, mListener.getControllerLinks(),
                            linkClickListener));
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)
                            .commit();
                }
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /*
        Workaround for Android's drag-to-select menu bug, where the
        menu becomes unusable after a drag gesture
     */
    public void flashSearchView() {
        if (itemSearch != null) {
            itemSearch.expandActionView();
            itemSearch.collapseActionView();
        }
    }

    private void resetAdapter(AdapterLink newAdapter) {
        int[] currentPosition = new int[3];
        if (layoutManager instanceof LinearLayoutManager) {
            currentPosition[0] = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPositions(
                    currentPosition);
        }

        recyclerThreadList.removeItemDecoration(adapterLink.getItemDecoration());
        adapterLink = newAdapter;
        layoutManager = adapterLink.getLayoutManager();
        if (adapterLink.getItemDecoration() != null) {
//            recyclerThreadList.addItemDecoration(adapterLink.getItemDecoration());
        }
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.scrollToPosition(currentPosition[0]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_thread_list, container, false);

        linkClickListener = new ControllerLinks.LinkClickListener() {
            @Override
            public void onClickComments(final Link link, final RecyclerView.ViewHolder viewHolder) {
                mListener.getControllerComments()
                        .setLink(link);

                if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                    ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                            true);
                    viewHolder.itemView.requestLayout();
                }

                viewHolder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        final float viewStartY = viewHolder.itemView.getY();
                        // Grid layout has a 4 dp layout_margin that needs to be accounted for
                        final float minY = viewHolder instanceof AdapterLinkGrid.ViewHolder ?
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()) : 0;
                        final float viewStartPaddingBottom = viewHolder.itemView.getPaddingBottom();
                        final float screenHeight = getResources().getDisplayMetrics().heightPixels;

                        long duration = (long) Math.abs(
                                viewStartY / screenHeight * AnimationUtils.MOVE_DURATION);
                        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0,
                                -viewStartY + minY);

                        Animation heightAnimation = new Animation() {
                            @Override
                            protected void applyTransformation(float interpolatedTime,
                                                               Transformation t) {
                                super.applyTransformation(interpolatedTime, t);
                                viewHolder.itemView.setPadding(viewHolder.itemView.getPaddingLeft(),
                                        viewHolder.itemView.getPaddingTop(),
                                        viewHolder.itemView.getPaddingRight(),
                                        (int) (viewStartPaddingBottom + interpolatedTime * screenHeight));
                            }

                            @Override
                            public boolean willChangeBounds() {
                                return true;
                            }
                        };
                        heightAnimation.setStartOffset(duration / 10);
                        heightAnimation.setInterpolator(new LinearInterpolator());

                        AnimationSet animation = new AnimationSet(false);
                        animation.addAnimation(translateAnimation);
                        animation.addAnimation(heightAnimation);

                        animation.setDuration(duration);
                        animation.setFillAfter(false);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                FragmentComments fragmentComments = FragmentComments.newInstance(
                                        link.getSubreddit(), link.getId(),
                                        viewHolder instanceof AdapterLinkGrid.ViewHolder);

                                getFragmentManager().beginTransaction()
                                        .add(R.id.frame_fragment, fragmentComments,
                                                FragmentComments.TAG)
                                        .addToBackStack(null)
                                        .commit();

                                viewHolder.itemView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewHolder.itemView.setPadding(
                                                viewHolder.itemView.getPaddingLeft(),
                                                viewHolder.itemView.getPaddingTop(),
                                                viewHolder.itemView.getPaddingRight(),
                                                (int) viewStartPaddingBottom);
                                        viewHolder.itemView.clearAnimation();
                                    }
                                }, 150);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        viewHolder.itemView.startAnimation(animation);
                    }
                });
            }

            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFullLoaded(final int position) {
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
            public void setRefreshing(boolean refreshing) {
                swipeRefreshThreadList.setRefreshing(refreshing);
            }

            @Override
            public void setToolbarTitle(String title) {
                if (mListener != null) {
                    mListener.setToolbarTitle(title);
                }
            }

            @Override
            public AdapterLink getAdapter() {
                if (recyclerThreadList.getAdapter() == adapterSubreddits) {
                    recyclerThreadList.setLayoutManager(adapterLink.getLayoutManager());
                    recyclerThreadList.setAdapter(adapterLink);
                }

                return adapterLink;
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                recyclerThreadList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshThreadList.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerThreadList.getHeight();
            }

            @Override
            public void loadSideBar(Listing listingSubreddits) {
                if (listingSubreddits.getChildren()
                        .size() == 1) {
                    Subreddit subreddit = ((Subreddit) listingSubreddits.getChildren()
                            .get(0));
                    mListener.getControllerLinks()
                            .getReddit()
                            .loadGet(
                                    Reddit.OAUTH_URL + "/r/" + subreddit.getDisplayName() + "/about",
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                Subreddit loadedSubreddit = Subreddit.fromJson(
                                                        new JSONObject(response));
                                                String html = Html.fromHtml(
                                                        loadedSubreddit.getDescriptionHtml())
                                                        .toString();
                                                CharSequence sequence = Html.fromHtml(html);
                                                textSidebar.setText(sequence);
                                                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                                            }
                                            catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {

                                        }
                                    }, 0);
                }
                else {
                    textSidebar.setText(null);
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
            }

            @Override
            public void setEmptyView(boolean visible) {
                textEmpty.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        };

        subredditListener = new ControllerSubreddits.SubredditListener() {

            @Override
            public void onClickSubreddit(Subreddit subreddit) {
                recyclerThreadList.setLayoutManager(adapterLink.getLayoutManager());
                recyclerThreadList.setAdapter(adapterLink);
                menu.findItem(R.id.item_sort_hot)
                        .setChecked(true);
                itemSearch.collapseActionView();
                mListener.getControllerLinks().setParameters(subreddit.getDisplayName(), "hot");
            }

            @Override
            public AdapterSubreddits getAdapter() {
                Log.d(TAG, "AdapterSubreddits getAdapter");
                return adapterSubreddits;
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {

            }
        };

        swipeRefreshThreadList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_thread_list);
        swipeRefreshThreadList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerLinks()
                        .reloadAllLinks();
            }
        });
        if (adapterLink == null) {
            if (AppSettings.MODE_LIST.equals(
                    PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())
                            .getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST))) {
                adapterLink = new AdapterLinkList(activity, mListener.getControllerLinks(),
                        linkClickListener);
            }
            else {
                adapterLink = new AdapterLinkGrid(activity, mListener.getControllerLinks(),
                        linkClickListener);
            }
        }
        adapterLink.setActivity(activity);

        if (adapterSubreddits == null) {
            adapterSubreddits = new AdapterSubreddits(activity, mListener.getControllerSubreddits(), subredditListener);
        }

        layoutManager = adapterLink.getLayoutManager();

        recyclerThreadList = (RecyclerView) view.findViewById(R.id.recycler_thread_list);
        recyclerThreadList.setScrollBarDefaultDelayBeforeFade(0);
        recyclerThreadList.setScrollBarFadeDuration(100);
        if (adapterLink.getItemDecoration() != null) {
//            recyclerThreadList.addItemDecoration(adapterLink.getItemDecoration());
        }
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setHasFixedSize(true);

        drawerLayout = (DrawerLayout) view.findViewById(R.id.drawer_layout);

        textSidebar = (TextView) view.findViewById(R.id.text_sidebar);
        textSidebar.setMovementMethod(LinkMovementMethod.getInstance());

        textEmpty = (TextView) view.findViewById(R.id.text_empty);

        mListener.getControllerLinks()
                .addListener(linkClickListener);
        mListener.getControllerSubreddits()
                .addListener(subredditListener);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        swipeRefreshThreadList.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshThreadList.setRefreshing(true);
                mListener.getControllerLinks()
                        .reloadAllLinks();

            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
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
        mListener = null;
        activity = null;
    }


    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerLinks()
                .addListener(linkClickListener);
        mListener.getControllerSubreddits()
                .addListener(subredditListener);
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
    public void onStop() {
        mListener.getControllerLinks()
                .removeListener(linkClickListener);
        mListener.getControllerSubreddits()
                .removeListener(subredditListener);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity()).watch(this);
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
        ControllerComments getControllerComments();
        ControllerSubreddits getControllerSubreddits();
        void setNavigationAnimation(float value);
    }

}