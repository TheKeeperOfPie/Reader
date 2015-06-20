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

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
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
import java.util.Map;
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
    private boolean isLoading;
    private ControllerLinks controllerLinks;

    public ControllerComments(Activity activity,
            String subreddit,
            String linkId) {
        setActivity(activity);
        this.listeners = new HashSet<>();
        this.subreddit = subreddit;
        this.linkId = linkId;
    }

    public ControllerComments(Activity activity, JSONObject data) {
        this(activity, "", "");
        try {
            setLinkId(data.getString("subreddit"), data.getString("linkId"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String saveData() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("subreddit", subreddit);
        jsonObject.put("linkId", linkId);
        return jsonObject.toString();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        this.reddit = Reddit.getInstance(activity);
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                activity.getResources()
                        .getDisplayMetrics());
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
    }

    public void addListener(CommentClickListener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapter()
                .notifyDataSetChanged();
    }

    public void removeListener(CommentClickListener listener) {
        listeners.remove(listener);
    }

    public void setTitle() {
        for (CommentClickListener listener : listeners) {
            listener.setToolbarTitle(link == null ? "" : Reddit.getTrimmedHtml(link.getTitle()));
        }
    }

    public void setLink(Link link) {
        setLinkId(link.getSubreddit(), link.getId());
        Listing listing = new Listing();
        // TODO: Make this logic cleaner
        if (link.getComments() != null) {
            listing.setChildren(new ArrayList<>(link.getComments()
                    .getChildren()));
        }
        else {
            listing.setChildren(new ArrayList<Thing>());
        }
        this.listingComments = listing;
        this.link = link;
        for (CommentClickListener listener : listeners) {
            listener.getAdapter()
                    .notifyDataSetChanged();
        }
    }

    public void setLinkId(String subreddit, String linkId) {

        link = new Link();
        link.setSubreddit(subreddit);
        link.setId(linkId);

        this.subreddit = subreddit;
        this.linkId = linkId;
        reloadAllComments();
    }

    public void reloadAllComments() {
        if (TextUtils.isEmpty(linkId)) {
            setRefreshing(false);
            return;
        }

        setRefreshing(true);
        reddit.loadGet(
                Reddit.OAUTH_URL + "/r/" + subreddit + "/comments/" + linkId + "?depth=10&showmore=true&showedits=true&limit=100",
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        for (int index = 0; index < response.length() / 500; index++) {
                            Log.d(TAG, "reloadAllComments onResponse: " + response.substring(
                                    index * 500,
                                    (index + 1) * 500 > response.length() ? response.length() :
                                            (index + 1) * 500));
                        }
                        try {
                            Listing listing = new Listing();
                            link = Link.fromJson(new JSONArray(response));
                            // TODO: Make this logic cleaner
                            if (link.getComments() != null) {
                                listing.setChildren(new ArrayList<>(link.getComments()
                                        .getChildren()));
                            }
                            else {
                                listing.setChildren(new ArrayList<Thing>());
                            }
                            listingComments = listing;
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
                        setRefreshing(false);
                    }
                }, 0);
    }

    private void setRefreshing(boolean refreshing) {
        isLoading = refreshing;
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

        if (TextUtils.isEmpty(thumbnail) || thumbnail.equals(Reddit.DEFAULT) || thumbnail.equals(
                Reddit.NSFW)) {
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
        return isLoading;
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
    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    @Override
    public boolean showSubreddit() {
        return true;
    }

    @Override
    public void deletePost(Link link) {
        controllerLinks.deletePost(link);
    }

    @Override
    public void loadNestedComments(final Comment moreComment) {

        Log.d(TAG, "loadNestedComments");

        String url = Reddit.OAUTH_URL + "/api/morechildren";

        String children = "";
        List<String> childrenList = moreComment.getChildren();
        if (childrenList.isEmpty()) {
            int commentIndex = listingComments.getChildren()
                    .indexOf(moreComment);
            if (commentIndex >= 0) {
                listingComments.getChildren()
                        .remove(commentIndex);
                for (CommentClickListener listener : listeners) {
                    listener.getAdapter()
                            .notifyItemRemoved(commentIndex + 1);
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

                            JSONArray jsonArray = new JSONObject(response).getJSONObject("json")
                                    .getJSONObject("data")
                                    .getJSONArray("things");

                            Listing listing = new Listing();
                            List<Thing> things = new ArrayList<>();
                            List<Thing> comments = new ArrayList<>(jsonArray.length());

                            for (int index = 0; index < jsonArray.length(); index++) {
                                Log.d(TAG, "thing: " + jsonArray.getJSONObject(index));
                                Comment comment = Comment.fromJson(jsonArray.getJSONObject(index),
                                        moreComment.getLevel());

                                if (comment.getParentId()
                                        .equals(link.getId())) {
                                    comments.add(comment);
                                }
                                else {
                                    int commentIndex = -1;

                                    for (int position = 0; position < comments.size(); position++) {
                                        if (comments.get(position)
                                                .getId()
                                                .equals(comment.getParentId())) {
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
                            }
                            if (comments.isEmpty()) {
                                int commentIndex = link.getComments()
                                        .getChildren()
                                        .indexOf(moreComment);
                                if (commentIndex >= 0) {
                                    link.getComments()
                                            .getChildren()
                                            .remove(commentIndex);
                                }
                                commentIndex = listingComments.getChildren()
                                        .indexOf(moreComment);
                                if (commentIndex >= 0) {
                                    listingComments.getChildren()
                                            .remove(commentIndex);
                                    for (CommentClickListener listener : listeners) {
                                        listener.getAdapter()
                                                .notifyItemRemoved(commentIndex + 1);
                                    }
                                }
                            }
                            else {
                                things.addAll(comments);
                                listing.setChildren(things);
                                insertComments(moreComment, listing);
                            }

                            setRefreshing(false);
                        }
                        catch (JSONException e1) {
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
        int commentIndex = link.getComments()
                .getChildren()
                .indexOf(moreComment);
        if (commentIndex >= 0) {
            link.getComments()
                    .getChildren()
                    .remove(commentIndex);
            for (int index = listComments.size() - 1; index >= 0; index--) {
                Comment comment = (Comment) listComments.get(index);
                link.getComments()
                        .getChildren()
                        .add(commentIndex, comment);
            }
        }

        commentIndex = listingComments.getChildren()
                .indexOf(moreComment);
        if (commentIndex >= 0) {
            listingComments.getChildren()
                    .remove(commentIndex);
            for (CommentClickListener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRemoved(commentIndex + 1);
            }

            for (int index = listComments.size() - 1; index >= 0; index--) {
                Comment comment = (Comment) listComments.get(index);
                listingComments.getChildren()
                        .add(commentIndex, comment);
            }

            for (CommentClickListener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRangeInserted(commentIndex + 1, listComments.size());
            }
        }

        link.getComments()
                .checkChildren();
        listingComments.checkChildren();

    }

    @Override
    public void insertComment(Comment comment) {

        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());
        int commentIndex;

        if (link.getComments() != null) {
            commentIndex = link.getComments()
                    .getChildren()
                    .indexOf(parentComment);
            link.getComments()
                    .getChildren()
                    .add(commentIndex + 1, comment);
        }

        if (listingComments != null) {
            commentIndex = listingComments.getChildren()
                    .indexOf(parentComment);
            listingComments.getChildren()
                    .add(commentIndex + 1, comment);

            // TODO: Fix index offset as this will not work with Profile page
            for (CommentClickListener listener : listeners) {
                listener.getAdapter()
                        .notifyItemInserted(commentIndex + 2);
            }
        }
    }

    @Override
    public void deleteComment(Comment comment) {

        int commentIndex = link.getComments()
                .getChildren()
                .indexOf(comment);
        if (commentIndex > -1) {
            Comment newComment = (Comment) link.getComments()
                    .getChildren()
                    .get(commentIndex);
            newComment.setBodyHtml(Comment.HTML_DELETED);
            newComment.setAuthor("[deleted]");
//            link.getComments().getChildren().set(commentIndex, newComment);
        }

        commentIndex = listingComments.getChildren()
                .indexOf(comment);
        if (commentIndex > -1) {
            Comment newComment = (Comment) listingComments
                    .getChildren()
                    .get(commentIndex);
            newComment.setBodyHtml(Comment.HTML_DELETED);
            newComment.setAuthor("[deleted]");
//            listingComments.getChildren().set(commentIndex, newComment);

            for (CommentClickListener listener : listeners) {
                listener.getAdapter()
                        .notifyItemChanged(commentIndex + 1);
            }
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
    /**
     * Toggles children of comment
     *
     * @param position
     * @return true if comment is now expanded, false if collapsed
     */
    public boolean toggleComment(int position) {

        position = position - 1;

        if (position == listingComments.getChildren()
                .size() - 1) {
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
        else if (comment.getLevel() < nextComment.getLevel()) {
            collapseComment(position);
            return false;
        }

        return false;

    }

    @Override
    public void expandComment(int position) {
        List<Thing> commentList = link.getComments()
                .getChildren();
        int index = commentList.indexOf(listingComments.getChildren()
                .get(position));
        if (index < 0) {
            return;
        }
        List<Comment> commentsToInsert = new LinkedList<>();
        Comment comment = (Comment) commentList.get(index);
        int numAdded = 0;
        while (++index < commentList.size() && ((Comment) commentList.get(
                index)).getLevel() != comment.getLevel()) {
            commentsToInsert.add((Comment) commentList.get(index));
        }

        for (int insertIndex = commentsToInsert.size() - 1; insertIndex >= 0; insertIndex--) {
            listingComments.getChildren()
                    .add(position + 1, commentsToInsert.get(insertIndex));
            numAdded++;
        }

        for (CommentClickListener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRangeInserted(position + 2, numAdded);
        }
    }

    @Override
    public void collapseComment(int position) {
        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        position++;
        int numRemoved = 0;
        while (position < commentList.size() && ((Comment) commentList.get(
                position)).getLevel() != comment.getLevel()) {
            commentList.remove(position);
            numRemoved++;
        }
        for (CommentClickListener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRangeRemoved(position + 1, numRemoved);
        }
    }

    @Override
    public void voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment, final int vote) {

        reddit.voteComment(viewHolder, comment, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public void voteLink(final RecyclerView.ViewHolder viewHolder,
            final Link link,
            final int vote) {

        reddit.voteLink(viewHolder, link, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
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
        if (listingComments == null || listingComments.getChildren()
                .isEmpty()) {
            return 1;
        }

        return listingComments.getChildren()
                .size() + 1;
    }

    @Override
    public Comment getComment(int position) {
        return (Comment) listingComments.getChildren()
                .get(position - 1);
    }

    @Override
    public boolean isCommentExpanded(int position) {
        position = position - 1;

        if (position == listingComments.getChildren()
                .size() - 1) {
            return false;
        }

        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        Comment nextComment = (Comment) commentList.get(position + 1);

        if (comment.getLevel() == nextComment.getLevel()) {
            return false;
        }
        else if (comment.getLevel() < nextComment.getLevel()) {
            return true;
        }

        return false;
    }

    @Override
    public Link getMainLink() {
        return link;
    }

    @Override
    public void loadMoreComments() {
    }

    @Override
    public boolean hasChildren(Comment comment) {

        Log.d(TAG, "comment level: " + comment.getLevel());

        int commentIndex = link.getComments()
                .getChildren()
                .indexOf(comment);

        if (commentIndex > -1 && commentIndex + 1 < link.getComments()
                .getChildren()
                .size()) {
            Comment nextComment = (Comment) link.getComments()
                    .getChildren()
                    .get(commentIndex + 1);
            Log.d(TAG, "next level: " + nextComment.getLevel());
            return nextComment.getLevel() > comment.getLevel();

        }

        return false;
    }

    public void setControllerLinks(ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
    }

    public int getPreviousCommentPosition(int commentIndex) {

        Log.d(TAG, "commentIndex: " + commentIndex);

        for (int index = commentIndex - 1; index >= 0; index--) {
            if (((Comment) listingComments.getChildren()
                    .get(index)).getLevel() == 0) {
                return index;
            }
        }

        return commentIndex;
    }

    public int getNextCommentPosition(int commentIndex) {
        Log.d(TAG, "commentIndex: " + commentIndex);

        for (int index = commentIndex + 1; index < listingComments.getChildren()
                .size(); index++) {
            if (((Comment) listingComments.getChildren()
                    .get(index)).getLevel() == 0) {
                return index;
            }
        }

        return commentIndex;
    }

    public interface CommentClickListener extends DisallowListener {

        void loadUrl(String url);
        void setRefreshing(boolean refreshing);
        AdapterCommentList getAdapter();
        int getRecyclerHeight();
        int getRecyclerWidth();
        void setToolbarTitle(CharSequence title);
        void loadYouTube(Link link, String id, AdapterLink.ViewHolderBase viewHolderBase);
        boolean hideYouTube();
    }

    public interface ListenerCallback {
        CommentClickListener getCommentClickListener();
        ControllerCommentsBase getControllerComments();
        SharedPreferences getPreferences();
        User getUser();
        float getItemWidth();
    }

}