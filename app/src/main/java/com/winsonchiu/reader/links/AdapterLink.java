/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
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

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.FinalizingSubscriber;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.SimplePlayerStateChangeListener;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.views.WebViewFixed;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_LINK_HEADER = 0;
    public static final int VIEW_LINK = 1;

    private static final String TAG = AdapterLink.class.getCanonicalName();
    private static final int TIMESTAMP_BITMASK = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
    public static final String TAG_PICASSO = "picassoAdapterLink";

    protected Activity activity;
    protected SharedPreferences preferences;
    protected LayoutManager layoutManager;
    protected ControllerLinksBase controllerLinks;
    protected List<RecyclerView.ViewHolder> viewHolders;

    protected ViewHolderHeader.EventListener eventListenerHeader;
    protected ViewHolderBase.EventListener eventListenerBase;
    protected DisallowListener disallowListener;
    protected RecyclerCallback recyclerCallback;

    @Inject ControllerUser controllerUser;

    public AdapterLink(Activity activity, ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener, RecyclerCallback recyclerCallback) {
        this.eventListenerHeader = eventListenerHeader;
        this.eventListenerBase = eventListenerBase;
        this.disallowListener = disallowListener;
        this.recyclerCallback = recyclerCallback;
        viewHolders = new ArrayList<>();

        ((MainActivity) activity).getComponentActivity().inject(this);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public void setController(ControllerLinksBase controllerLinks) {
        this.controllerLinks = controllerLinks;
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
        return controllerLinks.sizeLinks() + 1;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (!controllerLinks.isLoading() && position > controllerLinks.sizeLinks() - 5) {
            controllerLinks.loadMoreLinks()
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }
        Reddit.incrementBind();
        viewHolders.add(holder);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ViewHolderBase) {
            ((ViewHolderBase) holder).onRecycle();
        }

        Reddit.incrementRecycled();
        viewHolders.remove(holder);
    }

    public void setVisibility(int visibility) {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            viewHolder.itemView.setVisibility(visibility);
        }
    }

    public void setVisibility(int visibility, @NonNull Thing thing) {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (getItemViewType(viewHolder.getAdapterPosition()) == VIEW_LINK && thing.equals(((ViewHolderBase) viewHolder).link)) {
                viewHolder.itemView.setVisibility(visibility);
            }
        }
    }

    public void pauseViewHolders() {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof AdapterLink.ViewHolderBase) {
                AdapterLink.ViewHolderBase viewHolderBase = (AdapterLink.ViewHolderBase) viewHolder;
                if (viewHolderBase.mediaPlayer != null) {
                    viewHolderBase.mediaPlayer.stop();
                }
            }
        }
    }

    public boolean navigateBack() {
        boolean navigateBack = true;

        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof ViewHolderBase) {

                ViewHolderBase viewHolderBase = (ViewHolderBase) viewHolder;

                if (viewHolderBase.youTubePlayer != null) {
                    if (viewHolderBase.isYouTubeFullscreen) {
                        viewHolderBase.youTubePlayer.setFullscreen(false);
                        navigateBack = false;
                        break;
                    }
                }
            }
        }

        return navigateBack;
    }

    public void destroyViewHolders() {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            if (viewHolder instanceof ViewHolderBase) {
                Reddit.incrementRecycled();
                ((ViewHolderBase) viewHolder).onRecycle();
            }
        }
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder
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

            textDescription.setOnClickListener(this);
            buttonShowSidebar.setOnClickListener(this);
            buttonSubmitLink.setOnClickListener(this);
            buttomSubmitSelf.setOnClickListener(this);
            itemView.setOnClickListener(this);

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

            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(
                    new int[] {R.attr.colorIconFilter});
            buttonShowSidebar.setColorFilter(typedArray.getColor(0, 0xFFFFFFFF),
                    PorterDuff.Mode.MULTIPLY);
            typedArray.recycle();

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
        }

        public void onBind(Subreddit subreddit) {
            if (TextUtils.isEmpty(subreddit.getDisplayName()) || "/r/all/".equalsIgnoreCase(
                    subreddit.getUrl()) || subreddit.getUrl().contains("+")) {
                setVisibility(View.GONE);
                return;
            }
            setVisibility(View.VISIBLE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(Html.fromHtml(subreddit.getTitle()));

            if (TextUtils.isEmpty(subreddit.getPublicDescription())) {
                textDescription.setText("");
                textDescription.setVisibility(View.GONE);
            }
            else {
                textDescription.setText(Reddit.getFormattedHtml(subreddit.getPublicDescriptionHtml()));
                textDescription.setVisibility(View.VISIBLE);
            }

            textHidden.setText(itemView.getResources().getString(R.string.subreddit_info, subreddit.getSubscribers(), new Date(subreddit.getCreatedUtc())));

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

            if (Reddit.PostType.fromString(subreddit.getSubmissionType()) == Reddit.PostType.LINK) {
                buttomSubmitSelf.setVisibility(View.GONE);
            }
            else {
                buttonSubmitLink.setVisibility(View.GONE);
            }

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_show_sidebar:
                    eventListener.showSidebar();
                    break;
                case R.id.button_submit_link:
                    eventListener.onClickSubmit(Reddit.PostType.LINK);
                    break;
                case R.id.button_submit_self:
                    eventListener.onClickSubmit(Reddit.PostType.SELF);
                    break;
                default:
                    UtilsAnimation.animateExpand(layoutContainerExpand, 1f, null);
                    break;
            }
        }

        public interface EventListener {
            void onClickSubmit(Reddit.PostType postType);
            void showSidebar();
        }
    }

    public static abstract class ViewHolderBase extends RecyclerView.ViewHolder
            implements Toolbar.OnMenuItemClickListener, View.OnClickListener,
            View.OnLongClickListener, SurfaceHolder.Callback {

        public Link link;
        public boolean showSubreddit;

        public FrameLayout frameFull;
        public ProgressBar progressImage;
        public ViewPager viewPagerFull;
        public ImageView imagePlay;
        public ImageView imageThumbnail;
        public WebViewFixed webFull;
        public TextView textThreadFlair;
        public TextView textThreadTitle;
        public TextView textThreadSelf;
        public TextView textThreadInfo;
        public TextView textHidden;
        public ImageButton buttonComments;
        public RelativeLayout layoutContainerExpand;
        public Toolbar toolbarActions;
        public RelativeLayout layoutContainerReply;
        public EditText editTextReply;
        public TextView textUsername;
        public Button buttonSendReply;
        public ImageButton buttonReplyEditor;
        public YouTubePlayerView viewYouTube;
        public View viewOverlay;

        public Subscription subscription;
        public MediaController mediaController;
        public AdapterAlbum adapterAlbum;
        public YouTubePlayer youTubePlayer;

        public int toolbarItemWidth;
        public int titleMargin;

        public EventListener eventListener;
        public DisallowListener disallowListener;
        public RecyclerCallback recyclerCallback;

        public MenuItem itemUpvote;
        public MenuItem itemDownvote;
        public MenuItem itemSave;
        public MenuItem itemReply;
        public MenuItem itemShare;
        public MenuItem itemDownloadImage;
        public MenuItem itemEdit;
        public MenuItem itemMarkNsfw;
        public MenuItem itemDelete;
        public MenuItem itemReport;
        public MenuItem itemViewSubreddit;
        public MenuItem itemCopyText;
        public PorterDuffColorFilter colorFilterSave;
        public PorterDuffColorFilter colorFilterPositive;
        public PorterDuffColorFilter colorFilterNegative;
        public CustomColorFilter colorFilterIconDefault;
        public CustomColorFilter colorFilterIconLight;
        public CustomColorFilter colorFilterIconDark;
        public CustomColorFilter colorFilterMenuItem;
        public int colorAccent;
        public int colorPositive;
        public int colorNegative;
        public int colorTextPrimaryDefault;
        public int colorTextSecondaryDefault;
        public int colorTextAlertDefault;
        public int titleTextColor;
        public int titleTextColorAlert;
        public int colorTextSecondary;
        public Drawable drawableDefault;

        public SharedPreferences preferences;
        public Resources resources;
        public boolean isYouTubeFullscreen;
        public SurfaceView surfaceVideo;
        public SurfaceHolder surfaceHolder;
        public MediaPlayer mediaPlayer;
        public Uri uriVideo;
        public int bufferPercentage;

        @Inject Historian historian;
        @Inject Picasso picasso;

        public ViewHolderBase(View itemView,
                EventListener eventListener,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback) {
            super(itemView);
            CustomApplication.getComponentMain().inject(this);
            this.eventListener = eventListener;
            this.disallowListener = disallowListener;
            this.recyclerCallback = recyclerCallback;
            initialize();
            initializeToolbar();
            initializeListeners();
        }

        public Intent getShareIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, link.getUrl());
            return shareIntent;
        }

        protected void expandToolbarActions() {

            if (!toolbarActions.isShown()) {
                setToolbarValues();
            }

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {
                    UtilsAnimation.animateExpand(layoutContainerExpand,
                            getRatio(), null);
                }
            });
        }

        protected void showToolbarActionsInstant() {

            if (!toolbarActions.isShown()) {
                setToolbarValues();
            }

            layoutContainerExpand.setVisibility(View.VISIBLE);
            layoutContainerExpand.getLayoutParams().height = UtilsAnimation.getMeasuredHeight(layoutContainerExpand, getRatio());
            layoutContainerExpand.requestLayout();
        }

        private void setToolbarValues() {

            addToHistory();
            setVoteColors();

            ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat
                    .getActionProvider(
                            itemShare);
            if (shareActionProvider != null) {
                shareActionProvider.setShareIntent(getShareIntent());
            }

            itemDownloadImage.setVisible(
                    Reddit.checkIsImage(link.getUrl()) || Reddit.placeImageUrl(link) && !link
                            .getUrl().endsWith(Reddit.GIF));
            itemDownloadImage.setEnabled(
                    Reddit.checkIsImage(link.getUrl()) || Reddit.placeImageUrl(link) && !link
                            .getUrl().endsWith(Reddit.GIF));

            setToolbarMenuVisibility();
            clearOverlay();
        }

        private void sendComment() {
            // TODO: Move add to immediate on button click, check if failed afterwards

            eventListener.sendComment(link.getName(), editTextReply.getText().toString());
            link.setReplyExpanded(false);
            layoutContainerReply.setVisibility(View.GONE);
        }

        protected void initialize() {

            resources = itemView.getResources();
            preferences = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());

            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(
                    new int[]{android.R.attr.textColorPrimary, android.R.attr.textColorSecondary, R.attr.colorAlert, R.attr.colorAccent, R.attr.colorIconFilter});
            colorTextPrimaryDefault = typedArray.getColor(0,
                    resources.getColor(R.color.darkThemeTextColor));
            colorTextSecondaryDefault = typedArray.getColor(1,
                    resources.getColor(R.color.darkThemeTextColorMuted));
            colorTextAlertDefault = typedArray.getColor(2, resources.getColor(R.color.textColorAlert));
            colorAccent = typedArray.getColor(3, resources.getColor(R.color.colorAccent));
            int colorIconFilter = typedArray.getColor(4, 0xFFFFFFFF);
            typedArray.recycle();
            colorPositive = resources.getColor(R.color.positiveScore);
            colorNegative = resources.getColor(R.color.negativeScore);

            progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            imagePlay = (ImageView) itemView.findViewById(R.id.image_play);
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
            textUsername = (TextView) itemView.findViewById(R.id.text_username);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonReplyEditor = (ImageButton) itemView.findViewById(R.id.button_reply_editor);
            viewYouTube = (YouTubePlayerView) itemView.findViewById(R.id.youtube);
            viewOverlay = itemView.findViewById(R.id.view_overlay);

            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    resources.getDisplayMetrics());
            titleMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    resources.getDisplayMetrics());

            drawableDefault = resources.getDrawable(
                    R.drawable.ic_web_white_48dp);
            mediaController = new MediaController(itemView.getContext());
            adapterAlbum = new AdapterAlbum(viewPagerFull, new Album(), new AdapterAlbum.EventListener() {
                @Override
                public void downloadImage(String fileName, String url) {
                    eventListener.downloadImage(fileName, url);
                }
            }, new DisallowListener() {
                @Override
                public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                    disallowListener.requestDisallowInterceptTouchEventVertical(disallow);
                }

                @Override
                public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                    disallowListener.requestDisallowInterceptTouchEventHorizontal(disallow);
                }
            }, colorFilterMenuItem);

            viewPagerFull.setAdapter(adapterAlbum);
            viewPagerFull.setPageTransformer(false, new ViewPager.PageTransformer() {
                @Override
                public void transformPage(View page, float position) {
                    if (page.getTag() instanceof AdapterAlbum.ViewHolder) {
                        AdapterAlbum.ViewHolder viewHolder = (AdapterAlbum.ViewHolder) page
                                .getTag();
                        if (position >= -1 && position <= 1) {
                            viewHolder.textAlbumIndicator.setTranslationX(
                                    -position * page.getWidth());
                            viewHolder.layoutDownloadImage.setTranslationX(
                                    -position * page.getWidth());
                        }
                    }
                }
            });

            colorFilterIconDefault = new CustomColorFilter(colorIconFilter, PorterDuff.Mode.MULTIPLY);
            colorFilterIconLight = new CustomColorFilter(resources.getColor(R.color.darkThemeIconFilter), PorterDuff.Mode.MULTIPLY);
            colorFilterIconDark = new CustomColorFilter(resources.getColor(R.color.lightThemeIconFilter), PorterDuff.Mode.MULTIPLY);
            colorFilterMenuItem = colorFilterIconDefault;
            colorFilterPositive = new PorterDuffColorFilter(colorPositive,
                    PorterDuff.Mode.MULTIPLY);
            colorFilterNegative = new PorterDuffColorFilter(colorNegative,
                    PorterDuff.Mode.MULTIPLY);
            colorFilterSave = new PorterDuffColorFilter(colorAccent, PorterDuff.Mode.MULTIPLY);

            buttonComments.setColorFilter(colorFilterIconDefault);

            surfaceVideo = (SurfaceView) itemView.findViewById(R.id.surface_video);
        }

        protected void initializeListeners() {

            buttonComments.setOnClickListener(this);
            buttonSendReply.setOnClickListener(this);
            imageThumbnail.setOnClickListener(this);
            textThreadSelf.setOnClickListener(this);
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());

            final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (!TextUtils.isEmpty(eventListener.getUser().getName())) {
                        eventListener.voteLink(ViewHolderBase.this, link, 1);
                    }
                    if (layoutContainerExpand.getVisibility() == View.VISIBLE) {
                        layoutContainerExpand.clearAnimation();
                        layoutContainerExpand.setVisibility(View.GONE);
                    }
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    expandToolbarActions();
                    return super.onSingleTapUp(e);
                }

            });

            View.OnTouchListener onTouchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetector.onTouchEvent(event);
                }
            };

            // setClickable is needed otherwise we can't intercept touch events
            itemView.setClickable(true);
            itemView.setOnTouchListener(onTouchListener);
            toolbarActions.setOnTouchListener(onTouchListener);

            mediaController.setMediaPlayer(new MediaController.MediaPlayerControl() {
                @Override
                public void start() {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                }

                @Override
                public void pause() {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                    }
                }

                @Override
                public int getDuration() {
                    return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
                }

                @Override
                public int getCurrentPosition() {
                    return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
                }

                @Override
                public void seekTo(int pos) {
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(pos);
                    }
                }

                @Override
                public boolean isPlaying() {
                    return mediaPlayer != null && mediaPlayer.isPlaying();
                }

                @Override
                public int getBufferPercentage() {
                    return bufferPercentage;
                }

                @Override
                public boolean canPause() {
                    return true;
                }

                @Override
                public boolean canSeekBackward() {
                    return true;
                }

                @Override
                public boolean canSeekForward() {
                    return true;
                }

                @Override
                public int getAudioSessionId() {
                    return mediaPlayer != null ? mediaPlayer.getAudioSessionId() : 0;
                }
            });
            surfaceVideo.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // TODO: Use custom MediaController
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
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
            mediaController.setAnchorView(surfaceVideo);

            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
            editTextReply.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        link.setReplyText(s.toString());
                    }
                }
            });
            buttonReplyEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.showReplyEditor(link);
                }
            });
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
                case R.id.button_send_reply:
                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        sendComment();
                        InputMethodManager inputManager = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(itemView.getWindowToken(), 0);
                    }
                    break;
                default:
                    expandToolbarActions();
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                default:
                    eventListener.voteLink(ViewHolderBase.this, link, 1);
                    clearOverlay();
                    return true;
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
            itemEdit = menu.findItem(R.id.item_edit);
            itemMarkNsfw = menu.findItem(R.id.item_mark_nsfw);
            itemDelete = menu.findItem(R.id.item_delete);
            itemReport = menu.findItem(R.id.item_report);
            itemViewSubreddit = menu.findItem(R.id.item_view_subreddit);
            itemCopyText = menu.findItem(R.id.item_copy_text);

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterMenuItem);
            }
            buttonReplyEditor.setColorFilter(colorFilterMenuItem);

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
                    eventListener.downloadImage(link.getId(), link.getUrl());
                    break;
                case R.id.item_web:
                    eventListener.loadWebFragment(link.getUrl());
                    break;
                case R.id.item_reply:
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveLink();
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
                    ClipboardManager clipboard = (ClipboardManager) itemView.getContext()
                            .getSystemService(
                                    Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(
                            resources.getString(R.string.comment),
                            link.getSelfText());
                    clipboard.setPrimaryClip(clip);
                    eventListener
                            .toast(resources.getString(R.string.copied));
                    break;
                case R.id.item_edit:
                    eventListener.editLink(link);
                    break;
                case R.id.item_mark_nsfw:
                    markNsfw();
                    break;
                case R.id.item_delete:
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete post?")
                            .setMessage(link.getTitle())
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            eventListener.deletePost(link);
                                        }
                                    })
                            .setNegativeButton("No", null)
                            .show();
                    break;
                case R.id.item_view_subreddit:
                    Intent intentViewSubreddit = new Intent(itemView.getContext(),
                            MainActivity.class);
                    intentViewSubreddit.setAction(Intent.ACTION_VIEW);
                    intentViewSubreddit.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/r/" + link.getSubreddit());
                    eventListener.startActivity(intentViewSubreddit);
                    break;
                // Reporting
                case R.id.item_report_spam:
                    eventListener.report(link, "spam", null);
                    break;
                case R.id.item_report_vote_manipulation:
                    eventListener.report(link, "vote manipulation", null);
                    break;
                case R.id.item_report_personal_information:
                    eventListener.report(link, "personal information", null);
                    break;
                case R.id.item_report_sexualizing_minors:
                    eventListener.report(link, "sexualizing minors", null);
                    break;
                case R.id.item_report_breaking_reddit:
                    eventListener.report(link, "breaking reddit", null);
                    break;
                case R.id.item_report_other:
                    View viewDialog = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.dialog_text_input, null, false);
                    InputFilter[] filterArray = new InputFilter[1];
                    filterArray[0] = new InputFilter.LengthFilter(100);
                    final EditText editText = (EditText) viewDialog.findViewById(R.id.edit_text);
                    editText.setFilters(filterArray);
                    new AlertDialog.Builder(itemView.getContext())
                            .setView(viewDialog)
                            .setTitle(R.string.item_report)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    eventListener
                                            .report(link, "other", editText.getText().toString());
                                }
                            })
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                            .show();
                    break;
            }
            return true;
        }

        private void saveLink() {
            eventListener.save(link);
            syncSaveIcon();
        }

        public void toggleReply() {
            expandFull(true);
            link.setReplyExpanded(!link.isReplyExpanded());
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);
            if (link.isReplyExpanded()) {
                textUsername.setText("- " + eventListener.getUser().getName());
                recyclerCallback.hideToolbar();
                recyclerCallback.onReplyShown();
                InputMethodManager inputManager = (InputMethodManager) itemView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                editTextReply.setText(link.getReplyText());
                editTextReply.clearFocus();
                editTextReply.post(new Runnable() {
                    @Override
                    public void run() {
                        editTextReply.requestFocus();
                    }
                });
            }
        }

        public abstract float getRatio();

        public void loadFull() {
            recyclerCallback.hideToolbar();

            addToHistory();
            clearOverlay();

            // TODO: Toggle visibility of web and video views

            String urlString = link.getUrl();
            if (!TextUtils.isEmpty(urlString)) {
                if (!checkLinkUrl()) {
                    attemptLoadImage();
                }
            }
        }

        public void onClickThumbnail() {
            clearOverlay();
            if (!loadSelfText()) {
                loadFull();
            }
        }

        public void loadComments() {

            addToHistory();
            clearOverlay();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            // TODO: Improve where this logic is handled in click timeline
            if (link.getNumComments() == 0) {
                if (!link.isCommentsClicked()) {
                    eventListener.toast(resources.getString(R.string.no_comments));
                    if (link.isSelf() && !TextUtils.isEmpty(link.getSelfText())) {
                        expandFull(true);
                        textThreadSelf.setVisibility(View.VISIBLE);
                        recyclerCallback.scrollTo(getAdapterPosition());
                    }
                    link.setCommentsClicked(true);
                    return;
                }
            }

            // Hide YouTubePlayerView to prevent GPU drawing overlaps
            if (viewYouTube.isShown()) {
                hideYouTube();
            }

            eventListener.onClickComments(link, this);
        }

        /**
         *
         * @return true if link is self text, false otherwise
         */
        public boolean loadSelfText() {
            addToHistory();
            if (!link.isSelf()) {
                return false;
            }

            if (textThreadSelf.isShown()) {
                textThreadSelf.setVisibility(View.GONE);
                // TODO: Check if textThreadSelf is taller than view and optimize animation
//                UtilsAnimation.animateExpand(textThreadSelf, 1f, null);
            }
            else if (TextUtils.isEmpty(link.getSelfText())) {
                loadComments();
            }
            else {
                expandFull(true);
                textThreadSelf.setVisibility(View.VISIBLE);
                recyclerCallback.scrollTo(getAdapterPosition());
            }

            return true;
        }

        public void addToHistory() {
            if (preferences.getBoolean(AppSettings.PREF_SAVE_HISTORY, true)) {
                historian.add(link);
            }
        }

        public void expandFull(boolean expand) {
            setToolbarMenuVisibility();
        }

        public void setVoteColors() {

            switch (link.getLikes()) {
                case 1:
                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterPositive);
                    itemDownvote.getIcon().setColorFilter(colorFilterMenuItem);
                    break;
                case -1:
                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterNegative);
                    itemUpvote.getIcon().setColorFilter(colorFilterMenuItem);
                    break;
                case 0:
                    itemUpvote.getIcon().setColorFilter(colorFilterMenuItem);
                    itemDownvote.getIcon().setColorFilter(colorFilterMenuItem);
                    break;
            }

            setTextValues(link);

        }

        public void setTextValues(Link link) {

            if (!TextUtils.isEmpty(link.getLinkFlairText())) {
                textThreadFlair.setVisibility(View.VISIBLE);
                textThreadFlair.setText(link.getLinkFlairText());
            }
            else {
                textThreadFlair.setVisibility(View.GONE);
            }

            textThreadTitle.setText(link.getTitle()
                    .toString());
            syncTitleColor();

            textThreadSelf.setText(link.getSelfTextHtml());
        }

        public String getFlairString() {
            return TextUtils.isEmpty(link.getAuthorFlairText()) || "null".equals(link.getAuthorFlairText()) ? "" : " (" + Html
                    .fromHtml(link.getAuthorFlairText()) + ") ";
        }

        public String getSubredditString() {
            return showSubreddit ? "/r/" + link.getSubreddit() : "";
        }

        public CharSequence getSpannableScore() {

            String voteIndicator = "";
            int voteColor = 0;

            switch (link.getLikes()) {
                case -1:
                    voteIndicator = " \u25BC";
                    voteColor = colorNegative;
                    break;
                case 1:
                    voteIndicator = " \u25B2";
                    voteColor = colorPositive;
                    break;
            }

            Spannable spannableVote = new SpannableString(voteIndicator);
            if (!TextUtils.isEmpty(spannableVote)) {
                spannableVote.setSpan(new ForegroundColorSpan(voteColor), 0, spannableVote.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }

            Spannable spannableScore = new SpannableString(" " + link.getScore() + " ");
            spannableScore.setSpan(new ForegroundColorSpan(
                            link.getScore() > 0 ? colorPositive : colorNegative), 0,
                    spannableScore.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return TextUtils.concat(spannableVote, spannableScore);
        }

        public CharSequence getTimestamp() {

            if (preferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false)) {
                String editTimestamp = link.getEdited() > 1 ? "Edited " + DateUtils.formatDateTime(
                        itemView.getContext(), link.getEdited(),
                        TIMESTAMP_BITMASK) + "\n" : "";

                return editTimestamp + DateUtils.formatDateTime(itemView.getContext(), link.getCreatedUtc(),
                        TIMESTAMP_BITMASK);
            }

            String editTimestamp = link.getEdited() > 1 ? "Edited " + DateUtils.getRelativeTimeSpanString(link.getEdited()) + "\n" : "";

            return editTimestamp + DateUtils.getRelativeTimeSpanString(link.getCreatedUtc());
        }

        public boolean isInHistory() {
            return historian.contains(link.getName());
        }

        public void attemptLoadImage() {

            if (Reddit.placeImageUrl(link)) {
                expandFull(true);
                recyclerCallback.getLayoutManager().requestLayout();
                itemView.invalidate();
                itemView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (webFull == null) {
                            webFull = eventListener.getNewWebView(
                                    new WebViewFixed.OnFinishedListener() {
                                        @Override
                                        public void onFinished() {
                                            webFull.setVisibility(View.GONE);
                                            webFull.setVisibility(View.VISIBLE);
                                            recyclerCallback.scrollTo(getAdapterPosition());
                                            Log.d(TAG, "onFinished");
                                        }
                                    });
                            webFull.setOnTouchListener(
                                    new OnTouchListenerDisallow(disallowListener));
                            webFull.setWebViewClient(new WebViewClient() {

                                @Override
                                public void onPageStarted(WebView view,
                                        String url,
                                        Bitmap favicon) {
                                    super.onPageStarted(view, url, favicon);
                                    progressImage.setVisibility(View.VISIBLE);
                                    Log.d(TAG, "onPageStarted");
                                }

                                @Override
                                public void onPageFinished(WebView view, String url) {
                                    super.onPageFinished(view, url);
                                    progressImage.setVisibility(View.GONE);
                                    Log.d(TAG, "onPageFinished");
                                }

                                @Override
                                public void onScaleChanged(WebView view,
                                        float oldScale,
                                        float newScale) {
                                    ((WebViewFixed) view).lockHeight();
                                    super.onScaleChanged(view, oldScale, newScale);
                                }

                                @Override
                                public void onReceivedError(WebView view,
                                        WebResourceRequest request,
                                        WebResourceError error) {
                                    super.onReceivedError(view, request, error);
                                    Log.e(TAG, "WebView error: " + error);
                                }

                            });
                            frameFull.addView(webFull, frameFull.getChildCount() - 1);
                        }
                        webFull.onResume();
                        webFull.post(new Runnable() {
                            @Override
                            public void run() {
                                webFull.loadData(Reddit.getImageHtml(link.getUrl()),
                                        "text/html", "UTF-8");
                            }
                        });
                    }
                }, 50);
            }
            else {
                eventListener.loadUrl(link.getUrl());
            }
        }

        public void setAlbum(Link link, Album album) {
            link.setAlbum(album);
            ViewGroup.LayoutParams layoutParams = viewPagerFull.getLayoutParams();
            layoutParams.height = recyclerCallback.getRecyclerHeight();
            viewPagerFull.setLayoutParams(layoutParams);
            viewPagerFull.setVisibility(View.VISIBLE);
            viewPagerFull.requestLayout();
            adapterAlbum.setAlbum(album, colorFilterMenuItem);
            viewPagerFull.setCurrentItem(0, false);
            recyclerCallback.scrollTo(getAdapterPosition());
        }

        /**
         * @return true if Link loading has been handled, false otherwise
         */
        public boolean checkLinkUrl() {

            if (link.getDomain()
                    .contains("imgur")) {
                expandFull(true);
                return loadImgur();
            }
            else if (link.getDomain()
                    .contains("gfycat")) {
                expandFull(true);
                return loadGfycat();
            }
            else if (link.getDomain()
                    .contains("youtu")) {
                expandFull(true);
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
                    ".*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=)([^#&\\?]*).*");
            final Matcher matcher = pattern.matcher(link.getUrl());
            if (matcher.matches()) {
                int time = 0;
                Uri uri = Uri.parse(link.getUrl());
                String timeQuery = uri.getQueryParameter("t");
                if (!TextUtils.isEmpty(timeQuery)) {
                    try {
                        // YouTube query provides time in seconds, but we need milliseconds
                        time = Integer.parseInt(timeQuery) * 1000;
                    }
                    catch (NumberFormatException e) {
                        time = 0;
                    }
                }
                loadYouTubeVideo(link, matcher.group(1), time);
                return true;
            }

            return false;
        }

        private boolean loadGfycat() {
            String gfycatId = Reddit.parseUrlId(link.getUrl(), Reddit.GFYCAT_PREFIX, ".");
            progressImage.setVisibility(View.VISIBLE);

            subscription = eventListener.getReddit()
                    .loadGfycat(gfycatId)
                    .flatMap(new Func1<String, Observable<JSONObject>>() {
                        @Override
                        public Observable<JSONObject> call(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(
                                        response).getJSONObject(Reddit.GFYCAT_ITEM);

                                return Observable.just(jsonObject);
                            }
                            catch (JSONException e) {
                                return Observable.error(e);
                            }
                        }
                    })
                    .subscribe(new FinalizingSubscriber<JSONObject>() {
                        @Override
                        public void error(Throwable e) {
                            progressImage.setVisibility(View.GONE);
                        }

                        @Override
                        public void next(JSONObject jsonObject) {
                            try {
                                loadVideo(jsonObject.getString(Reddit.GFYCAT_MP4),
                                        (float) jsonObject.getInt(
                                                "height") / jsonObject.getInt(
                                                "width"));
                            }
                            catch (JSONException e) {
                                error(e);
                            }
                        }
                    });
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

        private void loadGallery(String id, final Link link) {
            subscription = eventListener.getReddit()
                    .loadImgurGallery(id)
                    .flatMap(new Func1<String, Observable<Album>>() {
                        @Override
                        public Observable<Album> call(String response) {
                            try {
                                return Observable.just(Album.fromJson(new JSONObject(response)
                                        .getJSONObject("data")));
                            }
                            catch (JSONException e) {
                                return Observable.error(e);
                            }
                        }
                    })
                    .subscribe(new FinalizingSubscriber<Album>() {
                        @Override
                        public void start() {
                            progressImage.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void next(Album next) {
                            setAlbum(link, next);
                        }

                        @Override
                        public void finish() {
                            progressImage.setVisibility(View.GONE);
                        }
                    });
        }

        private void loadAlbum(String id, final Link link) {
            subscription = eventListener.getReddit().loadImgurAlbum(id)
                    .flatMap(new Func1<String, Observable<Album>>() {
                        @Override
                        public Observable<Album> call(String response) {
                            try {
                                return Observable.just(Album.fromJson(new JSONObject(response)
                                        .getJSONObject("data")));
                            }
                            catch (JSONException e) {
                                return Observable.error(e);
                            }
                        }
                    })
                    .subscribe(new FinalizingSubscriber<Album>() {
                        @Override
                        public void start() {
                            progressImage.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void next(Album next) {
                            setAlbum(link, next);
                        }

                        @Override
                        public void finish() {
                            progressImage.setVisibility(View.GONE);
                        }
                    });
        }

        private void loadGifv(String id) {
            eventListener.getReddit()
                    .loadImgurImage(id)
                    .flatMap(new Func1<String, Observable<Image>>() {
                        @Override
                        public Observable<Image> call(String response) {
                            try {
                                return Observable.just(Image.fromJson(
                                        new JSONObject(
                                                response).getJSONObject(
                                                "data")));
                            }
                            catch (JSONException e) {
                                return Observable.error(e);
                            }
                        }
                    })
                    .subscribe(new FinalizingSubscriber<Image>() {
                        @Override
                        public void start() {
                            progressImage.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void next(Image image) {
                            if (!TextUtils.isEmpty(image.getMp4())) {
                                loadVideo(image.getMp4(),
                                        (float) image.getHeight() / image.getWidth());
                            }
                            else if (!TextUtils.isEmpty(image.getWebm())) {
                                loadVideo(image.getWebm(),
                                        (float) image.getHeight() / image.getWidth());
                            }
                        }

                        @Override
                        public void finish() {
                            progressImage.setVisibility(View.GONE);
                        }
                    });
        }

        private void loadVideo(final String url, float heightRatio) {
            uriVideo = Uri.parse(url);
            progressImage.setVisibility(View.GONE);
            surfaceVideo.setVisibility(View.VISIBLE);

            surfaceVideo.getLayoutParams().height = (int) (itemView.getWidth() * heightRatio);
            surfaceVideo.requestLayout();
            surfaceVideo.setVisibility(View.GONE);
            surfaceVideo.setVisibility(View.VISIBLE);
            surfaceHolder = surfaceVideo.getHolder();
            surfaceHolder.addCallback(ViewHolderBase.this);
            surfaceHolder.setSizeFromLayout();
            recyclerCallback.scrollTo(getAdapterPosition());
        }

        public void loadYouTubeVideo(final Link link, final String id, final int timeInMillis) {
            viewYouTube.initialize(ApiKeys.YOUTUBE_API_KEY,
                    new YouTubePlayer.OnInitializedListener() {
                        @Override
                        public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                final YouTubePlayer youTubePlayer,
                                boolean b) {
                            ViewHolderBase.this.youTubePlayer = youTubePlayer;
                            youTubePlayer.setFullscreenControlFlags(
                                    YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);
                            youTubePlayer.setManageAudioFocus(false);
                            youTubePlayer.setOnFullscreenListener(
                                    new YouTubePlayer.OnFullscreenListener() {
                                        @Override
                                        public void onFullscreen(boolean fullscreen) {
                                            Log.d(TAG, "fullscreen: " + fullscreen);
                                            isYouTubeFullscreen = fullscreen;
                                            youTubePlayer.setFullscreen(fullscreen);
                                        }
                                    });
                            youTubePlayer.setPlayerStateChangeListener(new SimplePlayerStateChangeListener() {
                                @Override
                                public void onVideoStarted() {
                                    youTubePlayer.seekToMillis(timeInMillis);
                                    youTubePlayer.setPlayerStateChangeListener(new SimplePlayerStateChangeListener());
                                }
                            });
                            youTubePlayer.loadVideo(id);
                            viewYouTube.setVisibility(View.VISIBLE);
                            recyclerCallback.scrollTo(getAdapterPosition());
                        }

                        @Override
                        public void onInitializationFailure(YouTubePlayer.Provider provider,
                                YouTubeInitializationResult youTubeInitializationResult) {
                            eventListener.toast(resources
                                    .getString(R.string.error_youtube));
                        }
                    });
        }

        private void hideYouTube() {
            viewYouTube.setVisibility(View.GONE);
        }

        public void setToolbarMenuVisibility() {
            Menu menu = toolbarActions.getMenu();

            boolean loggedIn = eventListener.isUserLoggedIn();
            boolean isAuthor = link.getAuthor().equals(eventListener.getUser().getName());

            itemEdit.setVisible(link.isSelf() && isAuthor);
            itemEdit.setEnabled(link.isSelf() && isAuthor);
            itemMarkNsfw.setVisible(isAuthor);
            itemMarkNsfw.setEnabled(isAuthor);
            itemDelete.setVisible(isAuthor);
            itemDelete.setEnabled(isAuthor);

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
            itemReport.setVisible(loggedIn);
            itemViewSubreddit.setVisible(showSubreddit);
            itemViewSubreddit.setEnabled(showSubreddit);
            itemCopyText.setVisible(link.isSelf());
            itemCopyText.setEnabled(link.isSelf());

            syncSaveIcon();

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {

                    calculateVisibleToolbarItems(itemView.getWidth());

                }
            });
        }

        public void calculateVisibleToolbarItems(int width) {

            Menu menu = toolbarActions.getMenu();

            int maxNum = width / toolbarItemWidth;
            int numShown = 0;

            for (int index = 0; index < menu.size(); index++) {

                MenuItem menuItem = menu.getItem(index);

                if (!menuItem.isVisible()) {
                    continue;
                }

                if (numShown++ < maxNum - 1) {
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }

        public void onDetach() {

        }

        public void onRecycle() {
            destroyWebViews();
            
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
                subscription = null;
            }

            if (youTubePlayer != null) {
                isYouTubeFullscreen = false;
                youTubePlayer = null;
            }
            viewYouTube.setVisibility(View.GONE);
            surfaceVideo.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imagePlay.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);
            imageThumbnail.setImageBitmap(null);
        }

        public void onBind(Link link, boolean showSubreddit) {
            this.link = link;
            this.showSubreddit = showSubreddit;
            titleTextColor = colorTextPrimaryDefault;
            titleTextColorAlert = colorTextAlertDefault;
            colorTextSecondary = colorTextSecondaryDefault;
            colorFilterMenuItem = colorFilterIconDefault;
            isYouTubeFullscreen = false;
            layoutContainerExpand.setVisibility(View.GONE);

            if (link.isReplyExpanded()) {
                if (TextUtils.isEmpty(link.getReplyText())) {
                    link.setReplyExpanded(false);
                    layoutContainerReply.setVisibility(View.GONE);
                }
                else {
                    expandFull(true);
                    editTextReply.setText(link.getReplyText());
                    layoutContainerReply.setVisibility(View.VISIBLE);
                }
            }
            else {
                layoutContainerReply.setVisibility(View.GONE);
            }

            progressImage.setIndeterminate(true);

            frameFull.requestLayout();

            textThreadSelf.setVisibility(View.GONE);
            adapterAlbum.setAlbum(null, null);

            setTextValues(link);

            if (preferences.getBoolean(AppSettings.PREF_DIM_POSTS, true)) {
                viewOverlay.setVisibility(
                        isInHistory() && !link.isReplyExpanded() ? View.VISIBLE : View.GONE);
            }

            itemView.setVisibility(View.VISIBLE);
        }

        public void syncSaveIcon() {
            if (link.isSaved()) {
                itemSave.setTitle(resources.getString(R.string.unsave));
                itemSave.getIcon().mutate().setColorFilter(colorFilterSave);
            }
            else {
                itemSave.setTitle(resources.getString(R.string.save));
                itemSave.getIcon().setColorFilter(colorFilterMenuItem);
            }
        }

        public void markNsfw() {
            eventListener.markNsfw(link);
            syncTitleColor();
        }

        public void syncTitleColor() {
            textThreadTitle.setTextColor(link.isOver18() ? titleTextColorAlert : titleTextColor);
            textThreadInfo.setTextColor(colorTextSecondary);
            itemMarkNsfw.setTitle(link.isOver18() ? R.string.unmark_nsfw : R.string.mark_nsfw);
        }

        public void destroyWebViews() {

            if (webFull != null) {
                frameFull.removeView(webFull);
                webFull.setOnTouchListener(null);
                webFull.setWebChromeClient(null);
                webFull.setWebViewClient(null);
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
            adapterAlbum.setAlbum(null, null);
            viewPagerFull.removeAllViews();
            frameFull.requestLayout();
        }

        public int getBackgroundColor() {
            return link.getBackgroundColor();
        }

        public void clearOverlay() {
            viewOverlay.setVisibility(View.GONE);
        }

        // TODO: Calculate which Views are visible
        public void setVisibility(int visibility) {
            frameFull.setVisibility(visibility);
            surfaceVideo.setVisibility(visibility);
            progressImage.setVisibility(visibility);
            viewPagerFull.setVisibility(visibility);
            imagePlay.setVisibility(visibility);
            imageThumbnail.setVisibility(visibility);
            textThreadFlair.setVisibility(visibility);
            textThreadTitle.setVisibility(visibility);
            textThreadSelf.setVisibility(visibility);
            textThreadInfo.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            buttonComments.setVisibility(visibility);
            layoutContainerExpand.setVisibility(visibility);
            toolbarActions.setVisibility(visibility);
            layoutContainerReply.setVisibility(visibility);
            editTextReply.setVisibility(visibility);
            buttonSendReply.setVisibility(visibility);
            viewYouTube.setVisibility(visibility);
            itemView.setVisibility(visibility);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            if (uriVideo == null) {
                return;
            }

            bufferPercentage = 0;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    bufferPercentage = percent;
                }
            });
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaController.hide();
                    surfaceHolder.setFixedSize(mp.getVideoWidth(), mp.getVideoHeight());
                    mp.start();
                    Log.d(TAG, "onPrepared");
                }
            });
            mediaPlayer.setDisplay(surfaceHolder);

            try {
                mediaPlayer.setDataSource(surfaceVideo.getContext(), uriVideo);
                mediaPlayer.prepareAsync();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (surfaceHolder != null) {
                surfaceHolder.removeCallback(this);
            }
        }

        public int[] getScreenAnchor() {
            int[] location = new int[2];
            itemView.getLocationOnScreen(location);
            return location;
        }

        public interface EventListener {

            void sendComment(String name, String text);
            void sendMessage(String name, String text);
            void onClickComments(Link link, ViewHolderBase viewHolderBase);
            void save(Link link);
            void save(Comment comment);
            void loadUrl(String url);
            void downloadImage(String fileName, String url);
            Reddit getReddit();
            WebViewFixed getNewWebView(WebViewFixed.OnFinishedListener onFinishedListener);
            void toast(String text);
            boolean isUserLoggedIn();
            void voteLink(ViewHolderBase viewHolderBase, Link link, int vote);
            void startActivity(Intent intent);
            void deletePost(Link link);
            void report(Thing thing, String reason, String otherReason);
            void hide(Link link);
            void editLink(Link link);
            void showReplyEditor(Replyable replyable);
            void markRead(Thing thing);
            void markNsfw(Link link);
            void loadWebFragment(String url);
            User getUser();
        }

    }

}
