/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.ControllerCommentsTop;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.search.ControllerSearch;

import java.io.IOException;
import java.util.HashMap;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 3/27/2016.
 */
public abstract class EventListenerBase implements AdapterLink.ViewHolderBase.EventListener {

    @Inject Context context;
    @Inject ControllerLinks controllerLinks;
    @Inject ControllerUser controllerUser;
    @Inject ControllerCommentsTop controllerCommentsTop;
    @Inject ControllerProfile controllerProfile;
    @Inject ControllerInbox controllerInbox;
    @Inject ControllerSearch controllerSearch;
    @Inject ControllerHistory controllerHistory;
    @Inject Reddit reddit;

    public EventListenerBase(ComponentActivity componentActivity) {
        componentActivity.inject(this);
    }

    @Override
    public void sendComment(String name, String text) {
        reddit.sendComment(name, text)
                .flatMap(new Func1<String, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(String response) {
                        try {
                            Comment comment = Comment.fromJson(ComponentStatic.getObjectMapper()
                                    .readValue(response, JsonNode.class).get("json")
                                    .get("data")
                                    .get("things")
                                    .get(0), 0);

                            return Observable.just(comment);
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribe(new FinalizingSubscriber<Comment>() {
                    @Override
                    public void error(Throwable e) {
                        Toast.makeText(context, R.string.failed_reply, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void next(Comment comment) {
                        controllerCommentsTop.insertComment(comment);
                        controllerProfile.insertComment(comment);
                        controllerInbox.insertComment(comment);
                    }
                });
    }

    @Override
    public void sendMessage(String name, String text) {
        reddit.sendComment(name, text)
                .flatMap(new Func1<String, Observable<Message>>() {
                    @Override
                    public Observable<Message> call(String response) {
                        try {
                            Message message = Message.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class).get("json")
                                    .get("data")
                                    .get("things")
                                    .get(0));

                            return Observable.just(message);
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribe(new FinalizingSubscriber<Message>() {
                    @Override
                    public void error(Throwable e) {
                        Toast.makeText(context, R.string.failed_message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void next(Message next) {
                        controllerInbox.insertMessage(next);
                    }
                });
    }

    @Override
    public void save(Link link) {
        link.setSaved(!link.isSaved());
        if (link.isSaved()) {
            reddit.save(link, null)
                    .subscribe(new FinalizingSubscriber<String>() {
                        @Override
                        public void error(Throwable e) {
                            Toast.makeText(context, R.string.error_saving_post, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            reddit.unsave(link)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    @Override
    public void save(Comment comment) {
        comment.setSaved(!comment.isSaved());
        if (comment.isSaved()) {
            reddit.save(comment, null)
                    .subscribe(new FinalizingSubscriber<String>() {
                        @Override
                        public void error(Throwable e) {
                            Toast.makeText(context, R.string.error_saving_comment, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            reddit.unsave(comment)
                    .subscribe(new ObserverEmpty<>());
        }
    }

    @Override
    public void toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isUserLoggedIn() {
        return controllerUser.hasUser();
    }

    @Override
    public void voteLink(final AdapterLink.ViewHolderBase viewHolderBase, final Link link, int vote) {
        final int position = viewHolderBase.getAdapterPosition();

        final int oldVote = link.getLikes();
        int newVote = 0;

        if (link.getLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, link.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        link.setScore(link.getScore() + newVote - link.getLikes());
        link.setLikes(newVote);
        if (position == viewHolderBase.getAdapterPosition()) {
            viewHolderBase.setVoteColors();
        }
        final int finalNewVote = newVote;

        reddit.voteLink(link, newVote)
                .subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void error(Throwable e) {
                        link.setScore(link.getScore() - finalNewVote);
                        link.setLikes(oldVote);
                        if (position == viewHolderBase.getAdapterPosition()) {
                            viewHolderBase.setVoteColors();
                        }
                        Toast.makeText(context, context.getString(R.string.error_voting), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    @Override
    public void deletePost(Link link) {
        Observable.merge(controllerLinks.deletePost(link), controllerProfile.deletePost(link))
                .subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void error(Throwable e) {
                        Toast.makeText(context, R.string.error_deleting_post, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void report(Thing thing, String reason, String otherReason) {
        reddit.report(thing.getName(),
                reason, otherReason)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {

                    }
                });
    }

    @Override
    public void hide(Link link) {
        link.setHidden(!link.isHidden());
        if (link.isHidden()) {
            reddit.hide(link)
                    .subscribe(new ObserverEmpty<>());
        }
        else {
            reddit.unhide(link)
                    .subscribe(new ObserverEmpty<>());
        }

    }

    @Override
    public void markRead(Thing thing) {
        reddit.markRead(thing.getName())
                .subscribe(new ObserverEmpty<>());
    }

    @Override
    public Observable<String> markNsfw(final Link link) {
        link.setOver18(!link.isOver18());
        syncNsfw(link);

        if (link.isOver18()) {
            return reddit.markNsfw(link)
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            link.setOver18(false);
                            syncNsfw(link);
                            Toast.makeText(context, R.string.error_unmarking_nsfw, Toast.LENGTH_LONG).show();
                        }
                    });
        }
        else {
            return reddit.unmarkNsfw(link)
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            link.setOver18(true);
                            syncNsfw(link);
                            Toast.makeText(context, R.string.error_unmarking_nsfw, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void syncNsfw(Link link) {
        controllerLinks.setNsfw(link.getName(), link.isOver18());
        controllerCommentsTop.setNsfw(link.getName(), link.isOver18());
        controllerProfile.setNsfw(link.getName(), link.isOver18());
        controllerHistory.setNsfw(link.getName(), link.isOver18());
        controllerSearch.setNsfwLinks(link.getName(), link.isOver18());
        controllerSearch.setNsfwLinksSubreddit(link.getName(), link.isOver18());
    }

    @Override
    public User getUser() {
        return controllerUser.getUser();
    }
}
