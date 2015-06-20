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
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
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
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.User;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    protected User user;
    private int titleMargin;

    public AdapterLink() {
        super();
        viewHolders = new ArrayList<>();
        this.user = new User();
    }

    @Override
    public User getUser() {
        return user;
    }

    public void setActivity(Activity activity) {
        Resources resources = activity.getResources();
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
        this.titleMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                resources.getDisplayMetrics());
        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
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

    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public int getTitleMargin() {
        return titleMargin;
    }

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

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (!controllerLinks.isLoading() && position > controllerLinks.sizeLinks() - 5) {
            controllerLinks.loadMoreLinks();
        }
    }

    protected static class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected TextView textInfo;
        protected ImageButton buttonOpen;
        protected Button buttonSubmitLink;
        protected Button buttomSubmitSelf;
        protected LinearLayout layoutButtons;
        protected View viewDivider;

        private String defaultTextSubmitLink;
        private String defaultTextSubmitText;

        private ControllerLinks.ListenerCallback callback;

        public ViewHolderHeader(View itemView,
                final ControllerLinks.ListenerCallback listenerCallback) {
            super(itemView);

            callback = listenerCallback;
            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutButtons = (LinearLayout) itemView.findViewById(R.id.layout_buttons);
            buttonSubmitLink = (Button) itemView.findViewById(R.id.button_submit_link);
            buttomSubmitSelf = (Button) itemView.findViewById(R.id.button_submit_self);
            viewDivider = itemView.findViewById(R.id.view_divider);

            buttonSubmitLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listenerCallback.getListener()
                            .onClickSubmit(Reddit.POST_TYPE_LINK);
                }
            });
            buttomSubmitSelf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listenerCallback.getListener()
                            .onClickSubmit(Reddit.POST_TYPE_SELF);
                }
            });

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        true);
            }

            defaultTextSubmitLink = itemView.getContext()
                    .getResources()
                    .getString(R.string.submit_link);
            defaultTextSubmitText = itemView.getContext()
                    .getResources()
                    .getString(R.string.submit_text);
        }

        private void setVisibility(int visibility) {
            textName.setVisibility(visibility);
            textTitle.setVisibility(visibility);
            textDescription.setVisibility(visibility);
            textInfo.setVisibility(visibility);
            layoutButtons.setVisibility(visibility);
            buttonSubmitLink.setVisibility(visibility);
            buttomSubmitSelf.setVisibility(visibility);
            viewDivider.setVisibility(visibility);
        }

        public void onBind(Subreddit subreddit) {

            if (TextUtils.isEmpty(subreddit.getDisplayName()) || "/r/all/".equalsIgnoreCase(
                    subreddit.getUrl())) {
                setVisibility(View.GONE);
                return;
            }
            setVisibility(View.VISIBLE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(Reddit.getTrimmedHtml(subreddit.getTitle()));

            if (TextUtils.isEmpty(subreddit.getPublicDescriptionHtml()) || "null".equals(
                    subreddit.getPublicDescriptionHtml())) {
                textDescription.setText("");
            }
            else {
                textDescription.setText(
                        Reddit.getTrimmedHtml(subreddit.getPublicDescriptionHtml()));
            }

            textInfo.setText(subreddit.getSubscribers() + " subscribers\n" +
                    "created " + new Date(subreddit.getCreatedUtc()));

            if (TextUtils.isEmpty(subreddit.getSubmitLinkLabel()) || "null".equals(
                    subreddit.getSubmitLinkLabel())) {
                buttonSubmitLink.setText(defaultTextSubmitLink);
            }
            else {
                buttonSubmitLink.setText(subreddit.getSubmitLinkLabel());
            }

            if (TextUtils.isEmpty(subreddit.getSubmitTextLabel()) || "null".equals(
                    subreddit.getSubmitTextLabel())) {
                buttomSubmitSelf.setText(defaultTextSubmitText);
            }
            else {
                buttomSubmitSelf.setText(subreddit.getSubmitTextLabel());
            }

            if (Reddit.POST_TYPE_LINK.equalsIgnoreCase(subreddit.getSubmissionType())) {
                buttomSubmitSelf.setVisibility(View.GONE);
            }
            else if (Reddit.POST_TYPE_SELF.equalsIgnoreCase(subreddit.getSubmissionType())) {
                buttonSubmitLink.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (ViewHolderBase viewHolder : viewHolders) {
            viewHolder.destroy();
        }
    }

    protected static abstract class ViewHolderBase extends RecyclerView.ViewHolder {

        private Calendar calendar;
        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected AdapterAlbum adapterAlbum;
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
        protected RelativeLayout layoutContainerExpand;
        protected Toolbar toolbarActions;
        protected ControllerLinks.ListenerCallback callback;
        protected ImageLoader.ImageContainer imageContainer;
        protected Request request;
        protected String imageUrl;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected YouTubePlayerView viewYouTube;
        protected YouTubePlayer youTubePlayer;

        public ViewHolderBase(final View itemView,
                ControllerLinks.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            calendar = Calendar.getInstance();
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
                    .setLoadWithOverviewMode(true);
            webFull.getSettings()
                    .setBuiltInZoomControls(true);
            webFull.getSettings()
                    .setDisplayZoomControls(false);
            webFull.getSettings()
                    .setJavaScriptEnabled(true);
            webFull.getSettings()
                    .setDomStorageEnabled(true);
            webFull.getSettings()
                    .setDatabaseEnabled(true);
            webFull.getSettings()
                    .setAppCacheEnabled(true);
            webFull.setBackgroundColor(0x000000);
            webFull.setWebChromeClient(new WebChromeClient());
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
            adapterAlbum = new AdapterAlbum(callback.getController().getActivity(), new Album(), callback.getListener());
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            viewPagerFull.setAdapter(adapterAlbum);
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
                    Link link = callback.getController().getLink(getAdapterPosition());

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
                        case R.id.item_download_image:
                            String url = link.getUrl();
                            // TODO: Consolidate image format checking
                            if (Reddit.checkIsImage(url)) {
                                downloadImage(url);
                            }
                            break;
                        case R.id.item_web:
                            ViewHolderBase.this.callback.getListener()
                                    .loadUrl(link.getUrl());
                            break;
                        case R.id.item_reply:
                            if (TextUtils.isEmpty(callback.getPreferences()
                                    .getString(AppSettings.REFRESH_TOKEN, ""))) {
                                Toast.makeText(callback.getController()
                                        .getActivity(), callback.getController()
                                        .getActivity()
                                        .getString(R.string.must_be_logged_in_to_reply),
                                        Toast.LENGTH_SHORT)
                                        .show();
                                return false;
                            }
                            toggleReply();
                            break;
                        case R.id.item_view_profile:
                            Intent intent = new Intent(callback.getControllerComments()
                                    .getActivity(), MainActivity.class);
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.putExtra(MainActivity.REDDIT_PAGE,
                                    "https://reddit.com/user/" + link.getAuthor());
                            callback.getControllerComments()
                                    .getActivity()
                                    .startActivity(intent);
                            break;
                        case R.id.item_delete:
                            callback.getController()
                                    .deletePost(callback.getController()
                                            .getLink(getAdapterPosition()));
                            break;
                    }
                    return true;
                }
            });
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            editTextReply.setOnTouchListener(new View.OnTouchListener() {

                float startY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();

                            if ((editTextReply.canScrollVertically(
                                    1) && editTextReply.canScrollVertically(
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
                            if (event.getY() - startY < 0 && editTextReply.canScrollVertically(1)) {
                                callback.getListener()
                                        .requestDisallowInterceptTouchEvent(true);
                            }
                            else if (event.getY() - startY > 0 && editTextReply.canScrollVertically(
                                    -1)) {
                                callback.getListener()
                                        .requestDisallowInterceptTouchEvent(true);
                            }
                            break;
                    }
                    return false;
                }
            });
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

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

                        layoutContainerReply.setVisibility(View.GONE);
//                        AnimationUtils.animateExpand(layoutContainerReply,
//                                getRatio(getAdapterPosition()), null);
                    }
                }
            });
            viewYouTube = (YouTubePlayerView) itemView.findViewById(R.id.youtube);

            final View.OnClickListener clickListenerLink = new View.OnClickListener() {
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
                        menu.findItem(R.id.item_download_image)
                                .setVisible(true);
                        menu.findItem(R.id.item_download_image)
                                .setEnabled(true);
                    }
                    else {
                        menu.findItem(R.id.item_download_image)
                                .setVisible(false);
                        menu.findItem(R.id.item_download_image)
                                .setEnabled(false);
                    }

                    boolean isAuthor = link.getAuthor()
                            .equals(callback.getUser().getName());

                    toolbarActions.getMenu()
                            .findItem(R.id.item_delete)
                            .setEnabled(isAuthor);
                    toolbarActions.getMenu()
                            .findItem(R.id.item_delete)
                            .setVisible(isAuthor);

                    AnimationUtils.animateExpand(layoutContainerExpand,
                            getRatio(getAdapterPosition()), null);

                }
            };

            this.itemView.setOnClickListener(clickListenerLink);

            this.imageThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = callback.getController()
                            .getLink(getAdapterPosition());
                    onClickThumbnail(link);
                }
            });

            View.OnTouchListener onTouchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.offsetLocation(v.getLeft(), v.getTop());
                    itemView.onTouchEvent(newEvent);
                    newEvent.recycle();
                    return false;
                }
            };
            textThreadSelf.setOnTouchListener(onTouchListener);
            buttonComments.setOnTouchListener(onTouchListener);
            imageThumbnail.setOnTouchListener(onTouchListener);

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
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);
//            AnimationUtils.animateExpand(layoutContainerReply, getRatio(getAdapterPosition()),
//                    null);
        }

        public abstract float getRatio(int adapterPosition);

        public void loadFull(final Link link) {

            Log.d(TAG, "loadFull: " + link.getUrl());

            // TODO: Toggle visibility of web and video views

            String urlString = link.getUrl();
            if (!TextUtils.isEmpty(urlString)) {
                if (link.getDomain()
                        .contains("imgur")) {
                    int startIndex;
                    int lastIndex;
                    if (urlString.contains("imgur.com/a/")) {
                        startIndex = urlString.indexOf("imgur.com/a/") + 12;
                        int slashIndex = urlString.substring(startIndex)
                                .indexOf("/") + startIndex;
                        lastIndex = slashIndex > startIndex ? slashIndex : urlString.length();
                        String imgurId = urlString.substring(startIndex, lastIndex);
                        loadAlbum(imgurId);
                    }
                    else if (urlString.contains("imgur.com/gallery/")) {
                        startIndex = urlString.indexOf("imgur.com/gallery/") + 18;
                        int slashIndex = urlString.substring(startIndex)
                                .indexOf("/") + startIndex;
                        lastIndex = slashIndex > startIndex ? slashIndex : urlString.length();
                        String imgurId = urlString.substring(startIndex, lastIndex);
                        loadGallery(imgurId);
                    }
                    else if (urlString.contains(Reddit.GIFV)) {
                        startIndex = urlString.indexOf("imgur.com/") + 10;
                        int dotIndex = urlString.substring(startIndex)
                                .indexOf(".") + startIndex;
                        lastIndex = dotIndex > startIndex ? dotIndex : urlString.length();
                        String imgurId = urlString.substring(startIndex, lastIndex);
                        loadGifv(imgurId);
                    }
                    else {
                        attemptLoadImage(link);
                    }
                }
                else if (link.getDomain()
                        .contains("gfycat")) {
                    int startIndex = urlString.indexOf("gfycat.com/") + 11;
                    int dotIndex = urlString.substring(startIndex)
                            .indexOf(".");
                    int lastIndex = dotIndex > startIndex ? dotIndex : urlString.length();
                    String gfycatId = urlString.substring(startIndex, lastIndex);
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
                else if (link.getDomain()
                        .contains("youtu")) {
                    Log.d(TAG, "youtube");
                    /*
                        Regex taken from Gubatron at
                        http://stackoverflow.com/questions/24048308/how-to-get-the-video-id-from-a-youtube-url-with-regex-in-java
                    */
                    Pattern pattern = Pattern.compile(
                            ".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
                    final Matcher matcher = pattern.matcher(urlString);
                    if (matcher.matches()) {
                        Log.d(TAG, "YouTube URL loaded: " + matcher.group(1));
                        final String id = matcher.group(1);
                        viewYouTube.initialize(ApiKeys.YOUTUBE_API_KEY,
                                new YouTubePlayer.OnInitializedListener() {
                                    @Override
                                    public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                            YouTubePlayer youTubePlayer,
                                            boolean b) {
                                        ViewHolderBase.this.youTubePlayer = youTubePlayer;
                                        youTubePlayer.setManageAudioFocus(false);
                                        youTubePlayer.loadVideo(id);
                                        viewYouTube.setVisibility(View.VISIBLE);
                                        callback.getListener()
                                                .onFullLoaded(getAdapterPosition());
                                    }

                                    @Override
                                    public void onInitializationFailure(YouTubePlayer.Provider provider,
                                            YouTubeInitializationResult youTubeInitializationResult) {
                                        attemptLoadImage(link);
                                    }
                                });
                    }
                    else {
                        attemptLoadImage(link);
                    }
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
                    textThreadSelf.setVisibility(View.VISIBLE);
                    textThreadSelf.setText(Reddit.getTrimmedHtml(link.getSelfTextHtml()));
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

                                        adapterAlbum.setAlbum(album);
                                        viewPagerFull.getLayoutParams().height = callback.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        if (ViewHolderBase.this instanceof AdapterLinkGrid.ViewHolder) {
                                            imageThumbnail.setVisibility(View.GONE);
                                            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                                                    RelativeLayout.START_OF,
                                                    buttonComments.getId());
                                            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(
                                                    0);
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

                                        adapterAlbum.setAlbum(album);
                                        viewPagerFull.getLayoutParams().height = callback.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        if (ViewHolderBase.this instanceof AdapterLinkGrid.ViewHolder) {
                                            imageThumbnail.setVisibility(View.GONE);
                                            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                                                    RelativeLayout.START_OF,
                                                    buttonComments.getId());
                                            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(
                                                    0);
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

            boolean loggedIn = !TextUtils.isEmpty(callback.getPreferences()
                    .getString(AppSettings.REFRESH_TOKEN, ""));

            menu.findItem(R.id.item_upvote).setVisible(loggedIn);
            menu.findItem(R.id.item_downvote).setVisible(loggedIn);
            menu.findItem(R.id.item_reply).setVisible(loggedIn);

            int maxNum = (int) (itemView.getWidth() / callback.getItemWidth());
            int numShown = 0;

            for (int index = 0; index < menu.size(); index++) {

                if (!loggedIn) {
                    switch (menu.getItem(index).getItemId()) {
                        case R.id.item_upvote:
                        case R.id.item_downvote:
                        case R.id.item_reply:
                            continue;
                    }
                }

                if (numShown++ < maxNum - 1) {
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

            if (youTubePlayer != null) {
                youTubePlayer.release();
                youTubePlayer = null;
            }
            viewYouTube.setVisibility(View.GONE);
            webFull.onPause();
            webFull.resetMaxHeight();
            webFull.setVisibility(View.GONE);
            videoFull.stopPlayback();
            videoFull.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);
            if (this instanceof AdapterLinkGrid.ViewHolder) {
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                        RelativeLayout.START_OF);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(
                        callback.getTitleMargin());
            }

            imageThumbnail.setImageBitmap(null);

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

            textThreadSelf.setVisibility(View.GONE);

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

        public String getFormatttedDate(long time) {
            calendar.setTimeInMillis(time);
            int minute = calendar.get(Calendar.MINUTE);
            return calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                    Locale.getDefault()) + " " + calendar.get(
                    Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.YEAR) + " " + calendar.get(
                    Calendar.HOUR_OF_DAY) + (minute < 10 ? ":0" : ":") + minute;
        }

        public void destroy() {
            if (imageContainer != null) {
                imageContainer.cancelRequest();
            }
            if (request != null) {
                request.cancel();
            }
            if (youTubePlayer != null) {
                youTubePlayer.release();
            }
            videoFull.stopPlayback();
            webFull.removeAllViews();
            webFull.destroy();
            adapterAlbum.destroyViews();
            for (int index = 0; index < viewPagerFull.getChildCount(); index++) {
                ((WebView) viewPagerFull.getChildAt(index)
                        .findViewById(R.id.web_image)).onPause();
                ((WebView) viewPagerFull.getChildAt(index)
                        .findViewById(R.id.web_image)).destroy();
            }
            viewPagerFull.setAdapter(null);
        }
    }

}
