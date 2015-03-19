package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private DividerItemDecoration itemDecoration;
    private int defaultColor;
    private int thumbnailWidth;

    public AdapterLinkGrid(Activity activity, ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        this.thumbnailWidth = activity.getResources().getDisplayMetrics().widthPixels / 2;
        this.layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
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

        if (viewHolder.imagePreview.getTag() != null) {
            ((ImageLoader.ImageContainer) viewHolder.imagePreview.getTag()).cancelRequest();
        }

        final Link link = controllerLinks.getLink(position);
        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(false);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null && showThumbnail(link)) {
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
                    if (Reddit.placeImageUrl(link)) {
                        viewHolder.imagePreview.setTag(controllerLinks.loadImage(link.getUrl(),
                                new ImageLoader.ImageListener() {
                                    @Override
                                    public void onResponse(ImageLoader.ImageContainer response,
                                                           boolean isImmediate) {
                                        if (response.getBitmap() != null) {
                                            viewHolder.imagePreview.setImageBitmap(
                                                    ThumbnailUtils.extractThumbnail(
                                                            response.getBitmap(),
                                                            thumbnailWidth,
                                                            thumbnailWidth));
                                            viewHolder.progressImage.setVisibility(View.GONE);
                                        }
                                    }

                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                }));
                    }
                    else {
                        viewHolder.imagePreview.setImageBitmap(response.getBitmap());
                        viewHolder.imagePlay.setVisibility(View.VISIBLE);
                        viewHolder.progressImage.setVisibility(View.GONE);
                    }
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

    private boolean showThumbnail(Link link) {
        String domain = link.getDomain();
        return domain.contains("gfycat") || domain.contains("imgur") || Reddit.placeImageUrl(link);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        if (viewHolder.imagePreview.getTag() != null) {
            ((ImageLoader.ImageContainer) viewHolder.imagePreview.getTag()).cancelRequest();
        }

        viewHolder.imagePlay.setVisibility(View.GONE);
        viewHolder.webFull.onPause();
        viewHolder.webFull.resetMaxHeight();
        viewHolder.webFull.loadUrl("about:blank");
        viewHolder.webFull.setVisibility(View.GONE);
        viewHolder.videoFull.setVisibility(View.GONE);
        viewHolder.imagePreview.setImageBitmap(null);
        viewHolder.imagePreview.setVisibility(View.GONE);
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
        protected ImageView imagePlay;
        protected MediaController mediaController;
        protected VideoView videoFull;
        protected WebViewFixed webFull;
        protected ImageView imagePreview;
        protected TextView textThreadTitle;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolder(final View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.imagePlay = (ImageView) itemView.findViewById(R.id.image_play);
            this.mediaController = new MediaController(itemView.getContext());
            this.videoFull = (VideoView) itemView.findViewById(R.id.video_full);
            this.videoFull.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaController.hide();
                }
            });
            this.mediaController.setAnchorView(videoFull);
            this.videoFull.setMediaController(mediaController);
            this.webFull = (WebViewFixed) itemView.findViewById(R.id.web_full);
            this.webFull.getSettings()
                    .setUseWideViewPort(true);
            this.webFull.getSettings().setBuiltInZoomControls(true);
            this.webFull.getSettings().setDisplayZoomControls(false);
            this.webFull.setBackgroundColor(0x000000);
            this.webFull.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    webFull.lockHeight();
                    super.onScaleChanged(view, oldScale, newScale);
                }
            });
            this.webFull.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        if ((webFull.canScrollVertically(1) && webFull.canScrollVertically(-1))) {
                            controllerLinks.getListener()
                                    .requestDisallowInterceptTouchEvent(true);
                        }
                        else {
                            controllerLinks.getListener()
                                    .requestDisallowInterceptTouchEvent(false);
                            if (webFull.getScrollY() == 0) {
                                webFull.setScrollY(1);
                            }
                            else {
                                webFull.setScrollY(webFull.getScrollY() - 1);
                            }
                        }
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        controllerLinks.getListener().requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
            this.imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);

            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder viewHolder = ViewHolder.this;
                    StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
                    layoutParams.setFullSpan(true);
                    viewHolder.itemView.setLayoutParams(layoutParams);

                    imagePreview.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    Link link = controllerLinks.getLink(getPosition());

                    String url = link.getUrl();
                    if (!TextUtils.isEmpty(url)) {
                        if (url.contains(Reddit.GIFV)) {
                            loadVideo(url.replaceAll(".gifv", ".mp4"), 16f / 9f);
                        }
                        else if (link.getDomain()
                                .contains("gfycat")) {
                            int startIndex = url.indexOf("gfycat.com") + 10;
                            int lastIndex =
                                    url.lastIndexOf(".") > startIndex ? url.lastIndexOf(".") :
                                            url.length();
                            String gfycatId = url.substring(url.indexOf("gfycat.com/") + 11,
                                    lastIndex);
                            Log.d(TAG, "url length: " + url.length());
                            progressImage.setVisibility(View.VISIBLE);
                            controllerLinks.getReddit()
                                    .loadGet(Reddit.GFYCAT_URL + gfycatId,
                                            new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    try {
                                                        JSONObject jsonObject = new JSONObject(
                                                                response).getJSONObject(
                                                                Reddit.GFYCAT_ITEM);
                                                        loadVideo(jsonObject.getString(
                                                                        Reddit.GFYCAT_WEBM),
                                                                (float) jsonObject.getInt(
                                                                        "height") / jsonObject.getInt(
                                                                        "width"));
                                                    }
                                                    catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    finally {
                                                        progressImage.setVisibility(View.GONE);
                                                    }
                                                }
                                            }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    progressImage.setVisibility(View.GONE);
                                                }
                                            }, 0);
                        }
                        else {
                            webFull.resetMaxHeight();
                            webFull.onResume();
                            webFull.loadData(Reddit.getImageHtml(link.getUrl()), "text/html", "UTF-8");
                            webFull.setVisibility(View.VISIBLE);
                            controllerLinks.getListener()
                                    .onFullLoaded(getPosition());
                        }
                    }
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
                }
            };
            this.itemView.setOnClickListener(clickListenerLink);
            this.textThreadTitle.setOnClickListener(clickListenerLink);
        }

        private void loadVideo(String url, float heightRatio) {
            Uri uri = Uri.parse(url);
            videoFull.setVideoURI(uri);
            videoFull.getLayoutParams().height = (int) (ViewHolder.this.itemView.getWidth() * heightRatio);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.invalidate();
            videoFull.requestFocus();
            videoFull.start();
            videoFull.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoFull.start();
                        }
                    });
            controllerLinks.getListener()
                    .onFullLoaded(getPosition());
        }

    }

}