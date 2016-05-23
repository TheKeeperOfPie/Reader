/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.FragmentNewMessage;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverError;
import com.winsonchiu.reader.theme.ThemeWrapper;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.CustomItemTouchHelper;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;

import javax.inject.Inject;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

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
    private CustomItemTouchHelper.SimpleCallback callback;
    private View view;
    private CustomColorFilter colorFilterPrimary;

    @Inject ControllerUser controllerUser;
    @Inject ControllerProfile controllerProfile;

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

        View view = searchView.findViewById(android.support.v7.appcompat.R.id.search_go_btn);
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(colorFilterPrimary);
        }

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
                controllerProfile.loadUser(query.replaceAll("\\s", ""))
                        .subscribe(new FinalizingSubscriber<User>() {
                            @Override
                            public void error(Throwable e) {
                                Toast.makeText(activity, getString(R.string.error_loading), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
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
                getString(R.string.time_description, getString(
                        R.string.item_sort_all)));

        if (TextUtils.isEmpty(controllerUser.getUser().getName()) && !controllerProfile.isLoading()) {
            itemSearch.expandActionView();
        }

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
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
    protected void inject() {
        ((ActivityMain) activity).getComponentActivity().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        listener = new ControllerProfile.Listener() {
            @Override
            public void setSortAndTime(Sort sort, Time time) {
                menu.findItem(sort.getMenuId()).setChecked(true);
                menu.findItem(time.getMenuId()).setChecked(true);
                itemSortTime.setTitle(
                        getString(R.string.time_description, menu.findItem(controllerProfile.getTime().getMenuId()).toString()));
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
                Intent intent = new Intent(activity, ActivityMain.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(ActivityMain.REDDIT_PAGE,
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
                new int[]{R.attr.colorPrimary});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        int styleColorBackground = AppSettings.THEME_DARK.equals(mListener.getThemeBackground()) ? R.style.MenuDark : R.style.MenuLight;

        colorFilterPrimary = new CustomColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ThemeWrapper(activity, UtilsColor.getThemeForColor(getResources(), colorPrimary, mListener)), styleColorBackground);

        toolbar = (Toolbar) activity.getLayoutInflater().cloneInContext(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);
        layoutAppBar.addView(toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);

        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
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
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        setUpOptionsMenu();

        adapterProfilePage = new AdapterProfilePage(activity);
        spinnerPage = new AppCompatSpinner(contextThemeWrapper);
        toolbar.addView(spinnerPage);
        ((Toolbar.LayoutParams) spinnerPage.getLayoutParams()).setMarginEnd((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        spinnerPage.setAdapter(adapterProfilePage);
        spinnerPage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                controllerProfile.setPage(adapterProfilePage.getItem(position))
                        .subscribe(getReloadObserver());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swipeRefreshProfile = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_profile);
        swipeRefreshProfile.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerProfile.reload().subscribe(getReloadObserver());
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerProfile = (RecyclerView) view.findViewById(R.id.recycler_profile);
        recyclerProfile.setHasFixedSize(true);
        recyclerProfile.setItemAnimator(null);
        recyclerProfile.setLayoutManager(linearLayoutManager);
        recyclerProfile.addItemDecoration(new ItemDecorationDivider(activity, ItemDecorationDivider.VERTICAL_LIST));

        AdapterListener adapterListener = new AdapterListener() {

            @Override
            public void scrollAndCenter(int position, int height) {
                UtilsAnimation.scrollToPositionWithCentering(position, recyclerProfile, false);
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
            public void requestMore() {
                controllerProfile.loadMoreLinks()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new ObserverError<Listing>() {
                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(getContext(), getString(R.string.error_loading_links), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

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
        };

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
            public void onVote(Link link, AdapterLink.ViewHolderLink viewHolderLink, int vote) {

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

        AdapterCommentList.ViewHolderComment.Listener listenerComments = new AdapterCommentList.ViewHolderComment.Listener() {
            @Override
            public void onClickComments() {

            }

            @Override
            public void onToggleComment(Comment comment) {

            }

            @Override
            public void onShowReplyEditor(Comment comment) {

            }

            @Override
            public void onEditComment(Comment comment, String text) {

            }

            @Override
            public void onSendComment(Comment comment, String text) {

            }

            @Override
            public void onMarkRead(Comment comment) {

            }

            @Override
            public void onLoadNestedComments(Comment comment) {

            }

            @Override
            public void onJumpToParent(Comment comment) {

            }

            @Override
            public void onViewProfile(Comment comment) {

            }

            @Override
            public void onCopyText(Comment comment) {

            }

            @Override
            public void onDeleteComment(Comment comment) {

            }

            @Override
            public void onReport(Comment comment) {

            }

            @Override
            public void onVoteComment(Comment comment, AdapterCommentList.ViewHolderComment viewHolderComment, int vote) {

            }

            @Override
            public void onSave(Comment comment) {

            }
        };

        if (adapterProfile == null) {
            adapterProfile = new AdapterProfile(getActivity(),
                    controllerProfile,
                    adapterListener,
                    listenerLink,
                    listenerComments,
                    listener);
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
                if (position == 2 || (position >= 6 && controllerProfile
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
                        adapterPosition == 2 ? controllerProfile.remove(
                                -1) : controllerProfile.remove(position);
                mListener.getEventListenerBase().hide(link);

                if (snackbar != null) {
                    snackbar.dismiss();
                }

                SpannableString text = new SpannableString(link.isHidden() ? getString(R.string.link_hidden) : getString(R.string.link_shown));
                text.setSpan(new ForegroundColorSpan(colorFilterPrimary.getColor()), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                //noinspection ResourceType
                snackbar = Snackbar.make(recyclerProfile, text,
                        UtilsAnimation.SNACKBAR_DURATION)
                        .setActionTextColor(colorFilterPrimary.getColor())
                        .setAction(
                                R.string.undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mListener.getEventListenerBase().hide(link);
                                        if (adapterPosition == 2) {
                                            controllerProfile.setTopLink(link);
                                            adapterProfile.notifyItemChanged(2);
                                        }
                                        else {
                                            controllerProfile.add(position, link);
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

    private Observer<Listing> getReloadObserver() {
        return new FinalizingSubscriber<Listing>() {
            @Override
            public void error(Throwable e) {
                Toast.makeText(activity, getString(R.string.error_loading),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();
        controllerProfile.addListener(listener);
    }

    @Override
    public void onPause() {
        controllerProfile.removeListener(listener);
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
        int id = item.getItemId();

        switch (id) {
            case R.id.item_new_message:
                FragmentNewMessage fragmentNewMessage = FragmentNewMessage.newInstance(controllerProfile.getUser().getName(), "", "");
                getFragmentManager().beginTransaction()
                        .hide(FragmentProfile.this)
                        .add(R.id.frame_fragment, fragmentNewMessage, FragmentNewMessage.TAG)
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.item_search:
                return true;
        }

        item.setChecked(true);

        Sort sort = Sort.fromMenuId(item.getItemId());
        if (sort != null) {
            controllerProfile.setSort(sort)
                    .subscribe(getReloadObserver());
            flashSearchView();
            return true;
        }

        Time time = Time.fromMenuId(item.getItemId());
        if (time != null) {
            controllerProfile.setTime(time)
                    .subscribe(getReloadObserver());
            itemSortTime.setTitle(
                    getString(R.string.time_description, item.toString()));
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
    public void onShown() {
        adapterProfile.setVisibility(View.VISIBLE);
    }

}
