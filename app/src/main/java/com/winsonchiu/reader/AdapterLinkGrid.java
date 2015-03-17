package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private DividerItemDecoration itemDecoration;
    private int defaultColor;
    private int deviceWidth;
    private int[] firstPositions;
    private int[] lastPositions;

    public AdapterLinkGrid(Activity activity, ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        this.deviceWidth = activity.getResources().getDisplayMetrics().widthPixels;
        this.layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        this.firstPositions = new int[2];
        this.lastPositions = new int[2];
        this.itemDecoration = null;
        this.defaultColor = activity.getResources().getColor(R.color.darkThemeDialog);
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

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 5) {
            controllerLinks.loadMoreLinks();
        }

        final Link link = controllerLinks.getLink(position);
        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(false);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null && Reddit.placeFormattedUrl(link)) {
            viewHolder.imagePreview.setVisibility(View.VISIBLE);
            viewHolder.progressImage.setVisibility(View.VISIBLE);
            controllerLinks.loadImage(link.getThumbnail(), new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (response.getBitmap() == null) {
                        this.onErrorResponse(null);
                        return;
                    }
                    Palette palette = Palette.generate(response.getBitmap());
                    viewHolder.itemView.setBackgroundColor(
                            palette.getVibrantColor(palette.getDarkVibrantColor(defaultColor)));
                    viewHolder.imagePreview.setTag(controllerLinks.loadImage(link.getUrl(),
                            new ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(ImageLoader.ImageContainer response,
                                                       boolean isImmediate) {
                                    if (response.getBitmap() != null) {
                                        viewHolder.imagePreview.setImageBitmap(
                                                ThumbnailUtils.extractThumbnail(
                                                        response.getBitmap(),
                                                        viewHolder.imagePreview.getWidth(),
                                                        viewHolder.imagePreview.getWidth()));
                                        viewHolder.progressImage.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onErrorResponse(VolleyError error) {

                                }
                            }));
                }

                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
        }
        else {
            viewHolder.imagePreview.setVisibility(View.GONE);
        }
        viewHolder.imagePreview.invalidate();

        viewHolder.textThreadTitle.setText(link.getTitle());
        viewHolder.layoutContainerActions.setVisibility(View.GONE);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        if (viewHolder.imagePreview.getTag() != null) {
            ((ImageLoader.ImageContainer) viewHolder.imagePreview.getTag()).cancelRequest();
        }

        viewHolder.webFull.setVisibility(View.GONE);
        viewHolder.imagePreview.setVisibility(View.GONE);
        viewHolder.imagePreview.setImageBitmap(null);
        viewHolder.itemView.setBackgroundColor(defaultColor);
        viewHolder.progressImage.setVisibility(View.GONE);

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        protected ProgressBar progressImage;
        protected WebView webFull;
        protected ImageView imagePreview;
        protected TextView textThreadTitle;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolder(final View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.webFull = (WebView) itemView.findViewById(R.id.web_full);
            this.webFull.getSettings().setUseWideViewPort(true);
            this.webFull.getSettings().setBuiltInZoomControls(true);
            this.webFull.setBackgroundColor(0x000000);
            this.imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);

            // TODO: Change to load full size image inside of Fragment view without loading comments
            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder viewHolder = ViewHolder.this;
                    StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
                    layoutParams.setFullSpan(true);
                    viewHolder.itemView.setLayoutParams(layoutParams);

                    imagePreview.setVisibility(View.GONE);
                    webFull.setVisibility(View.VISIBLE);
                    webFull.loadData(Reddit.getImageHtml(controllerLinks.getLink(getPosition()).getUrl()), "text/html", "UTF-8");
                    controllerLinks.getListener().onFullLoaded(getPosition());
                }
            });

            this.buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.getListener().onClickComments(controllerLinks.getLink(getPosition()));
                }
            });

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layoutContainerActions.setVisibility(
                            layoutContainerActions.getVisibility() == View.VISIBLE ? View.GONE :
                                    View.VISIBLE);
                    Link link = controllerLinks.getLink(getPosition());
                    Toast.makeText(activity, "isNsfw: " + link.isOver18() + " URL: " + link.getUrl() + ": " + Reddit.placeFormattedUrl(link), Toast.LENGTH_SHORT).show();
                    Ion.with(activity)
                            .load(controllerLinks.getLink(getPosition()).getThumbnail())
                            .asBitmap()
                            .setCallback(new FutureCallback<Bitmap>() {
                                @Override
                                public void onCompleted(Exception e, Bitmap result) {
                                    imagePreview.setMaxHeight(imagePreview.getWidth());
                                    imagePreview.setImageBitmap(result);
                                }
                            });
                }
            };
            this.itemView.setOnClickListener(clickListenerLink);
            this.textThreadTitle.setOnClickListener(clickListenerLink);
        }

    }

}