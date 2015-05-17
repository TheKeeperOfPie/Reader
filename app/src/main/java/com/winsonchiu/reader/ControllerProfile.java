package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;
import com.winsonchiu.reader.data.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
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

    private Set<ItemClickListener> listeners;
    private Listing data;
    private Reddit reddit;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Link link;
    private Link topLink;
    private Comment topComment;
    private User user;
    private String page;

    public ControllerProfile(Activity activity) {
        data = new Listing();
        listeners = new HashSet<>();
        this.reddit = Reddit.getInstance(activity);
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        link = new Link();
        topLink = new Link();
        topComment = new Comment();
        user = new User();
        page = "Overview";
    }

    public void addListener(ItemClickListener listener) {
        listeners.add(listener);
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

    public int getItemCount() {
        return data.getChildren().size();
    }

    @Override
    public Link getLink(int position) {
        if (position == 2) {
            return topLink;
        }
        return (Link) data.getChildren().get(position - 6);
    }

    @Override
    public Comment getComment(int position) {
        if (position == 4) {
            return topComment;
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
        for (ControllerProfile.ItemClickListener listener : listeners) {
            listener.setToolbarTitle("/u/" + user.getName());
        }
        reload();
    }

    public void reload() {

        reddit.loadGet(Reddit.OAUTH_URL + "/user/" + user.getName() + "/" + page.toLowerCase(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            setData(Listing.fromJson(new JSONObject(response)));
                            for (ControllerProfile.ItemClickListener listener : listeners) {
                                listener.setRefreshing(
                                        false);
                                listener.resetRecycler();
                            }

                            topLink = null;
                            topComment = null;
                            if (!TextUtils.isEmpty(user.getName()) && page.equalsIgnoreCase("Overview")) {
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
                                    listener.getAdapter()
                                            .notifyItemChanged(2);
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
                                                    topComment = null;
                                                    topComment = (Comment) listingComment.getChildren()
                                                            .get(0);
                                                    for (ControllerProfile.ItemClickListener listener : listeners) {
                                                        listener.setRefreshing(
                                                                false);
                                                        listener.getAdapter()
                                                                .notifyItemChanged(
                                                                        4);
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
    public void voteLink(RecyclerView.ViewHolder viewHolder, int vote) {

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
    public void insertComments(Comment moreComment, Listing listing) {
        // Not implemented
    }

    @Override
    public void insertComment(int commentIndex, Comment comment) {
        // Not implemented
    }

    @Override
    public void removeComment(int commentIndex) {
        // Not implemented
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
    public boolean voteComment(AdapterCommentList.ViewHolderComment viewHolder,
                               int vote) {
        // Not implemented
        return false;
    }

    @Override
    public int getIndentWidth(Comment comment) {
        // Not implemented
        return 0;
    }

    @Override
    public void loadMoreComments(Comment moreComment) {
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

    public void setData(Listing data) {
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public void loadUser(String query) {

        for (ControllerProfile.ItemClickListener listener : listeners) {
            listener.setRefreshing(true);
        }

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
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                            }
                        }, 0);
    }

    public interface ItemClickListener extends DisallowListener {

        void onClickComments(Link link, RecyclerView.ViewHolder viewHolder);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        void setToolbarTitle(String title);
        AdapterProfile getAdapter();
        int getRecyclerHeight();
        void resetRecycler();
    }

    public interface ListenerCallback {
        ItemClickListener getListener();
        ControllerProfile getController();
        int getColorPositive();
        int getColorNegative();
        Activity getActivity();
        float getItemWidth();
    }

}