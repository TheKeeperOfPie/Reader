/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.animation.Animator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.internal.widget.TintImageView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.utils.AnimationUtils;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.imgur.Album;

import java.util.ArrayList;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();
    private static final int ALPHA_OVERLAY = 140;
    private static final int ALPHA_OVERLAY_IMAGE = 200;
    protected int thumbnailSize;

    public AdapterLinkGrid(Activity activity,
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

        Resources resources = activity.getResources();

        boolean isLandscape = resources.getDisplayMetrics().widthPixels > resources
                .getDisplayMetrics().heightPixels;
        int spanCount = isLandscape ? 3 : 2;
        layoutManager = new StaggeredGridLayoutManager(spanCount,
                StaggeredGridLayoutManager.VERTICAL);
//        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(
//                StaggeredGridLayoutManager.GAP_HANDLING_NONE);

        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        this.thumbnailSize = displayMetrics.widthPixels / 2;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.header_link, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false), eventListenerBase, disallowListener,
                recyclerCallback, thumbnailSize);
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
                viewHolder
                        .onBind(controllerLinks.getLink(position), controllerLinks.showSubreddit(),
                                controllerUser.getUser().getName());
                break;
        }
    }

    public static class ViewHolder extends AdapterLink.ViewHolderBase {

        private final int thumbnailSize;
        protected ImageView imageFull;
        private int colorBackgroundDefault;

        public ViewHolder(View itemView,
                EventListener eventListener,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                int thumbnailSize) {
            super(itemView, eventListener, disallowListener, recyclerCallback);
            this.thumbnailSize = thumbnailSize;

        }

        @Override
        protected void initialize() {
            super.initialize();
            imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            if (itemView.getBackground() instanceof ColorDrawable) {
                colorBackgroundDefault = ((ColorDrawable) itemView.getBackground()).getColor();
            }
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
                        imageFull.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    loadComments();
                }
            });
            imageFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    imageFull.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    loadFull();
                }
            });
        }

        @Override
        public void onBind(Link link, boolean showSubbreddit, String userName) {

            super.onBind(link, showSubbreddit, userName);

            int position = getAdapterPosition();

            if (viewOverlay.getVisibility() == View.GONE) {
                itemView.setBackgroundColor(colorBackgroundDefault);
                viewOverlay.setBackgroundColor(0x00000000);
            }
            else {
                itemView.setBackgroundColor(0x00000000);
                viewOverlay.setBackgroundColor(ColorUtils.setAlphaComponent(colorBackgroundDefault, ALPHA_OVERLAY));
            }

            link.setBackgroundColor(colorBackgroundDefault);
            buttonComments.setColorFilter(colorFilterIconDefault);
            imagePlay.setColorFilter(colorFilterIconDefault);
            textThreadInfo.setTextColor(colorTextSecondaryDefault);
            textHidden.setTextColor(colorTextSecondaryDefault);

            imagePlay.setVisibility(View.GONE);

            Drawable drawable = Reddit.getDrawableForLink(itemView.getContext(), link);
            if (drawable != null) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawable);
                imageThumbnail.setVisibility(View.VISIBLE);
            }
            else if (!preferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) ||
                    (link.isOver18() && !preferences
                            .getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true))) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                imageThumbnail.setVisibility(View.VISIBLE);
            }
            else if (Reddit.showThumbnail(link)) {
                loadThumbnail(link, position);
                return;
            }
            else if (!TextUtils.isEmpty(link.getThumbnail()) && !Reddit.NSFW.equals(link.getThumbnail())) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.clearColorFilter();
                imageThumbnail.setVisibility(View.VISIBLE);
                Reddit.loadPicasso(itemView.getContext())
                        .load(link.getThumbnail())
                        .tag(TAG_PICASSO)
                        .into(imageThumbnail);
            }
            else {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setColorFilter(colorFilterIconDefault);
                imageThumbnail.setImageDrawable(drawableDefault);
                imageThumbnail.setVisibility(View.VISIBLE);
            }

            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                    RelativeLayout.START_OF);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(
                    titleMargin);
        }

        @Override
        public void expandFull(boolean expand) {
            super.expandFull(expand);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                        .setFullSpan(expand);
                if (expand) {
                    if (recyclerCallback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager) recyclerCallback.getLayoutManager())
                                .invalidateSpanAssignments();
                    }
                    recyclerCallback.scrollTo(getAdapterPosition());
                }
            }
        }

        @Override
        protected void expandToolbarActions() {
            super.expandToolbarActions();
            setOverflowColorFilter();
        }

        private int getAdjustedThumbnailSize() {
            float modifier = Float.parseFloat(
                    preferences.getString(AppSettings.PREF_GRID_THUMBNAIL_SIZE, "0.75"));
            if (modifier > 0) {
                return (int) (thumbnailSize * modifier);
            }

            return itemView.getResources().getDisplayMetrics().widthPixels;
        }

        private void loadThumbnail(final Link link, final int position) {

            // TODO: Improve thumbnail loading logic

            imageFull.setVisibility(View.VISIBLE);
            imageThumbnail.setVisibility(View.GONE);
            progressImage.setVisibility(View.VISIBLE);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                    RelativeLayout.START_OF, buttonComments.getId());
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(0);

            Reddit.loadPicasso(itemView.getContext())
                    .load(android.R.color.transparent)
                    .into(imageFull);

            if (TextUtils.isEmpty(link.getThumbnail()) || Reddit.NSFW.equals(link.getThumbnail())) {
                if (Reddit.placeImageUrl(
                        link) && position == getAdapterPosition()) {
                    int size = getAdjustedThumbnailSize();

                    Reddit.loadPicasso(itemView.getContext())
                            .load(link.getUrl())
                            .tag(TAG_PICASSO)
                            .resize(size, size)
                            .centerCrop()
                            .into(imageFull, new Callback() {
                                @Override
                                public void onSuccess() {
                                    loadBackgroundColor();
                                    progressImage.setVisibility(
                                            View.GONE);
                                }

                                @Override
                                public void onError() {

                                }
                            });

                }
                else {
                    imageFull.setVisibility(View.GONE);
                    imageThumbnail.setVisibility(View.VISIBLE);
                    imageThumbnail.setColorFilter(colorFilterIconDefault);
                    imageThumbnail.setImageDrawable(drawableDefault);
                    progressImage.setVisibility(View.GONE);

                    ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                            RelativeLayout.START_OF);
                    ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams())
                            .setMarginEnd(titleMargin);
                }
            }
            else {
                Reddit.loadPicasso(itemView.getContext())
                        .load(link.getThumbnail())
                        .tag(TAG_PICASSO)
                        .into(imageFull,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();

                                        if (position == getAdapterPosition()) {
                                            if (Reddit.placeImageUrl(link)) {
                                                int size = getAdjustedThumbnailSize();
                                                Reddit.loadPicasso(itemView.getContext())
                                                        .load(link.getUrl())
                                                        .tag(TAG_PICASSO)
                                                        .resize(size, size)
                                                        .centerCrop()
                                                        .into(imageFull, new Callback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }

                                                            @Override
                                                            public void onError() {

                                                            }
                                                        });

                                            }
                                            else {
                                                if (link.getDomain().contains("imgur") && (link
                                                        .getUrl()
                                                        .contains(Reddit.IMGUR_PREFIX_ALBUM) || link
                                                        .getUrl()
                                                        .contains(Reddit.IMGUR_PREFIX_GALLERY))) {
                                                    imagePlay.setImageResource(
                                                            R.drawable.ic_photo_album_white_48dp);
                                                }
                                                else {
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

        }

        public void loadBackgroundColor() {
            if (link.getBackgroundColor() != colorBackgroundDefault) {
                setBackgroundColor(link.getBackgroundColor());
                return;
            }

            Drawable drawable = imageFull.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                final int position = getAdapterPosition();
                Palette.from(((BitmapDrawable) drawable).getBitmap())
                        .generate(
                                new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        if (position == getAdapterPosition()) {
                                            // Fix desync of background colors

                                            setBackgroundColor(palette.getDarkVibrantColor(
                                                    palette.getMutedColor(colorBackgroundDefault)));

                                        }
                                    }
                                });
            }
        }

        public void setBackgroundColor(int color) {

            link.setBackgroundColor(color);

            if (viewOverlay.getVisibility() == View.GONE) {
                AnimationUtils.animateBackgroundColor(
                        itemView,
                        ((ColorDrawable) itemView.getBackground())
                                .getColor(), color);

                setTextColors(color);
            }
            else {

                color = ColorUtils.setAlphaComponent(color, ALPHA_OVERLAY_IMAGE);

                itemView.setBackgroundColor(0x00000000);
                AnimationUtils.animateBackgroundColor(
                        viewOverlay,
                        ((ColorDrawable) viewOverlay.getBackground())
                                .getColor(), color);

                titleTextColor = colorTextPrimaryDefault;
                colorTextSecondary = colorTextSecondaryDefault;
                syncTitleColor();
            }

        }

        public double calculateLuminance(int color) {
            return Math.sqrt(0.299f * Math.pow(Color.red(color) / 255f, 2) + 0.587f * Math.pow(Color.green(color) / 255f, 2) + 0.114f * Math.pow(Color.blue(color) / 255f, 2));
        }

        public void setTextColors(int color) {

            Menu menu = toolbarActions.getMenu();

            double luminance = calculateLuminance(color);

            if (luminance < 0.5) {
                buttonComments.setColorFilter(colorFilterIconLight);
                imagePlay.setColorFilter(colorFilterIconLight);
                textThreadInfo.setTextColor(resources.getColor(R.color.darkThemeTextColorMuted));
                textHidden.setTextColor(resources.getColor(R.color.darkThemeTextColorMuted));
                colorTextSecondary = resources.getColor(R.color.darkThemeTextColorMuted);
                titleTextColorAlert = resources.getColor(R.color.textColorAlert);
                titleTextColor = resources.getColor(R.color.darkThemeTextColor);
                colorFilterMenuItem = colorFilterIconLight;

            }
            else {
                buttonComments.setColorFilter(colorFilterIconDark);
                imagePlay.setColorFilter(colorFilterIconDark);
                textThreadInfo.setTextColor(resources.getColor(R.color.lightThemeTextColorMuted));
                textHidden.setTextColor(resources.getColor(R.color.lightThemeTextColorMuted));
                colorTextSecondary = resources.getColor(R.color.lightThemeTextColorMuted);
                titleTextColorAlert = resources.getColor(R.color.textColorAlertMuted);
                titleTextColor = resources.getColor(R.color.lightThemeTextColor);
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
                TintImageView imageOverflow = (TintImageView) views.get(0);
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

            textThreadInfo.setText(TextUtils
                    .concat(getSubredditString(), showSubreddit ? "\n" : "", getSpannableScore(),
                            "by ", link.getAuthor(), getFlairString()));

            Linkify.addLinks(textThreadInfo, Linkify.WEB_URLS);

            textHidden.setText(getTimestamp() + ", " + link.getNumComments() + " comments");

        }

        @Override
        public void setAlbum(Link link, Album album) {
            super.setAlbum(link, album);
            imageThumbnail.setVisibility(View.GONE);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                    RelativeLayout.START_OF,
                    buttonComments.getId());
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(
                    0);
        }

        @Override
        public void onRecycle() {
            super.onRecycle();
            expandFull(false);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                    RelativeLayout.START_OF);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams())
                    .setMarginEnd(titleMargin);
        }

        @Override
        public void clearOverlay() {
            if (viewOverlay.getBackground() instanceof ColorDrawable) {
                itemView.setBackgroundColor(link.getBackgroundColor());
                setTextColors(link.getBackgroundColor());
            }
            viewOverlay.setVisibility(View.GONE);
        }
    }

}