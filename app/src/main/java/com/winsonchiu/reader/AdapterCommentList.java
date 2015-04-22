package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */

public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ControllerLinks.ListenerCallback {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;
    private static final int LINK_MENU_SIZE = 4;
    private static final int COMMENT_MENU_SIZE = 6;
    private final ControllerComments.CommentClickListener listener;
    private final float itemWidth;
    private User user;

    private Activity activity;
    private ControllerComments controllerComments;
    private ControllerLinks.LinkClickListener linkClickListener;
    private int colorPrimary;
    private int colorPositive;
    private int colorNegative;
    private Drawable drawableUpvote;
    private Drawable drawableDownvote;
    private SharedPreferences preferences;

    public AdapterCommentList(Activity activity, ControllerComments controllerComments, final ControllerComments.CommentClickListener listener) {
        // TODO: Add setActivity
        this.activity = activity;
        this.controllerComments = controllerComments;
        this.listener = listener;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.colorPrimary = resources.getColor(R.color.colorPrimary);
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
        this.drawableDownvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_down_white_24dp);
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
        this.linkClickListener = new ControllerLinks.LinkClickListener() {
            @Override
            public void onClickComments(Link link) {
                // Not required
            }

            @Override
            public void loadUrl(String url) {
                listener.loadUrl(url);
            }

            @Override
            public void onFullLoaded(int position) {
                // Not required
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                // Not required
            }

            @Override
            public void setToolbarTitle(String title) {
                // Not required
            }

            @Override
            public AdapterLink getAdapter() {
                return null;
            }

            @Override
            public int getRecyclerHeight() {
                return listener.getRecyclerHeight();
            }

            @Override
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                listener.requestDisallowInterceptTouchEvent(disallow);
            }
        };
        this.user = new User();

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
            return new AdapterLinkList.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_link, parent, false), this);
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof AdapterLinkList.ViewHolder) {
            ((AdapterLinkList.ViewHolder) holder).onBind(position);
        }
        else if (holder instanceof AdapterLinkGrid.ViewHolder) {
            ((AdapterLinkGrid.ViewHolder) holder).onBind(position);
        }
        else {

            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;

            Comment comment = controllerComments.get(position - 1);

            if (comment.isReplyExpanded()) {
                viewHolderComment.editTextReply.setVisibility(View.VISIBLE);
                viewHolderComment.buttonSendReply.setVisibility(View.VISIBLE);
                viewHolderComment.layoutContainerActions.setVisibility(View.VISIBLE);
                viewHolderComment.toolbarActions.setVisibility(View.VISIBLE);
            }
            else {
                viewHolderComment.editTextReply.setVisibility(View.GONE);
                viewHolderComment.buttonSendReply.setVisibility(View.GONE);
                viewHolderComment.toolbarActions.setVisibility(View.GONE);
            };

            ViewGroup.LayoutParams layoutParams = viewHolderComment.viewIndent.getLayoutParams();
            layoutParams.width = controllerComments.getIndentWidth(comment);
            viewHolderComment.viewIndent.setLayoutParams(layoutParams);

            if (comment.isMore()) {
                viewHolderComment.textComment.setText(R.string.load_more_comments);
                viewHolderComment.textInfo.setText("");
            }
            else {
                String html = comment.getBodyHtml();
                html = Html.fromHtml(html.trim())
                        .toString();

                setTextViewHTML(viewHolderComment.textComment, html);

                Spannable spannableInfo = new SpannableString(
                        comment.getScore() + " by " + comment.getAuthor());
                spannableInfo.setSpan(new ForegroundColorSpan(
                                comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                        String.valueOf(comment.getScore())
                                .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                if (controllerComments.getLink(0).getAuthor().equals(comment.getAuthor())) {
                    spannableInfo.setSpan(new ForegroundColorSpan(colorPrimary), spannableInfo.length() - comment.getAuthor().length(), spannableInfo.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                viewHolderComment.textInfo.setText(spannableInfo);
            }
        }

    }

    @Override
    public int getItemCount() {
        return controllerComments.getItemCount();
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

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return linkClickListener;
    }

    @Override
    public Controller getController() {
        return controllerComments;
    }

    @Override
    public int getColorPositive() {
        return colorPositive;
    }

    @Override
    public int getColorNegative() {
        return colorNegative;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public float getItemWidth() {
        return itemWidth;
}

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return null;
    }

    protected class ViewHolderComment extends RecyclerView.ViewHolder {

        protected View viewIndent;
        protected View viewIndicator;
        protected TextView textComment;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        protected MenuItem itemCollapse;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemShare;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolderComment(final View itemView) {
            super(itemView);

            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textComment.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(preferences.getString(AppSettings.REFRESH_TOKEN, ""))) {
                        Toast.makeText(activity, "Must be logged in to reply", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        Comment comment = controllerComments.get(getAdapterPosition() - 1);
                        final int commentIndex = getAdapterPosition() - 1;
                        final int parentLevel = comment.getLevel();
                        Map<String, String> params = new HashMap<>();
                        params.put("api_type", "json");
                        params.put("text", editTextReply.getText()
                                .toString());
                        params.put("thing_id", comment.getName());

                        // TODO: Move add to immediate on button click, check if failed afterwards
                        controllerComments.getReddit()
                                .loadPost(Reddit.OAUTH_URL + "/api/comment",
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject(response);
                                                    Comment comment = Comment.fromJson(
                                                            jsonObject.getJSONObject("json")
                                                                    .getJSONObject("data")
                                                                    .getJSONArray("things")
                                                                    .getJSONObject(0), parentLevel + 1);
                                                    controllerComments.insertComment(commentIndex, comment);
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
                        comment.setReplyExpanded(!comment.isReplyExpanded());
                        AnimationUtils.animateExpand(editTextReply);
                        AnimationUtils.animateExpand(buttonSendReply);
                    }
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_comment);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    Comment comment;
                    final int commentIndex = getAdapterPosition() - 1;
                    switch (menuItem.getItemId()) {
                        case R.id.item_collapse:
                            controllerComments.toggleComment(commentIndex);
                            break;
                        case R.id.item_upvote:
                            controllerComments.voteComment(ViewHolderComment.this, 1);
                            break;
                        case R.id.item_downvote:
                            controllerComments.voteComment(ViewHolderComment.this, -1);
                            break;
                        case R.id.item_reply:
                            comment = controllerComments.get(commentIndex);
                            if (!comment.isReplyExpanded()) {
                                editTextReply.requestFocus();
                                editTextReply.setText(null);
                            }
                            comment.setReplyExpanded(!comment.isReplyExpanded());
                            AnimationUtils.animateExpand(editTextReply);
                            AnimationUtils.animateExpand(buttonSendReply);
                            break;
                        case R.id.item_share:
                            break;
                        case R.id.item_delete:
                            comment = controllerComments.get(commentIndex);

                            Map<String, String> params = new HashMap<>();
                            params.put("id", comment.getName());

                            controllerComments.getReddit()
                                    .loadPost(Reddit.OAUTH_URL + "/api/del",
                                            new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    controllerComments.removeComment(commentIndex);
                                                }
                                            }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {

                                                }
                                            }, params, 0);
                            break;
                    }
                    return true;
                }
            });
            toolbarActions.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    setToolbarMenuVisibility();
                    toolbarActions.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
            itemUpvote = toolbarActions.getMenu().findItem(R.id.item_upvote);
            itemDownvote = toolbarActions.getMenu().findItem(R.id.item_downvote);

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Comment comment = controllerComments.get(getAdapterPosition() - 1);
                    if (comment.isMore()) {
                        Log.d(TAG, "loadMoreComments");
                        controllerComments.loadMoreComments(comment);
                        return;
                    }

                    boolean isAuthor = comment.getAuthor().equals(user.getName());

                    toolbarActions.getMenu().findItem(R.id.item_delete).setEnabled(isAuthor);
                    toolbarActions.getMenu().findItem(R.id.item_delete).setVisible(isAuthor);

                    setVoteColors();
                    AnimationUtils.animateExpandActions(layoutContainerActions, true);
                    AnimationUtils.animateExpandActions(toolbarActions, false);
                    viewIndicator.invalidate();
                }
            };
            textComment.setOnClickListener(clickListenerLink);
            textInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

        }

        private void setToolbarMenuVisibility() {
            int maxNum = (int) (itemView.getWidth() / itemWidth);

            for (int index = 0; index < COMMENT_MENU_SIZE; index++) {
                if (index < maxNum) {
                    toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }

        public void setVoteColors() {

            Comment comment = (Comment) controllerComments.getListingComments().getChildren().get(getAdapterPosition() - 1);
            switch (comment.isLikes()) {
                case 1:
                    drawableUpvote.setColorFilter(colorPositive, PorterDuff.Mode.MULTIPLY);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case -1:
                    drawableDownvote.setColorFilter(colorNegative, PorterDuff.Mode.MULTIPLY);
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

    }
}
