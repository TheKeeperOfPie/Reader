package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;

import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.User;

import java.util.List;

/**
 * Created by TheKeeperOfPie on 6/3/2015.
 */
public class ControllerSearch implements ControllerLinksBase {

    private Activity activity;
    private SharedPreferences preferences;
    private List<Subreddit> subreddits;
    private List<Link> links;
    private List<User> ussrs;

    public ControllerSearch(Activity activity) {
        setActivity(activity);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
    }

    public void setQuery(String query) {

    }

    public Subreddit getSubreddit(int position) {
        return null;
    }

    public int getSubredditCount() {
        return 0;
    }

    @Override
    public Link getLink(int position) {
        return null;
    }

    @Override
    public Reddit getReddit() {
        return null;
    }

    @Override
    public void voteLink(RecyclerView.ViewHolder viewHolder, int vote) {

    }

    @Override
    public Drawable getDrawableForLink(Link link) {
        return null;
    }

    public int getLinkCount() {
        return 0;
    }

    public User getUser(int position) {
        return null;
    }

    public int getUserCount() {
        return 0;
    }

    public interface ListenerCallback {
        ControllerSearch.Listener getListener();
        ControllerSearch getController();
        Activity getActivity();
    }

    public interface Listener {
        void onClickSubreddit(Subreddit subreddit);
    }
}
