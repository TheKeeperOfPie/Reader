package com.winsonchiu.reader;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;
import com.winsonchiu.reader.data.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerComments implements ControllerLinksBase, ControllerCommentsBase {

    private static final long ALPHA_DURATION = 500;
    private static final String TAG = ControllerComments.class.getCanonicalName();

    private Activity activity;
    private SharedPreferences preferences;
    private Set<CommentClickListener> listeners;
    private Link link;
    private Listing listingComments;
    private String subreddit;
    private String linkId;
    private int indentWidth;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Reddit reddit;

    public ControllerComments(Activity activity, String subreddit, String linkId) {
        setActivity(activity);
        this.reddit = Reddit.getInstance(activity);
        this.listeners = new HashSet<>();
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, activity.getResources().getDisplayMetrics());
        this.subreddit = subreddit;
        this.linkId = linkId;
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
    }

    public void addListener(CommentClickListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CommentClickListener listener) {
        listeners.remove(listener);
    }

    public void setLink(Link link) {
        Listing listing = new Listing();
        // TODO: Make this logic cleaner
        if (link.getComments() != null) {
            listing.setChildren(new ArrayList<>(link.getComments().getChildren()));
        }
        else {
            listing.setChildren(new ArrayList<Thing>());
        }
        this.listingComments = listing;
        this.link = link;
        this.subreddit = link.getSubreddit();
        this.linkId = link.getId();
        for (CommentClickListener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }
    }

    public void setLinkId(String subreddit, String linkId) {
        this.subreddit = subreddit;
        this.linkId = linkId;
        reloadAllComments();
    }

    public void reloadAllComments() {
        if (TextUtils.isEmpty(linkId)) {
            setRefreshing(false);
            return;
        }

        reddit.loadGet(Reddit.OAUTH_URL + "/r/" + subreddit + "/comments/" + linkId + "?depth=10",
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "reloadAllComments onResponse: " + response);
                        try {
                            setLink(Link.fromJson(new JSONArray(response)));
                            setRefreshing(false);
                            for (CommentClickListener listener : listeners) {
                                listener.getAdapter()
                                        .notifyDataSetChanged();
                            }
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "reloadAllComments onErrorResponse: " + error.toString());
                    }
                }, 0);
    }

    private void setRefreshing(boolean refreshing) {
        for (CommentClickListener listener : listeners) {
            listener.setRefreshing(refreshing);
        }
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

    @Override
    public int sizeLinks() {
        return 1;
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
    public Activity getActivity() {
        return activity;
    }

    @Override
    public void loadMoreComments(final Comment moreComment) {

        String url = Reddit.OAUTH_URL + "/api/morechildren";


        String children = "";
        List<String> childrenList = moreComment.getChildren();
        if (childrenList.isEmpty()) {
            int commentIndex = listingComments.getChildren().indexOf(moreComment);
            if (commentIndex >= 0) {
                listingComments.getChildren()
                        .remove(commentIndex);
                for (CommentClickListener listener : listeners) {
                    listener.getAdapter().notifyItemRemoved(commentIndex + 1);
                }
            }
            return;
        }
        for (String id : childrenList) {
            children += id + ",";
        }

        HashMap<String, String> params = new HashMap<>();
        params.put("link_id", link.getName());
        params.put("children", children.substring(0, children.length() - 1));
        params.put("api_type", "json");

        reddit.loadPost(url,
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "moreComments response: " + response);
                            JSONArray jsonArray = new JSONObject(response).getJSONObject("json").getJSONObject("data").getJSONArray("things");

                            Listing listing = new Listing();
                            List<Thing> things = new ArrayList<>();
                            List<Thing> comments = new ArrayList<>(jsonArray.length());

                            for (int index = 0; index < jsonArray.length(); index++) {
                                Comment comment = Comment.fromJson(jsonArray.getJSONObject(index), moreComment.getLevel());

                                int commentIndex = -1;

                                for (int position = 0; position < comments.size(); position++) {
                                    if (comments.get(position).getId().equals(comment.getParentId())) {
                                        commentIndex = position;
                                        break;
                                    }
                                }

                                if (commentIndex >= 0) {
                                    comment.setLevel(((Comment) comments.get(commentIndex))
                                            .getLevel() + 1);
                                }
                                comments.add(commentIndex + 1, comment);
                            }
                            if (comments.isEmpty()) {
                                int commentIndex = link.getComments().getChildren().indexOf(moreComment);
                                if (commentIndex >= 0) {
                                    link.getComments().getChildren().remove(commentIndex);
                                }
                                commentIndex = listingComments.getChildren().indexOf(moreComment);
                                if (commentIndex >= 0) {
                                    listingComments.getChildren().remove(commentIndex);
                                    for (CommentClickListener listener : listeners) {
                                        listener.getAdapter().notifyItemRemoved(commentIndex + 1);
                                    }
                                }
                            }
                            else {
                                things.addAll(comments);
                                listing.setChildren(things);
                                insertComments(moreComment, listing);
                            }

                            setRefreshing(false);
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "error" + error.toString());
                    }
                }, params, 0);
    }

    @Override
    public void insertComments(Comment moreComment, Listing listing) {

        List<Thing> listComments = listing.getChildren();
        int commentIndex = link.getComments().getChildren().indexOf(moreComment);
        if (commentIndex >= 0) {
            link.getComments().getChildren().remove(commentIndex);
            for (int index = listComments.size() - 1; index >= 0; index--) {
                Comment comment = (Comment) listComments.get(index);
                link.getComments().getChildren().add(commentIndex, comment);
            }
        }

        commentIndex = listingComments.getChildren().indexOf(moreComment);
        if (commentIndex >= 0) {
            listingComments.getChildren().remove(commentIndex);
            for (CommentClickListener listener : listeners) {
                listener.getAdapter().notifyItemRemoved(commentIndex + 1);
            }

            for (int index = listComments.size() - 1; index >= 0; index--) {
                Comment comment = (Comment) listComments.get(index);
                listingComments.getChildren().add(commentIndex, comment);
            }

            for (CommentClickListener listener : listeners) {
                listener.getAdapter().notifyItemRangeInserted(commentIndex + 1, listComments.size());
            }
        }

    }

    @Override
    public void insertComment(Comment comment) {

        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());
        int commentIndex;

        if (link.getComments() != null) {
            commentIndex = link.getComments().getChildren().indexOf(parentComment);
            link.getComments()
                    .getChildren()
                    .add(commentIndex + 1, comment);
        }

        if (listingComments != null) {
            commentIndex = listingComments.getChildren().indexOf(parentComment);
            listingComments.getChildren()
                    .add(commentIndex + 1, comment);

            // TODO: Fix index offset as this will not work with Profile page
            for (CommentClickListener listener : listeners) {
                listener.getAdapter().notifyItemInserted(commentIndex + 2);
            }
        }
    }

    @Override
    public void deleteComment(Comment comment) {

        int commentIndex = link.getComments().getChildren().indexOf(comment);
        if (commentIndex > -1) {
            Comment newComment = (Comment) link.getComments()
                    .getChildren()
                    .get(commentIndex);
            newComment.setBodyHtml(Comment.HTML_DELETED);
            newComment.setAuthor("[deleted]");
//            link.getComments().getChildren().set(commentIndex, newComment);
        }

        commentIndex = listingComments.getChildren().indexOf(comment);
        if (commentIndex > -1) {
            Comment newComment = (Comment) listingComments
                    .getChildren()
                    .get(commentIndex);
            newComment.setBodyHtml(Comment.HTML_DELETED);
            newComment.setAuthor("[deleted]");
//            listingComments.getChildren().set(commentIndex, newComment);

            for (CommentClickListener listener : listeners) {
                listener.getAdapter().notifyItemChanged(commentIndex + 1);
            }
        }
    }

    @Override
    /**
     * Toggles children of comment
     *
     * @param position
     * @return true if comment is now expanded, false if collapsed
     */
    public boolean toggleComment(int position) {

        position = position - 1;

        if (position == listingComments.getChildren().size() - 1) {
            expandComment(position);
            return true;
        }

        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        Comment nextComment = (Comment) commentList.get(position + 1);

        if (comment.getLevel() == nextComment.getLevel()) {
            expandComment(position);
            return true;
        }
        else if (comment.getLevel() < nextComment.getLevel()){
            collapseComment(position);
            return false;
        }

        return false;

    }

    @Override
    public void expandComment(int position) {
        List<Thing> commentList = link.getComments().getChildren();
        int index = commentList.indexOf(listingComments.getChildren().get(position));
        if (index < 0) {
            return;
        }
        List<Comment> commentsToInsert = new LinkedList<>();
        Comment comment = (Comment) commentList.get(index);
        int numAdded = 0;
        while (++index < commentList.size() && ((Comment) commentList.get(index)).getLevel() != comment.getLevel()) {
            commentsToInsert.add((Comment) commentList.get(index));
        }

        for (int insertIndex = commentsToInsert.size() - 1; insertIndex >= 0; insertIndex--) {
            listingComments.getChildren().add(position + 1, commentsToInsert.get(insertIndex));
            numAdded++;
        }

        for (CommentClickListener listener : listeners) {
            listener.getAdapter().notifyItemRangeInserted(position + 2, numAdded);
        }
    }

    @Override
    public void collapseComment(int position) {
        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        position++;
        int numRemoved = 0;
        while (position < commentList.size() && ((Comment) commentList.get(position)).getLevel() != comment.getLevel()) {
            commentList.remove(position);
            numRemoved++;
        }
        for (CommentClickListener listener : listeners) {
            listener.getAdapter().notifyItemRangeRemoved(position + 1, numRemoved);
        }
    }

    @Override
    public boolean voteComment(final AdapterCommentList.ViewHolderComment viewHolder, final int vote) {

        if (TextUtils.isEmpty(preferences.getString(AppSettings.REFRESH_TOKEN, null))) {
            Toast.makeText(activity, "Must be logged in to vote", Toast.LENGTH_SHORT).show();
            return false;
        }

        final int position = viewHolder.getAdapterPosition();
        final Comment comment = (Comment) listingComments.getChildren().get(viewHolder.getAdapterPosition());

        final int oldVote = comment.isLikes();
        int newVote = 0;

        if (comment.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, comment.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        comment.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            viewHolder.setVoteColors();
        }
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();

                comment.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    viewHolder.setVoteColors();
                }
            }
        }, params, 0);
        return true;
    }

    @Override
    public void voteLink(final RecyclerView.ViewHolder viewHolder, final int vote) {

        if (TextUtils.isEmpty(preferences.getString(AppSettings.REFRESH_TOKEN, null))) {
            Toast.makeText(activity, "Must be logged in to vote", Toast.LENGTH_SHORT).show();
            return;
        }

        final int position = viewHolder.getAdapterPosition();

        final int oldVote = link.isLikes();
        int newVote = 0;

        if (link.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, link.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        link.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo(link);
            }
            else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
            }
        }
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();

                link.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                        ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                        ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo(link);
                    }
                    else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                        ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
                    }
                }
            }
        }, params, 0);
    }

    @Override
    public int getIndentWidth(Comment comment) {
        return indentWidth * comment.getLevel();
    }

    @Override
    public Link getLink(int position) {
        return link;
    }

    @Override
    public Reddit getReddit() {
        return reddit;
    }

    public void animateAlpha(View view, float start, float end) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", start, end);
        objectAnimator.setDuration(ALPHA_DURATION);
        objectAnimator.start();
    }

    public void clear() {
        listingComments = new Listing();
        link = new Link();
    }

    public int getItemCount() {

        if (link == null || TextUtils.isEmpty(link.getId())) {
            return 0;
        }
        if (listingComments == null || listingComments.getChildren().isEmpty()) {
            return 1;
        }

        return listingComments.getChildren().size() + 1;
    }

    @Override
    public Comment getComment(int position) {
        return (Comment) listingComments.getChildren().get(position - 1);
    }

    @Override
    public boolean isCommentExpanded(int position) {
        position = position - 1;

        if (position == listingComments.getChildren().size() - 1) {
            return false;
        }

        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        Comment nextComment = (Comment) commentList.get(position + 1);

        if (comment.getLevel() == nextComment.getLevel()) {
            return false;
        }
        else if (comment.getLevel() < nextComment.getLevel()){
            return true;
        }

        return false;
    }

    @Override
    public Link getMainLink() {
        return link;
    }

    public interface CommentClickListener extends DisallowListener {

        void loadUrl(String url);
        void setRefreshing(boolean refreshing);
        AdapterCommentList getAdapter();
        int getRecyclerHeight();
        int getRecyclerWidth();
    }

    public interface ListenerCallback {
        CommentClickListener getCommentClickListener();
        ControllerCommentsBase getControllerComments();
        SharedPreferences getPreferences();
        User getUser();
        float getItemWidth();
    }

}