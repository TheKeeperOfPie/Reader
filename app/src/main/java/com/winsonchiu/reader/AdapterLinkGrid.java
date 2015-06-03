package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

import java.util.Date;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private DividerItemDecoration itemDecoration;
    private int defaultColor;
    private int thumbnailSize;
    private SharedPreferences preferences;

    public AdapterLinkGrid(Activity activity,
            ControllerLinksBase controllerLinks,
            ControllerLinks.LinkClickListener listener) {
        setControllerLinks(controllerLinks, listener);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.thumbnailSize = resources.getDisplayMetrics().widthPixels / 2;
        boolean isLandscape = resources.getDisplayMetrics().widthPixels > resources.getDisplayMetrics().heightPixels;

        this.layoutManager = new StaggeredGridLayoutManager(isLandscape ? 3 : 2,
                StaggeredGridLayoutManager.VERTICAL);
        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(
                StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        this.itemDecoration = null;
        this.defaultColor = resources.getColor(R.color.darkThemeDialog);
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return itemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false), this, defaultColor, thumbnailSize);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        if (!controllerLinks.isLoading() && position > controllerLinks.sizeLinks() - 10) {
            controllerLinks.loadMoreLinks();
        }

        final ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.onBind(position);
    }

    public static boolean showThumbnail(Link link) {
        if (link.getThumbnail()
                .equals("nsfw")) {
            return false;
        }
        String domain = link.getDomain();
        return domain.contains("gfycat") || domain.contains("imgur") || Reddit.placeImageUrl(link);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        viewHolder.itemView.setBackgroundColor(defaultColor);
        viewHolder.imagePlay.setVisibility(View.GONE);
        viewHolder.onRecycle();

        super.onViewRecycled(holder);
    }

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return listener;
    }

    @Override
    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public ControllerCommentsBase getControllerComments() {
        return listener.getControllerComments();
    }

    protected static class ViewHolder extends AdapterLink.ViewHolderBase {

        private final int defaultColor;
        private final int thumbnailSize;
        protected ImageView imageFull;

        public ViewHolder(View itemView,
                ControllerLinks.ListenerCallback listenerCallback,
                int defaultColor,
                int thumbnailSize) {
            super(itemView, listenerCallback);
            this.defaultColor = defaultColor;
            this.thumbnailSize = thumbnailSize;

            this.imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            this.imageFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ViewHolder viewHolder = ViewHolder.this;

                    if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                                true);
                        ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                    }

                    imageFull.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    loadFull(callback.getController()
                            .getLink(getAdapterPosition()));

                    viewHolder.itemView.post(new Runnable() {
                        @Override
                        public void run() {
                            setToolbarMenuVisibility();
                        }
                    });
                }
            });

            this.videoFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoFull.isPlaying()) {
                        videoFull.stopPlayback();
                        videoFull.setVisibility(View.GONE);
                        imageFull.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    callback.getListener()
                            .onClickComments(
                                    callback.getController()
                                            .getLink(getAdapterPosition()), ViewHolder.this);
                }
            });
        }

        @Override
        public void onBind(final int position) {
            super.onBind(position);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        false);
            }

            itemView.setBackgroundColor(defaultColor);
            imagePlay.setVisibility(View.GONE);

            final Link link = callback.getController()
                    .getLink(position);

            Log.d(TAG, "onBind: " + link.getUrl());
            Log.d(TAG, "thumbnail: " + link.getThumbnail());

            Drawable drawable = callback.getController()
                    .getDrawableForLink(link);
            if (drawable != null) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                imageThumbnail.setImageDrawable(drawable);
            }
            else if (showThumbnail(link)) {
                Log.d(TAG, "showThumbnail true: " + link.getUrl());
                loadThumbnail(link, position);
            }
            else {
                Log.d(TAG, "showThumbnail false: " + link.getUrl());
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                Picasso.with(callback.getActivity())
                        .load(link.getThumbnail())
                        .into(imageThumbnail);
            }

            setTextInfo(link);
        }

        private void loadThumbnail(final Link link, final int position) {

            imageFull.setVisibility(View.VISIBLE);
            imageThumbnail.setVisibility(View.GONE);
            progressImage.setVisibility(View.VISIBLE);

            Picasso.with(callback.getActivity())
                    .load(link.getThumbnail())
                    .into(imageFull,
                            new Callback() {
                                @Override
                                public void onSuccess() {
                                    Drawable drawable = imageFull.getDrawable();
                                    if (drawable instanceof BitmapDrawable) {
                                        Palette.from(((BitmapDrawable) drawable).getBitmap())
                                                .generate(
                                                        new Palette.PaletteAsyncListener() {
                                                            @Override
                                                            public void onGenerated(Palette palette) {
                                                                if (position == getAdapterPosition()) {
                                                                    AnimationUtils.animateBackgroundColor(
                                                                            itemView,
                                                                            ((ColorDrawable) itemView.getBackground()).getColor(),
                                                                            palette.getDarkVibrantColor(
                                                                                    palette.getMutedColor(
                                                                                            defaultColor)));
                                                                }
                                                            }
                                                        });
                                    }

                                    imageUrl = link.getThumbnail();
                                    if (Reddit.placeImageUrl(
                                            link) && position == getAdapterPosition()) {
                                        Picasso.with(callback.getActivity())
                                                .load(link.getUrl())
                                                .resize(thumbnailSize, thumbnailSize)
                                                .centerCrop()
//                                                .noPlaceholder()
//                                                .noFade()
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
                                        imagePlay.setVisibility(View.VISIBLE);
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onError() {

                                }
                            });

        }

        // TODO: Fix expanding reply when cell is not full span
        @Override
        public void toggleReply() {
            ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                    true);
            itemView.requestLayout();
            callback.getListener()
                    .onFullLoaded(getAdapterPosition());
            super.toggleReply();
            itemView.post(new Runnable() {
                @Override
                public void run() {
                    setToolbarMenuVisibility();
                }
            });
        }

        @Override
        public float getRatio(int adapterPosition) {
            return ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).isFullSpan() ?
                    1f :
                    1f / ((StaggeredGridLayoutManager) callback.getLayoutManager()).getSpanCount();
        }

        @Override
        public void setTextInfo(Link link) {
            super.setTextInfo(link);

            if (!TextUtils.isEmpty(link.getLinkFlairText())) {
                textThreadFlair.setVisibility(View.VISIBLE);
                textThreadFlair.setText(link.getLinkFlairText());
            }
            else {
                textThreadFlair.setVisibility(View.GONE);
            }

            textThreadTitle.setText(Html.fromHtml(link.getTitle())
                    .toString());
            textThreadTitle.setTextColor(
                    link.isOver18() ? callback.getColorTextAlert() : callback.getColorText());

            String subreddit = "/r/" + link.getSubreddit();
            int scoreLength = String.valueOf(link.getScore())
                    .length();

            Spannable spannableInfo = new SpannableString(
                    subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
            spannableInfo.setSpan(new ForegroundColorSpan(callback.getColorMuted()), 0,
                    subreddit.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? callback.getColorPositive() :
                            callback.getColorNegative()),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + scoreLength, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(callback.getColorMuted()),
                    subreddit.length() + 1 + scoreLength, spannableInfo.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);

            textHidden.setText(new Date(
                    link.getCreatedUtc()).toString() + ", " + link.getNumComments() + " comments");
        }
    }

}