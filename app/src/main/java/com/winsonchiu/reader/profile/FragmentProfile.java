/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
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

import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.views.CustomItemTouchHelper;

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
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private Spinner spinnerPage;
    private AdapterProfilePage adapterProfilePage;
    private Snackbar snackbar;
    private CustomItemTouchHelper itemTouchHelper;
    private PorterDuffColorFilter colorFilterIcon;
    private CustomItemTouchHelper.SimpleCallback callback;
    private View view;

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
                getString(R.string.time) + Reddit.TIME_SEPARATOR + getString(
                        R.string.item_sort_all));

        if (TextUtils.isEmpty(mListener.getControllerUser().getUser().getName()) && !mListener
                .getControllerProfile().isLoading()) {
            itemSearch.expandActionView();
        }

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().setColorFilter(colorFilterIcon);
        }

    }

    @Override
    public void onDestroyOptionsMenu() {
        if (itemSearch != null) {
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            itemSearch = null;
        }
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

        view = inflater.inflate(R.layout.fragment_profile, container, false);

        listener = new ControllerProfile.Listener() {
            @Override
            public void setSortAndTime(Sort sort, Time time) {
                menu.findItem(sort.getMenuId()).setChecked(true);
                itemSortTime.setTitle(
                        getString(R.string.time) + Reddit.TIME_SEPARATOR + menu
                                .findItem(mListener.getControllerLinks()
                                        .getTime().getMenuId()).toString());
            }

            @Override
            public void setPage(Page page) {
                spinnerPage.setSelection(adapterProfilePage.getPages().indexOf(page));
                if (page.getPage().equals(ControllerProfile.PAGE_HIDDEN)) {
                    callback.setDrawable(
                            getResources().getDrawable(R.drawable.ic_visibility_white_24dp));
                }
                else {
                    callback.setDrawable(
                            getResources().getDrawable(R.drawable.ic_visibility_off_white_24dp));
                }
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
                        Reddit.BASE_URL + "/r/" + comment.getSubreddit() + "/comments/" + comment
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

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[] {R.attr.colorPrimary, R.attr.colorIconFilter});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorIconFilter = typedArray.getColor(1, 0xFFFFFFFF);
        typedArray.recycle();

        colorFilterIcon = new PorterDuffColorFilter(colorIconFilter,
                PorterDuff.Mode.MULTIPLY);

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
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterIcon);
        setUpOptionsMenu();

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

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
        recyclerProfile.setItemAnimator(null);
        recyclerProfile.setLayoutManager(linearLayoutManager);

        if (adapterProfile == null) {
            adapterProfile = new AdapterProfile(mListener.getControllerProfile(),
                    mListener.getControllerLinks(),
                    mListener.getEventListenerBase(),
                    new AdapterCommentList.ViewHolderComment.EventListener() {
                        @Override
                        public void loadNestedComments(Comment comment) {
                            mListener.getControllerProfile().loadNestedComments(comment);
                        }

                        @Override
                        public void voteComment(AdapterCommentList.ViewHolderComment viewHolderComment,
                                Comment comment,
                                int vote) {
                            mListener.getControllerProfile()
                                    .voteComment(viewHolderComment, comment, vote);
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
                        public void editComment(String name, int level, String text) {
                            mListener.getControllerProfile().editComment(name, level, text);
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
                            itemTouchHelper.select(null, CustomItemTouchHelper.ACTION_STATE_IDLE);
                        }

                        @Override
                        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                            itemTouchHelper.setDisallow(disallow);
                        }
                    }, new RecyclerCallback() {
                @Override
                public void scrollTo(int position) {
                    linearLayoutManager.scrollToPositionWithOffset(position, 0);
                }

                @Override
                public int getRecyclerHeight() {
                    return recyclerProfile.getHeight();
                }

                @Override
                public RecyclerView.LayoutManager getLayoutManager() {
                    return linearLayoutManager;
                }

                @Override
                public void hideToolbar() {
                    AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                    behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
                }

                @Override
                public void onReplyShown() {

                }

            }, listener);
        }

        recyclerProfile.setAdapter(adapterProfile);

        callback = new CustomItemTouchHelper.SimpleCallback(activity,
                R.drawable.ic_delete_white_24dp,
                ItemTouchHelper.START | ItemTouchHelper.END,
                ItemTouchHelper.START | ItemTouchHelper.END) {

            @Override
            public int getSwipeDirs(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position == 2 || (position >= 6 && mListener.getControllerProfile()
                        .getViewType(position - 6) == ControllerProfile.VIEW_TYPE_LINK)) {
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

                Log.d(TAG, "onSwiped: " + viewHolder.getAdapterPosition());

                final int adapterPosition = viewHolder.getAdapterPosition();
                final int position = adapterPosition == 2 ? -1 : adapterPosition - 6;
                final Link link =
                        adapterPosition == 2 ? mListener.getControllerProfile().remove(
                                -1) : mListener.getControllerProfile().remove(position);
                mListener.getEventListenerBase().hide(link);

                if (snackbar != null) {
                    snackbar.dismiss();
                }
                snackbar = Snackbar.make(recyclerProfile,
                        link.isHidden() ? R.string.link_hidden : R.string.link_shown,
                        Snackbar.LENGTH_LONG)
                        .setActionTextColor(getResources().getColor(R.color.colorAccent))
                        .setAction(
                                R.string.undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mListener.getEventListenerBase().hide(link);
                                        if (adapterPosition == 2) {
                                            mListener.getControllerProfile()
                                                    .setTopLink(link);
                                            adapterProfile.notifyItemChanged(2);
                                        }
                                        else {
                                            mListener.getControllerProfile()
                                                    .add(position, link);
                                        }
                                        recyclerProfile.invalidate();
                                    }
                                });
                snackbar.getView()
                        .setBackgroundColor(colorPrimary);
                snackbar.show();
            }
        };

        itemTouchHelper = new CustomItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerProfile);

        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();
        mListener.getControllerProfile()
                .addListener(listener);
    }

    @Override
    public void onPause() {
        mListener.getControllerProfile()
                .removeListener(listener);
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
        super.onDetach();
        mListener = null;
        activity = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        item.setChecked(true);


        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            mListener.getControllerProfile()
                    .setSort(sort);
            flashSearchView();
            return true;
        }

        Time time = Time.fromMenuId(item.getItemId());
        if (time != null) {
            mListener.getControllerProfile()
                    .setTime(time);
            itemSortTime.setTitle(
                    getString(R.string.time) + Reddit.TIME_SEPARATOR + item.toString());
            flashSearchView();
            return true;
        }

        return false;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterProfile.pauseViewHolders();
            view.setVisibility(View.INVISIBLE);
        }
        else {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setVisibilityOfThing(int visibility, Thing thing) {
        super.setVisibilityOfThing(visibility, thing);
        adapterProfile.setVisibility(visibility, thing);
    }

    @Override
    public boolean navigateBack() {
        return true;
    }

    @Override
    public void onShown() {
        adapterProfile.setVisibility(View.VISIBLE);
    }

}
