/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

public class FragmentProfile extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentProfile.class.getCanonicalName();

    private FragmentListenerBase mListener;
    private Activity activity;
    private ControllerProfile.Listener listener;
    private SwipeRefreshLayout swipeRefreshProfile;
    private RecyclerView recyclerProfile;
    private LinearLayoutManager linearLayoutManager;
    private AdapterProfile adapterProfile;
    private MenuItem itemSearch;
    private Menu menu;
    private MenuItem itemSortTime;
    private Toolbar toolbar;
    private Spinner spinnerPage;
    private AdapterProfilePage adapterProfilePage;
    private Snackbar snackbar;

    public static FragmentProfile newInstance() {
        FragmentProfile fragment = new FragmentProfile();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentProfile() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        menu.findItem(R.id.item_sort_hot).setChecked(true);
        menu.findItem(R.id.item_sort_time).setTitle(
                getString(R.string.time) + Reddit.TIME_SEPARATOR + getString(R.string.item_sort_all));

        if (TextUtils.isEmpty(mListener.getControllerUser().getUser().getName()) && !mListener.getControllerProfile().isLoading()) {
            itemSearch.expandActionView();
        }

//        resetSubmenuSelected();
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

        final View view = inflater.inflate(R.layout.fragment_profile, container, false);

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
                Log.d(TAG, "Link ID: " + comment.getLinkId());
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

            @Override
            public void post(Runnable runnable) {
                recyclerProfile.post(runnable);
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
            adapterProfile = new AdapterProfile(mListener.getControllerProfile(),
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

                        @Override
                        public void sendComment(String name, String text) {
                            mListener.getControllerProfile().sendComment(name, text);
                        }

                        @Override
                        public void jumpToParent(Comment comment) {

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

                @Override
                public RecyclerView.LayoutManager getLayoutManager() {
                    return linearLayoutManager;
                }

            }, listener);
        }

        recyclerProfile.setAdapter(adapterProfile);

        CustomItemTouchHelper itemTouchHelper = new CustomItemTouchHelper(
                new CustomItemTouchHelper.SimpleCallback(ItemTouchHelper.START | ItemTouchHelper.END, ItemTouchHelper.START | ItemTouchHelper.END) {

                    @Override
                    public int getSwipeDirs(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder) {
                        int position = viewHolder.getAdapterPosition();
                        if (position == 2 || (position >= 6 && mListener.getControllerProfile().getViewType(position - 6) == ControllerProfile.VIEW_TYPE_LINK)) {
                            return super.getSwipeDirs(recyclerView, viewHolder);
                        }
                        return 0;
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return false;
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                        // Offset by 1 due to subreddit header
                        final int position = viewHolder.getAdapterPosition() == 2 ? 2 : viewHolder.getAdapterPosition() - 6;
                        final Link link = mListener.getControllerProfile().remove(position);
                        mListener.getEventListenerBase().hide(link);

                        if (snackbar != null) {
                            snackbar.dismiss();
                        }
                        snackbar = Snackbar.make(recyclerProfile, link.isHidden() ? R.string.link_hidden : R.string.link_shown,
                                Snackbar.LENGTH_LONG)
                                .setActionTextColor(getResources().getColor(R.color.colorAccent))
                                .setAction(
                                        R.string.undo, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mListener.getEventListenerBase().hide(link);
                                                mListener.getControllerProfile().add(position, link);
                                                recyclerProfile.invalidate();
                                            }
                                        });
                        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        snackbar.show();
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerProfile);

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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterProfile.pauseViewHolders();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeRefreshProfile.setRefreshing(mListener.getControllerProfile().isLoading());
    }

    @Override
    boolean navigateBack() {
        return true;
    }

    @Override
    public void onShown() {
        adapterProfile.setVisibility(View.VISIBLE);
    }

}
