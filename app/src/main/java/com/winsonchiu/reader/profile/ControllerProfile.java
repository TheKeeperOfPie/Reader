/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.utils.ControllerListener;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.ControllerLinksBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 5/16/2015.
 */
public class ControllerProfile implements ControllerLinksBase {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_HEADER_TEXT = 1;
    public static final int VIEW_TYPE_LINK = 2;
    public static final int VIEW_TYPE_COMMENT = 3;

    public static final String PAGE_OVERVIEW = "Overview";
    public static final String PAGE_SUBMITTED = "Submitted";
    public static final String PAGE_COMMENTS = "Comments";
    public static final String PAGE_GILDED = "Gilded";
    public static final String PAGE_UPVOTED = "Upvoted";
    public static final String PAGE_DOWNVOTED = "Downvoted";
    public static final String PAGE_HIDDEN = "Hidden";
    public static final String PAGE_SAVED = "Saved";

    private static final String TAG = ControllerProfile.class.getCanonicalName();

    private Activity activity;
    private ControllerUser controllerUser;
    private Set<Listener> listeners;
    private Listing data;
    private Reddit reddit;
    private Link topLink;
    private Comment topComment;
    private User user;
    private Page page;
    private Sort sort;
    private Time time;
    private boolean isLoading;

    public ControllerProfile(Activity activity) {
        setActivity(activity);
        data = new Listing();
        listeners = new HashSet<>();
        topLink = new Link();
        topComment = new Comment();
        user = new User();
        page = new Page(PAGE_OVERVIEW, activity.getString(R.string.profile_page_overview));
        sort = Sort.NEW;
        time = Time.ALL;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.setSortAndTime(sort, time);
        listener.setRefreshing(isLoading());
        listener.getAdapter().notifyDataSetChanged();
        listener.setIsUser(user.getName().equals(controllerUser.getUser().getName()));
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public int getViewType(int position) {

        List<Thing> children = data.getChildren();

        if (position < 0 || position >= children.size()) {
            throw new IndexOutOfBoundsException("ControllerProfile position invalid");
        }

        Thing thing = children.get(position);

        if (thing instanceof Link) {
            return VIEW_TYPE_LINK;
        }
        else if (thing instanceof Comment) {
            return VIEW_TYPE_COMMENT;
        }

        throw new IllegalStateException(thing + " is not a valid view type");

    }

    @Override
    public Link getLink(int position) {
        if (position == 2) {
            return getTopLink();
        }

        return (Link) data.getChildren().get(position - 6);
    }

    public Link getTopLink() {
        return page.getPage().equalsIgnoreCase(PAGE_OVERVIEW) ? topLink : null;
    }

    public Comment getComment(int position) {
        if (position == 4) {
            return getTopComment();
        }

        return (Comment) data.getChildren().get(position - 6);
    }

    public void setPage(Page page) {
        if (!this.page.equals(page)) {
            this.page = page;
            reload();
        }
    }

    public Page getPage() {
        return page;
    }

    public void setUser(User user) {
        this.user = user;
        sort = Sort.NEW;
        page = new Page(PAGE_OVERVIEW, activity.getString(R.string.profile_page_overview));
        for (Listener listener : listeners) {
            listener.setSortAndTime(sort, time);
            listener.setIsUser(user.getName()
                    .equals(controllerUser.getUser().getName()));
        }
        reload();
    }

    public void reload() {

        setLoading(true);

        String url = Reddit.OAUTH_URL + "/user/" + user.getName() + "/" + page.getPage().toLowerCase() + "?limit=15&sort=" + sort.toString() + "&t=" + time.toString();

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            setData(Listing.fromJson(new JSONObject(response)));
                            for (Listener listener : listeners) {
                                listener.setPage(page);
                                listener.getAdapter().notifyDataSetChanged();
                            }
                            setLoading(false);
                            if (!TextUtils.isEmpty(user.getName()) && page.getPage().equalsIgnoreCase(
                                    PAGE_OVERVIEW)) {
                                loadTopEntries();
                            }
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLoading(false);
                        Toast.makeText(activity, activity.getString(R.string.error_loading),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }, 0);
    }

    public void loadMore() {

        if (isLoading()) {
            return;
        }

        setLoading(true);

        String url = Reddit.OAUTH_URL + "/user/" + user.getName() + "/" + page.getPage().toLowerCase() + "?limit=15&sort=" + sort.toString() + "&t=" + time.toString() + "&after=" + data.getAfter();

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            int startSize = data.getChildren().size();
                            int positionStart = startSize + 5;

                            Listing listing = Listing.fromJson(new JSONObject(response));

                            data.addChildren(listing.getChildren());
                            data.setAfter(listing.getAfter());

                            for (Listener listener : listeners) {

                                listener.getAdapter()
                                        .notifyItemRangeInserted(positionStart,
                                                data.getChildren().size() - positionStart);
                                listener.setPage(page);
                            }
                            setLoading(false);
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setData(new Listing());
                        for (Listener listener : listeners) {
                            listener.setRefreshing(
                                    false);
                            listener.getAdapter().notifyDataSetChanged();
                        }
                    }
                }, 0);
    }

    private void loadTopEntries() {

        // TODO: Support loading trophies
//        reddit.loadGet(Reddit.OAUTH_URL + "/api/v1/user/" + user.getName() + "/trophies",
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        Log.d(TAG, "Trophies response: " + response);
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//
//                    }
//                }, 0);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/user/" + user.getName() + "/submitted?sort=top&limit=10&",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Submitted onResponse: " + response);
                        try {
                            Listing listingLink = Listing.fromJson(
                                    new JSONObject(response));
                            if (!listingLink.getChildren().isEmpty()) {
                                topLink = null;
                                for (Thing thing : listingLink.getChildren()) {
                                    Link link = (Link) thing;
                                    if (!link.isHidden()) {
                                        topLink = link;
                                        break;
                                    }
                                }

                                for (Listener listener : listeners) {
                                    listener.setRefreshing(false);
                                    listener.getAdapter().notifyItemRangeChanged(1, 2);
                                }
                            }
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/user/" + user.getName() + "/comments?sort=top&limit=10&",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG,
                                "onResponse: " + response);
                        try {
                            Listing listingComment = Listing.fromJson(
                                    new JSONObject(
                                            response));
                            if (!listingComment.getChildren().isEmpty()) {
                                topComment = (Comment) listingComment.getChildren()
                                        .get(0);
                                topComment.setLevel(0);
                                for (Listener listener : listeners) {
                                    listener.setRefreshing(
                                            false);
                                    listener.getAdapter().notifyItemRangeChanged(
                                            3, 2);
                                }
                            }
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    @Override
    public int sizeLinks() {
        return data.getChildren().size();
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        for (Listener listener : listeners) {
            listener.setRefreshing(loading);
        }
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public void loadMoreLinks() {
        // Not implemented
    }

    @Override
    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    @Override
    public boolean showSubreddit() {
        return true;
    }


    @Override
    public boolean setReplyText(String name, String text, boolean collapsed) {

        if (topLink != null && name.equals(topLink.getName())) {
            topLink.setReplyText(text);
            topLink.setReplyExpanded(!collapsed);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(2);
            }
            return true;
        }

        if (topComment != null && name.equals(topComment.getName())) {
            topComment.setReplyText(text);
            topComment.setReplyExpanded(!collapsed);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(4);
            }
            return true;
        }

        for (int index = 0; index < data.getChildren().size(); index++) {
            Thing thing = data.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 6);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void setNsfw(String name, boolean over18) {

        if (topLink != null && name.equals(topLink.getName())) {
            topLink.setOver18(over18);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(2);
            }
            return;
        }

        for (int index = 0; index < data.getChildren().size(); index++) {
            Thing thing = data.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 6);
                }
                return;
            }
        }
    }

    public Link remove(int position) {
        Link link;
        if (position == -1) {
            link = topLink;
            topLink = null;
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(2);
            }
        }
        else {
            link = (Link) data.getChildren().remove(position);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemRemoved(position + 6);
            }
        }
        return link;
    }

    public void insertComment(Comment comment) {

        // Placeholder to use ArrayList.indexOf() properly
        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());

        int commentIndex = data.getChildren()
                    .indexOf(parentComment);
        if (commentIndex > -1) {
            // Level and context are set as they are not provided by the send API
            parentComment = (Comment) data.getChildren().get(commentIndex);
            comment.setLevel(parentComment.getLevel() + 1);
            comment.setContext(parentComment.getContext());
            data.getChildren()
                    .add(commentIndex + 1, comment);

            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemInserted(commentIndex + 7);
            }
        }

    }

    public void deleteComment(Comment comment) {
        int commentIndex = data.getChildren().indexOf(comment);
        data.getChildren().remove(commentIndex);

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(commentIndex + 6);

        }

        Map<String, String> params = new HashMap<>();
        params.put("id", comment.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/del",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(
                            String response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(
                            VolleyError error) {

                    }
                }, params, 0);
    }

    public boolean toggleComment(int position) {
        // Not implemented
        return true;
    }

    public void voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment,
            int vote) {

        reddit.voteComment(viewHolder, comment, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, activity.getString(R.string.error_voting), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    public void loadNestedComments(Comment moreComment) {
        // Not implemented
    }

    public boolean isCommentExpanded(int position) {
        // Not implemented
        return true;
    }

    public boolean hasChildren(Comment comment) {
        return false;
    }

    public void editComment(final Comment comment, String text) {

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("text", text);
        params.put("thing_id", comment.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/editusertext", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Comment newComment = Comment.fromJson(new JSONObject(response).getJSONObject("json").getJSONObject("data").getJSONArray("things").getJSONObject(0), comment.getLevel());
                    comment.setBodyHtml(newComment.getBodyHtml());
                    if (comment.getName().equals(topComment.getName())) {
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyItemChanged(2);
                        }
                    }
                    else {
                        int commentIndex = data.getChildren()
                                .indexOf(comment);

                        if (commentIndex > -1) {
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyItemChanged(commentIndex + 6);
                            }
                        }
                    }

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

    public void setData(Listing data) {
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public void loadUser(String query) {

        setLoading(true);

        reddit.loadGet(Reddit.OAUTH_URL + "/user/" + query + "/about",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.d(TAG, "User FragmentProfile onResponse: " + new JSONObject(response).getJSONObject("data").toString());
                                    user = User.fromJson(new JSONObject(response).getJSONObject("data"));
                                }
                                catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                setUser(user);
                                setLoading(false);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                setLoading(false);
                                Toast.makeText(activity, activity.getString(R.string.error_loading), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }, 0);
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            reload();
        }
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            reload();
        }
    }

    public Comment getTopComment() {
        return page.getPage().equalsIgnoreCase(PAGE_OVERVIEW) ? topComment : null;
    }

    public void setControllerUser(ControllerUser controllerUser) {
        this.controllerUser = controllerUser;
    }

    public void sendComment(String name, String text) {
        reddit.sendComment(name, text, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Comment newComment = Comment.fromJson(
                            jsonObject.getJSONObject("json")
                                    .getJSONObject("data")
                                    .getJSONArray("things")
                                    .getJSONObject(0), 0);
                    insertComment(newComment);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }

    public void add(int position, Link link) {
        data.getChildren().add(position, link);
        for (Listener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }
    }

    public void setTopLink(Link topLink) {
        this.topLink = topLink;
    }

    public interface Listener
            extends ControllerListener {
        void setSortAndTime(Sort sort, Time time);
        void setPage(Page page);
        void setIsUser(boolean isUser);
        void loadLink(Comment comment);
    }

}