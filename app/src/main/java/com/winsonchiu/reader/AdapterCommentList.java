package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */
public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;

    private Activity activity;
    private ControllerComments controllerComments;
    private int colorPositive;
    private int colorNegative;

    private Future futureImage;

    public AdapterCommentList(Activity activity, ControllerComments controllerComments) {
        // TODO: Add setActivity
        this.activity = activity;
        this.controllerComments = controllerComments;
        Resources resources = activity.getResources();
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_LINK;
        }

        return VIEW_COMMENT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_LINK) {
            return new ViewHolderHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_link, parent, false));
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ViewHolderHeader) {
            ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;

            Link link = controllerComments.getLink();

            viewHolderHeader.progressImage.setVisibility(View.GONE);
            viewHolderHeader.imagePreview.setImageBitmap(null);
            viewHolderHeader.imagePreview.setVisibility(View.VISIBLE);
            String thumbnail = link.getThumbnail();
            Drawable drawable = controllerComments.getDrawableForLink();
            if (drawable != null) {
                viewHolderHeader.imagePreview.setImageDrawable(drawable);
            }
            else {
                Ion.with(viewHolderHeader.imagePreview)
                        .smartSize(true)
                        .load(thumbnail);
            }
            viewHolderHeader.textThreadTitle.setText(link.getTitle());

            Spannable spannableInfo = new SpannableString(link.getScore() + " by " + link.getAuthor());
            spannableInfo.setSpan(new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative), 0,
                    String.valueOf(link.getScore())
                            .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            viewHolderHeader.textThreadInfo.setText(spannableInfo);

        }
        else {

            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;

            Comment comment = controllerComments.getComments().get(position - 1);

            ViewGroup.LayoutParams layoutParams = viewHolderComment.viewIndent.getLayoutParams();
            layoutParams.width = controllerComments.getIndentWidth(comment);
            viewHolderComment.viewIndent.setLayoutParams(layoutParams);

            if (TextUtils.isEmpty(comment.getBodyHtml())) {
                viewHolderComment.textComment.setText(comment.getKind());
            }
            else {
                String html = comment.getBodyHtml();
                html = Html.fromHtml(html.trim())
                        .toString();

                setTextViewHTML(viewHolderComment.textComment, html);
            }

            Spannable spannableInfo = new SpannableString(
                    comment.getScore() + " by " + comment.getAuthor());
            spannableInfo.setSpan(new ForegroundColorSpan(comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                    String.valueOf(comment.getScore())
                            .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            viewHolderComment.textInfo.setText(spannableInfo);
        }

    }

    @Override
    public int getItemCount() {
        return controllerComments.getLink() == null ? 0 : controllerComments.getComments().size() + 1;
    }

    private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                controllerComments.getListener().loadUrl(span.getURL());
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    private void setTextViewHTML(TextView text, String html) {
        CharSequence sequence = Html.fromHtml(html);

        // Trims leading and trailing whitespace
        int start = 0;
        int end = sequence.length();
        while (start < end && Character.isWhitespace(sequence.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
            end--;
        }
        sequence = sequence.subSequence(start, end);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
    }
    public void cancelRequests() {
        if (futureImage != null) {
            futureImage.cancel(true);
        }
    }

    protected class ViewHolderHeader extends RecyclerView.ViewHolder {

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

        public ViewHolderHeader(View itemView) {
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
                            controllerComments.getListener()
                                    .requestDisallowInterceptTouchEvent(true);
                        }
                        else {
                            controllerComments.getListener()
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
                        controllerComments.getListener()
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
            this.buttonComments.setVisibility(View.INVISIBLE);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);
            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = controllerComments.getLink();
                    String url = link.getUrl();
                    imagePreview.setVisibility(View.VISIBLE);

                    if (link.isSelf()) {
                        String html = link.getSelfTextHtml();
                        html = Html.fromHtml(html.trim())
                                .toString();
                        setTextViewHTML(textThreadTitle, html);
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
                            controllerComments.getReddit().loadGet(Reddit.GFYCAT_URL + gfycatId,
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
                    }
                }
            });
        }

        private void attemptLoadImage(Link link) {
            if (Reddit.placeImageUrl(link)) {
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(
                        controllerComments.getLink()
                                .getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
            }
            else {
                controllerComments.getListener().loadUrl(link.getUrl());
            }
        }

        private void loadAlbum(String id) {
            progressImage.setVisibility(View.VISIBLE);
            imagePreview.setTag(controllerComments.getReddit()
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
                                                        controllerComments.getListener()));
                                        viewPagerFull.getLayoutParams().height = controllerComments.getListener()
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
            imagePreview.setTag(controllerComments.getReddit()
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
            videoFull.getLayoutParams().height = (int) (ViewHolderHeader.this.itemView.getWidth() * heightRatio);
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
        }

    }

    protected class ViewHolderComment extends RecyclerView.ViewHolder {

        protected View viewIndent;
        protected View viewIndicator;
        protected TextView textComment;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolderComment(View itemView) {
            super(itemView);

            this.viewIndent = itemView.findViewById(R.id.view_indent);
            this.viewIndicator = itemView.findViewById(R.id.view_indicator);
            this.textComment = (TextView) itemView.findViewById(R.id.text_comment);
            this.textComment.setMovementMethod(LinkMovementMethod.getInstance());
            this.textInfo = (TextView) itemView.findViewById(R.id.text_info);
            this.layoutContainerActions = (RelativeLayout) itemView.findViewById(R.id.layout_container_actions);

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layoutContainerActions.setVisibility(
                            layoutContainerActions.getVisibility() == View.VISIBLE ? View.GONE :
                                    View.VISIBLE);
                    viewIndicator.invalidate();
                }
            };
            this.itemView.setOnClickListener(clickListenerLink);
            this.textComment.setOnClickListener(clickListenerLink);
            this.textInfo.setOnClickListener(clickListenerLink);

        }
    }
}
