/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.theme.ThemeWrapper;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.CustomItemTouchHelper;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.ItemDecorationDivider;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;

import java.util.Calendar;
import java.util.TimeZone;

import javax.inject.Inject;

import static android.support.v7.widget.RecyclerView.Adapter;
import static android.support.v7.widget.RecyclerView.LayoutManager;

public class FragmentHistory extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentHistory.class.getCanonicalName();
    private static final int FORMAT_DATE = DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR;
    private FragmentListenerBase mListener;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshHistory;
    private LayoutManager layoutManager;
    private RecyclerView recyclerHistory;
    private ControllerHistory.Listener listener;
    private CustomItemTouchHelper itemTouchHelper;
    private AdapterLink adapterLink;
    private Snackbar snackbar;
    private MenuItem itemInterface;
    private MenuItem itemSearch;
    private Menu menu;
    private SharedPreferences preferences;
    private AdapterLinkList adapterLinkList;
    private AdapterLinkGrid adapterLinkGrid;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private View view;
    private CustomColorFilter colorFilterPrimary;
    private ItemDecorationDivider itemDecorationDivider;

    @Inject ControllerHistory controllerHistory;

    public static FragmentHistory newInstance() {
        FragmentHistory fragment = new FragmentHistory();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentHistory() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_history);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        itemInterface = menu.findItem(R.id.item_interface);
        switch (preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)) {
            case AppSettings.MODE_LIST:
                itemInterface.setIcon(R.drawable.ic_view_module_white_24dp);
                break;
            case AppSettings.MODE_GRID:
                itemInterface.setIcon(R.drawable.ic_view_list_white_24dp);
                break;
        }

        itemSearch = menu.findItem(R.id.item_search);

        final SearchView searchView = (SearchView) itemSearch.getActionView();

        View view = searchView.findViewById(android.support.v7.appcompat.R.id.search_go_btn);
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(colorFilterPrimary);
        }

        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        controllerHistory.setQuery("");
                        Log.d(TAG, "collapse");
                        return true;
                    }
                });

        searchView.setQueryHint(getString(R.string.hint_title));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                controllerHistory.setQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                controllerHistory.setQuery(newText);
                return true;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }
    }

    @Override
    public void onDestroyOptionsMenu() {
        if (itemSearch != null) {
            MenuItemCompat.setOnActionExpandListener(itemSearch, null);
            itemSearch = null;
        }
        super.onDestroyOptionsMenu();
    }

    @Override
    protected void inject() {
        ((ActivityMain) getActivity()).getComponentActivity().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        listener = new ControllerHistory.Listener() {
            @Override
            public Adapter getAdapter() {
                return adapterLink;
            }

            @Override
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshHistory.setRefreshing(refreshing);
            }

            @Override
            public void post(Runnable runnable) {
                recyclerHistory.post(runnable);
            }
        };

        TypedArray typedArray = getActivity().getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new CustomColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);

        int styleColorBackground = AppSettings.THEME_DARK.equals(mListener.getThemeBackground()) ? R.style.MenuDark : R.style.MenuLight;

        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(new ThemeWrapper(getActivity(), UtilsColor.getThemeForColor(getResources(), colorPrimary, mListener)), styleColorBackground);

        toolbar = (Toolbar) getActivity().getLayoutInflater().cloneInContext(contextThemeWrapper).inflate(R.layout.toolbar, layoutAppBar, false);
        layoutAppBar.addView(toolbar);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams()).setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);

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

        swipeRefreshHistory = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_history);
        swipeRefreshHistory.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerHistory.reload();
            }
        });

        AdapterLink.ViewHolderHeader.EventListener eventListenerHeader = new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(Reddit.PostType postType) {

            }

            @Override
            public void showSidebar() {

            }
        };

        DisallowListener disallowListener = new DisallowListener() {
            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                swipeRefreshHistory.requestDisallowInterceptTouchEvent(disallow);
                recyclerHistory.requestDisallowInterceptTouchEvent(disallow);
                itemTouchHelper.select(null, CustomItemTouchHelper.ACTION_STATE_IDLE);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                itemTouchHelper.setDisallow(disallow);
            }
        };

        RecyclerCallback recyclerCallback = new RecyclerCallback() {

            @Override
            public int getRecyclerHeight() {
                return recyclerHistory.getHeight();
            }

            @Override
            public LayoutManager getLayoutManager() {
                return layoutManager;
            }

            @Override
            public void scrollTo(final int position) {
                UtilsAnimation.scrollToPositionWithCentering(position, recyclerHistory, false);
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
            public void clearDecoration() {
                AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar.getLayoutParams()).getBehavior();
                behaviorAppBar.onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
            }

            @Override
            public RequestManager getRequestManager() {
                return getGlideRequestManager();
            }
        };

        if (adapterLinkList == null) {
            adapterLinkList = new AdapterHistoryLinkList(getActivity(),
                    controllerHistory,
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }
        if (adapterLinkGrid == null) {
            adapterLinkGrid = new AdapterHistoryLinkGrid(getActivity(),
                    controllerHistory,
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }

        if (AppSettings.MODE_LIST.equals(preferences.getString(AppSettings.INTERFACE_MODE,
                AppSettings.MODE_GRID))) {
            adapterLink = adapterLinkList;
        }
        else {
            adapterLink = adapterLinkGrid;
        }

        layoutManager = adapterLink.getLayoutManager();

        itemDecorationDivider = new ItemDecorationDivider(getActivity(), ItemDecorationDivider.VERTICAL_LIST);

        recyclerHistory = (RecyclerView) view.findViewById(R.id.recycler_history);
        recyclerHistory.setHasFixedSize(true);
        recyclerHistory.setItemAnimator(null);
        resetAdapter(adapterLink);

        itemTouchHelper = new CustomItemTouchHelper(
                new CustomItemTouchHelper.SimpleCallback(getActivity(),
                        R.drawable.ic_visibility_off_white_24dp,
                        ItemTouchHelper.START | ItemTouchHelper.END,
                        ItemTouchHelper.START | ItemTouchHelper.END) {

                    @Override
                    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {

                        if (layoutManager instanceof StaggeredGridLayoutManager) {
                            return 1f / ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
                        }

                        return 0.4f;
                    }

                    @Override
                    public int getSwipeDirs(RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder) {

                        if (viewHolder.getAdapterPosition() == 0) {
                            return 0;
                        }

                        ViewGroup.LayoutParams layoutParams = viewHolder.itemView.getLayoutParams();

                        if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams &&
                                !((StaggeredGridLayoutManager.LayoutParams) layoutParams)
                                        .isFullSpan()) {

                            int spanCount = layoutManager instanceof StaggeredGridLayoutManager ?
                                    ((StaggeredGridLayoutManager) layoutManager).getSpanCount() : 2;
                            int spanIndex = ((StaggeredGridLayoutManager.LayoutParams) layoutParams)
                                    .getSpanIndex() % spanCount;
                            if (spanIndex == 0) {
                                return ItemTouchHelper.END;
                            }
                            else if (spanIndex == spanCount - 1) {
                                return ItemTouchHelper.START;
                            }

                        }

                        return super.getSwipeDirs(recyclerView, viewHolder);
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
                        final int position = viewHolder.getAdapterPosition();
                        final Link link = controllerHistory.remove(position);

                        if (snackbar != null) {
                            snackbar.dismiss();
                        }
                        SpannableString text = new SpannableString(getString(R.string.history_entry_deleted));
                        text.setSpan(new ForegroundColorSpan(colorFilterPrimary.getColor()), 0, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                        //noinspection ResourceType
                        snackbar = Snackbar.make(recyclerHistory, text,
                                UtilsAnimation.SNACKBAR_DURATION)
                                .setActionTextColor(colorFilterPrimary.getColor())
                                .setAction(
                                        R.string.undo, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                controllerHistory.add(position, link);
                                                recyclerHistory.invalidate();
                                            }
                                        });
                        snackbar.getView()
                                .setBackgroundColor(colorPrimary);
                        snackbar.show();
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerHistory);

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerHistory.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerHistory.setPadding(padding, 0, padding, 0);
        }

        return view;
    }

    private void scrollToPositionWithOffset(int position, int offset) {
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        controllerHistory.addListener(listener);
    }

    @Override
    public void onPause() {
        controllerHistory.removeListener(listener);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FragmentListenerBase");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterLink.pauseViewHolders();
            view.setVisibility(View.INVISIBLE);
        }
        else {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setVisibilityOfThing(int visibility, Thing thing) {
        super.setVisibilityOfThing(visibility, thing);
        adapterLink.setVisibility(visibility, thing);
    }

    @Override
    public void onShown() {
        adapterLink.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_interface:
                if (AppSettings.MODE_LIST.equals(
                        preferences.getString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID))) {
                    resetAdapter(adapterLinkGrid);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_list_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .apply();
                }
                else {
                    resetAdapter(adapterLinkList);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)
                            .apply();
                }
                item.getIcon().setColorFilter(colorFilterPrimary);
                return true;
            case R.id.item_time_range:
                showDateRangeDialog();

                return true;
        }
        return false;
    }

    private void showDateRangeDialog() {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_date_range, null, false);
        final DatePicker datePickerStart = (DatePicker) view.findViewById(R.id.date_picker_start);
        final DatePicker datePickerEnd = (DatePicker) view.findViewById(R.id.date_picker_end);
        final ViewPager viewPager = (ViewPager) view.findViewById(R.id.view_pager_time);
        viewPager.setAdapter(new PagerAdapter() {

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                switch (position) {
                    case 0:
                        return datePickerStart;
                    case 1:
                        return datePickerEnd;
                }

                return super.instantiateItem(container, position);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return viewPager.getContext().getResources().getString(R.string.start);
                    case 1:
                        return viewPager.getContext().getResources().getString(R.string.end);
                }
                return super.getPageTitle(position);
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });
        final TabLayout layoutTab = (TabLayout) view.findViewById(R.id.layout_tab);
        layoutTab.setupWithViewPager(viewPager);

        final Calendar calendarStart = Calendar.getInstance();
        calendarStart.setTimeZone(TimeZone.getTimeZone("UTC"));
        datePickerStart.init(calendarStart.get(Calendar.YEAR), calendarStart.get(Calendar.MONTH),
                calendarStart.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view,
                            int year,
                            int monthOfYear,
                            int dayOfMonth) {
                        calendarStart.set(year, monthOfYear, dayOfMonth);
                        layoutTab.getTabAt(0).setText(DateUtils
                                .formatDateTime(getActivity(), calendarStart.getTimeInMillis(),
                                        FORMAT_DATE));
                    }
                });
        long timeStart = controllerHistory.getTimeStart();
        if (timeStart > 0) {
            calendarStart.setTimeInMillis(timeStart);
            datePickerStart.updateDate(calendarStart.get(Calendar.YEAR), calendarStart.get(Calendar.MONTH), calendarStart.get(Calendar.DAY_OF_MONTH));
        }

        final Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.setTimeZone(TimeZone.getTimeZone("UTC"));
        datePickerEnd.init(calendarEnd.get(Calendar.YEAR), calendarEnd.get(Calendar.MONTH),
                calendarEnd.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view,
                            int year,
                            int monthOfYear,
                            int dayOfMonth) {
                        calendarEnd.set(year, monthOfYear, dayOfMonth);
                        layoutTab.getTabAt(1).setText(DateUtils
                                .formatDateTime(getActivity(), calendarEnd.getTimeInMillis(),
                                        FORMAT_DATE));
                    }
                });
        datePickerEnd.setMaxDate(System.currentTimeMillis() + AlarmManager.INTERVAL_DAY);

        long timeEnd = controllerHistory.getTimeEnd();
        if (timeEnd < Long.MAX_VALUE) {
            calendarEnd.setTimeInMillis(timeEnd);
            calendarEnd.add(Calendar.DAY_OF_MONTH, -1);
            datePickerEnd.updateDate(calendarEnd.get(Calendar.YEAR),
                    calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH));
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Calendar calendarStart = Calendar.getInstance();
                        calendarStart.set(datePickerStart.getYear(), datePickerStart.getMonth(),
                                datePickerStart.getDayOfMonth());
                        calendarStart.setTimeZone(
                                TimeZone.getTimeZone("UTC"));

                        Calendar calendarEnd = Calendar.getInstance();
                        calendarEnd.set(datePickerEnd.getYear(), datePickerEnd.getMonth(),
                                datePickerEnd.getDayOfMonth());
                        calendarEnd.setTimeZone(
                                TimeZone.getTimeZone("UTC"));
                        calendarEnd.add(Calendar.DAY_OF_MONTH, 1);

                        controllerHistory.setTimeStart(
                                calendarStart.getTimeInMillis());

                        controllerHistory.setTimeEnd(
                                calendarEnd.getTimeInMillis());

                        controllerHistory.reload();
                    }
                })
                .setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        controllerHistory.setTimeStart(0);
                        controllerHistory.setTimeEnd(Long.MAX_VALUE);
                        controllerHistory.reload();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();

    }

    private void resetAdapter(AdapterLink newAdapter) {

        RecyclerView.LayoutManager layoutManagerCurrent = recyclerHistory.getLayoutManager();

        int size = layoutManagerCurrent instanceof StaggeredGridLayoutManager ? ((StaggeredGridLayoutManager) layoutManagerCurrent).getSpanCount() : 1;

        int[] currentPosition = new int[size];
        if (layoutManagerCurrent instanceof LinearLayoutManager) {
            currentPosition[0] = ((LinearLayoutManager) layoutManagerCurrent)
                    .findFirstVisibleItemPosition();
        }
        else if (layoutManagerCurrent instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManagerCurrent).findFirstCompletelyVisibleItemPositions(
                    currentPosition);
        }

        this.adapterLink = newAdapter;
        this.layoutManager = adapterLink.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerHistory.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerHistory.setPadding(padding, 0, padding, 0);
        }

        /*
            Note that we must call setAdapter before setLayoutManager or the ViewHolders
            will not be properly recycled, leading to memory leaks.
         */
        recyclerHistory.setAdapter(adapterLink);
        recyclerHistory.setLayoutManager(layoutManager);
        recyclerHistory.scrollToPosition(currentPosition[0]);
        if (layoutManager instanceof LinearLayoutManager) {
            recyclerHistory.addItemDecoration(itemDecorationDivider);
        }
        else {
            recyclerHistory.removeItemDecoration(itemDecorationDivider);
        }
    }

}
