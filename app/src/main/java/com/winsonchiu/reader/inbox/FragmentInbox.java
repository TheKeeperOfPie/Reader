/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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

import com.bumptech.glide.RequestManager;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.FragmentNewMessage;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.FinalizingSubscriber;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.ScrollAwareFloatingActionButtonBehavior;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;

import javax.inject.Inject;

import rx.Observable;

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
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterAccent;

    @Inject ControllerInbox controllerInbox;

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

    @Override
    protected View onCreateViewInternal(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorAccent});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorAccent = typedArray.getColor(1, getResources().getColor(R.color.colorAccent));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceAccent = UtilsColor.computeContrast(colorAccent, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        int styleToolbar = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? mListener.getAppColorTheme().getStyle(AppSettings.THEME_DARK, mListener.getThemeAccentPrefString()) : mListener.getAppColorTheme().getStyle(AppSettings.THEME_LIGHT, mListener.getThemeAccentPrefString());

        int styleColorBackground = AppSettings.THEME_DARK.equals(mListener.getThemeBackgroundPrefString()) ? R.style.MenuDark : R.style.MenuLight;

        colorFilterPrimary = new PorterDuffColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterAccent = new PorterDuffColorFilter(getResources().getColor(colorResourceAccent), PorterDuff.Mode.MULTIPLY);

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ContextThemeWrapper(activity, styleToolbar), styleColorBackground);

        toolbar = (Toolbar) activity.getLayoutInflater().cloneInContext(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);
        layoutAppBar.addView(toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
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
        floatingActionButtonNewMessage.setColorFilter(colorFilterAccent);

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

        if (adapterInbox == null) {
            adapterInbox = new AdapterInbox(controllerInbox,
                    mListener.getEventListenerBase(),
                    new AdapterCommentList.ViewHolderComment.EventListenerComment() {
                        @Override
                        public void loadNestedComments(Comment comment) {
                            controllerInbox.loadNestedComments(comment);
                        }

                        @Override
                        public Observable<String> voteComment(AdapterCommentList.ViewHolderComment viewHolderComment,
                                Comment comment,
                                int vote) {
                            return controllerInbox.voteComment(viewHolderComment, comment, vote);
                        }

                        @Override
                        public boolean toggleComment(Comment comment) {
                            return true;
                        }

                        @Override
                        public Observable<String> deleteComment(Comment comment) {
                            return controllerInbox.deleteComment(comment);
                        }

                        @Override
                        public void editComment(String name, int level, String text) {
                            controllerInbox.editComment(name, level, text);
                        }

                        @Override
                        public void jumpToParent(Comment comment) {

                        }

                        @Override
                        public String getLinkId() {
                            return "";
                        }

                        @Override
                        public String getSubredditName() {
                            return "";
                        }
                    },
                    mListener.getEventListener(),
                    new DisallowListener() {
                        @Override
                        public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                            recyclerInbox.requestDisallowInterceptTouchEvent(disallow);
                            swipeRefreshInbox.requestDisallowInterceptTouchEvent(disallow);
                        }

                        @Override
                        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

                        }
                    }, new RecyclerCallback() {
                @Override
                public int getRecyclerHeight() {
                    return recyclerInbox.getHeight();
                }

                @Override
                public RecyclerView.LayoutManager getLayoutManager() {
                    return linearLayoutManager;
                }

                @Override
                public void scrollTo(int position) {
                    linearLayoutManager.scrollToPositionWithOffset(position, 0);
                }

                @Override
                public void scrollAndCenter(int position, int height) {

                }

                @Override
                public void hideToolbar() {
                    AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                    behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
                }

                @Override
                public void onReplyShown() {
                    behaviorFloatingActionButton.animateOut(floatingActionButtonNewMessage);
                }

                @Override
                public RequestManager getRequestManager() {
                    return getGlideRequestManager();
                }
            }, new ControllerProfile.Listener() {
                @Override
                public void setSortAndTime(Sort sort, Time time) {

                }

                @Override
                public void setPage(Page page) {

                }

                @Override
                public void setIsUser(boolean isUser) {

                }

                @Override
                public void loadLink(Comment comment) {
                    Intent intent = new Intent(activity, ActivityMain.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(ActivityMain.REDDIT_PAGE,
                            Reddit.BASE_URL + comment.getContext());
                    startActivity(intent);
                }

                @Override
                public RecyclerView.Adapter getAdapter() {
                    return null;
                }

                @Override
                public void setToolbarTitle(CharSequence title) {

                }

                @Override
                public void setRefreshing(boolean refreshing) {

                }

                @Override
                public void post(Runnable runnable) {
                    recyclerInbox.post(runnable);
                }
            });
        }

        recyclerInbox.setAdapter(adapterInbox);

        return view;
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_inbox);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
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
