/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.content.Context;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Sort;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.Time;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.ControllerLinksBase;
import com.winsonchiu.reader.utils.ControllerListener;
import com.winsonchiu.reader.utils.FinalizingSubscriber;
import com.winsonchiu.reader.utils.ObserverEmpty;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 5/16/2015.
 */
public class ControllerProfile implements ControllerLinksBase {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_HEADER_TEXT = 1;
    public static final int VIEW_TYPE_LINK = 2;
    public static final int VIEW_TYPE_COMMENT = 3;

    public static final String PAGE_OVERVIEW = "Overview";
    public static final String PAGE_SUBMITTED = "Submitted";
    public static final String PAGE_COMMENTS = "Comments";
    public static final String PAGE_GILDED = "Gilded";
    public static final String PAGE_UPVOTED = "Upvoted";
    public static final String PAGE_DOWNVOTED = "Downvoted";
    public static final String PAGE_HIDDEN = "Hidden";
    public static final String PAGE_SAVED = "Saved";

    private static final String TAG = ControllerProfile.class.getCanonicalName();
    public static final int LIMIT = 15;

    private ControllerUser controllerUser;
    private Set<Listener> listeners;
    private Listing data;
    private Link topLink;
    private Comment topComment;
    private User user;
    private Page page;
    private Sort sort;
    private Time time;
    private boolean isLoading;

    private Page pageDefault;

    @Inject Reddit reddit;

    public ControllerProfile(Context context, ControllerUser controllerUser) {
        this.controllerUser = controllerUser;
        CustomApplication.getComponentMain().inject(this);
        data = new Listing();
        listeners = new HashSet<>();
        topLink = new Link();
        topComment = new Comment();
        user = new User();
        pageDefault = new Page(PAGE_OVERVIEW, context.getString(R.string.profile_page_overview));
        page = pageDefault;
        sort = Sort.NEW;
        time = Time.ALL;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.setSortAndTime(sort, time);
        listener.setRefreshing(isLoading());
        listener.getAdapter().notifyDataSetChanged();
        listener.setIsUser(user.getName().equals(controllerUser.getUser().getName()));
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public int getViewType(int position) {

        List<Thing> children = data.getChildren();

        if (position < 0 || position >= children.size()) {
            throw new IndexOutOfBoundsException("ControllerProfile position invalid");
        }

        Thing thing = children.get(position);

        if (thing instanceof Link) {
            return VIEW_TYPE_LINK;
        } else if (thing instanceof Comment) {
            return VIEW_TYPE_COMMENT;
        }

        throw new IllegalStateException(thing + " is not a valid view type");

    }

    @Override
    public Link getLink(int position) {
        if (position == 2) {
            return getTopLink();
        }

        return (Link) data.getChildren().get(position - 6);
    }

    public Link getTopLink() {
        return page.getPage().equalsIgnoreCase(PAGE_OVERVIEW) ? topLink : null;
    }

    public Comment getComment(int position) {
        if (position == 4) {
            return getTopComment();
        }

        return (Comment) data.getChildren().get(position - 6);
    }

    public Observable<Listing> setPage(Page page) {
        if (!this.page.equals(page)) {
            this.page = page;
            return reload();
        }
        return Observable.empty();
    }

    public Page getPage() {
        return page;
    }

    public Observable<Listing> setUser(User user) {
        this.user = user;
        sort = Sort.NEW;
        page = pageDefault;
        for (Listener listener : listeners) {
            listener.setSortAndTime(sort, time);
            listener.setIsUser(user.getName()
                    .equals(controllerUser.getUser().getName()));
        }
        return reload();
    }

    public Observable<Listing> reload() {
        Observable<Listing> observable = reddit.user(user.getName(), page.getPage().toLowerCase(), sort.toString(), time.toString(), null, LIMIT)
                .flatMap(Listing.FLAT_MAP);
        observable.subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        setData(listing);
                        for (Listener listener : listeners) {
                            listener.setPage(page);
                            listener.getAdapter().notifyDataSetChanged();
                        }
                        if (!TextUtils.isEmpty(user.getName()) && page.getPage().equalsIgnoreCase(
                                PAGE_OVERVIEW)) {
                            loadTopEntries();
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });
        return observable;
    }

    public void loadMore() {
        if (isLoading()) {
            return;
        }

        reddit.user(user.getName(), page.getPage().toLowerCase(), sort.toString(), time.toString(), data.getAfter(), 15)
                .flatMap(Listing.FLAT_MAP)
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        int startSize = data.getChildren().size();
                        int positionStart = startSize + 5;

                        data.addChildren(listing.getChildren());
                        data.setAfter(listing.getAfter());

                        for (Listener listener : listeners) {

                            listener.getAdapter()
                                    .notifyItemRangeInserted(positionStart,
                                            data.getChildren().size() - positionStart);
                            listener.setPage(page);
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });
    }

    private void loadTopEntries() {

        // TODO: Support loading trophies
//        reddit.loadGet(Reddit.OAUTH_URL + "/api/v1/user/" + user.getName() + "/trophies",
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        Log.d(TAG, "Trophies response: " + response);
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//
//                    }
//                }, 0);

        reddit.user(user.getName(), PAGE_SUBMITTED.toLowerCase(), Sort.TOP.toString(), Time.ALL.toString(), null, 10)
                .flatMap(Listing.FLAT_MAP)
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        if (!listing.getChildren().isEmpty()) {
                            topLink = null;
                            for (Thing thing : listing.getChildren()) {
                                Link link = (Link) thing;
                                if (!link.isHidden()) {
                                    topLink = link;
                                    break;
                                }
                            }

                            for (Listener listener : listeners) {
                                listener.setRefreshing(false);
                                listener.getAdapter().notifyItemRangeChanged(1, 2);
                            }
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });

        reddit.user(user.getName(), PAGE_COMMENTS.toLowerCase(), Sort.TOP.toString(), Time.ALL.toString(), null, 10)
                .flatMap(Listing.FLAT_MAP)
                .subscribe(new FinalizingSubscriber<Listing>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(Listing listing) {
                        if (!listing.getChildren().isEmpty()) {
                            topComment = (Comment) listing.getChildren()
                                    .get(0);
                            topComment.setLevel(0);
                            for (Listener listener : listeners) {
                                listener.setRefreshing(
                                        false);
                                listener.getAdapter().notifyItemRangeChanged(
                                        3, 2);
                            }
                        }
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });
    }

    @Override
    public int sizeLinks() {
        return data.getChildren().size();
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        for (Listener listener : listeners) {
            listener.setRefreshing(loading);
        }
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public Observable<Listing> loadMoreLinks() {
        // Not implemented
        return Observable.empty();
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
    public boolean setReplyText(String name, String text, boolean collapsed) {

        if (topLink != null && name.equals(topLink.getName())) {
            topLink.setReplyText(text);
            topLink.setReplyExpanded(!collapsed);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(2);
            }
            return true;
        }

        if (topComment != null && name.equals(topComment.getName())) {
            topComment.setReplyText(text);
            topComment.setReplyExpanded(!collapsed);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(4);
            }
            return true;
        }

        for (int index = 0; index < data.getChildren().size(); index++) {
            Thing thing = data.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Replyable) thing).setReplyText(text);
                ((Replyable) thing).setReplyExpanded(!collapsed);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 6);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void setNsfw(String name, boolean over18) {

        if (topLink != null && name.equals(topLink.getName())) {
            topLink.setOver18(over18);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(2);
            }
            return;
        }

        for (int index = 0; index < data.getChildren().size(); index++) {
            Thing thing = data.getChildren().get(index);
            if (thing.getName().equals(name)) {
                ((Link) thing).setOver18(over18);
                for (Listener listener : listeners) {
                    listener.getAdapter().notifyItemChanged(index + 6);
                }
                return;
            }
        }
    }

    public Link remove(int position) {
        Link link;
        if (position == -1) {
            link = topLink;
            topLink = null;
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemChanged(2);
            }
        } else {
            link = (Link) data.getChildren().remove(position);
            for (Listener listener : listeners) {
                listener.getAdapter().notifyItemRemoved(position + 6);
            }
        }
        return link;
    }

    public void insertComment(Comment comment) {

        // Placeholder to use ArrayList.indexOf() properly
        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());

        int commentIndex = data.getChildren()
                .indexOf(parentComment);
        if (commentIndex > -1) {
            // Level and context are set as they are not provided by the send API
            parentComment = (Comment) data.getChildren().get(commentIndex);
            comment.setLevel(parentComment.getLevel() + 1);
            comment.setContext(parentComment.getContext());
            data.getChildren()
                    .add(commentIndex + 1, comment);

            for (Listener listener : listeners) {
                listener.getAdapter()
                        .notifyItemInserted(commentIndex + 7);
            }
        }

    }


    public Observable<String> deletePost(Link link) {
        int index = data.getChildren()
                .indexOf(link);

        if (index < 0) {
            return Observable.empty();
        }

        data.getChildren()
                .remove(index);
        for (Listener listener : listeners) {
            listener.getAdapter()
                    .notifyItemRemoved(index + 6);
        }

        return reddit.delete(link);
    }

    public Observable<String> deleteComment(Comment comment) {
        int commentIndex = data.getChildren().indexOf(comment);
        data.getChildren().remove(commentIndex);

        for (Listener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(commentIndex + 6);

        }

        Observable<String> observable = reddit.delete(comment);
        observable.subscribe(new ObserverEmpty<String>());
        return observable;
    }

    public Observable<String> voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
                                          final Comment comment,
                                          int vote) {
        final int position = viewHolder.getAdapterPosition();

        final int oldVote = comment.getLikes();
        int newVote = 0;

        if (comment.getLikes() != vote) {
            newVote = vote;
        }

        comment.setScore(comment.getScore() + newVote - comment.getLikes());
        comment.setLikes(newVote);

        if (position == viewHolder.getAdapterPosition()) {
            viewHolder.setVoteColors();
        }

        final int finalNewVote = newVote;

        Observable<String> observable = reddit.voteComment(comment, newVote);
        observable.subscribe(new FinalizingSubscriber<String>() {
                    @Override
                    public void error(Throwable e) {
                        comment.setScore(comment.getScore() - finalNewVote);
                        comment.setLikes(oldVote);
                        if (position == viewHolder.getAdapterPosition()) {
                            viewHolder.setVoteColors();
                        }
                    }
                });
        return observable;
    }

    public void loadNestedComments(Comment moreComment) {
        // Not implemented
    }

    public boolean isCommentExpanded(int position) {
        // Not implemented
        return true;
    }

    public boolean hasChildren(Comment comment) {
        return false;
    }

    public void editComment(String name, final int level, String text) {
        reddit.editUserText(name, text)
                .flatMap(new Func1<String, Observable<Comment>>() {
                    @Override
                    public Observable<Comment> call(String response) {
                        try {
                            Comment comment = Comment.fromJson(ComponentStatic.getObjectMapper()
                                    .readValue(response, JsonNode.class)
                                    .get("json")
                                    .get("data")
                                    .get("things")
                                    .get(0), level);

                            return Observable.just(comment);
                        }
                        catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribe(new Observer<Comment>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Comment newComment) {
                        if (newComment.getName().equals(topComment.getName())) {
                            topComment.setBodyHtml(newComment.getBodyHtml());
                            topComment.setEdited(newComment.getEdited());
                            for (Listener listener : listeners) {
                                listener.getAdapter().notifyItemChanged(2);
                            }
                        } else {
                            int commentIndex = data.getChildren()
                                    .indexOf(newComment);

                            if (commentIndex > -1) {
                                Comment comment = (Comment) data.getChildren().get(commentIndex);
                                comment.setBodyHtml(newComment.getBodyHtml());
                                comment.setEdited(newComment.getEdited());
                                for (Listener listener : listeners) {
                                    listener.getAdapter().notifyItemChanged(commentIndex + 6);
                                }
                            }
                        }
                    }
                });
    }

    public void setData(Listing data) {
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public Observable<User> loadUser(String query) {
        Observable<User> observable = reddit.user(query, "about", null, null, null, null)
                .flatMap(new Func1<String, Observable<User>>() {
                    @Override
                    public Observable<User> call(String response) {
                        try {
                            return Observable.just(User.fromJson(ComponentStatic.getObjectMapper().readValue(
                                    response, JsonNode.class).get("data")));
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                });
        observable.subscribe(new FinalizingSubscriber<User>() {
                    @Override
                    public void start() {
                        setLoading(true);
                    }

                    @Override
                    public void next(User user) {
                        setUser(user);
                    }

                    @Override
                    public void finish() {
                        setLoading(false);
                    }
                });
        return observable;
    }

    public Sort getSort() {
        return sort;
    }

    public Observable<Listing> setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            for (Listener listener : listeners) {
                listener.setSortAndTime(sort, time);
            }
            return reload();
        }
        return Observable.empty();
    }

    public Time getTime() {
        return time;
    }

    public Observable<Listing> setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            for (Listener listener : listeners) {
                listener.setSortAndTime(sort, time);
            }
            return reload();
        }
        return Observable.empty();
    }

    public Comment getTopComment() {
        return page.getPage().equalsIgnoreCase(PAGE_OVERVIEW) ? topComment : null;
    }

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
                    public void next(Comment next) {
                        insertComment(next);
                    }
                });
    }

    public void add(int position, Link link) {
        data.getChildren().add(position, link);
        for (Listener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }
    }

    public void setTopLink(Link topLink) {
        this.topLink = topLink;
    }

    public interface Listener
            extends ControllerListener {
        void setSortAndTime(Sort sort, Time time);

        void setPage(Page page);

        void setIsUser(boolean isUser);

        void loadLink(Comment comment);
    }

}