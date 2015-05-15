package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink implements ControllerLinks.ListenerCallback {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private DividerItemDecoration itemDecoration;
    private int defaultColor;
    private int thumbnailWidth;

    public AdapterLinkGrid(Activity activity, ControllerLinks controllerLinks, ControllerLinks.LinkClickListener listener) {
        setControllerLinks(controllerLinks, listener);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        Resources resources = activity.getResources();
        this.thumbnailWidth = resources.getDisplayMetrics().widthPixels / 2;
        boolean isLandscape = resources.getDisplayMetrics().widthPixels > resources.getDisplayMetrics().heightPixels;

        this.layoutManager = new StaggeredGridLayoutManager(isLandscape ? 3 : 2, StaggeredGridLayoutManager.VERTICAL);
        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        this.itemDecoration = null;
        this.defaultColor = resources.getColor(R.color.darkThemeDialog);
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return itemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_link, viewGroup, false), this, defaultColor, thumbnailWidth);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 10) {
            controllerLinks.loadMoreLinks();
        }

        final ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.onBind(position);
    }

    public static boolean showThumbnail(Link link) {
        if (link.getThumbnail().equals("nsfw")) {
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
    public int getItemCount() {
        return controllerLinks.size();
    }

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return listener;
    }

    @Override
    public ControllerLinks getController() {
        return controllerLinks;
    }

    @Override
    public int getColorPositive() {
        return colorPositive;
    }

    @Override
    public int getColorNegative() {
        return colorNegative;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return layoutManager;
    }

    protected static class ViewHolder extends AdapterLink.ViewHolderBase {

        private final int defaultColor;
        private final int thumbnailWidth;
        protected ImageView imageThumbnail;
        protected String currentThumbnailLink;

        public ViewHolder(View itemView, ControllerLinks.ListenerCallback listenerCallback, int defaultColor, int thumbnailWidth) {
            super(itemView, listenerCallback);
            this.defaultColor = defaultColor;
            this.thumbnailWidth = thumbnailWidth;

            this.imageThumbnail = (ImageView) itemView.findViewById(R.id.image_thumbnail);

            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ViewHolder viewHolder = ViewHolder.this;

                    if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                                true);
                        ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                    }

                    imagePreview.setVisibility(View.GONE);
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
            this.imageThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = callback.getController().getLink(getAdapterPosition());

                    if (link.isSelf()) {
                        if (textThreadSelf.isShown()) {
                            textThreadSelf.setVisibility(View.GONE);
                            return;
                        }
                        if (TextUtils.isEmpty(link.getSelfText())) {
                            callback.getListener().onClickComments(link, ViewHolder.this);
                        }
                        else {
                            ViewHolder viewHolder = ViewHolder.this;
                            if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                                ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                                        true);
                                ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                            }

                            String html = link.getSelfTextHtml();
                            html = Html.fromHtml(html.trim())
                                    .toString();
                            textThreadSelf.setVisibility(View.VISIBLE);
                            textThreadSelf.setText(Reddit.formatHtml(html,
                                    new Reddit.UrlClickListener() {
                                        @Override
                                        public void onUrlClick(String url) {
                                            callback.getListener().loadUrl(url);
                                        }
                                    }));
                        }
                    }
                    else {
                        loadFull(link);
                    }
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

            setTextInfo();
            toolbarActions.setVisibility(View.GONE);
            itemView.setBackgroundColor(defaultColor);

            final Link link = callback.getController().getLink(position);

            Drawable drawable = callback.getController().getDrawableForLink(link);
            if (drawable != null) {
                imagePreview.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                imageThumbnail.setImageDrawable(drawable);
            }
            else if (showThumbnail(link)) {
                imagePreview.setVisibility(View.VISIBLE);
                imageThumbnail.setVisibility(View.GONE);
                progressImage.setVisibility(View.VISIBLE);
                imageContainer = callback.getController()
                        .getReddit().getImageLoader().get(link.getThumbnail(), new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response,
                                                   boolean isImmediate) {
                                imageContainer = response;
                                if (response.getBitmap() == null) {
                                    this.onErrorResponse(null);
                                    return;
                                }
                                currentThumbnailLink = link.getThumbnail();
                                try {
                                    Palette.from(response.getBitmap()).generate(
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
                                catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                }
//                                DataUtils.setNewImageBitmap(imagePreview, response.getBitmap());
                                CustomApplication.getRefWatcher(callback.getActivity()).watch(response.getBitmap());
                                imageUrl = link.getThumbnail();
                                imagePreview.setImageBitmap(response.getBitmap());
                                if (Reddit.placeImageUrl(
                                        link) && position == getAdapterPosition()) {
                                    imageContainer = callback.getController()
                                            .getReddit().getImageLoader().get(link.getUrl(),
                                                    new ImageLoader.ImageListener() {
                                                        @Override
                                                        public void onResponse(
                                                                ImageLoader.ImageContainer response,
                                                                boolean isImmediate) {
                                                            imageContainer = response;
                                                            if (response.getBitmap() != null && position == getAdapterPosition()) {
                                                                imagePreview.setAlpha(
                                                                        0.0f);
//                                                                DataUtils.setNewImageBitmap(imagePreview,
//                                                                        ThumbnailUtils.extractThumbnail(
//                                                                                response.getBitmap(),
//                                                                                thumbnailWidth,
//                                                                                thumbnailWidth));
                                                                CustomApplication.getRefWatcher(callback.getActivity()).watch(response.getBitmap());
                                                                imageUrl = link.getUrl();
                                                                imagePreview.setImageBitmap(ThumbnailUtils.extractThumbnail(
                                                                                response.getBitmap(),
                                                                                thumbnailWidth,
                                                                                thumbnailWidth));
                                                                AnimationUtils.animateAlpha(
                                                                        imagePreview,
                                                                        0.0f,
                                                                        1.0f);
                                                                progressImage.setVisibility(
                                                                        View.GONE);
                                                            }
                                                        }

                                                        @Override
                                                        public void onErrorResponse(
                                                                VolleyError error) {

                                                        }
                                                    });
                                }
                                else {
                                    imagePreview.setAlpha(0.0f);
//                                    DataUtils.setNewImageBitmap(imagePreview, response.getBitmap());
                                    CustomApplication.getRefWatcher(callback.getActivity()).watch(response.getBitmap());
                                    imageUrl = link.getThumbnail();
                                    imagePreview.setImageBitmap(response.getBitmap());
                                    AnimationUtils.animateAlpha(imagePreview, 0.0f, 1.0f);
                                    imagePlay.setVisibility(View.VISIBLE);
                                    progressImage.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
            }
            else {
                imagePreview.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                imageContainer = callback.getController()
                        .getReddit()
                        .getImageLoader()
                        .get(link.getThumbnail(), new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response,
                                                   boolean isImmediate) {
                                imageContainer = response;
                                if (response.getBitmap() == null) {
                                    this.onErrorResponse(null);
                                    return;
                                }
                                if (position == getAdapterPosition()) {
                                    imageUrl = link.getThumbnail();
                                    imagePreview.setImageBitmap(response.getBitmap());
                                    CustomApplication.getRefWatcher(callback.getActivity()).watch(response.getBitmap());
                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
            }

            textThreadTitle.setText(link.getTitle().trim());
        }
    }

}