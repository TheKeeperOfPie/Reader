/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.CustomColorFilter;

import java.util.Date;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class AdapterSearchSubreddits extends RecyclerView.Adapter<AdapterSearchSubreddits.ViewHolder> {

    private static final String TAG = AdapterSearchSubreddits.class.getCanonicalName();
    private RecyclerView.LayoutManager layoutManager;
    private ControllerSearchBase controllerSearchBase;
    private ViewHolder.EventListener eventListener;

    public AdapterSearchSubreddits(Activity activity,
            ControllerSearchBase controllerSearchBase,
            ViewHolder.EventListener eventListener) {
        this.controllerSearchBase = controllerSearchBase;
        this.eventListener = eventListener;
        this.layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_subreddit, parent, false), eventListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.onBind(controllerSearchBase.getSubreddit(position));
    }

    @Override
    public int getItemCount() {
        return controllerSearchBase.getSubredditCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected EventListener eventListener;
        protected ImageButton buttonReorder;
        protected ImageButton buttonOpen;
        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerExpand;
        protected CustomColorFilter colorFilterIcon;
        protected Subreddit subreddit;

        public ViewHolder(final View itemView, final EventListener eventListener) {
            super(itemView);
            this.eventListener = eventListener;

            buttonReorder = (ImageButton) itemView.findViewById(R.id.button_reorder);
            buttonOpen = (ImageButton) itemView.findViewById(R.id.button_open);
            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(R.id.layout_container_expand);

            final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (eventListener.isSubscriptionListShown()) {
                        eventListener.sendToTop(ViewHolder.this);
                        return true;
                    }
                    return super.onDoubleTap(e);
                }
            });

            buttonReorder.setOnTouchListener(new View.OnTouchListener() {

                private float startY;
                private float dragDistance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, itemView.getContext().getResources().getDisplayMetrics());

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (gestureDetector.onTouchEvent(event)) {
                        return true;
                    }

                    switch (MotionEventCompat.getActionMasked(event)) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (Math.abs(event.getY() - startY) > dragDistance) {
                                eventListener.onStartDrag(ViewHolder.this);
                            }
                            break;
                    }
                    return false;
                }
            });

            textDescription.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.offsetLocation(v.getLeft(), v.getTop());
                    ViewHolder.this.itemView.onTouchEvent(newEvent);
                    newEvent.recycle();
                    return false;
                }
            });
            textName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder.this.eventListener.onClickSubreddit(subreddit);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UtilsAnimation.animateExpand(layoutContainerExpand, 1f, null);
                }
            });

            buttonOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.onClickSubreddit(subreddit);
                }
            });

            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(
                    new int[] {R.attr.colorIconFilter});
            colorFilterIcon = new CustomColorFilter(typedArray.getColor(0, 0xFFFFFFFF), PorterDuff.Mode.MULTIPLY);
            typedArray.recycle();
            buttonReorder.setColorFilter(colorFilterIcon);
            buttonOpen.setColorFilter(colorFilterIcon);

        }

        public void onBind(Subreddit subreddit) {
            this.subreddit = subreddit;

            layoutContainerExpand.setVisibility(View.GONE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(Html.fromHtml(subreddit.getTitle()));

            if ("null".equals(subreddit.getPublicDescriptionHtml())) {
                textDescription.setVisibility(View.GONE);
            }
            else {
                textDescription
                        .setText(Reddit.getFormattedHtml(subreddit.getPublicDescriptionHtml()));
            }

            // TODO: Move to String resource

            textInfo.setText(subreddit.getSubscribers() + " subscribers\n" +
                    "created " + new Date(subreddit.getCreatedUtc()));

        }

        public interface EventListener {
            void onClickSubreddit(Subreddit subreddit);
            boolean supportsDrag();
            void onStartDrag(ViewHolder viewHolder);
            void sendToTop(ViewHolder viewHolder);
            boolean isSubscriptionListShown();
        }

    }

}
