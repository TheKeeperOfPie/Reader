package com.winsonchiu.reader;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;

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
public class ControllerComments {

    private static final String TAG = ControllerComments.class.getCanonicalName();

    private Activity activity;
    private Set<Listener> listeners;
    private Link link;
    private Listing listingComments;
    private String subreddit;
    private String linkId;
    private int indentWidth;
    private Reddit reddit;
    private boolean isLoading;
    private boolean isCommentThread;

    public ControllerComments(Activity activity,
            String subreddit,
            String linkId) {
        setActivity(activity);
        this.listeners = new HashSet<>();
        this.subreddit = subreddit;
        this.linkId = linkId;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                activity.getResources()
                        .getDisplayMetrics());
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        setTitle();
        listener.getAdapter().notifyDataSetChanged();
        listener.setRefreshing(isLoading());
        listener.setIsCommentThread(isCommentThread);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle(link == null ? "" : Reddit.getTrimmedHtml(link.getTitle()));
        }
    }

    public void setLink(Link link) {
        setLinkId(link.getSubreddit(), link.getId());
//        Listing listing = new Listing();
//        // TODO: Make this logic cleaner
//        if (link.getComments() != null) {
//            listing.setChildren(new ArrayList<>(link.getComments()
//                    .getChildren()));
//        }
//        else {
//            listing.setChildren(new ArrayList<Thing>());
//        }
        this.listingComments = new Listing();
        this.link = link;
//        for (Listener listener : listeners) {
//            listener.getAdapter().notifyDataSetChanged();
//        }
    }

    public void setLinkId(String subreddit, String linkId) {
        setLinkIdValues(subreddit, linkId);
        reloadAllComments();
    }

    public void setLinkId(String subreddit, String linkId, String commentId) {
        setLinkIdValues(subreddit, linkId);
        loadCommentThread(commentId);
    }

    private void setLinkIdValues(String subreddit, String linkId) {
        link = new Link();
        link.setSubreddit(subreddit);
        link.setId(linkId);

        this.subreddit = subreddit;
        this.linkId = linkId;
    }

    public void reloadAllComments() {
        if (TextUtils.isEmpty(linkId)) {
            setRefreshing(false);
            return;
        }

        if (!link.getComments().getChildren().isEmpty()) {
            Comment commentFirst = ((Comment) link.getComments().getChildren().get(0));
            if (!commentFirst.getParentId().equals(link.getId())) {
                loadCommentThread(commentFirst.getId());
                return;
            }
        }

        loadLinkComments();

    }

    public void loadLinkComments() {
        setRefreshing(true);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/r/" + subreddit + "/comments/" + linkId + "?depth=10&showmore=true&showedits=true&limit=100",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            Listing listing = new Listing();
                            link = Link.fromJson(new JSONArray(response));

                            // For some reason Reddit doesn't report the link author, so we'll do it manually
                            for (Thing thing : link.getComments().getChildren()) {
                                Comment comment = (Comment) thing;
                                comment.setLinkAuthor(link.getAuthor());
                            }

                            // TODO: Make this logic cleaner
                            if (link.getComments() != null) {
                                listing.setChildren(new ArrayList<>(link.getComments()
                                        .getChildren()));
                            }
                            else {
                                listing.setChildren(new ArrayList<Thing>());
                            }
                            listingComments = listing;
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyDataSetChanged();
                            }
                            setTitle();
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        setIsCommentThread(false);
                        setRefreshing(false);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "reloadAllComments onErrorResponse: " + error.toString());
                        setRefreshing(false);
                    }
                }, 0);
    }

    public void loadCommentThread(String commentId) {

        setRefreshing(true);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/r/" + subreddit + "/comments/" + linkId + "?depth=10&showmore=true&showedits=true&limit=100&context=3&comment=" + commentId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            Listing listing = new Listing();
                            link = Link.fromJson(new JSONArray(response));

                            // For some reason Reddit doesn't report the link author, so we'll do it manually
                            for (Thing thing : link.getComments().getChildren()) {
                                Comment comment = (Comment) thing;
                                comment.setLinkAuthor(link.getAuthor());
                            }

                            // TODO: Make this logic cleaner
                            if (link.getComments() != null) {
                                listing.setChildren(new ArrayList<>(link.getComments()
                                        .getChildren()));
                            }
                            else {
                                listing.setChildren(new ArrayList<Thing>());
                            }
                            listingComments = listing;
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyDataSetChanged();
                            }
                            setTitle();
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        setIsCommentThread(true);
                        setRefreshing(false);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error.toString());
                        setRefreshing(false);
                    }
                }, 0);
    }

    private void setRefreshing(boolean refreshing) {
        isLoading = refreshing;
        for (Listener listener : listeners) {
            listener.setRefreshing(refreshing);
        }
    }

    private void setIsCommentThread(boolean isCommentThread) {
        this.isCommentThread = isCommentThread;
        for (Listener listener : listeners) {
            listener.setIsCommentThread(isCommentThread);
        }
    }

    public int sizeLinks() {
        return 1;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getSubredditName() {
        return subreddit;
    }

    public boolean showSubreddit() {
        return true;
    }

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
                for (Listener listener : listeners) {
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
                new Response.Listener<String>() {
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


                                // For some reason Reddit doesn't report the link author, so we'll do it manually
                                comment.setLinkAuthor(link.getAuthor());

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
                                    for (Listener listener : listeners) {
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
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                        setRefreshing(false);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setRefreshing(false);
                        Log.d(TAG, "error" + error.toString());
                    }
                }, params, 0);
    }

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
            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRemoved(commentIndex + 1);
            }

            for (int index = listComments.size() - 1; index >= 0; index--) {
                Comment comment = (Comment) listComments.get(index);
                listingComments.getChildren()
                        .add(commentIndex, comment);
            }

            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRangeInserted(commentIndex + 1, listComments.size());
            }
        }

        link.getComments()
                .checkChildren();
        listingComments.checkChildren();

    }

    public void insertComment(Comment comment) {

        // Check to see if comment is actually a part of the link's comment thread
        if (!comment.getLinkId().equals(link.getName())) {
            return;
        }

        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());
        int commentIndex;

        if (link.getComments() != null) {
            commentIndex = link.getComments()
                    .getChildren()
                    .indexOf(parentComment);
            if (commentIndex > -1) {
                comment.setLevel(
                        ((Comment) link.getComments().getChildren().get(commentIndex)).getLevel() + 1);
            }
            link.getComments()
                    .getChildren()
                    .add(commentIndex + 1, comment);
        }

        if (listingComments != null) {
            commentIndex = listingComments.getChildren()
                    .indexOf(parentComment);
            listingComments.getChildren()
                    .add(commentIndex + 1, comment);

            if (commentIndex > -1) {
                comment.setLevel(((Comment) listingComments.getChildren().get(commentIndex)).getLevel() + 1);
            }
            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemInserted(commentIndex + 2);
            }
        }
    }

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
        }

        commentIndex = listingComments.getChildren()
                .indexOf(comment);
        if (commentIndex > -1) {
            Comment newComment = (Comment) listingComments
                    .getChildren()
                    .get(commentIndex);
            newComment.setBodyHtml(Comment.HTML_DELETED);
            newComment.setAuthor("[deleted]");

            for (Listener listener : listeners) {
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

        for (Listener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRangeInserted(position + 2, numAdded);
        }
    }

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
        for (Listener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRangeRemoved(position + 1, numRemoved);
        }
    }

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

    public Link getLink(int position) {
        return link;
    }

    public Reddit getReddit() {
        return reddit;
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

    public Comment getComment(int position) {
        return (Comment) listingComments.getChildren()
                .get(position - 1);
    }

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

    public Link getMainLink() {
        return link;
    }

    public void loadMoreComments() {
    }

    public boolean hasChildren(Comment comment) {

        int commentIndex = link.getComments()
                .getChildren()
                .indexOf(comment);

        if (commentIndex > -1 && commentIndex + 1 < link.getComments()
                .getChildren()
                .size()) {
            Comment nextComment = (Comment) link.getComments()
                    .getChildren()
                    .get(commentIndex + 1);
            return nextComment.getLevel() > comment.getLevel();

        }

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
                    Log.d(TAG, "response: " + response);
                    Comment newComment = Comment.fromJson(new JSONObject(response).getJSONObject("json").getJSONObject("data").getJSONArray("things").getJSONObject(0), comment.getLevel());
                    comment.setBodyHtml(newComment.getBodyHtml());
                    int commentIndex = listingComments.getChildren()
                            .indexOf(comment);

                    Log.d(TAG, "commentIndex: " + commentIndex);

                    if (commentIndex > -1) {
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyItemChanged(commentIndex + 1);
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

    public interface Listener extends ControllerListener{
        void setIsCommentThread(boolean isCommentThread);
    }

}