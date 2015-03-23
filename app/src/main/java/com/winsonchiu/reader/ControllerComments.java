package com.winsonchiu.reader;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/20/2015.
 */
public class ControllerComments extends Controller{

    private static final long ALPHA_DURATION = 500;
    private static final String TAG = ControllerComments.class.getCanonicalName();

    private Activity activity;
    private List<CommentClickListener> listeners;
    private Link link;
    private Listing listingComments;
    private String subreddit;
    private String linkId;
    private int indentWidth;
    private Drawable drawableSelf;
    private Drawable drawableDefault;
    private Reddit reddit;

    public ControllerComments(Activity activity, String subreddit, String linkId) {
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.listeners = new ArrayList<>();
        this.indentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, activity.getResources().getDisplayMetrics());
        this.subreddit = subreddit;
        this.linkId = linkId;
        Resources resources = activity.getResources();
        this.drawableSelf = resources.getDrawable(R.drawable.ic_chat_white_48dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_web_white_48dp);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void addListener(CommentClickListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CommentClickListener listener) {
        listeners.remove(listener);
    }

    public void setLink(Link link) {
        Listing listing = new Listing();
        listing.setChildren(new ArrayList<>(link.getComments().getChildren()));
        this.listingComments = listing;
        this.link = link;
        this.subreddit = link.getSubreddit();
        this.linkId = link.getId();
        for (CommentClickListener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }
    }

    public void reloadAllComments() {
        if (TextUtils.isEmpty(linkId)) {
            return;
        }

        reddit.loadGet("https://oauth.reddit.com" + "/r/" + subreddit + "/comments/" + linkId,
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            setLink(Link.fromJson(new JSONArray(response)));
                            for (CommentClickListener listener : listeners) {
                                listener.setRefreshing(false);
                            }
                        }
                        catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    public Drawable getDrawableForLink() {
        String thumbnail = link.getThumbnail();

        if (link.isSelf()) {
            return drawableSelf;
        }

        if (TextUtils.isEmpty(thumbnail) || thumbnail.equals(Reddit.DEFAULT)) {
            return drawableDefault;
        }

        return null;
    }

    public Link getLink() {
        return link;
    }

    public Listing getListingComments() {
        return link == null ? new Listing() : listingComments;
    }

    public void toggleComment(int position) {

        if (position == listingComments.getChildren().size() - 1) {
            expandComment(position);
            return;
        }

        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        Comment nextComment = (Comment) commentList.get(position + 1);

        if (comment.getLevel() == nextComment.getLevel()) {
            expandComment(position);
        }
        else if (comment.getLevel() < nextComment.getLevel()){
            collapseComment(position);
        }

    }

    private void expandComment(int position) {
        List<Thing> commentList = link.getComments().getChildren();
        int index = commentList.indexOf(listingComments.getChildren().get(position));
        if (index < 0) {
            return;
        }
        List<Comment> commentsToInsert = new LinkedList<>();
        Comment comment = (Comment) commentList.get(index);
        while (++index < commentList.size() && ((Comment) commentList.get(index)).getLevel() != comment.getLevel()) {
            commentsToInsert.add((Comment) commentList.get(index));
        }

        for (int insertIndex = commentsToInsert.size() - 1; insertIndex >= 0; insertIndex--) {
            listingComments.getChildren().add(position + 1, commentsToInsert.get(insertIndex));
        }

        for (CommentClickListener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }
    }

    private void collapseComment(int position) {

        List<Thing> commentList = listingComments.getChildren();
        Comment comment = (Comment) commentList.get(position);
        position++;
        while (position < commentList.size() && ((Comment) commentList.get(position)).getLevel() != comment.getLevel()) {
            commentList.remove(position);
        }
        for (CommentClickListener listener : listeners) {
            listener.getAdapter().notifyDataSetChanged();
        }
    }


    public void voteLink(final RecyclerView.ViewHolder viewHolder, final int vote) {
        final int position = viewHolder.getPosition();

        final int oldVote = link.isLikes();
        int newVote = 0;

        if (link.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, link.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        link.setLikes(newVote);
        if (position == viewHolder.getPosition()) {
            if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo();
            }
            else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
            }
        }
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();

                link.setLikes(oldVote);
                if (position == viewHolder.getPosition()) {
                    if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                        ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                        ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo();
                    }
                    else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                        ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
                    }
                }
            }
        }, params, 0);
    }

    public void voteComment(final AdapterCommentList.ViewHolderComment viewHolder, final int vote) {

        final int position = viewHolder.getPosition();
        final Comment comment = (Comment) listingComments.getChildren().get(viewHolder.getPosition());

        final int oldVote = comment.isLikes();
        int newVote = 0;

        if (comment.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, comment.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        comment.setLikes(newVote);
        if (position == viewHolder.getPosition()) {
            viewHolder.setVoteColors();
        }
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, "Error voting", Toast.LENGTH_SHORT)
                        .show();

                comment.setLikes(oldVote);
                if (position == viewHolder.getPosition()) {
                    viewHolder.setVoteColors();
                }
            }
        }, params, 0);
    }

    public int getIndentWidth(Comment comment) {
        return indentWidth * comment.getLevel();
    }

    public Reddit getReddit() {
        return reddit;
    }

    public ImageLoader.ImageContainer loadImage(String url, ImageLoader.ImageListener imageListener) {
        return reddit.getImageLoader().get(url, imageListener);
    }

    public void animateAlpha(View view, float start, float end) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", start, end);
        objectAnimator.setDuration(ALPHA_DURATION);
        objectAnimator.start();
    }

    public interface CommentClickListener extends DisallowListener {

        void loadUrl(String url);
        void setRefreshing(boolean refreshing);
        AdapterCommentList getAdapter();
        int getRecyclerHeight();
    }
}