package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentThreadList.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentThreadList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentThreadList extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentThreadList.class.getCanonicalName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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
    private MenuItem itemSearch;
    private TextView textSidebar;
    private DrawerLayout drawerLayout;
    private TextView textEmpty;
    private Menu menu;
    private MenuItem itemSortTime;
    private Toolbar toolbar;
    private AdapterLinkList adapterLinkList;
    private AdapterLinkGrid adapterLinkGrid;

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
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_thread_list);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());

        itemInterface = menu.findItem(R.id.item_interface);
        switch (preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)) {
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

        resetSubmenuSelected();

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
            currentPosition[0] = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
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
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
            recyclerThreadList.setPadding(padding, 0, padding, 0);
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

                // TODO: Move onClickComments code to shared class to prevent code duplication

                if (link.getNumComments() == 0) {
                    if (!link.isCommentsClicked()) {
                        Toast.makeText(activity, activity.getString(R.string.no_comments), Toast.LENGTH_SHORT).show();
                        link.setCommentsClicked(true);
                        return;
                    }
                }

                mListener.getControllerComments()
                        .setLink(link);

                if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                    ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                            true);
                    viewHolder.itemView.requestLayout();
                    viewHolder.itemView.post(new Runnable() {
                        @Override
                        public void run() {
                            ((StaggeredGridLayoutManager) layoutManager).invalidateSpanAssignments();
                        }
                    });
                }

                viewHolder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        final float viewStartY = viewHolder.itemView.getY();
                        // Grid layout has a 2 dp layout_margin that needs to be accounted for
                        final float minY = viewHolder instanceof AdapterLinkGrid.ViewHolder ?
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                                        getResources().getDisplayMetrics()) : 0;
                        final float viewStartPaddingBottom = viewHolder.itemView.getPaddingBottom();
                        final float screenHeight = getResources().getDisplayMetrics().heightPixels;

                        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                                activity.getResources()
                                        .getDisplayMetrics());
                        long duration = (long) Math.abs(viewStartY / speed);

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
                                        .hide(FragmentThreadList.this)
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
                        .hide(FragmentThreadList.this)
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
                toolbar.setTitle(title);
            }

            @Override
            public AdapterLink getAdapter() {
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
            public void loadSideBar(Subreddit subreddit) {
                if (subreddit.getUrl().equals("/") || "/r/all/".equalsIgnoreCase(subreddit.getUrl())) {
                    return;
                }

                mListener.getControllerLinks()
                        .getReddit()
                        .loadGet(
                                Reddit.OAUTH_URL + subreddit.getUrl() + "about",
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Subreddit loadedSubreddit = Subreddit.fromJson(
                                                    new JSONObject(response));
                                            textSidebar.setText(Reddit.getTrimmedHtml(loadedSubreddit.getDescriptionHtml()));
                                            drawerLayout.setDrawerLockMode(
                                                    DrawerLayout.LOCK_MODE_UNLOCKED);
                                        }
                                        catch (JSONException e) {
                                            textSidebar.setText(null);
                                            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        textSidebar.setText(null);
                                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                                    }
                                }, 0);
            }

            @Override
            public void setEmptyView(boolean visible) {
                textEmpty.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            @Override
            public int getRecyclerWidth() {
                return recyclerThreadList.getWidth();
            }

            @Override
            public void onClickSubmit(String postType) {
                if (TextUtils.isEmpty(mListener.getControllerInbox().getUser().getName())) {
                    Toast.makeText(activity, getString(R.string.must_be_logged_in), Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(activity, ActivityNewPost.class);
                intent.putExtra(ActivityNewPost.USER, mListener.getControllerInbox().getUser().getName());
                intent.putExtra(ActivityNewPost.SUBREDDIT, mListener.getControllerLinks().getSubreddit().getUrl().substring(
                        3, mListener.getControllerLinks()
                        .getSubreddit()
                        .getUrl()
                        .length() - 1));
                intent.putExtra(ActivityNewPost.POST_TYPE, postType);
                intent.putExtra(ActivityNewPost.SUBMIT_TEXT_HTML, mListener.getControllerLinks().getSubreddit().getSubmitTextHtml());
                startActivityForResult(intent, ActivityNewPost.REQUEST_CODE);
            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return mListener.getControllerComments();
            }

            @Override
            public void setSort(Sort sort) {
                menu.findItem(sort.getMenuId()).setChecked(true);
            }
        };

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
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
                        .reloadAllLinks(false);
            }
        });
        if (adapterLinkList == null) {
            adapterLinkList = new AdapterLinkList(activity, mListener.getControllerLinks(),
                    linkClickListener);
        }
        if (adapterLinkGrid == null) {
            adapterLinkGrid = new AdapterLinkGrid(activity, mListener.getControllerLinks(),
                    linkClickListener);
        }

        if (AppSettings.MODE_LIST.equals(
                PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext())
                        .getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST))) {
            adapterLink = adapterLinkList;
        }
        else {
            adapterLink = adapterLinkGrid;
        }

        adapterLinkList.setActivity(activity);
        adapterLinkGrid.setActivity(activity);

        layoutManager = adapterLink.getLayoutManager();

        recyclerThreadList = (RecyclerView) view.findViewById(R.id.recycler_thread_list);
        recyclerThreadList.setScrollBarDefaultDelayBeforeFade(0);
        recyclerThreadList.setScrollBarFadeDuration(100);
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setHasFixedSize(true);
        recyclerThreadList.setAdapter(adapterLink);

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerThreadList.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
            recyclerThreadList.setPadding(padding, 0, padding, 0);
        }

        drawerLayout = (DrawerLayout) view.findViewById(R.id.drawer_layout);

        textSidebar = (TextView) view.findViewById(R.id.text_sidebar);
        textSidebar.setMovementMethod(LinkMovementMethod.getInstance());

        textEmpty = (TextView) view.findViewById(R.id.text_empty);
        mListener.getControllerLinks()
                .addListener(linkClickListener);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            mListener.getControllerLinks().reloadAllLinks(false);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "onActivityCreated");

        swipeRefreshThreadList.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshThreadList.setRefreshing(true);
                mListener.getControllerLinks()
                        .reloadAllLinks(false);

            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
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
    }

    @Override
    public void onStop() {
        mListener.getControllerLinks()
                .removeListener(linkClickListener);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());

        item.setChecked(true);
        switch (item.getItemId()) {
            case R.id.item_search:
                getFragmentManager().beginTransaction()
                        .hide(FragmentThreadList.this)
                        .add(R.id.frame_fragment, FragmentSearch.newInstance("", ""))
                        .addToBackStack(null)
                        .commit();
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

        for (Sort sort : Sort.values()) {
            if (sort.getMenuId() == item.getItemId()) {
                mListener.getControllerLinks()
                        .setSort(sort);
                flashSearchView();
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
                return true;
            }
        }

        return false;
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
    public interface OnFragmentInteractionListener extends FragmentListenerBase {
        ControllerLinks getControllerLinks();
        ControllerComments getControllerComments();
        ControllerInbox getControllerInbox();
    }

}