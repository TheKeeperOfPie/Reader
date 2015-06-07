package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentComments.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentComments#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentComments extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_IS_GRID = "isGrid";
    public static final String TAG = FragmentComments.class.getCanonicalName();

    // TODO: Rename and change types of parameters
    private String subreddit;
    private String linkId;

    private OnFragmentInteractionListener mListener;
    private RecyclerView recyclerCommentList;
    private Activity activity;
    private LinearLayoutManager linearLayoutManager;
    private AdapterCommentList adapterCommentList;
    private SwipeRefreshLayout swipeRefreshCommentList;
    private ControllerComments controllerComments;
    private ControllerComments.CommentClickListener listener;
    private RecyclerView.ViewHolder viewHolder;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentComments.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentComments newInstance(String param1, String param2, boolean isGrid) {
        FragmentComments fragment = new FragmentComments();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putBoolean(ARG_IS_GRID, isGrid);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentComments() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            subreddit = getArguments().getString(ARG_PARAM1);
            linkId = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_comments, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_comments, container, false);

        listener = new ControllerComments.CommentClickListener() {
            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshCommentList.setRefreshing(refreshing);
            }

            @Override
            public AdapterCommentList getAdapter() {
                Log.d(TAG, "Adapter returned");
                return adapterCommentList;
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerCommentList.getHeight();
            }

            @Override
            public int getRecyclerWidth() {
                return recyclerCommentList.getWidth();
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
            }
        };

        swipeRefreshCommentList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_comment_list);
        swipeRefreshCommentList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controllerComments.reloadAllComments();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerCommentList = (RecyclerView) view.findViewById(R.id.recycler_comment_list);
        recyclerCommentList.setHasFixedSize(true);
        recyclerCommentList.setItemAnimator(new DefaultItemAnimator());
        recyclerCommentList.getItemAnimator()
                .setRemoveDuration(AnimationUtils.EXPAND_ACTION_DURATION);
        recyclerCommentList.setLayoutManager(linearLayoutManager);
//        recyclerCommentList.addItemDecoration(
//                new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST));

        controllerComments = mListener.getControllerComments();

        if (adapterCommentList == null) {
            adapterCommentList = new AdapterCommentList(activity, controllerComments, listener,
                    getArguments().getBoolean(ARG_IS_GRID, false));
        }

        recyclerCommentList.setAdapter(adapterCommentList);
        controllerComments.addListener(listener);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        swipeRefreshCommentList.post(new Runnable() {
//            @Override
//            public void run() {
//                swipeRefreshCommentList.setRefreshing(true);
//                controllerComments.setLinkId(subreddit, linkId);
//            }
//        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        controllerComments.addListener(listener);
    }

    @Override
    public void onStop() {
        controllerComments.removeListener(listener);
        super.onStop();
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
        mListener.setNavigationAnimation(0.0f);
        mListener = null;
        activity = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
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

        void setToolbarTitle(CharSequence title);

        ControllerComments getControllerComments();
    }

}