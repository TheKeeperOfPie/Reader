/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.theme.ThemeWrapper;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;

import javax.inject.Inject;

public class FragmentInbox extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentInbox.class.getCanonicalName();

    private Activity activity;
    private FragmentListenerBase mListener;
    private SwipeRefreshLayout swipeRefreshInbox;
    private RecyclerView recyclerInbox;
    private LinearLayoutManager linearLayoutManager;
    private AdapterInbox adapterInbox;
    private ControllerInbox.Listener listener;
    private Toolbar toolbar;
    private FloatingActionButton floatingActionButtonNewMessage;
    private Spinner spinnerPage;
    private AdapterInboxPage adapterInboxPage;
    private ScrollAwareFloatingActionButtonBehavior behaviorFloatingActionButton;
    private Menu menu;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;

    @Inject ControllerInbox controllerInbox;
    @Inject ControllerUser controllerUser;

    public static FragmentInbox newInstance() {
        FragmentInbox fragment = new FragmentInbox();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentInbox() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @Override
    protected void inject() {
        ((ActivityMain) getActivity()).getComponentActivity().inject(this);
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        listener = new ControllerInbox.Listener() {
            @Override
            public void setPage(Page page) {
                spinnerPage.setSelection(adapterInboxPage.getPages().indexOf(page));
            }

            @Override
            public RecyclerView.Adapter getAdapter() {
                return adapterInbox;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshInbox.setRefreshing(refreshing);
            }

            @Override
            public void post(Runnable runnable) {
                recyclerInbox.post(runnable);
            }
        };

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        int styleColorBackground = AppSettings.THEME_DARK.equals(mListener.getThemeBackground()) ? R.style.MenuDark : R.style.MenuLight;

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ThemeWrapper(activity, UtilsColor.getThemeForColor(getResources(), themer.getColorPrimary(), mListener)), styleColorBackground);

        toolbar = (Toolbar) activity.getLayoutInflater().cloneInContext(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);
        layoutAppBar.addView(toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        toolbar.setTitleTextColor(themer.getColorFilterPrimary().getColor());
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.getNavigationIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.openDrawer();
            }
        });
        setUpOptionsMenu();

        floatingActionButtonNewMessage = (FloatingActionButton) view
                .findViewById(R.id.fab_new_message);
        floatingActionButtonNewMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentNewMessage fragmentNewMessage = FragmentNewMessage.newInstance();
                getFragmentManager().beginTransaction()
                        .hide(FragmentInbox.this)
                        .add(R.id.frame_fragment, fragmentNewMessage, FragmentNewMessage.TAG)
                        .addToBackStack(null)
                        .commit();
            }
        });
        floatingActionButtonNewMessage.setColorFilter(themer.getColorFilterAccent());

        behaviorFloatingActionButton = new ScrollAwareFloatingActionButtonBehavior(activity, null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                    }

                    @Override
                    public void onEndHideFromScroll() {
                    }

                });
        ((CoordinatorLayout.LayoutParams) floatingActionButtonNewMessage.getLayoutParams())
                .setBehavior(behaviorFloatingActionButton);

        adapterInboxPage = new AdapterInboxPage(activity);
        spinnerPage = new AppCompatSpinner(contextThemeWrapper);
        toolbar.addView(spinnerPage);
        ((Toolbar.LayoutParams) spinnerPage.getLayoutParams()).setMarginEnd((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        spinnerPage.setAdapter(adapterInboxPage);
        spinnerPage.setSelection(
                adapterInboxPage.getPages().indexOf(controllerInbox.getPage()));
        spinnerPage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                controllerInbox.setPage(adapterInboxPage.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swipeRefreshInbox = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_inbox);
        swipeRefreshInbox.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerInbox.reload();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerInbox = (RecyclerView) view.findViewById(R.id.recycler_inbox);
        recyclerInbox.setHasFixedSize(true);
        recyclerInbox.setItemAnimator(new DefaultItemAnimator());
        recyclerInbox.getItemAnimator()
                .setRemoveDuration(UtilsAnimation.EXPAND_ACTION_DURATION);
        recyclerInbox.setLayoutManager(linearLayoutManager);
        recyclerInbox.addItemDecoration(new ItemDecorationDivider(activity, ItemDecorationDivider.VERTICAL_LIST));

        AdapterListener adapterListener = new AdapterListener() {

            @Override
            public void scrollAndCenter(int position, int height) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0);
            }

            @Override
            public void hideToolbar() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void clearDecoration() {
                behaviorFloatingActionButton.animateOut(floatingActionButtonNewMessage);
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public void requestMore() {
                controllerInbox.loadMore();
            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                recyclerInbox.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshInbox.requestDisallowInterceptTouchEvent(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

            }
        };

        AdapterCommentList.ViewHolderComment.Listener listenerComments = new AdapterCommentList.ViewHolderComment.Listener() {
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

        if (adapterInbox == null) {
            adapterInbox = new AdapterInbox(controllerInbox,
                    controllerUser,
                    adapterListener,
                    listenerComments,
                    mListener.getEventListenerBase());
        }

        recyclerInbox.setAdapter(adapterInbox);

        return view;
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_inbox);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(themer.getColorFilterPrimary());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        controllerInbox.addListener(listener);
    }

    @Override
    public void onPause() {
        controllerInbox.removeListener(listener);
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
    public void onShown() {
        adapterInbox.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_mark_all_read:
                controllerInbox.markAllRead()
                        .subscribe(new FinalizingSubscriber<String>() {
                            @Override
                            public void next(String next) {
                                Toast.makeText(activity, R.string.marked_read, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void error(Throwable e) {
                                Toast.makeText(activity, R.string.error_marking_read, Toast.LENGTH_LONG).show();
                            }
                        });
                return true;
        }

        return false;
    }
}
