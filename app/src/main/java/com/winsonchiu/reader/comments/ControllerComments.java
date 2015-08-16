/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.utils.ControllerListener;

import java.io.IOException;
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

    private Link link = new Link();
    private Set<Listener> listeners = new HashSet<>();
    private Sort sort = Sort.CONFIDENCE;
    private Listing listingComments = new Listing();

    private Activity activity;
    private Reddit reddit;
    private boolean isRefreshing;
    private boolean isCommentThread;
    private int contextNumber;

    public ControllerComments(Activity activity) {
        setActivity(activity);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
    }

    public void addListener(Listener listener) {
        if (listeners.add(listener)) {
            setTitle();
            listener.getAdapter().notifyDataSetChanged();
            listener.setSort(sort);
            listener.setRefreshing(isRefreshing());
            listener.setIsCommentThread(isCommentThread);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void setTitle() {
        for (Listener listener : listeners) {
            listener.setToolbarTitle(link.getTitle());
        }
    }

    public void setLink(Link link) {
        this.listingComments = new Listing();
        this.link = link;
        setSort(link.getSuggestedSort());
        reloadAllComments();
    }

    public void setLinkId(String subreddit, String linkId) {
        setLinkIdValues(subreddit, linkId);
        reloadAllComments();
    }

    public void setLinkId(String subreddit, String linkId, String commentId, int contextNumber) {
        this.contextNumber = contextNumber;
        setLinkIdValues(subreddit, linkId);
        loadCommentThread(commentId);
    }

    private void setLinkIdValues(String subreddit, String linkId) {
        link = new Link();
        link.setSubreddit(subreddit);
        link.setId(linkId);
        setSort(link.getSuggestedSort());
    }

    public void reloadAllComments() {
        if (TextUtils.isEmpty(link.getId())) {
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

    public void setLinkWithComments(Link link) {

        this.link = link;
        Listing listing = new Listing();

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

    public void loadLinkComments() {
        setRefreshing(true);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/r/" + link.getSubreddit() + "/comments/" + link
                        .getId() + "?depth=10&showmore=true&showedits=true&limit=100&sort=" + sort
                        .toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            setLinkWithComments(Link.fromJsonWithComments(
                                    Reddit.getObjectMapper().readValue(response,
                                            JsonNode.class)));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        setIsCommentThread(false);
                        setRefreshing(false);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "reloadAllComments onErrorResponse: " + error);
                        setRefreshing(false);
                    }
                }, 0);
    }

    public void loadCommentThread(String commentId) {

        Log.d(TAG, "loadCommentThread: " + commentId);

        setRefreshing(true);

        reddit.loadGet(
                Reddit.OAUTH_URL + "/r/" + link.getSubreddit() + "/comments/" + link
                        .getId() + "?depth=10&showmore=true&showedits=true&limit=100&context=" + contextNumber + "&comment=" + commentId + "&sort=" + sort
                        .toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            setLinkWithComments(Link.fromJsonWithComments(
                                    Reddit.getObjectMapper().readValue(response,
                                            JsonNode.class)));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        setIsCommentThread(true);
                        setRefreshing(false);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error);
                        setRefreshing(false);
                    }
                }, 0);
    }

    private void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
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

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public String getSubredditName() {
        return link.getSubreddit();
    }

    public boolean showSubreddit() {
        return true;
    }

    public void loadNestedComments(final Comment moreComment) {

        setRefreshing(true);

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

                            JsonNode nodeThings = Reddit.getObjectMapper().readValue(
                                    response, JsonNode.class).get("json").get("data").get("things");

                            Listing listing = new Listing();
                            List<Thing> things = new ArrayList<>();
                            List<Thing> comments = new ArrayList<>();

                            for (JsonNode node : nodeThings) {

                                Comment comment = Comment.fromJson(node, moreComment.getLevel());

                                // For some reason Reddit doesn't report the link author, so we'll do it manually
                                comment.setLinkAuthor(link.getAuthor());

                                if (comment.getParentId().equals(link.getId())) {
                                    comments.add(comment);
                                }
                                else {
                                    // TODO: Find a starting index to insert comments, without iterating the entire data list so many times
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
                        catch (IOException e1) {
                            e1.printStackTrace();
                        }

                        setRefreshing(false);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setRefreshing(false);
                        Log.d(TAG, "error" + error);
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

            insertComments(commentIndex, listComments, link.getComments());
        }

        commentIndex = listingComments.getChildren()
                .indexOf(moreComment);
        if (commentIndex > -1) {
            listingComments.getChildren()
                    .remove(commentIndex);
            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRemoved(commentIndex + 1);
            }

            insertComments(commentIndex, listComments, listingComments);

            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemRangeInserted(commentIndex + 1, listComments.size());
            }
        }

        // TODO: This is an expensive operation and should be tested for possible removal
        link.getComments().checkChildren();
        listingComments.checkChildren();

    }

    private void insertComments(int positionStart, List<Thing> comments, Listing listing) {

        for (int index = comments.size() - 1; index >= 0; index--) {
            Comment comment = (Comment) comments.get(index);
            listing.getChildren().add(positionStart, comment);
        }
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
                parentComment = (Comment) link.getComments().getChildren().get(commentIndex);
                comment.setLevel(parentComment.getLevel() + 1);
            }
            link.getComments().getChildren().add(commentIndex + 1, comment);
        }

        if (listingComments != null) {
            commentIndex = listingComments.getChildren()
                    .indexOf(parentComment);

            if (commentIndex > -1) {
                parentComment = (Comment) listingComments.getChildren().get(commentIndex);
                comment.setLevel(parentComment.getLevel() + 1);
            }
            listingComments.getChildren().add(commentIndex + 1, comment);

            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemInserted(commentIndex + 2);
            }
        }
    }

    public void deleteComment(Comment comment) {

        deleteComment(comment, link.getComments());
        int commentIndex = deleteComment(comment, listingComments);

        if (commentIndex > -1) {
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(commentIndex + 1);
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", comment.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/del", null, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, R.string.error_deleting_comment, Toast.LENGTH_LONG).show();
            }
        }, params, 0);
    }


    /**
     * Helper method to prevent code duplication when deleting a comment
     *
     * @param comment
     * @param listing
     * @return index the comment was located at in the listing
     */
    private int deleteComment(Comment comment, Listing listing) {

        int commentIndex = listing.getChildren()
                .indexOf(comment);
        if (commentIndex > -1) {
            comment = (Comment) listing.getChildren()
                    .get(commentIndex);
            comment.setBodyHtml(Comment.DELETED);
            comment.setAuthor(Comment.DELETED);
        }
        return commentIndex;
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

        if (comment.getLevel() >= nextComment.getLevel()) {
            expandComment(position);
            return true;
        }
        else {
            collapseComment(position);
            return false;
        }

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
        while (++index < commentList.size() && ((Comment) commentList.get(index))
                .getLevel() > comment.getLevel()) {
            commentsToInsert.add((Comment) commentList.get(index));
        }

        for (int insertIndex = commentsToInsert.size() - 1; insertIndex >= 0; insertIndex--) {
            listingComments.getChildren()
                    .add(position + 1, commentsToInsert.get(insertIndex));
            numAdded++;
        }
        comment.setCollapsed(0);

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemChanged(position + 1);
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
                position)).getLevel() > comment.getLevel()) {
            commentList.remove(position);
            numRemoved++;
        }
        if (numRemoved > 0) {
            comment.setCollapsed(numRemoved);
        }
        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemChanged(position);
            listener.getAdapter()
                    .notifyItemRangeRemoved(position + 1, numRemoved);
        }
    }

    public void voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment, final int vote) {

        reddit.voteComment(viewHolder, comment, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, activity.getString(R.string.error_voting),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    public Reddit getReddit() {
        return reddit;
    }

    public int getItemCount() {

        if (TextUtils.isEmpty(link.getId())) {
            return 0;
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


    public Link getLink() {
        return link;
    }

    public void loadMoreComments() {
        if (link.getComments().getChildren().isEmpty()) {
            return;
        }

        Comment comment = (Comment) link.getComments().getChildren()
                .get(link.getComments().getChildren().size() - 1);

        if (comment.isMore()) {
            loadNestedComments(comment);
        }

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
                    Log.d(TAG, "response: " + response);
                    Comment newComment = Comment.fromJson(
                            Reddit.getObjectMapper().readValue(response, JsonNode.class).get("json")
                                    .get("data").get("things").get(0), level);

                    int commentIndex = link.getComments().getChildren().indexOf(newComment);

                    if (commentIndex > -1) {
                        Comment comment = (Comment) link.getComments().getChildren().get(commentIndex);
                        comment.setBodyHtml(newComment.getBodyHtml());
                        comment.setEdited(newComment.getEdited());
                    }

                    commentIndex = listingComments.getChildren()
                            .indexOf(newComment);

                    if (commentIndex > -1) {
                        Comment comment = (Comment) listingComments.getChildren().get(commentIndex);
                        comment.setBodyHtml(newComment.getBodyHtml());
                        comment.setEdited(newComment.getEdited());
                        for (Listener listener : listeners) {
                            listener.getAdapter().notifyItemChanged(commentIndex + 1);
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

    public int getPreviousCommentPosition(int commentIndex) {

        List<Thing> things = listingComments.getChildren();

        for (int index = commentIndex - 1; index >= 0; index--) {
            if (((Comment) things.get(index)).getLevel() == 0) {
                return index;
            }
        }

        return commentIndex;
    }

    public int getNextCommentPosition(int commentIndex) {

        List<Thing> things = listingComments.getChildren();

        for (int index = commentIndex + 1; index < listingComments.getChildren()
                .size(); index++) {
            if (((Comment) things.get(index)).getLevel() == 0) {
                return index;
            }
        }

        return commentIndex;
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            for (Listener listener : listeners) {
                listener.setSort(sort);
            }
            reloadAllComments();
        }
    }

    public Sort getSort() {
        return sort;
    }

    public void jumpToParent(Comment child) {

        int commentIndex = listingComments.getChildren()
                .indexOf(child);

        if (commentIndex > -1) {
            for (int index = commentIndex - 1; index >= 0; index--) {
                Comment comment = (Comment) listingComments.getChildren().get(index);
                if (comment.getLevel() == child.getLevel() - 1) {
                    for (Listener listener : listeners) {
                        listener.scrollTo(index + 1);
                    }
                    break;
                }
            }
        }

    }

    public boolean setReplyText(String name, String text, boolean collapsed) {

        if (name.equals(link.getName())) {
            link.setReplyText(text);
            link.setReplyExpanded(!collapsed);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(0);
            }
            return true;
        }

        for (int index = 0; index < listingComments.getChildren().size(); index++) {
            Thing thing = listingComments.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 1);
                }
                return true;
            }
        }

        return false;
    }

    public void setNsfw(String name, boolean over18) {
        if (name.equals(link.getName())) {
            link.setOver18(over18);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(0);
            }
        }
    }

    public interface Listener extends ControllerListener {
        void setSort(Sort sort);
        void setIsCommentThread(boolean isCommentThread);
        void scrollTo(int position);
    }

}