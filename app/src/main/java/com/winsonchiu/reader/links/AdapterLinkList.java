/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    public AdapterLinkList(FragmentActivity activity,
            ControllerLinksBase controllerLinks,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(activity, eventListenerHeader, eventListenerBase, disallowListener, recyclerCallback);
        setController(controllerLinks);
    }

    @Override
    public void setActivity(FragmentActivity activity) {
        super.setActivity(activity);
        layoutManager = new LinearLayoutManager(activity);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == TYPE_HEADER) {
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
    public void onBindViewHolder(ViewHolderBase holder, int position) {

        super.onBindViewHolder(holder, position);

        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerLinks.getSubreddit());
                break;
            case TYPE_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(controllerLinks.getLink(position), controllerLinks.showSubreddit());
                break;
        }
    }

    public static class ViewHolder extends ViewHolderLink {

        public ViewHolder(FragmentActivity activity,
                View itemView,
                EventListener eventListener,
                Source source,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                CallbackYouTubeDestruction callbackYouTubeDestruction) {
            super(activity, itemView, eventListener, source, disallowListener, recyclerCallback, callbackYouTubeDestruction);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        destroySurfaceView();
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    loadComments();
                    break;
                default:
                    super.onClick(v);
            }
        }

        @Override
        public float getRatio() {
            return 1f;
        }

        @Override
        public void onBind(Link link, boolean showSubreddit) {
            super.onBind(link, showSubreddit);

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
        public void setTextValues(Link link) {
            super.setTextValues(link);

            textThreadInfo.setText(TextUtils
                    .concat(getSubredditString(), getSpannableScore(), "by ", link.getAuthor(),
                            " ", getFlairString()));

            Linkify.addLinks(textThreadInfo, Linkify.WEB_URLS);

            textHidden.setText(resources.getString(R.string.hidden_description, getTimestamp(), link.getNumComments()));

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