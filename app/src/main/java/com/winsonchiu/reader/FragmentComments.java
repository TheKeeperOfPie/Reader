package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.winsonchiu.reader.data.Link;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentComments.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentComments#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentComments extends Fragment {

    public static final String TAG = FragmentComments.class.getCanonicalName();

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_IS_GRID = "isGrid";
    private static final long DURATION_ACTIONS_FADE = 150;

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
    private LinearLayout layoutActions;
    private FloatingActionButton buttonExpandActions;
    private FloatingActionButton buttonCommentPrevious;
    private FloatingActionButton buttonCommentNext;
    private ScrollAwareFloatingActionButtonBehavior behaviorFloatingActionButton;
    private YouTubePlayerView viewYouTube;
    private YouTubePlayer youTubePlayer;
    private RecyclerView.AdapterDataObserver observer;

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
                        .hide(FragmentComments.this)
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
            public void setToolbarTitle(CharSequence title) {
                toolbar.setTitle(title);
            }

            @Override
            public void loadYouTube(final Link link, final String id, final AdapterLink.ViewHolderBase viewHolderBase) {
                if (youTubePlayer != null) {
                    viewYouTube.setVisibility(View.VISIBLE);
                    return;
                }

                viewYouTube.initialize(ApiKeys.YOUTUBE_API_KEY,
                        new YouTubePlayer.OnInitializedListener() {
                            @Override
                            public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                    YouTubePlayer youTubePlayer,
                                    boolean b) {
                                FragmentComments.this.youTubePlayer = youTubePlayer;
                                youTubePlayer.setShowFullscreenButton(false);
                                youTubePlayer.setManageAudioFocus(false);
                                youTubePlayer.loadVideo(id);
                                viewYouTube.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onInitializationFailure(YouTubePlayer.Provider provider,
                                    YouTubeInitializationResult youTubeInitializationResult) {
                                viewHolderBase.attemptLoadImage(link);
                            }
                        });
            }

            @Override
            public boolean hideYouTube() {
                if (viewYouTube.isShown()) {
                    if (youTubePlayer != null) {
                        youTubePlayer.pause();
                    }
                    viewYouTube.setVisibility(View.GONE);
                    return false;
                }

                return true;
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                recyclerCommentList.requestDisallowInterceptTouchEvent(disallow);
                swipeRefreshCommentList.requestDisallowInterceptTouchEvent(disallow);
            }
        };

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
            }
        });
        toolbar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(activity, "Return to top", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });
        setUpOptionsMenu();

        layoutActions = (LinearLayout) view.findViewById(R.id.layout_actions);
        buttonExpandActions = (FloatingActionButton) view.findViewById(R.id.button_expand_actions);
        buttonExpandActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonExpandActions.setImageResource(buttonCommentPrevious.isShown() ? R.drawable.ic_unfold_more_white_24dp : android.R.color.transparent);
                toggleLayoutActions();
            }
        });

        behaviorFloatingActionButton = new ScrollAwareFloatingActionButtonBehavior(activity, null,
                new ScrollAwareFloatingActionButtonBehavior.OnVisibilityChangeListener() {
                    @Override
                    public void onStartHideFromScroll() {
                        hideLayoutActions();
                    }

                    @Override
                    public void onEndHideFromScroll() {
                        buttonExpandActions.setImageResource(R.drawable.ic_unfold_more_white_24dp);
                    }

                });
        ((CoordinatorLayout.LayoutParams) buttonExpandActions.getLayoutParams()).setBehavior(behaviorFloatingActionButton);

        buttonCommentPrevious = (FloatingActionButton) view.findViewById(R.id.button_comment_previous);
        buttonCommentPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = linearLayoutManager.findFirstVisibleItemPosition();
                if (position == 1) {
                    linearLayoutManager.scrollToPositionWithOffset(0, 0);
                    return;
                }
                linearLayoutManager.scrollToPositionWithOffset(mListener.getControllerComments().getPreviousCommentPosition(
                        position - 1) + 1, 0);
            }
        });

        buttonCommentNext = (FloatingActionButton) view.findViewById(R.id.button_comment_next);
        buttonCommentNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = linearLayoutManager.findFirstVisibleItemPosition();
                if (position == 0) {
                    if (adapterCommentList.getItemCount() > 0) {
                        linearLayoutManager.scrollToPositionWithOffset(1, 0);
                    }
                    return;
                }
                linearLayoutManager.scrollToPositionWithOffset(mListener.getControllerComments()
                        .getNextCommentPosition(position - 1) + 1, 0);
            }
        });

        viewYouTube = (YouTubePlayerView) view.findViewById(R.id.youtube);

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

        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                if (positionStart == 0 && youTubePlayer != null) {
                    youTubePlayer.release();
                    youTubePlayer = null;
                }
            }
        };

        adapterCommentList.registerAdapterDataObserver(observer);

        recyclerCommentList.setAdapter(adapterCommentList);
        mListener.getControllerComments().addListener(listener);

        return view;
    }

    private void toggleLayoutActions() {
        if (buttonCommentPrevious.isShown()) {
            hideLayoutActions();
        }
        else {
            showLayoutActions();
        }
    }

    private void showLayoutActions() {

        for (int index = layoutActions.getChildCount() - 1; index >= 0; index--) {
            final View view = layoutActions.getChildAt(index);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setDuration(DURATION_ACTIONS_FADE);
            alphaAnimation.setStartOffset(index * DURATION_ACTIONS_FADE / 3);
            view.startAnimation(alphaAnimation);
        }

    }

    private void hideLayoutActions() {
        for (int index = 0; index < layoutActions.getChildCount(); index++) {
            final View view = layoutActions.getChildAt(index);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1f, 0f);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            alphaAnimation.setDuration(DURATION_ACTIONS_FADE);
            alphaAnimation.setStartOffset(index * DURATION_ACTIONS_FADE / 3);
            view.startAnimation(alphaAnimation);
        }
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
    public void onDestroyView() {
        adapterCommentList.unregisterAdapterDataObserver(observer);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (youTubePlayer != null) {
            youTubePlayer.release();
            youTubePlayer = null;
        }
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
    public interface OnFragmentInteractionListener extends FragmentListenerBase {
        ControllerComments getControllerComments();
    }

}