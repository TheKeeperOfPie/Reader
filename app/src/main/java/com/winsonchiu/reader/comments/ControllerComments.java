/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.jakewharton.rxrelay.PublishRelay;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.ReplyModel;
import com.winsonchiu.reader.adapter.RxAdapterEvent;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.database.reddit.RedditDatabase;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.utils.UtilsRx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerComments implements AdapterCommentList.ViewHolderComment.EventListenerComment {

    private static final String TAG = ControllerComments.class.getCanonicalName();

    private Link link = new Link();
    private Listing listingComments = new Listing();

    private EventHolder eventHolder = new EventHolder();

    @Inject Reddit reddit;
    @Inject RedditDatabase redditDatabase;
    @Inject SharedPreferences sharedPreferences;
    @Inject ControllerUser controllerUser;

    private Subscription subscriptionComments;

    public ControllerComments(Reddit reddit,
            RedditDatabase redditDatabase,
            SharedPreferences sharedPreferences,
            ControllerUser controllerUser) {
        this.reddit = reddit;
        this.redditDatabase = redditDatabase;
        this.sharedPreferences = sharedPreferences;
        this.controllerUser = controllerUser;
    }

    public ControllerComments(ComponentActivity componentActivity) {
        componentActivity.inject(this);
    }

    public EventHolder getEventHolder() {
        return eventHolder;
    }

    public void setLink(Link link) {
        this.listingComments = new Listing();
        this.link = link;
        eventHolder.call(new RxAdapterEvent<>(getData()));
        setSort(link.getSuggestedSort());
        reloadAllComments();
    }

    public void setLinkFromCache(Link link) {
        this.link = link;
        eventHolder.call(new RxAdapterEvent<>(getData()));
        this.listingComments = link.getComments();
        eventHolder.getSort().call(link.getSuggestedSort());
    }

    public void reloadAllComments() {
        if (TextUtils.isEmpty(link.getId())) {
            setLoading(false);
            return;
        }

        if (link.getContextLevel() > 0 && !TextUtils.isEmpty(link.getCommentId())) {
            loadCommentThread();
            return;
        }

        if (!link.getComments().getChildren().isEmpty()) {
            Comment commentFirst = ((Comment) link.getComments().getChildren().get(0));
            if (!commentFirst.getParentId().equals(link.getId())) {
                link.setCommentId(commentFirst.getId());
                loadCommentThread();
                return;
            }
        }

        loadLinkComments();

    }

    public void setLinkWithComments(Link link) {
        link.setBackgroundColor(this.link.getBackgroundColor());
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
            listing.setChildren(new ArrayList<>());
        }
        listingComments = listing;

        if (sharedPreferences.getBoolean(AppSettings.PREF_COLLAPSE_COMMENT_THREADS, false)) {
            for (int index = listingComments.getChildren().size() - 1; index >= 0; index--) {
                if (((Comment) listingComments.getChildren().get(index)).getLevel() == 0) {
                    collapseComment(index, false);
                }
            }
        }

        eventHolder.call(new RxAdapterEvent<>(getData()));
    }

    public void loadLinkComments() {
        link.setCommentId(null);
        link.setContextLevel(0);

        UtilsRx.unsubscribe(subscriptionComments);
        subscriptionComments = reddit.comments(link.getSubreddit(), link.getId(), link.getCommentId(), eventHolder.getSort().getValue().toString(), true, true, link.getContextLevel(), 10, 100)
                .observeOn(Schedulers.computation())
                .doOnNext(redditDatabase.storeLink())
                .onErrorResumeNext(Observable.<Link>empty())
                .switchIfEmpty(redditDatabase.getLink(link.getId()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getSubscriberLink());
    }

    public void loadCommentThread() {
        UtilsRx.unsubscribe(subscriptionComments);
        subscriptionComments = reddit.comments(link.getSubreddit(), link.getId(), link.getCommentId(), eventHolder.getSort().getValue().toString(), true, true, link.getContextLevel(), 10, 100)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getSubscriberLink());
    }

    private Observer<Link> getSubscriberLink() {
        return new FinalizingSubscriber<Link>() {
            @Override
            public void start() {
                setLoading(true);
            }

            @Override
            public void next(com.winsonchiu.reader.data.reddit.Link link) {
                setLinkWithComments(link);
            }

            @Override
            public void finish() {
                setLoading(false);
            }
        };
    }

    private void setLoading(boolean loading) {
        if (loading) {
            Log.d(TAG, "setLoading() called with: loading = [" + loading + "]", new Exception());
        }
        eventHolder.getLoading().call(loading);
    }

    public String getSubredditName() {
        return link.getSubreddit();
    }

    public boolean showSubreddit() {
        return true;
    }

    public CommentsModel getData() {
        List<Comment> comments = new ArrayList<>(listingComments.getChildren().size());

        for (Thing thing : listingComments.getChildren()) {
            if (thing instanceof Comment) {
                comments.add((Comment) thing);
            }
        }
        return new CommentsModel(link, comments, true, controllerUser.getUser());
    }

    public void loadNestedComments(final Comment moreComment) {

        setLoading(true);

        String children = "";
        List<String> childrenList = moreComment.getChildren();
        if (childrenList.isEmpty()) {
            int commentIndex = listingComments.getChildren()
                    .indexOf(moreComment);
            if (commentIndex >= 0) {
                listingComments.getChildren().remove(commentIndex);
                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, commentIndex + 1));
            }
            return;
        }
        for (String id : childrenList) {
            children += id + ",";
        }

        reddit.moreChildren(link.getName(), children.substring(0, children.length() - 1))
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        setLoading(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String response) {
                        Log.d(TAG, "onNext() called with: " + "response = [" + response + "]");
                        try {
                            JsonNode nodeThings = ComponentStatic.getObjectMapper().readValue(
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
                                    listingComments.getChildren().remove(commentIndex);
                                    eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, commentIndex + 1));
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
                    }
                });
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
            listingComments.getChildren().remove(commentIndex);
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.REMOVE, commentIndex + 1));

            insertComments(commentIndex, listComments, listingComments);

            if (sharedPreferences.getBoolean(AppSettings.PREF_COLLAPSE_COMMENT_THREADS, false)) {
                for (int index = listingComments.getChildren().size() - 1; index >= commentIndex; index--) {
                    if (((Comment) listingComments.getChildren().get(index)).getLevel() == 0) {
                        collapseComment(index, false);
                    }
                }
            }

            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, commentIndex + 1, listComments.size()));
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

            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, commentIndex + 2));
        }
    }

    public Observable<String> deleteComment(Comment comment) {

        deleteComment(comment, link.getComments());
        int commentIndex = deleteComment(comment, listingComments);

        if (commentIndex > -1) {
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, commentIndex + 1));
        }

        Observable<String> observable = reddit.delete(comment);
        observable.subscribe(new ObserverEmpty<String>());
        return observable;
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

    private int indexOf(Comment comment) {
        return listingComments.getChildren().indexOf(comment);
    }

    /**
     * Toggles children of comment
     *
     * @param comment
     * @return true if comment is now expanded, false if collapsed
     */
    @Override
    public boolean toggleComment(Comment comment) {

        int position = indexOf(comment);

        if (position == listingComments.getChildren()
                .size() - 1) {
            expandComment(position);
            return true;
        }

        List<Thing> commentList = listingComments.getChildren();
        comment = (Comment) commentList.get(position);
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

    private void expandComment(int position) {
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

        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, position + 1));
        eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, position + 2, numAdded));
    }

    private void collapseComment(int position) {
        collapseComment(position, true);
    }

    private void collapseComment(int position, boolean notify) {
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

        if (notify) {
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, position));
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.INSERT, position + 1, numRemoved));
        }
    }

    public Observable<String> voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment, Likes vote) {
        // TODO: Combine these instances into utility
        final int position = viewHolder.getAdapterPosition();

        final Likes oldVote = comment.getLikes();
        Likes newVote = Likes.NONE;

        if (comment.getLikes() != vote) {
            newVote = vote;
        }

        comment.setScore(comment.getScore() + newVote.getScoreValue() - comment.getLikes().getScoreValue());
        comment.setLikes(newVote);

        if (position == viewHolder.getAdapterPosition()) {
            viewHolder.setVoteColors();
        }

        final Likes finalNewVote = newVote;

        Observable<String> observable = reddit.voteComment(comment, newVote);
        observable.subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void error(Throwable e) {
                        comment.setScore(comment.getScore() - finalNewVote.getScoreValue());
                        comment.setLikes(oldVote);
                        if (position == viewHolder.getAdapterPosition()) {
                            viewHolder.setVoteColors();
                        }
                    }
                });

        return observable;
    }

    public Reddit getReddit() {
        return reddit;
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
        reddit.editUserText(name, text)
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response ->
                        Comment.fromJson(ComponentStatic.getObjectMapper()
                                .readValue(response, JsonNode.class)
                                .get("json")
                                .get("data")
                                .get("things")
                                .get(0), level)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Comment>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Comment newComment) {
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
                            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, commentIndex + 1));
                        }
                    }
                });
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
        if (eventHolder.getSort().getValue() != sort) {
            eventHolder.getSort().call(sort);
            reloadAllComments();
        }
    }

    public void jumpToParent(Comment child) {
        int commentIndex = listingComments.getChildren()
                .indexOf(child);

        if (commentIndex > -1) {
            for (int index = commentIndex - 1; index >= 0; index--) {
                Comment comment = (Comment) listingComments.getChildren().get(index);
                if (comment.getLevel() == child.getLevel() - 1) {
                    eventHolder.getScrollEvents().call(index + 1);
                    break;
                }
            }
        }

    }

    @Override
    public String getLinkId() {
        return link.getId();
    }

    public boolean setReplyText(ReplyModel replyModel) {
        String name = replyModel.getNameParent();
        String text = replyModel.getText();
        boolean collapsed = replyModel.isCollapsed();

        if (name.equals(link.getName())) {
            link.setReplyText(text);
            link.setReplyExpanded(!collapsed);
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0));
            return true;
        }

        for (int index = 0; index < listingComments.getChildren().size(); index++) {
            Thing thing = listingComments.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, index + 1));
                return true;
            }
        }

        return false;
    }

    public void setNsfw(String name, boolean over18) {
        if (name.equals(link.getName())) {
            link.setOver18(over18);
            eventHolder.call(new RxAdapterEvent<>(getData(), RxAdapterEvent.Type.CHANGE, 0));
        }
    }

    public static class EventHolder implements Action1<RxAdapterEvent<CommentsModel>> {

        private BehaviorRelay<RxAdapterEvent<CommentsModel>> relayData = BehaviorRelay.create(new RxAdapterEvent<>(new CommentsModel()));
        private BehaviorRelay<Boolean> relayLoading = BehaviorRelay.create(false);
        private BehaviorRelay<Sort> relaySort = BehaviorRelay.create(Sort.CONFIDENCE);
        private BehaviorRelay<CommentsError> relayErrors = BehaviorRelay.create();
        private PublishRelay<Integer> relayScrollEvents = PublishRelay.create();

        @Override
        public void call(RxAdapterEvent<CommentsModel> event) {
            relayData.call(event);
        }

        public Observable<RxAdapterEvent<CommentsModel>> getData() {
            return relayData.skip(1)
                    .startWith(new RxAdapterEvent<>(relayData.getValue().getData()));
        }

        public BehaviorRelay<Boolean> getLoading() {
            return relayLoading;
        }

        public BehaviorRelay<Sort> getSort() {
            return relaySort;
        }

        public BehaviorRelay<CommentsError> getErrors() {
            return relayErrors;
        }

        public PublishRelay<Integer> getScrollEvents() {
            return relayScrollEvents;
        }
    }
}