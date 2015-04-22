package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
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
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */

public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;
    private static final int LINK_MENU_SIZE = 4;
    private static final int COMMENT_MENU_SIZE = 5;
    private final ControllerComments.CommentClickListener listener;
    private final float itemWidth;

    private Activity activity;
    private ControllerComments controllerComments;
    private int colorPrimary;
    private int colorPositive;
    private int colorNegative;
    private Drawable drawableUpvote;
    private Drawable drawableDownvote;

    public AdapterCommentList(Activity activity, ControllerComments controllerComments, ControllerComments.CommentClickListener listener) {
        // TODO: Add setActivity
        this.activity = activity;
        this.controllerComments = controllerComments;
        this.listener = listener;
        Resources resources = activity.getResources();
        this.colorPrimary = resources.getColor(R.color.colorPrimary);
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
        this.drawableDownvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_down_white_24dp);
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, resources.getDisplayMetrics());
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
            final ViewHolderHeader viewHolder = (ViewHolderHeader) holder;
            viewHolder.imagePreview.setImageBitmap(null);

            Link link = controllerComments.getLink();

            Drawable drawable = controllerComments.getDrawableForLink();
            if (drawable == null) {
                viewHolder.imagePreview.setTag(
                        controllerComments.loadImage(link.getThumbnail(), new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response,
                                                   boolean isImmediate) {
                                if (response.getBitmap() != null) {
                                    viewHolder.imagePreview.setAlpha(0.0f);
                                    viewHolder.imagePreview.setImageBitmap(response.getBitmap());
                                    AnimationUtils.animateAlpha(viewHolder.imagePreview, 0.0f, 1.0f);
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
            viewHolder.setTextInfo();
            viewHolder.toolbarActions.setVisibility(View.GONE);

            if (link.isSelf()) {
                if (!TextUtils.isEmpty(link.getSelfText())) {
                    String html = link.getSelfTextHtml();
                    html = Html.fromHtml(html.trim())
                            .toString();
                    viewHolder.textThreadSelf.setVisibility(View.VISIBLE);
                    viewHolder.textThreadSelf.setText(Reddit.formatHtml(html,
                            new Reddit.UrlClickListener() {
                                @Override
                                public void onUrlClick(String url) {
                                    listener.loadUrl(url);
                                }
                            }));
                }
            }


        }
        else {

            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;

            Comment comment = controllerComments.get(position - 1);

            if (comment.isReplyExpanded()) {
                viewHolderComment.editTextReply.setVisibility(View.VISIBLE);
                viewHolderComment.buttonSendReply.setVisibility(View.VISIBLE);
                viewHolderComment.layoutContainerActions.setVisibility(View.VISIBLE);
                viewHolderComment.toolbarActions.setVisibility(View.VISIBLE);
            }
            else {
                viewHolderComment.editTextReply.setVisibility(View.GONE);
                viewHolderComment.buttonSendReply.setVisibility(View.GONE);
                viewHolderComment.toolbarActions.setVisibility(View.GONE);
            };

            ViewGroup.LayoutParams layoutParams = viewHolderComment.viewIndent.getLayoutParams();
            layoutParams.width = controllerComments.getIndentWidth(comment);
            viewHolderComment.viewIndent.setLayoutParams(layoutParams);

            if (comment.isMore()) {
                viewHolderComment.textComment.setText(R.string.load_more_comments);
                viewHolderComment.textInfo.setText("");
            }
            else {
                String html = comment.getBodyHtml();
                html = Html.fromHtml(html.trim())
                        .toString();

                setTextViewHTML(viewHolderComment.textComment, html);

                Spannable spannableInfo = new SpannableString(
                        comment.getScore() + " by " + comment.getAuthor());
                spannableInfo.setSpan(new ForegroundColorSpan(
                                comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                        String.valueOf(comment.getScore())
                                .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                if (controllerComments.getLink().getAuthor().equals(comment.getAuthor())) {
                    spannableInfo.setSpan(new ForegroundColorSpan(colorPrimary), spannableInfo.length() - comment.getAuthor().length(), spannableInfo.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                viewHolderComment.textInfo.setText(spannableInfo);
            }
        }

    }

    @Override
    public int getItemCount() {
        return controllerComments.getItemCount();
    }

    private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                listener.loadUrl(span.getURL());
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

    // TODO: Abstract Link ViewHolder or find a different way to prevent duplicate code (maybe a shared Listener/Callback?)
    protected class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected ImageView imagePlay;
        protected ImageView imagePreview;
        protected VideoView videoFull;
        protected WebViewFixed webFull;
        protected TextView textThreadTitle;
        protected TextView textThreadSelf;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected Toolbar toolbarActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolderHeader(final View itemView) {
            super(itemView);

            progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            imagePlay = (ImageView) itemView.findViewById(R.id.image_play);
            mediaController = new MediaController(itemView.getContext());
            videoFull = (VideoView) itemView.findViewById(R.id.video_full);
            videoFull.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaController.hide();
                }
            });
            mediaController.setAnchorView(videoFull);
            videoFull.setMediaController(mediaController);
            webFull = (WebViewFixed) itemView.findViewById(R.id.web_full);
            webFull.getSettings().setUseWideViewPort(true);
            webFull.getSettings().setLoadWithOverviewMode(true);
            webFull.getSettings().setBuiltInZoomControls(true);
            webFull.getSettings().setDisplayZoomControls(false);
            webFull.getSettings().setJavaScriptEnabled(true);
            webFull.getSettings().setDomStorageEnabled(true);
            webFull.setBackgroundColor(0x000000);
            webFull.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    webFull.lockHeight();
                    super.onScaleChanged(view, oldScale, newScale);
                }
            });
            webFull.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        if ((webFull.canScrollVertically(1) && webFull.canScrollVertically(-1))) {
                            listener
                                    .requestDisallowInterceptTouchEvent(true);
                        } else {
                            listener
                                    .requestDisallowInterceptTouchEvent(false);
                            if (webFull.getScrollY() == 0) {
                                webFull.setScrollY(1);
                            } else {
                                webFull.setScrollY(webFull.getScrollY() - 1);
                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        listener
                                .requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            textThreadSelf = (TextView) itemView.findViewById(R.id.text_thread_self);
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());
            buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            buttonComments.setVisibility(View.INVISIBLE);
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_link);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_upvote:
                            controllerComments.voteLink(ViewHolderHeader.this, 1);
                            break;
                        case R.id.item_downvote:
                            controllerComments.voteLink(ViewHolderHeader.this, -1);
                            break;
                        case R.id.item_share:
                            break;
                        case R.id.item_web:
                            listener.loadUrl(controllerComments.getLink().getUrl());
                            break;
                    }
                    return true;
                }
            });
            toolbarActions.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int maxNum = (int) (itemView.getWidth() / itemWidth);

                    for (int index = 0; index < LINK_MENU_SIZE; index++) {
                        if (index <= maxNum) {
                            toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        }
                        else {
                            toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        }
                    }
                    toolbarActions.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });



            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVoteColors();
                    AnimationUtils.animateExpandActions(toolbarActions, false);
                }
            };
            textThreadTitle.setOnClickListener(clickListenerLink);
            textThreadInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = controllerComments.getLink();
                    imagePreview.setVisibility(View.VISIBLE);

                    if (!link.isSelf()) {
                        loadFull(link);
                    }
                }
            });

        }

        public void loadFull(Link link ) {

            Log.d(TAG, "loadFull: " + link.getUrl());

            String url = link.getUrl();
            if (!TextUtils.isEmpty(url)) {
                if (link.getDomain()
                        .contains("imgur")) {
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
                        int dotIndex = url.substring(startIndex)
                                .indexOf(".") + startIndex;
                        lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                        String imgurId = url.substring(startIndex, lastIndex);
                        loadGifv(imgurId);
                    }
                    else {
                        attemptLoadImage(link);
                    }
                }
                else if (link.getDomain()
                        .contains("gfycat")) {
                    int startIndex = url.indexOf("gfycat.com/") + 11;
                    int dotIndex = url.substring(startIndex)
                            .indexOf(".");
                    int lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                    String gfycatId = url.substring(startIndex, lastIndex);
                    progressImage.setVisibility(View.VISIBLE);
                    controllerComments.getReddit()
                            .loadGet(Reddit.GFYCAT_URL + gfycatId,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(
                                                        response).getJSONObject(Reddit.GFYCAT_ITEM);
                                                loadVideo(jsonObject.getString(Reddit.GFYCAT_WEBM),
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
                    attemptLoadImage(link);
                }
            }
        }

        public void setVoteColors() {

            Link link = controllerComments.getLink();
            switch (link.isLikes()) {
                case 1:
                    toolbarActions.getMenu()
                            .findItem(R.id.item_upvote)
                            .getIcon()
                            .setColorFilter(
                                    colorPositive, PorterDuff.Mode.MULTIPLY);
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().clearColorFilter();
                    break;
                case -1:
                    toolbarActions.getMenu().findItem(R.id.item_downvote)
                            .getIcon()
                            .setColorFilter(
                                    colorNegative, PorterDuff.Mode.MULTIPLY);
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().clearColorFilter();
                    break;
                case 0:
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().clearColorFilter();
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().clearColorFilter();
                    break;
            }
        }

        public void setTextInfo() {
            Link link = controllerComments.getLink();

            String subreddit = "/r/" + link.getSubreddit();
            Spannable spannableInfo = new SpannableString(subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + String.valueOf(link.getScore())
                            .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);
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
                listener.loadUrl(link.getUrl());
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
                                        Log.d(TAG, "loadAlbum: " + response);
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(activity, album,
                                                        listener));
                                        viewPagerFull.getLayoutParams().height = listener
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } finally {
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
            Log.d(TAG, "loadGifv: " + id);
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

                                        if (!TextUtils.isEmpty(image.getMp4())) {
                                            loadVideo(image.getMp4(), (float) image.getHeight() / image.getWidth());
                                        }
                                        else if (!TextUtils.isEmpty(image.getWebm())) {
                                            loadVideo(image.getWebm(), (float) image.getHeight() / image.getWidth());
                                        }
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
                                    try {
                                        Log.d(TAG, "onErrorResponse");
                                        Log.d(TAG, "" + error.networkResponse.statusCode);
                                        Log.d(TAG, error.networkResponse.headers.toString());
                                        Log.d(TAG, new String(error.networkResponse.data));
                                    }
                                    catch (Throwable e) {

                                    }
                                    Log.d(TAG, "error on loadGifv");
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0));
        }


        private void loadVideo(String url, float heightRatio) {
            Log.d(TAG, "loadVideo: " + url + " : " + heightRatio);
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
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        protected MenuItem itemCollapse;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemShare;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolderComment(final View itemView) {
            super(itemView);

            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textComment.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_comment);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_collapse:
                            controllerComments.toggleComment(getPosition() - 1);
                            break;
                        case R.id.item_upvote:
                            controllerComments.voteComment(ViewHolderComment.this, 1);
                            break;
                        case R.id.item_downvote:
                            controllerComments.voteComment(ViewHolderComment.this, -1);
                            break;
                        case R.id.item_reply:
                            Comment comment = controllerComments.get(getPosition() - 1);
                            if (!comment.isReplyExpanded()) {
                                editTextReply.requestFocus();
                            }
                            comment.setReplyExpanded(!comment.isReplyExpanded());
                            AnimationUtils.animateExpand(editTextReply);
                            AnimationUtils.animateExpand(buttonSendReply);
                            break;
                        case R.id.item_share:
                            break;
                    }
                    return true;
                }
            });
            toolbarActions.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int maxNum = (int) (itemView.getWidth() / itemWidth);

                    for (int index = 0; index < COMMENT_MENU_SIZE; index++) {
                        if (index <= maxNum) {
                            toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        }
                        else {
                            toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        }
                    }
                    toolbarActions.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
            itemUpvote = toolbarActions.getMenu().findItem(R.id.item_upvote);
            itemDownvote = toolbarActions.getMenu().findItem(R.id.item_downvote);

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Comment comment = controllerComments.get(getPosition() - 1);
                    if (comment.isMore()) {
                        Log.d(TAG, "loadMoreComments");
                        controllerComments.loadMoreComments(comment);
                        return;
                    }

                    setVoteColors();
                    AnimationUtils.animateExpandActions(layoutContainerActions, true);
                    AnimationUtils.animateExpandActions(toolbarActions, false);
                    viewIndicator.invalidate();
                }
            };
            textComment.setOnClickListener(clickListenerLink);
            textInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

        }

        public void setVoteColors() {

            Comment comment = (Comment) controllerComments.getListingComments().getChildren().get(getPosition() - 1);
            switch (comment.isLikes()) {
                case 1:
                    drawableUpvote.setColorFilter(colorPositive, PorterDuff.Mode.MULTIPLY);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case -1:
                    drawableDownvote.setColorFilter(colorNegative, PorterDuff.Mode.MULTIPLY);
                    itemDownvote.setIcon(drawableDownvote);
                    drawableUpvote.clearColorFilter();
                    itemUpvote.setIcon(drawableUpvote);
                    break;
                case 0:
                    drawableUpvote.clearColorFilter();
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
            }
        }

    }
}
