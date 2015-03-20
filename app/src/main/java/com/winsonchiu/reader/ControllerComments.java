package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.TypedValue;

import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerComments {

    private SharedPreferences preferences;
    private CommentClickListener listener;
    private Link link;
    private String subreddit;
    private String linkId;
    private int indentWidth;
    private Drawable drawableEmpty;
    private Drawable drawableDefault;
    private Reddit reddit;

    public ControllerComments(Activity activity, CommentClickListener listener, String subreddit, String linkId) {

        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        this.reddit = Reddit.getInstance(activity);
        this.listener = listener;
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, activity.getResources().getDisplayMetrics());
        this.subreddit = subreddit;
        this.linkId = linkId;
        Resources resources = activity.getResources();

        this.drawableEmpty = resources.getDrawable(R.drawable.ic_web_white_24dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_textsms_white_24dp);
        reloadAllComments();
    }

    public void setLink(Link link) {
        this.link = link;
        this.subreddit = link.getSubreddit();
        this.linkId = link.getId();
        listener.notifyDataSetChanged();
    }

    public void reloadAllComments() {
        reddit.loadGet("https://oauth.reddit.com" + "/r/" + subreddit + "/comments/" + linkId,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            setLink(Link.fromJson(new JSONArray(response)));
                            listener.setRefreshing(false);
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    public Drawable getDrawableForLink() {
        String thumbnail = link.getThumbnail();
        if (TextUtils.isEmpty(thumbnail)) {
            return drawableEmpty;
        }
        else if (thumbnail.equals(Reddit.SELF) || thumbnail.equals(
                Reddit.DEFAULT) || thumbnail.equals(Reddit.NSFW)) {
            return drawableDefault;
        }
        return null;
    }

    public Link getLink() {
        return link;
    }

    public List<Comment> getComments() {
        return link == null ? new ArrayList<Comment>() : link.getComments();
    }

    public int getIndentWidth(Comment comment) {
        return indentWidth * comment.getLevel();
    }

    public Reddit getReddit() {
        return reddit;
    }

    public CommentClickListener getListener() {
        return listener;
    }

    public interface CommentClickListener extends DisallowListener {

        void loadUrl(String url);
        void setRefreshing(boolean refreshing);
        void notifyDataSetChanged();
        int getRecyclerHeight();
    }
}
