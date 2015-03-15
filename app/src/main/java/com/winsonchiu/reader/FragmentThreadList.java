package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.winsonchiu.reader.data.Link;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentThreadList.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentThreadList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentThreadList extends Fragment {

    private static final String TAG = FragmentThreadList.class.getCanonicalName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Activity activity;
    private OnFragmentInteractionListener mListener;

    private RecyclerView recyclerThreadList;
    private AdapterLink adapterLink;
    private SwipeRefreshLayout swipeRefreshThreadList;
    private RecyclerView.LayoutManager layoutManager;
    private ControllerLinks controllerLinks;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentThreadList.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentThreadList newInstance(String param1, String param2) {
        FragmentThreadList fragment = new FragmentThreadList();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentThreadList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_thread_list, menu);

        final MenuItem itemSearch = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) itemSearch.getActionView();

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
                Log.d(TAG, "Query entered");
                switch (query) {
                    case "SETGRID":
                        PreferenceManager.getDefaultSharedPreferences(
                                activity.getApplicationContext())
                                .edit()
                                .putString("interface_mode", "GRID").commit();
                        break;
                    case "SETLIST":
                        PreferenceManager.getDefaultSharedPreferences(
                                activity.getApplicationContext())
                                .edit()
                                .putString("interface_mode", "LIST").commit();
                        break;
                    default:
                        controllerLinks.setParameters(query, "hot");
                        break;
                }
                itemSearch.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Remove spaces from query text

//                if (newText.contains(" ")) {
//                    searchView.setQuery(newText.replaceAll(" ", ""), false);
//                }
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_thread_list, container, false);

        swipeRefreshThreadList = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_thread_list);
        swipeRefreshThreadList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerLinks.reloadAllLinks();
            }
        });

        if (controllerLinks == null) {
            controllerLinks = new ControllerLinks(activity,
                    new ControllerLinks.LinkClickListener() {
                        @Override
                        public void onClickComments(Link link) {
                            getFragmentManager().beginTransaction().add(R.id.frame_fragment, FragmentComments
                                    .newInstance(link.getSubreddit(), link.getId()), "fragmentComments").addToBackStack(null)
                                    .commit();
                        }

                        @Override
                        public void loadUrl(String url) {
                            getFragmentManager().beginTransaction().add(R.id.frame_fragment, FragmentWeb
                                    .newInstance(url, ""), "fragmentWeb").addToBackStack(null)
                                    .commit();
                        }

                        @Override
                        public void onFullLoaded(int position) {
                            layoutManager.smoothScrollToPosition(recyclerThreadList, null, position);
                        }

                        @Override
                        public void setRefreshing(boolean refreshing) {
                            swipeRefreshThreadList.setRefreshing(refreshing);
                        }

                        @Override
                        public void setToolbarTitle(String title) {
                            mListener.setToolbarTitle(title);
                        }

                        @Override
                        public void notifyDataSetChanged() {
                            adapterLink.notifyDataSetChanged();
                        }

                        @Override
                        public void notifyItemRangeInserted(int startPosition, int endPosition) {
                            adapterLink.notifyItemRangeInserted(startPosition, endPosition);
                        }
                    }, "all", "hot");
        }
        controllerLinks.setActivity(activity);

        if (adapterLink == null) {
            if (PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getString("interface_mode", "").equals("GRID")) {
                adapterLink = new AdapterLinkGrid(activity, controllerLinks);
            }
            else {
                adapterLink = new AdapterLinkList(activity, controllerLinks);
            }
        }
        adapterLink.setActivity(activity);
        layoutManager = adapterLink.getLayoutManager();

        recyclerThreadList = (RecyclerView) view.findViewById(R.id.recycler_thread_list);
        recyclerThreadList.setItemAnimator(new DefaultItemAnimator());
        if (adapterLink.getItemDecoration() != null) {
            recyclerThreadList.addItemDecoration(adapterLink.getItemDecoration());
        }
        recyclerThreadList.setLayoutManager(layoutManager);
        recyclerThreadList.setAdapter(adapterLink);
        recyclerThreadList.setHasFixedSize(true);
        recyclerThreadList.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        adapterLink.setViewHeight(recyclerThreadList.getHeight());
                        recyclerThreadList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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
    }

    @Override
    public void onDetach() {
        mListener = null;
        activity = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        controllerLinks.setLoading(false);
    }

    @Override
    public void onPause() {
        super.onPause();
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
        void setToolbarTitle(CharSequence title);
    }

}
