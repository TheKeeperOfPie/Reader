package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentWeb.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentWeb#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentWeb extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_URL = "url";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = FragmentWeb.class.getCanonicalName();

    // TODO: Rename and change types of parameters
    private String url;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshWeb;
    private Activity activity;
    private MenuItem itemSearch;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param url Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentWeb.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentWeb newInstance(String url, String param2) {
        FragmentWeb fragment = new FragmentWeb();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_PARAM2, param2);
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
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_web, menu);

        itemSearch = menu.findItem(R.id.item_search);

        SearchView searchView = (SearchView) itemSearch.getActionView();

        searchView.setQueryHint(getString(R.string.search_in_page));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                webView.findAllAsync(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_web, container, false);

        swipeRefreshWeb = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_web);
        swipeRefreshWeb.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        webView = (WebView) view.findViewById(R.id.web);
        webView.setBackgroundColor(getResources().getColor(R.color.darkThemeBackground));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshWeb.setRefreshing(false);
                webView.setBackgroundColor(0xFFFFFFFF);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                webView.setBackgroundColor(
                        ColorUtils.setAlphaComponent(0xFFFFFFFF, (int) (newProgress / 100f * 255)));
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
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        swipeRefreshWeb.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshWeb.setRefreshing(true);
                webView.loadUrl(url);
            }
        });

        return view;
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
        mListener.setNavigationAnimation(1.0f);
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
        webView.onResume();
    }

    @Override
    public void onPause() {
        webView.onPause();
        super.onPause();
    }

    public boolean navigateBack() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
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
    public interface OnFragmentInteractionListener {
        void setNavigationAnimation(float value);
    }

}
