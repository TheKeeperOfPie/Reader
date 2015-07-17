/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
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
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.utils.AnimationUtils;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.RecyclerCallback;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.AdapterLinkList;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */

public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;
    private static final int MAX_ALPHA = 180;
    private static final int ALPHA_LEVELS = 8;
    private final int colorLink;
    private ControllerUser controllerUser;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private ViewHolderComment.EventListener eventListenerComment;
    private DisallowListener disallowListener;
    private ViewHolderComment.ReplyCallback replyCallback;
    private RecyclerCallback recyclerCallback;
    private FragmentComments.YouTubeListener youTubeListener;

    private ControllerComments controllerComments;
    private AdapterLink.ViewHolderBase viewHolderLink;
    private int thumbnailSize;
    private boolean isGrid;
    private boolean animationFinished;
    private boolean isSelfTextLoaded;

    public AdapterCommentList(Activity activity,
            ControllerComments controllerComments,
            ControllerUser controllerUser,
            AdapterLink.ViewHolderBase.EventListener eventListenerBase,
            ViewHolderComment.EventListener eventListenerComment,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback,
            ViewHolderComment.ReplyCallback replyCallback,
            FragmentComments.YouTubeListener youTubeListener,
            boolean isGrid, int colorLink) {
        this.controllerUser = controllerUser;
        this.eventListenerBase = eventListenerBase;
        this.eventListenerComment = eventListenerComment;
        this.disallowListener = disallowListener;
        this.recyclerCallback = recyclerCallback;
        this.replyCallback = replyCallback;
        this.youTubeListener = youTubeListener;
        this.isGrid = isGrid;
        this.colorLink = colorLink;
        this.controllerComments = controllerComments;
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        this.thumbnailSize = displayMetrics.widthPixels / 2;
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
            if (isGrid) {
                viewHolderLink = new AdapterLinkGrid.ViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.cell_link, parent, false), eventListenerBase,
                        disallowListener,
                        recyclerCallback,
                        thumbnailSize) {

                    @Override
                    public Intent getShareIntent() {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                        shareIntent
                                .putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());
                        return shareIntent;
                    }

                    @Override
                    public boolean isInHistory() {
                        return false;
                    }

                    @Override
                    public void loadBackgroundColor() {
                        if (colorLink != 0) {
                            itemView.setBackgroundColor(colorLink);
                            setTextColors(colorLink);
                        }
                        else {
                            super.loadBackgroundColor();
                        }
                    }

                    @Override
                    public void onBind(Link link,
                            boolean showSubbreddit,
                            String userName) {
                        super.onBind(link, showSubbreddit, userName);
                        if (animationFinished) {
                            if (!isSelfTextLoaded) {
                                if (!TextUtils.isEmpty(link.getSelfText())) {
                                    loadSelfText();
                                }
                                isSelfTextLoaded = true;
                            }
                            else if (link.isSelf() && !TextUtils
                                    .isEmpty(link.getSelfText())) {
                                textThreadSelf.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void loadComments() {
                        // Override to prevent action
                    }

                    @Override
                    public void loadYouTubeVideo(Link link, String id, int timeInMillis) {
                        youTubeListener.loadYouTube(link, id, timeInMillis);
                    }

                    @Override
                    public void onClickThumbnail() {
                        if (youTubeListener.hideYouTube()) {
                            super.onClickThumbnail();
                        }
                    }
                };
                if (colorLink != 0) {
                    viewHolderLink.itemView.setBackgroundColor(colorLink);
                }
            }
            else {
                viewHolderLink = new AdapterLinkList.ViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_link, parent, false), eventListenerBase,
                        disallowListener,
                        recyclerCallback) {

                    @Override
                    public Intent getShareIntent() {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                        shareIntent
                                .putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());
                        return shareIntent;
                    }

                    @Override
                    public boolean isInHistory() {
                        return false;
                    }

                    @Override
                    public void onBind(Link link,
                            boolean showSubreddit,
                            String userName) {
                        super.onBind(link, showSubreddit, userName);
                        if (animationFinished) {
                            if (!isSelfTextLoaded) {
                                if (!TextUtils.isEmpty(link.getSelfText())) {
                                    loadSelfText();
                                }
                                isSelfTextLoaded = true;
                            }
                            else if (link.isSelf() && !TextUtils
                                    .isEmpty(link.getSelfText())) {
                                textThreadSelf.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void loadComments() {
                        // Override to prevent action
                    }

                    @Override
                    public void loadYouTubeVideo(Link link, String id, int timeInMillis) {
                        youTubeListener.loadYouTube(link, id, timeInMillis);
                    }

                    @Override
                    public void onClickThumbnail() {
                        if (youTubeListener.hideYouTube()) {
                            super.onClickThumbnail();
                        }
                    }
                };
            }

            return viewHolderLink;
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_comment, parent, false), eventListenerBase,
                eventListenerComment, disallowListener, replyCallback);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (!controllerComments.isLoading() && position > controllerComments.getItemCount() - 5) {
            controllerComments.loadMoreComments();
        }

        if (getItemViewType(position) == VIEW_LINK) {
            AdapterLink.ViewHolderBase viewHolderBase = (AdapterLink.ViewHolderBase) holder;

            viewHolderBase
                    .onBind(controllerComments.getMainLink(), controllerComments.showSubreddit(),
                            controllerUser.getUser().getName());

            viewHolderBase.itemView.invalidate();

            viewHolderLink = viewHolderBase;
        }
        else {
            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;
            viewHolderComment.onBind(controllerComments.getComment(position),
                    controllerUser.getUser().getName());
        }

    }

    @Override
    public int getItemCount() {
        int count = controllerComments.getItemCount();
        if (count > 0) {
            if (!animationFinished) {
                return 1;
            }
        }
        return count;
    }

    public void collapseViewHolderLink() {
        // TODO: Support collapsing enlarged thumbnail and self text
        if (viewHolderLink == null) {
            return;
        }

        if (controllerComments.getMainLink().isSelf()) {
            viewHolderLink.textThreadSelf.setVisibility(View.GONE);
        }
        else {
            viewHolderLink.destroyWebViews();
            viewHolderLink.onRecycle();
            viewHolderLink.onBind(controllerComments.getMainLink(),
                    controllerComments.showSubreddit(),
                    controllerUser.getUser().getName());
        }
    }

    public void destroyViewHolderLink() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyWebViews();
        }
    }

    public void setAnimationFinished(boolean isAnimationFinished) {
        this.animationFinished = isAnimationFinished;
        notifyDataSetChanged();
    }

    public static class ViewHolderComment extends RecyclerView.ViewHolder
            implements Toolbar.OnMenuItemClickListener {

        protected Comment comment;

        protected View viewIndent;
        protected View viewIndicator;
        protected TextView textComment;
        protected TextView textInfo;
        protected TextView textHidden;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected RelativeLayout layoutContainerExpand;
        protected Toolbar toolbarActions;
        protected MenuItem itemViewLink;
        protected MenuItem itemJumpParent;
        protected MenuItem itemCollapse;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemReply;
        protected MenuItem itemSave;
        protected MenuItem itemEdit;
        protected MenuItem itemDelete;
        protected MenuItem itemReport;
        protected Drawable drawableUpvote;
        protected Drawable drawableDownvote;
        protected PorterDuffColorFilter colorFilterSave;
        protected PorterDuffColorFilter colorFilterPositive;
        protected PorterDuffColorFilter colorFilterNegative;
        protected PorterDuffColorFilter colorFilterMenuItem;
        protected View viewIndicatorContainer;
        protected RelativeLayout layoutContainerCollapsed;
        protected View viewIndicatorCollapsed;
        protected TextView textCollapsed;

        protected AdapterLink.ViewHolderBase.EventListener eventListenerBase;
        protected EventListener eventListener;
        protected DisallowListener disallowListener;
        protected ReplyCallback replyCallback;
        protected String userName;
        protected int indentWidth;
        protected int toolbarItemWidth;
        protected SharedPreferences preferences;
        protected Resources resources;
        protected int colorTextSecondary;
        protected int colorTextPrimary;

        public ViewHolderComment(final View itemView,
                AdapterLink.ViewHolderBase.EventListener eventListenerBase,
                EventListener eventListener,
                DisallowListener disallowListener,
                ReplyCallback replyCallback,
                final ControllerProfile.Listener listener) {
            this(itemView, eventListenerBase, eventListener, disallowListener, replyCallback);
            itemViewLink.setVisible(true);
            itemViewLink.setEnabled(true);
            itemViewLink.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            itemViewLink.setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            listener.loadLink(comment);
                            return true;
                        }
                    });
        }

        public ViewHolderComment(final View itemView,
                AdapterLink.ViewHolderBase.EventListener eventListenerBase,
                EventListener eventListener,
                DisallowListener disallowListener,
                ReplyCallback replyCallback) {
            super(itemView);

            this.eventListenerBase = eventListenerBase;
            this.eventListener = eventListener;
            this.disallowListener = disallowListener;
            this.replyCallback = replyCallback;

            intiialize();
            initializeToolbar();
            initializeListeners();
        }

        private void intiialize() {

            resources = itemView.getResources();
            preferences = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());
            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(new int[] {android.R.attr.textColor, android.R.attr.textColorSecondary});
            colorTextPrimary = typedArray.getColor(0, resources.getColor(R.color.darkThemeTextColor));
            colorTextSecondary = typedArray.getColor(1, resources.getColor(R.color.darkThemeTextColorMuted));
            typedArray.recycle();

            this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
            this.drawableDownvote = resources.getDrawable(
                    R.drawable.ic_keyboard_arrow_down_white_24dp);

            indentWidth = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, resources.getDisplayMetrics());
            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    resources.getDisplayMetrics());

            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            viewIndicatorContainer = itemView.findViewById(R.id.view_indicator_container);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);

            viewIndicatorCollapsed = itemView.findViewById(R.id.view_indicator_collapsed);
            layoutContainerCollapsed = (RelativeLayout) itemView.findViewById(R.id.layout_container_collapsed);
            textCollapsed = (TextView) itemView.findViewById(R.id.text_collapsed);

        }

        private void initializeListeners() {

            textComment.setMovementMethod(LinkMovementMethod.getInstance());
            textComment.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        sendReply();
                        InputMethodManager inputManager = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(itemView.getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
            });

            final GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    itemCollapse.setIcon(eventListener.toggleComment(getAdapterPosition()) ?
                            R.drawable.ic_arrow_drop_up_white_24dp :
                            R.drawable.ic_arrow_drop_down_white_24dp);
                    itemCollapse.getIcon().setColorFilter(colorFilterMenuItem);
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    expandToolbarActions();
                    return true;
                }
            });

            View.OnTouchListener onTouchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetectorCompat.onTouchEvent(event);
                }
            };

            View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    eventListener.voteComment(ViewHolderComment.this, comment, 1);
                    return true;
                }
            };
            textComment.setOnLongClickListener(longClickListener);
            textInfo.setOnLongClickListener(longClickListener);
            this.itemView.setOnLongClickListener(longClickListener);

            textComment.setOnTouchListener(onTouchListener);
            textInfo.setOnTouchListener(onTouchListener);
            itemView.setOnTouchListener(onTouchListener);

            editTextReply.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    comment.setReplyText(s.toString());
                }
            });

        }

        private void sendReply() {
            if (comment.isEditMode()) {
                eventListener.editComment(comment, editTextReply.getText().toString());
                comment.setEdited(System.currentTimeMillis());
            }
            else {
                eventListener.sendComment(comment.getName(), editTextReply.getText().toString());
            }
            comment.setReplyExpanded(!comment.isReplyExpanded());
            layoutContainerReply.setVisibility(View.GONE);
        }

        private void initializeToolbar() {

            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_comment);
            toolbarActions.setOnMenuItemClickListener(this);

            Menu menu = toolbarActions.getMenu();
            itemViewLink = menu.findItem(R.id.item_view_link);
            itemJumpParent = menu.findItem(R.id.item_jump_parent);
            itemCollapse = menu.findItem(R.id.item_collapse);
            itemUpvote = menu.findItem(R.id.item_upvote);
            itemDownvote = menu.findItem(R.id.item_downvote);
            itemReply = menu.findItem(R.id.item_reply);
            itemSave = menu.findItem(R.id.item_save);
            itemEdit = menu.findItem(R.id.item_edit);
            itemDelete = menu.findItem(R.id.item_delete);
            itemReport = menu.findItem(R.id.item_report);


            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(new int[] {R.attr.colorIconFilter});
            int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
            typedArray.recycle();

            colorFilterMenuItem = new PorterDuffColorFilter(colorIconFilter,
                    PorterDuff.Mode.MULTIPLY);

            colorFilterPositive = new PorterDuffColorFilter(resources.getColor(
                    R.color.positiveScore),
                    PorterDuff.Mode.MULTIPLY);
            colorFilterNegative = new PorterDuffColorFilter(resources.getColor(
                    R.color.negativeScore),
                    PorterDuff.Mode.MULTIPLY);
            colorFilterSave = new PorterDuffColorFilter(resources.getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().setColorFilter(colorFilterMenuItem);
            }

        }

        public void expandToolbarActions() {

            if (comment.isMore()) {
                eventListener.loadNestedComments(comment);
                return;
            }

            if (!toolbarActions.isShown()) {

                setVoteColors();

                setToolbarMenuVisibility();
            }

            AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
        }

        private void setToolbarMenuVisibility() {
            // TODO: Move instances to shared class to prevent code duplication

            Menu menu = toolbarActions.getMenu();

            int maxNum = (itemView.getWidth() - getIndentWidth()) / toolbarItemWidth;
            int numShown = 0;

            boolean loggedIn = !TextUtils.isEmpty(userName);
            boolean isAuthor = comment.getAuthor()
                    .equals(userName);

            if (eventListener.isCommentExpanded(getAdapterPosition())) {
                itemCollapse.setIcon(R.drawable.ic_arrow_drop_up_white_24dp);
            }
            else {
                itemCollapse.setIcon(R.drawable.ic_arrow_drop_down_white_24dp);
            }
            itemCollapse.getIcon().setColorFilter(colorFilterMenuItem);

            itemEdit.setEnabled(isAuthor);
            itemEdit.setVisible(isAuthor);
            itemDelete.setEnabled(isAuthor);
            itemDelete.setVisible(isAuthor);
            itemJumpParent.setEnabled(comment.getLevel() > 0);
            itemJumpParent.setVisible(comment.getLevel() > 0);

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
            itemReport.setVisible(loggedIn);
            itemCollapse.setVisible(eventListener.hasChildren(comment));

            for (int index = 0; index < menu.size(); index++) {
                if (!loggedIn) {
                    switch (menu.getItem(index)
                            .getItemId()) {
                        case R.id.item_upvote:
                        case R.id.item_downvote:
                        case R.id.item_reply:
                            continue;
                    }
                }

                if (menu.getItem(index).getItemId() == R.id.item_view_link) {
                    continue;
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

            syncSaveIcon();
        }

        public void setVoteColors() {
            switch (comment.getLikes()) {
                case 1:
                    drawableUpvote.mutate().setColorFilter(colorFilterPositive);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.setColorFilter(colorFilterMenuItem);
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case -1:
                    drawableDownvote.mutate().setColorFilter(colorFilterNegative);
                    itemDownvote.setIcon(drawableDownvote);
                    drawableUpvote.setColorFilter(colorFilterMenuItem);
                    itemUpvote.setIcon(drawableUpvote);
                    break;
                case 0:
                    drawableUpvote.setColorFilter(colorFilterMenuItem);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.setColorFilter(colorFilterMenuItem);
                    itemDownvote.setIcon(drawableDownvote);
                    break;
            }
        }

        private int getIndentWidth() {
            return comment.getLevel() > 10 ? indentWidth * 10 : indentWidth * comment.getLevel();
        }

        public void onBind(Comment comment, String userName) {

            this.comment = comment;
            this.userName = userName;
            itemView.getBackground().setState(new int[0]);

            layoutContainerReply
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
            layoutContainerExpand
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);

            if (comment.isReplyExpanded()) {
                editTextReply.setText(comment.getReplyText());
            }

            int alphaLevel = comment.getLevel() * MAX_ALPHA / ALPHA_LEVELS;

            int overlayColor = ColorUtils.setAlphaComponent(0xFF000000,
                    alphaLevel <= MAX_ALPHA ? alphaLevel : MAX_ALPHA);
            int indicatorColor = ColorUtils.compositeColors(overlayColor,
                    resources.getColor(R.color.colorPrimary));

            viewIndicator.setBackgroundColor(indicatorColor);
            viewIndicatorContainer.setBackgroundColor(indicatorColor);

            if (comment.getCollapsed() > 0) {
                viewIndicatorCollapsed.setBackgroundColor(indicatorColor);
                textCollapsed.setText(
                        comment.getCollapsed() + " " + resources
                                .getString(R.string.comments_collapsed));
                layoutContainerCollapsed.setVisibility(View.VISIBLE);
            }
            else {
                layoutContainerCollapsed.setVisibility(View.GONE);
            }

            ViewGroup.LayoutParams layoutParams = viewIndent.getLayoutParams();
            layoutParams.width = getIndentWidth();
            viewIndent.setLayoutParams(layoutParams);

            if (comment.isMore()) {
                if (comment.getCount() == 0) {
                    textComment.setText(R.string.continue_thread);
                }
                else {
                    textComment.setText(R.string.load_more_comments);
                }
                textInfo.setText("");
            }
            else {
                textComment.setText(comment.getBodyHtml());

                int colorPositive = resources.getColor(R.color.positiveScore);
                int colorNegative = resources.getColor(R.color.negativeScore);


                String voteIndicator = "";
                int voteColor = 0;

                switch (comment.getLikes()) {
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

                Spannable spannableScore = new SpannableString(String.valueOf(comment.getScore()));

                if (!comment.isScoreHidden()) {
                    spannableScore.setSpan(new ForegroundColorSpan(
                                    comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                            spannableScore.length(),
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }

                String suffixAuthor = "";
                int color = colorTextSecondary;

                if (comment.getLinkAuthor().equals(comment.getAuthor())) {
                    color = resources.getColor(R.color.colorAccent);
                }
                else if (userName.equals(comment.getAuthor())) {
                    color = resources.getColor(R.color.colorPrimary);
                }
                else {
                    switch (comment.getDistinguished()) {
                        case MODERATOR:
                            color = resources.getColor(R.color.moderator);
                            suffixAuthor = resources.getString(R.string.prefix_moderator);
                            break;
                        case ADMIN:
                            color = resources.getColor(R.color.admin);
                            suffixAuthor = resources.getString(R.string.prefix_admin);
                            break;
                        case SPECIAL:
                            color = resources.getColor(R.color.special);
                            suffixAuthor = resources.getString(R.string.prefix_special);
                            break;
                    }
                }

                Spannable spannableAuthor = new SpannableString(comment.getAuthor() + suffixAuthor);
                spannableAuthor.setSpan(new ForegroundColorSpan(color), 0, spannableAuthor.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                String flair = TextUtils.isEmpty(comment.getAuthorFlairText()) || "null"
                        .equals(comment.getAuthorFlairText()) ? " " : " (" + Html
                        .fromHtml(comment.getAuthorFlairText()) + ") ";

                CharSequence timestamp =
                        preferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false) ? DateUtils
                                .formatDateTime(itemView.getContext(), comment.getCreatedUtc(),
                                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR) :
                                DateUtils.getRelativeTimeSpanString(comment.getCreatedUtc());

                textInfo.setTextColor(comment.isNew() ? resources.getColor(
                        R.color.textColorAlert) : colorTextSecondary);

                textInfo.setText(TextUtils.concat(spannableVote, spannableScore, " by ", spannableAuthor, flair,
                        timestamp, comment.getEdited() > 0 ? "*" : ""));

                if (comment.getEdited() > 1) {
                    textHidden.setText(
                            "Edited " + DateUtils.getRelativeTimeSpanString(comment.getEdited()));
                    textHidden.setVisibility(View.VISIBLE);
                }
                else {
                    textHidden.setVisibility(View.GONE);
                }

            }

            textComment.setTextColor(comment.getGilded() > 0 ? resources.getColor(R.color.gildedComment) : colorTextPrimary);

        }

        public void syncSaveIcon() {
            if (comment.isSaved()) {
                itemSave.getIcon().mutate().setColorFilter(colorFilterSave);
            }
            else {
                itemSave.getIcon().setColorFilter(colorFilterMenuItem);
            }
        }

        public void toggleReply() {
            comment.setReplyExpanded(!comment.isReplyExpanded());
            buttonSendReply.setText(
                    comment.isEditMode() ? itemView.getContext().getString(R.string.send_edit) :
                            itemView.getContext().getString(R.string.send_reply));
            editTextReply.setText(comment.getReplyText());
            layoutContainerReply.setVisibility(
                    comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
            if (comment.isReplyExpanded()) {
                editTextReply.clearFocus();
                InputMethodManager inputManager = (InputMethodManager) itemView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                editTextReply.requestFocus();
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_jump_parent:
                    eventListener.jumpToParent(comment);
                    break;
                case R.id.item_collapse:
                    item.setIcon(eventListener.toggleComment(getAdapterPosition()) ?
                            R.drawable.ic_arrow_drop_up_white_24dp :
                            R.drawable.ic_arrow_drop_down_white_24dp);
                    item.getIcon().setColorFilter(colorFilterMenuItem);
                    break;
                case R.id.item_upvote:
                    eventListener.voteComment(ViewHolderComment.this, comment, 1);
                    break;
                case R.id.item_downvote:
                    eventListener.voteComment(ViewHolderComment.this, comment, -1);
                    break;
                case R.id.item_reply:
                    comment.setEditMode(false);
                    comment.setReplyText(comment.getReplyText());
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveComment(comment);
                    break;
                case R.id.item_view_profile:
                    Intent intent = new Intent(itemView.getContext(), MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/user/" + comment.getAuthor());
                    eventListenerBase.startActivity(intent);
                    break;
                case R.id.item_copy_text:
                    ClipboardManager clipboard = (ClipboardManager) itemView.getContext()
                            .getSystemService(
                                    Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(
                            resources.getString(R.string.comment),
                            comment.getBody());
                    clipboard.setPrimaryClip(clip);
                    eventListenerBase.toast(resources.getString(
                            R.string.copied));
                    break;
                case R.id.item_edit:
                    comment.setEditMode(true);
                    comment.setReplyText(comment.getBody());
                    toggleReply();
                    break;
                case R.id.item_delete:
                    // TODO: Test if truncate needed

                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete comment?")
                            .setMessage(comment.getBodyHtml())
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {

                                            eventListener.deleteComment(
                                                    comment);
                                        }
                                    })
                            .setNegativeButton("No", null)
                            .show();
                    break;
                // Reporting
                case R.id.item_report_spam:
                    eventListenerBase.report(comment, "spam", null);
                    break;
                case R.id.item_report_vote_manipulation:
                    eventListenerBase.report(comment, "vote manipulation", null);
                    break;
                case R.id.item_report_personal_information:
                    eventListenerBase.report(comment, "personal information", null);
                    break;
                case R.id.item_report_sexualizing_minors:
                    eventListenerBase.report(comment, "sexualizing minors", null);
                    break;
                case R.id.item_report_breaking_reddit:
                    eventListenerBase.report(comment, "breaking reddit", null);
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
                                    eventListenerBase.report(comment, "other",
                                            editText.getText().toString());
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

        private void saveComment(final Comment comment) {
            eventListenerBase.save(comment);
            syncSaveIcon();
        }

        public void setVisibility(int visibility) {
            viewIndent.setVisibility(visibility);
            viewIndicator.setVisibility(visibility);
            viewIndicatorContainer.setVisibility(visibility);
            textComment.setVisibility(visibility);
            textInfo.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            layoutContainerReply.setVisibility(visibility);
            editTextReply.setVisibility(visibility);
            buttonSendReply.setVisibility(visibility);
            toolbarActions.setVisibility(visibility);
            layoutContainerExpand.setVisibility(visibility);
            itemView.setVisibility(visibility);
        }

        public interface EventListener {
            void loadNestedComments(Comment comment);
            boolean isCommentExpanded(int position);
            boolean hasChildren(Comment comment);
            void voteComment(ViewHolderComment viewHolderComment, Comment comment, int vote);
            boolean toggleComment(int position);
            void deleteComment(Comment comment);
            void editComment(Comment comment, String text);
            void sendComment(String name, String text);
            void jumpToParent(Comment comment);
        }

        public interface ReplyCallback{
            void onReplyShown();
        }

    }
}