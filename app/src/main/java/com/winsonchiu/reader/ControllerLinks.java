package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Response;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public class ControllerLinks {

    private static final String TAG = ControllerLinks.class.getCanonicalName();
    private Activity activity;
    private LinkClickListener listener;
    private Listing listingLinks;
    private boolean isLoading;
    private String sort = "";
    private String subreddit = "";
    private Drawable drawableEmpty;
    private Drawable drawableDefault;

    public ControllerLinks(Activity activity, LinkClickListener listener, String subreddit, String sort) {
        super();
        this.activity = activity;
        this.listener = listener;
        this.subreddit = subreddit;
        this.sort = sort;
        listingLinks = new Listing();
        Resources resources = activity.getResources();
        this.drawableEmpty = resources.getDrawable(R.drawable.ic_web_white_24dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_textsms_white_24dp);
        reloadAllLinks();
    }

    public void setParameters(String subreddit, String sort) {
        this.subreddit = subreddit;
        this.sort = sort;
        listener.setToolbarTitle("/r/" + subreddit);
        reloadAllLinks();
    }

    public LinkClickListener getListener() {
        return listener;
    }

    public Link getLink(int position) {
        return (Link) listingLinks.getChildren().get(position);
    }

    public Drawable getDrawableForLink(Link link) {
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

    public void reloadAllLinks() {
        setLoading(true);
        String url = "https://oauth.reddit.com" + "/r/" + subreddit + "/" + sort;
        Reddit.loadGet(activity, url, new FutureCallback<Response<String>>() {
            @Override
            public void onCompleted(Exception e, Response<String> result) {
                Log.d(TAG, "Result: " + result.getResult());

                try {
                    listingLinks = Listing.fromJson(new JSONObject(result.getResult()));
                    listener.notifyDataSetChanged();
                    listener.onFullLoaded(0);
                }
                catch (JSONException exception) {
                    exception.printStackTrace();
                }
                finally {
                    setLoading(false);
                }
            }
        }, 0);
        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        String url = "https://oauth.reddit.com" + "/r/" + subreddit + "/" + sort + "?" + "after=" + listingLinks.getAfter();

        Reddit.loadGet(activity, url, new FutureCallback<Response<String>>() {
            @Override
            public void onCompleted(Exception e, Response<String> result) {
                try {
                    int startPosition = listingLinks.getChildren()
                            .size();
                    Listing listing = Listing.fromJson(new JSONObject(result.getResult()));
                    listingLinks.addChildren(listing.getChildren());
                    listingLinks.setAfter(listing.getAfter());
                    listener.notifyItemRangeInserted(startPosition, listingLinks.getChildren()
                            .size() - 1);
                }
                catch (JSONException exception) {
                    exception.printStackTrace();
                }
                finally {
                    setLoading(false);
                }
            }
        }, 0);
    }


    public void setLoading(boolean loading) {
        isLoading = loading;
        listener.setRefreshing(loading);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public int size() {
        return listingLinks.getChildren() == null ? 0 : listingLinks.getChildren().size();
    }

    public interface LinkClickListener {

        void onClickComments(Link link);
        void loadUrl(String url);
        void onFullLoaded(int position);
        void setRefreshing(boolean refreshing);
        void setToolbarTitle(String title);
        void notifyDataSetChanged();
        void notifyItemRangeInserted(int startPosition, int endPosition);

    }

}
