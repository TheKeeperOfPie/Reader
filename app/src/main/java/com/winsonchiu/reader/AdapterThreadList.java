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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rjeschke.txtmark.Processor;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.koushikdutta.ion.Response;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterThreadList extends RecyclerView.Adapter<AdapterThreadList.ViewHolder> {

    private static final String TAG = AdapterThreadList.class.getCanonicalName();

    private ThreadClickListener listener;
    private Listing listingLinks;
    private boolean isLoading;
    private String sort = "";
    private String subreddit = "";
    private Drawable drawableEmpty;
    private Drawable drawableDefault;
    private Activity activity;
    private SharedPreferences preferences;
    private int colorPositive;
    private int colorNegative;
    private int viewHeight;

    public AdapterThreadList(Activity activity, ThreadClickListener listener, String subreddit, String sort) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        this.activity = activity;
        this.listener = listener;
        this.listingLinks = new Listing();
        setParameters(subreddit, sort);
        Resources resources = activity.getResources();
        this.drawableEmpty = resources.getDrawable(R.drawable.ic_web_white_24dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_textsms_white_24dp);
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setParameters(String subreddit, String sort) {
        this.subreddit = subreddit;
        this.sort = sort;
        listener.setToolbarTitle("/r/" + subreddit);
        reloadAllLinks();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_thread, viewGroup, false));
    }

    @Override
    public int getItemCount() {
        return listingLinks.getChildren().size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {

        if (!isLoading && i > listingLinks.getChildren().size() - 5) {
            loadMoreLinks();
        }

        Link link = (Link) listingLinks.getChildren().get(i);
        holder.imageFull.setMaxHeight(viewHeight - holder.itemView.getHeight());
        holder.progressImage.setVisibility(View.GONE);
        holder.imageFull.setVisibility(View.GONE);
        holder.imageThreadPreview.setImageBitmap(null);
        holder.imageThreadPreview.setVisibility(View.VISIBLE);
        String thumbnail = link.getThumbnail();
        if (TextUtils.isEmpty(thumbnail)) {
            holder.imageThreadPreview.setImageDrawable(drawableEmpty);
        }
        else if (thumbnail.equals(Reddit.SELF) || thumbnail.equals(
                Reddit.DEFAULT) || thumbnail.equals(Reddit.NSFW)) {
            holder.imageThreadPreview.setImageDrawable(drawableDefault);
        }
        else {
            Ion.with(holder.imageThreadPreview)
                    .load(thumbnail);
        }
        holder.textThreadTitle.setText(link.getTitle());

        Spannable spannableInfo = new SpannableString(link.getScore() + " by " + link.getAuthor());
        spannableInfo.setSpan(new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative), 0,
                String.valueOf(link.getScore())
                        .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        holder.textThreadInfo.setText(spannableInfo);
        holder.layoutContainerActions.setVisibility(View.GONE);

    }

    public void reloadAllLinks() {
        setLoading(true);
        String url = "https://oauth.reddit.com" + "/r/" + subreddit + "/" + sort;
        Ion.with(activity)
                .load("GET", url)
                .addHeader(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
                .addHeader(Reddit.AUTHORIZATION, Reddit.BEARER + preferences.getString(AppSettings
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
                                reloadAllLinks();
                            }
                        })) {
                            return;
                        }

                        try {
                            listingLinks = Listing.fromJson(new JSONObject(result.getResult()));
                            notifyDataSetChanged();
                            listener.onFullLoaded(0);
                        }
                        catch (JSONException exception) {
                            exception.printStackTrace();
                        }
                        finally {
                            setLoading(false);
                        }
                    }
                });
        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        String url = "https://oauth.reddit.com" + "/r/" + subreddit + "/" + sort + "?" + "after=" + listingLinks.getAfter();
        Ion.with(activity)
                .load("GET", url)
                .addHeader(Reddit.USER_AGENT, Reddit.CUSTOM_USER_AGENT)
                .addHeader(Reddit.AUTHORIZATION, Reddit.BEARER + preferences.getString(AppSettings
                        .APP_ACCESS_TOKEN, ""))
                .addHeader("Content-Type","application/json; charset=utf-8")
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {

                        if (Reddit.resolveError(result.getHeaders()
                                .code(), activity, new Reddit.ErrorListener() {
                            @Override
                            public void onErrorHandled() {
                                loadMoreLinks();
                            }
                        })) {
                            return;
                        }

                        try {
                            int startPosition = listingLinks.getChildren()
                                    .size();
                            Listing listing = Listing.fromJson(new JSONObject(result.getResult()));
                            listingLinks.addChildren(listing.getChildren());
                            listingLinks.setAfter(listing.getAfter());
                            notifyItemRangeInserted(startPosition, listingLinks.getChildren()
                                    .size() - 1);
                        }
                        catch (JSONException exception) {
                            exception.printStackTrace();
                        }
                        finally {
                            setLoading(false);
                        }
                    }
                });
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        listener.setRefreshing(loading);
    }

    public void setViewHeight(int viewHeight) {
        this.viewHeight = viewHeight;
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
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        protected ProgressBar progressImage;
        protected ImageView imageFull;
        protected ImageView imageThreadPreview;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolder(View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            this.imageThreadPreview = (ImageView) itemView.findViewById(R.id.image_thread_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);

            this.imageThreadPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = (Link) listingLinks.getChildren().get(getPosition());
                    String url = link.getUrl();
                    String thumbnail = link.getThumbnail();
                    imageFull.setImageBitmap(null);
                    imageFull.setVisibility(View.GONE);
                    imageThreadPreview.setVisibility(View.VISIBLE);

                    if (link.isSelf()) {
                        setTextViewHTML(textThreadTitle, Processor.process(link.getSelfText()));
                        listener.onFullLoaded(getPosition());
                    }
                    else if (!TextUtils.isEmpty(url)) {

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
                                Log.d(TAG, "loadUrl: " + url);
                                listener.loadUrl(url);
                            }
                        }
                    }
                }
            });
            this.buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickComments((Link) listingLinks.getChildren().get(getPosition()));
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
                                listener.onFullLoaded(getPosition());
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
                                listener.onFullLoaded(getPosition());
                            }
                            else {
                                Toast.makeText(activity, "Error loading image", Toast
                                        .LENGTH_SHORT)
                                        .show();
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

    public interface ThreadClickListener {

        void onClickComments(Link link);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        void setToolbarTitle(String title);

    }

}