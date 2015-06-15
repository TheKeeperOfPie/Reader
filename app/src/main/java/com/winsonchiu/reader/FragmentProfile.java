package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
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

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentProfile.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentProfile#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentProfile extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = FragmentProfile.class.getCanonicalName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private Activity activity;
    private ControllerProfile.ItemClickListener listener;
    private SwipeRefreshLayout swipeRefreshProfile;
    private RecyclerView recyclerProfile;
    private LinearLayoutManager linearLayoutManager;
    private User user;
    private AdapterProfile adapterProfile;
    private SharedPreferences preferences;
    private MenuItem itemSearch;
    private Menu menu;
    private MenuItem itemSortTime;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentProfile.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentProfile newInstance(String param1, String param2) {
        FragmentProfile fragment = new FragmentProfile();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentProfile() {
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
        preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        this.user = new User();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_profile, menu);
        this.menu = menu;

        menu.findItem(R.id.item_sort_hot)
                .setChecked(true);

        itemSortTime = menu.findItem(R.id.item_sort_time);
        itemSearch = menu.findItem(R.id.item_search);

        final SearchView searchView = (SearchView) itemSearch.getActionView();

        searchView.setQueryHint(getString(R.string.username));
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
                mListener.getControllerProfile()
                        .loadUser(query);
                itemSearch.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Remove spaces from query text
                if (newText.contains(" ")) {
                    searchView.setQuery(newText.replaceAll(" ", ""), false);
                }
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        resetSubmenuSelected();

        mListener.setFloatingActionButtonValues(null, 0);
    }

    private void resetSubmenuSelected() {
        onOptionsItemSelected(menu.findItem(mListener.getControllerProfile()
                .getSort()
                .getMenuId()));
        onOptionsItemSelected(menu.findItem(mListener.getControllerProfile()
                .getTime()
                .getMenuId()));

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

        item.setChecked(true);

        for (Sort sort : Sort.values()) {
            if (sort.getMenuId() == item.getItemId()) {
                mListener.getControllerProfile()
                        .setSort(sort);
                flashSearchView();
                return super.onOptionsItemSelected(item);
            }
        }

        for (Time time : Time.values()) {
            if (time.getMenuId() == item.getItemId()) {
                mListener.getControllerProfile()
                        .setTime(time);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + item.toString());
                flashSearchView();
                return super.onOptionsItemSelected(item);
            }
        }
        return super.onOptionsItemSelected(item);
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

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        listener = new ControllerProfile.ItemClickListener() {
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
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                                        getResources().getDisplayMetrics()) : 0;
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
            public void onFullLoaded(int position) {

            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshProfile.setRefreshing(refreshing);
            }

            @Override
            public void setToolbarTitle(String title) {
                mListener.setToolbarTitle(title);
            }

            @Override
            public AdapterProfile getAdapter() {
                return adapterProfile;
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerProfile.getHeight();
            }

            @Override
            public void resetRecycler() {
                recyclerProfile.setAdapter(null);
                recyclerProfile.swapAdapter(adapterProfile, true);
            }

            @Override
            public void setSwipeRefreshEnabled(boolean enabled) {
                swipeRefreshProfile.setEnabled(enabled);
            }

            @Override
            public int getRecyclerWidth() {
                return recyclerProfile.getWidth();
            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return mListener.getControllerComments();
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {

            }
        };

        swipeRefreshProfile = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_profile);
        swipeRefreshProfile.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerProfile()
                        .reload();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerProfile = (RecyclerView) view.findViewById(R.id.recycler_profile);
        recyclerProfile.setHasFixedSize(true);
        recyclerProfile.setItemAnimator(new DefaultItemAnimator());
        recyclerProfile.getItemAnimator()
                .setRemoveDuration(AnimationUtils.EXPAND_ACTION_DURATION);
        recyclerProfile.setLayoutManager(linearLayoutManager);
//        recyclerProfile.addItemDecoration(
//                new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST));

        if (adapterProfile == null) {
            adapterProfile = new AdapterProfile(activity, mListener.getControllerProfile(),
                    listener);
        }

        recyclerProfile.setAdapter(adapterProfile);
        mListener.getControllerProfile()
                .addListener(listener);

        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            swipeRefreshProfile.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshProfile.setRefreshing(true);
                    mListener.getControllerProfile()
                            .setUser(user);
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerProfile()
                .addListener(listener);
    }

    @Override
    public void onStop() {
        mListener.getControllerProfile()
                .removeListener(listener);
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
        mListener = null;
        activity = null;
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
    public interface OnFragmentInteractionListener extends FragmentListenerBase {
        // TODO: Update argument type and name
        void setToolbarTitle(CharSequence title);

        ControllerComments getControllerComments();

        ControllerProfile getControllerProfile();
    }

}
