/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.PicassoEndCallback;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.ViewHolderBase;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();
    private static final int ALPHA_OVERLAY = 140;
    private static final int ALPHA_OVERLAY_IMAGE = 200;

    public AdapterLinkGrid(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase) {
        super(activity, adapterListener, eventListenerHeader, eventListenerBase);
    }

    @Override
    public void setActivity(FragmentActivity activity) {
        super.setActivity(activity);

        Resources resources = activity.getResources();

        int spanCount = 0;

        try {
            spanCount = Integer.parseInt(preferences.getString(AppSettings.PREF_GRID_COLUMNS, String.valueOf(0)));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (spanCount <= 0) {
            int columnThreshold = resources.getDimensionPixelSize(R.dimen.grid_column_width_threshold);
            int width = resources.getDisplayMetrics().widthPixels;
            int columns = width / columnThreshold;
            spanCount = Math.max(1, columns);
        }

        layoutManager = new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_subreddit, parent, false),
                    adapterCallback,
                    eventListenerHeader);
        }

        return new ViewHolder(activity,
                parent,
                adapterCallback,
                adapterListener,
                eventListenerBase,
                Source.LINKS,
                this);
    }

    @Override
    public void onBindViewHolder(ViewHolderBase holder, int position) {

        super.onBindViewHolder(holder, position);

        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(subreddit);
                break;
            case TYPE_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(data.get(position - 1), showSubreddit);
                break;
        }
    }

    public static class ViewHolder extends ViewHolderLink {

        @Bind(R.id.layout_background) ViewGroup layoutBackground;
        @Bind(R.id.image_square) ImageView imageSquare;

        private int colorBackgroundDefault;
        protected ValueAnimator valueAnimatorBackground;

        public ViewHolder(FragmentActivity activity,
                ViewGroup parent,
                AdapterCallback adapterCallback,
                AdapterListener adapterListener,
                EventListener eventListener,
                Source source,
                CallbackYouTubeDestruction callbackYouTubeDestruction) {
            super(activity, parent, R.layout.cell_link, adapterCallback, adapterListener, eventListener, source, callbackYouTubeDestruction);

        }

        @Override
        protected void initialize() {
            super.initialize();
            if (layoutBackground.getBackground() instanceof ColorDrawable) {
                colorBackgroundDefault = ((ColorDrawable) layoutBackground.getBackground()).getColor();
            }
        }

        @Override
        protected void initializeListeners() {
            super.initializeListeners();
            imageSquare.setOnClickListener(v -> {
                imageSquare.setVisibility(View.GONE);
                progressImage.setVisibility(View.GONE);
                imagePlay.setVisibility(View.GONE);

                if (link.isSelf()) {
                    attemptLoadImageSelfPost();
                }

                onClickThumbnail();
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    if (mediaPlayer != null) {
                        destroySurfaceView();
                        imageSquare.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    super.onClick(v);
                    break;
                default:
                    super.onClick(v);
            }
        }

        @Override
        public void onBind(Link link, boolean showSubreddit) {
            super.onBind(link, showSubreddit);

            int position = getAdapterPosition();

            if (link.getBackgroundColor() == 0) {
                link.setBackgroundColor(colorBackgroundDefault);
            }

            if (viewOverlay.getVisibility() == View.GONE) {
                layoutBackground.setBackgroundColor(link.getBackgroundColor());
                viewOverlay.setBackgroundColor(0x00000000);
            }
            else {
                layoutBackground.setBackgroundColor(0x00000000);
                viewOverlay.setBackgroundColor(ColorUtils.setAlphaComponent(link.getBackgroundColor(), ALPHA_OVERLAY));
            }

            Drawable drawable = UtilsImage.getDrawableForLink(itemView.getContext(), link);
            if (drawable != null) {
                imageSquare.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawable);
                showThumbnail(true);

                if (link.isSelf()) {
                    loadSelfPostThumbnail(link);
                }
            }
            else if (!sharedPreferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) ||
                    (link.isOver18() && !sharedPreferences
                            .getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true))) {
                imageSquare.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                showThumbnail(true);
            }
            else if (UtilsImage.showThumbnail(link)) {
                loadThumbnail(link, position);
            }
            else {
                String thumbnail = UtilsImage.parseThumbnail(link);
                if (URLUtil.isNetworkUrl(thumbnail)) {
                    imageSquare.setVisibility(View.GONE);
                    imageThumbnail.clearColorFilter();
                    showThumbnail(true);

                    picasso.load(thumbnail)
                            .tag(TAG_PICASSO)
                            .priority(Picasso.Priority.HIGH)
                            .into(imageThumbnail);
                }
                else {
                    imageSquare.setVisibility(View.GONE);
                    imageThumbnail.setColorFilter(colorFilterIconDefault);
                    imageThumbnail.setImageDrawable(drawableDefault);
                    showThumbnail(true);
                }
            }
        }

        @Override
        public void onRecycle() {
            super.onRecycle();
            picasso.cancelRequest(imageSquare);
            expandFull(false);
            if (valueAnimatorBackground != null) {
                valueAnimatorBackground.cancel();
            }

            buttonComments.setColorFilter(colorFilterIconDefault);
            imagePlay.setColorFilter(colorFilterIconDefault);
            textThreadInfo.setTextColor(colorTextSecondaryDefault);
            textHidden.setTextColor(colorTextSecondaryDefault);

            imagePlay.setVisibility(View.GONE);
        }

        @Override
        public void expandFull(boolean expand) {
            super.expandFull(expand);

            itemView.postOnAnimation(() -> {
                if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                    ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                            .setFullSpan(expand);
                }
                if (expand) {
                    if (adapterCallback.getRecyclerView().getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager) adapterCallback.getRecyclerView().getLayoutManager())
                                .invalidateSpanAssignments();
                    }
                }
            });
        }

        @Override
        protected void toggleToolbarActions() {
            super.toggleToolbarActions();
            setOverflowColorFilter();
        }

        private int getAdjustedThumbnailSize() {
            int width = adapterCallback.getRecyclerView().getWidth();

            RecyclerView.LayoutManager layoutManager = adapterCallback.getRecyclerView().getLayoutManager();
            if (layoutManager instanceof StaggeredGridLayoutManager) {
                width /= ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
            }

            return (int) (width * Float.parseFloat(sharedPreferences.getString(AppSettings.PREF_GRID_THUMBNAIL_SIZE, "0.5")));
        }

        private void loadThumbnail(final Link link, final int position) {

            // TODO: Improve thumbnail loading logic

            imageSquare.setVisibility(View.VISIBLE);
            showThumbnail(false);
            progressImage.setVisibility(View.VISIBLE);

            picasso.cancelRequest(imageSquare);
            imageSquare.setImageDrawable(null);

            final int size = getAdjustedThumbnailSize();

            String thumbnail = UtilsImage.parseThumbnail(link);

            if (URLUtil.isNetworkUrl(thumbnail)) {
                picasso.load(thumbnail)
                        .tag(TAG_PICASSO)
                        .priority(Picasso.Priority.HIGH)
                        .into(imageSquare,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();

                                        if (position == getAdapterPosition()) {
                                            if (UtilsImage.placeImageUrl(link)) {
                                                RequestCreator request = picasso.load(link.getUrl())
                                                        .tag(TAG_PICASSO)
                                                        .priority(Picasso.Priority.HIGH);

                                                if (size > 0) {
                                                    request.centerCrop()
                                                            .resize(size, size);
                                                }

                                                request.into(imageSquare, new PicassoEndCallback() {
                                                            @Override
                                                            public void onEnd() {
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }
                                                        });
                                            }
                                            else {
                                                imagePlay.setImageResource(UtilsImage.isAlbum(link)
                                                        ? R.drawable.ic_photo_album_white_48dp
                                                        : R.drawable.ic_play_circle_outline_white_48dp);

                                                imagePlay.setColorFilter(colorFilterMenuItem);
                                                imagePlay.setVisibility(View.VISIBLE);
                                                progressImage.setVisibility(View.GONE);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }
            else if (UtilsImage.placeImageUrl(link)) {
                RequestCreator request = picasso.load(link.getUrl())
                        .tag(TAG_PICASSO)
                        .priority(Picasso.Priority.HIGH);

                if (size > 0) {
                    request.centerCrop()
                            .resize(size, size);
                }

                request.into(imageSquare,
                                new PicassoEndCallback() {
                                    @Override
                                    public void onSuccess() {
                                        super.onSuccess();
                                        loadBackgroundColor();

                                        if (position == getAdapterPosition()) {
                                            if (UtilsImage.isAlbum(link)) {
                                                imagePlay.setImageResource(
                                                        R.drawable.ic_photo_album_white_48dp);
                                                imagePlay.setColorFilter(colorFilterMenuItem);
                                                imagePlay.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        super.onError();
                                        showThumbnail(true);
                                        imageThumbnail.setColorFilter(colorFilterIconDefault);
                                        imageThumbnail.setImageDrawable(drawableDefault);
                                        imageSquare.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onEnd() {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }
            else {
                showThumbnail(true);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                progressImage.setVisibility(View.GONE);
                imageSquare.setVisibility(View.GONE);
            }
        }

        private void loadSelfPostThumbnail(final Link link) {

            // TODO: Improve thumbnail loading logic

            imageSquare.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.VISIBLE);

            final int size = getAdjustedThumbnailSize();

            String thumbnail = UtilsImage.parseSourceImage(link);

            if (URLUtil.isNetworkUrl(thumbnail)) {
                picasso.load(thumbnail)
                        .tag(TAG_PICASSO)
                        .resize(size, size)
                        .centerCrop()
                        .priority(Picasso.Priority.HIGH)
                        .into(imageSquare,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();
                                        progressImage.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onError() {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }
            else {
                imageSquare.setVisibility(View.GONE);
                progressImage.setVisibility(View.GONE);
            }
        }

        public boolean attemptLoadImageSelfPost() {
            final String url = UtilsImage.parseSourceImage(link);

            if (URLUtil.isNetworkUrl(url)) {
                imageFull.setVisibility(View.VISIBLE);
                expandFull(true);
                adapterCallback.getRecyclerView().getLayoutManager().requestLayout();
                itemView.invalidate();
                itemView.post(() -> {
                    picasso.load(url)
                            .into(imageFull);
                });

                return true;
            }

            return false;
        }

        public void loadBackgroundColor() {
            if (link.getBackgroundColor() != colorBackgroundDefault) {
                syncBackgroundColor();
                return;
            }

            final Link linkSaved = link;

            final int position = getAdapterPosition();
            Drawable drawable = imageSquare.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Palette.from(((BitmapDrawable) drawable).getBitmap())
                        .generate(palette -> {
                            if (position == getAdapterPosition()) {
                                linkSaved.setBackgroundColor(palette.getDarkVibrantColor(
                                        palette.getMutedColor(colorBackgroundDefault)));
                                syncBackgroundColor();
                            }
                        });
            }
        }

        public void syncBackgroundColor() {
            int color = link.getBackgroundColor();

            if (viewOverlay.getVisibility() == View.GONE) {
                int viewBackgroundColor = ((ColorDrawable) layoutBackground.getBackground())
                        .getColor();
                if (link.getBackgroundColor() != viewBackgroundColor) {
                    valueAnimatorBackground = UtilsAnimation.animateBackgroundColor(
                            layoutBackground,
                            viewBackgroundColor,
                            color);
                }

                setTextColors(color);
            }
            else {
                color = ColorUtils.setAlphaComponent(color, ALPHA_OVERLAY_IMAGE);
                int overlayBackgroundColor = ((ColorDrawable) viewOverlay.getBackground())
                        .getColor();

                layoutBackground.setBackgroundColor(0x00000000);
                if (link.getBackgroundColor() != overlayBackgroundColor) {
                    valueAnimatorBackground = UtilsAnimation.animateBackgroundColor(
                            viewOverlay,
                            overlayBackgroundColor,
                            color);
                }

                titleTextColor = colorTextPrimaryDefault;
                colorTextSecondary = colorTextSecondaryDefault;
                syncTitleColor();
            }
        }

        public void setTextColors(int color) {
            Menu menu = toolbarActions.getMenu();

            boolean showOnWhite = UtilsColor.showOnWhite(color);

            if (showOnWhite) {
                imagePlay.setColorFilter(colorFilterIconLight);
                buttonComments.setColorFilter(colorFilterIconLight);
                textThreadInfo.setTextColor(getColor(R.color.darkThemeTextColorMuted));
                textHidden.setTextColor(getColor(R.color.darkThemeTextColorMuted));
                colorTextSecondary = getColor(R.color.darkThemeTextColorMuted);
                titleTextColorAlert = getColor(R.color.textColorAlert);
                titleTextColor = getColor(R.color.darkThemeTextColor);
                colorFilterMenuItem = colorFilterIconLight;

            }
            else {
                imagePlay.setColorFilter(colorFilterIconDark);
                buttonComments.setColorFilter(colorFilterIconDark);
                textThreadInfo.setTextColor(getColor(R.color.lightThemeTextColorMuted));
                textHidden.setTextColor(getColor(R.color.lightThemeTextColorMuted));
                colorTextSecondary = getColor(R.color.lightThemeTextColorMuted);
                titleTextColorAlert = getColor(R.color.textColorAlertMuted);
                titleTextColor = getColor(R.color.lightThemeTextColor);
                colorFilterMenuItem = colorFilterIconDark;
            }
            syncTitleColor();

            setOverflowColorFilter();

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterMenuItem);
            }
        }

        public void setOverflowColorFilter() {
            ArrayList<View> views = new ArrayList<>();
            toolbarActions.findViewsWithText(views, "toolbar_overflow_access",
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

            if (!views.isEmpty()) {
                ImageView imageOverflow = (ImageView) views.get(0);
                imageOverflow.setColorFilter(colorFilterMenuItem);
            }
        }

        @Override
        public void setTextValues(Link link) {
            super.setTextValues(link);

            showThumbnail(imageThumbnail.getVisibility() == View.VISIBLE);

            textThreadInfo.setText(TextUtils
                    .concat(getSubredditString(), showSubreddit ? "\n" : "", getSpannableScore(),
                            "by ", link.getAuthor(), " ", getFlairString()));

            Linkify.addLinks(textThreadInfo, Linkify.WEB_URLS);

            textHidden.setText(resources.getString(R.string.hidden_description, getTimestamp(), link.getNumComments()));
        }

        @Override
        public void setAlbum(Link link, Album album) {
            super.setAlbum(link, album);
            showThumbnail(false);
        }

        private void showThumbnail(boolean show) {
            imageThumbnail.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show) {
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(titleMargin);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(RelativeLayout.START_OF);
                ((RelativeLayout.LayoutParams) textThreadFlair.getLayoutParams()).addRule(RelativeLayout.BELOW, imageThumbnail.getId());
            }
            else {
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(0);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(RelativeLayout.START_OF, buttonComments.getId());
                ((RelativeLayout.LayoutParams) textThreadFlair.getLayoutParams()).addRule(RelativeLayout.BELOW, viewMargin.getId());
            }

            textThreadTitle.setText(link.getTitle());
            textThreadTitle.requestLayout();
        }

        @Override
        public void clearOverlay() {
            layoutBackground.setBackgroundColor(link.getBackgroundColor());
            setTextColors(link.getBackgroundColor());
            viewOverlay.setVisibility(View.GONE);
        }

        @Override
        public int[] getScreenAnchor() {
            int[] location = new int[2];
            if (link.isSelf()) {
                imageThumbnail.getLocationOnScreen(location);

                if (imageSquare.isShown() || imageFull.isShown()) {
                    location[1] -= itemView.getWidth();
                }
            }
            else if (imageSquare.isShown()) {
                imageSquare.getLocationOnScreen(location);
            }
            else {
                layoutFull.getLocationOnScreen(location);
                location[1] += layoutFull.getHeight();
                if (!imageThumbnail.isShown()) {
                    location[1] -= itemView.getWidth();
                }
            }

            return location;
        }
    }

}