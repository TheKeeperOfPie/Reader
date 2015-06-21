package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterProfile extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ControllerProfile.ListenerCallback {

    private static final String TAG = AdapterProfile.class.getCanonicalName();

    protected Activity activity;
    protected RecyclerView.LayoutManager layoutManager;
    protected ControllerProfile controllerProfile;
    private SharedPreferences preferences;
    private float itemWidth;
    private int titleMargin;
    private ControllerProfile.ItemClickListener listener;
    private ControllerLinks.ListenerCallback linksCallback;
    private ControllerComments.ListenerCallback commentsCallback;
    private List<AdapterLink.ViewHolderBase> viewHolderLinks;
    private User user;

    public AdapterProfile(final Activity activity,
                          final ControllerProfile controllerProfile,
                          ControllerProfile.ItemClickListener listener) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
        this.titleMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, resources.getDisplayMetrics());
        this.controllerProfile = controllerProfile;
        this.listener = listener;
        viewHolderLinks = new ArrayList<>();
        setCallbacks();
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
                    public void loadSideBar(Subreddit listingSubreddits) {

                    }

                    @Override
                    public void setEmptyView(boolean visible) {
                        // TODO: Implement empty view for profile
                    }

                    @Override
                    public int getRecyclerWidth() {
                        return listener.getRecyclerWidth();
                    }

                    @Override
                    public void onClickSubmit(String postType) {
                        // Not implemented
                    }

                    @Override
                    public ControllerCommentsBase getControllerComments() {
                        return listener.getControllerComments();
                    }

                    @Override
                    public void setSort(Sort sort) {

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
            public float getItemWidth() {
                return itemWidth;
            }

            @Override
            public int getTitleMargin() {
                return titleMargin;
            }

            @Override
            public RecyclerView.LayoutManager getLayoutManager() {
                // Not necessary for Link row
                return null;
            }

            @Override
            public SharedPreferences getPreferences() {
                return preferences;
            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return listener.getControllerComments();
            }

            @Override
            public User getUser() {
                return user;
            }

            @Override
            public void pauseViewHolders() {
                for (AdapterLink.ViewHolderBase viewHolder : viewHolderLinks) {
                    viewHolder.videoFull.stopPlayback();
                }
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
                    public int getRecyclerWidth() {
                        return listener.getRecyclerWidth();
                    }

                    @Override
                    public void setToolbarTitle(CharSequence title) {
                        // Not implemented
                    }

                    @Override
                    public void loadYouTube(Link link,
                            String id,
                            AdapterLink.ViewHolderBase viewHolderBase) {
                        // Not implemented
                    }

                    @Override
                    public boolean hideYouTube() {
                        // Not implemented
                        return false;
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
                AdapterLink.ViewHolderBase viewHolder =  new AdapterLinkList.ViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_link, parent, false), linksCallback);
                viewHolderLinks.add(viewHolder);
                viewHolder.toolbarActions.getMenu().findItem(R.id.item_view_profile).setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_NEVER);
                viewHolder.toolbarActions.getMenu().findItem(R.id.item_view_profile).setVisible(false);
                viewHolder.toolbarActions.getMenu().findItem(R.id.item_view_profile).setEnabled(false);
                return viewHolder;
            case ControllerProfile.VIEW_TYPE_COMMENT:
                return new AdapterCommentList.ViewHolderComment(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_comment, parent, false), commentsCallback, listener);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        holder.itemView.setVisibility(View.VISIBLE);

        if (!controllerProfile.isLoading() && position > controllerProfile.sizeLinks() - 5) {
            controllerProfile.loadMore();
        }

        switch (position) {
            case 0:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerProfile.getUser());
                break;
            case 1:
                ViewHolderText viewHolderTextLink = (ViewHolderText) holder;
                viewHolderTextLink.itemView.setVisibility(controllerProfile.getTopLink() == null ? View.GONE : View.VISIBLE);
                viewHolderTextLink.onBind("Top Post");
                break;
            case 2:
                AdapterLinkList.ViewHolder viewHolderLinkTop = (AdapterLinkList.ViewHolder) holder;
                if (controllerProfile.getTopLink() == null) {
                    viewHolderLinkTop.itemView.setVisibility(View.GONE);
                }
                else {
                    viewHolderLinkTop.itemView.setVisibility(View.VISIBLE);
                    viewHolderLinkTop.onBind(controllerProfile.getTopLink());
                }
                break;
            case 3:
                ViewHolderText viewHolderTextComment = (ViewHolderText) holder;
                viewHolderTextComment.itemView.setVisibility(controllerProfile.getTopComment() == null ? View.GONE : View.VISIBLE);
                viewHolderTextComment.onBind("Top Comment");
                break;
            case 4:
                AdapterCommentList.ViewHolderComment viewHolderCommentTop = (AdapterCommentList.ViewHolderComment) holder;
                if (controllerProfile.getTopComment() == null) {
                    viewHolderCommentTop.itemView.setVisibility(View.GONE);
                }
                else {
                    viewHolderCommentTop.itemView.setVisibility(View.VISIBLE);
                    viewHolderCommentTop.onBind(controllerProfile.getTopComment());
                }
                break;
            case 5:
                ViewHolderText viewHolderTextOverview = (ViewHolderText) holder;
                viewHolderTextOverview.onBind(controllerProfile.getPage());
                break;
            default:
                if (holder instanceof AdapterLinkList.ViewHolder) {

                    AdapterLinkList.ViewHolder viewHolderLink = (AdapterLinkList.ViewHolder) holder;
                    viewHolderLink.onBind(controllerProfile.getLink(position));

                }
                else if (holder instanceof AdapterCommentList.ViewHolderComment) {

                    AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                    viewHolderComment.onBind(controllerProfile.getComment(position));
                }
        }

    }

    @Override
    public int getItemCount() {
        return controllerProfile.sizeLinks() > 0 ? controllerProfile.sizeLinks() + 4 : 0;
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
                            user.getLinkKarma() > 0 ? callback.getController().getActivity().getResources().getColor(
                                    R.color.positiveScore) :
                                    callback.getController().getActivity().getResources().getColor(
                                            R.color.negativeScore)), 0, linkLength,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(
                            user.getCommentKarma() > 0 ? callback.getController().getActivity().getResources().getColor(
                                    R.color.positiveScore) :
                                    callback.getController().getActivity().getResources().getColor(R.color.negativeScore)), linkLength + 12,
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
