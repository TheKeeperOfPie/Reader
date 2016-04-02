/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.UtilsRx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import rx.Observer;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerCommentsTop {

    private static final String TAG = ControllerCommentsTop.class.getCanonicalName();

    private Set<Listener> listeners = new HashSet<>();
    private Link link;
    private Source source = Source.NONE;

    @Inject Reddit reddit;

    public ControllerCommentsTop() {
        CustomApplication.getComponentMain().inject(this);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void insertComment(Comment comment) {
        for (Listener listener : listeners) {
            listener.insertComment(comment);
        }
    }

    public void setNsfw(String name, boolean over18) {
        for (Listener listener : listeners) {
            listener.setNsfw(name, over18);
        }
    }

    public void setLink(Link link, Source source) {
        this.link = link;
        this.source = source;
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
    }

    public Link getLink() {
        return link;
    }

    public void editComment(String name, final int level, String text) {
        reddit.editUserText(name, text)
                .flatMap(UtilsRx.flatMapWrapError(response -> Comment.fromJson(ComponentStatic.getObjectMapper()
                        .readValue(response, JsonNode.class)
                        .get("json")
                        .get("data")
                        .get("things")
                        .get(0), level)))
                .subscribe(new Observer<Comment>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Comment newComment) {
                        for (Listener listener : listeners) {
                            listener.updateComment(newComment);
                        }
                    }
                });
    }

    public void setReplyText(String nameParent, String text, boolean collapsed) {
        for (Listener listener : listeners) {
            listener.setReplyText(nameParent, text, collapsed);
        }
    }

    public Source getSource() {
        return source;
    }

    public interface Listener {
        void insertComment(Comment comment);
        void setNsfw(String name, boolean over18);
        void updateComment(Comment newComment);
        void setReplyText(String nameParent, String text, boolean collapsed);
    }

}