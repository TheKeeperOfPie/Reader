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
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.ViewHolderBase;

import java.util.ArrayList;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();
    private static final int ALPHA_OVERLAY = 140;
    private static final int ALPHA_OVERLAY_IMAGE = 200;
    protected int thumbnailSize;

    public AdapterLinkGrid(FragmentActivity activity,
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

        layoutManager = new StaggeredGridLayoutManager(spanCount,
                StaggeredGridLayoutManager.VERTICAL);
//        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(
//                StaggeredGridLayoutManager.GAP_HANDLING_NONE);

        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        this.thumbnailSize = displayMetrics.widthPixels / 2;
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == TYPE_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.header_subreddit, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(activity,
                LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false),
                eventListenerBase,
                Source.LINKS,
                disallowListener,
                recyclerCallback,
                this,
                thumbnailSize);
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

        private final int thumbnailSize;
        protected ImageView imageSquare;
        private int colorBackgroundDefault;
        private ValueAnimator valueAnimatorBackground;

        public ViewHolder(FragmentActivity activity,
                View itemView,
                EventListener eventListener,
                Source source,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                CallbackYouTubeDestruction callbackYouTubeDestruction,
                int thumbnailSize) {
            super(activity, itemView, eventListener, source, disallowListener, recyclerCallback, callbackYouTubeDestruction);
            this.thumbnailSize = thumbnailSize;

        }

        @Override
        protected void initialize() {
            super.initialize();
            imageSquare = (ImageView) itemView.findViewById(R.id.image_square);
            if (itemView.getBackground() instanceof ColorDrawable) {
                colorBackgroundDefault = ((ColorDrawable) itemView.getBackground()).getColor();
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
                    loadSelfText();
                }
                else {
                    loadFull();
                }
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        destroySurfaceView();
                        imageSquare.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    loadComments();
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
                itemView.setBackgroundColor(link.getBackgroundColor());
                viewOverlay.setBackgroundColor(0x00000000);
            }
            else {
                itemView.setBackgroundColor(0x00000000);
                viewOverlay.setBackgroundColor(ColorUtils.setAlphaComponent(link.getBackgroundColor(), ALPHA_OVERLAY));
            }

            buttonComments.setColorFilter(colorFilterIconDefault);
            imagePlay.setColorFilter(colorFilterIconDefault);
            textThreadInfo.setTextColor(colorTextSecondaryDefault);
            textHidden.setTextColor(colorTextSecondaryDefault);

            imagePlay.setVisibility(View.GONE);

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

//                    recyclerCallback.getRequestManager()
//                            .load(thumbnail)
//                            .priority(Priority.HIGH)
//                            .into(new GlideDrawableImageViewTarget(imageThumbnail));
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
        public void expandFull(boolean expand) {
            super.expandFull(expand);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                        .setFullSpan(expand);
            }
            if (expand) {
                if (recyclerCallback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager) recyclerCallback.getLayoutManager())
                            .invalidateSpanAssignments();
                }
                scrollToSelf();
            }
        }

        @Override
        protected void toggleToolbarActions() {
            super.toggleToolbarActions();
            setOverflowColorFilter();
        }

        private int getAdjustedThumbnailSize() {
            float modifier = Float.parseFloat(
                    sharedPreferences.getString(AppSettings.PREF_GRID_THUMBNAIL_SIZE, "0.75"));
            if (modifier > 0) {
                return (int) (thumbnailSize * modifier);
            }

            return itemView.getResources().getDisplayMetrics().widthPixels;
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
//                recyclerCallback.getRequestManager()
//                        .load(thumbnail)
//                        .priority(Priority.HIGH)
//                        .dontAnimate()
//                        .listener(new RequestListener<String, GlideDrawable>() {
//                            @Override
//                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
//                                progressImage.setVisibility(View.GONE);
//                                return false;
//                            }
//
//                            @Override
//                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                                loadBackgroundColor(resource);
//
//                                if (position == getAdapterPosition()) {
//                                    if (UtilsImage.placeImageUrl(link)) {
//                                        recyclerCallback.getRequestManager()
//                                                .load(link.getUrl())
//                                                .priority(Priority.HIGH)
//                                                .dontAnimate()
//                                                .override(size, size)
//                                                .centerCrop()
//                                                .listener(new RequestListenerCompletion<String, GlideDrawable>() {
//                                                    @Override
//                                                    protected void onCompleted() {
//                                                        progressImage.setVisibility(View.GONE);
//                                                    }
//                                                })
//                                                .into(new GlideDrawableImageViewTarget(imageSquare));
//                                        return true;
//                                    } else {
//                                        if (link.getDomain().contains("imgur") && (link
//                                                .getUrl()
//                                                .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
//                                                .getUrl()
//                                                .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
//                                            imagePlay.setImageResource(
//                                                    R.drawable.ic_photo_album_white_48dp);
//                                        } else {
//                                            imagePlay.setImageResource(
//                                                    R.drawable.ic_play_circle_outline_white_48dp);
//                                        }
//
//                                        imagePlay.setColorFilter(colorFilterMenuItem);
//                                        imagePlay.setVisibility(View.VISIBLE);
//                                        progressImage.setVisibility(View.GONE);
//                                    }
//                                }
//
//                                return false;
//                            }
//                        })
//                        .into(new GlideDrawableImageViewTarget(imageSquare));
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
                                                picasso.load(link.getUrl())
                                                        .tag(TAG_PICASSO)
                                                        .resize(size, size)
                                                        .centerCrop()
                                                        .priority(Picasso.Priority.HIGH)
                                                        .into(imageSquare, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }

                                                            @Override
                                                            public void onError() {
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }
                                                        });

                                            } else {
                                                if (link.getDomain().contains("imgur") && (link
                                                        .getUrl()
                                                        .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
                                                        .getUrl()
                                                        .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
                                                    imagePlay.setImageResource(
                                                            R.drawable.ic_photo_album_white_48dp);
                                                } else {
                                                    imagePlay.setImageResource(
                                                            R.drawable.ic_play_circle_outline_white_48dp);
                                                }

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
                Log.d(TAG, "loadThumbnail() called with: " + "url = [" + link.getUrl() + "], title = [" + link.getTitle() + "]");
//                recyclerCallback.getRequestManager()
//                        .load(link.getUrl())
//                        .priority(Priority.HIGH)
//                        .dontAnimate()
//                        .override(size, size)
//                        .centerCrop()
//                        .listener(new RequestListener<String, GlideDrawable>() {
//                            @Override
//                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
//                                imageSquare.setVisibility(View.GONE);
//                                showThumbnail(true);
//                                imageThumbnail.setColorFilter(colorFilterIconDefault);
//                                imageThumbnail.setImageDrawable(drawableDefault);
//                                progressImage.setVisibility(View.GONE);
//                                return false;
//                            }
//
//                            @Override
//                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                                loadBackgroundColor(resource);
//
//                                if (position == getAdapterPosition()) {
//                                    if (link.getDomain().contains("imgur") && (link
//                                            .getUrl()
//                                            .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
//                                            .getUrl()
//                                            .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
//                                        imagePlay.setImageResource(
//                                                R.drawable.ic_photo_album_white_48dp);
//                                        imagePlay.setColorFilter(colorFilterMenuItem);
//                                        imagePlay.setVisibility(View.VISIBLE);
//                                    }
//                                }
//
//                                progressImage.setVisibility(View.GONE);
//                                return false;
//                            }
//                        })
//                        .into(new GlideDrawableImageViewTarget(imageSquare));
                picasso.load(link.getUrl())
                        .tag(TAG_PICASSO)
                        .resize(size, size)
                        .centerCrop()
                        .priority(Picasso.Priority.HIGH)
                        .into(imageSquare,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();

                                        if (position == getAdapterPosition()) {
                                            if (link.getDomain().contains("imgur") && (link
                                                    .getUrl()
                                                    .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
                                                    .getUrl()
                                                    .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
                                                imagePlay.setImageResource(
                                                        R.drawable.ic_photo_album_white_48dp);
                                                imagePlay.setColorFilter(colorFilterMenuItem);
                                                imagePlay.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        progressImage.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onError() {
                                        imageSquare.setVisibility(View.GONE);
                                        showThumbnail(true);
                                        imageThumbnail.setColorFilter(colorFilterIconDefault);
                                        imageThumbnail.setImageDrawable(drawableDefault);
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }
            else {
                imageSquare.setVisibility(View.GONE);
                showThumbnail(true);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                progressImage.setVisibility(View.GONE);
            }
        }

        private void loadSelfPostThumbnail(final Link link) {

            // TODO: Improve thumbnail loading logic

            imageSquare.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.VISIBLE);

            picasso.cancelRequest(imageSquare);
            imageSquare.setImageDrawable(null);

            final int size = getAdjustedThumbnailSize();

            String thumbnail = UtilsImage.parseSourceImage(link);

            if (URLUtil.isNetworkUrl(thumbnail)) {
//                recyclerCallback.getRequestManager()
//                        .load(thumbnail)
//                        .priority(Priority.HIGH)
//                        .override(size, size)
//                        .centerCrop()
//                        .listener(new RequestListenerCompletion<String, GlideDrawable>() {
//                            @Override
//                            public boolean onResourceReady(GlideDrawable glideDrawable, String s, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                                loadBackgroundColor(glideDrawable);
//                                return super.onResourceReady(glideDrawable, s, target, isFromMemoryCache, isFirstResource);
//                            }
//
//                            @Override
//                            protected void onCompleted() {
//                                progressImage.setVisibility(View.GONE);
//                            }
//                        })
//                        .into(new GlideDrawableImageViewTarget(imageSquare));
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
                recyclerCallback.getLayoutManager().requestLayout();
                itemView.invalidate();
                itemView.post(() -> {
//                        recyclerCallback.getRequestManager()
//                                .load(url)
//                                .into(new GlideDrawableImageViewTarget(imageFull));
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
                int viewBackgroundColor = ((ColorDrawable) itemView.getBackground())
                        .getColor();
                if (link.getBackgroundColor() != viewBackgroundColor) {
                    valueAnimatorBackground = UtilsAnimation.animateBackgroundColor(
                            itemView,
                            viewBackgroundColor,
                            color);
                }

                setTextColors(color);
            }
            else {
                color = ColorUtils.setAlphaComponent(color, ALPHA_OVERLAY_IMAGE);
                int overlayBackgroundColor = ((ColorDrawable) viewOverlay.getBackground())
                        .getColor();

                itemView.setBackgroundColor(0x00000000);
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
        public float getRatio() {
            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                float width = itemView.getResources().getDisplayMetrics().widthPixels;

                return ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                        .isFullSpan() ?
                        1f : itemView.getWidth() / width;
            }

            return 1f;
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
        public void onRecycle() {
            super.onRecycle();
            expandFull(false);
            if (valueAnimatorBackground != null) {
                valueAnimatorBackground.cancel();
            }
        }

        @Override
        public void clearOverlay() {
            itemView.setBackgroundColor(link.getBackgroundColor());
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
                frameFull.getLocationOnScreen(location);
                location[1] += frameFull.getHeight();
                if (!imageThumbnail.isShown()) {
                    location[1] -= itemView.getWidth();
                }
            }

            return location;
        }
    }

}