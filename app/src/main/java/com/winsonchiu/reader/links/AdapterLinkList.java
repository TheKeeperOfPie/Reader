/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    public AdapterLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ControllerUser controllerUser,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(eventListenerHeader, eventListenerBase, disallowListener, recyclerCallback);
        setControllers(controllerLinks, controllerUser);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        layoutManager = new LinearLayoutManager(activity);
    }

    @Override
    public RecyclerView.ViewHolder  onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_link, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false), eventListenerBase, disallowListener,
                recyclerCallback);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        super.onBindViewHolder(holder, position);

        switch (getItemViewType(position)) {
            case VIEW_LINK_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerLinks.getSubreddit());
                break;
            case VIEW_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(controllerLinks.getLink(position), controllerLinks.showSubreddit(), controllerUser.getUser().getName());
                break;
        }
    }

    public static class ViewHolder extends AdapterLink.ViewHolderBase {

        public ViewHolder(View itemView,
                EventListener eventListener,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback) {
            super(itemView, eventListener, disallowListener, recyclerCallback);
        }

        @Override
        protected void initializeListeners() {
            super.initializeListeners();
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoFull.isPlaying()) {
                        videoFull.stopPlayback();
                        videoFull.setVisibility(View.GONE);
                        imageThumbnail.setVisibility(View.VISIBLE);
                    }
                    loadComments();
                }
            });
        }

        @Override
        public float getRatio() {
            return 1f;
        }

        @Override
        public void onBind(Link link, boolean showSubreddit, String userName) {
            super.onBind(link, showSubreddit, userName);

            imageThumbnail.setVisibility(View.VISIBLE);

            Drawable drawable = Reddit.getDrawableForLink(itemView.getContext(), link);
            if (drawable == null) {
                if (TextUtils.isEmpty(link.getThumbnail()) || !preferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) || (link.isOver18() && !preferences.getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true))) {
                    imageThumbnail.setColorFilter(colorFilterIconDefault);
                    imageThumbnail.setImageDrawable(drawableDefault);
                }
                else {
                    imageThumbnail.clearColorFilter();
                    Picasso.with(itemView.getContext())
                            .load(link.getThumbnail())
                            .tag(TAG_PICASSO)
                            .into(imageThumbnail);
                }
            }
            else {
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawable);
            }

        }

        @Override
        public void setTextValues(Link link) {
            super.setTextValues(link);

            textThreadInfo.setText(TextUtils.concat(getSubredditString(), getSpannableScore(), "by ", link.getAuthor(), getFlairString()));

            // TODO: Add link edited indicator

            textHidden.setText(getTimestamp() + ", " + link.getNumComments() + " comments");

        }
    }

}