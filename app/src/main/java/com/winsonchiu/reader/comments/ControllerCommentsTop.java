/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.support.v4.util.Pair;

import com.jakewharton.rxrelay.BehaviorRelay;
import com.jakewharton.rxrelay.PublishRelay;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.ReplyModel;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.JsonPayload;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.links.LinkModel;
import com.winsonchiu.reader.rx.ObserverNext;
import com.winsonchiu.reader.utils.UtilsRx;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerCommentsTop {

    private static final String TAG = ControllerCommentsTop.class.getCanonicalName();

    private CommentsTopModel commentsTopModel = new CommentsTopModel();

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

    private void publishUpdate() {
        eventHolder.call(new CommentsTopModel(commentsTopModel));
    }

    public void setLink(Link link, Source source) {
        commentsTopModel.setLinkModel(new LinkModel(link));
        commentsTopModel.setSource(source);
        publishUpdate();
    }

    public void setLinkId(String linkId, Source source) {
        setLinkIdValues(linkId, source);
    }

    public void setLinkId(String linkId, String commentId, int contextLevel, Source source) {
        setLinkIdValues(linkId, source);
        commentsTopModel.getLinkModel().setContextLevel(contextLevel);
        commentsTopModel.getLinkModel().setCommentId(commentId);
    }

    private void setLinkIdValues(String linkId, Source source) {
        Link link = new Link();
        link.setId(linkId);
        commentsTopModel.setLinkModel(new LinkModel(link));
        commentsTopModel.setSource(source);
        publishUpdate();
    }

    public void editComment(String name, final int level, String text) {
        reddit.editUserText(name, text)
                .observeOn(Schedulers.computation())
                .flatMap(UtilsRx.flatMapWrapError(JsonPayload::fromJson))
                .flatMap(jsonPayload -> Observable.from(jsonPayload.getData().getThings()))
                .ofType(Comment.class)
                .subscribe(new ObserverNext<Comment>() {
                    @Override
                    public void onNext(Comment newComment) {
                        newComment.setLevel(level);
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