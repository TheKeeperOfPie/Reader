package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

public class FragmentProfile extends FragmentBase implements Toolbar.OnMenuItemClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = FragmentProfile.class.getCanonicalName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FragmentListenerBase mListener;
    private Activity activity;
    private ControllerProfile.Listener listener;
    private SwipeRefreshLayout swipeRefreshProfile;
    private RecyclerView recyclerProfile;
    private LinearLayoutManager linearLayoutManager;
    private User user;
    private AdapterProfile adapterProfile;
    private SharedPreferences preferences;
    private MenuItem itemSearch;
    private Menu menu;
    private MenuItem itemSortTime;
    private Toolbar toolbar;
    private Spinner spinnerPage;
    private AdapterProfilePage adapterProfilePage;

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
        preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        this.user = new User();
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_profile);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();
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
                        .loadUser(query.replaceAll("\\s", ""));
                itemSearch.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        resetSubmenuSelected();
    }

    private void resetSubmenuSelected() {
        onMenuItemClick(menu.findItem(mListener.getControllerProfile()
                .getSort()
                .getMenuId()));
        onMenuItemClick(menu.findItem(mListener.getControllerProfile()
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

        listener = new ControllerProfile.Listener() {
            @Override
            public void setPage(String page) {
                spinnerPage.setSelection(adapterProfilePage.getPages().indexOf(page));
            }

            @Override
            public void setIsUser(boolean isUser) {
                // TODO: Fix set page for Profile view
                adapterProfilePage.setIsUser(isUser);
            }

            @Override
            public void loadLink(Comment comment) {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(MainActivity.REDDIT_PAGE,
                        "https://reddit.com/r/" + comment.getSubreddit() + "/comments/" + comment
                                .getLinkId().replace("t3_", ""));
                startActivity(intent);
            }

            @Override
            public RecyclerView.Adapter getAdapter() {
                return adapterProfile;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshProfile.setRefreshing(refreshing);
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

        adapterProfilePage = new AdapterProfilePage(activity);
        spinnerPage = (Spinner) view.findViewById(R.id.spinner_page);
        spinnerPage.setAdapter(adapterProfilePage);
        spinnerPage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mListener.getControllerProfile().setPage(adapterProfilePage.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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

        if (adapterProfile == null) {
            adapterProfile = new AdapterProfile(activity, mListener.getControllerProfile(),
                    mListener.getControllerLinks(),
                    mListener.getControllerUser(),
                    mListener.getEventListenerBase(),
                    new AdapterCommentList.ViewHolderComment.EventListener() {
                        @Override
                        public void loadNestedComments(Comment comment) {
                            mListener.getControllerProfile().loadNestedComments(comment);
                        }

                        @Override
                        public boolean isCommentExpanded(int position) {
                            return mListener.getControllerProfile().isCommentExpanded(position);
                        }

                        @Override
                        public boolean hasChildren(Comment comment) {
                            return mListener.getControllerProfile().hasChildren(comment);
                        }

                        @Override
                        public void voteComment(AdapterCommentList.ViewHolderComment viewHolderComment,
                                Comment comment,
                                int vote) {
                            mListener.getControllerProfile().voteComment(viewHolderComment, comment, vote);
                        }

                        @Override
                        public boolean toggleComment(int position) {
                            return mListener.getControllerProfile().toggleComment(position);
                        }

                        @Override
                        public void deleteComment(Comment comment) {
                            mListener.getControllerProfile().deleteComment(comment);
                        }

                        @Override
                        public void editComment(Comment comment, String text) {
                            mListener.getControllerProfile().editComment(comment, text);
                        }
                    },
                    new DisallowListener() {
                        @Override
                        public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                            recyclerProfile.requestDisallowInterceptTouchEvent(disallow);
                            swipeRefreshProfile.requestDisallowInterceptTouchEvent(disallow);
                        }

                        @Override
                        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

                        }
                    }, new RecyclerCallback() {
                @Override
                public void scrollTo(int position) {
                    linearLayoutManager.scrollToPositionWithOffset(0, 0);
                }

                @Override
                public int getRecyclerHeight() {
                    return recyclerProfile.getHeight();
                }
            }, listener);
        }

        recyclerProfile.setAdapter(adapterProfile);

        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
    public void onDestroy() {
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        item.setChecked(true);

        for (Sort sort : Sort.values()) {
            if (sort.getMenuId() == item.getItemId()) {
                mListener.getControllerProfile()
                        .setSort(sort);
                flashSearchView();
                return true;
            }
        }

        for (Time time : Time.values()) {
            if (time.getMenuId() == item.getItemId()) {
                mListener.getControllerProfile()
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
    boolean navigateBack() {
        return true;
    }
}
