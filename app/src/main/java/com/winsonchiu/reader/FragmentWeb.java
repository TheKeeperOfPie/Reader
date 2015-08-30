/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.winsonchiu.reader.utils.UtilsColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FragmentWeb extends FragmentBase implements Toolbar.OnMenuItemClickListener,
        View.OnClickListener {

    private static final String ARG_URL = "url";
    public static final String TAG = FragmentWeb.class.getCanonicalName();

    private String url;

    private FragmentListenerBase mListener;
    private WebView webView;
    private Activity activity;
    private MenuItem itemSearch;
    private Toolbar toolbarActions;
    private Toolbar toolbar;
    private Menu menu;
    private ProgressBar progressBar;
    private View viewWebFullscreen;
    private RelativeLayout layoutRoot;
    private boolean isFinished;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterIcon;
    private List<ResolveInfo> listDefaultWebResolveInfo = new ArrayList<>();

    public static FragmentWeb newInstance(String url) {
        FragmentWeb fragment = new FragmentWeb();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentWeb() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);
        }
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_web);
        menu = toolbar.getMenu();
        itemSearch = menu.findItem(R.id.item_search_web);

        MenuItemCompat.setOnActionExpandListener(itemSearch,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        toolbarActions.setVisibility(View.VISIBLE);
                        Log.d(TAG, "onMenuItemActionExpand");
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        toolbarActions.setVisibility(View.GONE);
                        Log.d(TAG, "onMenuItemActionCollapse");
                        return true;
                    }
                });

        SearchView searchView = (SearchView) itemSearch.getActionView();

        searchView.setQueryHint(getString(R.string.search_in_page));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                webView.findAllAsync(newText);
                return false;
            }
        });

        toolbar.setOnMenuItemClickListener(this);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }
    }

    @Override
    public void onDestroyOptionsMenu() {
        if (itemSearch != null) {
            SearchView searchView = (SearchView) itemSearch.getActionView();
            searchView.setOnQueryTextListener(null);
            MenuItemCompat.setOnActionExpandListener(itemSearch, null);
            itemSearch = null;
        }
        toolbar.setOnMenuItemClickListener(null);
        toolbarActions.setOnMenuItemClickListener(null);
        super.onDestroyOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_web, container, false);

        layoutRoot = (RelativeLayout) view;

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorIconFilter});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorIcon = typedArray.getColor(1, getResources().getColor(R.color.darkThemeIconFilter));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new PorterDuffColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterIcon = new PorterDuffColorFilter(colorIcon, PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.loading_web_page));
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        toolbar.setOnClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFinished = true;
                mListener.onNavigationBackClick();
            }
        });
        setUpOptionsMenu();

        progressBar = (ProgressBar) view.findViewById(R.id.progress_web);

        webView = (WebView) view.findViewById(R.id.web);
        webView.setBackgroundColor(getResources().getColor(R.color.darkThemeBackground));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                toolbar.setTitle(url);
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(view.getTitle());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                PackageManager packageManager = view.getContext().getPackageManager();
                List<ResolveInfo> listResolveInfo = packageManager.queryIntentActivities(intent, 0);

                Log.d(TAG, "listResolveInfo: " + listResolveInfo);

                boolean containsAll = true;

                for (ResolveInfo info : listResolveInfo) {

                    boolean contains = false;
                    for (ResolveInfo infoDefault : listDefaultWebResolveInfo) {
                        if (infoDefault.activityInfo.name.equals(info.activityInfo.name) &&
                                infoDefault.activityInfo.targetActivity.equals(info.activityInfo.targetActivity)) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        containsAll = false;
                        break;
                    }
                }

                if (!containsAll) {
                    Iterator<ResolveInfo> iterator = listResolveInfo.iterator();
                    while (iterator.hasNext()) {
                        ResolveInfo info = iterator.next();
                        boolean contains = false;
                        for (ResolveInfo infoDefault : listDefaultWebResolveInfo) {
                            if (infoDefault.activityInfo.name.equals(info.activityInfo.name) &&
                                    infoDefault.activityInfo.targetActivity.equals(info.activityInfo.targetActivity)) {
                                contains = true;
                                break;
                            }
                        }
                        if (contains) {
                            iterator.remove();
                        }
                    }

                    listResolveInfo.removeAll(listDefaultWebResolveInfo);
                    Collections.sort(listResolveInfo, new ResolveInfo.DisplayNameComparator(packageManager));
                    listResolveInfo.addAll(listDefaultWebResolveInfo);
                    showExternalLaunchDialog(url, listResolveInfo, packageManager);
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView error: " + error);
            }

        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setIndeterminate(false);
                progressBar.setProgress(newProgress);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                viewWebFullscreen = view;
                viewWebFullscreen.setBackgroundColor(0xFF000000);
                customViewCallback = callback;
                layoutRoot.addView(viewWebFullscreen, ViewGroup.LayoutParams.MATCH_PARENT);
                viewWebFullscreen.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                layoutRoot.removeView(viewWebFullscreen);
                viewWebFullscreen = null;
            }
        });
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setSupportMultipleWindows(false);
        webView.setBackgroundColor(0xFFFFFFFF);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        Log.d(TAG, "savedInstanceState: " + savedInstanceState);

        if (savedInstanceState == null) {
            webView.loadUrl(url);
        }
        else {
            webView.restoreState(savedInstanceState);
        }

        toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        toolbarActions.inflateMenu(R.menu.menu_web_search);
        toolbarActions.setOnMenuItemClickListener(this);

        Menu menuActions = toolbarActions.getMenu();
        for (int index = 0; index < menuActions.size(); index++) {
            menuActions.getItem(index).getIcon().mutate().setColorFilter(colorFilterIcon);
        }

        return view;
    }

    private void showExternalLaunchDialog(final String data, final List<ResolveInfo> listResolveInfo, PackageManager packageManager) {
        CharSequence[] titles = new CharSequence[1 + listResolveInfo.size()];
        titles[0] = getString(R.string.app_name);
        for (int index = 1; index < titles.length; index++) {
            titles[index] = listResolveInfo.get(index - 1).loadLabel(packageManager);
        }

        new AlertDialog.Builder(getActivity())
                .setItems(titles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which > 0) {
                            ResolveInfo resolveInfo = listResolveInfo.get(which - 1);
                            Intent intent = new Intent();
                            intent.setData(Uri.parse(data));
                            intent.setClassName(resolveInfo.activityInfo.applicationInfo.packageName,
                                    resolveInfo.activityInfo.name);
                            startActivity(intent);
                        }
                    }
                })
                .show();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://www.defaulturlthatnoappshouldevercatchspecifically.com"));
            listDefaultWebResolveInfo = activity.getPackageManager().queryIntentActivities(intent, 0);
            Log.d(TAG, "listDefaultWebResolveInfo: " + listDefaultWebResolveInfo);
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        activity = null;
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    public boolean navigateBack() {
        if (isFinished) {
            destroyWebView();
            webView = null;
            Log.d(TAG, "navigateBack finished");
            return true;
        }

        if (webView.canGoBack()) {
            webView.goBack();
        }
        else if (itemSearch.isActionViewExpanded()) {
            itemSearch.collapseActionView();
        }
        else if (viewWebFullscreen != null) {
            customViewCallback.onCustomViewHidden();
        }
        else {
            destroyWebView();
            webView = null;
            return true;
        }
        return false;
    }

    public void destroyWebView() {
        if (webView != null) {
            webView.removeAllViews();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            layoutRoot.removeView(webView);
            webView.destroy();
            webView = null;
        }
    }

    @Override
    public void onDestroyView() {
        destroyWebView();
        if (itemSearch != null) {
            ((SearchView) itemSearch.getActionView()).setOnQueryTextListener(null);
            MenuItemCompat.setOnActionExpandListener(itemSearch, null);
        }

        toolbar.setOnClickListener(null);
        toolbar.setNavigationOnClickListener(null);
        toolbar.setOnMenuItemClickListener(null);
        toolbarActions.setOnMenuItemClickListener(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_refresh:
                webView.reload();
                break;
            case R.id.item_open_in_browser:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(webView.getUrl()));
                startActivity(intent);
                break;
            case R.id.item_search_previous:
                webView.findNext(false);
                break;
            case R.id.item_search_next:
                webView.findNext(true);
                break;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar:
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                        Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(
                        getResources().getString(R.string.comment),
                        webView.getUrl());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(activity, R.string.url_copied, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
