package com.winsonchiu.reader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

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
    private RecyclerCallback recyclerCallback;
    private FragmentComments.YouTubeListener youTubeListener;

    private ControllerComments controllerComments;
    private AdapterLink.ViewHolderBase viewHolderLink;
    private int thumbnailWidth;
    private boolean isGrid;
    private boolean isInitialized;
    private boolean animationFinished;

    public AdapterCommentList(Activity activity,
            ControllerComments controllerComments,
            ControllerUser controllerUser,
            AdapterLink.ViewHolderBase.EventListener eventListenerBase,
            ViewHolderComment.EventListener eventListenerComment,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback,
            FragmentComments.YouTubeListener youTubeListener,
            boolean isGrid, int colorLink) {
        this.controllerUser = controllerUser;
        this.eventListenerBase = eventListenerBase;
        this.eventListenerComment = eventListenerComment;
        this.disallowListener = disallowListener;
        this.recyclerCallback = recyclerCallback;
        this.youTubeListener = youTubeListener;
        this.isGrid = isGrid;
        this.colorLink = colorLink;
        this.controllerComments = controllerComments;
        Resources resources = activity.getResources();
        this.thumbnailWidth = resources.getDisplayMetrics().widthPixels / 2;
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
                        thumbnailWidth) {

                    @Override
                    public void loadBackgroundColor(Drawable drawable, int position) {
                        if (colorLink != 0) {
                            itemView.setBackgroundColor(colorLink);
                        }
                        else {
                            super.loadBackgroundColor(drawable, position);
                        }
                    }

                    @Override
                    public void onBind(Link link,
                            boolean showSubbreddit,
                            String userName) {
                        super.onBind(link, showSubbreddit, userName);
                        if (link.isSelf() && !TextUtils.isEmpty(link.getSelfText())) {
                            textThreadSelf.setVisibility(View.VISIBLE);
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
                // TODO: Fix margin when expanding comment thread from grid UI
//                int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
//                        activity.getResources()
//                                .getDisplayMetrics());
//                ((RecyclerView.LayoutParams) viewHolderLink.itemView.findViewById(R.id.layout_link).getLayoutParams()).setMarginStart(
//                        margin);
//                ((RecyclerView.LayoutParams) viewHolderLink.itemView.findViewById(R.id.layout_link).getLayoutParams()).setMarginEnd(
//                        margin);
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
                    public void onBind(Link link,
                            boolean showSubreddit,
                            String userName) {
                        super.onBind(link, showSubreddit, userName);
                        if (link.isSelf() && !TextUtils.isEmpty(link.getSelfText())) {
                            textThreadSelf.setVisibility(View.VISIBLE);
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
                .inflate(R.layout.row_comment, parent, false), eventListenerBase, eventListenerComment, disallowListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (!controllerComments.isLoading() && position > controllerComments.sizeLinks() - 10) {
            controllerComments.loadMoreComments();
        }

        if (getItemViewType(position) == VIEW_LINK) {
            AdapterLink.ViewHolderBase viewHolderBase = (AdapterLink.ViewHolderBase) holder;

            viewHolderBase
                    .onBind(controllerComments.getMainLink(), controllerComments.showSubreddit(),
                            controllerUser.getUser().getName());
            if (!isInitialized) {
                if (controllerComments.getMainLink().isSelf() && !TextUtils.isEmpty(controllerComments.getMainLink().getSelfText())) {
                    viewHolderBase.loadSelfText();
                }
                isInitialized = true;
            }
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

    public void destroyViewHolderLink() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyWebViews();
        }
    }

    public void setAnimationFinished(boolean isAnimationFinished) {
        this.animationFinished = isAnimationFinished;
    }

    public static class ViewHolderComment extends RecyclerView.ViewHolder
            implements Toolbar.OnMenuItemClickListener {

        protected Comment comment;

        protected View viewIndent;
        protected View viewIndicator;
        protected View viewIndicatorContainer;
        protected View viewIndicatorContainerReply;
        protected TextView textComment;
        protected TextView textInfo;
        protected TextView textHidden;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        protected MenuItem itemViewLink;
        protected MenuItem itemCollapse;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemReply;
        protected MenuItem itemSave;
        protected MenuItem itemEdit;
        protected MenuItem itemDelete;
        protected PorterDuffColorFilter colorFilterSave;
        protected Drawable drawableUpvote;
        protected Drawable drawableDownvote;
        protected RelativeLayout layoutContainerExpand;

        protected AdapterLink.ViewHolderBase.EventListener eventListenerBase;
        protected EventListener eventListener;
        protected DisallowListener disallowListener;
        protected String userName;
        protected int indentWidth;
        protected int toolbarItemWidth;

        public ViewHolderComment(final View itemView,
                AdapterLink.ViewHolderBase.EventListener eventListenerBase,
                EventListener eventListener,
                DisallowListener disallowListener,
                final ControllerProfile.Listener listener) {
            this(itemView, eventListenerBase, eventListener, disallowListener);
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
                DisallowListener disallowListener) {
            super(itemView);

            this.eventListenerBase = eventListenerBase;
            this.eventListener = eventListener;
            this.disallowListener = disallowListener;

            intiialize();
            initializeToolbar();
            initializeListeners();
        }

        private void intiialize() {


            Resources resources = itemView.getContext().getResources();
            this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
            this.drawableDownvote = resources.getDrawable(
                    R.drawable.ic_keyboard_arrow_down_white_24dp);

            indentWidth = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, resources.getDisplayMetrics());
            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    itemView.getContext().getResources().getDisplayMetrics());

            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            viewIndicatorContainer = itemView.findViewById(R.id.view_indicator_container);
            viewIndicatorContainerReply = itemView.findViewById(
                    R.id.view_indicator_container_reply);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
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
                    }
                }
            });

            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expandToolbarActions();
                }
            };
            textComment.setOnClickListener(clickListenerLink);
            textInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);
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
            itemCollapse = menu.findItem(R.id.item_collapse);
            itemUpvote = menu.findItem(R.id.item_upvote);
            itemDownvote = menu.findItem(R.id.item_downvote);
            itemReply = menu.findItem(R.id.item_reply);
            itemSave = menu.findItem(R.id.item_save);
            itemEdit = menu.findItem(R.id.item_edit);
            itemDelete = menu.findItem(R.id.item_delete);

            colorFilterSave = new PorterDuffColorFilter(
                    itemView.getContext().getResources()
                            .getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);

        }

        public void expandToolbarActions() {

            if (comment.isMore()) {
                eventListener.loadNestedComments(comment);
                return;
            }

            if (!toolbarActions.isShown()) {

                boolean isAuthor = comment.getAuthor()
                        .equals(userName);

                if (eventListener.isCommentExpanded(getAdapterPosition())) {
                    itemCollapse.setIcon(R.drawable.ic_arrow_drop_up_white_24dp);
                }
                else {
                    itemCollapse.setIcon(R.drawable.ic_arrow_drop_down_white_24dp);
                }

                itemEdit.setEnabled(isAuthor);
                itemEdit.setVisible(isAuthor);
                itemDelete.setEnabled(isAuthor);
                itemDelete.setVisible(isAuthor);

                setVoteColors();

                setToolbarMenuVisibility();
            }

            AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
        }

        private void setToolbarMenuVisibility() {
            // TODO: Move instances to shared class to prevent code duplication

            Menu menu = toolbarActions.getMenu();

            int maxNum = itemView.getWidth() / toolbarItemWidth;
            int numShown = 0;

            boolean loggedIn = !TextUtils.isEmpty(userName);

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
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
        }

        public void setVoteColors() {
            switch (comment.isLikes()) {
                case 1:
                    drawableUpvote.setColorFilter(itemView.getContext().getResources()
                            .getColor(
                                    R.color.positiveScore), PorterDuff.Mode.MULTIPLY);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case -1:
                    drawableDownvote.setColorFilter(itemView.getContext().getResources()
                            .getColor(
                                    R.color.negativeScore), PorterDuff.Mode.MULTIPLY);
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

        public void onBind(Comment comment, String userName) {

            this.comment = comment;
            this.userName = userName;

            layoutContainerReply
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
            layoutContainerExpand
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);

            int alphaLevel = comment.getLevel() * MAX_ALPHA / ALPHA_LEVELS;

            int overlayColor = ColorUtils.setAlphaComponent(0xFF000000,
                    alphaLevel <= MAX_ALPHA ? alphaLevel : MAX_ALPHA);
            int indicatorColor = ColorUtils.compositeColors(overlayColor,
                    itemView.getContext().getResources()
                            .getColor(
                                    R.color.colorPrimary));

            viewIndicator.setBackgroundColor(indicatorColor);
            viewIndicatorContainer.setBackgroundColor(indicatorColor);
            viewIndicatorContainerReply.setBackgroundColor(indicatorColor);

            ViewGroup.LayoutParams layoutParams = viewIndent.getLayoutParams();
            layoutParams.width =
                    comment.getLevel() > 10 ? indentWidth * 10 : indentWidth * comment.getLevel();
            viewIndent.setLayoutParams(layoutParams);

            if (comment.isMore()) {
                textComment.setText(R.string.load_more_comments);
                textInfo.setText("");
            }
            else {
                textComment.setText(Reddit.getTrimmedHtml(comment.getBodyHtml()));

                Resources resources = itemView.getContext().getResources();

                int colorPositive = resources.getColor(R.color.positiveScore);
                int colorNegative = resources.getColor(R.color.negativeScore);

                Spannable spannableScore = new SpannableString(String.valueOf(comment.getScore()));
                spannableScore.setSpan(new ForegroundColorSpan(
                                comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                        spannableScore.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);


                Spannable spannableAuthor = new SpannableString(comment.getAuthor());
                if (comment.getLinkAuthor().equals(comment.getAuthor())) {
                    spannableAuthor.setSpan(
                            new ForegroundColorSpan(resources.getColor(R.color.colorAccent)), 0,
                            spannableAuthor.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }

                textInfo.setText(TextUtils.concat(spannableScore, " by ", spannableAuthor, " ",
                        DateUtils.getRelativeTimeSpanString(
                                comment.getCreatedUtc()) + (comment.getEdited() > 0 ? "*" : "")));

                textInfo.setTextColor(comment.isNew() ? itemView.getContext().getResources()
                        .getColor(R.color.darkThemeTextColorAlert) : itemView.getContext().getResources()
                        .getColor(R.color.darkThemeTextColorMuted));

                if (comment.getEdited() > 1) {
                    textHidden.setText(
                            "Edited " + DateUtils.getRelativeTimeSpanString(comment.getEdited()));
                    textHidden.setVisibility(View.VISIBLE);
                }
                else {
                    textHidden.setVisibility(View.GONE);
                }

            }

            if (comment.getGilded() > 0) {
                textComment.setTextColor(itemView.getContext().getResources()
                        .getColor(R.color.gildedComment));
            }
            else {
                textComment.setTextColor(itemView.getContext().getResources()
                        .getColor(R.color.darkThemeTextColor));
            }


        }

        public void syncSaveIcon() {
            if (comment.isSaved()) {
                itemSave.getIcon().setColorFilter(colorFilterSave);
            }
            else {
                itemSave.getIcon().clearColorFilter();
            }
        }

        public void toggleReply() {
            // TODO: Store reply text inside Comment
            if (!comment.isReplyExpanded()) {
                editTextReply.requestFocus();
                editTextReply.setText(comment.getReplyText());
                buttonSendReply.setText(comment.isEditMode() ? itemView.getContext().getString(R.string.send_edit) : itemView.getContext().getString(R.string.send_reply));
            }
            comment.setReplyExpanded(!comment.isReplyExpanded());
            layoutContainerReply.setVisibility(
                    comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_collapse:
                    item.setIcon(eventListener.toggleComment(getAdapterPosition()) ?
                            R.drawable.ic_arrow_drop_up_white_24dp :
                            R.drawable.ic_arrow_drop_down_white_24dp);
                    break;
                case R.id.item_upvote:
                    eventListener.voteComment(ViewHolderComment.this, comment, 1);
                    break;
                case R.id.item_downvote:
                    eventListener.voteComment(ViewHolderComment.this, comment, -1);
                    break;
                case R.id.item_reply:
                    comment.setEditMode(false);
                    comment.setReplyText("");
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
                            itemView.getContext().getResources().getString(R.string.comment),
                            comment.getBody());
                    clipboard.setPrimaryClip(clip);
                    eventListenerBase.toast(itemView.getContext().getResources().getString(
                            R.string.copied));
                    break;
                case R.id.item_edit:
                    comment.setEditMode(true);
                    comment.setReplyText(comment.getBody());
                    toggleReply();
                    break;
                case R.id.item_delete:
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete comment?")
                            .setMessage(Reddit.getTrimmedHtml(comment.getBodyHtml()))
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
            }
            return true;
        }

        private void saveComment(final Comment comment) {
            eventListenerBase.save(comment);
            comment.setSaved(!comment.isSaved());
            syncSaveIcon();
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
        }

    }
}
