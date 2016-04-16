/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
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
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.glide.RequestListenerCompletion;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.rx.ObserverEmpty;
import com.winsonchiu.reader.rx.ObserverError;
import com.winsonchiu.reader.utils.AdapterBase;
import com.winsonchiu.reader.utils.AdapterCallback;
import com.winsonchiu.reader.utils.BaseMediaPlayerControl;
import com.winsonchiu.reader.utils.BaseTextWatcher;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.utils.SimplePlayerStateChangeListener;
import com.winsonchiu.reader.utils.Utils;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.UtilsInput;
import com.winsonchiu.reader.utils.UtilsJson;
import com.winsonchiu.reader.utils.UtilsReddit;
import com.winsonchiu.reader.utils.UtilsRx;
import com.winsonchiu.reader.utils.UtilsView;
import com.winsonchiu.reader.utils.ViewHolderBase;
import com.winsonchiu.reader.utils.YouTubeListener;
import com.winsonchiu.reader.views.ImageViewZoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends AdapterBase<ViewHolderBase> implements CallbackYouTubeDestruction {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_LINK = 1;

    private static final String TAG = AdapterLink.class.getCanonicalName();
    private static final int TIMESTAMP_BITMASK = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
    public static final String TAG_PICASSO = "picassoAdapterLink";

    protected FragmentActivity activity;
    protected SharedPreferences preferences;
    protected LayoutManager layoutManager;
    protected ControllerLinksBase controllerLinks;
    protected List<ViewHolderBase> viewHolders;

    protected ViewHolderHeader.EventListener eventListenerHeader;
    protected ViewHolderLink.EventListener eventListenerBase;
    protected DisallowListener disallowListener;
    protected RecyclerCallback recyclerCallback;

    @Inject ControllerUser controllerUser;

    public AdapterLink(FragmentActivity activity, ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.EventListener eventListenerBase,
            DisallowListener disallowListener, RecyclerCallback recyclerCallback) {
        this.eventListenerHeader = eventListenerHeader;
        this.eventListenerBase = eventListenerBase;
        this.disallowListener = disallowListener;
        this.recyclerCallback = recyclerCallback;
        viewHolders = new ArrayList<>();

        ((ActivityMain) activity).getComponentActivity().inject(this);
        setActivity(activity);
    }

    public void setActivity(FragmentActivity activity) {
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
        return position == 0 ? TYPE_HEADER : TYPE_LINK;
    }

    @Override
    public int getItemCount() {
        return controllerLinks.sizeLinks() + 1;
    }

    @Override
    @CallSuper
    public void onBindViewHolder(ViewHolderBase holder, int position) {
        if (!controllerLinks.isLoading() && position > controllerLinks.sizeLinks() - 5) {
            controllerLinks.loadMoreLinks()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ObserverError<Listing>() {
                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(activity, activity.getString(R.string.error_loading_links), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        Reddit.incrementBind();
        viewHolders.add(holder);
    }

    @Override
    public void onViewRecycled(ViewHolderBase holder) {
        super.onViewRecycled(holder);
        holder.onRecycle();

        Reddit.incrementRecycled();
        viewHolders.remove(holder);
    }

    public void setVisibility(int visibility) {
        for (ViewHolderBase viewHolder : viewHolders) {
            viewHolder.itemView.setVisibility(visibility);
        }
    }

    public void setVisibility(int visibility, @NonNull Thing thing) {
        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK && thing.equals(((ViewHolderLink) viewHolder).link)) {
                viewHolder.itemView.setVisibility(visibility);
            }
        }
    }

    public void pauseViewHolders() {
        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK) {
                ViewHolderLink viewHolderLink = (ViewHolderLink) viewHolder;
                if (viewHolderLink.mediaPlayer != null) {
                    viewHolderLink.mediaPlayer.stop();
                }
            }
        }

        destroyYouTubePlayerFragments();
    }

    public boolean navigateBack() {
        boolean navigateBack = true;

        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK) {
                ViewHolderLink viewHolderLink = (ViewHolderLink) viewHolder;

                if (viewHolderLink.youTubePlayer != null) {
                    if (viewHolderLink.isYouTubeFullscreen) {
                        viewHolderLink.youTubePlayer.setFullscreen(false);
                        navigateBack = false;
                        break;
                    }
                }
            }
        }

        return navigateBack;
    }

    public void destroyViewHolders() {
        for (ViewHolderBase viewHolder : viewHolders) {
            Reddit.incrementRecycled();
            viewHolder.onRecycle();
        }
    }

    @Override
    public void destroyYouTubePlayerFragments() {
        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK) {
                ((ViewHolderLink) viewHolder).destroyYouTube();
            }
        }
    }

    public static class ViewHolderHeader extends ViewHolderBase implements View.OnClickListener {

        @Bind(R.id.text_name) TextView textName;
        @Bind(R.id.text_title) TextView textTitle;
        @Bind(R.id.text_description) TextView textDescription;
        @Bind(R.id.layout_buttons) LinearLayout layoutButtons;
        @Bind(R.id.button_submit_link) Button buttonSubmitLink;
        @Bind(R.id.button_submit_self) Button buttonSubmitSelf;
        @Bind(R.id.layout_container_expand) RelativeLayout layoutContainerExpand;
        @Bind(R.id.text_hidden) TextView textHidden;
        @Bind(R.id.button_show_sidebar) ImageButton buttonShowSidebar;

        @BindString(R.string.submit_link) String defaultTextSubmitLink;
        @BindString(R.string.submit_text) String defaultTextSubmitText;

        private EventListener eventListener;

        public ViewHolderHeader(View itemView,
                AdapterCallback adapterCallback,
                EventListener eventListener) {
            super(itemView, adapterCallback);
            this.eventListener = eventListener;
            ButterKnife.bind(this, itemView);

            textDescription.setMovementMethod(LinkMovementMethod.getInstance());

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        true);
            }

            buttonShowSidebar.setColorFilter(UtilsColor.getColorIconFilter(itemView.getContext()), PorterDuff.Mode.MULTIPLY);
        }

        private void setVisibility(int visibility) {
            textName.setVisibility(visibility);
            textTitle.setVisibility(visibility);
            textDescription.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            layoutButtons.setVisibility(visibility);
            buttonShowSidebar.setVisibility(visibility);
            buttonSubmitLink.setVisibility(visibility);
            buttonSubmitSelf.setVisibility(visibility);
        }

        public void onBind(Subreddit subreddit) {
            if (TextUtils.isEmpty(subreddit.getDisplayName())
                    || UtilsReddit.isAll(subreddit)
                    || UtilsReddit.isMultiple(subreddit)) {
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
                textDescription.setText(UtilsReddit.getFormattedHtml(subreddit.getPublicDescriptionHtml()));
                textDescription.setVisibility(View.VISIBLE);
            }

            textHidden.setText(itemView.getResources().getString(R.string.subreddit_info, subreddit.getSubscribers(), new Date(subreddit.getCreatedUtc())));

            buttonSubmitLink.setText(UtilsJson.isEmptyOrNullLiteral(subreddit.getSubmitLinkLabel())
                    ? defaultTextSubmitLink
                    : subreddit.getSubmitLinkLabel());

            buttonSubmitSelf.setText(UtilsJson.isEmptyOrNullLiteral(subreddit.getSubmitTextLabel())
                    ? defaultTextSubmitText
                    : subreddit.getSubmitTextLabel());

            if (Reddit.PostType.fromString(subreddit.getSubmissionType()) == Reddit.PostType.LINK) {
                buttonSubmitSelf.setVisibility(View.GONE);
            }
            else {
                buttonSubmitLink.setVisibility(View.GONE);
            }
        }

        @OnClick({
                R.id.button_show_sidebar,
                R.id.button_submit_link,
                R.id.button_submit_self,
                R.id.text_description,
                R.id.layout_root
        })
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
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

    public static abstract class ViewHolderLink extends ViewHolderBase
            implements Toolbar.OnMenuItemClickListener, View.OnClickListener,
            View.OnLongClickListener, SurfaceHolder.Callback {

        @Bind(R.id.layout_root) ViewGroup layoutRoot;
        @Bind(R.id.layout_inner) ViewGroup layoutInner;
        @Bind(R.id.layout_full) public ViewGroup layoutFull;
        @Bind(R.id.progress_image) public ProgressBar progressImage;
        @Bind(R.id.view_pager_full) public ViewPager viewPagerFull;
        @Bind(R.id.image_play) public ImageView imagePlay;
        @Bind(R.id.image_thumbnail) public ImageView imageThumbnail;
        @Bind(R.id.image_full) public ImageViewZoom imageFull;
        @Bind(R.id.view_margin) public View viewMargin;
        @Bind(R.id.button_comments) public ImageView buttonComments;
        @Bind(R.id.text_thread_flair) public TextView textThreadFlair;
        @Bind(R.id.text_thread_title) public TextView textThreadTitle;
        @Bind(R.id.text_thread_self) public TextView textThreadSelf;
        @Bind(R.id.text_thread_info) public TextView textThreadInfo;
        @Bind(R.id.text_hidden) public TextView textHidden;
        @Bind(R.id.layout_container_expand) public ViewGroup layoutContainerExpand;
        @Bind(R.id.toolbar_actions) public Toolbar toolbarActions;
        @Bind(R.id.layout_container_reply) public ViewGroup layoutContainerReply;
        @Bind(R.id.edit_text_reply) public EditText editTextReply;
        @Bind(R.id.text_username) public TextView textUsername;
        @Bind(R.id.button_send_reply) public Button buttonSendReply;
        @Bind(R.id.button_reply_editor) public ImageButton buttonReplyEditor;
        @Bind(R.id.view_overlay) public View viewOverlay;
        @Bind(R.id.layout_youtube) public ViewGroup layoutYouTube;

        @Nullable @Bind(R.id.view_mask_start) View viewMaskStart;
        @Nullable @Bind(R.id.view_mask_end) View viewMaskEnd;

        @BindDimen(R.dimen.touch_target_size) public int toolbarItemWidth;
        @BindDimen(R.dimen.activity_horizontal_margin) public int titleMargin;

        @BindDrawable(R.drawable.ic_web_white_48dp) public Drawable drawableDefault;

        @Inject protected Historian historian;
        @Inject protected Picasso picasso;
        @Inject protected Reddit reddit;
        @Inject protected SharedPreferences sharedPreferences;

        private final FragmentActivity activity;
        public Link link;
        public boolean showSubreddit;

        public Subscription subscription;
        public MediaController mediaController;
        public AdapterAlbum adapterAlbum;

        public YouTubePlayer youTubePlayer;
        public YouTubeListener youTubeListener;
        public YouTubePlayerSupportFragment youTubeFragment;
        public int youTubeViewId = View.generateViewId();

        public EventListener eventListener;
        public DisallowListener disallowListener;
        public RecyclerCallback recyclerCallback;
        public CallbackYouTubeDestruction callbackYouTubeDestruction;

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
        public CustomColorFilter colorFilterSave;
        public CustomColorFilter colorFilterPositive;
        public CustomColorFilter colorFilterNegative;
        public CustomColorFilter colorFilterIconDefault;
        public CustomColorFilter colorFilterIconLight;
        public CustomColorFilter colorFilterIconDark;
        public CustomColorFilter colorFilterMenuItem;

        @BindColor(R.color.positiveScore) int colorPositive;
        @BindColor(R.color.negativeScore) int colorNegative;
        public int colorAccent;
        public int colorTextPrimaryDefault;
        public int colorTextSecondaryDefault;
        public int colorTextAlertDefault;
        public int titleTextColor;
        public int titleTextColorAlert;
        public int colorTextSecondary;

        public boolean isYouTubeFullscreen;
        protected GestureDetectorCompat gestureDetectorDoubleTap = new GestureDetectorCompat(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!TextUtils.isEmpty(eventListener.getUser().getName())) {
                    eventListener.voteLink(ViewHolderLink.this, link, 1);
                }
                if (layoutContainerExpand.getVisibility() == View.VISIBLE) {
                    layoutContainerExpand.clearAnimation();
                    layoutContainerExpand.setVisibility(View.GONE);
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleToolbarActions();
                return super.onSingleTapUp(e);
            }

        });

        public SurfaceView surfaceVideo;
        public SurfaceHolder surfaceHolder;
        public MediaPlayer mediaPlayer;
        public Uri uriVideo;
        public int bufferPercentage;
        private Source source;
        private boolean expanded;

        public ViewHolderLink(FragmentActivity activity,
                View itemView,
                AdapterCallback adapterCallback,
                EventListener eventListener,
                Source source,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                CallbackYouTubeDestruction callbackYouTubeDestruction) {
            super(itemView, adapterCallback);
            this.activity = activity;
            this.eventListener = eventListener;
            this.source = source;
            this.disallowListener = disallowListener;
            this.recyclerCallback = recyclerCallback;
            this.callbackYouTubeDestruction = callbackYouTubeDestruction;

            ButterKnife.bind(this, itemView);
            CustomApplication.getComponentMain().inject(this);

            initialize();
            initializeToolbar();
            initializeListeners();
        }

        protected void toggleToolbarActions() {
            setToolbarValues();

            itemView.postOnAnimation(() -> {
                if (toolbarActions.isShown()) {
                    UtilsAnimation.animateCollapseHeight(layoutContainerExpand, 0, null);
                }
                else {
                    UtilsAnimation.animateExpandHeight(layoutContainerExpand, itemView.getWidth(), 0, null);
                }
            });
        }

        public void showToolbarActionsInstant() {
            setToolbarValues();

            layoutContainerExpand.setVisibility(View.VISIBLE);
            layoutContainerExpand.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutContainerExpand.requestLayout();
        }

        public void hideToolbarActionsInstant()  {
            layoutContainerExpand.setVisibility(View.GONE);
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

            boolean downloadable = UtilsImage.checkIsImageUrl(link.getUrl()) || UtilsImage.placeImageUrl(link);

            itemDownloadImage.setVisible(downloadable);
            itemDownloadImage.setEnabled(downloadable);

            setToolbarMenuVisibility();
            clearOverlay();
        }

        private void sendComment() {
            // TODO: Move add to immediate on button click, check if failed afterwards

            eventListener.sendComment(link.getName(), editTextReply.getText().toString());
            link.setReplyExpanded(false);
            layoutContainerReply.setVisibility(View.GONE);
        }

        @SuppressWarnings("ResourceType")
        protected void initialize() {
            Context context = itemView.getContext();

            TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                    new int[]{
                            android.R.attr.textColorPrimary,
                            android.R.attr.textColorSecondary,
                            R.attr.colorAlert,
                            R.attr.colorAccent,
                            R.attr.colorIconFilter
                    });

            colorTextPrimaryDefault = typedArray.getColor(0, getColor(R.color.darkThemeTextColor));
            colorTextSecondaryDefault = typedArray.getColor(1, getColor(R.color.darkThemeTextColorMuted));
            colorTextAlertDefault = typedArray.getColor(2, getColor(R.color.textColorAlert));
            colorAccent = typedArray.getColor(3, getColor(R.color.colorAccent));
            int colorIconFilter = typedArray.getColor(4, 0xFFFFFFFF);

            typedArray.recycle();

            colorFilterIconDefault = new CustomColorFilter(colorIconFilter);
            colorFilterIconLight = new CustomColorFilter(getColor(R.color.darkThemeIconFilter));
            colorFilterIconDark = new CustomColorFilter(getColor(R.color.lightThemeIconFilter));
            colorFilterPositive = new CustomColorFilter(colorPositive);
            colorFilterNegative = new CustomColorFilter(colorNegative);
            colorFilterSave = new CustomColorFilter(colorAccent);
            colorFilterMenuItem = colorFilterIconDefault;

            mediaController = new MediaController(context);
            adapterAlbum = new AdapterAlbum(
                    recyclerCallback.getRequestManager(),
                    disallowListener,
                    (title, fileName, url) -> eventListener.downloadImage(title, fileName, url),
                    colorFilterMenuItem
            );

            viewPagerFull.setAdapter(adapterAlbum);
            viewPagerFull.setPageTransformer(false, (page, position) -> {
                if (page.getTag() instanceof AdapterAlbum.ViewHolder) {
                    AdapterAlbum.ViewHolder viewHolder = (AdapterAlbum.ViewHolder) page
                            .getTag();
                    if (Utils.inRangeInclusive(-1, position, 1)) {
                        float translationX = -position * page.getWidth();
                        viewHolder.textAlbumIndicator.setTranslationX(translationX);
                        viewHolder.layoutDownload.setTranslationX(translationX);
                    }
                }
            });

            titleTextColor = colorTextPrimaryDefault;
            titleTextColorAlert = colorTextAlertDefault;
            colorTextSecondary = colorTextSecondaryDefault;
            colorFilterMenuItem = colorFilterIconDefault;
        }

        protected void initializeListeners() {
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());

            imageFull.setOnTouchListener(new OnTouchListenerDisallow(disallowListener) {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getPointerCount() > 1) {
                        disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                        disallowListener.requestDisallowInterceptTouchEventVertical(true);
                        return false;
                    }

                    switch (MotionEventCompat.getActionMasked(event)) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();

                            if ((view.canScrollVertically(1) && view.canScrollVertically(-1))) {
                                disallowListener.requestDisallowInterceptTouchEventVertical(true);
                            }
                            else {
                                disallowListener.requestDisallowInterceptTouchEventVertical(false);
                            }
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(false);
                            break;
                        case MotionEvent.ACTION_UP:
                            disallowListener.requestDisallowInterceptTouchEventVertical(false);
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(false);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                            if (event.getY() - startY < 0 && view.canScrollVertically(1)) {
                                disallowListener.requestDisallowInterceptTouchEventVertical(true);
                            }
                            else if (event.getY() - startY > 0 && view.canScrollVertically(-1)) {
                                disallowListener.requestDisallowInterceptTouchEventVertical(true);
                            }
                            break;
                    }
                    return false;
                }
            });
            imageFull.setListener(new ImageViewZoom.Listener() {
                @Override
                public void onTextureSizeExceeded() {
                    eventListener.loadUrl(link.getUrl());
                }

                @Override
                public void onBeforeContentLoad(int width, int height) {
                    recyclerCallback.scrollAndCenter(getAdapterPosition(), height);
                }
            });

            mediaController.setMediaPlayer(new BaseMediaPlayerControl() {
                @Override
                protected MediaPlayer getMediaPlayer() {
                    return mediaPlayer;
                }

                @Override
                public int getBufferPercentage() {
                    return bufferPercentage;
                }
            });

            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
            editTextReply.addTextChangedListener(new BaseTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        link.setReplyText(s.toString());
                    }
                }
            });
        }

        @OnTouch({R.id.layout_root, R.id.toolbar_actions})
        protected boolean onTouchEvent(MotionEvent event) {
            return gestureDetectorDoubleTap.onTouchEvent(event);
        }

        @OnClick({
                R.id.button_comments,
                R.id.button_reply_editor,
                R.id.image_thumbnail,
                R.id.button_send_reply,
                R.id.text_thread_self
        })
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    onClickComments();
                    break;
                case R.id.image_thumbnail:
                    onClickThumbnail();
                    break;
                case R.id.button_send_reply:
                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        sendComment();
                    }
                    break;
                case R.id.button_reply_editor:
                    eventListener.showReplyEditor(link);
                    break;
                case R.id.text_thread_self:
                    toggleToolbarActions();
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                default:
                    eventListener.voteLink(ViewHolderLink.this, link, 1);
                    clearOverlay();
                    return true;
            }
        }

        protected void initializeToolbar() {
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

            UtilsColor.tintMenu(menu, colorFilterMenuItem);

            buttonReplyEditor.setColorFilter(colorFilterMenuItem);
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.item_upvote:
                    eventListener.voteLink(ViewHolderLink.this, link, 1);
                    break;
                case R.id.item_downvote:
                    eventListener.voteLink(ViewHolderLink.this, link, -1);
                    break;
                case R.id.item_share:
                    break;
                case R.id.item_download_image:
                    eventListener.downloadImage(link.getTitle(), link.getId(), link.getUrl());
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
                    UtilsReddit.launchScreenProfile(itemView.getContext(), link);
                    break;
                case R.id.item_copy_text:
                    ClipboardManager clipboard = (ClipboardManager) itemView.getContext()
                            .getSystemService(
                                    Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(
                            resources.getString(R.string.comment),
                            link.getSelfText());
                    clipboard.setPrimaryClip(clip);
                    eventListener.toast(resources.getString(R.string.copied));
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
                                    (dialog, which) -> {
                                        eventListener.deletePost(link);
                                    })
                            .setNegativeButton("No", null)
                            .show();
                    break;
                case R.id.item_view_subreddit:
                    UtilsReddit.launchScreenSubreddit(itemView.getContext(), link);
                    break;
                // Reporting
                // TODO: Use report reasons from subreddit rules
                case R.id.item_report_spam:
                    requestReport("spam");
                    break;
                case R.id.item_report_vote_manipulation:
                    requestReport("vote manipulation");
                    break;
                case R.id.item_report_personal_information:
                    requestReport("personal information");
                    break;
                case R.id.item_report_sexualizing_minors:
                    requestReport("sexualizing minors");
                    break;
                case R.id.item_report_breaking_reddit:
                    requestReport("breaking reddit");
                    break;
                case R.id.item_report_other:
                    View viewDialog = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.dialog_text_input, null, false);
                    final EditText editText = (EditText) viewDialog.findViewById(R.id.edit_text);
                    editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(100)});
                    new AlertDialog.Builder(itemView.getContext())
                            .setView(viewDialog)
                            .setTitle(R.string.item_report)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                eventListener.report(link, "other", editText.getText().toString());
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                            .show();
                    break;
            }
            return true;
        }

        protected Intent getShareIntent() {
            return UtilsReddit.getShareIntentLinkSource(link);
        }

        // TODO: Improve scrolling/centering logic
        protected void scrollToSelf() {
            recyclerCallback.scrollTo(getAdapterPosition());
        }

        private void requestReport(final String reason) {
            String author = link.getAuthor();
            String title = link.getTitle();

            new AlertDialog.Builder(itemView.getContext())
                    .setMessage(resources.getString(R.string.report, title, author, reason))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        eventListener.report(link, reason, null);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
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
                textUsername.setText(resources.getString(R.string.as_author, eventListener.getUser().getName()));
                recyclerCallback.clearDecoration();
                editTextReply.setText(link.getReplyText());
                editTextReply.requestFocus();
                UtilsInput.showKeyboard(editTextReply);
            }
            else {
                UtilsInput.hideKeyboard(editTextReply);
            }
        }

        public void onClickComments() {
            clearOverlay();

            link.setYouTubeTime(youTubePlayer == null ? -1 : youTubePlayer.getCurrentTimeMillis());

            destroyYouTube();
            destroySurfaceView();

            itemView.post(() -> eventListener.onClickComments(link, ViewHolderLink.this, source));
        }

        public void onClickThumbnail() {
            clearOverlay();

            if (youTubeListener != null && !youTubeListener.hideYouTube()) {
                return;
            }
            else if (link.isSelf()) {
                if (TextUtils.isEmpty(link.getSelfText())) {
                    onClickComments();
                }
                else {
                    toggleSelfText();
                }
            }
            else {
                recyclerCallback.hideToolbar();

                String urlString = link.getUrl();
                if (!TextUtils.isEmpty(urlString)) {
                    if (!checkLinkUrl()) {
                        attemptLoadImage();
                    }
                }
            }
        }

        public void toggleSelfText() {
            if (textThreadSelf.isShown()) {
                UtilsAnimation.animateCollapseHeight(textThreadSelf, 0, null);
            }
            else {
                expandFull(true);
                UtilsAnimation.animateExpandHeight(textThreadSelf, UtilsView.getContentWidth(recyclerCallback.getLayoutManager()), 0, null);
            }
        }

        public void addToHistory() {
            historian.add(link);
        }

        @CallSuper
        public void expandFull(boolean expand) {
            if (expanded == expand) {
                return;
            }

            this.expanded = expand;

            if (expand) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                if ((layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT || layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT)
                        && (!(layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) || ((StaggeredGridLayoutManager.LayoutParams) layoutParams).isFullSpan())) {
                    setToolbarMenuVisibility();
                    return;
                }

                float startX = itemView.getX() - recyclerCallback.getLayoutManager().getPaddingStart();

                layoutInner.getLayoutParams().width = itemView.getWidth();
                layoutInner.setTranslationX((int) startX);
                layoutInner.requestLayout();

                if (viewMaskStart != null) {
                    viewMaskStart.getLayoutParams().width = (int) startX;
                }

                if (viewMaskEnd != null && adapterCallback.getRecyclerView() != null) {
                    viewMaskEnd.getLayoutParams().width = (int) (adapterCallback.getRecyclerView().getWidth() - itemView.getWidth() - startX);
                }

                itemView.requestLayout();

                int targetWidth = UtilsView.getContentWidth(recyclerCallback.getLayoutManager());
                itemView.postOnAnimation(() -> {
                    Log.d(TAG, "run() called with with: " + layoutInner.getLayoutParams().width);
                    UtilsAnimation.animateExpandRecyclerItemView(layoutInner, layoutRoot, viewMaskStart, viewMaskEnd, targetWidth, 0, null);

                    setToolbarMenuVisibility();
                });

                scrollToSelf();
            }
        }

        public void setVoteColors() {

            // TODO: Fix this
//            switch (link.getLikes()) {
//                case 1:
//                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterPositive);
//                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    break;
//                case -1:
//                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterNegative);
//                    break;
//                case 0:
//                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    break;
//            }

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

            textThreadTitle.setText(link.getTitle());
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

            if (sharedPreferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false)) {
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
            if (UtilsImage.placeImageUrl(link)) {
                imageFull.setVisibility(View.VISIBLE);
                progressImage.setVisibility(View.VISIBLE);
                expandFull(true);
                recyclerCallback.getLayoutManager().requestLayout();
                itemView.invalidate();
                itemView.post(() -> recyclerCallback.getRequestManager()
                        .load(link.getUrl())
                        .priority(Priority.IMMEDIATE)
                        .listener(new RequestListenerCompletion<String, GlideDrawable>() {
                            @Override
                            protected void onCompleted() {
                                progressImage.setVisibility(View.GONE);
                                if (adapterCallback.getRecyclerView() != null) {
                                    UtilsAnimation.scrollToPositionWithCentering(getAdapterPosition(), adapterCallback.getRecyclerView(), 0, 0, 0, false);
                                }
                            }
                        })
                        .into(new GlideDrawableImageViewTarget(imageFull)));
            }
            else {
                eventListener.loadUrl(link.getUrl());
            }
        }

        public Observer<Album> getObserverAlbum(Link link) {
            return new FinalizingSubscriber<Album>() {
                @Override
                public void start() {
                    super.start();
                    progressImage.setVisibility(View.VISIBLE);
                }

                @Override
                public void next(Album next) {
                    super.next(next);
                    setAlbum(link, next);
                }

                @Override
                public void finish() {
                    super.finish();
                    progressImage.setVisibility(View.GONE);
                }
            };
        }

        public void setAlbum(Link link, Album album) {
            link.setAlbum(album);
            ViewGroup.LayoutParams layoutParams = viewPagerFull.getLayoutParams();
            layoutParams.height = recyclerCallback.getRecyclerHeight();
            viewPagerFull.setLayoutParams(layoutParams);
            viewPagerFull.setVisibility(View.VISIBLE);
            viewPagerFull.requestLayout();
            adapterAlbum.setAlbum(album, colorFilterMenuItem);
            viewPagerFull.setCurrentItem(album.getPage(), false);
            scrollToSelf();
        }

        /**
         * @return true if Link loading has been handled, false otherwise
         */
        public boolean checkLinkUrl() {
            if (link.getAlbum() != null) {
                setAlbum(link, link.getAlbum());
                return true;
            }
            else if (link.getDomain()
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

        private boolean loadImgur() {
            // TODO: Use regex or better parsing system
            if (link.getUrl().contains(Reddit.IMGUR_PREFIX_ALBUM)) {
                loadAlbum(Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX_ALBUM, "/"),
                        link);
            }
            else if (link.getUrl().contains(Reddit.IMGUR_PREFIX_GALLERY)) {
                loadGallery(
                        Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX_GALLERY, "/"),
                        link);
            }
            else if (link.getUrl().contains(UtilsImage.GIFV)) {
                loadGifv(Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX, "."));
            }
            else {
                return false;
            }

            return true;
        }

        private boolean loadGfycat() {
            String gfycatId = Reddit.parseUrlId(link.getUrl(), Reddit.GFYCAT_PREFIX, ".");
            progressImage.setVisibility(View.VISIBLE);

            subscription = reddit.loadGfycat(gfycatId)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> new JSONObject(response).getJSONObject(Reddit.GFYCAT_ITEM)))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new FinalizingSubscriber<JSONObject>() {

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

                        @Override
                        public void finish() {
                            progressImage.setVisibility(View.GONE);
                        }
                    });
            return true;
        }

        public boolean loadYouTube() {

            if (layoutYouTube.isShown()) {
                hideYouTube();
                return true;
            }

            if (youTubePlayer != null) {
                layoutYouTube.setVisibility(View.VISIBLE);
                return true;
            }

            Uri uri = Uri.parse(link.getUrl());
            String host = uri.getHost();

            if (host.equalsIgnoreCase("youtube.com")
                    || host.equalsIgnoreCase("youtu.be")) {
                String id = uri.getQueryParameter("v");
                if (TextUtils.isEmpty(id)) {
                    /*
                        Regex taken from Gubatron at
                        http://stackoverflow.com/questions/24048308/how-to-get-the-video-id-from-a-youtube-url-with-regex-in-java
                    */
                    Pattern pattern = Pattern.compile(".*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=)([^#&\\?]*).*");
                    final Matcher matcher = pattern.matcher(link.getUrl());

                    if (matcher.matches()) {
                        id = matcher.group(1);
                    }
                }

                if (TextUtils.isEmpty(id)) {
                    return false;
                }

                int time = 0;
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

                loadYouTubeVideo(id, time);
                return true;
            }

            return false;
        }

        private void loadGallery(String id, final Link link) {
            subscription = reddit.loadImgurGallery(id)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Album.fromJson(new JSONObject(response).getJSONObject("data"))))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getObserverAlbum(link));
        }

        private void loadAlbum(String id, final Link link) {
            subscription = reddit.loadImgurAlbum(id)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Album.fromJson(new JSONObject(response).getJSONObject("data"))))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getObserverAlbum(link));
        }

        private void loadGifv(String id) {
            reddit.loadImgurImage(id)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Image.fromJson(new JSONObject(response).getJSONObject("data"))))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new FinalizingSubscriber<Image>() {
                        @Override
                        public void start() {
                            progressImage.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void next(Image image) {
                            if (!TextUtils.isEmpty(image.getMp4())) {
                                loadVideo(image.getMp4(), (float) image.getHeight() / image.getWidth());
                            }
                            else if (!TextUtils.isEmpty(image.getWebm())) {
                                loadVideo(image.getWebm(), (float) image.getHeight() / image.getWidth());
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

            if (surfaceVideo == null) {
                surfaceVideo = new SurfaceView(itemView.getContext());
                layoutFull.addView(surfaceVideo, layoutFull.getChildCount() - 1);

                surfaceVideo.setOnTouchListener((v, event) -> {
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
                });
                mediaController.setAnchorView(surfaceVideo);
            }

            surfaceVideo.setVisibility(View.VISIBLE);

            surfaceVideo.getLayoutParams().height = (int) (itemView.getWidth() * heightRatio);
            surfaceVideo.requestLayout();
            surfaceVideo.setVisibility(View.GONE);
            surfaceVideo.setVisibility(View.VISIBLE);

            surfaceHolder = surfaceVideo.getHolder();
            surfaceHolder.addCallback(ViewHolderLink.this);
            surfaceHolder.setSizeFromLayout();
            scrollToSelf();
        }

        public void loadYouTubeVideo(final String id, final int timeInMillis) {
            if (youTubeListener != null) {
                youTubeListener.loadYouTubeVideo(link, id, timeInMillis);
                return;
            }

            link.setYouTubeId(id);
            callbackYouTubeDestruction.destroyYouTubePlayerFragments();
            layoutYouTube.postOnAnimation(() -> {
                youTubeFragment = new YouTubePlayerSupportFragment();
                layoutYouTube.setId(youTubeViewId);
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .add(youTubeViewId, youTubeFragment, String.valueOf(youTubeViewId))
                        .commit();
                youTubeFragment.initialize(ApiKeys.YOUTUBE_API_KEY,
                        new YouTubePlayer.OnInitializedListener() {
                            @Override
                            public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                    final YouTubePlayer youTubePlayer1,
                                    boolean b) {
                                youTubePlayer = youTubePlayer1;
                                youTubePlayer.setFullscreenControlFlags(
                                        YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);
                                youTubePlayer.setManageAudioFocus(false);
                                youTubePlayer.setOnFullscreenListener(
                                        fullscreen -> {
                                            isYouTubeFullscreen = fullscreen;
                                            youTubePlayer.setFullscreen(fullscreen);
                                        });
                                youTubePlayer.setPlayerStateChangeListener(new SimplePlayerStateChangeListener() {
                                    @Override
                                    public void onVideoStarted() {
                                        youTubePlayer.seekToMillis(timeInMillis);
                                        youTubePlayer.setPlayerStateChangeListener(new SimplePlayerStateChangeListener());
                                    }
                                });
                                youTubePlayer.loadVideo(id);
                                layoutYouTube.setVisibility(View.VISIBLE);
                                scrollToSelf();
                            }

                            @Override
                            public void onInitializationFailure(YouTubePlayer.Provider provider,
                                    YouTubeInitializationResult youTubeInitializationResult) {
                                eventListener.toast(resources.getString(R.string.error_youtube));
                            }
                        });
            });
        }

        private void hideYouTube() {
            layoutYouTube.setVisibility(View.GONE);
        }

        public void setToolbarMenuVisibility() {
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

            toolbarActions.post(() -> calculateVisibleToolbarItems(itemView.getWidth()));
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

        public void onBind(Link link, boolean showSubreddit) {
            this.link = link;
            this.showSubreddit = showSubreddit;

            if (link.isReplyExpanded() && !TextUtils.isEmpty(link.getReplyText())) {
                expandFull(true);
                editTextReply.setText(link.getReplyText());
                layoutContainerReply.setVisibility(View.VISIBLE);
            }
            else {
                link.setReplyExpanded(false);
                layoutContainerReply.setVisibility(View.GONE);
            }

            setVoteColors();

            if (sharedPreferences.getBoolean(AppSettings.PREF_DIM_POSTS, true)) {
                viewOverlay.setVisibility(isInHistory() && !link.isReplyExpanded() ? View.VISIBLE : View.GONE);
            }
        }

        public void onRecycle() {
            UtilsAnimation.clearAnimation(textThreadSelf, layoutInner);
            textThreadSelf.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutInner.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutInner.setTranslationX(0);

            if (viewMaskStart != null) {
                viewMaskStart.getLayoutParams().width = 0;
            }

            if (viewMaskEnd != null) {
                viewMaskEnd.getLayoutParams().width = 0;
            }

            titleTextColor = colorTextPrimaryDefault;
            titleTextColorAlert = colorTextAlertDefault;
            colorTextSecondary = colorTextSecondaryDefault;
            colorFilterMenuItem = colorFilterIconDefault;
            isYouTubeFullscreen = false;
            layoutContainerExpand.setVisibility(View.GONE);
            itemView.setVisibility(View.VISIBLE);

            destroyWebViews();
            destroySurfaceView();

            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
                subscription = null;
            }

            destroyYouTube();

            layoutYouTube.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imagePlay.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);
            picasso.cancelRequest(imageThumbnail);
            imageThumbnail.setImageDrawable(null);
            imageFull.setImageDrawable(null);
            imageFull.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            imageFull.setVisibility(View.GONE);
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
            eventListener.markNsfw(link).subscribe(new ObserverEmpty<>());
            syncTitleColor();
        }

        public void syncTitleColor() {
            textThreadTitle.setTextColor(link.isOver18() ? titleTextColorAlert : titleTextColor);
            textThreadInfo.setTextColor(colorTextSecondary);
            itemMarkNsfw.setTitle(link.isOver18() ? R.string.unmark_nsfw : R.string.mark_nsfw);
        }

        public void destroyWebViews() {
            adapterAlbum.setAlbum(new Album(), null);
            layoutFull.requestLayout();
        }

        public void destroySurfaceView() {
            if (surfaceVideo != null) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                surfaceVideo.setVisibility(View.GONE);
                mediaController.setAnchorView(layoutFull);
                mediaController.hide();
                layoutFull.removeView(surfaceVideo);
                surfaceVideo = null;
            }
        }

        public int getBackgroundColor() {
            return link.getBackgroundColor();
        }

        public void clearOverlay() {
            addToHistory();
            viewOverlay.setVisibility(View.GONE);
        }

        // TODO: Calculate which Views are visible
        public void setVisibility(int visibility) {
            layoutFull.setVisibility(visibility);
            progressImage.setVisibility(visibility);
            viewPagerFull.setVisibility(visibility);
            imagePlay.setVisibility(visibility);
            imageThumbnail.setVisibility(visibility);
            textThreadFlair.setVisibility(visibility);
            viewMargin.setVisibility(visibility);
            textThreadTitle.setVisibility(visibility);
            textThreadSelf.setVisibility(visibility);
            textThreadInfo.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            layoutContainerExpand.setVisibility(visibility);
            toolbarActions.setVisibility(visibility);
            layoutContainerReply.setVisibility(visibility);
            editTextReply.setVisibility(visibility);
            buttonSendReply.setVisibility(visibility);
            layoutYouTube.setVisibility(visibility);
            itemView.setVisibility(visibility);
        }

        public void setYouTubeListener(YouTubeListener youTubeListener) {
            this.youTubeListener = youTubeListener;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceHolder = holder;
            if (uriVideo == null) {
                return;
            }

            bufferPercentage = 0;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> bufferPercentage = percent);
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaController.hide();
                surfaceHolder.setFixedSize(mp.getVideoWidth(), mp.getVideoHeight());
                mp.start();
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

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
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
            if (link.isSelf()) {
                imageThumbnail.getLocationOnScreen(location);
            }
            else {
                itemView.getLocationOnScreen(location);
            }
            return location;
        }

        public Link getLink() {
            return link;
        }

        public void destroyYouTube() {
            if (youTubePlayer != null) {
                isYouTubeFullscreen = false;
                youTubePlayer.release();
                youTubePlayer = null;
            }

            if (youTubeFragment != null) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .remove(youTubeFragment)
                        .commit();
            }

            layoutYouTube.setVisibility(View.GONE);
        }

        public static class State implements Parcelable {
            private boolean isSelfExpanded;
            private boolean isToolbarActionsExpanded;

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeByte(isSelfExpanded ? (byte) 1 : (byte) 0);
                dest.writeByte(isToolbarActionsExpanded ? (byte) 1 : (byte) 0);
            }

            public State() {
            }

            protected State(Parcel in) {
                this.isSelfExpanded = in.readByte() != 0;
                this.isToolbarActionsExpanded = in.readByte() != 0;
            }

            public static final Creator<State> CREATOR = new Creator<State>() {
                @Override
                public State createFromParcel(Parcel source) {
                    return new State(source);
                }

                @Override
                public State[] newArray(int size) {
                    return new State[size];
                }
            };
        }

        public interface EventListenerGeneral {

            void sendComment(String name, String text);
            void sendMessage(String name, String text);
            void save(Link link);
            void save(Comment comment);
            void toast(String text);
            boolean isUserLoggedIn();
            void voteLink(ViewHolderLink viewHolderLink, Link link, int vote);
            void deletePost(Link link);
            void report(Thing thing, String reason, String otherReason);
            void hide(Link link);
            void markRead(Thing thing);
            Observable<String> markNsfw(Link link);
            User getUser();
        }

        public interface EventListener extends EventListenerGeneral {
            void onClickComments(Link link, ViewHolderLink viewHolderLink, Source source);
            void loadUrl(String url);
            void downloadImage(String title, String fileName, String url);
            void editLink(Link link);
            void showReplyEditor(Replyable replyable);
            void loadWebFragment(String url);
            void launchScreen(Intent intent);
        }

    }

}
