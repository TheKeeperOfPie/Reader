package com.winsonchiu.reader;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
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
    private static final long EXPAND_ACTION_DURATION = 150;

    private Activity activity;
    private LinkClickListener listener;
    private Listing listingLinks;
    private boolean isLoading;
    private String sort = "";
    private String subreddit = "";
    private Drawable drawableEmpty;
    private Drawable drawableDefault;
    private Reddit reddit;

    public ControllerLinks(Activity activity, final LinkClickListener listener, String subreddit, String sort) {
        super();
        this.activity = activity;
        this.reddit = Reddit.getInstance(activity);
        this.listener = listener;
        listingLinks = new Listing();
        Resources resources = activity.getResources();
        this.drawableEmpty = resources.getDrawable(R.drawable.ic_web_white_24dp);
        this.drawableDefault = resources.getDrawable(R.drawable.ic_textsms_white_24dp);
        setParameters(subreddit, sort);
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
        reddit.loadGet(url, new Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // TODO: Catch null errors in parent method call
                        if (response == null) {
                            return;
                        }
                        Log.d(TAG, "Result: " + response);

                        try {
                            listingLinks = Listing.fromJson(new JSONObject(response));
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
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
        }, 0);
        Log.d(TAG, "reloadAllLinks");
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        String url = "https://oauth.reddit.com" + "/r/" + subreddit + "/" + sort + "?after=" + listingLinks.getAfter() + "&showAll=true";

        reddit.loadGet(url,
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {


                        try {
                            int startPosition = listingLinks.getChildren()
                                    .size();
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            listingLinks.addChildren(listing.getChildren());
                            listingLinks.setAfter(listing.getAfter());
                            listener.notifyItemRangeInserted(startPosition,
                                    listingLinks.getChildren()
                                            .size() - 1);
                        } catch (JSONException exception) {
                            exception.printStackTrace();
                        } finally {
                            setLoading(false);
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    public Reddit getReddit() {
        return reddit;
    }

    public ImageLoader.ImageContainer loadImage(String url, ImageLoader.ImageListener imageListener) {
        return reddit.getImageLoader().get(url, imageListener);
    }

    public void loadImage(String url, ImageViewNetwork imageView) {
        imageView.setImageUrl(url, reddit.getImageLoader());
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        listener.setRefreshing(loading);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void animateExpandActions(final View view) {
        Animation animation;
        final int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, view.getContext().getResources().getDisplayMetrics());
        if (view.getVisibility() == View.VISIBLE) {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = (int) (height * (1.0f - interpolatedTime));
                    view.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
        else {
            animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = (int) (interpolatedTime * height);
                    view.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            view.getLayoutParams().height = 0;
            view.requestLayout();
            view.setVisibility(View.VISIBLE);
        }
        animation.setDuration(EXPAND_ACTION_DURATION);
        view.startAnimation(animation);
        view.requestLayout();
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
        void requestDisallowInterceptTouchEvent(boolean disallow);
    }

}
