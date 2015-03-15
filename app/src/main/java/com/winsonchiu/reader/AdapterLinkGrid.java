package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
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

    public AdapterLinkGrid(Activity activity, ControllerLinks controllerLinks) {
        this.controllerLinks = controllerLinks;
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        this.layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        this.itemDecoration = null;
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        ViewHolder viewHolder = (ViewHolder) holder;

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 5) {
            controllerLinks.loadMoreLinks();
        }

        Link link = controllerLinks.getLink(position);
        // TODO: Set after redraw to scale view properly
        viewHolder.imageFull.setMaxHeight(viewHeight - viewHolder.itemView.getHeight());
        viewHolder.progressImage.setVisibility(View.GONE);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null) {
            if (Reddit.checkIsImage(link.getUrl())) {
                Ion.with(viewHolder.imageFull)
                        .smartSize(true)
                        .load(link.getThumbnail());
                Ion.with(viewHolder.imageFull)
                        .crossfade(true)
                        .smartSize(true)
                        .load(link.getUrl());
            }
            else {
                viewHolder.imageFull.setImageResource(R.drawable.ic_more_vert_white_24dp);
            }
        }
        else {
            viewHolder.imageFull.setImageDrawable(drawable);
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
                    controllerLinks.getListener().onClickComments(controllerLinks.getLink(getPosition()));
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
                }
            };
            this.itemView.setOnClickListener(clickListenerLink);
            this.textThreadTitle.setOnClickListener(clickListenerLink);
        }

    }

}