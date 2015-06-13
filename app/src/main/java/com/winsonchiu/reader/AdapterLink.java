package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ControllerLinks.ListenerCallback {

    public static final int VIEW_LINK_HEADER = 0;
    public static final int VIEW_LINK = 1;

    private static final String TAG = AdapterLink.class.getCanonicalName();

    protected Activity activity;
    protected LayoutManager layoutManager;
    protected ControllerLinksBase controllerLinks;
    protected float itemWidth;
    protected ControllerLinks.LinkClickListener listener;
    protected SharedPreferences preferences;
    protected List<ViewHolderBase> viewHolders;
    private static int ACTION_MENU_SIZE = 6;

    public AdapterLink() {
        super();
        viewHolders = new ArrayList<>();
    }

    public void setActivity(Activity activity) {
        Resources resources = activity.getResources();
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
    }

    public void setControllerLinks(ControllerLinksBase controllerLinks,
            ControllerLinks.LinkClickListener listener) {
        this.controllerLinks = controllerLinks;
        this.listener = listener;
    }

    public abstract ControllerLinks.LinkClickListener getListener();

    @Override
    public ControllerLinksBase getController() {
        return controllerLinks;
    }

    public abstract float getItemWidth();

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    public abstract RecyclerView.ItemDecoration getItemDecoration();

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_LINK_HEADER : VIEW_LINK;
    }

    @Override
    public int getItemCount() {
        return controllerLinks.sizeLinks() > 0 ? controllerLinks.sizeLinks() + 1 : 0;
    }

    protected static class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected TextView textInfo;
        protected ImageButton buttonOpen;
        private ControllerLinks.ListenerCallback callback;

        public ViewHolderHeader(View itemView,
                ControllerLinks.ListenerCallback listenerCallback,
                Subreddit subreddit) {
            super(itemView);

            callback = listenerCallback;
            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        true);
            }

            if (TextUtils.isEmpty(subreddit.getDisplayName())) {
                textName.setVisibility(View.GONE);
                textTitle.setVisibility(View.GONE);
                textDescription.setVisibility(View.GONE);
                textInfo.setVisibility(View.GONE);
                itemView.setVisibility(View.GONE);
            }
        }

        public void onBind(Subreddit subreddit) {

            if (TextUtils.isEmpty(subreddit.getDisplayName())) {
                textName.setVisibility(View.GONE);
                textTitle.setVisibility(View.GONE);
                textDescription.setVisibility(View.GONE);
                textInfo.setVisibility(View.GONE);
                itemView.setVisibility(View.GONE);
                return;
            }
            textName.setVisibility(View.VISIBLE);
            textTitle.setVisibility(View.VISIBLE);
            textDescription.setVisibility(View.VISIBLE);
            textInfo.setVisibility(View.VISIBLE);
            itemView.setVisibility(View.VISIBLE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(subreddit.getTitle());

            if (TextUtils.isEmpty(subreddit.getPublicDescriptionHtml()) || "null".equals(
                    subreddit.getPublicDescriptionHtml())) {
                textDescription.setText("");
            }
            else {
                // TODO: Move all instances to Reddit class
                String html = subreddit.getPublicDescriptionHtml();
                html = Html.fromHtml(html.trim())
                        .toString();

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

                textDescription.setText(sequence);
            }

            textInfo.setText(subreddit.getSubscribers() + " subscribers\n" +
                    "created " + new Date(subreddit.getCreatedUtc()));

        }
    }

    protected static abstract class ViewHolderBase extends RecyclerView.ViewHolder {

        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected ImageView imagePlay;
        protected ImageView imageThumbnail;
        protected VideoView videoFull;
        protected WebViewFixed webFull;
        protected TextView textThreadFlair;
        protected TextView textThreadTitle;
        protected TextView textThreadSelf;
        protected TextView textThreadInfo;
        protected TextView textHidden;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerExpand;
        protected Toolbar toolbarActions;
        protected ControllerLinks.ListenerCallback callback;
        protected ImageLoader.ImageContainer imageContainer;
        protected Request request;
        protected String imageUrl;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;

        public ViewHolderBase(final View itemView,
                ControllerLinks.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

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
            webFull.getSettings()
                    .setUseWideViewPort(true);
            webFull.getSettings()
                    .setBuiltInZoomControls(true);
            webFull.getSettings()
                    .setDisplayZoomControls(false);
            webFull.getSettings()
                    .setDomStorageEnabled(true);
            webFull.setBackgroundColor(0x000000);
            webFull.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    webFull.lockHeight();
                    super.onScaleChanged(view, oldScale, newScale);
                }
            });
            webFull.setOnTouchListener(new View.OnTouchListener() {

                float startY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();

                            if ((webFull.canScrollVertically(1) && webFull.canScrollVertically(
                                    -1))) {
                                callback.getListener()
                                        .requestDisallowInterceptTouchEvent(true);
                            }
                            else {
                                callback.getListener()
                                        .requestDisallowInterceptTouchEvent(false);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            callback.getListener()
                                    .requestDisallowInterceptTouchEvent(false);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (event.getY() - startY < 0 && webFull.canScrollVertically(1)) {
                                callback.getListener()
                                        .requestDisallowInterceptTouchEvent(true);
                            }
                            else if (event.getY() - startY > 0 && webFull.canScrollVertically(-1)) {
                                callback.getListener()
                                        .requestDisallowInterceptTouchEvent(true);
                            }
                            break;
                    }
                    return false;
                }
            });
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            imageThumbnail = (ImageView) itemView.findViewById(R.id.image_thumbnail);
            textThreadFlair = (TextView) itemView.findViewById(R.id.text_thread_flair);
            textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            textThreadSelf = (TextView) itemView.findViewById(R.id.text_thread_self);
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.pauseViewHolders();
                    callback.getListener()
                            .onClickComments(
                                    callback.getController()
                                            .getLink(getAdapterPosition()), ViewHolderBase.this);
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_link);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_upvote:
                            callback.getController()
                                    .voteLink(ViewHolderBase.this, 1);
                            break;
                        case R.id.item_downvote:
                            callback.getController()
                                    .voteLink(ViewHolderBase.this, -1);
                            break;
                        case R.id.item_share:
                            break;
                        case R.id.item_download:
                            String url = callback.getController()
                                    .getLink(getAdapterPosition())
                                    .getUrl();
                            // TODO: Consolidate image format checking
                            if (Reddit.checkIsImage(url)) {
                                downloadImage(url);
                            }
                            break;
                        case R.id.item_web:
                            ViewHolderBase.this.callback.getListener()
                                    .loadUrl(
                                            callback.getController()
                                                    .getLink(getAdapterPosition())
                                                    .getUrl());
                            break;
                        case R.id.item_reply:
                            toggleReply();
                    }
                    return true;
                }
            });
            layoutContainerExpand = (LinearLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(callback.getPreferences()
                            .getString(AppSettings.REFRESH_TOKEN, ""))) {
                        Toast.makeText(callback.getController()
                                .getActivity(), "Must be logged in to reply",
                                Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        final Link link = callback.getController()
                                .getLink(getAdapterPosition());
                        Map<String, String> params = new HashMap<>();
                        params.put("api_type", "json");
                        params.put("text", editTextReply.getText()
                                .toString());
                        params.put("thing_id", link.getName());

                        // TODO: Move add to immediate on button click, check if failed afterwards
                        callback.getController()
                                .getReddit()
                                .loadPost(Reddit.OAUTH_URL + "/api/comment",
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                try {
                                                    if (callback.getControllerComments()
                                                            .getMainLink()
                                                            .getName()
                                                            .equals(link.getName())) {
                                                        JSONObject jsonObject = new JSONObject(
                                                                response);
                                                        Comment newComment = Comment.fromJson(
                                                                jsonObject.getJSONObject("json")
                                                                        .getJSONObject("data")
                                                                        .getJSONArray("things")
                                                                        .getJSONObject(0), 0);
                                                        callback.getControllerComments()
                                                                .insertComment(newComment);
                                                    }
                                                }
                                                catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {

                                            }
                                        }, params, 0);

                        AnimationUtils.animateExpand(layoutContainerReply,
                                getRatio(getAdapterPosition()), null);
                    }
                }
            });

            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVoteColors();
                    Menu menu = toolbarActions.getMenu();
                    Link link = callback.getController()
                            .getLink(getAdapterPosition());
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());

                    setToolbarMenuVisibility();
                    ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(
                            toolbarActions.getMenu()
                                    .findItem(R.id.item_share));
                    if (shareActionProvider != null) {
                        shareActionProvider.setShareIntent(shareIntent);
                    }

                    if (Reddit.checkIsImage(link.getUrl()) || Reddit.placeImageUrl(link)) {
                        menu.findItem(R.id.item_download)
                                .setVisible(true);
                        menu.findItem(R.id.item_download)
                                .setEnabled(true);
                    }
                    else {
                        menu.findItem(R.id.item_download)
                                .setVisible(false);
                        menu.findItem(R.id.item_download)
                                .setEnabled(false);
                    }

                    AnimationUtils.animateExpand(layoutContainerExpand,
                            getRatio(getAdapterPosition()), null);

                }
            };


            this.imageThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = callback.getController()
                            .getLink(getAdapterPosition());
                    onClickThumbnail(link);
                }
            });

            textThreadTitle.setOnClickListener(clickListenerLink);
            textThreadInfo.setOnClickListener(clickListenerLink);
            textThreadSelf.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {
                    setToolbarMenuVisibility();
                }
            });

        }

        public void toggleReply() {
            Link link = callback.getController()
                    .getLink(getAdapterPosition());
            if (!link.isReplyExpanded()) {
                editTextReply.requestFocus();
                editTextReply.setText(null);
            }
            link.setReplyExpanded(!link.isReplyExpanded());
            AnimationUtils.animateExpand(layoutContainerReply, getRatio(getAdapterPosition()),
                    null);
        }

        public abstract float getRatio(int adapterPosition);

        public void loadFull(Link link) {

            Log.d(TAG, "loadFull: " + link.getUrl());

            // TODO: Toggle visibility of web and video views

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
                        loadGallery(imgurId);
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
                    callback.getController()
                            .getReddit()
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

        public void onClickThumbnail(Link link) {

            if (link.isSelf()) {
                if (textThreadSelf.isShown()) {
                    textThreadSelf.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(link.getSelfText())) {
                    callback.pauseViewHolders();
                    callback.getListener()
                            .onClickComments(link, this);
                }
                else {
                    if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                                true);
                        ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                    }

                    String html = link.getSelfTextHtml();
                    html = Html.fromHtml(html.trim())
                            .toString();

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

                    textThreadSelf.setVisibility(View.VISIBLE);
                    textThreadSelf.setText(sequence);
                }
            }
            else {
                if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                            true);
                    ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                }
                loadFull(link);
            }
        }

        public void setVoteColors() {

            Menu menu = toolbarActions.getMenu();
            Link link = callback.getController()
                    .getLink(getAdapterPosition());
            switch (link.isLikes()) {
                case 1:
                    menu.findItem(R.id.item_upvote)
                            .getIcon()
                            .setColorFilter(
                                    callback.getController()
                                            .getActivity()
                                            .getResources()
                                            .getColor(R.color.positiveScore),
                                    PorterDuff.Mode.MULTIPLY);
                    menu.findItem(R.id.item_downvote)
                            .getIcon()
                            .clearColorFilter();
                    break;
                case -1:
                    menu.findItem(R.id.item_downvote)
                            .getIcon()
                            .setColorFilter(
                                    callback.getController()
                                            .getActivity()
                                            .getResources()
                                            .getColor(R.color.negativeScore),
                                    PorterDuff.Mode.MULTIPLY);
                    menu.findItem(R.id.item_upvote)
                            .getIcon()
                            .clearColorFilter();
                    break;
                case 0:
                    menu.findItem(R.id.item_upvote)
                            .getIcon()
                            .clearColorFilter();
                    menu.findItem(R.id.item_downvote)
                            .getIcon()
                            .clearColorFilter();
                    break;
            }
        }

        public void setTextInfo(Link link) {

        }

        private void attemptLoadImage(Link link) {

            if (Reddit.placeImageUrl(link)) {
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(
                        callback.getController()
                                .getLink(getAdapterPosition())
                                .getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
                callback.getListener()
                        .onFullLoaded(getAdapterPosition());
            }
            else {
                callback.getListener()
                        .loadUrl(link.getUrl());
            }
        }

        private void loadGallery(String id) {
            progressImage.setVisibility(View.VISIBLE);
            request = callback.getController()
                    .getReddit()
                    .loadImgurGallery(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(callback.getController()
                                                        .getActivity(),
                                                        album,
                                                        callback.getListener()));
                                        viewPagerFull.getLayoutParams().height = callback.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        if (ViewHolderBase.this instanceof AdapterLinkGrid.ViewHolder) {
                                            imageThumbnail.setVisibility(View.GONE);
                                        }
                                        callback.getListener()
                                                .onFullLoaded(getAdapterPosition());
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
                                    Log.d(TAG, "loadGallery error: " + error.toString());
                                }
                            }, 0);
        }

        private void loadAlbum(String id) {
            progressImage.setVisibility(View.VISIBLE);
            request = callback.getController()
                    .getReddit()
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
                                                new AdapterAlbum(callback.getController()
                                                        .getActivity(), album,
                                                        callback.getListener()));
                                        viewPagerFull.getLayoutParams().height = callback.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        if (ViewHolderBase.this instanceof AdapterLinkGrid.ViewHolder) {
                                            imageThumbnail.setVisibility(View.GONE);
                                        }
                                        callback.getListener()
                                                .onFullLoaded(getAdapterPosition());
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


        private void loadGifv(String id) {
            Log.d(TAG, "loadGifv: " + id);
            request = callback.getController()
                    .getReddit()
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
                                            loadVideo(image.getMp4(),
                                                    (float) image.getHeight() / image.getWidth());
                                        }
                                        else if (!TextUtils.isEmpty(image.getWebm())) {
                                            loadVideo(image.getWebm(),
                                                    (float) image.getHeight() / image.getWidth());
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
                                        Log.d(TAG, "" + error.networkResponse.statusCode);
                                        Log.d(TAG, error.networkResponse.headers.toString());
                                        Log.d(TAG, new String(error.networkResponse.data));
                                    }
                                    catch (Throwable e) {

                                    }
                                    Log.d(TAG, "error on loadGifv");
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0);
        }

        private void loadVideo(String url, float heightRatio) {
            Log.d(TAG, "loadVideo: " + url + " : " + heightRatio);
            Uri uri = Uri.parse(url);
            videoFull.setVideoURI(uri);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.getLayoutParams().height = (int) (ViewHolderBase.this.itemView.getWidth() * heightRatio);
            videoFull.invalidate();
            videoFull.start();
            videoFull.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoFull.start();
                        }
                    });
            callback.getListener()
                    .onFullLoaded(getAdapterPosition());
        }

        public void setToolbarMenuVisibility() {
            Menu menu = toolbarActions.getMenu();
            int maxNum = (int) (itemView.getWidth() / callback.getItemWidth());

            for (int index = 0; index < ACTION_MENU_SIZE; index++) {
                if (index < maxNum - 1) {
                    menu.getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    menu.getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }


        public void onRecycle() {

            if (imageContainer != null) {
                imageContainer.cancelRequest();
            }
            if (request != null) {
                request.cancel();
            }

            webFull.onPause();
            webFull.resetMaxHeight();
            webFull.setVisibility(View.GONE);
            videoFull.stopPlayback();
            videoFull.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);

//            if (!TextUtils.isEmpty(imageUrl) && !callback.getController().getReddit().getImageLoader().isCached(imageUrl, 0, 0)) {
//                Drawable drawable = imageThumbnail.getDrawable();
//                if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
//                    ((BitmapDrawable) drawable).getBitmap().recycle();
//                }
//                imageUrl = null;
//            }
            imageThumbnail.setImageBitmap(null);

            if (viewPagerFull.getAdapter() != null) {
                for (int index = 0; index < viewPagerFull.getChildCount(); index++) {
                    ((WebView) viewPagerFull.getChildAt(index)
                            .findViewById(R.id.web_image)).onPause();
                    ((WebView) viewPagerFull.getChildAt(index)
                            .findViewById(R.id.web_image)).destroy();
                }
            }

        }

        public void onBind(int position) {

            Link link = callback.getController()
                    .getLink(position);
            layoutContainerExpand.setVisibility(View.GONE);

            if (link.isReplyExpanded()) {
                layoutContainerReply.setVisibility(View.VISIBLE);
            }
            else {
                layoutContainerReply.setVisibility(View.GONE);
            }

        }

        public void downloadImage(String url) {
            Picasso.with(callback.getController()
                    .getActivity())
                    .load(url)
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            File file = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/ReaderForReddit/" + System.currentTimeMillis() + ".png");

                            file.getParentFile()
                                    .mkdirs();

                            FileOutputStream out = null;
                            try {
                                out = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                            }
                            catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            finally {
                                try {
                                    if (out != null) {
                                        out.close();
                                    }
                                }
                                catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }

                            Toast.makeText(callback.getController()
                                    .getActivity(), "Image downloaded",
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    });
        }

    }

}
