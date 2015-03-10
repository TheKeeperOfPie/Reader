package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.RedditJsonRequest;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterThreadList extends RecyclerView.Adapter<AdapterThreadList.ViewHolder> {

    private static final String TAG = AdapterThreadList.class.getCanonicalName();
    private ThreadClickListener listener;
    private Reddit reddit;
    private Listing listingLinks;
    private boolean isLoading;
    private String sort = "";
    private String subreddit = "";
    private RedditJsonRequest redditJsonRequest;
    private Drawable drawableEmpty;
    private Drawable drawableDefault;
    private int maxLength;

    public AdapterThreadList(Activity activity, ThreadClickListener listener, String subreddit, String sort) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        this.listener = listener;
        this.reddit = Reddit.getReddit(activity);
        this.listingLinks = new Listing();
        this.maxLength = displayMetrics.heightPixels > displayMetrics.widthPixels ? displayMetrics.heightPixels : displayMetrics.widthPixels;
        setParameters(subreddit, sort);
        drawableEmpty = activity.getResources().getDrawable(R.drawable.ic_web_white_24dp);
        drawableDefault = activity.getResources().getDrawable(R.drawable.ic_textsms_white_24dp);
    }

    public void setParameters(String subreddit, String sort) {
        this.subreddit = subreddit;
        this.sort = sort;
        listener.setToolbarTitle("/r/" + subreddit);
        reloadAllLinks();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_thread, viewGroup, false));
    }

    @Override
    public int getItemCount() {
        return listingLinks.getChildren().size();
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {

        if (!isLoading && i > listingLinks.getChildren().size() - 5) {
            loadMoreLinks();
        }

        Link link = (Link) listingLinks.getChildren().get(i);
        viewHolder.imageFull.setVisibility(View.GONE);
        viewHolder.imageThreadPreview.setVisibility(View.VISIBLE);
        String thumbnail = link.getThumbnail();
        if (TextUtils.isEmpty(thumbnail)) {
            viewHolder.imageThreadPreview.setImageDrawable(drawableEmpty);
        }
        else if (thumbnail.equals("self") || thumbnail.equals("default") || thumbnail.equals("nsfw")) {
            viewHolder.imageThreadPreview.setImageDrawable(drawableDefault);
        }
        else {
            viewHolder.imageThreadPreview.setImageUrl(thumbnail, reddit.getImageLoader());
        }
        viewHolder.textThreadTitle.setText(link.getTitle());
        viewHolder.textThreadInfo.setText("by " + link.getAuthor());
        viewHolder.textScore.setText("" + link.getScore());

    }

    public void reloadAllLinks() {
        if (isLoading) {
            redditJsonRequest.cancel();
            reddit.getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }
        setLoading(true);
        try {
            redditJsonRequest = reddit.getMoreLinks(subreddit, sort, "", "", 30, false, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        listingLinks = Listing.fromJson(response);
                        notifyDataSetChanged();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    finally {
                        setLoading(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    setLoading(false);
                }
            });
            Log.d(TAG, "reloadAllLinks");
        }
        catch (JSONException e) {
            setLoading(false);
            e.printStackTrace();
        }
    }

    public void loadMoreLinks() {
        if (isLoading) {
            return;
        }
        setLoading(true);
        try {
            redditJsonRequest = reddit.getMoreLinks(subreddit, sort, listingLinks.getAfter(), "", 30, false, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        int startPosition = listingLinks.getChildren().size();
                        Listing listing = Listing.fromJson(response);
                        listingLinks.addChildren(listing.getChildren());
                        listingLinks.setAfter(listing.getAfter());
                        notifyItemRangeInserted(startPosition, listingLinks.getChildren().size() - 1);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    finally {
                        setLoading(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    listingLinks.setChildren(new ArrayList<Thing>());
                    setLoading(false);
                }
            });
        }
        catch (JSONException e) {
            setLoading(false);
            e.printStackTrace();
        }
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        listener.setRefreshing(loading);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected ImageViewNetwork imageFull;
        protected ImageViewNetwork imageThreadPreview;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected TextView textScore;

        public ViewHolder(View itemView) {
            super(itemView);
            this.imageFull = (ImageViewNetwork) itemView.findViewById(R.id.image_full);
            this.imageThreadPreview = (ImageViewNetwork) itemView.findViewById(R.id.image_thread_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            this.textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            this.textScore = (TextView) itemView.findViewById(R.id.text_score);
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onLinkClick((Link) listingLinks.getChildren().get(getPosition()));
                }
            });
            this.imageThreadPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = (Link) listingLinks.getChildren().get(getPosition());
                    String thumbnail = link.getThumbnail();
                    imageFull.setImageBitmap(null);
                    imageFull.setVisibility(View.GONE);
                    imageThreadPreview.setVisibility(View.VISIBLE);

                    if (!TextUtils.isEmpty(link.getUrl()) && !TextUtils.isEmpty(thumbnail) && !thumbnail.equals("self") && !thumbnail.equals("default") && !thumbnail.equals("nsfw")) {
                        // TODO: Add support for popular image domains
//                        String domain = link.getDomain();
//                        if (domain.contains("imgur")) {
//                            loadImgur(link, ViewHolder.this);
//                        }
//                        else if (domain.contains("gfycat")) {
//                            loadGfycat(link, ViewHolder.this);
//                        }
//                        else {
//                            loadBasicImage(link, ViewHolder.this);
//                        }
                        loadBasicImage(link, ViewHolder.this);
                    }
                    listener.onImagePreviewClick(ViewHolder.this.itemView, link);
                }
            });
        }


        private void loadGfycat(Link link, ViewHolder viewHolder) {
            // TODO: Add support for Gfycat
        }

        private void loadImgur(Link link, ViewHolder viewHolder) {
            // TODO: Add support for Imgur
        }

        private void loadBasicImage(final Link link, ViewHolder viewHolder) {
            reddit.fetchHeaders(link.getUrl(), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "onResponse");
                    try {
                        JSONObject headers = response.getJSONObject("headers");
                        Log.d(TAG, "Headers: " + headers);
                        if (headers.getString("Content-Type").toLowerCase().contains("image")) {
                            reddit.getImageLoader().get(link.getUrl(), new ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                    imageFull.setVisibility(View.VISIBLE);
                                    imageFull.setImageBitmap(response.getBitmap());
                                    imageThreadPreview.setVisibility(View.INVISIBLE);
                                }

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    error.printStackTrace();
                                }
                            }, maxLength, maxLength);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
        }
    }

    public interface ThreadClickListener {

        void setRefreshing(boolean loading);
        void onLinkClick(Link link);
        void onImagePreviewClick(View row, Link link);
        void setToolbarTitle(String title);

    }

}
