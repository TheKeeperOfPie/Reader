package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Message;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class ControllerInbox implements ControllerCommentsBase {

    public static final int VIEW_TYPE_MESSAGE = 0;
    public static final int VIEW_TYPE_COMMENT = 1;
    private static final String TAG = ControllerInbox.class.getCanonicalName();

    private Set<ItemClickListener> listeners;
    private Listing data;
    private Reddit reddit;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Link link;
    private String page;
    private User user;

    public ControllerInbox(Activity activity) {
        data = new Listing();
        listeners = new HashSet<>();
        this.reddit = Reddit.getInstance(activity);
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        link = new Link();
        page = "Inbox";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
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

    public void addListener(ItemClickListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ItemClickListener listener) {
        listeners.remove(listener);
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
                            for (ItemClickListener listener : listeners) {
                                listener.setRefreshing(
                                        false);
                                // TODO: Check if reset necessary
                                listener.resetRecycler();
                                listener.setToolbarTitle("Inbox");
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
                        for (ItemClickListener listener : listeners) {
                            listener.setRefreshing(
                                    false);
                            listener.resetRecycler();
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
    public Reddit getReddit() {
        return reddit;
    }

    @Override
    public void voteLink(RecyclerView.ViewHolder viewHolder, int vote) {

    }

    @Override
    public Drawable getDrawableForLink(Link link) {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return drawableSelf;
        }

        if (TextUtils.isEmpty(thumbnail) || thumbnail.equals(Reddit.DEFAULT) || thumbnail.equals(Reddit.NSFW)) {
            return drawableDefault;
        }

        return null;
    }

    public void insertMessage(Message message) {

        Message parentMessage = new Message();
        parentMessage.setId(message.getParentId());

        int messageIndex = link.getComments().getChildren().indexOf(parentMessage);
        if (messageIndex > -1) {
            data.getChildren()
                    .add(messageIndex + 1, message);
        }

        for (ItemClickListener listener : listeners) {
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

        for (ItemClickListener listener : listeners) {
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

        for (ItemClickListener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(commentIndex);
        }
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
    public boolean voteComment(AdapterCommentList.ViewHolderComment viewHolder,
                               int vote) {
        // Not implemented
        return false;
    }

    @Override
    public int getIndentWidth(Comment comment) {
        // Not implemented
        return 0;
    }

    @Override
    public void loadMoreComments(Comment moreComment) {
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

    public interface ItemClickListener extends DisallowListener {

        void onClickComments(Link link, RecyclerView.ViewHolder viewHolder);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        void setToolbarTitle(String title);
        AdapterInbox getAdapter();
        int getRecyclerHeight();
        void resetRecycler();
        void setSwipeRefreshEnabled(boolean enabled);
        int getRecyclerWidth();
    }

    public interface ListenerCallback {
        ItemClickListener getListener();
        ControllerInbox getController();
        int getColorPositive();
        int getColorNegative();
        int getColorMuted();
        Activity getActivity();
        float getItemWidth();
        SharedPreferences getPreferences();
    }

}
