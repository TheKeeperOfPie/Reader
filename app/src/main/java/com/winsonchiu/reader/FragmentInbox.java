/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Message;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FragmentInbox extends FragmentBase {

    public static final String TAG = FragmentInbox.class.getCanonicalName();

    private Activity activity;
    private FragmentListenerBase mListener;
    private SwipeRefreshLayout swipeRefreshInbox;
    private RecyclerView recyclerInbox;
    private LinearLayoutManager linearLayoutManager;
    private AdapterInbox adapterInbox;
    private AdapterInbox.ViewHolderMessage.EventListener eventListener;
    private ControllerInbox.Listener listener;
    private Toolbar toolbar;
    private FloatingActionButton floatingActionButtonNewMessage;
    private Spinner spinnerPage;
    private AdapterInboxPage adapterInboxPage;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        listener = new ControllerInbox.Listener() {
            @Override
            public void setPage(String page) {
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
        };

        eventListener = new AdapterInbox.ViewHolderMessage.EventListener() {
            @Override
            public void sendMessage(String name, String text) {
                Map<String, String> params = new HashMap<>();
                params.put("api_type", "json");
                params.put("thing_id", name);
                params.put("text", text);

                // TODO: Move add to immediate on button click, check if failed afterwards
                mListener.getReddit()
                        .loadPost(Reddit.OAUTH_URL + "/api/comment",
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(
                                                    response);
                                            Message newMessage = Message.fromJson(
                                                    jsonObject.getJSONObject("json")
                                                            .getJSONObject("data")
                                                            .getJSONArray("things")
                                                            .getJSONObject(0));
                                            mListener.getControllerInbox()
                                                    .insertMessage(newMessage);
                                        }
                                        catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                }, params, 0);
            }

            @Override
            public void markRead(Thing thing) {

                Map<String, String> params = new HashMap<>();
                params.put("id", thing.getName());

                mListener.getReddit()
                        .loadPost(Reddit.OAUTH_URL + "/api/read_message",
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                }, params, 0);
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

        floatingActionButtonNewMessage = (FloatingActionButton) view.findViewById(R.id.fab_new_message);
        floatingActionButtonNewMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, ActivityNewMessage.class);
                startActivity(intent);
            }
        });

        adapterInboxPage = new AdapterInboxPage(activity);
        spinnerPage = (Spinner) view.findViewById(R.id.spinner_page);
        spinnerPage.setAdapter(adapterInboxPage);
        spinnerPage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mListener.getControllerInbox().setPage(adapterInboxPage.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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

        if (adapterInbox == null) {
            adapterInbox = new AdapterInbox(mListener.getControllerInbox(),
                    mListener.getControllerUser(),
                    mListener.getEventListenerBase(),
                    new AdapterCommentList.ViewHolderComment.EventListener() {
                        @Override
                        public void loadNestedComments(Comment comment) {
                            mListener.getControllerInbox().loadNestedComments(comment);
                        }

                        @Override
                        public boolean isCommentExpanded(int position) {
                            return mListener.getControllerInbox().isCommentExpanded(position);
                        }

                        @Override
                        public boolean hasChildren(Comment comment) {
                            return mListener.getControllerInbox().hasChildren(comment);
                        }

                        @Override
                        public void voteComment(AdapterCommentList.ViewHolderComment viewHolderComment,
                                Comment comment,
                                int vote) {
                            mListener.getControllerInbox()
                                    .voteComment(viewHolderComment, comment, vote);
                        }

                        @Override
                        public boolean toggleComment(int position) {
                            return mListener.getControllerInbox().toggleComment(position);
                        }

                        @Override
                        public void deleteComment(Comment comment) {
                            mListener.getControllerInbox().deleteComment(comment);
                        }

                        @Override
                        public void editComment(Comment comment, String text) {
                            mListener.getControllerInbox().editComment(comment, text);
                        }

                        @Override
                        public void sendComment(String name, String text) {
                            mListener.getControllerInbox().sendComment(name, text);
                        }
                    },
                    eventListener,
                    new DisallowListener() {
                        @Override
                        public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                            recyclerInbox.requestDisallowInterceptTouchEvent(disallow);
                            swipeRefreshInbox.requestDisallowInterceptTouchEvent(disallow);
                        }

                        @Override
                        public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {

                        }
                    }, new ControllerProfile.Listener() {
                @Override
                public void setPage(String page) {

                }

                @Override
                public void setIsUser(boolean isUser) {

                }

                @Override
                public void loadLink(Comment comment) {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(MainActivity.REDDIT_PAGE,
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
            });
        }

        recyclerInbox.setAdapter(adapterInbox);

        return view;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeRefreshInbox.setRefreshing(mListener.getControllerInbox().isLoading());
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
//        CustomApplication.getRefWatcher(getActivity())
//                .watch(this);
    }

    @Override
    boolean navigateBack() {
        return true;
    }

    @Override
    public void onShown() {
        adapterInbox.setVisibility(View.VISIBLE);
    }
}
