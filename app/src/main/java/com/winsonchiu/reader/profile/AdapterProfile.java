/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
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

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterBase;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.ViewHolderBase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterProfile extends AdapterBase<RecyclerView.ViewHolder> implements CallbackYouTubeDestruction {

    private static final String TAG = AdapterProfile.class.getCanonicalName();;

    private FragmentActivity activity;
    private AdapterListener adapterListener;
    private AdapterLink.ViewHolderLink.Listener listenerLink;
    private AdapterCommentList.ViewHolderComment.Listener listenerComments;
    private ControllerProfile.Listener listener;
    private List<RecyclerView.ViewHolder> viewHolders;

    protected ControllerProfile controllerProfile;
    @Inject ControllerUser controllerUser;

    public AdapterProfile(FragmentActivity activity,
            ControllerProfile controllerProfile,
            AdapterListener adapterListener,
            AdapterLink.ViewHolderLink.Listener listenerLink,
            AdapterCommentList.ViewHolderComment.Listener listenerComments,
            ControllerProfile.Listener listener) {
        ((ActivityMain) activity).getComponentActivity().inject(this);
        this.activity = activity;
        this.adapterListener = adapterListener;
        this.listenerLink = listenerLink;
        this.listenerComments = listenerComments;
        this.listener = listener;
        this.controllerProfile = controllerProfile;
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
                return controllerProfile.getPage().getPage().equals(ControllerProfile.PAGE_OVERVIEW) && controllerProfile.getTopLink() != null ? ControllerProfile.VIEW_TYPE_LINK : ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 3:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 4:
                return controllerProfile.getPage().getPage().equals(ControllerProfile.PAGE_OVERVIEW) && controllerProfile.getTopComment() != null  ? ControllerProfile.VIEW_TYPE_COMMENT : ControllerProfile.VIEW_TYPE_HEADER_TEXT;
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
                        .inflate(R.layout.row_header, parent, false), adapterCallback);
            case ControllerProfile.VIEW_TYPE_HEADER_TEXT:
                return new ViewHolderText(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_text, parent, false), adapterCallback);
            case ControllerProfile.VIEW_TYPE_LINK:
                AdapterLink.ViewHolderLink viewHolder = new AdapterLinkList.ViewHolder(
                        activity,
                        parent,
                        adapterCallback,
                        adapterListener,
                        listenerLink,
                        Source.PROFILE,
                        this);
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
                        LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comment, parent, false),
                        adapterCallback,
                        adapterListener,
                        listenerComments,
                        listener);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
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
                viewHolderTextLink.setVisibility(controllerProfile.getPage().getPage().equalsIgnoreCase(
                        ControllerProfile.PAGE_OVERVIEW) ? View.VISIBLE : View.GONE);
                break;
            case 2:
                if (holder.getItemViewType() == ControllerProfile.VIEW_TYPE_LINK) {
                    AdapterLinkList.ViewHolder viewHolderLinkTop = (AdapterLinkList.ViewHolder) holder;
                    viewHolderLinkTop.onRecycle();
                    viewHolderLinkTop.onBind(controllerProfile.getTopLink(), controllerUser.getUser(), true);
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
                viewHolderTextComment.setVisibility(controllerProfile.getPage().getPage().equalsIgnoreCase(
                        ControllerProfile.PAGE_OVERVIEW) ? View.VISIBLE : View.GONE);
                break;
            case 4:
                if (holder.getItemViewType() == ControllerProfile.VIEW_TYPE_COMMENT) {
                    AdapterCommentList.ViewHolderComment viewHolderCommentTop = (AdapterCommentList.ViewHolderComment) holder;
                    viewHolderCommentTop.onBind(controllerProfile.getTopComment(), controllerUser.getUser());
                }
                else {
                    ViewHolderText viewHolderText = (ViewHolderText) holder;
                    viewHolderText.setVisibility(View.GONE);
                }
                break;
            case 5:
                ViewHolderText viewHolderTextOverview = (ViewHolderText) holder;
                viewHolderTextOverview.onBind(controllerProfile.getPage().getText());
                viewHolderTextOverview.setVisibility(View.VISIBLE);
                break;
            default:
                if (holder instanceof AdapterLinkList.ViewHolder) {

                    AdapterLinkList.ViewHolder viewHolderLink = (AdapterLinkList.ViewHolder) holder;
                    viewHolderLink.onBind(controllerProfile.getLink(position), controllerUser.getUser(), true);

                }
                else if (holder instanceof AdapterCommentList.ViewHolderComment) {

                    AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                    viewHolderComment.onBind(controllerProfile.getComment(position), controllerProfile.getUser());
                }
        }
        viewHolders.add(holder);

    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof AdapterLink.ViewHolderLink) {
            ((AdapterLink.ViewHolderLink) holder).onRecycle();
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

    public void setVisibility(int visibility, @NonNull Thing thing) {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            switch (viewHolder.getItemViewType()) {

                case ControllerProfile.VIEW_TYPE_LINK:
                    if (thing.equals(((AdapterLink.ViewHolderLink) viewHolder).link)) {
                        viewHolder.itemView.setVisibility(visibility);
                    }
                case ControllerProfile.VIEW_TYPE_COMMENT:
                case ControllerProfile.VIEW_TYPE_HEADER:
                case ControllerProfile.VIEW_TYPE_HEADER_TEXT:
                    break;
            }
        }
    }

    public void pauseViewHolders() {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof AdapterLink.ViewHolderLink) {
                AdapterLink.ViewHolderLink viewHolderLink = (AdapterLink.ViewHolderLink) viewHolder;
                if (viewHolderLink.mediaPlayer != null) {
                    viewHolderLink.mediaPlayer.stop();
                }
            }
        }
        destroyYouTubePlayerFragments();
    }

    @Override
    public void destroyYouTubePlayerFragments() {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof AdapterLink.ViewHolderLink) {
                ((AdapterLink.ViewHolderLink) viewHolder).destroyYouTube();
            }
        }
    }

    public static class ViewHolderHeader extends ViewHolderBase {

        protected TextView textUsername;
        protected TextView textKarma;

        public ViewHolderHeader(View itemView, AdapterCallback adapterCallback) {
            super(itemView, adapterCallback);

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

    public static class ViewHolderText extends ViewHolderBase {

        protected TextView textMessage;

        public ViewHolderText(View itemView, AdapterCallback adapterCallback) {
            super(itemView, adapterCallback);

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
