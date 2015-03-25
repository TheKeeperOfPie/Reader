package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.AnimationUtils;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

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
        this.layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
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
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_link, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        final ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.imagePreview.setImageBitmap(null);

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 10) {
            controllerLinks.loadMoreLinks();
        }

        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(false);

        final Link link = controllerLinks.getLink(position);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null && showThumbnail(link)) {
            viewHolder.imagePreview.setVisibility(View.VISIBLE);
            viewHolder.progressImage.setVisibility(View.VISIBLE);
            viewHolder.imagePreview.setTag(
                    controllerLinks.loadImage(link.getThumbnail(), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response,
                                               boolean isImmediate) {
                            if (response.getBitmap() == null) {
                                this.onErrorResponse(null);
                                return;
                            }
                            Palette.generateAsync(response.getBitmap(),
                                    new Palette.PaletteAsyncListener() {
                                        @Override
                                        public void onGenerated(Palette palette) {
                                            if (position == viewHolder.getPosition()) {
                                                AnimationUtils.animateBackgroundColor(
                                                        viewHolder.itemView,
                                                        ((ColorDrawable) viewHolder.itemView.getBackground()).getColor(),
                                                        palette.getDarkVibrantColor(
                                                                palette.getMutedColor(
                                                                        defaultColor)));
                                            }
                                        }
                                    });
                            if (Reddit.placeImageUrl(
                                    link) && position == viewHolder.getPosition()) {
                                viewHolder.imagePreview.setTag(
                                        controllerLinks.loadImage(link.getUrl(),
                                                new ImageLoader.ImageListener() {
                                                    @Override
                                                    public void onResponse(ImageLoader.ImageContainer response,
                                                                           boolean isImmediate) {
                                                        if (response.getBitmap() != null && position == viewHolder.getPosition()) {
                                                            viewHolder.imagePreview.setAlpha(0.0f);
                                                            viewHolder.imagePreview.setImageBitmap(
                                                                    ThumbnailUtils.extractThumbnail(
                                                                            response.getBitmap(),
                                                                            thumbnailWidth,
                                                                            thumbnailWidth));
                                                            AnimationUtils.animateAlpha(
                                                                    viewHolder.imagePreview, 0.0f,
                                                                    1.0f);
                                                            viewHolder.progressImage.setVisibility(
                                                                    View.GONE);
                                                        }
                                                    }

                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {

                                                    }
                                                }));
                            }
                            else {
                                viewHolder.imagePreview.setAlpha(0.0f);
                                viewHolder.imagePreview.setImageBitmap(response.getBitmap());
                                AnimationUtils.animateAlpha(viewHolder.imagePreview, 0.0f, 1.0f);
                                viewHolder.imagePlay.setVisibility(View.VISIBLE);
                                viewHolder.progressImage.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }));
        }
        else {
            viewHolder.itemView.setBackgroundColor(defaultColor);
            viewHolder.imagePreview.setVisibility(View.GONE);
        }
        viewHolder.imagePreview.invalidate();

        viewHolder.textThreadTitle.setText(link.getTitle());
//        viewHolder.setTextInfo();
        viewHolder.layoutContainerActions.setVisibility(View.GONE);
    }

    private boolean showThumbnail(Link link) {
        String domain = link.getDomain();
        return domain.contains("gfycat") || domain.contains("imgur") || Reddit.placeImageUrl(link);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        Object tag = viewHolder.imagePreview.getTag();

        if (tag != null) {
            if (tag instanceof ImageLoader.ImageContainer) {
                ((ImageLoader.ImageContainer) tag).cancelRequest();
            }
            else if (tag instanceof Request) {
                ((Request) tag).cancel();
            }
        }

        viewHolder.itemView.setBackgroundColor(defaultColor);
        viewHolder.imagePlay.setVisibility(View.GONE);
        viewHolder.webFull.onPause();
        viewHolder.webFull.resetMaxHeight();
        viewHolder.webFull.loadData("", "html", "UTF-8");
        viewHolder.webFull.setVisibility(View.GONE);
        viewHolder.videoFull.stopPlayback();
        viewHolder.videoFull.setVisibility(View.GONE);
        viewHolder.viewPagerFull.setVisibility(View.GONE);
        viewHolder.imagePreview.setImageBitmap(null);
        viewHolder.imagePreview.setVisibility(View.GONE);
        viewHolder.progressImage.setVisibility(View.GONE);

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    protected class ViewHolder extends AdapterLink.ViewHolderBase {

        public ViewHolder(View itemView) {
            super(itemView);

            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnimationUtils.animateExpandActions(layoutContainerActions, false);
                }
            };
            textThreadTitle.setOnClickListener(clickListenerLink);
            textThreadInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ViewHolder viewHolder = ViewHolder.this;
                    StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
                    layoutParams.setFullSpan(true);
                    viewHolder.itemView.setLayoutParams(layoutParams);
                    viewHolder.itemView.requestLayout();

                    imagePreview.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    loadFull(controllerLinks.getLink(getPosition()));
                }
            });

        }
    }

}