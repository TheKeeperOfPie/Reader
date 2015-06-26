package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Message;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class ControllerInbox implements ControllerCommentsBase {

    public static final int VIEW_TYPE_MESSAGE = 0;
    public static final int VIEW_TYPE_COMMENT = 1;
    private static final String TAG = ControllerInbox.class.getCanonicalName();

    private Activity activity;
    private Set<Listener> listeners;
    private Listing data;
    private Reddit reddit;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Link link;
    private String page;
    private User user;
    private SharedPreferences preferences;

    public ControllerInbox(Activity activity) {
        setActivity(activity);
        data = new Listing();
        listeners = new HashSet<>();
        link = new Link();
        page = "Inbox";

        // TODO: Support reloading user data
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            user = new User();
        }
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

    @Override
    public Link getLink(int position) {
        return link;
    }

    public Message getMessage(int position) {
        return (Message) data.getChildren().get(position);
    }

    @Override
    public Comment getComment(int position) {
        return (Comment) data.getChildren().get(position);
    }

    public void setPage(String page) {
        this.page = page;
        reload();
    }

    public String getPage() {
        return page;
    }

    public void reload() {

        reddit.loadGet(Reddit.OAUTH_URL + "/message/" + page.toLowerCase(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            setData(Listing.fromJson(new JSONObject(response)));
                            for (Listener listener : listeners) {
                                listener.setRefreshing(
                                        false);
                                // TODO: Check if reset necessary
                                listener.getAdapter().notifyDataSetChanged();
                            }
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

    public void setData(Listing data) {
        this.data = data;
    }

    @Override
    public Link getMainLink() {
        return link;
    }

    @Override
    public void loadMoreComments() {
        // Not implemented
    }

    @Override
    public boolean hasChildren(Comment comment) {
        return false;
    }

    @Override
    public void editComment(final Comment comment, String text) {

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("text", TextUtils.htmlEncode(text));
        params.put("thing_id", comment.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/editusertext", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Comment newComment = Comment.fromJson(new JSONObject(response).getJSONObject("json").getJSONObject("data").getJSONArray("things").getJSONObject(0), comment.getLevel());
                    comment.setBodyHtml(newComment.getBodyHtml());
                    int commentIndex = data.getChildren()
                            .indexOf(comment);
                    Log.d(TAG, "commentIndex: " + commentIndex);

                    if (commentIndex > -1) {
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyItemChanged(commentIndex);
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

    @Override
    public Reddit getReddit() {
        return reddit;
    }

    @Override
    public int sizeLinks() {
        // Not necessary
        return data.getChildren().size();
    }

    @Override
    public boolean isLoading() {
        return false;
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

    public void insertMessage(Message message) {

        Message parentMessage = new Message();
        parentMessage.setId(message.getParentId());

        int messageIndex = link.getComments().getChildren().indexOf(parentMessage);
        if (messageIndex > -1) {
            data.getChildren()
                    .add(messageIndex + 1, message);
        }

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemInserted(messageIndex + 1);
        }
    }

    @Override
    public void insertComments(Comment moreComment, Listing listing) {
        // Not implemented
    }

    @Override
    public void insertComment(Comment comment) {

        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());

        int commentIndex = link.getComments().getChildren().indexOf(parentComment);
        if (commentIndex > -1) {
            data.getChildren()
                    .add(commentIndex + 1, comment);
        }

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemInserted(commentIndex + 1);
        }
    }

    @Override
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

    @Override
    public boolean toggleComment(int position) {
        // Not implemented
        return true;
    }

    @Override
    public void expandComment(int position) {
        // Not implemented
    }

    @Override
    public void collapseComment(int position) {
        // Not implemented
    }

    @Override
    public void voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment,
            int vote) {

        reddit.voteComment(viewHolder, comment, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public int getIndentWidth(Comment comment) {
        // Not implemented
        return 0;
    }

    @Override
    public void loadNestedComments(Comment moreComment) {
        // Not implemented
    }

    @Override
    public boolean isCommentExpanded(int position) {
        // Not implemented
        return true;
    }

    public User getUser() {
        return user;
    }

    public interface Listener extends ControllerListener {

    }

}
