/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.utils.ControllerListener;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class ControllerInbox {

    public static final int VIEW_TYPE_MESSAGE = 0;
    public static final int VIEW_TYPE_COMMENT = 1;
    public static final String INBOX = "inbox";
    public static final String UNREAD = "unread";
    public static final String SENT = "sent";
    public static final String COMMENTS = "comments";
    public static final String SELF_REPLY = "selfreply";
    public static final String MENTIONS = "mentions";
    public static final String MODERATOR = "moderator";
    public static final String MODERATOR_UNREAD = "moderator/unread";

    private static final String TAG = ControllerInbox.class.getCanonicalName();

    private Activity activity;
    private Set<Listener> listeners;
    private Listing data;
    private Reddit reddit;
    private Link link;
    private Page page;
    private boolean isLoading;

    public ControllerInbox(Activity activity) {
        setActivity(activity);
        data = new Listing();
        listeners = new HashSet<>();
        link = new Link();
        page = new Page(INBOX, activity.getString(R.string.inbox_page_inbox));
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle("Inbox");
        }
    }

    public int getViewType(int position) {

        Thing thing = data.getChildren().get(position);

        if (thing instanceof Message) {
            return VIEW_TYPE_MESSAGE;
        }
        else if (thing instanceof Comment) {
            return VIEW_TYPE_COMMENT;
        }

        throw new IllegalStateException(thing + " is not a valid view type");
    }


    public int getItemCount() {
        return data.getChildren().size();
    }

    public Link getLink(int position) {
        return link;
    }

    public Message getMessage(int position) {
        return (Message) data.getChildren().get(position);
    }

    public Comment getComment(int position) {
        return (Comment) data.getChildren().get(position);
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

    public void reload() {

        Log.d(TAG, "Page: " + page.getPage());

        setLoading(true);

        reddit.loadGet(Reddit.OAUTH_URL + "/message/" + page.getPage(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            setData(Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class)));
                            for (Listener listener : listeners) {
                                listener.setPage(page);
                                listener.getAdapter().notifyDataSetChanged();
                            }
                            setLoading(false);
                        }
                        catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLoading(false);
                    }
                }, 0);
    }

    public void setData(Listing data) {
        this.data = data;
    }

    public boolean hasChildren(Comment comment) {
        return false;
    }

    public void editComment(String name, final int level, String text) {

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("text", text);
        params.put("thing_id", name);

        reddit.loadPost(Reddit.OAUTH_URL + "/api/editusertext", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Comment newComment = Comment.fromJson(Reddit.getObjectMapper().readValue(
                            response, JsonNode.class).get("json").get("data").get("things").get(0), level);

                    int commentIndex = data.getChildren()
                            .indexOf(newComment);

                    if (commentIndex > -1) {
                        Comment comment = (Comment) data.getChildren().get(commentIndex);
                        comment.setBodyHtml(newComment.getBodyHtml());
                        comment.setEdited(newComment.getEdited());
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyItemChanged(commentIndex);
                        }
                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }, params, 0);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    public boolean showSubreddit() {
        return true;
    }

    public void insertMessage(Message message) {

        Message parentMessage = new Message();
        parentMessage.setId(message.getParentId());

        int messageIndex = data.getChildren().indexOf(parentMessage);
        if (messageIndex > -1) {
            data.getChildren()
                    .add(messageIndex + 1, message);
        }

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemInserted(messageIndex + 1);
        }
    }

    public void insertComment(Comment comment) {

        // Placeholder to use ArrayList.indexOf() properly
        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());

        int commentIndex = data.getChildren().indexOf(parentComment);
        if (commentIndex > -1) {
            // Level and context are set as they are not provided by the send API
            parentComment = (Comment) data.getChildren().get(commentIndex);
            comment.setLevel(parentComment.getLevel() + 1);
            comment.setContext(parentComment.getContext());
            data.getChildren()
                    .add(commentIndex + 1, comment);

            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemInserted(commentIndex + 1);
            }
        }
    }

    public void deleteComment(Comment comment) {
        int commentIndex = data.getChildren().indexOf(comment);
        if (commentIndex > -1) {
            data.getChildren()
                    .remove(commentIndex);
        }

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(commentIndex);
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

    public void sendComment(String name, String text) {

        reddit.sendComment(name, text, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Comment newComment = Comment.fromJson(
                            Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class).get("json")
                                    .get("data")
                                    .get("things")
                                    .get(0), 0);
                    insertComment(newComment);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }

    public void loadMore() {

        setLoading(true);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/message/" + page.getPage() + "?after=" + data
                        .getAfter(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            int startSize = data.getChildren().size();
                            Listing listing = Listing.fromJson(Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class));
                            data.addChildren(listing.getChildren());
                            data.setAfter(listing.getAfter());
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyItemRangeInserted(startSize,
                                        data.getChildren().size() - startSize);
                            }
                            setLoading(false);
                        }
                        catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLoading(false);
                    }
                }, 0);
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        for (Listener listener : listeners) {
            listener.setRefreshing(isLoading());
        }
    }

    public boolean setReplyText(String name, String text, boolean collapsed) {

        for (int index = 0; index < data.getChildren().size(); index++) {
            Thing thing = data.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index);
                }
                return true;
            }
        }
        return false;
    }

    public void markAllRead() {
        reddit.loadPost(Reddit.OAUTH_URL + "/api/read_all_messages",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "markAllRead response: " + response);
                        Toast.makeText(activity, R.string.marked_read, Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, null, 0);
    }

    public interface Listener extends ControllerListener {
        void setPage(Page page);
    }

}
