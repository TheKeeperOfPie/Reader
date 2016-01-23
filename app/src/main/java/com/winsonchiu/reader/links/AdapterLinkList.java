/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    public AdapterLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(activity, eventListenerHeader, eventListenerBase, disallowListener, recyclerCallback);
        setController(controllerLinks);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        layoutManager = new LinearLayoutManager(activity);
    }

    @Override
    public RecyclerView.ViewHolder  onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_subreddit, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(activity,
                LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false),
                eventListenerBase,
                Source.LINKS,
                disallowListener,
                recyclerCallback,
                this);
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
                viewHolder.onBind(controllerLinks.getLink(position), controllerLinks.showSubreddit());
                break;
        }
    }

    public static class ViewHolder extends AdapterLink.ViewHolderBase {

        public ViewHolder(Activity activity,
                View itemView,
                EventListener eventListener,
                Source source,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                CallbackYouTubeDestruction callbackYouTubeDestruction) {
            super(activity, itemView, eventListener, source, disallowListener, recyclerCallback, callbackYouTubeDestruction);
        }

        @Override
        protected void initializeListeners() {
            super.initializeListeners();
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        destroySurfaceView();
                        imagePlay.setVisibility(View.VISIBLE);
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
        public void onBind(Link link, boolean showSubreddit) {
            super.onBind(link, showSubreddit);

            imageThumbnail.setVisibility(View.VISIBLE);

            Drawable drawable = Reddit.getDrawableForLink(itemView.getContext(), link);
            if (drawable == null) {
                if (TextUtils.isEmpty(link.getThumbnail()) || !preferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) || (link.isOver18() && !preferences.getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true)) || Reddit.NSFW.equals(link.getThumbnail())) {
                    imageThumbnail.setColorFilter(colorFilterIconDefault);
                    imageThumbnail.setImageDrawable(drawableDefault);
                }
                else {
                    imageThumbnail.clearColorFilter();
                    picasso.load(link.getThumbnail())
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

            textThreadInfo.setText(TextUtils
                    .concat(getSubredditString(), getSpannableScore(), "by ", link.getAuthor(),
                            getFlairString()));

            Linkify.addLinks(textThreadInfo, Linkify.WEB_URLS);

            textHidden.setText(getTimestamp() + ", " + link.getNumComments() + " comments");

        }

        @Override
        public void expandFull(boolean expand) {
            super.expandFull(expand);
            scrollToSelf();
        }

        @Override
        public int[] getScreenAnchor() {
            int[] location = new int[2];
            frameFull.getLocationOnScreen(location);
            location[1] += frameFull.getHeight();
            return location;
        }
    }

}