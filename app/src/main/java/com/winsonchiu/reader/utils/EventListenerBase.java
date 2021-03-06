/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.ControllerCommentsTop;
import com.winsonchiu.reader.dagger.components.ComponentActivity;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Saveable;
import com.winsonchiu.reader.data.reddit.Submission;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.data.reddit.Votable;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.search.ControllerSearch;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;

/**
 * Created by TheKeeperOfPie on 3/27/2016.
 */
public abstract class EventListenerBase implements AdapterLink.ViewHolderLink.EventListener {

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
                .flatMap(UtilsRx.flatMapWrapError(response ->
                        Comment.fromJson(ComponentStatic.getObjectMapper()
                                .readValue(response, JsonNode.class).get("json")
                                .get("data")
                                .get("things")
                                .get(0), 0)))
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
                .flatMap(UtilsRx.flatMapWrapError(response ->
                        Message.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class).get("json")
                                .get("data")
                                .get("things")
                                .get(0))))
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

    @Override
    public void copyText(CharSequence text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(context.getString(R.string.comment), text);
        clipboard.setPrimaryClip(clip);
        toast(context.getString(R.string.copied));
    }

    public <T extends Votable> Observable<T> onVote(T votable, Likes vote) {
        Likes voteOld = votable.getLikes();

        if (vote == voteOld) {
            vote = Likes.NONE;
        }

        votable.setLikes(vote);

        return reddit.voteLink(votable, vote)
                .flatMap(s -> Observable.<T>empty())
                .onErrorResumeNext(throwable -> {
                    votable.setLikes(voteOld);
                    return Observable.just(votable)
                            .concatWith(Observable.error(throwable));
                })
                .startWith(votable);
    }

    public <T extends Submission> Observable<T> onDelete(T submission) {
        return reddit.delete(submission)
                .map(s -> submission);
    }

    public Observable<String> onReport(Thing thing, String reason) {
        return reddit.report(thing.getName(), reason);
    }

    public <T extends Saveable> Observable<T> onSave(T saveable) {
        boolean savedOld = saveable.isSaved();
        saveable.setSaved(true);

        return reddit.save(saveable, null)
                .flatMap(s -> Observable.<T>empty())
                .onErrorResumeNext(throwable -> {
                    saveable.setSaved(savedOld);
                    return Observable.just(saveable)
                            .concatWith(Observable.error(throwable));
                })
                .startWith(saveable);
    }

    public <T extends Saveable> Observable<T> onUnsave(T saveable) {
        boolean savedOld = saveable.isSaved();
        saveable.setSaved(false);

        return reddit.unsave(saveable)
                .flatMap(s -> Observable.<T>empty())
                .onErrorResumeNext(throwable -> {
                    saveable.setSaved(savedOld);
                    return Observable.just(saveable)
                            .concatWith(Observable.error(throwable));
                })
                .startWith(saveable);
    }

    public Observable<Link> onMarkNsfw(Link link) {
        boolean over18 = link.isOver18();
        link.setOver18(true);

        return reddit.markNsfw(link)
                .flatMap(s -> Observable.<Link>empty())
                .onErrorResumeNext(throwable -> {
                    link.setSaved(over18);
                    return Observable.just(link)
                            .concatWith(Observable.error(throwable));
                })
                .startWith(link);
    }

    public Observable<Link> onUnmarkNsfw(Link link) {
        boolean over18 = link.isOver18();
        link.setOver18(false);

        return reddit.unmarkNsfw(link)
                .flatMap(s -> Observable.<Link>empty())
                .onErrorResumeNext(throwable -> {
                    link.setSaved(over18);
                    return Observable.just(link)
                            .concatWith(Observable.error(throwable));
                })
                .startWith(link);
    }
}
