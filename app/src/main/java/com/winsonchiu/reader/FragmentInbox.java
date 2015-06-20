package com.winsonchiu.reader;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import com.winsonchiu.reader.data.Link;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentInbox.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentInbox#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentInbox extends Fragment {

    public static final String TAG = FragmentInbox.class.getCanonicalName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Activity activity;
    private OnFragmentInteractionListener mListener;
    private SwipeRefreshLayout swipeRefreshInbox;
    private RecyclerView recyclerInbox;
    private LinearLayoutManager linearLayoutManager;
    private AdapterInbox adapterInbox;
    private ControllerInbox.ItemClickListener listener;
    private Toolbar toolbar;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentInbox.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentInbox newInstance(String param1, String param2) {
        FragmentInbox fragment = new FragmentInbox();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentInbox() {
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        listener = new ControllerInbox.ItemClickListener() {
            @Override
            public void onClickComments(final Link link, final RecyclerView.ViewHolder viewHolder) {

                if (link.getNumComments() == 0) {
                    if (!link.isCommentsClicked()) {
                        Toast.makeText(activity, activity.getString(R.string.no_comments),
                                Toast.LENGTH_SHORT).show();
                        link.setCommentsClicked(true);
                        return;
                    }
                }

                mListener.getControllerComments()
                        .setLink(link);

                if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                    ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                            true);
                    viewHolder.itemView.requestLayout();
                }

                viewHolder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        final float viewStartY = viewHolder.itemView.getY();
                        final float viewStartPaddingBottom = viewHolder.itemView.getPaddingBottom();
                        final float screenHeight = getResources().getDisplayMetrics().heightPixels;

                        float speed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                                activity.getResources()
                                        .getDisplayMetrics());
                        long duration = (long) Math.abs(viewStartY / speed);

                        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0,
                                -viewStartY);

                        Animation heightAnimation = new Animation() {
                            @Override
                            protected void applyTransformation(float interpolatedTime,
                                    Transformation t) {
                                super.applyTransformation(interpolatedTime, t);
                                viewHolder.itemView.setPadding(viewHolder.itemView.getPaddingLeft(),
                                        viewHolder.itemView.getPaddingTop(),
                                        viewHolder.itemView.getPaddingRight(),
                                        (int) (viewStartPaddingBottom + interpolatedTime * screenHeight));
                            }

                            @Override
                            public boolean willChangeBounds() {
                                return true;
                            }
                        };
                        heightAnimation.setStartOffset(duration / 10);
                        heightAnimation.setInterpolator(new LinearInterpolator());

                        AnimationSet animation = new AnimationSet(false);
                        animation.addAnimation(translateAnimation);
                        animation.addAnimation(heightAnimation);


                        animation.setDuration(duration);
                        animation.setFillAfter(false);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                FragmentComments fragmentComments = FragmentComments.newInstance(
                                        link.getSubreddit(), link.getId(),
                                        viewHolder instanceof AdapterLinkGrid.ViewHolder);

                                getFragmentManager().beginTransaction()
                                        .add(R.id.frame_fragment, fragmentComments,
                                                FragmentComments.TAG)
                                        .addToBackStack(null)
                                        .commit();

                                viewHolder.itemView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewHolder.itemView.setPadding(
                                                viewHolder.itemView.getPaddingLeft(),
                                                viewHolder.itemView.getPaddingTop(),
                                                viewHolder.itemView.getPaddingRight(),
                                                (int) viewStartPaddingBottom);
                                        viewHolder.itemView.clearAnimation();
                                    }
                                }, 150);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        viewHolder.itemView.startAnimation(animation);
                    }
                });
            }

            @Override
            public void loadUrl(String url) {
                getFragmentManager().beginTransaction()
                        .add(R.id.frame_fragment, FragmentWeb
                                .newInstance(url, ""), FragmentWeb.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onFullLoaded(int position) {

            }

            @Override
            public void setRefreshing(boolean refreshing) {
                swipeRefreshInbox.setRefreshing(refreshing);
            }

            @Override
            public void setToolbarTitle(String title) {
                toolbar.setTitle(title);
            }

            @Override
            public AdapterInbox getAdapter() {
                return adapterInbox;
            }

            @Override
            public int getRecyclerHeight() {
                return recyclerInbox.getHeight();
            }

            @Override
            public void resetRecycler() {
                adapterInbox.notifyDataSetChanged();
            }

            @Override
            public void setSwipeRefreshEnabled(boolean enabled) {
                swipeRefreshInbox.setEnabled(enabled);
            }

            @Override
            public int getRecyclerWidth() {
                return recyclerInbox.getWidth();
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {

            }
        };

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.openDrawer();
            }
        });

        swipeRefreshInbox = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_inbox);
        swipeRefreshInbox.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mListener.getControllerInbox()
                        .reload();
            }
        });

        linearLayoutManager = new LinearLayoutManager(activity);
        recyclerInbox = (RecyclerView) view.findViewById(R.id.recycler_inbox);
        recyclerInbox.setHasFixedSize(true);
        recyclerInbox.setItemAnimator(new DefaultItemAnimator());
        recyclerInbox.getItemAnimator()
                .setRemoveDuration(AnimationUtils.EXPAND_ACTION_DURATION);
        recyclerInbox.setLayoutManager(linearLayoutManager);
//        recyclerInbox.addItemDecoration(
//                new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST));

        if (adapterInbox == null) {
            adapterInbox = new AdapterInbox(activity, mListener.getControllerInbox(), listener);
        }

        recyclerInbox.setAdapter(adapterInbox);
        mListener.getControllerInbox()
                .addListener(listener);

        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        swipeRefreshInbox.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshInbox.setRefreshing(true);
                mListener.getControllerInbox()
                        .reload();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mListener.getControllerInbox()
                .addListener(listener);
    }

    @Override
    public void onStop() {
        mListener.getControllerInbox()
                .removeListener(listener);
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
        ControllerInbox getControllerInbox();
        ControllerComments getControllerComments();
    }

}
