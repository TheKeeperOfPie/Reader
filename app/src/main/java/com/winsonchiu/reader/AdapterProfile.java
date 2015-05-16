package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterProfile extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterProfile.class.getCanonicalName();


    protected Activity activity;
    protected RecyclerView.LayoutManager layoutManager;
    protected ControllerProfile controllerProfile;
    private User user;
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
    protected ControllerProfile.ItemClickListener listener;

    public AdapterProfile(Activity activity,
                          ControllerProfile controllerProfile,
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
        this.controllerProfile = controllerProfile;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0) {
            return ControllerProfile.VIEW_TYPE_HEADER;
        }

        return controllerProfile.getViewType(position - 1);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case ControllerProfile.VIEW_TYPE_HEADER:
                return new ViewHolderHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_header, parent, false));
            case ControllerProfile.VIEW_TYPE_LINK:
                return new AdapterLinkList.ViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_link, parent, false),
                        new ControllerLinks.ListenerCallback() {
                            @Override
                            public ControllerLinks.LinkClickListener getListener() {
                                // Not implemented
                                return new ControllerLinks.LinkClickListener() {
                                    @Override
                                    public void onClickComments(Link link,
                                                                RecyclerView.ViewHolder viewHolder) {

                                    }

                                    @Override
                                    public void loadUrl(String url) {

                                    }

                                    @Override
                                    public void onFullLoaded(int position) {

                                    }

                                    @Override
                                    public void setRefreshing(boolean refreshing) {

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
                                        return 0;
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
                        });
            case ControllerProfile.VIEW_TYPE_COMMENT:
                return new AdapterCommentList.ViewHolderComment(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_comment, parent, false),
                        new ControllerComments.ListenerCallback() {
                            @Override
                            public ControllerComments.CommentClickListener getCommentClickListener() {
                                return new ControllerComments.CommentClickListener() {
                                    @Override
                                    public void loadUrl(String url) {

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
                                        return 0;
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
                                return user;
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
                        });

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ViewHolderHeader) {

            ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;

        }
        else if (holder instanceof AdapterLinkList.ViewHolder) {

            AdapterLinkList.ViewHolder viewHolderLink = (AdapterLinkList.ViewHolder) holder;
            viewHolderLink.onBind(position);

        }
        else if (holder instanceof AdapterCommentList.ViewHolderComment) {

            AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
            viewHolderComment.onBind(position + 1);

        }

    }

    @Override
    public int getItemCount() {
        return controllerProfile.getItemCount() > 0 ? controllerProfile.getItemCount() + 1 : 0;
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {

        protected TextView textUsername;
        protected TextView textKarma;

        public ViewHolderHeader(View itemView) {
            super(itemView);

            textUsername = (TextView) itemView.findViewById(R.id.text_username);
            textKarma = (TextView) itemView.findViewById(R.id.text_karma);
        }
    }

}
