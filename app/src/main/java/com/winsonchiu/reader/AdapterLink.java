package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    protected List<RecyclerView.ViewHolder> viewHolders;
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

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return listener;
    }

    @Override
    public ControllerLinksBase getControllerLinks() {
        return controllerLinks;
    }

    @Override
    public ControllerCommentsBase getControllerComments() {
        return listener.getControllerComments();
    }

    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public int getTitleMargin() {
        return titleMargin;
    }

    @Override
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    @Override
    public void pauseViewHolders() {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof ViewHolderBase) {
                ((ViewHolderBase) viewHolder).videoFull.stopPlayback();
            }
        }
    }

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

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        if (holder instanceof ViewHolderBase) {
            ((ViewHolderBase) holder).onRecycle();
        }

        super.onViewRecycled(holder);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        viewHolders.add(holder);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        viewHolders.remove(holder);
        if (holder instanceof ViewHolderBase) {
            ((ViewHolderBase) holder).destroyWebViews();
        }
    }

    protected static class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected LinearLayout layoutButtons;
        protected Button buttonSubmitLink;
        protected Button buttomSubmitSelf;
        protected RelativeLayout layoutContainerExpand;
        protected TextView textHidden;
        protected View viewDivider;
        protected ImageButton buttonShowSidebar;

        private String defaultTextSubmitLink;
        private String defaultTextSubmitText;

        private ControllerLinks.ListenerCallback callback;

        public ViewHolderHeader(View itemView,
                final ControllerLinks.ListenerCallback listenerCallback) {
            super(itemView);

            callback = listenerCallback;
            buttonShowSidebar = (ImageButton) itemView.findViewById(R.id.button_show_sidebar);
            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            layoutButtons = (LinearLayout) itemView.findViewById(R.id.layout_buttons);
            buttonSubmitLink = (Button) itemView.findViewById(R.id.button_submit_link);
            buttomSubmitSelf = (Button) itemView.findViewById(R.id.button_submit_self);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(R.id.layout_container_expand);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            viewDivider = itemView.findViewById(R.id.view_divider);

            // TODO: Implement listener inside ViewHolder
            buttonShowSidebar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.getListener().showSidebar();
                }
            });
            buttonSubmitLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.getListener()
                            .onClickSubmit(Reddit.POST_TYPE_LINK);
                }
            });
            buttomSubmitSelf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.getListener()
                            .onClickSubmit(Reddit.POST_TYPE_SELF);
                }
            });
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
                }
            });
            textDescription.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.offsetLocation(v.getLeft(), v.getTop());
                    ViewHolderHeader.this.itemView.onTouchEvent(newEvent);
                    newEvent.recycle();
                    return false;
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
            textHidden.setVisibility(visibility);
            layoutButtons.setVisibility(visibility);
            buttonShowSidebar.setVisibility(visibility);
            buttonSubmitLink.setVisibility(visibility);
            buttomSubmitSelf.setVisibility(visibility);
            viewDivider.setVisibility(visibility);
        }

        public void onBind(Subreddit subreddit) {

            if (TextUtils.isEmpty(subreddit.getDisplayName()) || "/r/all/".equalsIgnoreCase(
                    subreddit.getUrl()) || subreddit.getUrl().contains("+")) {
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

            textHidden.setText(subreddit.getSubscribers() + " subscribers\n" +
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

            if (!subreddit.isUserIsContributor()) {
                buttomSubmitSelf.setVisibility(View.GONE);
                buttonSubmitLink.setVisibility(View.GONE);
            }
            else if (Reddit.POST_TYPE_LINK.equalsIgnoreCase(subreddit.getSubmissionType())) {
                buttomSubmitSelf.setVisibility(View.GONE);
            }
            else if (Reddit.POST_TYPE_SELF.equalsIgnoreCase(subreddit.getSubmissionType())) {
                buttonSubmitLink.setVisibility(View.GONE);
            }

        }
    }

    protected static abstract class ViewHolderBase extends RecyclerView.ViewHolder
            implements Toolbar.OnMenuItemClickListener, View.OnClickListener {

        protected Link link;

        protected FrameLayout frameFull;
        protected MediaController mediaController;
        protected VideoView videoFull;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected AdapterAlbum adapterAlbum;
        protected ImageView imagePlay;
        protected ImageView imageThumbnail;
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
        protected Request request;
        protected String imageUrl;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected YouTubePlayerView viewYouTube;
        protected YouTubePlayer youTubePlayer;
        protected int viewPagerMargin;

        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemSave;
        protected MenuItem itemReply;
        protected MenuItem itemShare;
        protected MenuItem itemDownloadImage;
        protected MenuItem itemDelete;
        protected MenuItem itemViewSubreddit;
        protected PorterDuffColorFilter colorFilterSave;
        protected PorterDuffColorFilter colorFilterPositive;
        protected PorterDuffColorFilter colorFilterNegative;
        protected Drawable drawableDefault;

        public ViewHolderBase(View itemView,
                ControllerLinks.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;
            initialize();
            initializeToolbar();
            initializeListeners();
        }

        private void expandToolbarActions() {

            if (!toolbarActions.isShown()) {
                setVoteColors();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());

                setToolbarMenuVisibility();
                ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat
                        .getActionProvider(
                                itemShare);
                if (shareActionProvider != null) {
                    shareActionProvider.setShareIntent(shareIntent);
                }

                if (Reddit.checkIsImage(link.getUrl()) || Reddit.placeImageUrl(link)) {
                    itemDownloadImage.setVisible(true);
                    itemDownloadImage.setEnabled(true);
                }
                else {
                    itemDownloadImage.setVisible(false);
                    itemDownloadImage.setEnabled(false);
                }

                boolean isAuthor = link.getAuthor()
                        .equals(callback.getUser()
                                .getName());

                itemDelete.setVisible(isAuthor);
                itemDelete.setEnabled(isAuthor);
            }

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {
                    AnimationUtils.animateExpand(layoutContainerExpand,
                            getRatio(getAdapterPosition()), null);
                }
            });
        }

        private void sendComment() {
            // TODO: Move add to immediate on button click, check if failed afterwards

            Map<String, String> params = new HashMap<>();
            params.put("api_type", "json");
            params.put("text", editTextReply.getText()
                    .toString());
            params.put("thing_id", link.getName());

            request = callback.getControllerLinks()
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
        }

        private void initialize() {

            drawableDefault = callback.getControllerComments().getActivity().getResources().getDrawable(
                    R.drawable.ic_web_white_48dp);
            mediaController = new MediaController(itemView.getContext());
            adapterAlbum = new AdapterAlbum(callback.getControllerLinks()
                    .getActivity(), new Album(),
                    new OnTouchListenerDisallow(callback.getListener()));

            progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            imagePlay = (ImageView) itemView.findViewById(R.id.image_play);
            videoFull = (VideoView) itemView.findViewById(R.id.video_full);
            frameFull = (FrameLayout) itemView.findViewById(R.id.frame_full);
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            imageThumbnail = (ImageView) itemView.findViewById(R.id.image_thumbnail);
            textThreadFlair = (TextView) itemView.findViewById(R.id.text_thread_flair);
            textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            textThreadSelf = (TextView) itemView.findViewById(R.id.text_thread_self);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            viewYouTube = (YouTubePlayerView) itemView.findViewById(R.id.youtube);

            viewPagerMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96, callback.getControllerLinks().getActivity().getResources().getDisplayMetrics());
            viewPagerFull.setAdapter(adapterAlbum);
            viewPagerFull.setMinimumHeight(viewPagerMargin);
            viewPagerFull.setPageTransformer(false, new ViewPager.PageTransformer() {
                @Override
                public void transformPage(View page, float position) {
                    if (page.getTag() instanceof AdapterAlbum.ViewHolder) {
                        AdapterAlbum.ViewHolder viewHolder = (AdapterAlbum.ViewHolder) page
                                .getTag();
                        if (position >= -1 && position <= 1) {
                            viewHolder.textAlbumIndicator.setTranslationX(
                                    -position * page.getWidth());
                        }
                    }
                }
            });

//            webFull = new WebViewFixed(callback.getControllerLinks()
//                    .getActivity()
//                    .getApplicationContext());
//            Reddit.incrementCreate();
//            webFull.getSettings()
//                    .setUseWideViewPort(true);
//            webFull.getSettings()
//                    .setLoadWithOverviewMode(true);
//            webFull.getSettings()
//                    .setBuiltInZoomControls(true);
//            webFull.getSettings()
//                    .setDisplayZoomControls(false);
//            webFull.setBackgroundColor(0x000000);
//            webFull.setWebChromeClient(null);
//            webFull.setWebViewClient(new WebViewClient() {
//                @Override
//                public void onScaleChanged(WebView view, float oldScale, float newScale) {
//                    webFull.lockHeight();
//                    super.onScaleChanged(view, oldScale, newScale);
//                }
//
//                @Override
//                public void onReceivedError(WebView view,
//                        int errorCode,
//                        String description,
//                        String failingUrl) {
//                    super.onReceivedError(view, errorCode, description, failingUrl);
//                    Toast.makeText(callback.getControllerLinks()
//                            .getActivity(), "WebView error: " + description, Toast.LENGTH_SHORT)
//                            .show();
//                }
//            });
//            webFull.setOnTouchListener(new OnTouchListenerDisallow(callback.getListener()));
//            frameFull.addView(webFull);
        }

        private void initializeListeners() {

            itemView.setOnClickListener(this);
            imageThumbnail.setOnClickListener(this);
            buttonComments.setOnClickListener(this);
            buttonSendReply.setOnClickListener(this);

            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());

            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(callback.getListener()));
            textThreadSelf.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.offsetLocation(v.getLeft(), v.getTop());
                    ViewHolderBase.this.itemView.onTouchEvent(newEvent);
                    newEvent.recycle();
                    return false;
                }
            });

            videoFull.setMediaController(mediaController);
            videoFull.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaController.hide();
                }
            });
            videoFull.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // TODO: Use custom MediaController
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (mediaController.isShowing()) {
                            mediaController.hide();
                        }
                        else {
                            mediaController.show();
                        }
                    }
                    return true;
                }
            });
            mediaController.setAnchorView(videoFull);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    callback.pauseViewHolders();
                    callback.getListener()
                            .onClickComments(link, ViewHolderBase.this);
                    break;
                case R.id.image_thumbnail:
                    onClickThumbnail();
                    break;
                case R.id.text_thread_self:
                    break;
                case R.id.button_send_reply:
                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        sendComment();
                    }
                    break;
                default:
                    expandToolbarActions();
                    break;
            }
        }

        private void initializeToolbar() {

            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_link);
            toolbarActions.setOnMenuItemClickListener(this);

            Menu menu = toolbarActions.getMenu();
            itemUpvote = menu.findItem(R.id.item_upvote);
            itemDownvote = menu.findItem(R.id.item_downvote);
            itemSave = menu.findItem(R.id.item_save);
            itemReply = menu.findItem(R.id.item_reply);
            itemShare = menu.findItem(R.id.item_share);
            itemDownloadImage = menu.findItem(R.id.item_download_image);
            itemDelete = menu.findItem(R.id.item_delete);
            itemViewSubreddit = menu.findItem(R.id.item_view_subreddit);

            Resources resources = callback.getControllerLinks().getActivity().getResources();
            colorFilterPositive = new PorterDuffColorFilter(resources.getColor(
                    R.color.positiveScore),
                    PorterDuff.Mode.MULTIPLY);
            colorFilterNegative = new PorterDuffColorFilter(resources.getColor(
                    R.color.negativeScore),
                    PorterDuff.Mode.MULTIPLY);
            colorFilterSave = new PorterDuffColorFilter(resources.getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.item_upvote:
                    callback.getControllerLinks()
                            .voteLink(ViewHolderBase.this, link, 1);
                    break;
                case R.id.item_downvote:
                    callback.getControllerLinks()
                            .voteLink(ViewHolderBase.this, link, -1);
                    break;
                case R.id.item_share:
                    break;
                case R.id.item_download_image:
                    downloadImage(link.getUrl());
                    break;
                case R.id.item_web:
                    callback.getListener()
                            .loadUrl(link.getUrl());
                    break;
                case R.id.item_reply:
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveLink(link);
                    break;
                case R.id.item_view_profile:
                    Intent intentViewProfile = new Intent(callback.getControllerComments()
                            .getActivity(), MainActivity.class);
                    intentViewProfile.setAction(Intent.ACTION_VIEW);
                    intentViewProfile.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/user/" + link.getAuthor());
                    callback.getControllerComments()
                            .getActivity()
                            .startActivity(intentViewProfile);
                    break;
                case R.id.item_delete:
                    callback.getControllerLinks()
                            .deletePost(link);
                    break;
                case R.id.item_view_subreddit:
                    Intent intentViewSubreddit = new Intent(callback.getControllerLinks()
                            .getActivity(), MainActivity.class);
                    intentViewSubreddit.setAction(Intent.ACTION_VIEW);
                    intentViewSubreddit.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/r/" + link.getSubreddit());
                    callback.getControllerLinks()
                            .getActivity()
                            .startActivity(intentViewSubreddit);
                    break;
            }
            return true;
        }

        private void saveLink(final Link link) {
            if (link.isSaved()) {
                callback.getControllerLinks()
                        .getReddit()
                        .unsave(link);
            }
            else {
                callback.getControllerLinks()
                        .getReddit()
                        .save(link, "",
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        link.setSaved(false);
                                        syncSaveIcon();
                                    }
                                });
            }
            link.setSaved(!link.isSaved());
            syncSaveIcon();
        }

        public void toggleReply() {
            if (TextUtils.isEmpty(callback.getPreferences()
                    .getString(AppSettings.REFRESH_TOKEN, ""))) {
                Toast.makeText(callback.getControllerLinks()
                                .getActivity(), callback.getControllerLinks()
                                .getActivity()
                                .getString(R.string.must_be_logged_in_to_reply),
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            expandFull(true);
            link.setReplyExpanded(!link.isReplyExpanded());
            if (link.isReplyExpanded()) {
                editTextReply.requestFocus();
                editTextReply.setText(null);
            }
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);
            callback.getListener()
                    .onFullLoaded(getAdapterPosition());
        }

        public abstract float getRatio(int adapterPosition);

        public void loadFull(final Link link) {

            expandFull(true);
            // TODO: Toggle visibility of web and video views

            String urlString = link.getUrl();
            if (!TextUtils.isEmpty(urlString)) {
                if (!checkLinkUrl()) {
                    attemptLoadImage(link);
                }
            }
        }

        /**
         * @return true if Link loading has been handled, false otherwise
         */
        public boolean checkLinkUrl() {

            if (link.getDomain()
                    .contains("imgur")) {
                return loadImgur();
            }
            else if (link.getDomain()
                    .contains("gfycat")) {
                return loadGfycat();
            }
            else if (link.getDomain()
                    .contains("youtu")) {
                return loadYouTube();
            }

            return false;
        }

        public boolean loadYouTube() {

            if (viewYouTube.isShown()) {
                hideYouTube();
                return true;
            }

            if (youTubePlayer != null) {
                viewYouTube.setVisibility(View.VISIBLE);
                return true;
            }
            /*
                Regex taken from Gubatron at
                http://stackoverflow.com/questions/24048308/how-to-get-the-video-id-from-a-youtube-url-with-regex-in-java
            */
            Pattern pattern = Pattern.compile(
                    ".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
            final Matcher matcher = pattern.matcher(link.getUrl());
            if (matcher.matches()) {
                loadYouTubeVideo(link, matcher.group(1));
                return true;
            }

            return false;
        }

        private boolean loadGfycat() {
            String gfycatId = Reddit.parseUrlId(link.getUrl(), Reddit.GFYCAT_PREFIX, ".");
            progressImage.setVisibility(View.VISIBLE);
            request = callback.getControllerLinks()
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
            return true;
        }

        /**
         * @return true if Link loading has been handled, false otherwise
         */
        private boolean loadImgur() {
            // TODO: Use regex or better parsing system
            if (link.getUrl().contains(Reddit.IMGUR_PREFIX_ALBUM)) {
                if (link.getAlbum() == null) {
                    loadAlbum(Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX_ALBUM, "/"),
                            link);
                }
                else {
                    setAlbum(link, link.getAlbum());
                }
            }
            else if (link.getUrl().contains(Reddit.IMGUR_PREFIX_GALLERY)) {
                if (link.getAlbum() == null) {
                    loadGallery(
                            Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX_GALLERY, "/"),
                            link);
                }
                else {
                    setAlbum(link, link.getAlbum());
                }
            }
            else if (link.getUrl().contains(Reddit.GIFV)) {
                loadGifv(Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX, "."));
            }
            else {
                return false;
            }

            return true;
        }

        public void onClickThumbnail() {
            if (link.isSelf()) {
                loadSelfText(link);
            }
            else {
                loadFull(link);
            }
        }

        public void loadSelfText(Link link) {
            if (textThreadSelf.isShown()) {
                textThreadSelf.setVisibility(View.GONE);
                // TODO: Check if textThreadSelf is taller than view and optimize animation
//                AnimationUtils.animateExpand(textThreadSelf, 1f, null);
            }
            else if (TextUtils.isEmpty(link.getSelfText())) {
                callback.pauseViewHolders();
                callback.getListener()
                        .onClickComments(link, this);
            }
            else {
                expandFull(true);
                textThreadSelf.setText(Reddit.getTrimmedHtml(link.getSelfTextHtml()));
                textThreadSelf.setVisibility(View.VISIBLE);
//                AnimationUtils.animateExpand(textThreadSelf, 1f, null);
            }
        }

        public void expandFull(boolean expand) {
        }

        public void setVoteColors() {

            switch (link.isLikes()) {
                case 1:
                    itemUpvote.getIcon().setColorFilter(colorFilterPositive);
                    itemDownvote.getIcon().clearColorFilter();
                    break;
                case -1:
                    itemDownvote.getIcon().setColorFilter(colorFilterNegative);
                    itemUpvote.getIcon().clearColorFilter();
                    break;
                case 0:
                    itemUpvote.getIcon().clearColorFilter();
                    itemDownvote.getIcon().clearColorFilter();
                    break;
            }
        }

        public void setTextInfo(Link link) {

        }

        public void attemptLoadImage(Link link) {

            if (Reddit.placeImageUrl(link)) {

                if (webFull == null) {

                    webFull = new WebViewFixed(callback.getControllerLinks()
                            .getActivity()
                            .getApplicationContext());
                    Reddit.incrementCreate();
                    webFull.getSettings()
                            .setUseWideViewPort(true);
                    webFull.getSettings()
                            .setLoadWithOverviewMode(true);
                    webFull.getSettings()
                            .setBuiltInZoomControls(true);
                    webFull.getSettings()
                            .setDisplayZoomControls(false);
                    webFull.setBackgroundColor(0x000000);
                    webFull.setWebChromeClient(null);
                    webFull.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onScaleChanged(WebView view, float oldScale, float newScale) {
                            webFull.lockHeight();
                            super.onScaleChanged(view, oldScale, newScale);
                        }

                        @Override
                        public void onReceivedError(WebView view,
                                int errorCode,
                                String description,
                                String failingUrl) {
                            super.onReceivedError(view, errorCode, description, failingUrl);
                            Toast.makeText(callback.getControllerLinks()
                                            .getActivity(), "WebView error: " + description,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                    webFull.setOnTouchListener(new OnTouchListenerDisallow(callback.getListener()));
                    frameFull.addView(webFull);
                    frameFull.requestLayout();
                    callback.getListener()
                            .onFullLoaded(getAdapterPosition());
                }
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(link.getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
            }
            else {
                callback.getListener()
                        .loadUrl(link.getUrl());
            }
        }

        private void loadGallery(String id, final Link link) {
            // TODO: Check in onResponse whether the same link is currently attached

            progressImage.setVisibility(View.VISIBLE);
            request = callback.getControllerLinks()
                    .getReddit()
                    .loadImgurGallery(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        setAlbum(link, Album.fromJson(
                                                new JSONObject(response).getJSONObject("data")));
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

        private void loadAlbum(String id, final Link link) {
            // TODO: Check in onResponse whether the same link is currently attached

            progressImage.setVisibility(View.VISIBLE);
            request = callback.getControllerLinks()
                    .getReddit()
                    .loadImgurAlbum(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Album album = Album.fromJson(
                                                new JSONObject(response).getJSONObject("data"));
                                        setAlbum(link, album);
                                        Log.d(TAG, "link URL: " + link.getUrl());
                                        Log.d(TAG, "album URL: " + album.getLink());
                                        Toast.makeText(callback.getControllerLinks()
                                                        .getActivity(),
                                                "New album loaded: " + album.getTitle(),
                                                Toast.LENGTH_SHORT)
                                                .show();
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

        public void setAlbum(Link link, Album album) {
            link.setAlbum(album);
            viewPagerFull.setCurrentItem(0);
            ViewGroup.LayoutParams layoutParams = viewPagerFull.getLayoutParams();
            layoutParams.height = callback.getListener()
                    .getRecyclerHeight() - viewPagerMargin;
            viewPagerFull.setLayoutParams(layoutParams);
            viewPagerFull.setVisibility(View.VISIBLE);
            viewPagerFull.requestLayout();
            adapterAlbum.setAlbum(album);
            callback.getListener()
                    .onFullLoaded(getAdapterPosition());
        }

        private void loadGifv(String id) {
            Log.d(TAG, "loadGifv: " + id);
            request = callback.getControllerLinks()
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
            videoFull.getLayoutParams().height = (int) (ViewHolderBase.this.itemView
                    .getWidth() * heightRatio);
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

        public void loadYouTubeVideo(final Link link, final String id) {
            viewYouTube.initialize(ApiKeys.YOUTUBE_API_KEY,
                    new YouTubePlayer.OnInitializedListener() {
                        @Override
                        public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                final YouTubePlayer youTubePlayer,
                                boolean b) {
                            ViewHolderBase.this.youTubePlayer = youTubePlayer;
                            youTubePlayer.setManageAudioFocus(false);
                            youTubePlayer.loadVideo(id);
                            if (callback.getListener().getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                youTubePlayer.setOnFullscreenListener(
                                        new YouTubePlayer.OnFullscreenListener() {
                                            @Override
                                            public void onFullscreen(boolean b) {
                                                callback.getListener().loadVideoLandscape(getAdapterPosition());
                                            }
                                        });
                            }
                            else {
                                youTubePlayer.setFullscreen(true);
                                youTubePlayer.setOnFullscreenListener(
                                        new YouTubePlayer.OnFullscreenListener() {
                                            @Override
                                            public void onFullscreen(boolean fullscreen) {
                                                youTubePlayer.setFullscreen(fullscreen);
                                            }
                                        });
                            }
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

        private void hideYouTube() {
            if (youTubePlayer != null) {
                youTubePlayer.pause();
            }
            viewYouTube.setVisibility(View.GONE);
        }

        public void setToolbarMenuVisibility() {
            Menu menu = toolbarActions.getMenu();

            boolean loggedIn = !TextUtils.isEmpty(callback.getPreferences()
                    .getString(AppSettings.REFRESH_TOKEN, ""));

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);

            int maxNum = (int) (itemView.getWidth() / callback.getItemWidth());
            int numShown = 0;

            for (int index = 0; index < menu.size(); index++) {

                if (!loggedIn) {
                    switch (menu.getItem(index)
                            .getItemId()) {
                        case R.id.item_upvote:
                        case R.id.item_downvote:
                        case R.id.item_reply:
                        case R.id.item_view_subreddit:
                        case R.id.item_save:
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

            boolean showSubreddit = callback.getControllerLinks().showSubreddit();

            itemViewSubreddit.setVisible(showSubreddit);
            itemViewSubreddit.setEnabled(showSubreddit);

            syncSaveIcon();
        }

        public void onRecycle() {

            if (request != null) {
                request.cancel();
            }

            if (youTubePlayer != null) {
                youTubePlayer.release();
                youTubePlayer = null;
            }
            viewYouTube.setVisibility(View.GONE);
//            webFull.loadData(Reddit.getImageHtml(""), "text/html", "UTF-8");
//            webFull.onPause();
//            webFull.resetMaxHeight();
//            webFull.setVisibility(View.GONE);
            videoFull.stopPlayback();
            videoFull.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imagePlay.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);
            imageThumbnail.setImageBitmap(null);

        }

        public void onBind(Link link) {
            this.link = link;
            layoutContainerExpand.setVisibility(View.GONE);
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);

            textThreadSelf.setVisibility(View.GONE);
            adapterAlbum.setAlbum(null);
            syncSaveIcon();

        }

        public void downloadImage(String url) {
            Picasso.with(callback.getControllerLinks()
                    .getActivity())
                    .load(url)
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            File file = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/ReaderForReddit/" + System
                                    .currentTimeMillis() + ".png");

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

                            Toast.makeText(callback.getControllerLinks()
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

        public void syncSaveIcon() {
            if (link.isSaved()) {
                itemSave.setTitle(callback.getControllerLinks()
                        .getActivity()
                        .getString(R.string.unsave));
                itemSave.getIcon()
                        .setColorFilter(colorFilterSave);
            }
            else {
                itemSave.setTitle(callback.getControllerLinks()
                        .getActivity()
                        .getString(R.string.save));
                itemSave.getIcon()
                        .clearColorFilter();
            }
        }

        public void destroy() {
            if (request != null) {
                request.cancel();
            }
            if (youTubePlayer != null) {
                youTubePlayer.release();
            }
            videoFull.stopPlayback();
            if (webFull != null) {
                webFull.removeAllViews();
                webFull.destroy();
                Reddit.incrementDestroy();
                webFull = null;
            }
            if (viewPagerFull.getChildCount() > 0) {
                for (int index = 0; index < viewPagerFull.getChildCount(); index++) {
                    View view = viewPagerFull.getChildAt(index);
                    WebView webView = (WebView) view.findViewById(R.id.web);
                    if (webView != null) {
                        webView.onPause();
                        webView.destroy();
                        Reddit.incrementDestroy();
                        ((RelativeLayout) view).removeView(webView);
                    }
                }
            }
            adapterAlbum.setAlbum(null);
            viewPagerFull.removeAllViews();
        }

        public void destroyWebViews() {

            if (webFull != null) {
                frameFull.removeView(webFull);
                webFull.removeAllViews();
                webFull.destroy();
                Reddit.incrementDestroy();
                webFull = null;
            }
            if (viewPagerFull.getChildCount() > 0) {
                for (int index = 0; index < viewPagerFull.getChildCount(); index++) {
                    View view = viewPagerFull.getChildAt(index);
                    WebView webView = (WebView) view.findViewById(R.id.web);
                    if (webView != null) {
                        webView.onPause();
                        webView.destroy();
                        Reddit.incrementDestroy();
                        ((RelativeLayout) view).removeView(webView);
                    }
                }
            }
            adapterAlbum.setAlbum(null);
            viewPagerFull.removeAllViews();
        }

    }

}
