package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private ControllerLinks controllerLinks;
    private DividerItemDecoration itemDecoration;
    private int defaultColor;
    private int deviceWidth;
    private int[] firstPositions;
    private int[] lastPositions;

    public AdapterLinkGrid(Activity activity, ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        this.deviceWidth = activity.getResources().getDisplayMetrics().widthPixels;
        this.layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        this.firstPositions = new int[2];
        this.lastPositions = new int[2];
        this.itemDecoration = null;
        this.defaultColor = activity.getResources().getColor(R.color.darkThemeBackground);
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return itemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_link, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 5) {
            controllerLinks.loadMoreLinks();
        }

        final Link link = controllerLinks.getLink(position);
        // TODO: Set after redraw to scale view properly
        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(false);
        viewHolder.progressImage.setVisibility(View.GONE);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null) {
            viewHolder.imageFull.setVisibility(View.VISIBLE);
            Ion.with(activity)
                    .load(link.getThumbnail())
                    .asBitmap()
                    .setCallback(new FutureCallback<Bitmap>() {
                        @Override
                        public void onCompleted(Exception e, Bitmap result) {
                            viewHolder.imageFull.setImageBitmap(result);
                            viewHolder.itemView.setBackgroundColor(Palette.generate(result)
                                    .getVibrantColor(defaultColor));
                            if (Reddit.placeFormattedUrl(link)) {
                                Ion.with(activity)
                                        .load(link.getUrl())
                                        .asBitmap()
                                        .setCallback(new FutureCallback<Bitmap>() {
                                            @Override
                                            public void onCompleted(Exception e, Bitmap result) {
                                                if (result != null) {
                                                    ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(
                                                            firstPositions);
                                                    ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(
                                                            lastPositions);
                                                    int firstPosition = firstPositions[0] < firstPositions[1] ? firstPositions[0] : firstPositions[1];
                                                    int lastPosition = lastPositions[0] > lastPositions[1] ? lastPositions[0] : lastPositions[1];
                                                    if (position >= firstPosition && position <= lastPosition) {
                                                        viewHolder.imageFull.setImageBitmap(result);
                                                    }
                                                    else {
                                                        viewHolder.itemView.setBackgroundColor(defaultColor);
                                                        viewHolder.imageFull.setVisibility(View.GONE);
                                                    }
                                                }
                                                else {
                                                    viewHolder.itemView.setBackgroundColor(defaultColor);
                                                    viewHolder.imageFull.setVisibility(View.GONE);
                                                }
                                            }
                                        });
                            }
                            else {
                                viewHolder.itemView.setBackgroundColor(defaultColor);
                                viewHolder.imageFull.setVisibility(View.GONE);
                            }
                        }
                    });
        }
        else {
            viewHolder.imageFull.setVisibility(View.GONE);
        }

        viewHolder.textThreadTitle.setText(link.getTitle());
        viewHolder.layoutContainerActions.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        protected ProgressBar progressImage;
        protected ImageView imageFull;
        protected TextView textThreadTitle;
        protected ImageButton buttonComments;
        protected LinearLayout layoutContainerActions;
        private View.OnClickListener clickListenerLink;

        public ViewHolder(View itemView) {
            super(itemView);

            this.progressImage = (ProgressBar) itemView.findViewById(R.id.progress_image);
            this.imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            // TODO: Remove and replace with a real TextView that holds self_text
            this.textThreadTitle.setMovementMethod(LinkMovementMethod.getInstance());
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            this.layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);

            // TODO: Change to load full size image inside of Fragment view without loading comments
            this.imageFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) ViewHolder.this.itemView.getLayoutParams();
                    layoutParams.setFullSpan(true);
                    ViewHolder.this.itemView.setLayoutParams(layoutParams);
                    controllerLinks.getListener().onFullLoaded(getPosition());
                }
            });

//            this.imageThreadPreview.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Link link = controllerLinks.getLink(getPosition());
//                    String url = link.getUrl();
//                    imageFull.setImageBitmap(null);
//                    imageFull.setVisibility(View.GONE);
//                    imageThreadPreview.setVisibility(View.VISIBLE);
//
//                    if (link.isSelf()) {
//                        String html = link.getSelfTextHtml();
//                        html = Html.fromHtml(html.trim())
//                                .toString();
//                        textThreadTitle.setText(Reddit.formatHtml(html,
//                                new Reddit.UrlClickListener() {
//                                    @Override
//                                    public void onUrlClick(String url) {
//                                        controllerLinks.getListener().loadUrl(url);
//                                    }
//                                }));
//                        controllerLinks.getListener().onFullLoaded(getPosition());
//                    }
//                    else if (!TextUtils.isEmpty(url)) {
//
//                        // TODO: Add support for popular image domains
//                        String domain = link.getDomain();
//                        if (domain.contains("imgur")) {
//                            loadImgur(link);
//                        }
//                        else {
//                            boolean isImage = Reddit.checkIsImage(url);
//                            if (isImage) {
//                                loadBasicImage(link);
//                            }
//                            else {
//                                Log.d(TAG, "loadUrl: " + url);
//                                controllerLinks.getListener().loadUrl(url);
//                            }
//                        }
//                    }
//                }
//            });
            this.buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controllerLinks.getListener().onClickComments(controllerLinks.getLink(getPosition()));
                }
            });

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layoutContainerActions.setVisibility(
                            layoutContainerActions.getVisibility() == View.VISIBLE ? View.GONE :
                                    View.VISIBLE);
                    Toast.makeText(activity, "URL: " + controllerLinks.getLink(getPosition()).getUrl(), Toast.LENGTH_SHORT).show();
                }
            };
            this.itemView.setOnClickListener(clickListenerLink);
            this.textThreadTitle.setOnClickListener(clickListenerLink);
        }

    }

}