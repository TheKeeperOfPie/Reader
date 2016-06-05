/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.support.v4.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.jakewharton.rxrelay.PublishRelay;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.ReplyModel;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.UtilsRx;

import javax.inject.Inject;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerCommentsTop {

    private static final String TAG = ControllerCommentsTop.class.getCanonicalName();

    private Link link = new Link();
    private Source source = Source.NONE;
    private EventHolder eventHolder = new EventHolder();

    @Inject Reddit reddit;

    public ControllerCommentsTop() {
        CustomApplication.getComponentMain().inject(this);
    }

    public EventHolder getEventHolder() {
        return eventHolder;
    }

    public void insertComment(Comment comment) {
        eventHolder.getInsertions().call(comment);
    }

    public void setNsfw(String name, boolean over18) {
        eventHolder.getUpdatesNsfw().call(Pair.create(name, over18));
    }

    public void setLink(Link link, Source source) {
        this.link = link;
        this.source = source;
        eventHolder.call(getData());
    }

    private CommentsTopModel getData() {
        return new CommentsTopModel(link, source);
    }

    public void setLinkId(String linkId, Source source) {
        setLinkIdValues(linkId, source);
    }

    public void setLinkId(String linkId, String commentId, int contextLevel, Source source) {
        setLinkIdValues(linkId, source);
        link.setContextLevel(contextLevel);
        link.setCommentId(commentId);
    }

    private void setLinkIdValues(String linkId, Source source) {
        link = new Link();
        link.setId(linkId);
        this.source = source;
        eventHolder.call(getData());
    }

    public void editComment(String name, final int level, String text) {
        reddit.editUserText(name, text)
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(response -> Comment.fromJson(ComponentStatic.getObjectMapper()
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
                        eventHolder.getUpdatesComment().call(newComment);
                    }
                });
    }

    public void setReplyText(String nameParent, String text, boolean collapsed) {
        eventHolder.getUpdatesReplyText().call(new ReplyModel(nameParent, text, collapsed));
    }

    public static class EventHolder implements Action1<CommentsTopModel> {

        private BehaviorRelay<CommentsTopModel> relayData = BehaviorRelay.create(new CommentsTopModel());
        private PublishRelay<Comment> relayInsertions = PublishRelay.create();
        private PublishRelay<Comment> relayUpdatesComment = PublishRelay.create();
        private PublishRelay<Pair<String, Boolean>> relayUpdatesNsfw = PublishRelay.create();
        private PublishRelay<ReplyModel> relayUpdatesReplyText = PublishRelay.create();

        @Override
        public void call(CommentsTopModel commentsTopModel) {
            relayData.call(commentsTopModel);
        }

        public BehaviorRelay<CommentsTopModel> getData() {
            return relayData;
        }

        public PublishRelay<Comment> getInsertions() {
            return relayInsertions;
        }

        public PublishRelay<Comment> getUpdatesComment() {
            return relayUpdatesComment;
        }

        public PublishRelay<Pair<String, Boolean>> getUpdatesNsfw() {
            return relayUpdatesNsfw;
        }

        public PublishRelay<ReplyModel> getUpdatesReplyText() {
            return relayUpdatesReplyText;
        }
    }

}