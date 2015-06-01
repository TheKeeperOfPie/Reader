package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterLink.class.getCanonicalName();
    protected Activity activity;
    protected LayoutManager layoutManager;
    protected ControllerLinks controllerLinks;
    protected int colorPositive;
    protected int colorNegative;
    protected int colorMuted;
    protected int colorText;
    protected int colorTextAlert;
    protected float itemWidth;
    protected ControllerLinks.LinkClickListener listener;
    private static int ACTION_MENU_SIZE = 5;

    public void setActivity(Activity activity) {
        Resources resources = activity.getResources();
        this.activity = activity;
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        this.colorMuted = resources.getColor(R.color.darkThemeTextColorMuted);
        this.colorText = resources.getColor(R.color.darkThemeTextColor);
        this.colorTextAlert = resources.getColor(R.color.darkThemeTextColorAlert);
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
    }

    public void setControllerLinks(ControllerLinks controllerLinks, ControllerLinks.LinkClickListener listener) {
        this.controllerLinks = controllerLinks;
        this.listener = listener;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public abstract RecyclerView.ItemDecoration getItemDecoration();

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    protected static class ViewHolderBase extends RecyclerView.ViewHolder {

        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected ImageView imagePlay;
        protected ImageView imageThumbnail;
        protected VideoView videoFull;
        protected WebViewFixed webFull;
        protected TextView textThreadFlair;
        protected TextView textThreadTitle;
        protected TextView textThreadSelf;
        protected TextView textThreadInfo;
        protected TextView textHidden;
        protected ImageButton buttonComments;
        protected Toolbar toolbarActions;
        protected ControllerLinks.ListenerCallback callback;
        protected ImageLoader.ImageContainer imageContainer;
        protected Request request;
        protected String imageUrl;

        public ViewHolderBase(final View itemView,
                              ControllerLinks.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            imagePlay = (ImageView) itemView.findViewById(R.id.image_play);
            mediaController = new MediaController(itemView.getContext());
            videoFull = (VideoView) itemView.findViewById(R.id.video_full);
            videoFull.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaController.hide();
                }
            });
            mediaController.setAnchorView(videoFull);
            videoFull.setMediaController(mediaController);
            webFull = (WebViewFixed) itemView.findViewById(R.id.web_full);
            webFull.getSettings()
                    .setUseWideViewPort(true);
            webFull.getSettings()
                    .setBuiltInZoomControls(true);
            webFull.getSettings()
                    .setDisplayZoomControls(false);
            webFull.getSettings()
                    .setDomStorageEnabled(true);
            webFull.setBackgroundColor(0x000000);
            webFull.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    webFull.lockHeight();
                    super.onScaleChanged(view, oldScale, newScale);
                }
            });
            webFull.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        if ((webFull.canScrollVertically(1) && webFull.canScrollVertically(-1))) {
                            callback.getListener()
                                    .requestDisallowInterceptTouchEvent(true);
                        }
                        else {
                            callback.getListener()
                                    .requestDisallowInterceptTouchEvent(false);
                            if (webFull.getScrollY() == 0) {
                                webFull.setScrollY(1);
                            }
                            else {
                                webFull.setScrollY(webFull.getScrollY() - 1);
                            }
                        }
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        callback.getListener()
                                .requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            imageThumbnail = (ImageView) itemView.findViewById(R.id.image_thumbnail);
            textThreadFlair = (TextView) itemView.findViewById(R.id.text_thread_flair);
            textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            textThreadSelf = (TextView) itemView.findViewById(R.id.text_thread_self);
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.getListener()
                            .onClickComments(
                                    callback.getController()
                                            .getLink(getAdapterPosition()), ViewHolderBase.this);
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_link);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_upvote:
                            callback.getController()
                                    .voteLink(ViewHolderBase.this, 1);
                            break;
                        case R.id.item_downvote:
                            callback.getController()
                                    .voteLink(ViewHolderBase.this, -1);
                            break;
                        case R.id.item_share:
                            break;
                        case R.id.item_download:
                            String url = callback.getController().getLink(getAdapterPosition()).getUrl();
                            // TODO: Consolidate image format checking
                            if (Reddit.checkIsImage(url)) {
                                downloadImage(url);
                            }
                            break;
                        case R.id.item_web:
                            ViewHolderBase.this.callback.getListener()
                                    .loadUrl(
                                            callback.getController()
                                                    .getLink(getAdapterPosition())
                                                    .getUrl());
                            break;
                    }
                    return true;
                }
            });

            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVoteColors();
                    Link link = callback.getController()
                            .getLink(getAdapterPosition());
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());

                    setToolbarMenuVisibility();
                    ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(
                            toolbarActions.getMenu()
                                    .findItem(R.id.item_share));
                    if (shareActionProvider != null) {
                        shareActionProvider.setShareIntent(shareIntent);
                    }

                    if (Reddit.checkIsImage(link.getUrl()) || Reddit.placeImageUrl(link)) {
                        toolbarActions.getMenu().findItem(R.id.item_download).setVisible(true);
                        toolbarActions.getMenu().findItem(R.id.item_download).setEnabled(true);
                    }
                    else {
                        toolbarActions.getMenu().findItem(R.id.item_download).setVisible(false);
                        toolbarActions.getMenu().findItem(R.id.item_download).setEnabled(false);
                    }

                    AnimationUtils.animateExpandActions(toolbarActions, false);

                    float ratio = ViewHolderBase.this instanceof AdapterLinkList.ViewHolder ? 1.0f : 0.5f;
                    Log.d(TAG, "ratio: " + ratio);

                    AnimationUtils.animateExpand(textHidden, ratio);
                }
            };


            this.imageThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    imageThumbnail.setVisibility(View.GONE);
                    Link link = callback.getController().getLink(getAdapterPosition());
                    onClickThumbnail(link);
                }
            });

            textThreadTitle.setOnClickListener(clickListenerLink);
            textThreadInfo.setOnClickListener(clickListenerLink);
            textThreadSelf.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {
                    setToolbarMenuVisibility();
                }
            });

        }

        public void loadFull(Link link) {

            Log.d(TAG, "loadFull: " + link.getUrl());

            // TODO: Toggle visibility of web and video views

            String url = link.getUrl();
            if (!TextUtils.isEmpty(url)) {
                if (link.getDomain()
                        .contains("imgur")) {
                    int startIndex;
                    int lastIndex;
                    if (url.contains("imgur.com/a/")) {
                        startIndex = url.indexOf("imgur.com/a/") + 12;
                        int slashIndex = url.substring(startIndex)
                                .indexOf("/") + startIndex;
                        lastIndex = slashIndex > startIndex ? slashIndex : url.length();
                        String imgurId = url.substring(startIndex, lastIndex);
                        loadAlbum(imgurId);
                    }
                    else if (url.contains("imgur.com/gallery/")) {
                        startIndex = url.indexOf("imgur.com/gallery/") + 18;
                        int slashIndex = url.substring(startIndex)
                                .indexOf("/") + startIndex;
                        lastIndex = slashIndex > startIndex ? slashIndex : url.length();
                        String imgurId = url.substring(startIndex, lastIndex);
                        loadGallery(imgurId);
                    }
                    else if (url.contains(Reddit.GIFV)) {
                        startIndex = url.indexOf("imgur.com/") + 10;
                        int dotIndex = url.substring(startIndex)
                                .indexOf(".") + startIndex;
                        lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                        String imgurId = url.substring(startIndex, lastIndex);
                        loadGifv(imgurId);
                    }
                    else {
                        attemptLoadImage(link);
                    }
                }
                else if (link.getDomain()
                        .contains("gfycat")) {
                    int startIndex = url.indexOf("gfycat.com/") + 11;
                    int dotIndex = url.substring(startIndex)
                            .indexOf(".");
                    int lastIndex = dotIndex > startIndex ? dotIndex : url.length();
                    String gfycatId = url.substring(startIndex, lastIndex);
                    progressImage.setVisibility(View.VISIBLE);
                    callback.getController()
                            .getReddit()
                            .loadGet(Reddit.GFYCAT_URL + gfycatId,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(
                                                        response).getJSONObject(Reddit.GFYCAT_ITEM);
                                                loadVideo(jsonObject.getString(Reddit.GFYCAT_WEBM),
                                                        (float) jsonObject.getInt(
                                                                "height") / jsonObject.getInt(
                                                                "width"));
                                            }
                                            catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            finally {
                                                progressImage.setVisibility(View.GONE);
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            progressImage.setVisibility(View.GONE);
                                        }
                                    }, 0);
                }
                else {
                    attemptLoadImage(link);
                }
            }
        }

        public void onClickThumbnail(Link link) {

            if (link.isSelf()) {
                if (textThreadSelf.isShown()) {
                    textThreadSelf.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(link.getSelfText())) {
                    callback.getListener().onClickComments(link, this);
                }
                else {
                    if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                                true);
                        ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                    }

                    String html = link.getSelfTextHtml();
                    html = Html.fromHtml(html.trim())
                            .toString();

                    CharSequence sequence = Html.fromHtml(html);

                    // Trims leading and trailing whitespace
                    int start = 0;
                    int end = sequence.length();
                    while (start < end && Character.isWhitespace(sequence.charAt(start))) {
                        start++;
                    }
                    while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
                        end--;
                    }
                    sequence = sequence.subSequence(start, end);

                    textThreadSelf.setVisibility(View.VISIBLE);
                    textThreadSelf.setText(sequence);
                }
            }
            else {
                if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                    ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                            true);
                    ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                }
                loadFull(link);
            }
        }

        public void setVoteColors() {

            Link link = callback.getController()
                    .getLink(getAdapterPosition());
            switch (link.isLikes()) {
                case 1:
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().setColorFilter(
                            callback.getColorPositive(), PorterDuff.Mode.MULTIPLY);
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().clearColorFilter();
                    break;
                case -1:
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().setColorFilter(
                            callback.getColorNegative(), PorterDuff.Mode.MULTIPLY);
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().clearColorFilter();
                    break;
                case 0:
                    toolbarActions.getMenu().findItem(R.id.item_upvote).getIcon().clearColorFilter();
                    toolbarActions.getMenu().findItem(R.id.item_downvote).getIcon().clearColorFilter();
                    break;
            }
        }

        public void setTextInfo(Link link) {

        }

        private void attemptLoadImage(Link link) {

            if (Reddit.placeImageUrl(link)) {
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(
                        callback.getController()
                                .getLink(getAdapterPosition())
                                .getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
                callback.getListener()
                        .onFullLoaded(getAdapterPosition());
            }
            else {
                callback.getListener().loadUrl(link.getUrl());
            }
        }

        private void loadGallery(String id) {
            progressImage.setVisibility(View.VISIBLE);
            request = callback.getController()
                    .getReddit()
                    .loadImgurGallery(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(callback.getActivity(),
                                                        album,
                                                        callback.getListener()));
                                        viewPagerFull.getLayoutParams().height = callback.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        callback.getListener()
                                                .onFullLoaded(getAdapterPosition());
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressImage.setVisibility(View.GONE);
                                    Log.d(TAG, "loadGallery error: " + error.toString());
                                }
                            }, 0);
        }

        private void loadAlbum(String id) {
            progressImage.setVisibility(View.VISIBLE);
            request = callback.getController()
                    .getReddit()
                    .loadImgurAlbum(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Log.d(TAG, "loadAlbum: " + response);
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(callback.getActivity(), album,
                                                        callback.getListener()));
                                        viewPagerFull.getLayoutParams().height = callback.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        viewPagerFull.requestLayout();
                                        callback.getListener()
                                                .onFullLoaded(getAdapterPosition());
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0);
        }


        private void loadGifv(String id) {
            Log.d(TAG, "loadGifv: " + id);
            request = callback.getController()
                    .getReddit()
                    .loadImgurImage(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Image image = Image.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        if (!TextUtils.isEmpty(image.getMp4())) {
                                            loadVideo(image.getMp4(),
                                                    (float) image.getHeight() / image.getWidth());
                                        }
                                        else if (!TextUtils.isEmpty(image.getWebm())) {
                                            loadVideo(image.getWebm(),
                                                    (float) image.getHeight() / image.getWidth());
                                        }
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    try {
                                        Log.d(TAG, "" + error.networkResponse.statusCode);
                                        Log.d(TAG, error.networkResponse.headers.toString());
                                        Log.d(TAG, new String(error.networkResponse.data));
                                    }
                                    catch (Throwable e) {

                                    }
                                    Log.d(TAG, "error on loadGifv");
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0);
        }

        private void loadVideo(String url, float heightRatio) {
            Log.d(TAG, "loadVideo: " + url + " : " + heightRatio);
            Uri uri = Uri.parse(url);
            videoFull.setVideoURI(uri);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.getLayoutParams().height = (int) (ViewHolderBase.this.itemView.getWidth() * heightRatio);
            videoFull.invalidate();
            videoFull.start();
            videoFull.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoFull.start();
                        }
                    });
            callback.getListener()
                    .onFullLoaded(getAdapterPosition());
        }

        public void setToolbarMenuVisibility() {
            int maxNum = (int) (itemView.getWidth() / callback.getItemWidth());

            for (int index = 0; index < ACTION_MENU_SIZE; index++) {
                if (index < maxNum - 1) {
                    toolbarActions.getMenu()
                            .getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    toolbarActions.getMenu()
                            .getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }


        public void onRecycle() {

            if (imageContainer != null) {
                imageContainer.cancelRequest();
            }
            if (request != null) {
                request.cancel();
            }

            webFull.onPause();
            webFull.resetMaxHeight();
            webFull.setVisibility(View.GONE);
            videoFull.stopPlayback();
            videoFull.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);

//            if (!TextUtils.isEmpty(imageUrl) && !callback.getController().getReddit().getImageLoader().isCached(imageUrl, 0, 0)) {
//                Drawable drawable = imageThumbnail.getDrawable();
//                if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
//                    ((BitmapDrawable) drawable).getBitmap().recycle();
//                }
//                imageUrl = null;
//            }
            imageThumbnail.setImageBitmap(null);

        }

        public void onBind(int position) {

        }

        public void downloadImage(String url) {
            Picasso.with(callback.getActivity()).load(url).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ReaderForReddit/" + System.currentTimeMillis() + ".png");

                    file.getParentFile().mkdirs();

                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        }
                        catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                    Toast.makeText(callback.getActivity(), "Image downloaded", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });
        }

    }

}
