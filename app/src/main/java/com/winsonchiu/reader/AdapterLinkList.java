package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
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
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

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
        viewHolder.imagePreview.setImageBitmap(null);

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 10) {
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
                                viewHolder.imagePreview.setAlpha(0.0f);
                                viewHolder.imagePreview.setImageBitmap(response.getBitmap());
                                controllerLinks.animateAlpha(viewHolder.imagePreview, 0.0f, 1.0f);
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

        String subreddit = "/r/" + link.getSubreddit();
        Spannable spannableInfo = new SpannableString(subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
        spannableInfo.setSpan(new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative), subreddit.length() + 1,
                subreddit.length() + 1 + String.valueOf(link.getScore())
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

        viewHolder.webFull.onPause();
        viewHolder.webFull.resetMaxHeight();
        viewHolder.webFull.loadUrl("about:blank");
        viewHolder.webFull.setVisibility(View.GONE);
        viewHolder.videoFull.stopPlayback();
        viewHolder.videoFull.setVisibility(View.GONE);
        viewHolder.viewPagerFull.setVisibility(View.GONE);
        viewHolder.imagePreview.setImageBitmap(null);
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
        protected WebViewFixed webFull;
        protected VideoView videoFull;
        protected ViewPager viewPagerFull;
        protected ImageView imagePreview;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolder(View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.webFull = (WebViewFixed) itemView.findViewById(R.id.web_full);
            this.webFull.getSettings().setUseWideViewPort(true);
            this.webFull.getSettings().setBuiltInZoomControls(true);
            this.webFull.getSettings().setDisplayZoomControls(false);
            this.webFull.setBackgroundColor(0x000000);
            this.webFull.setInitialScale(0);
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
                        controllerLinks.getListener()
                                .requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
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
            this.viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            this.imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);

            this.imagePreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "imagePreview onTouch");
                    return false;
                }
            });
            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
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
                    }
                    else if (!TextUtils.isEmpty(url)) {
                        if (link.getDomain().contains("imgur")) {
                            int startIndex;
                            int lastIndex;
                            if (url.contains("imgur.com/a/")) {
                                startIndex = url.indexOf("imgur.com/a/") + 12;
                                int slashIndex = url.substring(startIndex)
                                        .indexOf("/") + startIndex;
                                lastIndex = slashIndex > startIndex ? slashIndex : url.length();
                                String imgurId = url.substring(startIndex, lastIndex);
                                loadAlbum(imgurId);
                            }
                            else if (url.contains("imgur.com/gallery/")) {
                                startIndex = url.indexOf("imgur.com/gallery/") + 18;
                                int slashIndex = url.substring(startIndex)
                                        .indexOf("/") + startIndex;
                                lastIndex = slashIndex > startIndex ? slashIndex : url.length();
                                String imgurId = url.substring(startIndex, lastIndex);
                                loadAlbum(imgurId);

                            }
                            else if (url.contains(Reddit.GIFV)) {
                                startIndex = url.indexOf("imgur.com/") + 10;
                                int dotIndex = url.substring(startIndex).indexOf(".") + startIndex;
                                lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                                String imgurId = url.substring(startIndex, lastIndex);
                                loadGifv(imgurId);
                            }
                            else {
                                attemptLoadImage(link);
                            }
                        }
                        else if (link.getDomain().contains("gfycat")) {
                            int startIndex = url.indexOf("gfycat.com/") + 11;
                            int dotIndex = url.substring(startIndex).lastIndexOf(".");
                            int lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                            String gfycatId = url.substring(startIndex, lastIndex);
                            progressImage.setVisibility(View.VISIBLE);
                            controllerLinks.getReddit().loadGet(Reddit.GFYCAT_URL + gfycatId,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response).getJSONObject(Reddit.GFYCAT_ITEM);
                                                loadVideo(jsonObject.getString(Reddit.GFYCAT_WEBM), (float) jsonObject.getInt("height") / jsonObject.getInt("width"));
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
                            attemptLoadImage(link);
                        }
                        controllerLinks.getListener()
                                .onFullLoaded(getPosition());
                    }
                }
            });
            this.buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.getListener()
                            .onClickComments(controllerLinks.getLink(getPosition()));
                }
            });

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.animateExpandActions(layoutContainerActions);
                }
            };
            this.itemView.setOnClickListener(clickListenerLink);
            this.textThreadTitle.setOnClickListener(clickListenerLink);
            this.textThreadInfo.setOnClickListener(clickListenerLink);
        }

        private void attemptLoadImage(Link link) {
            if (Reddit.placeImageUrl(link)) {
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(
                        controllerLinks.getLink(getPosition())
                                .getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
            }
            else {
                controllerLinks.getListener().loadUrl(link.getUrl());
            }
        }

        private void loadAlbum(String id) {
            progressImage.setVisibility(View.VISIBLE);
            imagePreview.setTag(controllerLinks.getReddit()
                    .loadImgurAlbum(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(activity, album,
                                                        controllerLinks.getListener()));
                                        viewPagerFull.getLayoutParams().height = controllerLinks.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
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
                    }, 0));
        }


        private void loadGifv(String id) {
            imagePreview.setTag(controllerLinks.getReddit()
                    .loadImgurImage(id,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Image image = Image.fromJson(
                                        new JSONObject(
                                                response).getJSONObject(
                                                "data"));

                                loadVideo(image.getMp4(), (float) image.getHeight() / image.getWidth());
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
                    }, 0));
        }


        private void loadVideo(String url, float heightRatio) {
            Uri uri = Uri.parse(url);
            videoFull.setVideoURI(uri);
            videoFull.getLayoutParams().height = (int) (ViewHolder.this.itemView.getWidth() * heightRatio);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.invalidate();
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