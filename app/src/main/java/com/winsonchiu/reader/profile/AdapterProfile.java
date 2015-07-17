/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.links.ControllerLinksBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterProfile extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterProfile.class.getCanonicalName();

    protected ControllerProfile controllerProfile;
    protected ControllerLinksBase controllerLinks;
    protected ControllerUser controllerUser;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;
    private DisallowListener disallowListener;
    private RecyclerCallback recyclerCallback;
    private ControllerProfile.Listener listener;
    private List<RecyclerView.ViewHolder> viewHolders;
    private AdapterCommentList.ViewHolderComment.ReplyCallback replyCallback;

    public AdapterProfile(ControllerProfile controllerProfile,
            ControllerLinksBase controllerLinks,
            ControllerUser controllerUser,
            AdapterLink.ViewHolderBase.EventListener eventListenerBase,
            AdapterCommentList.ViewHolderComment.EventListener eventListenerComment,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback,
            ControllerProfile.Listener listener) {
        this.eventListenerBase = eventListenerBase;
        this.eventListenerComment = eventListenerComment;
        this.disallowListener = disallowListener;
        this.replyCallback = new AdapterCommentList.ViewHolderComment.ReplyCallback() {
            @Override
            public void onReplyShown() {

            }
        };
        this.recyclerCallback = recyclerCallback;
        this.listener = listener;
        this.controllerProfile = controllerProfile;
        this.controllerLinks = controllerLinks;
        this.controllerUser = controllerUser;
        viewHolders = new ArrayList<>();
    }
    @Override
    public int getItemViewType(int position) {

        switch (position) {
            case 0:
                return ControllerProfile.VIEW_TYPE_HEADER;
            case 1:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 2:
                return controllerProfile.getPage().equals("Overview") && controllerProfile.getTopLink() != null ? ControllerProfile.VIEW_TYPE_LINK : ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 3:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 4:
                return controllerProfile.getPage().equals("Overview") && controllerProfile.getTopComment() != null  ? ControllerProfile.VIEW_TYPE_COMMENT : ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 5:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            default:
                return controllerProfile.getViewType(position - 6);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case ControllerProfile.VIEW_TYPE_HEADER:
                return new ViewHolderHeader(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_header, parent, false));
            case ControllerProfile.VIEW_TYPE_HEADER_TEXT:
                return new ViewHolderText(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_text, parent, false));
            case ControllerProfile.VIEW_TYPE_LINK:
                AdapterLink.ViewHolderBase viewHolder = new AdapterLinkList.ViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_link, parent, false), eventListenerBase,
                        disallowListener, recyclerCallback);
                viewHolders.add(viewHolder);
                viewHolder.toolbarActions.getMenu().findItem(R.id.item_view_profile)
                        .setShowAsAction(
                                MenuItem.SHOW_AS_ACTION_NEVER);
                viewHolder.toolbarActions.getMenu().findItem(R.id.item_view_profile)
                        .setVisible(false);
                viewHolder.toolbarActions.getMenu().findItem(R.id.item_view_profile)
                        .setEnabled(false);
                return viewHolder;
            case ControllerProfile.VIEW_TYPE_COMMENT:
                return new AdapterCommentList.ViewHolderComment(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_comment, parent, false), eventListenerBase, eventListenerComment, disallowListener, replyCallback, listener);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        holder.itemView.setVisibility(View.VISIBLE);

        if (!controllerProfile.isLoading() && position > controllerProfile.sizeLinks() - 5) {
            controllerProfile.loadMore();
        }

        switch (position) {
            case 0:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerProfile.getUser());
                break;
            case 1:
                ViewHolderText viewHolderTextLink = (ViewHolderText) holder;
                viewHolderTextLink.itemView.setVisibility(
                        controllerProfile.getTopLink() == null ? View.GONE : View.VISIBLE);
                viewHolderTextLink.onBind("Top Post");
                viewHolderTextLink.setVisibility(controllerProfile.getPage().equalsIgnoreCase("Overview") ? View.VISIBLE : View.GONE);
                break;
            case 2:
                if (getItemViewType(position) == ControllerProfile.VIEW_TYPE_LINK) {
                    AdapterLinkList.ViewHolder viewHolderLinkTop = (AdapterLinkList.ViewHolder) holder;
                    viewHolderLinkTop.onRecycle();
                    viewHolderLinkTop.onBind(controllerProfile.getTopLink(), controllerLinks.showSubreddit(), controllerUser.getUser().getName());
                }
                else {
                    ViewHolderText viewHolderText = (ViewHolderText) holder;
                    viewHolderText.setVisibility(View.GONE);
                }
                break;
            case 3:
                ViewHolderText viewHolderTextComment = (ViewHolderText) holder;
                viewHolderTextComment.itemView.setVisibility(
                        controllerProfile.getTopComment() == null ? View.GONE : View.VISIBLE);
                viewHolderTextComment.onBind("Top Comment");
                viewHolderTextComment.setVisibility(controllerProfile.getPage().equalsIgnoreCase("Overview") ? View.VISIBLE : View.GONE);
                break;
            case 4:
                if (getItemViewType(position) == ControllerProfile.VIEW_TYPE_COMMENT) {
                    AdapterCommentList.ViewHolderComment viewHolderCommentTop = (AdapterCommentList.ViewHolderComment) holder;
                    viewHolderCommentTop.onBind(controllerProfile.getTopComment(), controllerUser.getUser().getName());
                }
                else {
                    ViewHolderText viewHolderText = (ViewHolderText) holder;
                    viewHolderText.setVisibility(View.GONE);
                }
                break;
            case 5:
                ViewHolderText viewHolderTextOverview = (ViewHolderText) holder;
                viewHolderTextOverview.onBind(controllerProfile.getPage());
                break;
            default:
                if (holder instanceof AdapterLinkList.ViewHolder) {

                    AdapterLinkList.ViewHolder viewHolderLink = (AdapterLinkList.ViewHolder) holder;
                    viewHolderLink.onBind(controllerProfile.getLink(position), controllerLinks.showSubreddit(), controllerUser.getUser().getName());

                }
                else if (holder instanceof AdapterCommentList.ViewHolderComment) {

                    AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                    viewHolderComment.onBind(controllerProfile.getComment(position), controllerUser.getUser().getName());
                }
        }
        viewHolders.add(holder);

    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof AdapterLink.ViewHolderBase) {
            ((AdapterLink.ViewHolderBase) holder).onRecycle();
        }

        viewHolders.remove(holder);
    }

    @Override
    public int getItemCount() {
        return controllerProfile.sizeLinks() > 0 ? controllerProfile.sizeLinks() + 4 : 0;
    }

    public void setVisibility(int visibility) {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            viewHolder.itemView.setVisibility(visibility);
        }
    }

    public void pauseViewHolders() {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof AdapterLink.ViewHolderBase) {
                AdapterLink.ViewHolderBase viewHolderBase = (AdapterLink.ViewHolderBase) viewHolder;
                viewHolderBase.videoFull.pause();
                if (viewHolderBase.youTubePlayer != null) {
                    viewHolderBase.youTubePlayer.pause();
                }
            }
        }
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected TextView textUsername;
        protected TextView textKarma;

        public ViewHolderHeader(View itemView) {
            super(itemView);

            textUsername = (TextView) itemView.findViewById(R.id.text_username);
            textKarma = (TextView) itemView.findViewById(R.id.text_karma);
        }

        public void onBind(User user) {
            textUsername.setText(user.getName());

            int linkLength = String.valueOf(user.getLinkKarma())
                    .length();
            int commentLength = String.valueOf(user.getCommentKarma())
                    .length();

            Spannable spannableInfo = new SpannableString(
                    user.getLinkKarma() + " Link Karma\n" + user
                            .getCommentKarma() + " Comment Karma");
            spannableInfo.setSpan(new ForegroundColorSpan(
                            user.getLinkKarma() > 0 ?
                                    itemView.getResources().getColor(
                                            R.color.positiveScore) :
                                    itemView.getResources().getColor(
                                            R.color.negativeScore)), 0, linkLength,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(
                            user.getCommentKarma() > 0 ?
                                    itemView.getResources().getColor(
                                            R.color.positiveScore) :
                                    itemView.getResources()
                                            .getColor(R.color.negativeScore)), linkLength + 12,
                    linkLength + 12 + commentLength, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            textKarma.setText(spannableInfo);
        }
    }

    public static class ViewHolderText extends RecyclerView.ViewHolder {

        protected TextView textMessage;

        public ViewHolderText(View itemView) {
            super(itemView);

            textMessage = (TextView) itemView.findViewById(R.id.text_message);
        }

        public void onBind(String text) {
            textMessage.setText(text);
        }

        public void setVisibility(int visibility) {
            textMessage.setVisibility(visibility);
            itemView.setVisibility(visibility);
        }

    }

}