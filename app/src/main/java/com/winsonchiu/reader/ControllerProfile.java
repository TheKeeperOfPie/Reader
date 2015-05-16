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
    public static final int VIEW_TYPE_LINK = 1;
    public static final int VIEW_TYPE_COMMENT = 2;

    private static final String TAG = ControllerProfile.class.getCanonicalName();

    private Set<ItemClickListener> listeners;
    private Listing data;
    private Reddit reddit;
    private String username;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Link link;

    public ControllerProfile(Activity activity) {
        data = new Listing();
        listeners = new HashSet<>();
        this.reddit = Reddit.getInstance(activity);
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
        link = new Link();
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
        return (Link) data.getChildren().get(position - 1);
    }

    @Override
    public Comment get(int position) {
        return (Comment) data.getChildren().get(position - 1);
    }

    @Override
    public Reddit getReddit() {
        return reddit;
    }

    public void setUser(String username) {
        this.username = username;
        link.setAuthor(username);
        reload();
    }

    public void reload() {

        reddit.loadGet(Reddit.OAUTH_URL + "/user/" + username + "/overview",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);
                        try {
                            setData(Listing.fromJson(new JSONObject(response)));
                            for (ControllerProfile.ItemClickListener listener : listeners) {
                                listener.setRefreshing(false);
                                listener.getAdapter()
                                        .notifyDataSetChanged();
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

    public interface ItemClickListener extends DisallowListener {

        void onClickComments(Link link, RecyclerView.ViewHolder viewHolder);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        void setToolbarTitle(String title);
        AdapterProfile getAdapter();
        int getRecyclerHeight();
    }

    public interface ListenerCallback {
        ItemClickListener getListener();
        ControllerLinksBase getController();
        int getColorPositive();
        int getColorNegative();
        Activity getActivity();
        float getItemWidth();
        RecyclerView.LayoutManager getLayoutManager();
    }


}
