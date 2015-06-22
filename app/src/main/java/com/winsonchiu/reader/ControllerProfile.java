package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.Thing;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by TheKeeperOfPie on 5/16/2015.
 */
public class ControllerProfile implements ControllerLinksBase, ControllerCommentsBase {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_HEADER_TEXT = 1;
    public static final int VIEW_TYPE_LINK = 2;
    public static final int VIEW_TYPE_COMMENT = 3;

    private static final String TAG = ControllerProfile.class.getCanonicalName();

    private Activity activity;
    private Set<ItemClickListener> listeners;
    private Listing data;
    private Reddit reddit;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Link link;
    private Link topLink;
    private Comment topComment;
    private User currentUser;
    private User user;
    private String page;
    private Sort sort;
    private Time time;
    private SharedPreferences preferences;
    private boolean isLoading;
    private int indentWidth;

    public ControllerProfile(Activity activity) {
        setActivity(activity);
        data = new Listing();
        listeners = new HashSet<>();
        link = new Link();
        topLink = new Link();
        topComment = new Comment();
        currentUser = new User();
        user = new User();
        page = "Overview";
        sort = Sort.HOT;
        time = Time.ALL;
        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.currentUser = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                activity.getResources()
                        .getDisplayMetrics());
    }

    public void addListener(ItemClickListener listener) {
        listeners.add(listener);
        listener.setRefreshing(isLoading);
        listener.getAdapter().notifyDataSetChanged();
        listener.setIsUser(user.getName().equals(currentUser.getName()));
    }

    public void removeListener(ItemClickListener listener) {
        listeners.remove(listener);
    }

    public int getViewType(int position) {

        List<Thing> children = data.getChildren();

        if (position < 0 || position >= children.size()) {
            throw new IndexOutOfBoundsException("ControllerProfile position invalid");
        }

        Thing thing = children.get(position);

        if (thing instanceof Link) {
            return VIEW_TYPE_LINK;
        }
        else if (thing instanceof Comment) {
            return VIEW_TYPE_COMMENT;
        }

        throw new IllegalStateException(thing + " is not a valid view type");

    }

    @Override
    public Link getLink(int position) {
        if (position == 2) {
            return getTopLink();
        }

        return (Link) data.getChildren().get(position - 6);
    }

    public Link getTopLink() {
        return page.equalsIgnoreCase("overview") ? topLink : null;
    }

    @Override
    public Comment getComment(int position) {
        if (position == 4) {
            return getTopComment();
        }

        return (Comment) data.getChildren().get(position - 6);
    }

    @Override
    public Reddit getReddit() {
        return reddit;
    }

    public void setPage(String page) {
        this.page = page;
        reload();
    }

    public String getPage() {
        return page;
    }

    public void setUser(User user) {
        this.user = user;
        sort = Sort.HOT;
        page = "Overview";
        for (ItemClickListener listener : listeners) {
            listener.setIsUser(user.getName()
                    .equals(currentUser.getName()));
        }
        reload();
        Log.d(TAG, "setUser: " + (user.getName()
                .equals(currentUser.getName())));
    }

    public void reload() {

        setLoading(true);

        String url = Reddit.OAUTH_URL + "/user/" + user.getName() + "/" + page.toLowerCase() + "?sort=" + sort.toString() + "&t=" + time.toString();

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            setData(Listing.fromJson(new JSONObject(response)));
                            for (ControllerProfile.ItemClickListener listener : listeners) {
                                listener.setPage(page);
                                listener.resetRecycler();
                            }
                            setLoading(false);
                            if (!TextUtils.isEmpty(user.getName()) && page.equalsIgnoreCase(
                                    "Overview")) {
                                loadTopEntries();
                            }
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLoading(false);
                        Toast.makeText(activity, activity.getString(R.string.error_loading), Toast.LENGTH_SHORT)
                                .show();
                    }
                }, 0);
    }

    public void loadMore() {

        if (isLoading()) {
            return;
        }

        setLoading(true);

        String url = Reddit.OAUTH_URL + "/user/" + user.getName() + "/" + page.toLowerCase() + "?sort=" + sort.toString() + "&t=" + time.toString() + "&after=" + data.getAfter();

        reddit.loadGet(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            int startSize = data.getChildren().size();
                            int positionStart = startSize + 5;

                            Listing listing = Listing.fromJson(new JSONObject(response));
//                            for (Thing thing : listing.getChildren()) {
//                                Comment comment = (Comment) thing;
//                                comment.setLevel(0);
//                            }

                            data.addChildren(listing.getChildren());
                            data.setAfter(listing.getAfter());

                            for (ItemClickListener listener : listeners) {

                                listener.getAdapter()
                                        .notifyItemRangeInserted(positionStart,
                                                data.getChildren().size() - positionStart);
                                listener.setPage(page);
                            }
                            setLoading(false);
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setData(new Listing());
                        for (ControllerProfile.ItemClickListener listener : listeners) {
                            listener.setRefreshing(
                                    false);
                            listener.resetRecycler();
                        }
                    }
                }, 0);
    }

    private void loadTopEntries() {
        reddit.loadGet(
                Reddit.OAUTH_URL + "/user/" + user.getName() + "/submitted?sort=top&limit=1&",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Submitted onResponse: " + response);
                        try {
                            Listing listingLink = Listing.fromJson(
                                    new JSONObject(response));
                            if (!listingLink.getChildren().isEmpty()) {
                                topLink = (Link) listingLink.getChildren()
                                        .get(0);
                                for (ControllerProfile.ItemClickListener listener : listeners) {
                                    listener.setRefreshing(false);
                                    listener.getAdapter().notifyItemRangeChanged(1, 2);
                                }
                            }

                            reddit.loadGet(
                                    Reddit.OAUTH_URL + "/user/" + user.getName() + "/comments?sort=top&limit=1&",
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            Log.d(TAG,
                                                    "onResponse: " + response);
                                            try {
                                                Listing listingComment = Listing.fromJson(
                                                        new JSONObject(
                                                                response));
                                                if (!listingComment.getChildren().isEmpty()) {
                                                    topComment = (Comment) listingComment.getChildren()
                                                            .get(0);
                                                    topComment.setLevel(0);
                                                    for (ControllerProfile.ItemClickListener listener : listeners) {
                                                        listener.setRefreshing(
                                                                false);
                                                        listener.getAdapter().notifyItemRangeChanged(
                                                                3, 2);
                                                    }
                                                }
                                            }
                                            catch (JSONException e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {

                                        }
                                    }, 0);
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    @Override
    public void voteLink(final RecyclerView.ViewHolder viewHolder, final Link link, int vote) {
        reddit.voteLink(viewHolder, link, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public Drawable getDrawableForLink(Link link) {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return drawableSelf;
        }

        if (TextUtils.isEmpty(thumbnail) || thumbnail.equals(Reddit.DEFAULT) || thumbnail.equals(Reddit.NSFW)) {
            return drawableDefault;
        }

        return null;
    }

    @Override
    public int sizeLinks() {
        return data.getChildren().size();
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        for (ItemClickListener listener : listeners) {
            listener.setRefreshing(loading);
        }
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public void loadMoreLinks() {
        // Not implemented
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public Subreddit getSubreddit() {
        return new Subreddit();
    }

    @Override
    public boolean showSubreddit() {
        return true;
    }

    @Override
    public void deletePost(Link link) {
        // TODO: Implement deleting posts from Profile view
    }

    @Override
    public void insertComments(Comment moreComment, Listing listing) {
        // Not implemented
    }

    @Override
    public void insertComment(Comment comment) {

        Comment parentComment = new Comment();
        parentComment.setId(comment.getParentId());
        int commentIndex = data.getChildren()
                    .indexOf(parentComment);

        if (commentIndex > -1) {
            comment.setLevel(parentComment.getLevel() + 1);
            data.getChildren()
                    .add(commentIndex + 1, comment);

            for (ItemClickListener listener : listeners) {
//                listener.getAdapter().notifyDataSetChanged();
                listener.getAdapter()
                        .notifyItemInserted(commentIndex + 7);
            }
        }

    }

    @Override
    public void deleteComment(Comment comment) {
        int commentIndex = data.getChildren().indexOf(comment);
        data.getChildren().remove(commentIndex);

        for (ControllerProfile.ItemClickListener listener : listeners) {
            listener.getAdapter().notifyItemRemoved(commentIndex + 6);

        }

        Map<String, String> params = new HashMap<>();
        params.put("id", comment.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/del",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(
                            String response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(
                            VolleyError error) {

                    }
                }, params, 0);
    }

    @Override
    public boolean toggleComment(int position) {
        // Not implemented
        return true;
    }

    @Override
    public void expandComment(int position) {
        // Not implemented
    }

    @Override
    public void collapseComment(int position) {
        // Not implemented
    }

    @Override
    public void voteComment(final AdapterCommentList.ViewHolderComment viewHolder,
            final Comment comment,
            int vote) {

        reddit.voteComment(viewHolder, comment, vote, new Reddit.VoteResponseListener() {
            @Override
            public void onVoteFailed() {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public int getIndentWidth(Comment comment) {
        return indentWidth * comment.getLevel();
    }

    @Override
    public void loadNestedComments(Comment moreComment) {
        // Not implemented
    }

    @Override
    public boolean isCommentExpanded(int position) {
        // Not implemented
        return true;
    }

    @Override
    public Link getMainLink() {
        return link;
    }

    @Override
    public void loadMoreComments() {
        // Not implemented
    }

    @Override
    public boolean hasChildren(Comment comment) {
        return false;
    }

    public void setData(Listing data) {
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public void loadUser(String query) {

        setLoading(true);

        reddit.loadGet(Reddit.OAUTH_URL + "/user/" + query + "/about",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Log.d(TAG, "User FragmentProfile onResponse: " + new JSONObject(response).getJSONObject("data").toString());
                                    user = User.fromJson(new JSONObject(response).getJSONObject("data"));
                                }
                                catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                setUser(user);
                                setLoading(false);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                setLoading(false);
                                Toast.makeText(activity, activity.getString(R.string.error_loading), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }, 0);
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        if (this.sort != sort) {
            this.sort = sort;
            reload();
        }
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        if (this.time != time) {
            this.time = time;
            reload();
        }
    }

    public Comment getTopComment() {
        return page.equalsIgnoreCase("overview") ? topComment : null;
    }

    public interface ItemClickListener extends DisallowListener {

        void onClickComments(Link link, RecyclerView.ViewHolder viewHolder);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        AdapterProfile getAdapter();
        int getRecyclerHeight();
        void resetRecycler();
        void setSwipeRefreshEnabled(boolean enabled);
        int getRecyclerWidth();
        ControllerCommentsBase getControllerComments();
        void loadLink(Comment comment);
        void setIsUser(boolean isUser);
        void setPage(String page);
    }

    public interface ListenerCallback {
        ItemClickListener getListener();
        ControllerProfile getController();
        float getItemWidth();
    }

}