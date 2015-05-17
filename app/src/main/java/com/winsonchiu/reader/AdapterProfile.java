package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.User;

import java.util.Date;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterProfile extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ControllerProfile.ListenerCallback {

    private static final String TAG = AdapterProfile.class.getCanonicalName();


    protected Activity activity;
    protected RecyclerView.LayoutManager layoutManager;
    protected ControllerProfile controllerProfile;
    private int colorMuted;
    private int colorAccent;
    private int colorPrimary;
    private int colorPositive;
    private int colorNegative;
    private int colorDefault;
    private Drawable drawableUpvote;
    private Drawable drawableDownvote;
    private SharedPreferences preferences;
    private float itemWidth;
    private ControllerProfile.ItemClickListener listener;
    private ControllerLinks.ListenerCallback linksCallback;
    private ControllerComments.ListenerCallback commentsCallback;

    public AdapterProfile(final Activity activity,
                          final ControllerProfile controllerProfile,
                          ControllerProfile.ItemClickListener listener) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.colorMuted = resources.getColor(R.color.darkThemeTextColorMuted);
        this.colorAccent = resources.getColor(R.color.colorAccent);
        this.colorPrimary = resources.getColor(R.color.colorPrimary);
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        this.colorDefault = resources.getColor(R.color.darkThemeDialog);
        this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
        this.drawableDownvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_down_white_24dp);
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
        this.controllerProfile = controllerProfile;
        this.listener = listener;
        setCallbacks();
    }

    private void setCallbacks() {

        this.linksCallback = new ControllerLinks.ListenerCallback() {
            @Override
            public ControllerLinks.LinkClickListener getListener() {
                // Not implemented
                return new ControllerLinks.LinkClickListener() {
                    @Override
                    public void onClickComments(Link link,
                                                RecyclerView.ViewHolder viewHolder) {
                        listener.onClickComments(link, viewHolder);
                    }

                    @Override
                    public void loadUrl(String url) {
                        listener.loadUrl(url);
                    }

                    @Override
                    public void onFullLoaded(int position) {

                    }

                    @Override
                    public void setRefreshing(boolean refreshing) {
                        listener.setRefreshing(refreshing);
                    }

                    @Override
                    public void setToolbarTitle(String title) {
                        listener.setToolbarTitle(title);
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

                    }
                };
            }

            @Override
            public ControllerLinksBase getController() {
                return controllerProfile;
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
            public int getColorMuted() {
                return colorMuted;
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
                // Not necessary for Link row
                return null;
            }
        };
        this.commentsCallback = new ControllerComments.ListenerCallback() {
            @Override
            public ControllerComments.CommentClickListener getCommentClickListener() {
                return new ControllerComments.CommentClickListener() {
                    @Override
                    public void loadUrl(String url) {
                        listener.loadUrl(url);
                    }

                    @Override
                    public void setRefreshing(boolean refreshing) {

                    }

                    @Override
                    public AdapterCommentList getAdapter() {
                        return null;
                    }

                    @Override
                    public int getRecyclerHeight() {
                        return listener.getRecyclerHeight();
                    }

                    @Override
                    public void requestDisallowInterceptTouchEvent(boolean disallow) {

                    }
                };
            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return controllerProfile;
            }

            @Override
            public SharedPreferences getPreferences() {
                return preferences;
            }

            @Override
            public User getUser() {
                return controllerProfile.getUser();
            }

            @Override
            public int getColorMuted() {
                return colorMuted;
            }

            @Override
            public int getColorAccent() {
                return colorAccent;
            }

            @Override
            public int getColorPrimary() {
                return colorPrimary;
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
            public int getColorDefault() {
                return colorDefault;
            }

            @Override
            public Activity getActivity() {
                return activity;
            }

            @Override
            public float getItemWidth() {
                return itemWidth;
            }
        };
    }

    @Override
    public int getItemViewType(int position) {

        switch (position) {
            case 0:
                return ControllerProfile.VIEW_TYPE_HEADER;
            case 1:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 2:
                return ControllerProfile.VIEW_TYPE_LINK;
            case 3:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            case 4:
                return ControllerProfile.VIEW_TYPE_COMMENT;
            case 5:
                return ControllerProfile.VIEW_TYPE_HEADER_TEXT;
            default:
                return controllerProfile.getViewType(position - 6);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case ControllerProfile.VIEW_TYPE_HEADER:
                return new ViewHolderHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_header, parent, false), this);
            case ControllerProfile.VIEW_TYPE_HEADER_TEXT:
                return new ViewHolderText(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_text, parent, false));
            case ControllerProfile.VIEW_TYPE_LINK:
                return new AdapterLinkList.ViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_link, parent, false), linksCallback);
            case ControllerProfile.VIEW_TYPE_COMMENT:
                return new AdapterCommentList.ViewHolderComment(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_comment, parent, false), commentsCallback);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        holder.itemView.setVisibility(View.VISIBLE);

        switch (position) {
            case 0:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerProfile.getUser());
                break;
            case 1:
                ViewHolderText viewHolderTextLink = (ViewHolderText) holder;
                viewHolderTextLink.onBind("Top Link");
                break;
            case 2:
                AdapterLinkList.ViewHolder viewHolderLinkTop = (AdapterLinkList.ViewHolder) holder;
                if (controllerProfile.getLink(position) == null) {
                    viewHolderLinkTop.itemView.setVisibility(View.GONE);
                }
                else {
                    viewHolderLinkTop.onBind(position);
                }
                break;
            case 3:
                ViewHolderText viewHolderTextComment = (ViewHolderText) holder;
                viewHolderTextComment.onBind("Top Comment");
                break;
            case 4:
                AdapterCommentList.ViewHolderComment viewHolderCommentTop = (AdapterCommentList.ViewHolderComment) holder;
                if (controllerProfile.getComment(position) == null) {
                    viewHolderCommentTop.itemView.setVisibility(View.GONE);
                }
                else {
                    viewHolderCommentTop.onBind(position);
                }
                break;
            case 5:
                ViewHolderText viewHolderTextOverview = (ViewHolderText) holder;
                viewHolderTextOverview.onBind(controllerProfile.getPage());
                break;
            default:
                if (holder instanceof AdapterLinkList.ViewHolder) {

                    AdapterLinkList.ViewHolder viewHolderLink = (AdapterLinkList.ViewHolder) holder;
                    viewHolderLink.onBind(position);

                }
                else if (holder instanceof AdapterCommentList.ViewHolderComment) {

                    AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                    viewHolderComment.onBind(position);
                }
        }

    }

    @Override
    public int getItemCount() {
        return controllerProfile.getItemCount() > 0 ? controllerProfile.getItemCount() + 4 : 0;
    }

    @Override
    public ControllerProfile.ItemClickListener getListener() {
        return listener;
    }

    @Override
    public ControllerProfile getController() {
        return controllerProfile;
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

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected TextView textUsername;
        protected TextView textKarma;
        private ControllerProfile.ListenerCallback callback;

        public ViewHolderHeader(View itemView, ControllerProfile.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            textUsername = (TextView) itemView.findViewById(R.id.text_username);
            textKarma = (TextView) itemView.findViewById(R.id.text_karma);
        }

        public void onBind(User user) {
            textUsername.setText(user.getName());

            int linkLength = String.valueOf(user.getLinkKarma())
                    .length();
            int commentLength = String.valueOf(user.getCommentKarma())
                    .length();

            Spannable spannableInfo = new SpannableString(
                    user.getLinkKarma() + " Link Karma\n" + user.getCommentKarma() + " Comment Karma");
            spannableInfo.setSpan(new ForegroundColorSpan(
                            user.getLinkKarma() > 0 ? callback.getColorPositive() :
                                    callback.getColorNegative()), 0, linkLength,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(
                            user.getCommentKarma() > 0 ? callback.getColorPositive() :
                                    callback.getColorNegative()), linkLength + 12,
                    linkLength + 12 + commentLength, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            textKarma.setText(spannableInfo);
        }
    }

    public static class ViewHolderText extends RecyclerView.ViewHolder {

        protected TextView textMessage;

        public ViewHolderText(View itemView) {
            super(itemView);

            textMessage = (TextView) itemView.findViewById(R.id.text_message);
        }

        public void onBind(String text) {
            textMessage.setText(text);
        }
    }

}
