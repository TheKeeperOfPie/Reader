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
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.views.CustomItemTouchHelper;

import java.util.Calendar;
import java.util.TimeZone;

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
    private Activity activity;
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
    private PorterDuffColorFilter colorFilterIcon;

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

        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        mListener.getControllerHistory().setQuery("");
                        Log.d(TAG, "collapse");
                        return true;
                    }
                });

        searchView.setQueryHint(getString(R.string.hint_title));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mListener.getControllerHistory().setQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mListener.getControllerHistory().setQuery(newText);
                return true;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().setColorFilter(colorFilterIcon);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);


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

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
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

        swipeRefreshHistory = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_history);
        swipeRefreshHistory.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerHistory().reload();
            }
        });

        AdapterLink.ViewHolderHeader.EventListener eventListenerHeader = new AdapterLink.ViewHolderHeader.EventListener() {
            @Override
            public void onClickSubmit(String postType) {

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
            public void scrollTo(int position) {
                if (layoutManager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, 0);
                }
                else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager) layoutManager)
                            .scrollToPositionWithOffset(position, 0);
                }
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerHistory.getHeight();
            }

            @Override
            public LayoutManager getLayoutManager() {
                return layoutManager;
            }
        };


        if (adapterLinkList == null) {
            adapterLinkList = new AdapterHistoryLinkList(activity, mListener.getControllerHistory(),
                    mListener.getControllerUser(),
                    eventListenerHeader,
                    mListener.getEventListenerBase(),
                    disallowListener,
                    recyclerCallback);
        }
        if (adapterLinkGrid == null) {
            adapterLinkGrid = new AdapterHistoryLinkGrid(activity, mListener.getControllerHistory(),
                    mListener.getControllerUser(),
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

        recyclerHistory = (RecyclerView) view.findViewById(R.id.recycler_history);
        recyclerHistory.setHasFixedSize(true);
        recyclerHistory.setItemAnimator(null);
        recyclerHistory.setLayoutManager(layoutManager);
        recyclerHistory.setAdapter(adapterLink);

        itemTouchHelper = new CustomItemTouchHelper(
                new CustomItemTouchHelper.SimpleCallback(
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
                        final Link link = mListener.getControllerHistory().remove(position);

                        if (snackbar != null) {
                            snackbar.dismiss();
                        }
                        snackbar = Snackbar.make(recyclerHistory, R.string.history_entry_deleted,
                                Snackbar.LENGTH_LONG)
                                .setActionTextColor(getResources().getColor(R.color.colorAccent))
                                .setAction(
                                        R.string.undo, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mListener.getControllerHistory()
                                                        .add(position, link);
                                                recyclerHistory.invalidate();
                                            }
                                        });
                        snackbar.getView()
                                .setBackgroundColor(getResources().getColor(R.color.colorPrimary));
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

    @Override
    public void onResume() {
        super.onResume();
        mListener.getControllerHistory().addListener(listener);
    }

    @Override
    public void onPause() {
        mListener.getControllerHistory().removeListener(listener);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
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
        activity = null;
        mListener = null;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            adapterLink.pauseViewHolders();
        }
    }

    @Override
    public boolean navigateBack() {
        return true;
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
                    item.getIcon().setColorFilter(colorFilterIcon);
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_GRID)
                            .commit();
                }
                else {
                    resetAdapter(adapterLinkList);
                    item.setIcon(getResources().getDrawable(R.drawable.ic_view_module_white_24dp));
                    item.getIcon().setColorFilter(colorFilterIcon);
                    preferences.edit()
                            .putString(AppSettings.INTERFACE_MODE, AppSettings.MODE_LIST)
                            .commit();
                }
                return true;
            case R.id.item_time_range:
                showDateRangeDialog();

                return true;
        }
        return false;
    }

    private void showDateRangeDialog() {

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_date_range, null, false);
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
                                .formatDateTime(activity, calendarStart.getTimeInMillis(),
                                        FORMAT_DATE));
                    }
                });
        long timeStart = mListener.getControllerHistory().getTimeStart();
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
                                .formatDateTime(activity, calendarEnd.getTimeInMillis(),
                                        FORMAT_DATE));
                    }
                });
        datePickerEnd.setMaxDate(System.currentTimeMillis() + AlarmManager.INTERVAL_DAY);

        long timeEnd = mListener.getControllerHistory().getTimeEnd();
        if (timeEnd < Long.MAX_VALUE) {
            calendarEnd.setTimeInMillis(timeEnd);
            calendarEnd.add(Calendar.DAY_OF_MONTH, -1);
            datePickerEnd.updateDate(calendarEnd.get(Calendar.YEAR),
                    calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH));
        }


        AlertDialog dialog = new AlertDialog.Builder(activity)
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

                        mListener.getControllerHistory().setTimeStart(
                                calendarStart.getTimeInMillis());

                        mListener.getControllerHistory().setTimeEnd(
                                calendarEnd.getTimeInMillis());

                        mListener.getControllerHistory().reload();
                    }
                })
                .setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.getControllerHistory().setTimeStart(0);
                        mListener.getControllerHistory().setTimeEnd(Long.MAX_VALUE);
                        mListener.getControllerHistory().reload();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();

    }

    private void resetAdapter(AdapterLink newAdapter) {

        int[] currentPosition = new int[3];
        if (layoutManager instanceof LinearLayoutManager) {
            currentPosition[0] = ((LinearLayoutManager) layoutManager)
                    .findFirstVisibleItemPosition();
        }
        else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).findFirstCompletelyVisibleItemPositions(
                    currentPosition);
        }

        adapterLink = newAdapter;
        layoutManager = adapterLink.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            recyclerHistory.setPadding(0, 0, 0, 0);
        }
        else {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    getResources().getDisplayMetrics());
            recyclerHistory.setPadding(padding, 0, padding, 0);
        }

        recyclerHistory.setLayoutManager(layoutManager);
        recyclerHistory.setAdapter(adapterLink);
        recyclerHistory.scrollToPosition(currentPosition[0]);
    }
}