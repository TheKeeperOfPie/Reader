package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rjeschke.txtmark.Processor;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.Response;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */
public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;

    private Activity activity;
    private SharedPreferences preferences;
    private Link link;
    private CommentClickListener listener;
    private int indentWidth;
    private int colorPositive;
    private int colorNegative;
    private String subreddit;
    private String linkId;
    private Drawable drawableEmpty;
    private Drawable drawableDefault;

    public AdapterCommentList(Activity activity, CommentClickListener listener,
                              String subreddit, String linkId) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        this.activity = activity;
        this.listener = listener;
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, activity.getResources().getDisplayMetrics());
        this.subreddit = subreddit;
        this.linkId = linkId;
        Resources resources = activity.getResources();

        this.drawableEmpty = resources.getDrawable(R.drawable.ic_web_white_24dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_textsms_white_24dp);
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        reloadAllComments();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setLink(Link link) {
        this.link = link;
        this.subreddit = link.getSubreddit();
        this.linkId = link.getId();
        notifyDataSetChanged();
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
            return new ViewHolderHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_thread, parent, false));
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ViewHolderHeader) {
            ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;

            viewHolderHeader.progressImage.setVisibility(View.GONE);
            viewHolderHeader.imageThreadPreview.setImageBitmap(null);
            viewHolderHeader.imageThreadPreview.setVisibility(View.VISIBLE);
            String thumbnail = link.getThumbnail();
            if (TextUtils.isEmpty(thumbnail)) {
                viewHolderHeader.imageThreadPreview.setImageDrawable(drawableEmpty);
            }
            else if (thumbnail.equals(Reddit.SELF) || thumbnail.equals(
                    Reddit.DEFAULT) || thumbnail.equals(Reddit.NSFW)) {
                viewHolderHeader.imageThreadPreview.setImageDrawable(drawableDefault);
            }
            else {
                Ion.with(viewHolderHeader.imageThreadPreview)
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

            Comment comment = link.getComments().get(position - 1);

            ViewGroup.LayoutParams layoutParams = viewHolderComment.viewIndent.getLayoutParams();
            layoutParams.width = indentWidth * comment.getLevel();
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
        return link == null ? 0 : link.getComments().size() + 1;
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

    public void reloadAllComments() {
        Ion.with(activity)
                .load("https://oauth.reddit.com" + "/r/" + subreddit + "/comments/" + linkId)
                .addHeader(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
                .addHeader(Reddit.AUTHORIZATION,
                        Reddit.BEARER + PreferenceManager.getDefaultSharedPreferences(
                                activity.getApplicationContext()).getString(AppSettings
                                .APP_ACCESS_TOKEN, ""))
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {

                        if (Reddit.resolveError(result.getHeaders()
                                .code(), activity, new Reddit.ErrorListener() {
                            @Override
                            public void onErrorHandled() {
                                reloadAllComments();
                            }
                        })) {
                            return;
                        }

                        try {
                            setLink(Link.fromJson(new JSONArray(result.getResult())));
                            listener.setRefreshing(false);
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
    }

    protected class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected ProgressBar progressImage;
        protected ImageView imageFull;
        protected ImageView imageThreadPreview;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;

        public ViewHolderHeader(View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            this.imageThreadPreview = (ImageView) itemView.findViewById(R.id.image_thread_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.buttonComments.setVisibility(View.INVISIBLE);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);
            this.layoutContainerActions.setVisibility(View.VISIBLE);

            this.imageThreadPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = link.getUrl();
                    String thumbnail = link.getThumbnail();
                    imageFull.setImageBitmap(null);
                    imageFull.setVisibility(View.GONE);
                    imageThreadPreview.setVisibility(View.VISIBLE);

                    if (link.isSelf()) {
                        setTextViewHTML(textThreadTitle, Processor.process(link.getSelfText()));
                    }
                    else if (!TextUtils.isEmpty(url) && !thumbnail.equals(
                            Reddit.DEFAULT) && !thumbnail.equals(Reddit.NSFW)) {

                        // TODO: Add support for popular image domains
                        String domain = link.getDomain();
                        if (domain.contains("imgur")) {
                            loadImgur(link);
                        }
                        else {
                            boolean isImage = checkIsImage(url);
                            if (isImage) {
                                loadBasicImage(link);
                            }
                            else {
                                listener.loadUrl(url);
                            }
                        }
                    }
                }
            });
        }

        private void loadGfycat(Link link) {
            // TODO: Add support for Gfycat
        }

        private void loadImgur(Link link) {
            // TODO: Add support for Imgur

            Log.d(TAG, "loadImgur: " + link.getUrl());

            String url = link.getUrl();
            if (!url.contains("http")) {
                url += "http://";
            }

            if (!checkIsImage(url)) {
                if (url.charAt(url.length() - 1) == '/') {
                    url = url.substring(0, url.length() - 2);
                }
                url += ".jpg";
            }

            loadImage(url);

        }

        private void loadBasicImage(final Link link) {

            Log.d(TAG, "loadBasicImage: " + link.getUrl());

            String url = link.getUrl();
            if (!url.contains("http")) {
                url += "http://";
            }

            if (url.endsWith(".gif")) {
                progressImage.setVisibility(View.VISIBLE);
                Ion.with(activity)
                        .load(url)
                        .progress(new ProgressCallback() {
                            @Override
                            public void onProgress(long downloaded, long total) {
                                progressImage.setProgress((int) ((float) downloaded / total *
                                        1000));
                            }
                        })
                        .intoImageView(imageFull)
                        .setCallback(new FutureCallback<ImageView>() {
                            @Override
                            public void onCompleted(Exception e, ImageView result) {
                                imageFull.setVisibility(View.VISIBLE);
                                imageThreadPreview.setVisibility(View.INVISIBLE);
                                progressImage.setVisibility(View.GONE);
                                Log.d(TAG, "loadBasicImage completed");
                            }
                        });
            }
            else {
                loadImage(url);
            }
        }

        private void loadImage(String url) {
            progressImage.setVisibility(View.VISIBLE);
            Ion.with(activity)
                    .load(url)
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            progressImage.setProgress((int) ((float) downloaded / total *
                                    1000));
                        }
                    })
                    .asBitmap()
                    .setCallback(new FutureCallback<Bitmap>() {
                        @Override
                        public void onCompleted(Exception e, Bitmap result) {
                            if (result != null) {
                                imageFull.setVisibility(View.VISIBLE);
                                imageFull.setImageBitmap(result);
                                imageThreadPreview.setVisibility(View.INVISIBLE);
                            }
                            else {
                                Toast.makeText(activity, "Error loading image", Toast
                                        .LENGTH_SHORT).show();
                            }
                            progressImage.setVisibility(View.GONE);
                            Log.d(TAG, "loadBasicImage completed");
                        }
                    });
        }

        private boolean checkIsImage(String url) {
            return url.endsWith(Reddit.GIF) || url.endsWith(Reddit.PNG) || url.endsWith(Reddit.JPG)
                    || url.endsWith(Reddit.JPEG) || url.endsWith(Reddit.WEBP);
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

    public interface CommentClickListener {

        void loadUrl(String url);
        void setRefreshing(boolean refreshing);

    }
}
