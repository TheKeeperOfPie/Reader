package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    private int colorPositive;
    private int colorNegative;
    private ControllerLinks controllerLinks;
    private DividerItemDecoration itemDecoration;

    public AdapterLinkList(Activity activity, ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        Resources resources = activity.getResources();
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        this.layoutManager = new LinearLayoutManager(activity);
        this.itemDecoration = new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST);
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return itemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_link, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 5) {
            controllerLinks.loadMoreLinks();
        }

        Link link = controllerLinks.getLink(position);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null) {
            viewHolder.imagePreview.setTag(
                    controllerLinks.loadImage(link.getThumbnail(), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response,
                                               boolean isImmediate) {
                            if (response.getBitmap() != null) {
                                viewHolder.imagePreview.setImageBitmap(response.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }));
        }
        else {
            viewHolder.imagePreview.setImageDrawable(drawable);
        }

        viewHolder.textThreadTitle.setText(link.getTitle());

        Spannable spannableInfo = new SpannableString(link.getScore() + " by " + link.getAuthor());
        spannableInfo.setSpan(new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative), 0,
                String.valueOf(link.getScore())
                        .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        viewHolder.textThreadInfo.setText(spannableInfo);
        viewHolder.layoutContainerActions.setVisibility(View.GONE);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        if (viewHolder.imagePreview.getTag() != null) {
            ((ImageLoader.ImageContainer) viewHolder.imagePreview.getTag()).cancelRequest();
        }

        viewHolder.webFull.setVisibility(View.GONE);
        viewHolder.videoFull.setVisibility(View.GONE);
        viewHolder.imagePreview.setVisibility(View.VISIBLE);
        viewHolder.progressImage.setVisibility(View.GONE);

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected WebView webFull;
        protected VideoView videoFull;
        protected ImageView imagePreview;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolder(View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.webFull = (WebView) itemView.findViewById(R.id.web_full);
            this.webFull.getSettings().setUseWideViewPort(true);
            this.webFull.getSettings().setBuiltInZoomControls(true);
            this.webFull.setBackgroundColor(0x000000);
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
            this.imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);

            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = controllerLinks.getLink(getPosition());
                    String url = link.getUrl();
                    imagePreview.setVisibility(View.VISIBLE);

                    if (link.isSelf()) {
                        String html = link.getSelfTextHtml();
                        html = Html.fromHtml(html.trim())
                                .toString();
                        textThreadTitle.setText(Reddit.formatHtml(html,
                                new Reddit.UrlClickListener() {
                                    @Override
                                    public void onUrlClick(String url) {
                                        controllerLinks.getListener()
                                                .loadUrl(url);
                                    }
                                }));
                        controllerLinks.getListener()
                                .onFullLoaded(getPosition());
                    }
                    else if (!TextUtils.isEmpty(url)) {
                        if (url.contains(Reddit.GIFV)) {
                            Uri uri = Uri.parse(url.replaceAll(".gifv", ".mp4"));
                            videoFull.setVideoURI(uri);
                            videoFull.getLayoutParams().height = ViewHolder.this.itemView.getWidth() / 16 * 9;
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
                        }
                        else if (Reddit.placeFormattedUrl(link)) {
                            webFull.setVisibility(View.VISIBLE);
                            webFull.loadData(Reddit.getImageHtml(
                                    controllerLinks.getLink(getPosition())
                                            .getUrl()), "text/html", "UTF-8");
                            controllerLinks.getListener()
                                    .onFullLoaded(getPosition());
                        }
                        else {
                            controllerLinks.getListener()
                                    .loadUrl(url);
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
            this.textThreadInfo.setOnClickListener(clickListenerLink);
        }

//        private void loadImage(String url) {
//            progressImage.setVisibility(View.VISIBLE);
//            controllerLinks.loadImage(url, new ImageLoader.ImageListener() {
//                @Override
//                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
////                    imageFull.setVisibility(View.VISIBLE);
////                    imageFull.setImageBitmap(response.getBitmap());
//                    imagePreview.setVisibility(View.INVISIBLE);
//                    controllerLinks.getListener()
//                            .onFullLoaded(getPosition());
//                    progressImage.setVisibility(View.GONE);
//                    Log.d(TAG, "loadImage completed");
//                }
//
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    progressImage.setVisibility(View.GONE);
//                    Toast.makeText(activity, "Error loading image",
//                            Toast.LENGTH_SHORT)
//                            .show();
//                }
//            });
//        }

    }

}