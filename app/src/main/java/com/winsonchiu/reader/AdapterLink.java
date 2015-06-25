package com.winsonchiu.reader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
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
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.User;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_LINK_HEADER = 0;
    public static final int VIEW_LINK = 1;

    private static final String TAG = AdapterLink.class.getCanonicalName();

    protected Activity activity;
    protected LayoutManager layoutManager;
    protected ControllerLinksBase controllerLinks;
    protected ControllerCommentsBase controllerComments;
    protected float itemWidth;
    protected SharedPreferences preferences;
    protected List<RecyclerView.ViewHolder> viewHolders;
    protected User user;
    private int titleMargin;
    protected int recyclerHeight;

    protected ViewHolderHeader.EventListener eventListenerHeader;
    protected ViewHolderBase.EventListener eventListenerBase;
    protected DisallowListener disallowListener;
    protected ScrollCallback scrollCallback;
    protected ControllerUser controllerUser;

    public AdapterLink(ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener, ScrollCallback scrollCallback) {
        super();
        this.eventListenerHeader = eventListenerHeader;
        this.eventListenerBase = eventListenerBase;
        this.disallowListener = disallowListener;
        this.scrollCallback = scrollCallback;
        this.user = new User();
        viewHolders = new ArrayList<>();
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

    public void setControllers(ControllerLinksBase controllerLinks,
            ControllerCommentsBase controllerComments,
            ControllerUser controllerUser) {
        this.controllerLinks = controllerLinks;
        this.controllerComments = controllerComments;
        this.controllerUser = controllerUser;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
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
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        recyclerHeight = recyclerView.getHeight();
        super.onAttachedToRecyclerView(recyclerView);
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

    protected static class ViewHolderHeader extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private final EventListener eventListener;
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

        public ViewHolderHeader(View itemView,
                EventListener eventListener) {
            super(itemView);

            this.eventListener = eventListener;
            buttonShowSidebar = (ImageButton) itemView.findViewById(R.id.button_show_sidebar);
            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            layoutButtons = (LinearLayout) itemView.findViewById(R.id.layout_buttons);
            buttonSubmitLink = (Button) itemView.findViewById(R.id.button_submit_link);
            buttomSubmitSelf = (Button) itemView.findViewById(R.id.button_submit_self);
            layoutContainerExpand = (RelativeLayout) itemView
                    .findViewById(R.id.layout_container_expand);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            viewDivider = itemView.findViewById(R.id.view_divider);

            // TODO: Implement listener inside ViewHolder
            buttonShowSidebar.setOnClickListener(this);
            buttonSubmitLink.setOnClickListener(this);
            buttomSubmitSelf.setOnClickListener(this);
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

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_show_sidebar:
                    break;
                case R.id.button_submit_link:
                    eventListener.onClickSubmit(Reddit.POST_TYPE_LINK);
                    break;
                case R.id.button_submit_self:
                    eventListener.onClickSubmit(Reddit.POST_TYPE_SELF);
                    break;
                default:
                    AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
                    break;
            }
        }

        public interface EventListener {
            void onClickSubmit(String postType);
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
        protected Request request;
        protected String imageUrl;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected YouTubePlayerView viewYouTube;
        protected YouTubePlayer youTubePlayer;

        protected int viewPagerMargin;
        protected int toolbarItemWidth;
        protected int titleMargin;

        protected EventListener eventListener;
        protected DisallowListener disallowListener;
        protected ScrollCallback scrollCallback;
        protected int recyclerHeight;

        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemSave;
        protected MenuItem itemReply;
        protected MenuItem itemShare;
        protected MenuItem itemDownloadImage;
        protected MenuItem itemDelete;
        protected MenuItem itemViewSubreddit;
        protected MenuItem itemCopyText;
        protected PorterDuffColorFilter colorFilterSave;
        protected PorterDuffColorFilter colorFilterPositive;
        protected PorterDuffColorFilter colorFilterNegative;
        protected Drawable drawableDefault;
        protected boolean showSubreddit;
        protected String userName;

        public ViewHolderBase(View itemView,
                EventListener eventListener,
                DisallowListener disallowListener,
                ScrollCallback scrollCallback) {
            super(itemView);
            this.eventListener = eventListener;
            this.disallowListener = disallowListener;
            this.scrollCallback = scrollCallback;
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

                boolean isAuthor = link.getAuthor().equals(userName);

                itemDelete.setVisible(isAuthor);
                itemDelete.setEnabled(isAuthor);
                setToolbarMenuVisibility();
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

            eventListener.sendComment(link.getName(), editTextReply.getText().toString());
            layoutContainerReply.setVisibility(View.GONE);
        }

        protected void initialize() {

            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    itemView.getContext().getResources().getDisplayMetrics());
            titleMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    itemView.getContext().getResources().getDisplayMetrics());

            drawableDefault = itemView.getContext().getResources().getDrawable(
                    R.drawable.ic_web_white_48dp);
            mediaController = new MediaController(itemView.getContext());
            adapterAlbum = new AdapterAlbum(new Album(),
                    new OnTouchListenerDisallow(disallowListener));

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

            viewPagerMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96,
                    itemView.getContext().getResources().getDisplayMetrics());
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
        }

        protected void initializeListeners() {

            itemView.setOnClickListener(this);
            imageThumbnail.setOnClickListener(this);
            buttonComments.setOnClickListener(this);
            buttonSendReply.setOnClickListener(this);

            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());

            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
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
                    loadComments();
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

        protected void initializeToolbar() {

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
            itemCopyText = menu.findItem(R.id.item_copy_text);

            Resources resources = itemView.getContext().getResources();
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
                    eventListener.voteLink(ViewHolderBase.this, link, 1);
                    break;
                case R.id.item_downvote:
                    eventListener.voteLink(ViewHolderBase.this, link, -1);
                    break;
                case R.id.item_share:
                    break;
                case R.id.item_download_image:
                    eventListener.downloadImage(link);
                    break;
                case R.id.item_web:
                    eventListener.loadUrl(link.getUrl());
                    break;
                case R.id.item_reply:
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveLink(link);
                    break;
                case R.id.item_view_profile:
                    Intent intentViewProfile = new Intent(itemView.getContext(),
                            MainActivity.class);
                    intentViewProfile.setAction(Intent.ACTION_VIEW);
                    intentViewProfile.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/user/" + link.getAuthor());
                    eventListener.startActivity(intentViewProfile);
                    break;
                case R.id.item_copy_text:
                    ClipboardManager clipboard = (ClipboardManager) itemView.getContext().getSystemService(
                            Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(
                            itemView.getContext().getResources().getString(R.string.comment), link.getSelfText());
                    clipboard.setPrimaryClip(clip);
                    eventListener.toast(itemView.getContext().getResources().getString(R.string.copied));
                    break;
                case R.id.item_delete:
                    eventListener.deletePost(link);
                    break;
                case R.id.item_view_subreddit:
                    Intent intentViewSubreddit = new Intent(itemView.getContext(),
                            MainActivity.class);
                    intentViewSubreddit.setAction(Intent.ACTION_VIEW);
                    intentViewSubreddit.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/r/" + link.getSubreddit());
                    eventListener.startActivity(intentViewSubreddit);
                    break;
            }
            return true;
        }

        private void saveLink(final Link link) {
            eventListener.save(link);
            syncSaveIcon();
        }

        public void toggleReply() {
            expandFull(true);
            link.setReplyExpanded(!link.isReplyExpanded());
            if (link.isReplyExpanded()) {
                editTextReply.requestFocus();
                editTextReply.setText(null);
            }
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);
            scrollCallback.scrollTo(getAdapterPosition());
        }

        public abstract float getRatio(int adapterPosition);

        public void loadFull() {

            expandFull(true);
            // TODO: Toggle visibility of web and video views

            String urlString = link.getUrl();
            if (!TextUtils.isEmpty(urlString)) {
                if (!checkLinkUrl()) {
                    attemptLoadImage();
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
            request = eventListener.getReddit().loadGet(Reddit.GFYCAT_URL + gfycatId,
                    new Listener<String>() {
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
                if (textThreadSelf.isShown()) {
                    textThreadSelf.setVisibility(View.GONE);
                    // TODO: Check if textThreadSelf is taller than view and optimize animation
//                AnimationUtils.animateExpand(textThreadSelf, 1f, null);
                }
                else if (TextUtils.isEmpty(link.getSelfText())) {
                    loadComments();
                }
                else {
                    loadSelfText();
                }
            }
            else {
                loadFull();
            }
        }

        public void loadComments() {
            eventListener.onClickComments(link, this);
        }

        public void loadSelfText() {
            expandFull(true);
            textThreadSelf.setText(Reddit.getTrimmedHtml(link.getSelfTextHtml()));
            textThreadSelf.setVisibility(View.VISIBLE);
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

        public void attemptLoadImage() {

            if (Reddit.placeImageUrl(link)) {

                if (webFull == null) {
                    webFull = eventListener.getNewWebView(disallowListener);

                }
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(link.getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
                frameFull.addView(webFull);
                frameFull.requestLayout();
                scrollCallback.scrollTo(getAdapterPosition());
            }
            else {
                eventListener.loadUrl(link.getUrl());
            }
        }

        private void loadGallery(String id, final Link link) {
            // TODO: Check in onResponse whether the same link is currently attached

            progressImage.setVisibility(View.VISIBLE);
            request = eventListener.getReddit()
                    .loadImgurGallery(id,
                            new Listener<String>() {
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
            request = eventListener.getReddit()
                    .loadImgurAlbum(id,
                            new Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Album album = Album.fromJson(
                                                new JSONObject(response).getJSONObject("data"));
                                        setAlbum(link, album);
                                        Log.d(TAG, "link URL: " + link.getUrl());
                                        Log.d(TAG, "album URL: " + album.getLink());
                                        eventListener
                                                .toast("New album loaded: " + album.getTitle());
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
            layoutParams.height = recyclerHeight - viewPagerMargin;
            viewPagerFull.setLayoutParams(layoutParams);
            viewPagerFull.setVisibility(View.VISIBLE);
            viewPagerFull.requestLayout();
            adapterAlbum.setAlbum(album);
            scrollCallback.scrollTo(getAdapterPosition());
        }

        private void loadGifv(String id) {
            Log.d(TAG, "loadGifv: " + id);
            request = eventListener.getReddit()
                    .loadImgurImage(id,
                            new Listener<String>() {
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
            scrollCallback.scrollTo(getAdapterPosition());
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
                            youTubePlayer.setFullscreenControlFlags(
                                    YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);
                            viewYouTube.setVisibility(View.VISIBLE);
                            scrollCallback.scrollTo(getAdapterPosition());
                        }

                        @Override
                        public void onInitializationFailure(YouTubePlayer.Provider provider,
                                YouTubeInitializationResult youTubeInitializationResult) {
                            eventListener.toast(itemView.getContext().getResources().getString(R.string.error_youtube));
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

            boolean loggedIn = eventListener.isUserLoggedIn();

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
            itemViewSubreddit.setVisible(showSubreddit);
            itemViewSubreddit.setEnabled(showSubreddit);
            itemCopyText.setVisible(link.isSelf());
            itemCopyText.setEnabled(link.isSelf());

            int maxNum = itemView.getWidth() / toolbarItemWidth;
            int numShown = 0;

            for (int index = 0; index < menu.size(); index++) {

                MenuItem menuItem = menu.getItem(index);

                if (!menuItem.isVisible()) {
                    continue;
                }

                if (numShown++ < maxNum - 1) {
                    menuItem
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    menuItem
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }

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

        public void onBind(Link link, boolean showSubreddit, int recyclerHeight, String userName) {
            this.link = link;
            this.showSubreddit = showSubreddit;
            this.recyclerHeight = recyclerHeight;
            this.userName = userName;
            layoutContainerExpand.setVisibility(View.GONE);
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);

            textThreadSelf.setVisibility(View.GONE);
            adapterAlbum.setAlbum(null);
            syncSaveIcon();

        }

        public void syncSaveIcon() {
            if (link.isSaved()) {
                itemSave.setTitle(itemView.getContext().getResources().getString(R.string.unsave));
                itemSave.getIcon()
                        .setColorFilter(colorFilterSave);
            }
            else {
                itemSave.setTitle(itemView.getContext().getResources().getString(R.string.save));
                itemSave.getIcon()
                        .clearColorFilter();
            }
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
            frameFull.requestLayout();
        }

        public int getBackgroundColor() {
            if (itemView.getBackground() instanceof ColorDrawable) {
                return ((ColorDrawable) itemView.getBackground()).getColor();
            }
            return 0x00000000;
        }

        public interface EventListener {

            void sendComment(String name, String text);
            void onClickComments(Link link, ViewHolderBase viewHolderBase);
            void save(Link link);
            void save(Comment comment);
            void loadUrl(String url);
            void downloadImage(Link link);
            Reddit getReddit();
            WebViewFixed getNewWebView(DisallowListener disallowListener);
            void toast(String text);
            boolean isUserLoggedIn();
            void voteLink(ViewHolderBase viewHolderBase, Link link, int vote);
            void startActivity(Intent intent);
            void deletePost(Link link);
        }

    }

}
