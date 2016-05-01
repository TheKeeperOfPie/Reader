/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.adapter.AdapterBase;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsReddit;
import com.winsonchiu.reader.utils.UtilsView;
import com.winsonchiu.reader.utils.YouTubeListener;

import javax.inject.Inject;

/**
 * Created by TheKeeperOfPie on 12/29/2015.
 */
public class AdapterLinkHeader extends AdapterBase<AdapterLink.ViewHolderLink> implements CallbackYouTubeDestruction {

    private FragmentActivity activity;
    private boolean isGrid;
    private String firstLinkName;
    private int colorLink;
    private boolean actionsExpanded;
    private AdapterLink.ViewHolderLink viewHolderLink;
    private AdapterListener adapterListener;
    private AdapterLink.ViewHolderLink.Listener listenerLink;
    private YouTubeListener youTubeListener;
    private CallbackYouTubeDestruction callbackYouTubeDestruction;
    private boolean animationFinished;

    private ControllerComments controllerComments;

    @Inject ControllerUser controllerUser;

    public AdapterLinkHeader(FragmentActivity activity,
            ControllerComments controllerComments,
            AdapterListener adapterListener,
            AdapterLink.ViewHolderLink.Listener listenerLink,
            YouTubeListener youTubeListener,
            CallbackYouTubeDestruction callbackYouTubeDestruction,
            boolean isGrid,
            String firstLinkName,
            int colorLink,
            boolean actionsExpanded) {
        ((ActivityMain) activity).getComponentActivity().inject(this);
        this.activity = activity;
        this.controllerComments = controllerComments;
        this.adapterListener = adapterListener;
        this.listenerLink = listenerLink;
        this.youTubeListener = youTubeListener;
        this.callbackYouTubeDestruction = callbackYouTubeDestruction;
        this.isGrid = isGrid;
        this.firstLinkName = firstLinkName;
        this.colorLink = colorLink;
        this.actionsExpanded = actionsExpanded;
    }

    @Override
    public AdapterLink.ViewHolderLink onCreateViewHolder(ViewGroup parent, int viewType) {
        if (isGrid) {
            viewHolderLink = new AdapterLinkGrid.ViewHolder(
                    activity,
                    parent,
                    adapterCallback,
                    adapterListener,
                    listenerLink,
                    Source.NONE,
                    callbackYouTubeDestruction) {

                @Override
                protected Intent getShareIntent() {
                    return UtilsReddit.getShareIntentLinkComments(link);
                }

                @Override
                public boolean isInHistory() {

                    return false;
                }

                @Override
                public void loadBackgroundColor() {
                    if (colorLink != 0) {
                        if (link.getName().equals(firstLinkName)) {
                            link.setBackgroundColor(colorLink);
                            itemView.setBackgroundColor(colorLink);
                            setTextColors(colorLink, link.getTextTitleColor(), link.getTextBodyColor());
                        }
                        colorLink = 0;
                    } else {
                        super.loadBackgroundColor();
                    }
                }

                @Override
                public void onBind(Link link, @Nullable User user, boolean showSubreddit) {
                    super.onBind(link, user, showSubreddit);
                    if (actionsExpanded) {
                        setToolbarMenuVisibility();
                        showToolbarActionsInstant();
                    }
                    if (animationFinished) {
                        if (!TextUtils.isEmpty(link.getSelfText())) {
                            UtilsAnimation.animateExpandHeight(textThreadSelf, UtilsView.getContentWidth(adapterCallback.getRecyclerView().getLayoutManager()), 0, null);
                        }
                    }
                }

                @Override
                public void onRecycle() {
                    super.onRecycle();
                    actionsExpanded = false;
                }

                @Override
                public void onClickComments() {
                    controllerComments.loadLinkComments();
                }

                @Override
                public void addToHistory() {
                    // Override to prevent adding to history
                }
            };
        }
        else {
            viewHolderLink = new AdapterLinkList.ViewHolder(
                    activity,
                    parent,
                    adapterCallback,
                    adapterListener,
                    listenerLink,
                    Source.NONE,
                    callbackYouTubeDestruction) {

                @Override
                protected Intent getShareIntent() {
                    return UtilsReddit.getShareIntentLinkComments(link);
                }

                @Override
                public boolean isInHistory() {
                    return false;
                }

                @Override
                public void onBind(Link link, User user, boolean showSubreddit) {
                    super.onBind(link, user, showSubreddit);
                    if (actionsExpanded) {
                        setToolbarMenuVisibility();
                        showToolbarActionsInstant();
                    }
                    if (animationFinished) {
                        if (!TextUtils.isEmpty(link.getSelfText())) {
                            UtilsAnimation.animateExpandHeight(textThreadSelf, UtilsView.getContentWidth(adapterCallback.getRecyclerView().getLayoutManager()), 0, null);
                        }
                    }
                }

                @Override
                public void onRecycle() {
                    super.onRecycle();
                    actionsExpanded = false;
                }

                @Override
                public void onClickComments() {
                    controllerComments.loadLinkComments();
                }

                @Override
                public void addToHistory() {
                    // Override to prevent adding to history
                }
            };
        }

        viewHolderLink.setYouTubeListener(youTubeListener);

        return viewHolderLink;
    }

    @Override
    public void onBindViewHolder(AdapterLink.ViewHolderLink holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.onBind(controllerComments.getLink(), controllerUser.getUser(), true);
    }

    @Override
    public void onViewRecycled(AdapterLink.ViewHolderLink holder) {
        holder.onRecycle();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public void setAnimationFinished(boolean animationFinished) {
        this.animationFinished = animationFinished;
        notifyDataSetChanged();
    }

    public void recycle() {
        if (viewHolderLink != null) {
            viewHolderLink.onRecycle();
        }
    }

    public void destroyViewHolderLink() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyWebViews();
            destroyYouTubePlayerFragments();
        }
    }

    public void onTouchEvent(MotionEvent event) {
        viewHolderLink.itemView.dispatchTouchEvent(event);
    }

    public void onBind() {
        viewHolderLink.onBind(controllerComments.getLink(), controllerUser.getUser(), true);
    }

    @Override
    public void destroyYouTubePlayerFragments() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyYouTube();
        }
    }
}
