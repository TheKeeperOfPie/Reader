/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    public AdapterLinkList(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.Listener listenerLink) {
        super(activity, adapterListener, eventListenerHeader, listenerLink);
        layoutManager = new LinearLayoutManager(activity);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.header_subreddit, parent, false),
                    adapterCallback,
                    eventListenerHeader);
        }

        return new ViewHolder(activity,
                parent,
                adapterCallback,
                adapterListener,
                listenerLink,
                Source.LINKS,
                this);
    }

    @Override
    public void onBindViewHolder(ViewHolderBase holder, int position) {

        super.onBindViewHolder(holder, position);

        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(data.getSubreddit());
                break;
            case TYPE_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(data.getLinks().get(position - 1), data.getUser(), data.isShowSubreddit());
                break;
        }
    }

    public static class ViewHolder extends ViewHolderLink {

        public ViewHolder(FragmentActivity activity,
                ViewGroup parent,
                AdapterCallback adapterCallback,
                AdapterListener adapterListener,
                Listener listener,
                Source source,
                CallbackYouTubeDestruction callbackYouTubeDestruction) {
            super(activity, parent, R.layout.row_link, adapterCallback, adapterListener, listener, source, callbackYouTubeDestruction);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        destroySurfaceView();
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                default:
                    super.onClick(v);
            }
        }

        @Override
        public void onBind(Link link, User user, boolean showSubreddit) {
            super.onBind(link, user, showSubreddit);

            imageThumbnail.setVisibility(View.VISIBLE);

            Drawable drawable = UtilsImage.getDrawableForLink(itemView.getContext(), link);
            if (drawable == null) {
                String thumbnail = UtilsImage.parseThumbnail(link);
                if (!URLUtil.isNetworkUrl(thumbnail) || !sharedPreferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) || (link.isOver18() && !sharedPreferences.getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true))) {
                    imageThumbnail.setColorFilter(colorFilterIconDefault);
                    imageThumbnail.setImageDrawable(drawableDefault);
                }
                else {
                    imageThumbnail.clearColorFilter();

//                    recyclerCallback.getRequestManager()
//                            .load(thumbnail)
//                            .into(new GlideDrawableImageViewTarget(imageThumbnail));
                    picasso.load(thumbnail)
                            .into(imageThumbnail);
                }
            }
            else {
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawable);
            }
        }

        @Override
        public int[] getScreenAnchor() {
            int[] location = new int[2];
            layoutFull.getLocationOnScreen(location);
            location[1] += layoutFull.getHeight();
            return location;
        }
    }

}