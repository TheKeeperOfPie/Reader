package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.AnimationUtils;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import org.json.JSONException;
import org.json.JSONObject;

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

    public void setActivity(Activity activity) {
        Resources resources = activity.getResources();
        this.activity = activity;
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
    }

    public void setControllerLinks(ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public abstract RecyclerView.ItemDecoration getItemDecoration();

    protected class ViewHolderBase extends RecyclerView.ViewHolder {

        protected MediaController mediaController;
        protected ProgressBar progressImage;
        protected ViewPager viewPagerFull;
        protected ImageView imagePlay;
        protected ImageView imagePreview;
        protected VideoView videoFull;
        protected WebViewFixed webFull;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;
        protected ImageButton buttonUpvote;
        protected ImageButton buttonDownvote;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolderBase(final View itemView) {
            super(itemView);

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
            webFull.getSettings().setBuiltInZoomControls(true);
            webFull.getSettings().setDisplayZoomControls(false);
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
                            controllerLinks.getListener()
                                    .requestDisallowInterceptTouchEvent(true);
                        }
                        else {
                            controllerLinks.getListener()
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
                        controllerLinks.getListener()
                                .requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
            viewPagerFull = (ViewPager) itemView.findViewById(R.id.view_pager_full);
            imagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
            textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            // TODO: Remove and replace with a real TextView that holds self_text
            textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);
            buttonUpvote = (ImageButton) layoutContainerActions.findViewById(R.id.button_upvote);
            buttonDownvote = (ImageButton) layoutContainerActions.findViewById(R.id.button_downvote);
            buttonUpvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.vote(ViewHolderBase.this, 1);
                }
            });
            buttonDownvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.vote(ViewHolderBase.this, -1);
                }
            });

            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.getListener().onClickComments(controllerLinks.getLink(getPosition()));
                }
            });

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVoteColors();
                    AnimationUtils.animateExpandActions(layoutContainerActions, false);
                }
            };
            textThreadTitle.setOnClickListener(clickListenerLink);
            textThreadInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);
        }

        public void loadFull(Link link ) {

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
                        loadAlbum(imgurId);

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
                    controllerLinks.getReddit()
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

        public void setVoteColors() {

            Link link = controllerLinks.getLink(getPosition());
            switch (link.isLikes()) {
                case 1:
                    buttonUpvote.setColorFilter(colorPositive, PorterDuff.Mode.MULTIPLY);
                    buttonDownvote.clearColorFilter();
                    break;
                case -1:
                    buttonDownvote.setColorFilter(colorNegative, PorterDuff.Mode.MULTIPLY);
                    buttonUpvote.clearColorFilter();
                    break;
                case 0:
                    buttonUpvote.clearColorFilter();
                    buttonDownvote.clearColorFilter();
                    break;
            }
        }

        public void setTextInfo() {
            Link link = controllerLinks.getLink(getPosition());

            String subreddit = "/r/" + link.getSubreddit();
            Spannable spannableInfo = new SpannableString(subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? colorPositive : colorNegative),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + String.valueOf(link.getScore())
                            .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);
        }


        private void attemptLoadImage(Link link) {

            if (Reddit.placeImageUrl(link)) {
                webFull.onResume();
                webFull.resetMaxHeight();
                webFull.loadData(Reddit.getImageHtml(
                        controllerLinks.getLink(getPosition())
                                .getUrl()), "text/html", "UTF-8");
                webFull.setVisibility(View.VISIBLE);
                controllerLinks.getListener()
                        .onFullLoaded(getPosition());
            }
            else {
                controllerLinks.getListener().loadUrl(link.getUrl());
            }
        }


        private void loadAlbum(String id) {
            progressImage.setVisibility(View.VISIBLE);
            imagePreview.setTag(controllerLinks.getReddit()
                    .loadImgurAlbum(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Album album = Album.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        viewPagerFull.setAdapter(
                                                new AdapterAlbum(activity, album,
                                                        controllerLinks.getListener()));
                                        viewPagerFull.getLayoutParams().height = controllerLinks.getListener()
                                                .getRecyclerHeight() - itemView.getHeight();
                                        viewPagerFull.setVisibility(View.VISIBLE);
                                        controllerLinks.getListener()
                                                .onFullLoaded(getPosition());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0));
        }


        private void loadGifv(String id) {
            imagePreview.setTag(controllerLinks.getReddit()
                    .loadImgurImage(id,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        Image image = Image.fromJson(
                                                new JSONObject(
                                                        response).getJSONObject(
                                                        "data"));

                                        loadVideo(image.getMp4(), (float) image.getHeight() / image.getWidth());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } finally {
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressImage.setVisibility(View.GONE);
                                }
                            }, 0));
        }


        private void loadVideo(String url, float heightRatio) {
            Log.d(TAG, "loadVideo: " + url + " : " + heightRatio);
            Uri uri = Uri.parse(url);
            videoFull.setVideoURI(uri);
            videoFull.getLayoutParams().height = (int) (ViewHolderBase.this.itemView.getWidth() * heightRatio);
            videoFull.setVisibility(View.VISIBLE);
            videoFull.invalidate();
            videoFull.start();
            videoFull.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoFull.start();
                        }
                    });
            controllerLinks.getListener()
                    .onFullLoaded(getPosition());
        }
    }

}
