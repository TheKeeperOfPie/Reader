package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.RelativeLayout;


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
    private static final int MENU_ACTIONS_SIZE = 4;

    // TODO: Rename and change types of parameters
    private String subreddit;
    private String linkId;

    private OnFragmentInteractionListener mListener;
    private RecyclerView recyclerCommentList;
    private Activity activity;
    private LinearLayoutManager linearLayoutManager;
    private AdapterCommentList adapterCommentList;
    private SwipeRefreshLayout swipeRefreshCommentList;
    private ControllerComments.CommentClickListener listener;
    private RecyclerView.ViewHolder viewHolder;
    private Toolbar toolbar;
    private Toolbar toolbarActions;
    private ImageButton buttonExpandActions;
    private RelativeLayout layoutContainerExpand;

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

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_comments);
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
            public void setToolbarTitle(String title) {
                toolbar.setTitle(title);
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
            }
        };

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });
        setUpOptionsMenu();

        toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        toolbarActions.inflateMenu(R.menu.menu_comment_actions);
        toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int position;
                switch (item.getItemId()) {

                    case R.id.item_comment_previous:
                        position = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                        if (position == 1) {
                            linearLayoutManager.scrollToPositionWithOffset(0, 0);
                        }
                        linearLayoutManager.scrollToPositionWithOffset(mListener.getControllerComments().getPreviousCommentPosition(
                                position - 1) + 1, 0);
                        break;
                    case R.id.item_comment_next:
                        position = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                        if (position == 0) {
                            if (adapterCommentList.getItemCount() > 0) {
                                linearLayoutManager.scrollToPositionWithOffset(1, 0);
                            }
                            return true;
                        }
                        linearLayoutManager.scrollToPositionWithOffset(mListener.getControllerComments()
                                .getNextCommentPosition(position - 1) + 1, 0);
                        break;
                    case R.id.item_jump_to_top:
                        linearLayoutManager.smoothScrollToPosition(recyclerCommentList, null, 0);
                        break;
                    case R.id.item_view_subreddit:
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.putExtra(MainActivity.REDDIT_PAGE, "https://reddit.com/r/" + mListener.getControllerComments().getMainLink().getSubreddit());
                        startActivity(intent);
                        break;
                }

                return true;
            }
        });
        toolbarActions.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        Menu menu = toolbarActions.getMenu();

                        int maxNum = (int) (toolbarActions.getWidth() / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, activity.getResources().getDisplayMetrics()));
                        int numShown = 0;

                        Log.d(TAG, "maxNum: " + maxNum);

                        for (int index = 0; index < MENU_ACTIONS_SIZE; index++) {
                            if (numShown < maxNum - 1) {
                                menu.getItem(index)
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                            }
                            else {
                                menu.getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                            }
                        }

                        toolbarActions.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        layoutContainerExpand = (RelativeLayout) view.findViewById(R.id.layout_container_expand);

        buttonExpandActions = (ImageButton) view.findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutContainerExpand.isShown()) {
                    layoutContainerExpand.setVisibility(View.GONE);
                    buttonExpandActions.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
                }
                else {
                    layoutContainerExpand.setVisibility(View.VISIBLE);
                    buttonExpandActions.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
                }
            }
        });

        swipeRefreshCommentList = (SwipeRefreshLayout) view.findViewById(
                R.id.swipe_refresh_comment_list);
        swipeRefreshCommentList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerComments().reloadAllComments();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerCommentList = (RecyclerView) view.findViewById(R.id.recycler_comment_list);
        recyclerCommentList.setHasFixedSize(true);
        recyclerCommentList.setLayoutManager(linearLayoutManager);
        recyclerCommentList.setItemAnimator(null);

        if (adapterCommentList == null) {
            adapterCommentList = new AdapterCommentList(activity, mListener.getControllerComments(), listener,
                    getArguments().getBoolean(ARG_IS_GRID, false));
        }

        recyclerCommentList.setAdapter(adapterCommentList);
        mListener.getControllerComments().addListener(listener);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        swipeRefreshCommentList.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshCommentList.setRefreshing(true);
                mListener.getControllerComments()
                        .reloadAllComments();

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerComments().addListener(listener);
    }

    @Override
    public void onStop() {
        mListener.getControllerComments().removeListener(listener);
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
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
    public void onDestroy() {
        super.onDestroy();
        CustomApplication.getRefWatcher(getActivity())
                .watch(this);
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
    public interface OnFragmentInteractionListener extends FragmentListenerBase {
        ControllerComments getControllerComments();
    }

}